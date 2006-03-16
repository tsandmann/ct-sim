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
import ctSim.View.ControlFrame;

import java.awt.Color;
import java.util.*;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
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
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.AmbientLight;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Color3f;
import javax.vecmath.Vector4f;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.image.TextureLoader;

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

	/** Hoehe des Spielfelds in m */
	public static final float PLAYGROUND_HEIGHT = 4f;

	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_WIDTH = 4f;

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
	private List<Bot> bots;

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
	 * Vier BranchGroups, eine fuer die ganze Welt, eine fuer den Boden, eine
	 * fuer die Lichtquellen und die letzte fuer die Hindernisse
	 */

	/*
	 * BranchGroup fuer die ganze Welt
	 */
	// private BranchGroup scene;
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

	/*
	 * BranchGroup fuer die Hindernisse. Hier kommen auch die Bots hinein
	 */
	// public BranchGroup obstBG;
	/**
	 * BranchGroup fuer die Lichtquellen
	 */
	public BranchGroup lightBG;

	/** TransformGroup der gesamten Welt */
	private TransformGroup worldTG;

	/** Aussehen von Hindernissen */
	private Appearance obstacleAppear;

	/** Pfad zu einer Textur fuer die Hindernisse */
	public static final String OBST_TEXTURE = "textures/rock_wall.jpg";

	/** Aussehen des Bodens */
	private Appearance playgroundAppear;

	/** Aussehen der Linien auf dem Boden */
	private Appearance playgroundLineAppear;

	/** Aussehen einer Lichtquelle */
	private Appearance lightSourceAppear;

	/** Aussehen der Bots */
	private Appearance botAppear;

	/** Aussehen der Bots nach einer Kollision */
	private Appearance botAppearCollision;

	/** Aussehen der Bots nach einem Fall */
	private Appearance botAppearFall;

	/** Erzeugt eine neue Welt */
	public World(Controller controller) {
		super();

		this.controller = controller;
		bots = new LinkedList<Bot>();
		obstacles = new LinkedList();
		haveABreak = false;
		slowMotion = false;
		/* erstelle und sichere die Szene */
		sceneLight = new SceneLight();
		sceneLight.setScene(createSceneGraph());
		sceneLightBackup = sceneLight.clone();
	}

	/**
	 * Baut die 3D-Repraesentation des Bodens aus 2D-Polygonen zusammen
	 * 
	 * @return der Boden
	 */
	private Shape3D createTerrainShape() {

		Shape3D ts = new Shape3D();
		// Anzahl der verwendeten Punkte
		int N = 10 + 6 + 9 + 2 + 6;
		int totalN = N;
		// data muss pro Punkt die Werte x, y und z speichern
		float[] data = new float[totalN * 3];
		// zwei Polygone (Deckel und Boden) mit N Ecken
		int stripCounts[] = { N };
		// Zaehler
		int n = 0;

		// Boden erzeugen
		//
		// Umriss des Bodens erzeugen, beachte dabei die Oeffnung!
		// 1.unten links
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 2;
		data[n++] = 0f;
		// 2.unten rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 2;
		data[n++] = 0f;
		// 2a.unteres Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 6;
		data[n++] = 0f;
		// 2b.unteres Drittel Mitte
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 6;
		data[n++] = 0f;
		// 2c.unteres Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 6;
		data[n++] = 0f;
		// 2a.unteres Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 8;
		data[n++] = 0f;
		// 2b.unteres Drittel Mitte
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 8;
		data[n++] = 0f;
		// 2c.unteres Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = -PLAYGROUND_HEIGHT / 8;
		data[n++] = 0f;
		// I.Mitte rechts / Licht2 rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = 0f;
		data[n++] = 0f;
		// II.Licht2 Mitte
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f;
		data[n++] = 0f;
		data[n++] = 0f;
		// III.Licht2 oben
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f;
		data[n++] = 2f;
		data[n++] = 0f;
		// IV.Licht2 Mitte
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f;
		data[n++] = 0f;
		data[n++] = 0f;
		// V.Licht2 links
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f - 1f;
		data[n++] = 0f;
		data[n++] = 0f;
		// V-1.Licht2 links oben
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f - 1f;
		data[n++] = 0.2f;
		data[n++] = 0f;
		// V-2.Licht2 links
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f - 1f;
		data[n++] = 0f;
		data[n++] = 0f;
		// VI.Licht2 Mitte
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f;
		data[n++] = 0f;
		data[n++] = 0f;
		// VII.Licht2 unten
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f;
		data[n++] = -2f;
		data[n++] = 0f;
		// VIII.Licht2 Mitte
		data[n++] = PLAYGROUND_WIDTH / 2 - 0.35f;
		data[n++] = 0f;
		data[n++] = 0f;
		// IX.Mitte rechts/Licht2 rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = 0f;
		data[n++] = 0f;
		// 2d.oberes Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 8;
		data[n++] = 0f;
		// 2e.oberes Drittel Mitte
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 8;
		data[n++] = 0f;
		// 2f.oberes Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 8;
		data[n++] = 0f;
		// 2d.oberes Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 6;
		data[n++] = 0f;
		// 2e.oberes Drittel Mitte
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 6;
		data[n++] = 0f;
		// 2f.oberes Drittel rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 6;
		data[n++] = 0f;
		// 3.oben rechts
		data[n++] = PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 2;
		data[n++] = 0f;
		// 4.oben links
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = PLAYGROUND_HEIGHT / 2;
		data[n++] = 0f;
		// 5.R2m links
		data[n++] = -PLAYGROUND_WIDTH / 2;
		data[n++] = -1.0f;
		data[n++] = 0f;
		// 6.R2 Loch OR
		data[n++] = -0.3f;
		data[n++] = -1.0f;
		data[n++] = 0f;
		// 7.R2 Loch UR
		data[n++] = -0.3f;
		data[n++] = -1.5f;
		data[n++] = 0f;
		// 8.R2 Loch UL
		data[n++] = -0.8f;
		data[n++] = -1.5f;
		data[n++] = 0f;
		// 9.R2 Loch OL
		data[n++] = -0.8f;
		data[n++] = -1.0f;
		data[n++] = 0f;
		// 10.R2m links
		data[n++] = -PLAYGROUND_WIDTH / 2;
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

		// Hinzufuegen der Ober- und Unterseite des Terrain-Shape3D
		ts.addGeometry(gi.getGeometryArray());

		return ts;
	}

	/**
	 * Baut die 3D-Repraesentation der Linien fuer den Boden aus 2D-Polygonen
	 * zusammen
	 * 
	 * @return die Linie
	 */
	private Shape3D createLineShape() {

		Shape3D ls = new Shape3D();
		// Anzahl der verwendeten Punkte
		int N = 10;
		int totalN = N;
		// data muss pro Punkt die Werte x, y und z speichern
		float[] data = new float[totalN * 3];
		// zwei Polygone (Deckel und Boden) mit N Ecken
		int stripCounts[] = { N };
		// Zaehler
		int n = 0;

		// Linien erzeugen
		// 11
		data[n++] = -0.71f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		// 12
		data[n++] = 0.71f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		// 13
		data[n++] = 0.71f;
		data[n++] = 1.71f;
		data[n++] = 0f;
		// 14
		data[n++] = -0.71f;
		data[n++] = 1.71f;
		data[n++] = 0f;
		// 15
		data[n++] = -0.71f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		// 16
		data[n++] = -0.7f;
		data[n++] = -1.71f;
		data[n++] = 0f;
		// 17
		data[n++] = -0.7f;
		data[n++] = 1.7f;
		data[n++] = 0f;
		// 18
		data[n++] = 0.7f;
		data[n++] = 1.7f;
		data[n++] = 0f;
		// 19
		data[n++] = 0.7f;
		data[n++] = -1.7f;
		data[n++] = 0f;
		// 20
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

		// Hinzufuegen der Ober- und Unterseite des Linien-Shape3D
		ls.addGeometry(gi.getGeometryArray());

		return ls;
	}

	private void createAppearance() {
		// Material fuer die Lichtreflektionen der Welt definieren
		// Boden- und Bot-Material
		Material mat = new Material();
		mat.setAmbientColor(new Color3f(Color.LIGHT_GRAY));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));

		// Linien-Material
		Material matLine = new Material();
		matLine.setAmbientColor(new Color3f(0f, 0f, 0f));
		matLine.setDiffuseColor(new Color3f(0.1f, 0.1f, 0.1f));
		matLine.setSpecularColor(new Color3f(1f, 1f, 1f));

		// Lichtquellen-Material
		Material matLight = new Material();
		matLight.setEmissiveColor(new Color3f(1f, 1f, 0.7f));
		matLight.setAmbientColor(new Color3f(1f, 1f, 0f));
		matLight.setDiffuseColor(new Color3f(1f, 1f, 0f));
		matLight.setSpecularColor(new Color3f(0.7f, 0.7f, 0.7f));

		// Aussehen der Lichtquellen
		lightSourceAppear = new Appearance();
		lightSourceAppear.setMaterial(matLight);

		// Aussehen des Bodens -- hellgrau:
		playgroundAppear = new Appearance();
		playgroundAppear.setMaterial(mat);

		// Aussehen der Linien auf dem Boden -- dunkelgrau:
		playgroundLineAppear = new Appearance();
		playgroundLineAppear.setMaterial(matLine);

		// Aussehen der Hindernisse -- dunkelgrau:
		obstacleAppear = new Appearance();
		obstacleAppear.setMaterial(mat);

		// ...und mit einer Textur ueberzogen:
		TexCoordGeneration tcg = new TexCoordGeneration(
				TexCoordGeneration.OBJECT_LINEAR,
				TexCoordGeneration.TEXTURE_COORDINATE_3, new Vector4f(1.0f,
						1.0f, 0.0f, 0.0f),
				new Vector4f(0.0f, 1.0f, 1.0f, 0.0f), new Vector4f(1.0f, 0.0f,
						1.0f, 0.0f));
		obstacleAppear.setTexCoordGeneration(tcg);

		TextureLoader loader = new TextureLoader(ClassLoader
				.getSystemResource(OBST_TEXTURE), null);
		Texture2D texture = (Texture2D) loader.getTexture();
		texture.setBoundaryModeS(Texture.WRAP);
		texture.setBoundaryModeT(Texture.WRAP);
		obstacleAppear.setTexture(texture);

		// Aussehen der Bots:
		botAppear = new Appearance(); // Bots sind rot ;-)
		botAppear.setColoringAttributes(new ColoringAttributes(new Color3f(
				Color.RED), ColoringAttributes.FASTEST));
		botAppear.setMaterial(mat);

		// Aussehen der Bots nach einer Kollision:
		botAppearCollision = new Appearance(); // Bots sind blau ;-)
		botAppearCollision.setColoringAttributes(new ColoringAttributes(
				new Color3f(Color.BLUE), ColoringAttributes.FASTEST));
		botAppearCollision.setMaterial(mat);

		// Aussehen der Bots beim Kippen:
		botAppearFall = new Appearance(); // Bots sind gruen ;-)
		botAppearFall.setColoringAttributes(new ColoringAttributes(new Color3f(
				Color.GREEN), ColoringAttributes.FASTEST));
		botAppearFall.setMaterial(mat);

	}

	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * 
	 * @return der Szenegraph
	 */
	public BranchGroup createSceneGraph() {

		float[][] pillarPositions = { { 0.2f, -1f }, { 0.2f, -0.5f },
				{ 0.2f, 0f }, { 0.2f, 0.5f }, { 0.05f, 0.9f },
				{ -0.48f, 1.0f }, { -1.0f, 1.0f }, { -1.35f, 0.65f } };

		// Die Lichtquellen aus Version 0.2 des Simulators
		// float[][] oldLights =
		// {{PLAYGROUND_WIDTH/2,PLAYGROUND_HEIGHT/2,0.5f},{PLAYGROUND_WIDTH/2 -
		// 0.35f, 0f, 0.5f}};

		createAppearance();
		// Die Wurzel des Ganzen:
		BranchGroup objRoot = new BranchGroup();

		Transform3D transform = new Transform3D();

		transform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		worldTG = new TransformGroup(transform);

		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		worldTG.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		worldTG.setCapability(TransformGroup.ALLOW_PICKABLE_READ);
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
		lightBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		lightBG.setPickable(true);

		// Lichtpunkte
		BoundingSphere pointLightBounds = new BoundingSphere(new Point3d(0d,
				0d, 0d), 10d);
		Color3f pointLightColor = new Color3f(1.0f, 1.0f, 0.9f);

		/*
		 * Fuer den Hindernis-Parcours brauchen wir 8 Lichtquellen und 8
		 * Saeulen. Um nicht jede Lichtquelle von Hand bauen zu muessen, 
		 * wurde alles in eine Subroutine ausgelagert.
		 */

		for (float[] pos : pillarPositions) {
			createLight(pointLightBounds, pointLightColor, pos[0], pos[1], 0.5f);
		}

		worldTG.addChild(lightBG);

		// Die Branchgroup fuer den Boden
		terrainBG = new BranchGroup();
		terrainBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		terrainBG.setPickable(true);

		// Erzeuge Boden
		Shape3D floor = createTerrainShape();
		floor.setAppearance(getPlaygroundAppear());
		floor.setPickable(true);

		// Schiebe den Boden so, dass die Oberflaeche genau auf der
		// (relativen) Ebene Z = 0 liegt.
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, -PLAYGROUND_THICKNESS));
		TransformGroup tgT = new TransformGroup(translate);
		tgT.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgT.setPickable(true);
		tgT.addChild(floor);
		terrainBG.addChild(tgT);

		// Erzeuge Linien auf dem Boden
		Shape3D lineFloor = createLineShape();
		lineFloor.setAppearance(getPlaygroundLineAppear());
		lineFloor.setPickable(true);

		// Schiebe die Linien knapp ueber den Boden.
		translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.001d));
		TransformGroup tgL = new TransformGroup(translate);
		tgL.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgL.setPickable(true);
		tgL.addChild(lineFloor);
		tgT.addChild(tgL);

		worldTG.addChild(terrainBG);

		// Die TranformGroup fuer alle Hindernisse:
		sceneLight.setObstBG(new BranchGroup());
		// Damit spaeter Bots hinzugefuegt werden koennen:
		sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		sceneLight.getObstBG().setCapability(
				TransformGroup.ALLOW_PICKABLE_WRITE);
		// Objekte sind fest
		sceneLight.getObstBG().setPickable(true);

		// Die vier Randmauern der Welt:
		Box north = new Box(PLAYGROUND_WIDTH / 2 + 0.2f, 0.1f, 0.2f,
				getObstacleAppear());
		north.setPickable(true);
		north.setName("North");
		Box south = new Box(PLAYGROUND_WIDTH / 2 + 0.2f, 0.1f, 0.2f,
				getObstacleAppear());
		south.setPickable(true);
		south.setName("South");
		Box east = new Box(0.1f, PLAYGROUND_HEIGHT / 2 + 0.2f, 0.2f,
				getObstacleAppear());
		east.setPickable(true);
		east.setName("East");
		Box west = new Box(0.1f, PLAYGROUND_HEIGHT / 2 + 0.2f, 0.2f,
				getObstacleAppear());
		west.setPickable(true);
		west.setName("West");

		// sechs Zwischenwaende aus Version 0.2, sind ausgebaut fuer den neuen Parcours
		// Box wall1 = new Box(0.1f, 0.05f, 0.2f,
		// worldView.getObstacleAppear());
		// wall1.setPickable(true);
		// wall1.setName("Wall1");
		//		
		// Box wall2 = new Box(PLAYGROUND_WIDTH/2 - 0.45f, 0.05f, 0.2f,
		// worldView.getObstacleAppear());
		// wall2.setPickable(true);
		// wall2.setName("Wall2");
		//		
		// Box wall3 = new Box(0.1f, 0.05f, 0.2f,
		// worldView.getObstacleAppear());
		// wall3.setPickable(true);
		// wall3.setName("Wall3");
		//		
		// Box wall4 = new Box(0.2f, 0.025f, 0.2f,
		// worldView.getObstacleAppear());
		// wall4.setPickable(true);
		// wall4.setName("Wall4");
		//
		// Box wall5 = new Box(0.4f, 0.025f, 0.2f,
		// worldView.getObstacleAppear());
		// wall5.setPickable(true);
		// wall5.setName("Wall5");
		//		
		// Box wall6 = new Box(0.025f, 0.3f, 0.2f,
		// worldView.getObstacleAppear());
		// wall6.setPickable(true);
		// wall6.setName("Wall6");

		// Hindernisse werden an die richtige Position geschoben

		// Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau
		// buendig mit der Unterkante des Bodens ist:
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup tgO = new TransformGroup(translate);
		tgO.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgO.setPickable(true);
		sceneLight.getObstBG().addChild(tgO);

		/* Zusaetzlich zu den Lichtquellen werden 8 kleine Saeulen erzeugt. */

		for (float[] pos : pillarPositions) {
			createPillar(pillarPositions, tgO, pos[0], pos[1]);
		}

		// Danach bekommt jedes Hindernis seine eigene TransformGroup, um es
		// an den individuellen Platz zu schieben:

		translate.set(new Vector3f(0f, PLAYGROUND_HEIGHT / 2 + 0.1f, 0f));
		TransformGroup tgN = new TransformGroup(translate);
		tgN.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgN.setPickable(true);
		tgN.addChild(north);
		tgO.addChild(tgN);

		translate.set(new Vector3f(0f, -(PLAYGROUND_HEIGHT / 2 + 0.1f), 0f));
		TransformGroup tgS = new TransformGroup(translate);
		tgS.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgS.setPickable(true);
		tgS.addChild(south);
		tgO.addChild(tgS);

		translate.set(new Vector3f(PLAYGROUND_WIDTH / 2 + 0.1f, 0f, 0f));
		TransformGroup tgE = new TransformGroup(translate);
		tgE.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgE.setPickable(true);
		tgE.addChild(east);
		tgO.addChild(tgE);

		translate.set(new Vector3f(-(PLAYGROUND_WIDTH / 2 + 0.1f), 0f, 0f));
		TransformGroup tgW = new TransformGroup(translate);
		tgW.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgW.setPickable(true);
		tgW.addChild(west);
		tgO.addChild(tgW);

		// Trennwaende aus Version 0.2 an die richtigen Positionen schieben:
		// translate.set(new Vector3f(-(PLAYGROUND_WIDTH/2) + 0.1f,
		// 0f, 0f));
		// TransformGroup tgWall1 = new TransformGroup(translate);
		// tgWall1.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		// tgWall1.setPickable(true);
		// tgWall1.addChild(wall1);
		// tgO.addChild(tgWall1);
		//		
		// translate.set(new Vector3f(-0.05f, 0f, 0f));
		// TransformGroup tgWall2 = new TransformGroup(translate);
		// tgWall2.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		// tgWall2.setPickable(true);
		// tgWall2.addChild(wall2);
		// tgO.addChild(tgWall2);
		//		
		// translate.set(new Vector3f((PLAYGROUND_WIDTH/2) - 0.1f,
		// 0f, 0f));
		// TransformGroup tgWall3 = new TransformGroup(translate);
		// tgWall3.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		// tgWall3.setPickable(true);
		// tgWall3.addChild(wall3);
		// tgO.addChild(tgWall3);
		//		
		// translate.set(new Vector3f(0.6f, -1f, 0f));
		// TransformGroup tgWall4 = new TransformGroup(translate);
		// tgWall4.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		// tgWall4.setPickable(true);
		// tgWall4.addChild(wall4);
		// tgO.addChild(tgWall4);
		//
		// translate.set(new Vector3f(0.1f,
		// -0.65f, 0f));
		// TransformGroup tgWall5 = new TransformGroup(translate);
		// tgWall5.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		// tgWall5.setPickable(true);
		// tgWall5.addChild(wall5);
		// tgO.addChild(tgWall5);
		//		
		// translate.set(new Vector3f(-0.1f, -0.35f, 0f));
		// TransformGroup tgWall6 = new TransformGroup(translate);
		// tgWall6.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		// tgWall6.setPickable(true);
		// tgWall6.addChild(wall6);
		// tgO.addChild(tgWall6);

		/*
		 * ParcoursLoader pL = new ParcoursLoader();
		 * pL.setWallAppear(obstacleAppear); pL.insertSceneGraph(tgO);
		 */

		sceneLight.getObstBG().setCapability(Node.ENABLE_PICK_REPORTING);
		sceneLight.getObstBG().setCapability(Node.ALLOW_PICKABLE_READ);

		// obstBG.compile();

		// Die Hindernisse der Welt hinzufuegen
		worldTG.addChild(sceneLight.getObstBG());
		// es duerfen noch weitere dazukommen
		worldTG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);

		// objRoot.compile();

		return objRoot;
	}

	private void createPillar(float[][] pillarPositions, TransformGroup tgO,
			float xpos, float ypos) {
		Cylinder pillar = new Cylinder(0.05f, 0.5f, getObstacleAppear());
		pillar.setPickable(true);
		Transform3D transformer = new Transform3D();
		transformer.rotX(0.5 * Math.PI);
		transformer.setTranslation(new Vector3f(xpos, ypos, 0));
		TransformGroup tg = new TransformGroup(transformer);
		tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg.setPickable(true);
		tg.addChild(pillar);
		tgO.addChild(tg);
	}

	private void createLight(BoundingSphere pointLightBounds,
			Color3f pointLightColor, float xpos, float ypos, float zpos) {
		PointLight pointLight = new PointLight();
		pointLight.setColor(pointLightColor);
		pointLight.setPosition(xpos, ypos, zpos);
		pointLight.setInfluencingBounds(pointLightBounds);
		pointLight.setAttenuation(1f, 3f, 0f);
		pointLight.setEnable(true);
		lightBG.addChild(pointLight);

		Transform3D lsTranslate = new Transform3D();
		lsTranslate.set(new Vector3f(xpos, ypos, zpos));
		TransformGroup lsTransformGroup = new TransformGroup(lsTranslate);
		Sphere lightSphere = new Sphere(0.07f);
		lightSphere.setAppearance(getLightSourceAppear());
		lightSphere.setPickable(true);
		lsTransformGroup.addChild(lightSphere);
		lightBG.addChild(lsTransformGroup);
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
		BranchGroup bg = bot.getBotBG();

		// Sichere den neuen Bot in sceneLight
		sceneLight.addBot(bot.getBotName(), bot.getTranslationGroup(), bot
				.getRotationGroup());
		// Benachrichtige den Controller uber neue Bots
		controller.addBotToView(bot.getBotName(), bot.getTranslationGroup(),
				bot.getRotationGroup(), bg);

		sceneLight.getObstBG().addChild(bg);

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
		else
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
		else
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
		;
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
		else {
			int darkness = (int) ((pickInfo.getClosestDistance() / LIGHT_SOURCE_REACH) * 1023);
			if (darkness > 1023)
				darkness = 1023;
			return darkness;
		}
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
	 * @return Gibt bots zurueck.
	 */
	public List getBots() {
		return bots;
	}

	/**
	 * @param bots
	 *            Wert fuer bots, der gesetzt werden soll.
	 */
	public void setBots(List<Bot> bots) {
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
		} else
			return null;
	}

	/**
	 * @return Gibt das Erscheinungsbild der Hindernisse zurueck
	 */
	public Appearance getObstacleAppear() {
		return obstacleAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild des Bodens zurueck
	 */
	public Appearance getPlaygroundAppear() {
		return playgroundAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild der Linien auf dem Boden zurueck
	 */
	public Appearance getPlaygroundLineAppear() {
		return playgroundLineAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild einer Lichtquelle zurueck
	 */
	public Appearance getLightSourceAppear() {
		return lightSourceAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild der Bots zurueck
	 */
	public Appearance getBotAppear() {
		return botAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild der Bots nach einer Kollision zurueck
	 */
	public Appearance getBotAppearCollision() {
		return botAppearCollision;
	}

	/**
	 * @return Gibt das Erscheinungsbild der Bots nach einem Fall zurueck
	 */
	public Appearance getBotAppearFall() {
		return botAppearFall;
	}

	/**
	 * @return Gibt eine Referenz auf sceneLight zurueck
	 * @return Gibt den Wert von sceneLight zurueck
	 */
	public SceneLight getSceneLight() {
		return sceneLight;
	}

}
