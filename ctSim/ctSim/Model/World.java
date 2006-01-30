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

import ctSim.View.ControlFrame;
import ctSim.View.WorldView;

import java.util.*;

import javax.media.j3d.Node;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PickBounds;
import javax.media.j3d.PickConeRay;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickShape;
import javax.media.j3d.PickRay;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.universe.SimpleUniverse;

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
	public static final float PLAYGROUND_HEIGHT = 2f;

	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_WIDTH = 2f;

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
	
	/** ZeitlupenModus? */
	
	private boolean slowMotion;
	
	/** Zeitlupen-Simulationswert */
	
	private int slowMotionTime = 100;

	/** Interne Zeitbasis in Millisekunden. */
	private long simulTime = 0;

	/**
	 * Drei BranchGroups, eine fuer die ganze Welt, eine für den Boden und die 
	 * letzte fuer die Hindernisse
	 */
	public BranchGroup scene, terrainBG, obstBG;

	/** TransformGroup der gesamten Welt. Hier kommen auch die Bots hinein */
	private TransformGroup worldTG;

	/** Die Klasse SimpleUniverse macht die Handhabung der Welt etwas leichter */
	private SimpleUniverse simpleUniverse;

	/** Erzeugt eine neue Welt */
	public World() {

		bots = new LinkedList();
		obstacles = new LinkedList();
		haveABreak = false;
		
		// Slow Motion by Werner
		slowMotion = false;

		worldView = new WorldView(this);

		simpleUniverse = new SimpleUniverse(worldView.getWorldCanvas());
		scene = createSceneGraph();
		simpleUniverse.addBranchGraph(scene);

		worldView.setUniverse(simpleUniverse);
		worldView.initGUI();

	}

	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * 
	 * @return der Szenegraph
	 */
	public BranchGroup createSceneGraph() {
		// Die Wurzel des Ganzen:
		BranchGroup objRoot = new BranchGroup();

		// PickRotateBehavior pickRotate = null;
		Transform3D transform = new Transform3D();

		transform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		worldTG = new TransformGroup(transform);

		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		worldTG.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		worldTG.setCapability(TransformGroup.ALLOW_PICKABLE_READ);

		worldTG.setPickable(true);

		objRoot.addChild(worldTG);

		// Die Branchgroup für den Boden
		terrainBG = new BranchGroup();
		terrainBG.setPickable(true);
		
		// Der Boden selbst ist ein sehr flacher Quader:
		Box floor = new Box(PLAYGROUND_WIDTH/2, PLAYGROUND_HEIGHT/2, (float) 0.01,
				worldView.getPlaygroundAppear());
		floor.setPickable(true);
		
		// schiebe die Box so, das ihre oberfläche genau auf der 
		// Ebene z = 0 liegt.
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, -0.01d));
		TransformGroup tgb1 = new TransformGroup(translate);
		tgb1.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgb1.setPickable(true);
		tgb1.addChild(floor);
		terrainBG.addChild(tgb1);
		worldTG.addChild(terrainBG);

		// Die TranformGroup fuer alle Hindernisse:
		obstBG = new BranchGroup();
		obstBG.setPickable(true);

		// Die vier Hindernisse:
		Box north = new Box(PLAYGROUND_WIDTH/2 + (float) 0.2, (float) 0.1,
				(float) 0.2, worldView.getObstacleAppear());
		north.setPickable(true);
		north.setName("North");
		Box south = new Box(PLAYGROUND_WIDTH/2 + (float) 0.2, (float) 0.1,
				(float) 0.2, worldView.getObstacleAppear());
		south.setPickable(true);
		south.setName("South");
		Box east = new Box((float) 0.1, PLAYGROUND_HEIGHT/2 + (float) 0.2,
				(float) 0.2, worldView.getObstacleAppear());
		east.setPickable(true);
		east.setName("East");
		Box west = new Box((float) 0.1, PLAYGROUND_HEIGHT/2 + (float) 0.2,
				(float) 0.2, worldView.getObstacleAppear());
		west.setPickable(true);
		west.setName("West");

		// Hindernisse werden an die richtige Position geschoben:

		translate.set(new Vector3f((float) 0, PLAYGROUND_HEIGHT/2 + (float) 0.1,
				(float) 0.2));
		TransformGroup tg1 = new TransformGroup(translate);
		tg1.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg1.setPickable(true);
		tg1.addChild(north);
		obstBG.addChild(tg1);

		translate.set(new Vector3f((float) 0,
				-(PLAYGROUND_HEIGHT/2 + (float) 0.1), (float) 0.2));
		TransformGroup tg2 = new TransformGroup(translate);
		tg2.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg2.setPickable(true);
		tg2.addChild(south);
		//obstBG.addChild(tg2);

		translate.set(new Vector3f(PLAYGROUND_WIDTH/2 + (float) 0.1, (float) 0,
				(float) 0.2));
		TransformGroup tg3 = new TransformGroup(translate);
		tg3.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg3.setPickable(true);
		tg3.addChild(east);
		obstBG.addChild(tg3);

		translate.set(new Vector3f(-(PLAYGROUND_WIDTH/2 + (float) 0.1),
				(float) 0, (float) 0.2));
		TransformGroup tg4 = new TransformGroup(translate);
		tg4.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg4.setPickable(true);
		tg4.addChild(west);
		obstBG.addChild(tg4);

		obstBG.setCapability(Node.ENABLE_PICK_REPORTING);
		obstBG.setCapability(Node.ALLOW_PICKABLE_READ);

		obstBG.compile();

		// Die Hindernisse der Welt hinzufuegen
		worldTG.addChild(obstBG);
		// es duerfen noch weitere dazukommen
		worldTG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		return objRoot;
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
		worldTG.addChild(bot.getBotBG());
	}

	/**
	 * Prueft, ob ein Bot mit irgendeinem anderen Objekt kollidiert
	 * 
	 * @param bounds
	 *            die Grenzen des Bot 
	 * @param newPosition 
	 * 			  die angestrebte neue Position	
	 * @return True wenn der Bot sich frei bewegen kann
	 */
	public synchronized boolean checkCollision(Node botBody, Bounds bounds,
			Vector3f newPosition) {
		// schiebe probehalber Bound an die neue Position
		Transform3D transform = new Transform3D();
		transform.setTranslation(newPosition);
		bounds.transform(transform);

		// und noch die Welttransformation darauf anwenden
		worldTG.getTransform(transform);
		bounds.transform(transform);

		PickBounds pickShape = new PickBounds(bounds);
		
		// Eigenen Körper des Roboters verstecken
		botBody.setPickable(false);
		
		PickInfo pickInfo = obstBG.pickAny(PickInfo.PICK_BOUNDS, PickInfo.NODE,
				pickShape);
		
		// Eigenen Körper des Roboters wieder pickable machen
		botBody.setPickable(true);

		if ((pickInfo == null) || (pickInfo.getNode() == null))
			return true;
		else
			System.out.println("Kollision!");
		return false;
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
	 * @param openingAngle
	 * 			  Der Ã–ffnungswinkel des Blickstrahls           
	 * @return Die Distanz zum naechsten Objekt in Metern
	 */
	public double watchObstacle(Point3d pos, Vector3d heading, double openingAngle) {

		// TODO: Sehstrahl oeffnet einen Konus mit dem festen Winkel von 3 Grad;
		// mus an realen IR-Sensor angepasst werden!

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);

		PickShape pickShape = new PickConeRay(relPos, relHeading,
				openingAngle);
		PickInfo pickInfo = obstBG.pickClosest(PickInfo.PICK_GEOMETRY,
				PickInfo.CLOSEST_DISTANCE, pickShape);

		if (pickInfo == null)
			return 100.0;
		else
			return pickInfo.getClosestDistance();
	}

	/**
	 * Prüft ob unter dem angegebene Punkt innerhalb der Bodenfreiheit 
	 * des Bots noch Boden zu finden ist
	 * 
	 * @param pos
	 *            Die Position von der aus nach unten gemessen wird
	 * @param groundClearance
	 * 			  Die als normal anzusehende Bodenfreiheit   
	 * @param message
	 * 			  Name des Berührungspunktes, welcher getestet wird
	 * @return True wenn Bodenkontakt besteht.
	 */
	public boolean checkTerrain(Point3d pos, double groundClearance, String message) {

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(0d,0d,-1d);
		transform.transform(relHeading);
		
		PickShape pickShape = new PickRay(relPos, relHeading);
		PickInfo pickInfo = terrainBG.pickClosest(PickInfo.PICK_GEOMETRY,
				PickInfo.CLOSEST_DISTANCE, pickShape);
	
		if (pickInfo == null) {
			System.out.println(message + " fällt ins Bodenlose.");
			return false;
		} else if(Math.round(pickInfo.getClosestDistance()*1000) != Math.round(groundClearance*1000)) {
			System.out.println(message + " fällt " + pickInfo.getClosestDistance()*1000 + " mm.");
			return false;	
		} else
			return true;
	}
	
	/**
	 * Gibt Nachricht von aussen, dass sich der Zustand der Welt geaendert hat,
	 * an den View weiter
	 */
	public void reactToChange() {
		worldView.repaint();
	}

	/**
	 * @return Gibt simpleUniverse zurueck.
	 */
	public SimpleUniverse getSimpleUniverse() {
		return simpleUniverse;
	}

	/**
	 * Raumt auf, wenn der Simulator beendet wird
	 * 
	 * @see World#run()
	 */
	protected void cleanup() {
		// Unterbricht alle Bots, die sich dann selbst entfernen
		Iterator it = bots.iterator();
		while (it.hasNext()) {
			Bot curr = (Bot) it.next();
			curr.interrupt();
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
		// Schliesst das Fenster zur Welt:
		worldView.dispose();
		// Unterbricht sich selbst,
		// interrupt ruft cleanup auf:
		this.interrupt();
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
			try {
				// halbe baseTime warten,
				
				sleep(baseTimeReal / 2);
				
			} catch (InterruptedException IEx) {
				cleanup();
			}

			// Ist die Pause-Taste in der GUI gedrueckt worden?
			if (!haveABreak) {

				// Falls nicht, simulierte Zeit erhoehen,
				simulTime += baseTimeVirtual / 2;
				// dann alle Bots benachrichtigen, also
				// alle wartenden Threads wieder wecken:

				synchronized (this) {
					notifyAll();
				}

			}
			try {
				// nochmal halbe baseTime warten,
				sleep(baseTimeReal / 2);
			} catch (InterruptedException IEx) {
				cleanup();
			}

			// Ist die Pause-Taste in der GUI gedrueckt worden?
			if (!haveABreak) {
				// Ansonsten simulierte Zeit erhoehen,
				simulTime += baseTimeVirtual / 2;
			}
			// Auf jeden Fall WorldView benachrichtigen,
			// dass neu gezeichnet werden soll:
			worldView.repaint();

		}
	}

	/**
	 * @param baseTimeReal
	 *            Wert fuer baseTimeReal, der gesetzt werden soll.
	 */
	public void setBaseTimeReal(int baseTimeReal) {

		// wenn slowMotion gesetzt ist schalten wir das mal ab
		if(slowMotion)
			slowMotion = !slowMotion;
			
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

	/**
	 * @return true : Slow Motion on
	 * @author Werner Pirkl (morpheus.the.real@gmx.de)
	 */
	public boolean isSlowMotion() {
		return slowMotion;
	}

	/**
	 * @return true if SlowMotionMode is enabled
	 * @author Werner Pirkl (Morpheus.the.real@gmx.de)
	 */
	public boolean getSlowMotion(){
		return this.slowMotion;
	}
	
	/**
	 * Slow Motion heißt 10 mal so hohe Zeitbasis
	 * @param slowMotion true: Slow Motion on
	 * @author Werner Pirkl (Morpheus.the.real@gmx.de
	 */
	public void setSlowMotion(boolean slowMotion) {
		this.slowMotion = slowMotion;
		if(slowMotion)
		{	
			// in slowMotionTime wird der Wert vom Schieberegler festgehalten
			this.baseTimeReal = baseTimeVirtual * slowMotionTime;
			// die Virtuelle also nach außen sichtbare Zeitbasis bleibt jedoch erhalten da ja
			// nun in 100 ms 10 ms vergehen
			this.baseTimeVirtual = 10;
		}	
		else
		{	
			this.baseTimeReal = 10;
			this.baseTimeVirtual = 10;
		}	
	}

	/**
	 * @return slowMotionTime in ms (Standard: 100ms)
	 * @author Werner Pirkl (morpheus.the.real@gmx.de)
	 */
	public int getSlowMotionTime() {
		return slowMotionTime;
	}

	/**
	 * Setzt slowMotionTime
	 * @param slowMotionTime in ms (Standard: 100ms)
	 * @author Werner Pirkl (morpheus.the.real@gmx.de)
	 */
	public void setSlowMotionTime(int slowMotionTime) {
		if(slowMotion){
			baseTimeReal = baseTimeVirtual * slowMotionTime;
		}	
		this.slowMotionTime = slowMotionTime;
	}
}
