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

import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4f;
// import javax.vecmath.Point2d;
// import javax.vecmath.Vector2d;
import javax.vecmath.Vector3f;

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

	/** Name des Bots */
	private String botName;

	/** Steuerpanel des Bots */
	private ControlPanel panel;

	/** Zeiger auf die Welt, in der der Bot lebt */
	protected World world;

	/** Die 3D-Repraesentation eines Bots */
	BranchGroup botBG;
	
	/** Koerper des Roboters */
	protected Shape3D botBody;	

	/**
	 * Die Transformgruppe der Translation der 3D-Repraesentation eines Bots
	 */
	TransformGroup translationGroup;

	/**
	 * Die Transformgruppe der Rotation der 3D-Repraesentation eines Bots
	 */
	TransformGroup rotationGroup;

	/** Position */
	private Vector3f pos = new Vector3f(0.0f, 0f, getHeight() / 2 +0.006f);

	/**
	 * Blickvektor
	 */
	private Vector3f heading = new Vector3f(1f, 0f, 0f);

	/**
	 * Initialisierung des Bots
	 */
	public Bot() {
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
			Transform3D transform = new Transform3D();
			transform.setTranslation(vec);
			translationGroup.setTransform(transform);
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
			transform.setRotation(new AxisAngle4f(0f, 0f, 1f, angle));
			rotationGroup.setTransform(transform);
		}
	}

	/**
	 * @return Gibt eine Referenz auf die BranchGroup des Bot zurueck
	 */
	public BranchGroup getBotBG() {
		return botBG;
	}

	/**
	 * @param botBG
	 *            Referenz auf die BranchGroup, die der Bot erhalten soll
	 */
	public void setBotBG(BranchGroup botBG) {
		this.botBG = botBG;
	}

	/**
	 * @return Gibt botName zurueck
	 */
	public String getBotName() {
		return botName;
	}

	/**
	 * @param botName
	 *            String fuer botName, der gesetzt werden soll
	 */
	public void setBotName(String botName) {
		this.botName = botName;
	}

	/**
	 * @return Gibt die Grenzen des Bots zurueck
	 */
	public Bounds getBounds() {
		return (Bounds)bounds.clone();
	}

	/**
	 * @param bounds
	 *            Referenz auf die Grenzen des Bots, die gesetzt werden sollen
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}

	/**
	 * @return Gibt ControlPanel des Bot zurueck
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

	/**
	 * @return Gibt die RotationGroup des Bot zurueck
	 */
	public TransformGroup getRotationGroup() {
		return rotationGroup;
	}

	/**
	 * @param rotationGroup
	 *            Referenz auf die RotationGroup, die fuer den Bot gesetzt
	 *            werden soll
	 */
	public void setRotationGroup(TransformGroup rotationGroup) {
		this.rotationGroup = rotationGroup;
	}

	/**
	 * @return Gibt die TranslationGroup des Bot zurueck
	 */
	public TransformGroup getTranslationGroup() {
		return translationGroup;
	}

	/**
	 * @param translationGroup
	 *            Referenz auf die TranslationGroup, die der Bot erhalten soll
	 */
	public void setTranslationGroup(TransformGroup translationGroup) {
		this.translationGroup = translationGroup;
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

}
