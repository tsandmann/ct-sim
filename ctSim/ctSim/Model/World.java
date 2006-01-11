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

	/** Ein Link auf die Kontroll-Panele */
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

	/** Soll der Thread noch laufen? */
	private boolean run = true;

	/** Soll der Thread kurzzeitig ruhen? */

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
	 * Fuegt einen Bot in die Welt ein
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
	 * Prueft, ob ein Bot mit irgendeinem anderen Objekt kollidiert
	 * 
	 * @param pickShape
	 *            die zu pruefende Form
	 * @return True wenn der Bot sich frei bewegen kann
	 * @see World#playground
	 */
	public synchronized boolean checkCollision(Bounds bounds,
			Vector3f newPosition) {
		// schiebe probehalber Bound an die neue Position
		Transform3D transform = new Transform3D();
		transform.setTranslation(newPosition);
		bounds.transform(transform);

		// und noch die Welttransformation darauf anwenden
		getWorldView().getWorldTG().getTransform(transform);
		bounds.transform(transform);

		PickBounds pickShape = new PickBounds(bounds);
		// TODO: Obwohl nur die Branch Group der Hindernisse obstBG gewaehlt
		// ist, werden auch Kollisionen mit anderen Bots und dem Fussboden
		// gefunden
		// Workaround: Im Moment sind die Bots und der Boden nicht Pickable!
		// Anregungen zu Loesung:
		// @see http://www.lems.brown.edu/~wq/projects/cs252.html
		// @see http://forum.java.sun.com/thread.jspa?threadID=656337&tstart=135
		// @see http://java3d.j3d.org/implementation/collision.html
		// @see http://java3d.j3d.org/tutorials/
		// @see http://code.j3d.org/
		PickInfo pickInfo = worldView.obstBG.pickAny(PickInfo.PICK_BOUNDS,
				PickInfo.NODE, pickShape);

		if ((pickInfo == null) || (pickInfo.getNode() == null))
			return true;
		else
			return false;
	}

	/**
	 * Raumt auf, wenn der Simulator beendet wird
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
	 * Beendet den World-Thread
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
	 * @return Gibt baseTimeReal zurueck.
	 */
	public int getBaseTimeReal() {
		return baseTimeReal;
	}

	/**
	 * @return Gibt baseTimeVirtual zurueck.
	 */
	public int getBaseTimeVirtual() {
		return baseTimeVirtual;
	}

	/**
	 * Liefert die Weltzeit (simulTime) zurueck. Blockiert, bis der naechste
	 * Simualationschritt gekommen ist. Diese Methode dient der Synchronisation
	 * zwischen Bots und Welt
	 * 
	 * @return Die aktuelle Weltzeit in ms
	 * @throws InterruptedException
	 */
	public long getSimulTime() throws InterruptedException {
		synchronized (this) {
			// Alle Threads, die diese Methode aufrufen, werden schlafen gelegt:
			wait();
		}
		return simulTime;
	}

	/**
	 * Ueberschriebene run()-Methode der Oberklasse Thread. Hier geschieht die
	 * Welt-Simulation und vor allem auch die Zeitsynchronisation der
	 * simulierten Bots
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while (run == true) {
			// Ist die Pause-Taste in der GUI gedrueckt worden?
			if (!haveABreak) {
				try {
					// halbe baseTime warten,
					sleep(baseTimeReal / 2);
				} catch (InterruptedException IEx) {
					cleanup();
				}
				// dann simulierte Zeit erhoehen,
				simulTime += baseTimeVirtual / 2;
				// dann alle Bots benachrichtigen, also
				// alle wartenden Threads wieder wecken:
				synchronized (this) {
					notifyAll();
				}

				try {
					// nochmal halbe baseTime warten,
					sleep(baseTimeReal / 2);
				} catch (InterruptedException IEx) {
					cleanup();
				}
				// simulierte Zeit erhoehen,
				simulTime += baseTimeVirtual / 2;
				// dann WorldView benachrichtigen,
				// dass neu gezeichnet werden soll:
				Controller.getWorldView().reactToChange();
			}
		}
	}

	/**
	 * @param baseTimeReal
	 *            Wert fuer baseTimeReal, der gesetzt werden soll.
	 */
	public void setBaseTimeReal(int baseTimeReal) {
		this.baseTimeReal = baseTimeReal;
	}

	/**
	 * @param baseTimeVirtual
	 *            Wert fuer baseTimeVirtual, der gesetzt werden soll.
	 */
	public void setBaseTimeVirtual(int baseTimeVirtual) {
		this.baseTimeVirtual = baseTimeVirtual;
	}

	/**
	 * @param simulTime
	 *            Wert fuer simulTime, der gesetzt werden soll.
	 */
	public void setSimulTime(long simulTime) {
		this.simulTime = simulTime;
	}

	/**
	 * Liefert die Distanz in Metern zum naechesten Objekt zurueck, das man
	 * sieht, wenn man von der uebergebenen Position aus in Richtung des
	 * uebergebenen Vektors schaut.
	 * 
	 * @param pos
	 *            Die Position, von der aus der Seh-Strahl verfolgt wird
	 * @param heading
	 *            Die Blickrichtung
	 * @return Die Distanz zum naechsten Objekt in Metern
	 */
	public double watchObstacle(Point3d pos, Vector3d heading) {

		// TODO: liefert immer nur 0 zurueck!

		// TODO: Sehstrahl oeffnet einen Konus mit dem festen Winkel von 3 Grad;
		// mus an realen IR-Sensor angepasst werden!

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldView.getWorldTG().getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);

		PickShape pickShape = new PickConeRay(relPos, relHeading,
				Math.PI / 180 * 3);
		PickInfo pickInfo = worldView.obstBG.pickClosest(
				PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_DISTANCE, pickShape);

		if (pickInfo == null)
			return 100.0;
		else
			return pickInfo.getClosestDistance();
	}

	/**
	 * @return Gibt bots zurueck.
	 */
	public List getBots() {
		return bots;
	}

	/**
	 * @param bots
	 *            Wert fuer bots, der gesetzt werden soll.
	 */
	public void setBots(List bots) {
		this.bots = bots;
	}

	/**
	 * @return Gibt controlFrame zurueck.
	 */
	public ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @param controlFrame
	 *            Wert fuer controlFrame, der gesetzt werden soll.
	 */
	public void setControlFrame(ControlFrame controlFrame) {
		this.controlFrame = controlFrame;
	}

	/**
	 * @return Gibt obstacles zurueck.
	 */
	public List getObstacles() {
		return obstacles;
	}

	/**
	 * @param obstacles
	 *            Wert fuer obstacles, der gesetzt werden soll.
	 */
	public void setObstacles(List obstacles) {
		this.obstacles = obstacles;
	}

	/**
	 * @return Gibt worldView zurueck.
	 */
	public WorldView getWorldView() {
		return worldView;
	}

	/**
	 * @param worldView
	 *            Wert fuer worldView, der gesetzt werden soll.
	 */
	public void setWorldView(WorldView worldView) {
		this.worldView = worldView;
	}

	/**
	 * @return Gibt haveABreak zurueck.
	 */
	public boolean isHaveABreak() {
		return haveABreak;
	}

	/**
	 * @param haveABreak
	 *            Wert fuer haveABreak, der gesetzt werden soll.
	 */
	public void setHaveABreak(boolean haveABreak) {
		this.haveABreak = haveABreak;
	}
}
