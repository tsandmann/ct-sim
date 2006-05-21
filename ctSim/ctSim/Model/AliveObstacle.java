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
package ctSim.Model;

import java.util.HashMap;

import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import ctSim.Controller.Controller;

/**
 * Klasse fuer alle Hindernisse die sich selbst bewegn können
 * 
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public abstract class AliveObstacle extends Thread implements MovableObstacle {
	/** Die Grenzen des Roboters */
	private Bounds bounds = null;
	
	/** Konstanten fuer die Noderefernces */
	public static final String BG = "BG";
	/** Konstanten fuer die Noderefernces */
	public static final String TG = "TG";

	/** Liste mit allen Einsprungspunkten in den Szenegraphen */
	private HashMap<String,SceneGraphObject> nodeMap = new HashMap<String,SceneGraphObject>();
	
	/** Soll die Simulation noch laufen? */
	private boolean run = true;

	/** Zeiger auf die Welt, in der der Bot lebt */
	private World world;
	
	/** Position */
	private Vector3f pos = new Vector3f(0.0f, 0f, getHeight() / 2 + 0.006f);

	/** Verweis auf den zugehoerigen Controller */
	private Controller controller;

	
	/**
	 * @return Die Hoehe des Objektes in Metern
	 */
	abstract public float getHeight();
	
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
	public Vector3f getPos() {
		return pos;
	}
	
	/**
	 * @param pos
	 *            Die Position, an die der Bot gesetzt werden soll
	 */
	public void setPos(Vector3f pos) {
		synchronized (pos) {
			this.pos = pos;
			Vector3f vec = new Vector3f(pos);
			
			Transform3D transform = new Transform3D();
			((TransformGroup)getNodeReference(TG)).getTransform(transform);
			transform.setTranslation(vec);
			((TransformGroup)getNodeReference(TG)).setTransform(transform);
			
		}
	}

	
	/**
	 * Ueberschreibt die run() Methode aus der Klasse Thread und arbeitet drei
	 * Schritte ab: <br/> 1. init() - initialisiert alles <br/> 2. work() - wird
	 * in einer Schleife immer wieder aufgerufen <br/> 3. cleanup() - raeumt auf
	 * <br/> Die Methode die() beendet diese Schleife.
	 * 
	 * @see AliveObstacle#init()
	 * @see AliveObstacle#work()
	 * @see AliveObstacle#cleanup()
	 */
	@Override
	final public void run() {
		init();
		while (run == true) {
			work();
		}
		cleanup();
	}

	/**
	 * Beendet den AliveObstacle-Thread<b>
	 * 
	 * @see AliveObstacle#work()
	 */
	public void die() {
		run = false;
		this.interrupt();
	}

	/**
	 * Hier wird aufgeraeumt, wenn die Lebenszeit des AliveObstacle zuende ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest, das Panel
	 * wird aus dem ControlFrame entfernt
	 * 
	 * @see AliveObstacle#work()
	 */
	protected void cleanup() {
		((BranchGroup)getNodeReference(BG)).detach();
		world.remove(this);
		world = null;
	}

	/**
	 * Erzeugt ein neues Objekt mit einem zugeordneten Controller
	 * @param controller
	 */
	public AliveObstacle(Controller controller) {
		super();
		this.controller =controller;
		world = controller.getWorld();
	 	setName(controller.getNewBotName(getClass().getName()));
		createBranchGroup();
	}

	
	/**
	 * Gibt alle Referenzen auf den Szenegraphen zurück
	 * @return Die Referenzen
	 */
	public HashMap<String,SceneGraphObject> getNodeMap() {
		return nodeMap;
	}

	/** Liefert eine einzelne Referenz auf ein Szenegraph-Objekt
	 * 
	 * @param key
	 * @return Das Objekt
	 */
	public SceneGraphObject getNodeReference(String key){
		if (key.contains(getName()))
			return	 nodeMap.get(key);
		
		return	nodeMap.get(getName()+"_"+key);
	}

	/**
	 * Fuegt eine Referenz ein 
	 * @param key
	 * @param so
	 */
	public void addNodeReference(String key, SceneGraphObject so){
		nodeMap.put(getName()+"_"+key,so);
	}
	
	/**
	 * @return Gibt die Grenzen des Bots zurueck
	 */
	public Bounds getBounds() {
		return (Bounds) bounds.clone();
	}
	
	/**
	 * @param bounds
	 *            Referenz auf die Grenzen des Bots, die gesetzt werden sollen
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}
	/**
	 * @return Gibt Referenz auf die Welt zurueck
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * @param world
	 *            Referenz auf die Welt, die gesetzt werden soll
	 */
	public void setWorld(World world) {
		this.world = world;
	}
	
	/**
	 * @return Gibt eine Referenz auf controller zurueck
	 */
	public Controller getController() {
		return controller;
	}
	
}
