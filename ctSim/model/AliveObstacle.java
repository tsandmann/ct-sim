/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
 *
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 */
package ctSim.model;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.controller.Config;
import ctSim.controller.DefaultController;
import ctSim.util.Closure;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.gui.Debug;

/**
 * Klasse fuer alle Hindernisse die sich selbst bewegen koennen
 *
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public abstract class AliveObstacle implements MovableObstacle, Runnable {
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.AliveObstacle");

   private static final CountingMap numInstances = new CountingMap();

	private final List<Runnable> deathListeners = Misc.newList();
	private final List<Closure<Color>> appearanceListeners = Misc.newList();

	/**
	 * <p>
	 * Das Obstacle ist von der weiteren Simulation ausgeschlossen, d.h. seine
	 * work()-Methode wird nicht mehr aufgerufen. Es steht damit nur noch rum,
	 * bis die Simulation irgendwann endet. Dieser Zustand kann eintreten, wenn
	 * &ndash;
	 * <ul>
	 * <li>die work()-Methode l&auml;nger rechnet, als der AliveObstacleTimeout
	 * in der Konfigdatei erlaubt, oder</li>
	 * <li>wenn die TCP-Verbindung eines CtBotSimTcp abrei&szlig;t (d.h. der
	 * CtBotSimTcp gibt sich dann diesen Status).</li>
	 * </ul>
	 * </p>
	 */
	public static final int OBST_STATE_HALTED   = 0x0100;

	private int obstState = OBST_STATE_NORMAL;

	private String name;
	private Point3d posInWorldCoord;

	/**
	 * Letzte Position, an dem sich das AliveObstacle befand und dabei der
	 * obstState &quot;SAFE&quot; war. Sinn: Dieses Feld wird verwendet zur
	 * Berechnung des Abstands zum Ziel, was auch funktionieren soll, wenn der
	 * Bot z.B. in ein Loch gefallen ist (d.h. jetzt un-&quot;SAFE&quot; ist).
	 * Daher wird in dieser Variablen die letzte Position gehalten, wo der Bot
	 * noch au&szlig;erhalb des Lochs war.
	 */
	private Point3d lastSafePos = new Point3d();

	private Vector3d headingInWorldCoord;

	/** Verweis auf den zugehoerigen Controller */
	// TODO: Verweis auf Controller wirklich noetig?
	private DefaultController controller;

	private Thread thrd;

	private BranchGroup branchgrp;
	private TransformGroup transformgrp;
	private Group shape;

	/** Simultime beim letzten Aufruf */
	// TODO
	private long lastSimulTime = 0;

	/** Zeit zwischen letztem Aufruf von UpdateSimulation und jetzt*/
	// TODO
	private long deltaT = 0;

	/**
	 * @param shape Form des Obstacle
	 * @param position Position des Objekts
	 * @param heading Blickrichtung des Objekts
	 */
	public AliveObstacle(String name, Point3d position, Vector3d heading) {
		this.name = name;

		// Instanz-Zahl erhoehen
		numInstances.increase(getClass());
		// Wenn wir sterben, Instanz-Zahl reduzieren
		addDeathListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				numInstances.decrease(AliveObstacle.this.getClass());
			}
		});

		initBG();

		setPosition(position);
		setHeading(heading);
		updateAppearance();
	}

	private void initBG() {

		// Translationsgruppe fuer das Obst
		this.transformgrp = new TransformGroup();
		this.transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		this.transformgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Jetzt wird noch alles nett verpackt
		this.branchgrp = new BranchGroup();
		this.branchgrp.setCapability(BranchGroup.ALLOW_DETACH);
		this.branchgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.branchgrp.addChild(this.transformgrp);
	}

	/**
	 * @return Die 3D-Gestalt des Objekts
	 */
	public final Group getShape() {
		return shape;
	}

	/**
	 * @param shape 3D-Gestalt, die das Objekt erhalten soll
	 */
	public final void setShape(Group shape) {
		// TODO: Test: Reicht auch einfach "this.shape"-Referenz anzupassen?
		if (this.shape != null)
			transformgrp.removeChild(shape);
		this.shape = shape;
		transformgrp.addChild(shape);
	}

	/**
	 * @param ctrl Referenz auf den Controller, die gesetzt werden soll
	 */
	public final void setController(DefaultController ctrl) {

		this.controller = ctrl;
	}

	public final BranchGroup getBranchGroup() {
		return branchgrp;
	}

	/**
	 * @param relTrans
	 * @param comp
	 */
	public final void addBranchComponent(Transform3D relTrans, Node comp) {

		TransformGroup tg = new TransformGroup();
		tg.setTransform(relTrans);
		tg.addChild(comp);

		this.transformgrp.addChild(tg);
	}

	/**
	 * Startet den Bot (bzw. dessen Thread).
	 */
	public final void start() {

		this.thrd = new Thread(this, "ctSim/"+this.getName());

		this.thrd.start();
	}

	/**
	 *  Stoppt den Bot (bzw. dessen Thread).
	 */
	public void die() {

		Thread dummy = this.thrd;

		if(dummy == null)
			return;

		this.thrd = null;
		dummy.interrupt();

		for (Runnable r : deathListeners)
			r.run();
	}

	/**
	 * <p>
	 * Laufende Nummer des AliveObstacles, und zwar abh&auml;ngig von der
	 * Subklasse: Laufen z.B. 2 GurkenBots und 3 TomatenBots (alle von
	 * AliveObstacle abgeleitet), dann sind die Instance-Numbers:
	 * <ul>
	 * <li>GurkenBot 0</li>
	 * <li>GurkenBot 1</li>
	 * <li>TomatenBot 0</li>
	 * <li>TomatenBot 1</li>
	 * <li>TomatenBot 2</li>
	 * </ul>
	 * Instance-Numbers fangen immer bei 0 an.
	 * </p>
	 *
	 * @see #getName()
	 */
	private int getInstanceNumber() {
		/*
		 * Drandenken: Wenn einer ne Subklasse instanziiert, die von
		 * AliveObstacle abgeleitet ist, wird eine AliveObstacle-Instanz
		 * automatisch miterzeugt -- Wenn wir hier getClass() aufrufen, liefert
		 * das aber die exakte Klasse (also in unserm Fall niemals
		 * AliveObstacle, sondern z.B. BestimmterDingsBot)
		 */
		return numInstances.get(getClass());
	}

	/**
	 * <p>
	 * Benutzerfreundlicher Name des Bots (wie dem Konstruktor &uuml;bergeben),
	 * an die falls erforderlich eine laufende Nummer angeh&auml;ngt ist. Laufen
	 * z.B. 2 AliveObstacle-Instanzen mit Namen &quot;Gurken-Bot&quot; und 3 mit
	 * &quot;Tomaten-Bot&quot;, sind die R&uuml;ckgabewerte dieser Methode:
	 * <ul>
	 * <li>Gurken-Bot</li>
	 * <li>Gurken-Bot (2)</li>
	 * <li>Tomaten-Bot</li>
	 * <li>Tomaten-Bot (2)</li>
	 * <li>Tomaten-Bot (3)</li>
	 * </ul>
	 * </p>
	 */
	public String getName() {
		int n = getInstanceNumber() + 1; // 1-based ist benutzerfreundlicher
		return name + ((n < 2) ? "" : " (" + n + ")");
	}

	public String getDescription() {
		return "Was Bewegliches";
	}

	/**
	 * Diese Methode enthaelt die Routinen, die der Bot waehrend seiner Laufzeit
	 * immer wieder durchfuehrt. Die Methode darf keine Schleife enthalten!
	 */
	protected abstract void work();

	/**
	 * @return Gibt die Position zurueck
	 */
	public final Point3d getPositionInWorldCoord() {
		return posInWorldCoord;
	}

	public final Vector3d getHeadingInWorldCoord() {
		return headingInWorldCoord;
	}

	/**
	 * Drandenken: State setzen vor Aufruf dieser Methode
	 *
	 * @param posInWorldCoord Die Position, an die der Bot gesetzt werden soll
	 */
	public final synchronized void setPosition(Point3d posInWorldCoord) {
		this.posInWorldCoord = posInWorldCoord;

		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.setTranslation(new Vector3d(posInWorldCoord));
		transformgrp.setTransform(t);

		if ((obstState & OBST_STATE_SAFE) == 0)
			lastSafePos.set(posInWorldCoord);
	}

	//$$ Ziemlicher Quatsch, dass das ein Vector3 ist: double mit dem Winkel drin waere einfacher und wuerde dasselbe leisten
	public final synchronized void setHeading(Vector3d headingInWorldCoord) {
		/*
		 * Sinn der Methode: Transform3D aktualisieren, das von Bot- nach
		 * Weltkoordinaten transformiert. (Dieses steckt in unserer
		 * TransformGroup.)
		 */
		this.headingInWorldCoord = headingInWorldCoord;

		// Winkel zwischen Welt pos. y-Achse und Bot pos. y-Achse
		double angleInRad = radiansToYAxis(headingInWorldCoord);

		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.setRotation(new AxisAngle4d(0, 0, 1, angleInRad));
		transformgrp.setTransform(t);
	}

	/**
	 * Winkel zwischen positiver y-Achse der Welt und &uuml;bergebenem Vektor.
	 * Ergebnis ist im Bogenma&szlig; und liegt im Intervall [&minus;&pi;;
	 * +&pi;], positive Werte entsprechen der Backbordseite.
	 */
	//$$ Was ist mit exakt entgegengesetzt? ist das +Pi oder -Pi? -> Doku
	private static double radiansToYAxis(Vector3d v) {
		double rv = v.angle(new Vector3d(0, 1, 0));
		if (v.x > 0)
			rv = -rv;
		return rv;
	}

	/**
	 * &Uuml;berschreibt die run()-Methode aus der Klasse Thread und arbeitet
	 * zwei Schritte ab: <br/> 1. {@link #work()} &ndash; wird in einer Schleife
	 * immer wieder aufgerufen <br/> 2. {@link #cleanup()} &ndash; r&auml;umt
	 * auf.<br/> Die Schleife l&auml;uft so lang, bis sie von der Methode die()
	 * beendet wird.
	 *
	 * @see AliveObstacle#work()
	 */
	public final void run() {
		Thread thisThread = Thread.currentThread();

		int timeout = 0;
		try {
			timeout = Integer.parseInt(Config.getValue("AliveObstacleTimeout"));
		} catch (Exception e) {
			// Wenn kein Teimout im Config-File steht, ignorieren wir dieses Feature
			timeout = 0;
		}

		try {
			while (this.thrd == thisThread) {
				// Stoppe die Zeit, die work() benoetigt
				long realTimeBegin = System.currentTimeMillis();

				// Ein AliveObstacle darf nur dann seine work()-Routine 
				// ausfuehren, wenn es nicht Halted ist
				if ((this.obstState & OBST_STATE_HALTED) == 0)
					work();

				// berechne die Zeit, die work benoetigt hat
				long elapsedTime = System.currentTimeMillis() - realTimeBegin;

				// Wenn der Timeout aktiv ist und zuviel Zeit benoetigt wurde, halte dieses Alive Obstacle an
				if ((timeout > 0) && (elapsedTime > timeout))
					setHalted(true);

				if (this.thrd != null)
					this.controller.waitOnController();
			}
		} catch(InterruptedException ie) {
			lg.warn("Alive Obstacle '%s' wurde unterbrochen und stirbt",
				getName());
		}
		Debug.out.println("Alive Obstacle \""+this.getName()+"\" stirbt..."); //$NON-NLS-1$ //$NON-NLS-2$
		// TODO: ???
		cleanup();
	}

	/** Aufraeumen, wenn Bot stirbt */
	protected void cleanup() {
		// TODO Auto-generated method stub
		branchgrp=null;
		transformgrp=null;
		shape=null;
	}

	/**
	 * Haelt ein Alive-Obstacle an oder gibt es wieder frei. Ist ein
	 * Alive-Obstacle halted, so darf es seine work()-Methode nicht mehr
	 * ausfuehren. Es ist aber ansonsten noch funktionsfaehig und reagiert auch
	 * auf den Controller
	 *
	 * @param halt true, wennd as AliveObstacle angehalten werden soll; false,
	 * wenn es wieder freigelassen werden soll
	 */
	public final void setHalted(boolean halt){
		if (halt == true) {
			lg.info("AliveObstcale "+getName()+" wird angehalten und darf " +
					"ab sofort die work()-Methode nicht mehr ausfuehren");
			setObstState(getObstState() | OBST_STATE_HALTED);
		} else {
			lg.info("AliveObstcale "+getName()+" wird reaktiviert und darf " +
					"ab sofort die work()-Methode wieder ausfuehren");
			setObstState(getObstState() & ~OBST_STATE_HALTED);
		}
	}

	//$$ Umbenennen: isHalted()
	/**
	 * Liefert true zurueck, wenn der OBST_STATE_HALTED gesetzt ist
	 * @return true, wenn OBST_STATE_HALTED gesetzt, false, wenn nicht
	 */
	public final boolean istHalted(){
		if ((this.obstState & OBST_STATE_HALTED) != 0)
			return true;
		return false;
	}

	/**
	 * Hier wird aufgeraeumt, wenn die Lebenszeit des AliveObstacle zuende ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest, das Panel
	 * wird aus dem ControlFrame entfernt
	 *
	 * @see AliveObstacle#work()
	 */
	// TODO:
//	protected void cleanup() {
//		((BranchGroup)getNodeReference(BG)).detach();
//		world.remove(this);
//		world = null;
//	}

	// $$ Bounds ins AObstacle
	/**
	 * @return Gibt die Grenzen des Bots zurueck
	 */
//	public Bounds getBounds() {
//		return (Bounds) bounds.clone();
//	}
//
//	/**
//	 * @param bounds
//	 *            Referenz auf die Grenzen des Bots, die gesetzt werden sollen
//	 */
//	public void setBounds(Bounds bounds) {
//		this.bounds = bounds;
//	}

	/**
	 * Liefert den Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 *
	 * @return Der Zustand des Objekts
	 */
	public int getObstState() {
		return this.obstState;
	}

	/**
	 * Setztden Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 *
	 * @param newObstState Der Zustand, der gesetzt werden soll
	 */
	public void setObstState(int newObstState) {
		if (newObstState == obstState)
			return;

		if ((newObstState & OBST_STATE_COLLISION) != 0
		&& ((obstState    & OBST_STATE_COLLISION) == 0))
			lg.info(getName()+" kollidiert");
		if ((newObstState & OBST_STATE_COLLISION) == 0
		&& ((obstState    & OBST_STATE_COLLISION) != 0))
			lg.info(getName()+" ist nicht mehr kollidiert");

		if ((newObstState & OBST_STATE_FALLING) != 0
			&& ((obstState    & OBST_STATE_FALLING) == 0))
			lg.info(getName()+" verliert den Boden unter den F\u00FC\u00DFen");
		if ((newObstState & OBST_STATE_FALLING) == 0
			&& ((obstState    & OBST_STATE_FALLING) != 0))
			lg.info(getName()+" hat wieder Boden unter den F\u00FC\u00DFen");

		this.obstState = newObstState;
		updateAppearance();
	}

	private void updateAppearance() {
		//$$ obstState sollte ein Enum werden oder hoechstens ein EnumSet
		//$$ Also die Strings gehoeren woanders hin; ObstState als Enum, Strings da rein
		String key= null;

		if (getObstState() == OBST_STATE_COLLISION)
			key= "collision";
		if (getObstState() == OBST_STATE_FALLING)
			key= "falling";
		if (getObstState() == OBST_STATE_NORMAL)
			key= "normal";
		if ((getObstState() & OBST_STATE_HALTED) != 0)
			key="halted";

		Color c = Config.getBotColor(getClass(), getInstanceNumber(), key);

		for (Closure<Color> listener : appearanceListeners)
			listener.run(c);
	}

	public Color getNormalObstColor() {
		return Config.getBotColor(getClass(), getInstanceNumber(), "normal");
	}

	/**
	 * Diese Methode wird von au&szlig;en aufgerufen und erledigt die ganze
	 * Aktualisierung der Simulation. Steuerung des Bots hat hier jedoch nichts
	 * zu suchen. Die geh&ouml;rt in work().
	 *
	 * @param simulTime
	 * @see AliveObstacle#work()
	 */
	public void updateSimulation(long simulTime){
		deltaT = simulTime - lastSimulTime;
		lastSimulTime = simulTime;
	}

	/**
	 * Liefert die Sim-Zeit, die verstrichen ist seit dem vorigen Aufruf von
	 * updateSimulation().
	 *
	 * @return Delta-T in Millisekunden
	 */
	public long getDeltaTInMs() {
		return deltaT;
	}

	/**
	 * Liefert die letzte als sicher erachtete Position des Bots zurueck
	 * @return Returns the lastSavePos.
	 */
	public Point3d getLastSafePos() {
		return lastSafePos;
	}

	public void addDeathListener(Runnable runsWhenAObstDies) {
		if (runsWhenAObstDies == null)
			throw new NullPointerException();
		deathListeners.add(runsWhenAObstDies);
	}

	public Point3d worldCoordFromBotCoord(Point3d inBotCoord) {
		Point3d rv = (Point3d)inBotCoord.clone();
		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.transform(rv);
		return rv;
	}

	public Vector3d worldCoordFromBotCoord(Vector3d inBotCoord) {
		Vector3d rv = (Vector3d)inBotCoord.clone();
		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.transform(rv);
		return rv;
	}

		public void addAppearanceListener(
	Closure<Color> calledWhenObstStateChanges) {
		if (calledWhenObstStateChanges == null)
			throw new NullPointerException();
		appearanceListeners.add(calledWhenObstStateChanges);
	}

	public void removeObstStateListener(Closure<Color> listener) {
		appearanceListeners.remove(listener);
	}

	public static class CountingMap
	extends HashMap<Class<? extends AliveObstacle>, Integer> {
		private static final long serialVersionUID = 6419402218947363629L;

		public synchronized void increase(Class<? extends AliveObstacle> c) {
			if (containsKey(c))
				put(c, get(c) + 1);
			else
				put(c, 0);
		}

		public synchronized void decrease(Class<? extends AliveObstacle> c) {
			if (containsKey(c))
				put(c, get(c) - 1);
			else
				throw new IllegalStateException();
		}
	}
}
