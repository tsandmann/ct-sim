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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.PickBounds;
import javax.media.j3d.PickConeRay;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickRay;
import javax.media.j3d.PickShape;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.xml.sax.SAXException;

import ctSim.model.AliveObstacle;
import ctSim.model.Obstacle;
import ctSim.model.Parcours;
import ctSim.model.ParcoursLoader;
import ctSim.model.bots.Bot;
import ctSim.view.Debug;
//import ctSim.model.scene.SceneLight;

/**
 * Welt-Modell, kuemmert sich um die globale Simulation und das Zeitmanagement
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Werner Pirkl (morpheus.the.real@gmx.de)
 */
public class World {
	
	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_THICKNESS = 0f;
	
	private TransformGroup worldTG;
	
	private BranchGroup lightBG, terrainBG, obstBG;
	
	//private SceneLight sceneLight, sceneLightBackup;
	
	private BranchGroup scene;
	
	private Parcours parcours;
	
	// TODO: joinen?
	//private Set<Obstacle> obsts;
	private Set<AliveObstacle> aliveObsts;
	
	private Set<ViewPlatform> views;
	
	/**
	 * Der Konstruktor
	 */
	public World() {
		
		this.aliveObsts = new HashSet<AliveObstacle>();
		this.views = new HashSet<ViewPlatform>();
		
//		this.sceneLight = new SceneLight();
		
		init();
		
//		this.sceneLightBackup = this.sceneLight.clone();
	}
	
	/**
	 * Alternativer Konstruktor 
	 * @param parcours Der Parcours, den die Welt enthalten soll
	 */
	public World(Parcours parcours) {
		
		this.aliveObsts = new HashSet<AliveObstacle>();
		this.views = new HashSet<ViewPlatform>();
		
		this.parcours = parcours;
		
		//this.sceneLight = new SceneLight();
		
		init();
		setParcours(parcours);
		
		//this.sceneLightBackup = this.sceneLight.clone();
		
		//this.sceneLight.getScene().compile();
	}
	
	/**
	 * @return Gibt eine Referenz auf sceneLight zurueck
	 * @return Gibt den Wert von sceneLight zurueck
	 */
//	public SceneLight getSceneLight() {
//		return this.sceneLight;
//	}
	
	/**
	 * X-Dimension des Spielfldes im mm
	 * @return
	 */
	public float getPlaygroundDimX() {
		return this.parcours.getWidth();
	}

	/**
	 * Y-Dimension des Spielfldes im mm
	 * @return
	 */
	public float getPlaygroundDimY() {
		return this.parcours.getHeight();
	}
	
	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * @param parcoursFile Dateinamen des Parcours
	 * @return der Szenegraph
	 */
	private void setParcours(Parcours parcours) {
		
		this.parcours = parcours;
		// Hindernisse werden an die richtige Position geschoben
		
		// Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau
		// buendig mit der Unterkante des Bodens ist:
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup obstTG = new TransformGroup(translate);
		obstTG.setCapability(Node.ENABLE_PICK_REPORTING);
		obstTG.setPickable(true);
//		this.sceneLight.getObstBG().addChild(obstTG);
		this.obstBG.addChild(obstTG);

	    obstTG.addChild(parcours.getObstBG());
	    this.lightBG.addChild(parcours.getLightBG());
	    this.terrainBG.addChild(parcours.getTerrainBG());		
		
//	    this.sceneLight.getObstBG().setCapability(Node.ENABLE_PICK_REPORTING);
//	    this.sceneLight.getObstBG().setCapability(Node.ALLOW_PICKABLE_READ);
	    this.obstBG.setCapability(Node.ENABLE_PICK_REPORTING);
	    this.obstBG.setCapability(Node.ALLOW_PICKABLE_READ);

		// Die Hindernisse der Welt hinzufuegen
//		this.worldTG.addChild(this.sceneLight.getObstBG());
		// es duerfen noch weitere dazukommen
//		this.worldTG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND); // verschoben nach "init"
	}
	
	private void init() {

		// Die Wurzel des Ganzen:
		this.scene = new BranchGroup();
		this.scene.setName("World"); //$NON-NLS-1$
		this.scene.setUserData(new String("World")); //$NON-NLS-1$
		
		Transform3D worldTransform = new Transform3D();
		worldTransform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		this.worldTG = new TransformGroup(worldTransform);
		
		this.worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		this.worldTG.setCapability(Node.ENABLE_PICK_REPORTING);
		this.worldTG.setCapability(Node.ALLOW_PICKABLE_READ);
		this.worldTG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		this.worldTG.setPickable(true);

		this.scene.addChild(this.worldTG);
		
		// Lichtquellen einfuegen
		// Streulicht (ambient light)
		BoundingSphere ambientLightBounds = 
			new BoundingSphere(new Point3d(0d, 0d, 0d), 100d);
		Color3f ambientLightColor = new Color3f(0.3f, 0.3f, 0.3f);
		AmbientLight ambientLightNode = new AmbientLight(ambientLightColor);
		ambientLightNode.setInfluencingBounds(ambientLightBounds);
		ambientLightNode.setEnable(true);
		this.worldTG.addChild(ambientLightNode);
		
		// Die Branchgroup fuer die Lichtquellen
		this.lightBG = new BranchGroup();
		this.lightBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		this.lightBG.setPickable(true);
		this.worldTG.addChild(this.lightBG);

		// Die Branchgroup fuer den Boden
		this.terrainBG = new BranchGroup();
		this.terrainBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		this.terrainBG.setPickable(true);
		this.worldTG.addChild(this.terrainBG);
		
		// Die TranformGroup fuer alle Hindernisse:
//		this.sceneLight.setObstBG(new BranchGroup());
//		// Damit spaeter Bots hinzugefuegt werden koennen:
//		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
//		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_DETACH);
//		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_PICKABLE_WRITE);
		this.obstBG = new BranchGroup();
		this.obstBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		this.obstBG.setCapability(BranchGroup.ALLOW_DETACH);
		this.obstBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		this.obstBG.setCapability(BranchGroup.ALLOW_PICKABLE_WRITE);
		this.obstBG.setPickable(true);
		this.worldTG.addChild(this.obstBG);

		
		// Objekte sind fest
//		this.sceneLight.getObstBG().setPickable(true);
//		
//		this.sceneLight.setScene(root);
	}
	
	// TODO: Besser weg -> WorldPanel, WorldView ueberarbeiten...
	public BranchGroup getScene() {
		
		return this.scene;
	}
	
	public void addViewPlatform(ViewPlatform view) {
		
		this.views.add(view);
	}
	
	//  TODO:
//	public void addObstacle() {
//		
//		
//	}
	
	// TODO: Joinen mit den anderen adds
	public void addBot(Bot bot) {
		
		// TODO: Hmmm...... woanders hin... Fehlermeldung, wenn keine Pos,Head mehr...
		Point3d pos = new Point3d(parcours.getStartPosition(this.aliveObsts.size()+1));
		if (pos != null) {
			// TODO: Ganz haesslich:
			pos.z = bot.getPosition().z;	// Achtung die Bots stehen etwas ueber der Spielflaeche
			bot.setPosition(pos);
		}

		Vector3d head = new Vector3d(parcours.getStartHeading(this.aliveObsts.size()+1));
		if (head != null) {
			bot.setHeading(head);
		}
		
		this.addAliveObstacle(bot);
	}
	
	public void addAliveObstacle(AliveObstacle obst) {
		
		// TODO: mit addObst joinen, bzw. wenigstens verwenden
		this.aliveObsts.add(obst);
		this.views.addAll(obst.getViewingPlatforms());
		
		this.obstBG.addChild(obst.getBranchGroup());
	}
	
	public Set<AliveObstacle> getAliveObstacles() {
		
		return this.aliveObsts;
	}
	
	// TODO:
//	public void removeObstacle() {
//		
//		
//	}
	
	public void removeAliveObstacle(AliveObstacle obst) {
		
		// TODO: mit remObst joinen, bzw. wenigstens verwenden
		this.aliveObsts.remove(obst);
		this.views.removeAll(obst.getViewingPlatforms());
		
		this.obstBG.removeChild(obst.getBranchGroup());
	}
	
	/**
	 * Entfernt einen Bot wieder
	 * @param bot
	 */
//	public void remove(AliveObstacle obst){
//		aliveObstacles.remove(obst);
//		sceneLight.removeBot(obst.getName());
//		sceneLightBackup.removeBot(obst.getName());
//	}
	
	public static World parseWorldFile(File file)
			throws SAXException, IOException {
		
		ParcoursLoader pL = new ParcoursLoader();
		pL.load_xml_file(file.getAbsolutePath());
		Parcours parcours = pL.getParcours();
		
		return new World(parcours);
	}
	
	/* **********************************************************************
	 * **********************************************************************
	 * "Geerbte" Zeit-Sachen...
	 * 
	 * TODO: Auslagern in Controller?
	 * 
	 */
	/** Zeitbasis in Millisekunden. Realzeit - so oft wird simuliert */
	public int baseTimeReal = 10;
	
	/**
	 * Zeitbasis in Millisekunden. Virtuelle Zeit - das sieht die Welt pro
	 * Simulationsschritt
	 */
	public int baseTimeVirtual = 10;
	
	/** Interne Zeitbasis in Millisekunden. */
	private long simulTime = 0;
	
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
	 * @Throws InterruptedException
	 */
	public long getSimulTime() {
		return simulTime;
	}
	
	public long increaseSimulTime() {
		
		this.simulTime += this.baseTimeVirtual;
		
		return this.simulTime;
	}
	
	/* **********************************************************************
	 * **********************************************************************
	 * WORLD_FUNCTIONS
	 * 
	 * TODO: Auslagern in eigene Klasse?
	 * 
	 * Funktionen fuer die Sensoren usw. (Abstandsfunktionen u.ae.)
	 * 
	 */
	/** Reichweite des Lichtes in m */
	private static final float LIGHT_SOURCE_REACH = 1f;
	
	/**
	 * Prueft, ob ein Punkt auf dem Zielfeld liegt
	 * @return
	 */
	public boolean finishReached(Vector3d pos){
		return parcours.finishReached(pos);
	}
	
	/**
	 * Prueft, ob ein Bot mit irgendeinem anderen Objekt kollidiert
	 * 
	 * @param bounds
	 *            die Grenzen des Bot
	 * @param newPosition
	 *            die angestrebte neue Position
	 * @return True wenn der Bot sich frei bewegen kann
	 */
	// TODO: Ueberarbeiten?
	public boolean checkCollision(AliveObstacle obst, /*Shape3D botBody,*/ Bounds bounds,
			Vector3d newPosition) { //, String botName) {
		
		Shape3D botBody = obst.getShape();
//		Bounds bounds = obst.getBounds();
		String botName = obst.getName();
		
//		System.out.println(bounds.toString());
		
		// schiebe probehalber Bounds an die neue Position
		Transform3D transform = new Transform3D();
		transform.setTranslation(newPosition);
		bounds.transform(transform);
		
//		System.out.println(bounds.toString());
		
		// und noch die Welttransformation darauf anwenden
		worldTG.getTransform(transform);
		bounds.transform(transform);
		
//		System.out.println(bounds.toString());
		
		PickBounds pickShape = new PickBounds(bounds);
		PickInfo pickInfo;
		synchronized (this.obstBG) {
			// Eigenen Koerper des Roboters verstecken
			botBody.setPickable(false);

			pickInfo = this.obstBG.pickAny(PickInfo.PICK_BOUNDS,
					PickInfo.NODE, pickShape);

			// Eigenen Koerper des Roboters wieder "pickable" machen
			botBody.setPickable(true);
		}

		if ((pickInfo == null) || (pickInfo.getNode() == null))
			return true;
		else {
			//System.out.println(botName + " hatte einen Unfall!");
			Debug.out.println("Bot \""+botName + "\" hatte einen Unfall!");
			//obst.stop();
			return false;
		}
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
	// TODO: Ueberarbeiten... (GroundClearance?)
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
			Debug.out.println(message + " faellt ins Bodenlose.");
			return false;
		} else if (Math.round(pickInfo.getClosestDistance() * 1000) > Math
				.round(groundClearance * 1000)) {
			Debug.out.println(message + " faellt "
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
	// TODO: Ueberarbeiten?
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
			synchronized (this.obstBG) {
				this.obstBG.setPickable(false);
				terrainBG.setPickable(false);
				pickInfo = lightBG.pickClosest(PickInfo.PICK_GEOMETRY,
						PickInfo.CLOSEST_DISTANCE, pickShape);
				this.obstBG.setPickable(true);
				terrainBG.setPickable(true);
			}
		}
		if (pickInfo == null)
			return 1023;
		else {
			int darkness = (int) ((pickInfo.getClosestDistance() / LIGHT_SOURCE_REACH) * 1023);
			if (darkness > 1023)
				darkness = 1023;
			return darkness;
		}
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
ss
	 * @param botBody
	 *            Der Koerper des Roboter, der anfragt
	 * @return Die Distanz zum naechsten Objekt in Metern
	 */
	// TODO: Ueberarbeiten?
	public double watchObstacle(Point3d pos, Vector3d heading,
			double openingAngle, Shape3D botBody) {

		// TODO: Sehstrahl oeffnet einen Konus mit dem festen Winkel von 3 Grad;
		// muss an realen IR-Sensor angepasst werden!
//		System.out.println("--------------------------------------------------");
		// TODO: Wieder rein??
		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
//		System.out.println(Math.floor(relPos.x*1000)+" | "+Math.floor(relPos.y*1000)+" | "+Math.floor(relPos.z*1000));
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);
//		System.out.println(Math.floor(relPos.x*1000)+" | "+Math.floor(relPos.y*1000)+" | "+Math.floor(relPos.z*1000));

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
//		System.out.println(Math.floor(relHeading.angle(new Vector3d(0d, 1d, 0d))*100));
		transform.transform(relHeading);
//		System.out.println(Math.floor(relHeading.angle(new Vector3d(0d, 1d, 0d))*100));
		
//		Point3d relPos = pos;
//		Vector3d relHeading = heading;

		PickShape pickShape = new PickConeRay(relPos, relHeading, openingAngle);
		PickInfo pickInfo;
		synchronized (this.obstBG) {
			botBody.setPickable(false);
			pickInfo = this.obstBG.pickClosest(
					PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_DISTANCE,
					pickShape);
			botBody.setPickable(true);
		}
		if (pickInfo == null)
			return 100.0;
		else {
			double d = pickInfo.getClosestDistance();
//			System.out.println("IR: "+Math.floor(d*1000));
			return d;
		}
	}
}
