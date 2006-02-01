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

import javax.media.j3d.PointLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Node;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PickBounds;
import javax.media.j3d.PickConeRay;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickShape;
import javax.media.j3d.PickRay;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.AmbientLight;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Color3f;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.universe.SimpleUniverse;

/**
 * Welt-Modell, kuemmert sich um die globale Simulation und das Zeitmanagement
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */
public class World extends Thread {
	/** Ein Link auf die Darstellung der Welt */
	private WorldView worldView;

	/** Ein Link auf die Kontroll-Panele */
	private ControlFrame controlFrame;

	/** Hoehe des Spielfelds in m */
	public static final float PLAYGROUND_HEIGHT = 4f;

	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_WIDTH = 2f;
	
	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_THICKNESS = 0f;
	
	/** Reichweite des Lichtes in m */
	private static final float LIGHT_SOURCE_REACH = 4f;

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

	/*
	 * Vier BranchGroups, eine fuer die ganze Welt, eine f¸r den Boden, 
	 * eine f¸r die Lichtquellen und die letzte fuer die Hindernisse
	 */
	/**
	 * BranchGroup fuer die ganze Welt
	 */
	public BranchGroup scene;
	/**
	 * BranchGroup fuer den Boden
	 */ 
	public BranchGroup terrainBG;
	/**
	 * BranchGroup fuer die Hindernisse
	 */
	public BranchGroup obstBG;
	/**
	 * BranchGroup fuer die Lichtquellen
	 */
	public BranchGroup lightBG;

	
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
	 * Baut die flache 3D-Representation des Terrains 2D Polygonen zusammen
	 *  
	 * @return der Boden
	 */
	private Shape3D createTerrainShape() {
		
		Shape3D ts = new Shape3D();
		// Anzahl der verwendeten Punkte
		int N = 10;
		int totalN = N;
		// data muss pro Punkt die Werte x, y und z speichern
		float[] data = new float[totalN * 3];
		// zwei Polygone(Deckel und Boden) mit N Ecken
		int stripCounts[] = {N};
		// Z‰hler
		int n = 0;
	
		// Boden erzeugen
		//
		// Umriss des Bodens erzeugen, beachte dabei die ÷ffnung
		//1.unten links
		data[n++] = -PLAYGROUND_WIDTH/2;
		data[n++] = -PLAYGROUND_HEIGHT/2;
		data[n++] = 0f;
		//2.unten rechts
		data[n++] = PLAYGROUND_WIDTH/2;
		data[n++] = -PLAYGROUND_HEIGHT/2;
		data[n++] = 0f;
		//3.oben rechts
		data[n++] = PLAYGROUND_WIDTH/2;
		data[n++] = PLAYGROUND_HEIGHT/2;
		data[n++] = 0f;
		//4.oben links
		data[n++] = -PLAYGROUND_WIDTH/2;
		data[n++] = PLAYGROUND_HEIGHT/2;
		data[n++] = 0f;
		//5.R2m  links
		data[n++] = -PLAYGROUND_WIDTH/2;
		data[n++] = -1.0f;
		data[n++] = 0f;
		//6.R2 Loch  OR
		data[n++] = -0.3f;
		data[n++] = -1.0f;
		data[n++] = 0f;
		//7.R2 Loch  UR
		data[n++] = -0.3f;
		data[n++] = -1.5f;
		data[n++] = 0f;
		//8.R2 Loch  UL
		data[n++] = -0.8f;
		data[n++] = -1.5f;
		data[n++] = 0f;
		//9.R2 Loch  OL
		data[n++] = -0.8f;
		data[n++] = -1.0f;
		data[n++] = 0f;
		//10.R2m  links
		data[n++] = -PLAYGROUND_WIDTH/2;
		data[n++] = -1.0f;
		data[n++] = 0f;

		// Polygone in darstellbare Form umwandeln
		GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
		gi.setCoordinates(data);
		gi.setStripCounts(stripCounts);
		
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		gi.recomputeIndices();
		
		Stripifier st = new Stripifier();
		st.stripify(gi);
		gi.recomputeIndices();
		
		// Hinzuf¸gen der Ober- und Unterseite derTerrain-Shape3D 
		ts.addGeometry(gi.getGeometryArray());
	
		// Die folgenden Zeilen f¸hren dazu, das die H¸lle des
		// Bots durchsichtig wird und nur die Wireframe gezeichnet
		// wird. Weiterhin werden auch die R¸ckseiten gezeichnet.
		
//		 PolygonAttributes polyAppear = new PolygonAttributes();
//		 polyAppear.setPolygonMode(PolygonAttributes.POLYGON_LINE);
//		 polyAppear.setCullFace(PolygonAttributes.CULL_NONE);
//		 Appearance twistAppear = new Appearance();
//		 twistAppear.setPolygonAttributes(polyAppear);
//		 ts.setAppearance(twistAppear);
		
		return ts;
	}
	/**
	 * Baut die Linien 3D-Representation f¸r den Boden aus 2D Polygonen zusammen
	 *  
	 * @return der Boden
	 */
	private Shape3D createLineShape() {
		
		Shape3D ls = new Shape3D();
		// Anzahl der verwendeten Punkte
		int N = 10;
		int totalN = N;
		// data muss pro Punkt die Werte x, y und z speichern
		float[] data = new float[totalN * 3];
		// zwei Polygone(Deckel und Boden) mit N Ecken
		int stripCounts[] = {N};
		// Z‰hler
		int n = 0;
	
		// Linien erzeugen
		//11
		data[n++] = -0.71f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		//12
		data[n++] = 0.71f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		//13
		data[n++] = 0.71f;
		data[n++] = 1.71f;
		data[n++] = 0f;
		//14
		data[n++] = -0.71f;
		data[n++] = 1.71f;
		data[n++] = 0f;
		//15
		data[n++] = -0.71f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		//16
		data[n++] = -0.7f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		//17
		data[n++] = -0.7f;
		data[n++] = 1.7f;
		data[n++] = 0f;
		//18
		data[n++] = 0.7f;
		data[n++] = 1.7f;
		data[n++] = 0f;
		//19
		data[n++] = 0.7f;
		data[n++] = -1.7f;
		data[n++] = 0f;
		//20
		data[n++] = -0.71f;
		data[n++] = -1.7f;
		data[n++] = 0f;

		// Polygone in darstellbare Form umwandeln
		GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
		gi.setCoordinates(data);
		gi.setStripCounts(stripCounts);
		
		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		gi.recomputeIndices();
		
		Stripifier st = new Stripifier();
		st.stripify(gi);
		gi.recomputeIndices();
		
		// Hinzuf¸gen der Ober- und Unterseite derTerrain-Shape3D 
		ls.addGeometry(gi.getGeometryArray());
	
		// Die folgenden Zeilen f¸hren dazu, das die H¸lle des
		// Bots durchsichtig wird und nur die Wireframe gezeichnet
		// wird. Weiterhin werden auch die R¸ckseiten gezeichnet.
		
//		 PolygonAttributes polyAppear = new PolygonAttributes();
//		 polyAppear.setPolygonMode(PolygonAttributes.POLYGON_LINE);
//		 polyAppear.setCullFace(PolygonAttributes.CULL_NONE);
//		 Appearance twistAppear = new Appearance();
//		 twistAppear.setPolygonAttributes(polyAppear);
//		 ts.setAppearance(twistAppear);
		
		return ls;
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
		
		// Lichtquellen einf¸gen
		// Ambient light
		BoundingSphere ambientLightBounds = new BoundingSphere(new Point3d(0d,0d,0d),100d);
    	Color3f ambientLightColor = new Color3f (0.1f, 0.1f, 0.1f);
    	AmbientLight ambientLightNode = new AmbientLight (ambientLightColor);
    	ambientLightNode.setInfluencingBounds (ambientLightBounds);
    	worldTG.addChild (ambientLightNode);
    	
    	// Die Branchgroup f¸r die Lichtquellen
    	lightBG = new BranchGroup();
    	lightBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
    	lightBG.setPickable(true);
    	
    	// Lichtpunkte
    	BoundingSphere pointLightBounds = new BoundingSphere(new Point3d(0d,0d,0d),100d);
		Color3f pointLightColor = new Color3f (1.0f, 1.0f, 0.9f);
    	
		PointLight pointLight1  = new PointLight();
    	pointLight1.setColor(pointLightColor);
    	pointLight1.setPosition(PLAYGROUND_WIDTH/2,PLAYGROUND_HEIGHT/2,1.5f);
    	pointLight1.setInfluencingBounds(pointLightBounds);
    	pointLight1.setAttenuation(1.7f,0f,0f);
    	lightBG.addChild (pointLight1);
    	Transform3D lsTranslate = new Transform3D();
    	lsTranslate.set(new Vector3f(PLAYGROUND_WIDTH/2,PLAYGROUND_HEIGHT/2,1.5f));
    	TransformGroup lsTg = new TransformGroup(lsTranslate);	
    	Sphere lightSphere = new Sphere(0.01f);
    	lightSphere.setAppearance(worldView.getPlaygroundLineAppear());
    	lightSphere.setPickable(true);
    	lsTg.addChild(lightSphere);
    	lightBG.addChild(lsTg);
    	worldTG.addChild(lightBG);
		
		// Die Branchgroup f¸r den Boden
		terrainBG = new BranchGroup();
		terrainBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		terrainBG.setPickable(true);
		
		// Erzeuge Boden
		Shape3D floor = createTerrainShape();
		floor.setAppearance(worldView.getPlaygroundAppear());
		floor.setPickable(true);
		
		// schiebe den Boden so, dass die Oberfl‰che genau auf der 
		// Ebene z = 0 liegt.
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, -PLAYGROUND_THICKNESS));
		TransformGroup tgT = new TransformGroup(translate);
		tgT.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgT.setPickable(true);
		tgT.addChild(floor);
		terrainBG.addChild(tgT);
		
		// Erzeuge Linien auf dem Boden
		Shape3D lineFloor = createLineShape();
		lineFloor.setAppearance(worldView.getPlaygroundLineAppear());
		lineFloor.setPickable(true);
		
		// Schiebe die Linien knapp ¸ber den Boden.
		translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.001d));
		TransformGroup tgL = new TransformGroup(translate);
		tgL.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgL.setPickable(true);
		tgL.addChild(lineFloor);
		tgT.addChild(tgL);
		
		worldTG.addChild(terrainBG);
		
		// Die TranformGroup fuer alle Hindernisse:
		obstBG = new BranchGroup();
		// Damit sp‰ter Bots hinzugef¸gt werden kˆnnen
		obstBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		obstBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		// Objekte sind fest
		obstBG.setPickable(true);

		// Die vier Hindernisse:
		Box north = new Box(PLAYGROUND_WIDTH/2 + 0.2f, 0.1f,
				0.2f, worldView.getObstacleAppear());
		north.setPickable(true);
		north.setName("North");
		Box south = new Box(PLAYGROUND_WIDTH/2 + 0.2f, 0.1f,
				0.2f, worldView.getObstacleAppear());
		south.setPickable(true);
		south.setName("South");
		Box east = new Box(0.1f, PLAYGROUND_HEIGHT/2 + 0.2f,
				0.2f, worldView.getObstacleAppear());
		east.setPickable(true);
		east.setName("East");
		Box west = new Box(0.1f, PLAYGROUND_HEIGHT/2 + 0.2f,
				0.2f, worldView.getObstacleAppear());
		west.setPickable(true);
		west.setName("West");
		
		// vier W‰nde
		Box wall1 = new Box(0.1f, 0.05f, 0.2f, worldView.getObstacleAppear());
		wall1.setPickable(true);
		wall1.setName("Wall1");
		
		Box wall2 = new Box(PLAYGROUND_WIDTH/2 - 0.45f, 0.05f, 0.2f, 
				worldView.getObstacleAppear());
		wall2.setPickable(true);
		wall2.setName("Wall2");
		
		Box wall3 = new Box(0.1f, 0.05f, 0.2f, worldView.getObstacleAppear());
		wall3.setPickable(true);
		wall3.setName("Wall3");
		
		Box wall4 = new Box(0.2f, 0.025f, 0.2f, worldView.getObstacleAppear());
		wall4.setPickable(true);
		wall4.setName("Wall4");


		// Hindernisse werden an die richtige Position geschoben

		// Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau 
		// buendig mit der Unterkante des Bodens ist:
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup tgO = new TransformGroup(translate);
		tgO.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgO.setPickable(true);
		obstBG.addChild(tgO);
		
		// Danach bekommt jedes Hindernis seine eigene TransformGroup, um es
		// an den individuellen Platz zu schieben:
		
		translate.set(new Vector3f(0f, PLAYGROUND_HEIGHT/2 + 0.1f,
				0f));
		TransformGroup tgN = new TransformGroup(translate);
		tgN.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgN.setPickable(true);
		tgN.addChild(north);
		tgO.addChild(tgN);

		translate.set(new Vector3f(0f,
				-(PLAYGROUND_HEIGHT/2 + 0.1f), 0f));
		TransformGroup tgS = new TransformGroup(translate);
		tgS.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgS.setPickable(true);
		tgS.addChild(south);
		tgO.addChild(tgS);

		translate.set(new Vector3f(PLAYGROUND_WIDTH/2 + 0.1f, 0f,  0f));
		TransformGroup tgE = new TransformGroup(translate);
		tgE.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgE.setPickable(true);
		tgE.addChild(east);
		tgO.addChild(tgE);

		translate.set(new Vector3f(-(PLAYGROUND_WIDTH/2 + 0.1f),
				0f, 0f));
		TransformGroup tgW = new TransformGroup(translate);
		tgW.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgW.setPickable(true);
		tgW.addChild(west);
		tgO.addChild(tgW);

		// Trennw‰nde an die richtigen Positionen schieben.
		translate.set(new Vector3f(-(PLAYGROUND_WIDTH/2) + 0.1f,
				0f, 0f));
		TransformGroup tgWall1 = new TransformGroup(translate);
		tgWall1.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgWall1.setPickable(true);
		tgWall1.addChild(wall1);
		tgO.addChild(tgWall1);
		
		translate.set(new Vector3f(-0.05f, 0f, 0f));
		TransformGroup tgWall2 = new TransformGroup(translate);
		tgWall2.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgWall2.setPickable(true);
		tgWall2.addChild(wall2);
		tgO.addChild(tgWall2);
		
		translate.set(new Vector3f((PLAYGROUND_WIDTH/2) - 0.1f,
				0f, 0f));
		TransformGroup tgWall3 = new TransformGroup(translate);
		tgWall3.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgWall3.setPickable(true);
		tgWall3.addChild(wall3);
		tgO.addChild(tgWall3);
		
		translate.set(new Vector3f(0.6f, -1f, 0f));
		TransformGroup tgWall4 = new TransformGroup(translate);
		tgWall4.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgWall4.setPickable(true);
		tgWall4.addChild(wall4);
		tgO.addChild(tgWall4);
		
		
		obstBG.setCapability(Node.ENABLE_PICK_REPORTING);
		obstBG.setCapability(Node.ALLOW_PICKABLE_READ);

		obstBG.compile();

		// Die Hindernisse der Welt hinzufuegen
		worldTG.addChild(obstBG);
		// es duerfen noch weitere dazukommen
		worldTG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		objRoot.compile();

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
		obstBG.addChild(bot.getBotBG());
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
		
		// Eigenen Kˆrper des Roboters verstecken
		botBody.setPickable(false);
		
		PickInfo pickInfo = obstBG.pickAny(PickInfo.PICK_BOUNDS, PickInfo.NODE,
				pickShape);
		
		// Eigenen Kˆrper des Roboters wieder pickable machen
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
	 * 			  Der ÷ffnungswinkel des Blickstrahls           
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
	 * Pr¸ft ob unter dem angegebene Punkt innerhalb der Bodenfreiheit 
	 * des Bots noch Boden zu finden ist
	 * 
	 * @param pos
	 *            Die Position von der aus nach unten gemessen wird
	 * @param groundClearance
	 * 			  Die als normal anzusehende Bodenfreiheit   
	 * @param message
	 * 			  Name des Ber¸hrungspunktes, welcher getestet wird
	 * @return True wenn Bodenkontakt besteht.
	 */
	public synchronized boolean checkTerrain(Point3d pos, double groundClearance, String message) {

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
			System.out.println(message + " f‰llt ins Bodenlose.");
			return false;
		} else if(Math.round(pickInfo.getClosestDistance()*1000) > Math.round(groundClearance*1000)) {
			System.out.println(message + " f‰llt " + pickInfo.getClosestDistance()*1000 + " mm.");
			return false;	
		} else
			return true;
	}
	
	/**
	 * Liefert den Grad an Licht der vom Boden vor dem Sensor absorbiert wird.
	 * Je mehr Licht reflektiert wird, desto niedriger ist der zur¸ckgemeldete
	 * Wert. Der Wertebereich reicht also von 0 (weiﬂ oder maximale Reflexion) 
	 * bis 1023 (minimale Reflexion, schwarz oder Loch).
	 * 
	 * Es werden rayCount viele Strahlen gleichm‰ﬂig ortogonal zum Heading in
	 * die Szene Geschossen.
	 * 
	 * @param pos
	 *            Die Position, an der der Sensor angebracht ist
	 * @param heading
	 *            Die Blickrichtung
	 * @param openingAngle
	 * 			  Der ÷ffnungswinkel des Sensors       
	 * @param rayCount
	 * 			  Es werden rayCount viele Strahlen vom Sensor ausgewertet.    
	 * @return Die Menge an Licht die absorbiert wird von 1023(100%) bis 0(0%)
	 */
	public short sensGroundReflectionLine(Point3d pos, Vector3d heading, double openingAngle, short rayCount) {
		// Sensorposition
		Point3d sensPos = new Point3d(pos);
		// Sensorblickrichtung nach unten
		Vector3d sensHeading = new Vector3d(0d,0d,-1d);
		
		// Falls die Welt verschoben wurde:
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(sensPos);
		// oder rotiert:
		transform.transform(sensHeading);
		
		// Transformationsgruppen um den Sensorsweep zu machen
		Transform3D transformX = new Transform3D();	
		
		// Wenn mehr als ein Strahl ausgesendet werden soll, dann taste
		// den Sensorbereich parallel zur Achse des Bots ab.
		// Bei nur einem Strahl schaue in die Mitte. 
		if (rayCount > 2) {
			// beginne links aussen
			AxisAngle4d rotationAxisX = new AxisAngle4d(heading, openingAngle/2);
			transformX.set(rotationAxisX);
			transformX.transform(sensHeading);
			// arbeite dich nach rechts vor 
			// sende Strahlen in gleichm‰ssigen Abst‰nden aus
			rotationAxisX.set(heading, -(openingAngle/(rayCount-1)));
			transformX.set(rotationAxisX);
		} else {
			// ‰ndere die Blickrichtung NICHT
			transformX.setIdentity();
		}
		// Variablen und Objekte ausserhalb der Schleife vorbereiten.
		PickRay pickRay = new PickRay();
		PickInfo pickInfo;
		Shape3D shape;
		Color3f color = new Color3f();;
		// Variable zur Auswertung der Absorption/Reflexion
		float absorption = 0f;
		for(int j = 0; j < rayCount; j++) {
			// PickRay modifizieren
			pickRay.set(sensPos, sensHeading);
			// Picking durchf¸hren
			pickInfo = terrainBG.pickClosest(PickInfo.PICK_GEOMETRY,
					PickInfo.NODE, pickRay);
			// Boden auswerten
			if (pickInfo == null) {
				// kein Boden = 100% des Lichts wird verschluckt
				absorption += 1;
			} else if (pickInfo.getNode() instanceof Shape3D) {
				shape = (Shape3D) pickInfo.getNode();
				//shape.getAppearance().getColoringAttributes().getColor(color);
				shape.getAppearance().getMaterial().getDiffuseColor(color);
				// Je nach Farbe wird ein Teil des Lichts zur¸ckgeworfen.
				// Hierzu wird der Durchschnitt der Rot, Gr¸n und Blau Anteile 
				// der Farbe bestimmt.
				absorption += 1 - (color.x + color.y + color.z)/3;
			}
			// Heading anpassen
			transformX.transform(sensHeading);
		}// For-Schleife
		return  (short)(absorption*1023/(rayCount));
	}
	
	/**
	 * Liefert den Grad an Licht der vom Boden vor dem Sensor absorbiert wird.
	 * Je mehr Licht reflektiert wird, desto niedriger ist der zur¸ckgemeldete
	 * Wert. Der Wertebereich reicht also von 0 (weiﬂ oder maximale Reflexion) 
	 * bis 1023 (minimale Reflexion, schwarz oder Loch).
	 * 
	 * Es werden rayCount viele Strahlen gleichm‰ﬂig in form eines PLUSes in
	 * in die Szene Geschossen.
	 * 
	 * @param pos
	 *            Die Position, an der der Sensor angebracht ist
	 * @param heading
	 *            Die Blickrichtung
	 * @param openingAngle
	 * 			  Der ÷ffnungswinkel des Sensors       
	 * @param rayCount
	 * 			  Es werden rayCount viele Strahlen vom Sensor ausgewertet.    
	 * @return Die Menge an Licht die absorbiert wird von 1023(100%) bis 0(0%)
	 */
	public short sensGroundReflectionCross(Point3d pos, Vector3d heading, double openingAngle, short rayCount) {
		double absorption;
		Vector3d xHeading = new Vector3d(heading);
		absorption = sensGroundReflectionLine(pos,heading,openingAngle,(short)(rayCount/2));
		Transform3D rotation = new Transform3D();
		rotation.rotZ(Math.PI/2);
		rotation.transform(xHeading);
		absorption = sensGroundReflectionLine(pos,xHeading,openingAngle,(short)(rayCount/2));
		return  (short)(absorption/2);
	}
	
	/**
	 * Liefert die Helligkeit die auf einen Lichtsensor f‰llt
	 * 
	 * @param pos
	 *            Die Position des Lcihtsensors
	 * @param heading
	 *            Die Blickrichtung des Lichtsensors
	 * @param openingAngle
	 * 			  Der ÷ffnungswinkel des Blickstrahls          
	 * @return Die Dunkelheit um den Sensor herum von 1023(100%) bis 0(0%)
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
				openingAngle/2);
		obstBG.setPickable(false);
		terrainBG.setPickable(false);
		PickInfo pickInfo = lightBG.pickClosest(PickInfo.PICK_GEOMETRY,
				PickInfo.CLOSEST_DISTANCE, pickShape);
		obstBG.setPickable(true);
		terrainBG.setPickable(true);

		if (pickInfo == null)
			return 1023;
		else {
			int darkness = (int)((pickInfo.getClosestDistance()/LIGHT_SOURCE_REACH)*1023);
			if (darkness > 1023) darkness = 1023;
			return darkness;
		}
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
	 * Slow Motion heiﬂt 10 mal so hohe Zeitbasis
	 * @param slowMotion true: Slow Motion on
	 * @author Werner Pirkl (Morpheus.the.real@gmx.de
	 */
	public void setSlowMotion(boolean slowMotion) {
		this.slowMotion = slowMotion;
		if(slowMotion)
		{	
			// in slowMotionTime wird der Wert vom Schieberegler festgehalten
			this.baseTimeReal = baseTimeVirtual * slowMotionTime;
			// die Virtuelle also nach auﬂen sichtbare Zeitbasis bleibt jedoch erhalten da ja
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
