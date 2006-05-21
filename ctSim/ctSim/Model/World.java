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

import ctSim.Controller.Controller;
import ctSim.Model.Bots.Bot;
import ctSim.Model.Scene.SceneLight;
import ctSim.View.ControlFrame;

import java.util.*;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.NodeReferenceTable;
import javax.media.j3d.PickBounds;
import javax.media.j3d.PickConeRay;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickShape;
import javax.media.j3d.PickRay;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.AmbientLight;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Color3f;

/**
 * Welt-Modell, kuemmert sich um die globale Simulation und das Zeitmanagement
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Werner Pirkl (morpheus.the.real@gmx.de)
 */
public class World extends Thread {

	/** Ein Link auf den zugehoerigen Controller */
	private Controller controller;

	/** Ein Link auf die Kontroll-Panele */
	private ControlFrame controlFrame;

	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_THICKNESS = 0f;

	/** Reichweite des Lichtes in m */
	private static final float LIGHT_SOURCE_REACH = 1f;

	/** Zeitbasis in Millisekunden. Realzeit - so oft wird simuliert */
	public int baseTimeReal = 10;

	/**
	 * Zeitbasis in Millisekunden. Virtuelle Zeit - das sieht die Welt pro
	 * Simulationsschritt
	 */
	public int baseTimeVirtual = 10;

	/** Liste mit allen Bots, die in dieser Welt leben */
	private List<AliveObstacle> aliveObstacles;

	/** Liste mit allen Hindernissen, die in dieser Welt stehen */
	private List obstacles;

	/** Soll der Thread noch laufen? */
	private boolean run = true;

	/** Soll der Thread kurzzeitig ruhen? */

	private boolean haveABreak;

	/** Zeitlupenmodus? */

	private boolean slowMotion;

	/** Zeitlupen-Simulationswert */

	private int slowMotionTime = 100;

	/** Interne Zeitbasis in Millisekunden. */
	private long simulTime = 0;

	/*
	 * BranchGroups, eine fuer die ganze Welt (steckt in sceneLight), 
	 * eine fuer den Boden, eine
	 * fuer die Lichtquellen und die letzte fuer die Hindernisse (steckt in sceneLight)
	 */
	
	/**
	 * Eine abgespeckte Version des Szenegraphen, enthaelt die aktive
	 * Arbeitsversion
	 */
	private SceneLight sceneLight;

	/**
	 * Ein noch nicht compiliertes Backup des aktiven SceneLight, wird up to 
	 * date gehalten. Kann zur Darstellung exportiert werden, darf nicht
	 * direkt verwendet (aktiviert) werden, da sonst spaeter kein cloneTree()
	 * mehr moeglich ist
	 */

	private SceneLight sceneLightBackup;

	/**
	 * BranchGroup fuer den Boden
	 */
	public BranchGroup terrainBG;

	/**
	 * BranchGroup fuer die Lichtquellen
	 */
	public BranchGroup lightBG;

	/** TransformGroup der gesamten Welt */
	private TransformGroup worldTG;

	/** Der Parcours in dem sich der Bot bewegt */
	private Parcours parcours;
		
	/** Erzeugt eine neue Welt
	 * @param controller
	 * @param parcoursFile
	 * @throws Exception
	 */
	public World(Controller controller, String parcoursFile) throws Exception {
		super();

		this.controller = controller;
		aliveObstacles = new LinkedList<AliveObstacle>();
		obstacles = new LinkedList();
		haveABreak = false;
		slowMotion = false;
		/* erstelle und sichere die Szene */
		sceneLight = new SceneLight();
		sceneLight.setScene(createSceneGraph(parcoursFile));
		sceneLightBackup = sceneLight.clone();
		sceneLight.getScene().compile();
	}

	
	/**
	 * Prueft, ob ein Punkt auf dem Zielfeld liegt
	 * @param pos Der Punkt
	 * @return true, wenn der Punkt im Zielfeld liegt
	 */
	public boolean finishReached(Vector3f pos){
		return parcours.finishReached(pos);
	}
	
	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * @param parcoursFile Dateinamen des Parcours
	 * @return der Szenegraph
	 * @throws Exception
	 */
	public BranchGroup createSceneGraph(String parcoursFile) throws Exception {

		// Die Wurzel des Ganzen:
		BranchGroup objRoot = new BranchGroup();
		objRoot.setName("paul");
		objRoot.setUserData(new String("paul"));
		
		Transform3D transform = new Transform3D();

		transform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		worldTG = new TransformGroup(transform);

		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		worldTG.setCapability(Node.ENABLE_PICK_REPORTING);
		worldTG.setCapability(Node.ALLOW_PICKABLE_READ);
		worldTG.setPickable(true);

		objRoot.addChild(worldTG);

		// Lichtquellen einfuegen
		// Streulicht (ambient light)
		BoundingSphere ambientLightBounds = new BoundingSphere(new Point3d(0d,
				0d, 0d), 100d);
		Color3f ambientLightColor = new Color3f(0.3f, 0.3f, 0.3f);
		AmbientLight ambientLightNode = new AmbientLight(ambientLightColor);
		ambientLightNode.setInfluencingBounds(ambientLightBounds);
		ambientLightNode.setEnable(true);
		worldTG.addChild(ambientLightNode);

		// Die Branchgroup fuer die Lichtquellen
		lightBG = new BranchGroup();
		lightBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		lightBG.setPickable(true);
		worldTG.addChild(lightBG);

		// Die Branchgroup fuer den Boden
		terrainBG = new BranchGroup();
		terrainBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		terrainBG.setPickable(true);
		worldTG.addChild(terrainBG);

		// Die TranformGroup fuer alle Hindernisse:
		sceneLight.setObstBG(new BranchGroup());
		// Damit spaeter Bots hinzugefuegt werden koennen:
		sceneLight.getObstBG().setCapability(Group.ALLOW_CHILDREN_EXTEND);
		sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_DETACH);
		sceneLight.getObstBG().setCapability(Group.ALLOW_CHILDREN_WRITE);
		sceneLight.getObstBG().setCapability(Node.ALLOW_PICKABLE_WRITE);

		
		// Objekte sind fest
		sceneLight.getObstBG().setPickable(true);

		loadParcours(parcoursFile);

		// Hindernisse werden an die richtige Position geschoben

		
		// Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau
		// buendig mit der Unterkante des Bodens ist:
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup obstTG = new TransformGroup(translate);
		obstTG.setCapability(Node.ENABLE_PICK_REPORTING);
		obstTG.setPickable(true);
		sceneLight.getObstBG().addChild(obstTG);

	    	obstTG.addChild(parcours.getObstBG());
	    	lightBG.addChild(parcours.getLightBG());
	    	terrainBG.addChild(parcours.getTerrainBG());		
		
		sceneLight.getObstBG().setCapability(Node.ENABLE_PICK_REPORTING);
		sceneLight.getObstBG().setCapability(Node.ALLOW_PICKABLE_READ);

		// Die Hindernisse der Welt hinzufuegen
		worldTG.addChild(sceneLight.getObstBG());
		// es duerfen noch weitere dazukommen
		worldTG.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		return objRoot;
	}

	/**
	 * Laedt einen Parcours aus einer XML-Datei
	 * @param parcoursFile
	 * @throws Exception
	 */
	private void loadParcours(String parcoursFile) throws Exception{
		// Den Parcours Laden
		ParcoursLoader pL = new ParcoursLoader();
		pL.load_xml_file(parcoursFile);
		parcours= pL.getParcours();
	}

	/**
	 * Entfernt ein Obstacle wieder
	 * @param obst
	 */
	public void remove(AliveObstacle obst){
		aliveObstacles.remove(obst);
		sceneLight.removeBot(obst.getName());
		sceneLightBackup.removeBot(obst.getName());
	}

	/**
	 * Fuegt ein AliveObstacle wie z.B. einen Bot zur Welt dazu
	 * @param obst
	 */
	public void add(AliveObstacle obst){
		aliveObstacles.add(obst);
		obst.setWorld(this);
		BranchGroup bg = (BranchGroup)obst.getNodeReference(AliveObstacle.BG);
	
		// Erzeuge einen Clone des Bots fuers Backup
		NodeReferenceTable nr = new NodeReferenceTable();
		// das resultat von clone brauchen wir nicht, da alle Infos auch in nr stehen
		bg.cloneTree(nr);
		
		HashMap<String,SceneGraphObject> newMap = new HashMap<String,SceneGraphObject>();
		
		// Alle Referenzen aktualisieren
		Iterator it = obst.getNodeMap().keySet().iterator();
		while (it.hasNext()){
			String key = (String) it.next();
			SceneGraphObject ref = obst.getNodeReference(key);
			newMap.put(key,nr.getNewObjectReference(ref));
		}

		// Benachrichtige den Controller uber neue Bots
		controller.addToView(obst.getName(), newMap);

		// In den Backup fuegen wir den ganzen Bot ein
		sceneLightBackup.addBot(obst.getName(),newMap);
		// in den aktuellen Zweig nur die neuen Referenzen
		sceneLight.addBotRefs(obst.getNodeMap());

		
		bg.cloneTree();
		
		sceneLight.getObstBG().addChild(bg);
	}
	
	/**
	 * Fuegt einen Bot in die Welt ein
	 * 
	 * @param bot
	 *            Der neue Bot
	 */
	
	public void addBot(Bot bot) {
		
		
		
		
		Vector3f pos = parcours.getStartPosition(aliveObstacles.size()+1);
		if (pos != null) {
			pos.z = bot.getPos().z;	// Achtung die Bots stehen etwas ueber der Spielflaeche
			bot.setPos(pos);
		}

		Vector3f head = parcours.getStartHeading(aliveObstacles.size()+1);
		if (head != null) {
			bot.setHeading(head);
		}

		
		add(bot);

		


		
		
	}

	/**
	 * Prueft, ob ein Bot mit irgendeinem anderen Objekt kollidiert
	 * @param botBody Koerpeer des Bots
	 * @param bounds die Grenzen des Bot
	 * @param newPosition die angestrebte neue Position
	 * @param botName Name des Bots
	 * @return True wenn der Bot sich frei bewegen kann
	 */
	public boolean checkCollision(Shape3D botBody, Bounds bounds,
			Vector3f newPosition, String botName) {
		// schiebe probehalber Bounds an die neue Position
		Transform3D transform = new Transform3D();
		transform.setTranslation(newPosition);
		bounds.transform(transform);

		// und noch die Welttransformation darauf anwenden
		worldTG.getTransform(transform);
		bounds.transform(transform);

		PickBounds pickShape = new PickBounds(bounds);
		PickInfo pickInfo;
		synchronized (sceneLight.getObstBG()) {
			// Eigenen Koerper des Roboters verstecken
			botBody.setPickable(false);

			pickInfo = sceneLight.getObstBG().pickAny(PickInfo.PICK_BOUNDS,
					PickInfo.NODE, pickShape);

			// Eigenen Koerper des Roboters wieder "pickable" machen
			botBody.setPickable(true);
		}

		if ((pickInfo == null) || (pickInfo.getNode() == null))
			return true;
		
		System.out.println(botName + " hatte einen Unfall!");
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
	 *            Der Oeffnungswinkel des Blickstrahls
	 * @param botBody
	 *            Der Koerper des Roboter, der anfragt
	 * @return Die Distanz zum naechsten Objekt in Metern
	 */
	public double watchObstacle(Point3d pos, Vector3d heading,
			double openingAngle, Shape3D botBody) {

		// TODO: Sehstrahl oeffnet einen Konus mit dem festen Winkel von 3 Grad;
		// muss an realen IR-Sensor angepasst werden!

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);

		PickShape pickShape = new PickConeRay(relPos, relHeading, openingAngle);
		PickInfo pickInfo;
		synchronized (sceneLight.getObstBG()) {
			botBody.setPickable(false);
			pickInfo = sceneLight.getObstBG().pickClosest(
					PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_DISTANCE,
					pickShape);
			botBody.setPickable(true);
		}
		if (pickInfo == null)
			return 100.0;
		
		return pickInfo.getClosestDistance();
	}

	/**
	 * Prueft, ob unter dem angegebenen Punkt innerhalb der Bodenfreiheit des
	 * Bots noch Boden zu finden ist
	 * 
	 * @param pos
	 *            Die Position, von der aus nach unten gemessen wird
	 * @param groundClearance
	 *            Die als normal anzusehende Bodenfreiheit
	 * @param message
	 *            Name des Beruehrungspunktes, welcher getestet wird
	 * @return True wenn Bodenkontakt besteht.
	 */
	public boolean checkTerrain(Point3d pos, double groundClearance,
			String message) {

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(0d, 0d, -1d);
		transform.transform(relHeading);

		PickShape pickShape = new PickRay(relPos, relHeading);
		PickInfo pickInfo;
		synchronized (terrainBG) {
			pickInfo = terrainBG.pickClosest(PickInfo.PICK_GEOMETRY,
					PickInfo.CLOSEST_DISTANCE, pickShape);
		}
		if (pickInfo == null) {
			System.out.println(message + " faellt ins Bodenlose.");
			return false;
		} else if (Math.round(pickInfo.getClosestDistance() * 1000) > Math
				.round(groundClearance * 1000)) {
			System.out.println(message + " faellt "
					+ pickInfo.getClosestDistance() * 1000 + " mm.");
			return false;
		} else
			return true;
	}

	/**
	 * Liefert eine Angabe, wie viel Licht vom Boden absorbiert wird und den
	 * Linien- bzw. Abgrundsensor nicht mehr erreicht. Je mehr Licht reflektiert
	 * wird, desto niedriger ist der zurueckgegebene Wert. Der Wertebereich
	 * erstreckt sich von 0 (weiss oder maximale Reflexion) bis 1023 (minimale
	 * Reflexion, schwarz oder Loch).
	 * 
	 * Es werden rayCount viele Strahlen gleichmaessig orthogonal zum Heading
	 * in die Szene geschossen.
	 * 
	 * @param pos
	 *            Die Position, an der der Sensor angebracht ist
	 * @param heading
	 *            Die Blickrichtung
	 * @param openingAngle
	 *            Der Oeffnungswinkel des Sensors
	 * @param rayCount
	 *            Es werden rayCount viele Strahlen vom Sensor ausgewertet.
	 * @return Die Menge an Licht, die absorbiert wird, von 1023(100%) bis 0(0%)
	 */
	public short sensGroundReflectionLine(Point3d pos, Vector3d heading,
			double openingAngle, short rayCount) {
		// Sensorposition
		Point3d sensPos = new Point3d(pos);
		// Sensorblickrichtung nach unten
		Vector3d sensHeading = new Vector3d(0d, 0d, -1d);

		// Falls die Welt verschoben wurde:
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(sensPos);
		// oder rotiert:
		transform.transform(sensHeading);

		// Transformationsgruppen, fuer den Sensorsweep
		Transform3D transformX = new Transform3D();

		// Wenn mehr als ein Strahl ausgesendet werden soll, dann taste
		// den Sensorbereich parallel zur Achse des Bots ab.
		// Bei nur einem Strahl schaue in die Mitte.
		if (rayCount > 2) {
			// beginne links aussen
			AxisAngle4d rotationAxisX = new AxisAngle4d(heading,
					openingAngle / 2);
			transformX.set(rotationAxisX);
			transformX.transform(sensHeading);
			// arbeite dich nach rechts vor
			// sende Strahlen in gleichmaessigen Abstaenden aus
			rotationAxisX.set(heading, -(openingAngle / (rayCount - 1)));
			transformX.set(rotationAxisX);
		} else {
			// aendere die Blickrichtung NICHT
			transformX.setIdentity();
		}
		// Variablen und Objekte ausserhalb der Schleife vorbereiten.
		PickRay pickRay = new PickRay();
		PickInfo pickInfo;
		Shape3D shape;
		Color3f color = new Color3f();
		
		// Variable zur Auswertung der Absorption/Reflexion
		float absorption = 0f;
		for (int j = 0; j < rayCount; j++) {
			// PickRay modifizieren
			pickRay.set(sensPos, sensHeading);
			synchronized (terrainBG) {
				// Picking durchfuehren
				pickInfo = terrainBG.pickClosest(PickInfo.PICK_GEOMETRY,
						PickInfo.NODE, pickRay);
			}
			// Boden auswerten
			if (pickInfo == null) {
				// kein Boden = 100% des Lichts wird verschluckt
				absorption += 1;
			} else if (pickInfo.getNode() instanceof Shape3D) {
				shape = (Shape3D) pickInfo.getNode();
				shape.getAppearance().getMaterial().getDiffuseColor(color);
				// Je nach Farbe wird ein Teil des Lichts zurueckgeworfen.
				// Hierzu wird der Durchschnitt der Rot-, Gruen- und
				// Blau-Anteile
				// der Farbe bestimmt.
				absorption += 1 - (color.x + color.y + color.z) / 3;
			}
			// Heading anpassen
			transformX.transform(sensHeading);
		}// Ende for-Schleife
		return (short) (absorption * 1023 / (rayCount));
	}

	/**
	 * Liefert eine Angabe, wie viel Licht vom Boden absorbiert wird und den
	 * Linien- bzw. Abgrundsensor nicht mehr erreicht. Je mehr Licht reflektiert
	 * wird, desto niedriger ist der zurueckgegebene Wert. Der Wertebereich
	 * erstreckt sich von 0 (weiss oder maximale Reflexion) bis 1023 (minimale
	 * Reflexion, schwarz oder Loch).
	 * 
	 * Es werden rayCount viele Strahlen gleichmaessig orthogonal zum Heading in
	 * die Szene geschossen.
	 * 
	 * Es werden rayCount viele Strahlen gleichmaessig in Form eines "+" in in
	 * die Szene Geschossen.
	 * 
	 * @param pos
	 *            Die Position, an der der Sensor angebracht ist
	 * @param heading
	 *            Die Blickrichtung
	 * @param openingAngle
	 *            Der Oeffnungswinkel des Sensors
	 * @param rayCount
	 *            Es werden rayCount viele Strahlen vom Sensor ausgewertet.
	 * @return Die Menge an Licht, die absorbiert wird, von 1023(100%) bis 0(0%)
	 */
	public short sensGroundReflectionCross(Point3d pos, Vector3d heading,
			double openingAngle, short rayCount) {
		double absorption;
		Vector3d xHeading = new Vector3d(heading);
		absorption = sensGroundReflectionLine(pos, heading, openingAngle,
				(short) (rayCount / 2));
		Transform3D rotation = new Transform3D();
		rotation.rotZ(Math.PI / 2);
		rotation.transform(xHeading);
		absorption += sensGroundReflectionLine(pos, xHeading, openingAngle,
				(short) (rayCount / 2));
		return (short) (absorption / 2);
	}

	/**
	 * Liefert die Helligkeit, die auf einen Lichtsensor faellt
	 * 
	 * @param pos
	 *            Die Position des Lichtsensors
	 * @param heading
	 *            Die Blickrichtung des Lichtsensors
	 * @param openingAngle
	 *            Der Oeffnungswinkel des Blickstrahls
	 * @return Die Dunkelheit um den Sensor herum, von 1023(100%) bis 0(0%)
	 */
	public int sensLight(Point3d pos, Vector3d heading, double openingAngle) {

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);

		PickShape pickShape = new PickConeRay(relPos, relHeading,
				openingAngle / 2);
		PickInfo pickInfo;
		synchronized (terrainBG) {
			synchronized (sceneLight.getObstBG()) {
				sceneLight.getObstBG().setPickable(false);
				terrainBG.setPickable(false);
				pickInfo = lightBG.pickClosest(PickInfo.PICK_GEOMETRY,
						PickInfo.CLOSEST_DISTANCE, pickShape);
				sceneLight.getObstBG().setPickable(true);
				terrainBG.setPickable(true);
			}
		}

		if (pickInfo == null)
			return 1023;
		

		int darkness = (int) ((pickInfo.getClosestDistance() / LIGHT_SOURCE_REACH) * 1023);
		if (darkness > 1023)
			darkness = 1023;
		return darkness;
		
	}

	/**
	 * Gibt Nachricht von aussen, dass sich der Zustand der Welt geaendert hat,
	 * an den View weiter
	 */
	public void reactToChange() {
		controller.update();
	}

	/**
	 * Raumt auf, wenn der Simulator beendet wird
	 * 
	 * @see World#run()
	 */
	protected void cleanup() {
		// Unterbricht alle Bots, die sich dann selbst entfernen
		Iterator it = aliveObstacles.iterator();
		while (it.hasNext()) {
			Bot curr = (Bot) it.next();
			curr.interrupt();
		}
		aliveObstacles.clear();
		aliveObstacles = null;
	}

	/**
	 * Beendet den World-Thread
	 * 
	 * @see World#run()
	 */
	public void die() {
		run = false;
		// Schliesst das Fenster zur Welt:
		// controller.die();
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
	 * Liefert die Weltzeit (simulTime) zurueck. Blockiert, nicht
	 * 
	 * @return Die aktuelle Weltzeit in ms
	 */
	public long getSimulTimeUnblocking(){
		return simulTime;
	}
	
	/**
	 * Ueberschriebene run()-Methode der Oberklasse Thread. Hier geschieht die
	 * Welt-Simulation und vor allem auch die Zeitsynchronisation der
	 * simulierten Bots
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
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
			reactToChange();
		}
	}

	/**
	 * @param baseTimeReal
	 *            Wert fuer baseTimeReal, der gesetzt werden soll.
	 */
	public void setBaseTimeReal(int baseTimeReal) {

		// Wenn slowMotion gesetzt ist, diese abschalten
		if (slowMotion)
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
	 * @return Gibt aliveObstacles zurueck.
	 */
	public List getAliveObstacles() {
		return aliveObstacles;
	}

	/**
	 * Befuellt die Liste der Bots
	 * @param aliveObstacles 
	 */
	public void setBots(List<AliveObstacle> aliveObstacles) {
		this.aliveObstacles = aliveObstacles;
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
	 */
	public boolean isSlowMotion() {
		return slowMotion;
	}

	/**
	 * @return true if SlowMotionMode is enabled
	 */
	public boolean getSlowMotion() {
		return this.slowMotion;
	}

	/**
	 * Slow Motion hei�t 10 mal so hohe Zeitbasis
	 * 
	 * @param slowMotion
	 *            true: Slow Motion on
	 */
	public void setSlowMotion(boolean slowMotion) {
		this.slowMotion = slowMotion;
		if (slowMotion) {
			// in slowMotionTime wird der Wert vom Schieberegler festgehalten
			this.baseTimeReal = baseTimeVirtual * slowMotionTime;
			// die virtuelle -- also nach aussen sichtbare -- Zeitbasis bleibt
			// jedoch erhalten, da ja nun praktisch in 100 ms 10 ms vergehen
			this.baseTimeVirtual = 10;
		} else {
			this.baseTimeReal = 10;
			this.baseTimeVirtual = 10;
		}
	}

	/**
	 * @return slowMotionTime in ms (Standard: 100ms)
	 */
	public int getSlowMotionTime() {
		return slowMotionTime;
	}

	/**
	 * Setzt slowMotionTime
	 * 
	 * @param slowMotionTime
	 *            in ms (Standard: 100ms)
	 */
	public void setSlowMotionTime(int slowMotionTime) {
		if (slowMotion) {
			baseTimeReal = baseTimeVirtual * slowMotionTime;
		}
		this.slowMotionTime = slowMotionTime;
	}

	/**
	 * Exportiert den internen Szenegraphen zum darstellen Achtung, das geht nur
	 * solange dieser nicht alive ist
	 * 
	 * @return der Szenegraph
	 */
	public SceneLight exportSceneGraph() {
		// Haben wir schon eine Sicherheitskopie
		if (sceneLightBackup != null) {
			// Wenn ja, dann clone einfach diese
			return sceneLightBackup.clone();
		} 
		return null;
	}

	/**
	 * @return Gibt eine Referenz auf sceneLight zurueck
	 */
	public SceneLight getSceneLight() {
		return sceneLight;
	}

	/**
	 * Liefert das Parcours-Objekt zurueck
	 * @return Der Parcours
	 */
	public Parcours getParcours() {
		return parcours;
	}

	/**
	 * X-Dimension des Spielfldes im mm
	 * @return die X-Dimension
	 */
	public float getPlaygroundDimX() {
		return parcours.getWidth();
	}

	/**
	 * Y-Dimension des Spielfldes im mm
	 * @return die Y-Dimension
	 */
	public float getPlaygroundDimY() {
		return parcours.getHeight();
	}
}
