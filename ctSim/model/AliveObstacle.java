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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.ConfigManager;
import ctSim.ErrorHandler;
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
	/** Das Obstacle ist von der weiteren Simulation ausgeschlossen */
	public static final int OBST_STATE_HALTED   = 0x0100;

	private int obstState = OBST_STATE_NORMAL;

	private String name;
	private Point3d pos;

	/** Letzte Position an dem sich das AliveObstacle befand und dabei der obstState ok war.
	 * Also (obstState & OBSTSTATE_SAVE) ==0
	 */
	private Point3d lastSavePos;

	private Vector3d head;

	/** Verweis auf den zugehoerigen Controller */
	// TODO: hmmm
	private DefaultController controller;

	private Thread thrd;

	private BranchGroup branchgrp;
	private TransformGroup transformgrp;
	private Shape3D shape;
	private List<ViewPlatform> views;

	// TODO:
	private HashMap<String, Appearance> apps;

	/** Simultime beim letzten Aufruf */
	// TODO
	private long lastSimulTime = 0;

	/** Zeit zwischen letztem Aufruf von UpdateSimulation und jetzt*/
	// TODO
	private long deltaT = 0;

	protected FmtLogger lg = FmtLogger.getLogger("ctSim.AliveObstacle");

	/**
	 * Der Konstruktor
	 * @param position Position des Objekts
	 * @param heading Blickrichtung des Objekts
	 */
	public AliveObstacle(String name, Point3d position, Vector3d heading) {

		this.name = name;
		this.pos = position;
		this.head = heading;

		this.views = new ArrayList<ViewPlatform>();

		initBG();

		this.setPosition(this.pos);
		this.setHeading(this.head);
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
	public final Shape3D getShape() {

		return this.shape;
	}

	/**
	 * @param shape1 3D-Gestalt, die das Objekt erhalten soll
	 */
	public final void setShape(Shape3D shp) {

		// TODO: Test: Reicht auch einfach "this.shape"-Referenz anzupassen?

		if(this.shape != null)
			this.transformgrp.removeChild(this.shape);

		this.shape = shp;
		this.shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		//this.shape.setName(getName() + " Body");
		this.shape.setPickable(true);
		this.shape.setCapability(Node.ALLOW_PICKABLE_WRITE);

		this.transformgrp.addChild(this.shape);
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

	/**
	 * @param world1 Referenz auf die Welt, die gesetzt werden soll
	 */
//	public void setWorld(World wrld) {
//	}

	/**
	 * @see ctSim.model.Obstacle#getBranchGroup()
	 */
	public final BranchGroup getBranchGroup() {

		return this.branchgrp;
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
	 * @param relTrans
	 */
	public final void addViewingPlatform(Transform3D relTrans) {

		ViewPlatform view = new ViewPlatform();

		this.addBranchComponent(relTrans, view);

		this.views.add(view);
	}

	// TODO: Besser Set oder Iterator/Enumerator (dann nicht mehr veraenderlich?)
	/**
	 * @return Liste der ViewPlatforms
	 */
	public final List<ViewPlatform> getViewingPlatforms() {

		return this.views;
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
	public final void stop() {

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

		return this.pos;
	}

	// TODO: Vorsicht: Heading ist relativ zur Welt!
	/**
	 * @see ctSim.model.Obstacle#getHeading()
	 */
	public final Vector3d getHeading() {

		return this.head;
	}

	/**
	 * @return Die Transformation
	 */
	public final Transform3D getTransform() {

//		Transform3D transform = new Transform3D();
//
//		transform.setTranslation(new Vector3d(this.getPosition()));
//
//		double angle = this.getHeading().angle(new Vector3d(1d, 0d, 0d));
//		if(this.getHeading().y < 0)
//			angle = -angle;
//
//		transform.setRotation(new AxisAngle4d(0d, 0d, 1d, angle));
//
//		return transform;

		return SimUtils.getTransform(this.getPosition(), this.getHeading());
	}

	/**
	 * Drandenken: State setzen vor Aufruf dieser Methode
	 *
	 * @param p
	 *            Die Position, an die der Bot gesetzt werden soll
	 */
	public final synchronized void setPosition(Point3d p) {

		// TODO: synchron ist schoen, aber wird eine Pose �ber die GUI denn ueberhaupt verwendet?
		//synchronized (this) {

			this.pos = p;
			Vector3d vec = new Vector3d(p);

			Transform3D transform = new Transform3D();
			this.transformgrp.getTransform(transform);
			transform.setTranslation(vec);
			this.transformgrp.setTransform(transform);

			if ((obstState & OBST_STATE_SAVE) == 0)
				lastSavePos = new Point3d(p);
		//}
	}

	/**
	 * @see ctSim.model.MovableObstacle#setHeading(javax.vecmath.Vector3d)
	 */
	public final synchronized void setHeading(Vector3d vec) {

		this.head = vec;

		double angle = this.head.angle(new Vector3d(1d, 0d, 0d));
		if (this.head.y < 0)
			angle = -angle;

		Transform3D transform = new Transform3D();
		this.transformgrp.getTransform(transform);
		transform.setRotation(new AxisAngle4d(0d, 0d, 1d, angle));
		this.transformgrp.setTransform(transform);
	}

	// TODO:
//	public final Transform3D getTransform() {
//
//		Transform3D transform = new Transform3D();
//
//		this.transformgrp.getTransform(transform);
//
//		return transform;
//	}


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

		try {
			while (this.thrd == thisThread) {
				// Ein AliveObstacle darf nur dann seine work()-Routine ausführen, wenn es nicht Halted ist
				if ((this.obstState & OBST_STATE_HALTED) == 0)
					work();
				this.controller.waitOnController();
			}
		} catch(InterruptedException ie) {
			ErrorHandler.error("Alive Obstacle \""+this.getName()+"\" interrupted: "+ie);
			ie.printStackTrace();
		}
		Debug.out.println("Alive Obstacle \""+this.getName()+"\" stirbt..."); //$NON-NLS-1$ //$NON-NLS-2$
		// TODO: ???
		//cleanup();


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
	 * Haelt ein Alive-Obstacle an oder gibt es wieder frei.
	 * Ist ein Alive-Obstacle halted, so darf es seine work()-Methode nicht mehr ausfuehren.
	 * Es ist aber ansonsten noch funktionsfaehig und reagiert auch auf den Controller 
	 * @param halt true, wennd as AliveObstacle angehalten werden soll; false, wenn es wieder freigelassen werden soll
	 */
	public final void setHalted(boolean halt){
		if (halt == true) {
			lg.info("AliveObstcale "+getName()+" wird angehalten und darf ab sofort die work()-Methide nicht mehr ausfuehren");
			setObstState(getObstState() | OBST_STATE_HALTED);
		} else {
			lg.info("AliveObstcale "+getName()+" wird reaktiviert und darf ab sofort die work()-Methide wieder ausfuehren");
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
	public Point3d getLastSavePos() {
		return lastSavePos;
	}
}
