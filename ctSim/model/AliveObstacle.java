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

import static ctSim.model.AliveObstacle.ObstState.*;

import java.awt.Color;
import java.util.EnumSet;
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
public abstract class AliveObstacle implements Runnable {
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.AliveObstacle");

	private static final CountingMap numInstances = new CountingMap();
	private final String name;

	private final List<Runnable> disposeListeners = Misc.newList();

	private final List<Closure<Color>> appearanceListeners = Misc.newList();

	public enum ObstState {
		/** Das Hindernis hat eine Kollision */
		COLLIDED(0x001, "collision",
			"kollidiert",
			"ist nicht mehr kollidiert"),

		IN_HOLE(0x002, "falling",
			"hat keinen Boden mehr unter den F\u00FC\u00DFen",
			"hat wieder Boden unter den F\u00FC\u00DFen"),

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
		HALTED(0x100, "halted",
			"wird aus der Simulation ausgeschlossen");

		final int legacyValue;
		final String appearanceKeyInXml;
		final String messageOnEnter;
		final String messageOnExit;

		ObstState(int legacyValue, String appearanceKeyInXml,
		String messageOnEnter) {
			this(legacyValue, appearanceKeyInXml, messageOnEnter, null);
		}

		ObstState(int legacyValue, String appearanceKeyInXml,
		String messageOnEnter, String messageOnExit) {
			this.legacyValue = legacyValue;
			this.appearanceKeyInXml = appearanceKeyInXml;
			this.messageOnEnter = messageOnEnter;
			this.messageOnExit = messageOnExit;
		}
	}

	private final EnumSet<ObstState> obstState = EnumSet.noneOf(
		ObstState.class);

	private Point3d posInWorldCoord = new Point3d();

	/**
	 * Letzte Position, an dem sich das AliveObstacle befand und dabei der
	 * obstState &quot;SAFE&quot; war. Sinn: Dieses Feld wird verwendet zur
	 * Berechnung des Abstands zum Ziel, was auch funktionieren soll, wenn der
	 * Bot z.B. in ein Loch gefallen ist (d.h. jetzt un-&quot;SAFE&quot; ist).
	 * Daher wird in dieser Variablen die letzte Position gehalten, wo der Bot
	 * noch au&szlig;erhalb des Lochs war.
	 */
	private Point3d lastSafePos = new Point3d();

	private Vector3d headingInWorldCoord = new Vector3d();

	/** Unser Teil des J3D-Szenegraph:
	 * BranchGroup-Instanz
	 *
	 *      |
	 *      | hat Kind
	 *      v
	 *
	 * TransformGroup-Instanz
	 *
	 *      |
	 *      | hat Kind
	 *      v
	 *
	 *    shape
	 */
	private final BranchGroup branchgrp;
	private final TransformGroup transformgrp;
	private Group shape = null;

	/** Verweis auf den zugehoerigen Controller */
	// TODO: Verweis auf Controller wirklich noetig?
	private DefaultController controller;

	private Thread thrd;

	/** Simultime beim letzten Aufruf */
	// TODO
	private long lastSimulTime = 0;

	/** Zeit zwischen letztem Aufruf von UpdateSimulation und jetzt*/
	// TODO
	private long deltaT = 0;

	/**
	 * @param position Position des Objekts
	 * @param heading Blickrichtung des Objekts
	 */
	public AliveObstacle(String name, Point3d position, Vector3d heading) {
		this.name = name;

		// Instanz-Zahl erhoehen
		numInstances.increase(getClass());
		// Wenn wir sterben, Instanz-Zahl reduzieren
		addDisposeListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				numInstances.decrease(AliveObstacle.this.getClass());
			}
		});

		// Translationsgruppe fuer das Obst
		transformgrp = new TransformGroup();
		transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		transformgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Jetzt wird noch alles nett verpackt
		branchgrp = new BranchGroup();
		branchgrp.setCapability(BranchGroup.ALLOW_DETACH);
		branchgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);
		branchgrp.addChild(this.transformgrp);

		setPosition(position);
		setHeading(heading);
		updateAppearance();
	}

	/**
	 * @return Die 3D-Gestalt des Objekts
	 */
	public final Group getShape() {
		return shape;
	}

	/**
	 * Setzt die 3D-Gestalt, die das Hindernis hat. Nur einmal aufrufen:
	 * Wechseln der Gestalt wird nicht unterst&uuml;tzt
	 */
	public final void initShape(Group theShape) {
		if (this.shape != null)
			throw new IllegalStateException();
		this.shape = theShape;
		transformgrp.addChild(theShape);
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

		for (Runnable r : disposeListeners)
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
	 * @param posInWorldCoord Die Position, an die der Bot gesetzt werden soll
	 */
	public final synchronized void setPosition(Point3d posInWorldCoord) {
		// Optimierung (Transform-Kram ist teuer)
		if (this.posInWorldCoord.equals(posInWorldCoord))
			return;

		this.posInWorldCoord = posInWorldCoord;

		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.setTranslation(new Vector3d(posInWorldCoord));
		transformgrp.setTransform(t);

		if (! is(COLLIDED)
		&&  ! is(IN_HOLE)) {
			lastSafePos.set(posInWorldCoord);
		}
	}

	//$$ Ziemlicher Quatsch, dass das ein Vector3 ist: double mit dem Winkel drin waere einfacher und wuerde dasselbe leisten
	public final synchronized void setHeading(Vector3d headingInWorldCoord) {
		// Optimierung (Transform-Kram ist teuer)
		if (this.headingInWorldCoord.equals(headingInWorldCoord))
			return;

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

	public void set(ObstState state, boolean setOrClear) {
		if (setOrClear)
			set(state);
		else
			clear(state);
	}

	public void set(ObstState state) {
		if (obstState.add(state)) {
			lg.info(getName()+" "+state.messageOnEnter);
			updateAppearance();
		}
	}

	public void clear(ObstState state) {
		if (obstState.remove(state)) {
			lg.info(getName()+" "+state.messageOnExit);
			updateAppearance();
		}
	}

	public boolean is(ObstState s) {
		return obstState.contains(s);
	}

	public boolean isObstStateNormal() {
		return obstState.isEmpty();
	}

	// Gemaess dbfeld-ctsim-log-state.txt
	public int getLegacyObstState() {
		int rv = 0;
		for (ObstState s : obstState)
			rv += s.legacyValue;
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
				if (! is(HALTED))
					work();

				// berechne die Zeit, die work benoetigt hat
				long elapsedTime = System.currentTimeMillis() - realTimeBegin;

				// Wenn der Timeout aktiv ist und zuviel Zeit benoetigt wurde, halte dieses Alive Obstacle an
				if ((timeout > 0) && (elapsedTime > timeout))
					set(HALTED);

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
		shape=null;
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

	private void updateAppearance() {
		String key;
		if (obstState.isEmpty())
			key= "normal";
		else
			// appearanceKey des ersten gesetzten Elements
			// Im Fall, dass mehr als ein ObstState gesetzt ist (z.B. COLLIDED
			// und zugleich HALTED), werden alle ignoriert ausser dem ersten
			key = obstState.iterator().next().appearanceKeyInXml;

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

	public void addDisposeListener(Runnable runsWhenAObstDisposes) {
		if (runsWhenAObstDisposes == null)
			throw new NullPointerException();
		disposeListeners.add(runsWhenAObstDisposes);
	}

	public final void dispose() {
		for (Runnable r : disposeListeners)
			r.run();
	}

	public void addAppearanceListener(
	Closure<Color> calledWhenObstStateChanges) {
		if (calledWhenObstStateChanges == null)
			throw new NullPointerException();
		appearanceListeners.add(calledWhenObstStateChanges);
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
