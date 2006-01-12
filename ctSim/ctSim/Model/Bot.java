package ctSim.Model;

import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.AxisAngle4f;
//import javax.vecmath.Point2d;
//import javax.vecmath.Vector2d;
import javax.vecmath.Vector3f;

import ctSim.View.ControlPanel;

/**
 * Superklasse fuer alle Bots, unabhaengig davon, ob sie real 
 * oder simuliert sind.</br> 
 * Die Klasse ist abstract und muss daher erst abgeleitet werden, 
 * um instanziiert werden zu koennen.</br>
 * Der Haupt-Thread kuemmert sich um die eigentliche Simulation 
 * und die Koordination mit dem Zeittakt der Welt. Die Kommunikation
 * z.B. ueber eine TCP/IP-Verbindung muss von den abgeleiteten 
 * Klassen selbst behandelt werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 */

abstract public class Bot extends Thread implements Obstacle{

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
	
	/** Die Transformgruppe der Translation der 
	 * 3D-Repraesentation eines Bots */
	TransformGroup translationGroup;

	/** Die Transformgruppe der Rotation der 
	 * 3D-Repraesentation eines Bots */
	TransformGroup rotationGroup;	
	
	
	/** Position */
	private Vector3f pos = new Vector3f(0.0f, 0f,getHeight()/2+0.006f);

	/**
	 * Blickvektor 
	 */
	private Vector3f heading = new Vector3f(1f, 0f,0f);
	
	/**
	 * Initialisierung des Bots
	 */
	public Bot (){
	}
	
	/**
	 * Der Aufruf dieser Methode sorgt dafuer, dass der Bot 
	 * �ber ein passendes ControlPanel verfuegt.
	 */
	abstract public void providePanel();
	
	/**
	 * Hier muss alles rein, was ausgefuehrt werden soll, 
	 * bevor der Thread ueber work() seine Arbeit aufnimmt
	 * @see AbstractBot#work()
	 */ 
	abstract protected void init();
	
	/**
	 * Hier geschieht die eigentliche Arbeit.
	 * Die Methode darf keine Schleife enthalten!
	 */
	abstract protected void work();
	
	/**
	 * Hier wird aufgeraeumt, wenn die Arbeit beendet ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest,
	 * das Panel wird aus dem ControlFrame entfernt
	 * @see Bot#work()
	 */
	protected void cleanup(){
		world=null;
		panel.remove();
		panel=null;	
	}
	
	/**
	 * Die Hauptschleife des Threads
	 * Die run-Methode arbeitet drei Schritte ab:
	 * 1. init() - initilaisiert alles
	 * 2. work() - wird in einer Schleife immer wieder aufgerufen 
	 * 3. cleanup() - raeumt auf 
	 * Die Methode die() beendet die Schleife 
	 * 
	 * @see AbstractBot#init()
	 * @see AbstractBot#work()
	 * @see AbstractBot#cleanup()
	 */
	final public void run(){
		init();
		while (run ==true){
			work();
		}
		cleanup();
	}
	
	/**
	 * Zugriff auf das ControlPanel
	 * @return ControlPanel panel
	 */
	public ControlPanel getPanel() {
		return panel;
	}

	/**
	 * Setzt Referenz auf das ControlPanel
	 * @param panel The panel to set.
	 */
	public void setPanel(ControlPanel panel) {
		this.panel = panel;
	}
	
	/**
	 * Beendet den Bot-Thread<b>
	 * @see AbstractBot#work()
	 */
	public void die(){
		run=false;
		this.interrupt();
	}
	
	/**
	 * Zugriff auf den Namen des Bots
	 * @return Returns the botName.
	 */
	public String getBotName() {
		return botName;
	}

	/**
	 * Zugriff auf den Namen des Bots
	 * @param botName The botName to set.
	 */
	public void setBotName(String botName) {
		this.botName = botName;
	}

	/**
	 * @return Returns the world.
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * @param world The world to set.
	 */
	public void setWorld(World world) {
		this.world = world;
	}

	/**
	 * @return Returns the botTG.
	 */
	public TransformGroup getTranslationGroup() {
		return translationGroup;
	}

	/**
	 * @param botTG The botTG to set.
	 */
	public void setTranslationGroup(TransformGroup botTG) {
		this.translationGroup = botTG;
	}

	/**
	 * @return Returns the botBG.
	 */
	public BranchGroup getBotBG() {
		return botBG;
	}

	/**
	 * @param botBG The botBG to set.
	 */
	public void setBotBG(BranchGroup botBG) {
		this.botBG = botBG;
	}
	
	/**
	 * @return Returns the pos.
	 */
	public Vector3f getPos() {
			return pos;
		}
	
	/**
	 * @param pos The pos to set.
	 */
	public void setPos(Vector3f pos) {
		synchronized (pos) {
			this.pos = pos;
			
			Vector3f vec = new Vector3f(pos);
//			vec.add(new Vector3f(0.0f,0.0f,getHeight()/2));
			Transform3D transform =  new Transform3D();
			transform.setTranslation(vec);
			translationGroup.setTransform(transform);
		}

	}

	/** 
	 * liefert die Höhe des Bots zurück
	 * @return Höhe in [m] 
	 */
	abstract public float getHeight();
	
	/**
	 * @return Returns the heading.
	 */
	public Vector3f getHeading() {
			return heading;
	}

	/**
	 * @param heading The heading to set.
	 */
	public void setHeading(Vector3f heading) {
		synchronized (heading) {
			this.heading = heading;
			
			float angle= heading.angle(new Vector3f(1f,0f,0f));
			
			// TODO Prüfen, ob Drehrichtung korrekt !!!
			if (heading.y<0)
				angle=-angle;
				
			// ToDO Betrachtung -angle, angle !!!!
			Transform3D transform =  new Transform3D();
			transform.setRotation(new AxisAngle4f(0f,0f,1f,angle));
			rotationGroup.setTransform(transform);
		}
	}

	/**
	 * @return Returns the rotationGroup.
	 */
	public TransformGroup getRotationGroup() {
		return rotationGroup;
	}

	/**
	 * @param rotationGroup The rotationGroup to set.
	 */
	public void setRotationGroup(TransformGroup rotationGroup) {
		this.rotationGroup = rotationGroup;
	}

	/**
	 * Liefert die Bounds des Bots zurück 
	 * @return Bounds, die den Bot umschliessen
	 */
	public Bounds getBounds(){
		return (Bounds)bounds.clone();
	}

	/**
	 * @param bounds The bounds to set.
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}
}
