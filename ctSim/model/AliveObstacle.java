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

import java.util.HashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.ConfigManager;
import ctSim.SimUtils;
import ctSim.controller.DefaultController;
import ctSim.util.FmtLogger;
import ctSim.view.gui.Debug;

/**
 * Klasse fuer alle Hindernisse die sich selbst bewegen koennen
 *
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public abstract class AliveObstacle implements MovableObstacle, Runnable {
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.AliveObstacle");

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

	/**
	 * Letzte Position, an dem sich das AliveObstacle befand und dabei der
	 * obstState &quot;SAFE&quot; war. Sinn: Dieses Feld wird verwendet zur
	 * Berechnung des Abstands zum Ziel, was auch funktionieren soll, wenn der
	 * Bot z.B. in ein Loch gefallen ist (d.h. jetzt un-&quot;SAFE&quot; ist).
	 * Daher wird in dieser Variablen die letzte Position gehalten, wo der Bot
	 * noch au&szlig;erhalb des Lochs war.
	 */
	private Point3d lastSafePos = new Point3d();

	/** Verweis auf den zugehoerigen Controller */
	// TODO: hmmm
	private DefaultController controller;

	private Thread thrd;

	private Shape3D shape;

	// TODO:
	private HashMap<String, Appearance> apps;

	/** Simultime beim letzten Aufruf */
	// TODO
	private long lastSimulTime = 0;

	/** Zeit zwischen letztem Aufruf von UpdateSimulation und jetzt*/
	// TODO
	private long deltaT = 0;

	private NavState navState;

	private BranchGroup branchgrp;

	/**
	 * @param shape Form des Obstacle
	 * @param position Position des Objekts
	 * @param heading Blickrichtung des Objekts
	 */
	public AliveObstacle(Shape3D shape, String name, Point3d position,
		double heading) {
		/* Teil des J3D-Szenegraph, den wir hier bauen:
		 *
		 * BranchGroup-Instanz
		 *          |
		 *          |
		 *   NavState-Instanz (NavState extends TransformGroup)
		 *          |
		 *          |
		 *      this.shape (Shape3D)
		 */
		this.name = name;

		shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		shape.setCapability(Node.ALLOW_PICKABLE_WRITE);
		shape.setPickable(true);
		this.shape = shape;

		navState = new NavState(position, heading);
		navState.addChild(shape);

		branchgrp = new BranchGroup();
		branchgrp.setCapability(BranchGroup.ALLOW_DETACH);
		branchgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);
		branchgrp.addChild(navState);
	}

	/**
	 * @return Die 3D-Gestalt des Objekts
	 */
	public final Shape3D getShape() {

		return this.shape;
	}

	// TODO: Altlast: anders loesen...
	/**
	 * @param map
	 */
	private final void setAppearances(HashMap<String, Appearance> map) {

		if(map.isEmpty())
			return;

		this.apps = map;

		this.setAppearance(this.getObstState());
	}

	/**
	 * @param ctrl Referenz auf den Controller, die gesetzt werden soll
	 */
	public final void setController(DefaultController ctrl) {

		this.controller = ctrl;
		// setApp darf erst hier passieren (keine Ahnung warum). Es macht unerklaerliche NullPtrExcp, wenn es im Konstruktor ist
		setAppearances(ConfigManager.getBotAppearances(getName()));
	}

	public final BranchGroup getBranchGroup() {
		return branchgrp;
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
	public void stop() {

		Thread dummy = this.thrd;

		if(dummy == null)
			return;

		this.thrd = null;
		dummy.interrupt();
	}

	/**
	 * @return Gibt den Namen des Objektes zurueck.
	 */
	// TODO: Should be abstract or Interface -> Move to Bot with Pos, Head, ...
	//       A Bot should be a (big) BotComponent (BotPosition)...
	public String getName() {
		return this.name;
	}

	/**
	 * @return Die Hoehe des Objektes in Metern
	 */
//	abstract public float getHeight();

	/**
	 * Diese Methode muss alles enthalten, was ausgefuehrt werden soll, bevor
	 * der Thread ueber work() seine Arbeit aufnimmt
	 *
	 * @see AliveObstacle#work()
	 */
	abstract protected void init();

	/**
	 * Diese Methode enthaelt die Routinen, die der Bot waehrend seiner Laufzeit
	 * immer wieder durchfuehrt. Die Methode darf keine Schleife enthalten!
	 */
	abstract protected void work();

	/**
	 * @return Gibt die Position zurueck
	 */
	// TODO: Vorsicht: Pose ist relativ zur Welt!
	public final Point3d getPosition() {
		return navState.getPosition();
	}

	// TODO: Vorsicht: Heading ist relativ zur Welt!
	public final Vector3d getHeading() {
		return navState.getHeading();
	}

	public void setHeadingInDeg(double heading) {
		navState.setHeadingInDeg(heading);
	}

	public void setPosition(Point3d position) {
		navState.setPosition(position);
		if ((obstState & OBST_STATE_SAFE) == 0)
			lastSafePos.set(position);
	}

	/**
	 * @return Die Transformation
	 */
	public final Transform3D getTransform() {
		return SimUtils.getTransform(this.getPosition(), this.getHeading());
	}

	/**
	 * Ueberschreibt die run() Methode aus der Klasse Thread und arbeitet drei
	 * Schritte ab: <br/> 1. init() - initialisiert alles <br/> 2. work() - wird
	 * in einer Schleife immer wieder aufgerufen <br/> 3. cleanup() - raeumt auf
	 * <br/> Die Methode die() beendet diese Schleife.
	 *
	 * @see AliveObstacle#init()
	 * @see AliveObstacle#work()
	 */
	public final void run() {

		Thread thisThread = Thread.currentThread();

		init();

		int timeout = 0;
		try {
			timeout = Integer.parseInt(ConfigManager.getValue("AliveObstacleTimeout"));
		} catch (Exception e) {
			// Wenn kein Teimout im Config-File steht, ignorieren wir dieses Feature
			timeout = 0;
		}

		try {
			while (this.thrd == thisThread) {
				// Stoppe die Zeit, die work() benoetigt
				long realTimeBegin = System.currentTimeMillis();

				// Ein AliveObstacle darf nur dann seine work()-Routine ausführen, wenn es nicht Halted ist
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

	/**
	 * Aufr&auml;umen, wenn Bot stirbt
	 */
	protected void cleanup() {
		// TODO Auto-generated method stub
		shape=null;
	}

	/**
	 * Beendet den AliveObstacle-Thread<b>
	 *
	 * @see AliveObstacle#work()
	 */
	public final void die() {
		this.stop();
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

	// TODO: im Obstacle?
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
	 * @param state Der Zustand, der gesetzt werden soll
	 */
	public void setObstState(int state) {
		this.obstState = state;

		// TODO:
		this.setAppearance(state);
	}

	private void setAppearance(int state) {

		// TODO:
		if(this.apps == null || this.apps.isEmpty())
			return;

		String key= null;

		if (state == OBST_STATE_COLLISION)
			key= "collision";
		if (state == OBST_STATE_FALLING)
			key= "falling";
		if (state == OBST_STATE_NORMAL)
			key= "normal";
		if ((state & OBST_STATE_HALTED) != 0)
			key="halted";

		if (this.apps.containsKey(key))
			this.shape.setAppearance(this.apps.get(key));
	}

	/**
	 * Diese Methode wird von außen aufgerufen und erledigt die ganze Aktualisierung
	 * der Simulation.
	 * Steuerzung des Bots hat hier jedoch nichts zu suchen. die gehört in work()
	 * @param simulTime
	 * @see AliveObstacle#work()
	 */
	public void updateSimulation(long simulTime){
		deltaT = simulTime - lastSimulTime;
		lastSimulTime = simulTime;
	}

	/**
	 * Liefert die Zeit zwischen dem aktuellen Stand und dem vorhergehenden Aufruf von updateSimulation
	 * @return Returns the deltaT.
	 */
	public long getDeltaT() {
		return deltaT;
	}

	/**
	 * Liefert die letzte als sicher erachtete Position des Bots zurueck
	 * @return Returns the lastSavePos.
	 */
	public Point3d getLastSafePos() {
		return lastSafePos;
	}

	@Override
    public String toString() {
	    return getName();
    }
}
