package ctSim.Model;

import ctSim.Controller.Controller;
import ctSim.View.ControlFrame;
import ctSim.View.WorldView;

import java.util.*;

import javax.media.j3d.Bounds;
import javax.media.j3d.PickBounds;
import javax.media.j3d.PickConeRay;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickShape;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * Welt-Modell, kuemmert sich um die globale Simulation und das Zeitmanagement
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 */
public class World extends Thread {
	/** Ein Link auf die Darstellung der Welt */
	private WorldView worldView;

	/** Ein Link auf den Controller */
	private ControlFrame controlFrame;
	
	/** Hoehe des Spielfelds in m */
	public static final short PLAYGROUND_HEIGHT = 1;

	/** Breite des Spielfelds in m */
	public static final short PLAYGROUND_WIDTH = 1;

	/** Zeitbasis in Millisekunden. Realzeit - So oft wird simuliert */
	public int baseTimeReal = 10;

	/**
	 * Zeitbasis in Millisekunden. Virtuelle Zeit - das sieht die Welt pro
	 * Simulationsschritt
	 */
	public int baseTimeVirtual = 10;

	/** Liste mit allen Bots, die in dieser Welt leben */
	private List bots;

	/** Liste mit allen Hindernissen, die in dieser Welt stehen */
	private List obstacles;

	/** Soll der Thread noch laufen */
	private boolean run = true;

	/** Soll der Thread kurzzeitig ruhen */
	
private boolean haveABreak;
	
	/** Interne Zeitbasis in Millisekunden. */
	private long simulTime = 0;

	/** Erzeugt eine neue Welt */
	public World() {
		bots = new LinkedList();
		obstacles = new LinkedList();
		haveABreak = false;

	}

	/**
	 * Fügt einen Bot in die Welt ein
	 * 
	 * @param bot
	 *            Der neue Bot
	 */
	public void addBot(Bot bot) {
		bots.add(bot);
		bot.setWorld(this);
		
		worldView.addBot(bot.getBotBG());
	}

	/**
	 * Prüft, ob ein Bot mit irgendeinem anderen Objekt kollidiert
	 * 
	 * @param pickShape die zu prüfende Form
	 * @return True wenn der Bot sich frei bewegen kann
	 * @see World#playground
	 */
	public synchronized boolean checkCollision(Bounds bounds, Vector3f newPosition) {
		// Bound an die neue Position
		Transform3D transform = new Transform3D();
		transform.setTranslation(newPosition);
		bounds.transform(transform);
		
		// und noch die Welttransformation drauf
		getWorldView().getWorldTG().getTransform(transform);		
		bounds.transform(transform);
				
		PickBounds pickShape = new PickBounds(bounds);
		// TODO Obwohl die obstBG gewählt ist, findet er auch andere Bots und den Fussboden
		// Das sollte noch gelöst werden. Im Moment sind die Bots und der Boden nich Pickable
        PickInfo pickInfo = worldView.obstBG.pickAny(PickInfo.PICK_BOUNDS,PickInfo.NODE,pickShape);

        if ((pickInfo == null)||(pickInfo.getNode() == null)) 
	    	return true;
        else
        	return false;
        
		// @see http://www.lems.brown.edu/~wq/projects/cs252.html
		// @see http://forum.java.sun.com/thread.jspa?threadID=656337&tstart=135
	    // @see http://java3d.j3d.org/implementation/collision.html
	    // @see http://java3d.j3d.org/tutorials/
	    // @see http://code.j3d.org/
        
	}

	/**
	 * Hier wird aufgeräumt, wenn die Arbeit beendet ist connections auflösen,
	 * Handler zerstören, usw.
	 * 
	 * @see World#run()
	 */
	protected void cleanup() {
		ListIterator list = bots.listIterator();
		while (list.hasNext()) {
			((Bot) list.next()).die();
		}
		bots.clear();
		bots = null;
	}

	/**
	 * Beendet den World-Thread<b>
	 * 
	 * @see World#run()
	 */
	public void die() {
		run = false;
		this.interrupt();
		// TODO Dieing should be done by controller, not by world!!!
		worldView.dispose();
		controlFrame.dispose();
	}

	/**
	 * @return Returns the baseTimeReal.
	 */
	public int getBaseTimeReal() {
		return baseTimeReal;
	}

	/**
	 * @return Returns the baseTimeVirtuell.
	 */
	public int getBaseTimeVirtual() {
		return baseTimeVirtual;
	}

	/**
	 * Liefert die Weltzeit (simulTime) zurück<b> Blockiert, bis der nächste
	 * Simualationschritt gekommen ist.<b> Diese Funktion dient der
	 * Synhronisation zwischen Bots und Welt
	 * 
	 * @return Die aktuelle Weltzeit in ms
	 * @throws InterruptedException
	 */
	public long getSimulTime() throws InterruptedException {
		synchronized (this) {
			wait(); // Send all threads calling this method to sleep
		}
		return simulTime;
	}

	/**
	 * Hier geschieht die Welt-Simulation Und vorallem die Zeitsynchronisation
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (run == true) {
			if (!haveABreak){
			try {
				// Halbe baseTime warten,
				sleep(baseTimeReal / 2);
			} catch (InterruptedException IEx) {
				cleanup();
			}
			// simulierte Zeit erh�hen,
			simulTime += baseTimeVirtual / 2;
			// System.out.println("SimulTime= "+simulTime);
			// dann alle Bots benachrichtigen,
			// alle wartenden Threads wieder wecken:
			synchronized (this) {
				notifyAll();
			}

			try {
				// Nochmal halbe baseTime warten,
				sleep(baseTimeReal / 2);
			} catch (InterruptedException IEx) {
				cleanup();
			}
			// simulierte Zeit erh�hen,
			simulTime += baseTimeVirtual / 2;
			// System.out.println("SimulTime= "+simulTime);
			// dann WorldView benachrichtigen:
			Controller.getWorldView().reactToChange();
			}
		}
	}

	/**
	 * @param baseTimeReal
	 *            The baseTimeReal to set.
	 */
	public void setBaseTimeReal(int baseTimeReal) {
		this.baseTimeReal = baseTimeReal;
	}

	/**
	 * @param baseTimeVirtuell
	 *            The baseTimeVirtuell to set.
	 */
	public void setBaseTimeVirtuell(int baseTimeVirtuell) {
		this.baseTimeVirtual = baseTimeVirtuell;
	}

	/**
	 * @param simulTime
	 *            The simulTime to set.
	 */
	public void setSimulTime(long simulTime) {
		this.simulTime = simulTime;
	}

	/**
	 * Liefert die Distanz in m zum nähesten Objekt zurück, das man sieht,
	 * wenn man von pos aus in Richtung heading schaut.
	 * 
	 * @param pos
	 *            Die Position von der aus der Strahl verfolgt wird
	 * @param heading
	 *            Die Blickrichtung
	 * @return Die Distanz zum nächsten Objekt in m
	 */
	public double watchObstacle(Point3d pos, Vector3d heading) {
		// TODO Check Opening Angle! now fixed 3.0 degrees

		// The world maight be translated
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldView.getWorldTG().getTransform(transform);
		transform.transform(relPos);
		
		// or rotated
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);
		
        PickShape pickShape = new PickConeRay(relPos,relHeading ,Math.PI/180 * 3);
        PickInfo pickInfo = worldView.obstBG.pickClosest(PickInfo.PICK_GEOMETRY,PickInfo.CLOSEST_DISTANCE,pickShape);
	        
	    if (pickInfo == null) 
	    	return 100.0;
	    else 
	    	return pickInfo.getClosestDistance();
	}

	/**
	 * @return Returns the obstacles.
	 */
	public List getObstacles() {
		return obstacles;
	}

	/**
	 * @param obstacles
	 *            The obstacles to set.
	 */
	public void setObstacles(List obstacles) {
		this.obstacles = obstacles;
	}

	/**
	 * @return Returns the bots.
	 */
	public List getBots() {
		return bots;
	}

	/**
	 * @param bots
	 *            The bots to set.
	 */
	public void setBots(List bots) {
		this.bots = bots;
	}

	/**
	 * @return Returns the worldView.
	 */
	public WorldView getWorldView() {
		return worldView;
	}

	/**
	 * @param worldView The worldView to set.
	 */
	public void setWorldView(WorldView worldView) {
		this.worldView = worldView;
	}
	
	/**
	 * @return Returns the controlFrame.
	 */
	public ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @param controlFrame The controlFrame to set.
	 */
	public void setControlFrame(ControlFrame controlFrame) {
		this.controlFrame = controlFrame;
	}

	/**
	 * @return Gibt haveABreak zurueck.
	 */
	public boolean isHaveABreak() {
		return haveABreak;
	}

	/**
	 * @param haveABreak Wert fuer haveABreak, der gesetzt werden soll.
	 */
	public void setHaveABreak(boolean haveABreak) {
		this.haveABreak = haveABreak;
	}
}
