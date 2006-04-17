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

package ctSim.Model.Bots;

import java.util.HashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4f;
// import javax.vecmath.Point2d;
// import javax.vecmath.Vector2d;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;
import ctSim.Controller.Controller;
import ctSim.Model.Obstacle;
import ctSim.Model.World;
import ctSim.View.ControlPanel;

/**
 * Superklasse fuer alle Bots, unabhaengig davon, ob sie real oder simuliert
 * sind.</br> Die Klasse ist abstrakt und muss daher erst abgeleitet werden, um
 * instanziiert werden zu koennen.</br> Der Haupt-Thread kuemmert sich um die
 * eigentliche Simulation und die Koordination mit dem Zeittakt der Welt. Die
 * Kommunikation z.B. ueber eine TCP/IP-Verbindung muss von den abgeleiteten
 * Klassen selbst behandelt werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */

abstract public class Bot extends Thread implements Obstacle {

	/** Soll die Simulation noch laufen? */
	private boolean run = true;

	/** Die Grenzen des Roboters */
	private Bounds bounds = null;

	/** Name des Bots NUR LESEN!!!! */
	private String botName;

	/** Steuerpanel des Bots */
	private ControlPanel panel;

	/** Verweis auf den zugehoerigen Controller */
	private Controller controller;

	/** Zeiger auf die Welt, in der der Bot lebt */
	protected World world;
	
	// TODO nach Obstacle verschieben
	/** Position */
	private Vector3f pos = new Vector3f(0.0f, 0f, getHeight() / 2 + 0.006f);

	/**
	 * Blickvektor
	 */
	private Vector3f heading = new Vector3f(1f, 0f, 0f);

	/** Liste mit den verschiedenen Aussehen eines Bots */
	private HashMap appearances;

	/** Die 3D-Repraesentation eines Bots */
//	BranchGroup botBG;

	/** Koerper des Roboters */
//	protected Shape3D botBody;

	/**
	 * Die Transformgruppe der 3D-Repraesentation eines Bots
	 */
//	private TransformGroup transformGroup;

	/** Liste mit allen Einsprungspunkten in den Szenegraphen */
	private HashMap nodeMap = new HashMap();

	/** Konstanten fuer die Noderefernces */
	public static final String BOTBODY = "botBody";
	/** Konstanten fuer die Noderefernces */
	public static final String BOTBG = "botBG";
	/** Konstanten fuer die Noderefernces */
	public static final String BOTTG = "botTG";
	
	/**
	 * Initialisierung des Bots
	 * @param controller Verweis auf den zugehoerigen Controller
	 */
	public Bot(Controller controller) {
		super();
		this.controller = controller;
		botName = controller.getNewBotName(getClass().getName());
	}

	/**
	 * Der Aufruf dieser Methode direkt nach dem Erzeugen sorgt dafuer, dass der
	 * Bot ueber ein passendes ControlPanel verfuegt
	 */
	abstract public void providePanel();

	/**
	 * Diese Methode muss alles enthalten, was ausgefuehrt werden soll, bevor
	 * der Thread ueber work() seine Arbeit aufnimmt
	 * 
	 * @see Bot#work()
	 */
	abstract protected void init();

	/**
	 * Diese Methode enthaelt die Routinen, die der Bot waehrend seiner Laufzeit
	 * immer wieder durchfuehrt. Die Methode darf keine Schleife enthalten!
	 */
	abstract protected void work();

	/**
	 * Hier wird aufgeraeumt, wenn die Lebenszeit des Bots zuende ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest, das Panel
	 * wird aus dem ControlFrame entfernt
	 * 
	 * @see Bot#work()
	 */
	protected void cleanup() {
		((BranchGroup)getNodeReference(BOTBG)).detach();
		world.removeBot(this);
		controller.removeBotFromView(botName);
		world = null;
		panel.remove();
		panel = null;
	}

	/**
	 * Ueberschreibt die run() Methode aus der Klasse Thread und arbeitet drei
	 * Schritte ab: <br/> 1. init() - initialisiert alles <br/> 2. work() - wird
	 * in einer Schleife immer wieder aufgerufen <br/> 3. cleanup() - raeumt auf
	 * <br/> Die Methode die() beendet diese Schleife.
	 * 
	 * @see Bot#init()
	 * @see Bot#work()
	 * @see Bot#cleanup()
	 */
	final public void run() {
		init();
		while (run == true) {
			work();
		}
		cleanup();
	}

	/**
	 * Beendet den Bot-Thread<b>
	 * 
	 * @see Bot#work()
	 */
	public void die() {
		run = false;
		this.interrupt();
	}

	/**
	 * @param pos
	 *            Die Position, an die der Bot gesetzt werden soll
	 */
	public void setPos(Vector3f pos) {
		synchronized (pos) {
			this.pos = pos;
			Vector3f vec = new Vector3f(pos);
			//vec.add(new Vector3f(0.0f,0.0f,getHeight()/2+getGroundClearance()));
//			Transform3D transform = new Transform3D();
//			transform.setTranslation(vec);
//			translationGroup.setTransform(transform);
			
			Transform3D transform = new Transform3D();
			((TransformGroup)getNodeReference(BOTTG)).getTransform(transform);
			transform.setTranslation(vec);
			((TransformGroup)getNodeReference(BOTTG)).setTransform(transform);
			
		}
	}

	/**
	 * @return Die Hoehe des Bot in Metern
	 */
	abstract public float getHeight();

	/**
	 * @return Die Hoehe der Grundplatte des Bot ueber dem Boden in Metern
	 */
	abstract public float getGroundClearance();

	/**
	 * @param heading
	 *            Die Blickrichtung des Bot, die gesetzt werden soll
	 */
	public void setHeading(Vector3f heading) {
		synchronized (heading) {
			this.heading = heading;
			float angle = heading.angle(new Vector3f(1f, 0f, 0f));
			if (heading.y < 0)
				angle = -angle;
			Transform3D transform = new Transform3D();

			((TransformGroup)getNodeReference(BOTTG)).getTransform(transform);
			
			transform.setRotation(new AxisAngle4f(0f, 0f, 1f, angle));
			
			((TransformGroup)getNodeReference(BOTTG)).setTransform(transform);
//			rotationGroup.setTransform(transform);
		}
	}

	/**
	 * @return Gibt eine Referenz auf die BranchGroup des Bot zurueck
	 */
//	public BranchGroup getBotBG() {
//		return botBG;
//	}

	/**
	 * @param botBG
	 *            Referenz auf die BranchGroup, die der Bot erhalten soll
	 */
//	public void setBotBG(BranchGroup botBG) {
//		this.botBG = botBG;
//	}

	/**
	 * @return Gibt den Namen des Bot (botName) zurueck
	 */
	public String getBotName() {
		return botName;
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
	 * @return Gibt das ControlPanel des Bot zurueck
	 */
	public ControlPanel getPanel() {
		return panel;
	}

	/**
	 * @param panel
	 *            Referenz auf das ControlPanel, die gesetzt werden soll
	 */
	public void setPanel(ControlPanel panel) {
		this.panel = panel;
	}

//	/**
//	 * @return Gibt die RotationGroup des Bot zurueck
//	 */
//	public TransformGroup getRotationGroup() {
//		return rotationGroup;
//	}
//
//	/**
//	 * @param rotationGroup
//	 *            Referenz auf die RotationGroup, die fuer den Bot gesetzt
//	 *            werden soll
//	 */
//	public void setRotationGroup(TransformGroup rotationGroup) {
//		this.rotationGroup = rotationGroup;
//	}

//	/**
//	 * @return Gibt die TranslationGroup des Bot zurueck
//	 */
//	public TransformGroup getTranslationGroup() {
//		return translationGroup;
//	}

	/**
	 * @param translationGroup
	 *            Referenz auf die TranslationGroup, die der Bot erhalten soll
	 */
//	public void setTranslationGroup(TransformGroup translationGroup) {
//		this.translationGroup = translationGroup;
//	}

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
	 * @return Gibt die Blickrichtung zurueck
	 */
	public Vector3f getHeading() {
		return heading;
	}

	/**
	 * @return Gibt die Position zurueck
	 */
	public Vector3f getPos() {
		return pos;
	}

	/**
	 * @return Gibt eine Referenz auf controller zurueck
	 * @return Gibt den Wert von controller zurueck
	 */
	public Controller getController() {
		return controller;
	}

//	public TransformGroup getTransformGroup() {
//		return transformGroup;
//	}
//
//	public void setTransformGroup(TransformGroup transformGroup) {
//		this.transformGroup = transformGroup;
//	}

	/**
	 * Sucht ein Erscheinungsbild des Bots aus der Liste heraus
	 * @param key
	 * @return
	 */
	private Appearance getAppearance(String key) {
		Appearance app = null;
		if (appearances != null)
			app =(Appearance) appearances.get(key);
		
		return app;
	}

	/** Lagert die Erscheinungsbilder des Bots ein
	 * 
	 * @param appearances
	 */
	public void setAppearances(HashMap appearances) {
		this.appearances = appearances;
		setAppearance("normal");
		//((Shape3D)nodeMap.get(BOTBODY)).setAppearance(getAppearance("normal"));
	}

	/**
	 * Setzt das erscheinungsbild des Bots auf einenWert, zu dem bereits eine Appearance ind er Liste vorliegt
	 * @param key
	 */
	public void setAppearance(String key) {
		Shape3D botBody = (Shape3D) getNodeReference(BOTBODY);
		if (botBody != null)
			botBody.setAppearance(getAppearance(key));
		else
			ErrorHandler.error("Fehler beim Versuch eine Appearance zu setzen: botBody == null");
	}
	
	/**
	 * Gibt alle Referenzen auf den Szenegraphen zurück
	 * @return
	 */
	public HashMap getNodeMap() {
		return nodeMap;
	}
	
	public SceneGraphObject getNodeReference(String key){
		if (key.contains(botName))
			return	(SceneGraphObject) nodeMap.get(key);
		else
			return	(SceneGraphObject) nodeMap.get(botName+"_"+key);
	}

	/** Fuegt eine Referenz ein */
	@SuppressWarnings("unchecked")
	public void addNodeReference(String key, SceneGraphObject so){
		nodeMap.put(botName+"_"+key,so);
	}

	
}
