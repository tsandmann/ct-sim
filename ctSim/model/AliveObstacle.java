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
import java.util.Iterator;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.Cylinder;

import ctSim.SimUtils;
import ctSim.controller.Controller;

/**
 * Klasse fuer alle Hindernisse die sich selbst bewegen koennen
 * 
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public abstract class AliveObstacle 
		implements MovableObstacle, Runnable {
	
	private int obstState = OBST_STATE_NORMAL;
	
	/** Die Grenzen des Roboters */
//	private Bounds bounds;
	
	/** Konstanten fuer die Noderefernces */
//	public static final String BG = "BG";
//	/** Konstanten fuer die Noderefernces */
//	public static final String TG = "TG";

	/** Liste mit allen Einsprungspunkten in den Szenegraphen */
//	private HashMap<String,SceneGraphObject> nodeMap = new HashMap<String,SceneGraphObject>();
	
	/** Soll die Simulation noch laufen? */
//	private boolean run = true;
	
	/** Position */
	//private Vector3d pos = new Vector3d(0.0d, 0d, getHeight() / 2 + 0.006d);
	private Point3d pos;
	private Vector3d head;

	/** Zeiger auf die Welt, in der der Bot lebt */
	// TODO: weg?
	private World world;
	
	/** Verweis auf den zugehoerigen Controller */
	// TODO: hmmm
	private Controller controller;
	
	private Thread thrd;
	
	private BranchGroup branchgrp;
	private TransformGroup transformgrp;
	private Shape3D shape;
	private List<ViewPlatform> views;
	
//	public AliveObstacle() {
//		
//		this.pos = new Point3d();
//		this.head = new Vector3d();
//		
//		this.views = new ArrayList<ViewPlatform>();
//		
//		initBG();
//	}
	
	/**
	 * Der Konstruktor
	 * @param position Position des Objekts
	 * @param heading Blickrichtung des Objekts
	 */
	public AliveObstacle(Point3d position, Vector3d heading) {
		
		this.pos = position;
		this.head = heading;
		
		this.views = new ArrayList<ViewPlatform>();
		
		initBG();
		
		this.setPosition(this.pos);
		this.setHeading(this.head);
	}
	
	private void initBG() {
		
		// Translationsgruppe fuer das Obst
		//TransformGroup tg = new TransformGroup();
		//Transform3D transform = new Transform3D();
		//tg = new TransformGroup(transform);
		
		this.transformgrp = new TransformGroup();
		this.transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		this.transformgrp.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		
		// Referenz im Bot ablegen
//		addNodeReference(BOTBODY,realBot);
			
		// Die Grenzen (Bounds) des Bots sind wichtig
		// fuer die Kollisionserkennung.
		// Die Grenze des Roboters wird vorlaefig definiert ueber
		// eine Sphaere mit Radius der Bot-Grundplatte um die Position des Bot
//		setBounds(new BoundingSphere(new Point3d(super.getPos()), BOT_RADIUS));

		// Jetzt wird noch alles nett verpackt
		this.branchgrp = new BranchGroup();
		this.branchgrp.setCapability(BranchGroup.ALLOW_DETACH);
		this.branchgrp.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		this.branchgrp.addChild(this.transformgrp);

		// Referenz im Bot ablegen
//		addNodeReference(TG,tg);
//		addNodeReference(BG,bg);
	}
	
	/**
	 * @return Die 3D-Gestalt des Objekts
	 */
	public final Shape3D getShape() {
		
		return this.shape;
	}
	
	/**
	 * @param shape 3D-Gestalt, die das Objekt erhalten soll
	 */
	public final void setShape(Shape3D shape) {
		
		// TODO: Test: Reicht auch einfach "this.shape"-Referenz anzupassen?
		
		if(this.shape != null)
			this.transformgrp.removeChild(this.shape);
		
		this.shape = shape;
		this.shape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		//this.shape.setName(getName() + " Body");
		this.shape.setPickable(true);
		this.shape.setCapability(Cylinder.ALLOW_PICKABLE_WRITE);
		
		this.transformgrp.addChild(this.shape);
	}
	
	// TODO: Altlast: anders lösen...
	public final void setAppearances(HashMap<String, Appearance> map) {
		
		if(map.isEmpty())
			return;
		
		Iterator<Appearance> it = map.values().iterator();
		
		this.shape.setAppearance(it.next());
	}
	
	/**
	 * @param ctrl Referenz auf den Controller, die gesetzt werden soll
	 */
	public final void setController(Controller ctrl) {
		
		this.controller = ctrl;
	}
	
	/**
	 * @param world Referenz auf die Welt, die gesetzt werden soll
	 */
	public void setWorld(World world) {
		this.world = world;
	}
	
	/* (non-Javadoc)
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
	
	// TODO: Besser Set oder Iterator/Enumerator (dann nicht mehr veränderlich?)
	/**
	 * @return
	 */
	public final List<ViewPlatform> getViewingPlatforms() {
		
		return this.views;
	}
	
	/**
	 * Startet den Bot (bzw. dessen Thread).
	 */
	public final void start() {
		
		this.thrd = new Thread(this, this.getName());
		
		this.thrd.start();
	}
	
	/**
	 *  Stoppt den Bot (bzw. dessen Thread).
	 */
	public final void stop() {
		
		Thread dummy = this.thrd;
		this.thrd = null;
		dummy.interrupt();
	}
	
	/**
	 * @return Gibt den Namen des Objektes zurück.  
	 */
	abstract public String getName();
	
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
	
	// TODO: Vorischt: Heading ist relativ zur Welt!
	/* (non-Javadoc)
	 * @see ctSim.model.Obstacle#getHeading()
	 */
	public final Vector3d getHeading() {
		
		return this.head;
	}
	
	/**
	 * @return
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
	 * @param pos
	 *            Die Position, an die der Bot gesetzt werden soll
	 */
	public final synchronized void setPosition(Point3d pos) {
		
		// TODO: synchron ist schön, aber wird eine Pose über die GUI denn überhaupt verwendet?
		//synchronized (this) {
			
			this.pos = pos;
			Vector3d vec = new Vector3d(pos);
			
			Transform3D transform = new Transform3D();
			this.transformgrp.getTransform(transform);
			transform.setTranslation(vec);
			this.transformgrp.setTransform(transform);
		//}
	}
	
	/* (non-Javadoc)
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
				work();
				this.controller.waitOnController();
			}
		} catch(InterruptedException ie) {
			// nothing...
		}
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

	/**
	 * Erzeugt ein neues Objekt mit einem zugeordneten Controller
	 * @param controller
	 */
//	public AliveObstacle(Controller controller) {
//		super();
//		this.controller =controller;
//		world = controller.getWorld();
//	 	setName(controller.getNewBotName(getClass().getName()));
//		createBranchGroup();
//	}
//	public AliveObstacle() {
//		world = controller.getWorld();
//	 	setName(controller.getNewBotName(getClass().getName()));
//		createBranchGroup();
//	}

	
	/**
	 * Gibt alle Referenzen auf den Szenegraphen zurÃ¼ck
	 * @return
	 */
//	public HashMap<String,SceneGraphObject> getNodeMap() {
//		return nodeMap;
//	}

	/** Liefert eine einzelne Referenz auf ein Szenegraph-Objekt
	 * 
	 * @param key
	 * @return
	 */
//	public SceneGraphObject getNodeReference(String key){
//		if (key.contains(getName()))
//			return	(SceneGraphObject) nodeMap.get(key);
//		else
//			return	(SceneGraphObject) nodeMap.get(getName()+"_"+key);
//	}

	/** Fuegt eine Referenz ein */
//	public void addNodeReference(String key, SceneGraphObject so){
//		nodeMap.put(getName()+"_"+key,so);
//	}
	
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
	 * @return Gibt Referenz auf die Welt zurueck
	 */
//	public World getWorld() {
//		return world;
//	}

	/**
	 * @param world
	 *            Referenz auf die Welt, die gesetzt werden soll
	 */
//	public void setWorld(World world) {
//		this.world = world;
//	}
	
	/**
	 * @return Gibt eine Referenz auf controller zurueck
	 * @return Gibt den Wert von controller zurueck
	 */
//	public Controller getController() {
//		return controller;
//	}
	
	/**
	 * Liefert den Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 * 
	 * @return Der Zustand des Objekts
	 */
	public int getObstState() {
		return obstState;
	}

	/**
	 * Setztden Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 * 
	 * @param state Der Zustand, der gesetzt werden soll
	 */
	public void setObstState(int state) {
		this.obstState = state;
	}
}
