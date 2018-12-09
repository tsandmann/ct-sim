/*
 * c't-Sim - Robotersimulator für den c't-Bot
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

import java.awt.Color;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.Material;
import javax.media.j3d.PointLight;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.image.TextureLoader;

import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Diese Klasse hilft einen Parcours aus einer ASCII-Datei zu laden
 * Linien-Kreuzungen:
 * [ - X-Kreuzung
 * T - sieht aus wie das T, d.h. Linie geht nach unten weg
 * ] - T-Kreuzung steht auf dem Kopf, d.h. die Linie geht nach oben weg
 * { - in Uhrzeigersinn gedrehte T-Kreuzung, Linie geht nach links weg (Richtung der Mittelspitze)
 * } - entgegen Uhrzeigersinn gedrehte T-Kreuzung (Linie geht in Richtung der Mittelspitze nach rechts weg)
 *
 * @author Benjamin Benz
 */
public class ParcoursLoader {
	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.ParcoursLoader");

	/** Z-Koorndinate der Lampen */
	public static final float LIGHTZ = 0.5f;

	/** Linienbreite */
	public static final float LINEWIDTH = 0.1f;

	/** Horizontales Liniensegment */
	public static final float[] LINE_HORIZ = {
		-0.5f,	0f - LINEWIDTH/2,0f,
		0.5f,	0f - LINEWIDTH/2,0f,
		0.5f,	0f + LINEWIDTH/2,0f,
		-0.5f,	0f + LINEWIDTH/2,0f,
		-0.5f,	0f - LINEWIDTH/2,0f,
	};

	/** Vertikales Liniensegment */
	public static final float[] LINE_VERT = {
		0f - LINEWIDTH/2,	-0.5f,	0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,	0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.5f,	0f,	// lange Linie hoch
		0f - LINEWIDTH/2,	0.5f,	0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	-0.5f,	0f,	// lange Linie runter
	};

	/** Linie -- Ecke im Nordwesten */
	public static final float[] LINE_CORNER_NW = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// lange Linie hoch
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// lange Linie nach rechts
		0.5f,				0.0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		0f -LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// lange Linie nach links
		0f - LINEWIDTH/2,	-0.5f,				0f,	// lange Linie nach unten
	};

	/** Linie -- Ecke im Nordosten */
	public static final float[] LINE_CORNER_NE = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// lange Linie hoch
		-0.5f,				0.0f + LINEWIDTH/2,	0f,	// lange Linie nach links
		-0.5f,				0.0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f -LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// lange Linie nach rechts
		0f - LINEWIDTH/2,	-0.5f,				0f,	// lange Linie nach unten
	};

	/** Linie -- Ecke im Südwesten */
	public static final float[] LINE_CORNER_SW = {
		0f +LINEWIDTH/2 , 0.5f               ,0f,	// Start oben rechts
		0f -LINEWIDTH/2 , 0.5f               ,0f,	// kurze Linie nach links
		0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// lange Linie nach unten
		0.5f			, 0.0f - LINEWIDTH/2 ,0f,	// lange Linie nach rechts
		0.5f			, 0.0f + LINEWIDTH/2 ,0f,	// kurze Linie nach oben
		0f +LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,	// lange Linie nach links
		0f +LINEWIDTH/2 , 0.5f				 ,0f,	// lange Linie nach oben
	};

	/** Linie -- Ecke im Südosten */
	public static final float[] LINE_CORNER_SE = {
		-0.5f,				0.0f + LINEWIDTH/2,	0f,	// Start Links oben
		-0.5f,				0.0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f +LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// lange Linie nach rechts
		0f +LINEWIDTH/2,	0.5f,				0f,	// lange Linie nach oben
		0f -LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f -LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// lange Linie nach unten
		-0.5f,				0.0f + LINEWIDTH/2,	0f,	// lange Linie nach links
	};

	/** X-Kreuzung */
	public static final float[] LINE_CROSSING_X = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie hoch bis Mitte
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// lange Linie nach rechts
		0.5f,				0.0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		0f + LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// Linie bis Mitte
		0f + LINEWIDTH/2,	0.5f,				0f,	// Linie bis Hoch
		0f - LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// Linie runter bis Mitte
		-0.5f,				0f + LINEWIDTH/2,	0f,	// Linie nach links
		-0.5f,				0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f - LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie nach rechts bis Mitte
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Kreuz schließen zum Ausgangspunkt
	};

	/** T-Kreuzung, Ausrichtung wie das T selbst, also Linie geht nach unten */
	public static final float[] LINE_CROSSING_T = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie hoch bis Mitte
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// lange Linie nach rechts
		0.5f,				0.0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		-0.5f,			0.0f + LINEWIDTH/2,		0f,	// lange Linie nach links
		-0.5f,			0f - LINEWIDTH/2,		0f,	// kurze Linie runter
		0f - LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie nach rechts bis Mitte
		0f - LINEWIDTH/2,	-0.5f,				0f,	// T schließen zum Ausgangspunkt
	};

	/** gespiegelte T-Kreuzung, Ausrichtung wie gespiegeltes, also Linie geht nach oben */
	public static final float[] LINE_CROSSING_T_MIRR = {
		-0.5f,				0.0f - LINEWIDTH/2,	0f,	// Start Ecke unten links
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// lange Linie nach rechts
		0.5f,				0 + LINEWIDTH/2,	0f,	// kurze Linie hoch
		0f + LINEWIDTH/2,	0f + LINEWIDTH/2,	0f,	// Linie links bis Mitte
		0f + LINEWIDTH/2,	0.5f,				0f,	// Linie hoch
		0f - LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	0f + LINEWIDTH/2,	0f,	// Linie runter bis Mitte
		-0.5f,				0f + LINEWIDTH/2,	0f,	// Linie nach links
		-0.5f,				0f - LINEWIDTH/2,	0f,	// kurze Linie runter zum Ausgangspunkt


	};

	/** T-Kreuzung, 90 Grad gedreht entgegen Uhrzeigersinn, also Linie geht nach rechts */
	public static final float[] LINE_CROSSING_T_ROT_UNCLOCKWISE = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start Ecke unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie hoch bis Mitte
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// Linie nach rechts
		0.5f,				0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		0f + LINEWIDTH/2,	0f + LINEWIDTH/2,	0f,	// Linie nach links bis Mitte
		0f + LINEWIDTH/2,	0.5f,				0f,	// Linie hoch
		0f - LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	-0.5f,				0f,	// lange Linie runter zum Ausgangspunkt

	};

	/** T-Kreuzung, 90 Grad gedreht in Uhrzeigersinn, also Linie geht nach links */
	public static final float[] LINE_CROSSING_T_ROT_CLOCKWISE = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start Ecke unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.5f,				0f,	// lange Linie hoch
		0f - LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	0f + LINEWIDTH/2,	0f,	// Linie runter bis Mitte
		-0.5f,				0f + LINEWIDTH/2,	0f,	// Linie nach links
		-0.5f,				0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f - LINEWIDTH/2,	0f - LINEWIDTH/2,	0f,	// Linie rechts bis Mitte
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Linie runter zum Ausgangspunkt
	};

	/** Linie -- mit Unterbrechung vertikal besteht aus 2 untereinander liegenden Teillinien */
	public static final float[] LINE_BREAK_VERT = {
		0f +LINEWIDTH/2 , 0.5f               ,0f,	// Start oben rechts
		0f -LINEWIDTH/2 , 0.5f               ,0f,	// kurze Linie nach links
		0f -LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,	// Linie runter bis oberhalb Mitte
		0f +LINEWIDTH/2	, 0.0f + LINEWIDTH/2 ,0f,	// Linie nach rechts
		0f +LINEWIDTH/2	, 0.5f               ,0f,	// Linie wieder nach oben

		0f +LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// Start rechts unterhalb Mitte
		0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// kurze Linie nach links
		0f -LINEWIDTH/2 , -0.5f               ,0f,	// Linie ganz runter
		0f +LINEWIDTH/2 , -0.5f               ,0f,	// kurze Linie unten nach rechts
		0f +LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// Linie wieder hoch bis unterhalb Mitte
	};

	/** Linie -- mit Unterbrechung horizontal besteht aus 2 nebeneinander liegenden Teillinien */
	public static final float[] LINE_BREAK_HOR = {
		-0.5f           , 0.0f + LINEWIDTH/2 ,0f,	// Start links oberhalb Mitte
		-0.5f           , 0.0f - LINEWIDTH/2 ,0f,	// kurze Linie links runter
		0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// Linie nach rechts bis links von Mitte
		0f -LINEWIDTH/2	, 0.0f + LINEWIDTH/2 ,0f,	// kurze Linie hoch
        -0.5f			, 0.0f + LINEWIDTH/2 ,0f,	// Linie wieder nach Links

        0f +LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,	// Start Linie oberhalb Mitte rechts
		0f +LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// Kurze Linie runter
		0.5f            , 0.0f - LINEWIDTH/2 ,0f,	// Linie bis ganz rechts
		0.5f            , 0.0f + LINEWIDTH/2 ,0f,	// kurze Linie rechts hoch
		0f +LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,	// Linie wieder links bis kurz vor Mitte
	};

	/** Wand-Höhe */
	private static final float WALL_HEIGHT = 0.2f;

	/** Verwaltet alle Aussehen */
	HashMap appearances = new HashMap();

	/** Parcours-Map */
	private int[][] parcoursMap = null;

	/** Der eigentliche Parcours */
	private Parcours parcours;

	/** Neuen ParcoursLoader instantiieren */
	public ParcoursLoader() {
		super();
		parcours = new Parcours(this);
	}

	/**
	 * Erzeugt ein Wandsegment Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 *
	 * @param x				Position in X-Richtung
	 * @param y				Position in X-Richtung
	 * @param lengthX		Länge der Wand in X-Richtung
	 * @param lengthY		Länge der Wand in Y-Richtung
	 * @param appearance	Die Appearance
	 */
	private void createWall(int x, int y, int lengthX, int lengthY, Appearance appearance) {
		Box box = new Box(parcours.getBlockSizeInM() / 2 * lengthX, parcours.getBlockSizeInM() / 2 * lengthY,
				WALL_HEIGHT, appearance);
		parcours.addObstacle(box, x + lengthX / 2.0f, y + lengthY / 2.0f);
	}

	/**
	 * Erzeugt ein Stück Fussboden Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 *
	 * @param x			Position in X-Richtung
	 * @param y			Position in X-Richtung
	 * @param lengthX	Länge der Fläche in X-Richtung
	 * @param lengthY	Länge der Fläche in Y-Richtung
	 * @param app		Aussehen des Bodens
	 */
	@SuppressWarnings("unused")
	private void createFloor(int x, int y, int lengthX, int lengthY, Appearance app) {
		Box box = new Box(parcours.getBlockSizeInM() / 2 * lengthX, parcours.getBlockSizeInM() / 2 * lengthY,
				World.PLAYGROUND_THICKNESS, app);
		parcours.addFloor(box, x + lengthX / 2.0f, y + lengthY / 2.0f, - World.PLAYGROUND_THICKNESS + 0.001f);
	}

	/**
	 * Erzeugt ein Stück Fussboden Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 *
	 * @param x		Position in X-Richtung
	 * @param y		Position in Y-Richtung
	 * @param app	Aussehen des Bodens
	 */
	private void createFloor(int x, int y, Appearance app) {
		Box box = new Box(parcours.getBlockSizeInM() * 0.5f, parcours.getBlockSizeInM() * 0.5f,
				World.PLAYGROUND_THICKNESS, app);
		parcours.addFloor(box, x + 0.5f, y + 0.5f, - World.PLAYGROUND_THICKNESS + 0.001f);
	}

	/**
	 * Erzeugt einen Fussboden aus einem Stück Alle Postionen sind keine
	 * Weltkoordinaten, sondern ganzen Einheiten, wie sie aus dem ASCII-File
	 * kommen
	 *
	 * @param app	Aussehen des Bodens
	 */
	private void createWholeFloor(Appearance app) {
		Box box = new Box(parcours.getWidthInBlocks() * parcours.getBlockSizeInM() * 0.5f,
			parcours.getHeightInBlocks() * parcours.getBlockSizeInM() * 0.5f, World.PLAYGROUND_THICKNESS, app);
		parcours.addFloor(box, ((float) parcours.getWidthInBlocks()) / 2, ((float) parcours.getHeightInBlocks()) / 2,
			- World.PLAYGROUND_THICKNESS - 0.005f);
	}


	/**
	 * Erzeugt eine Linie auf dem Boden Alle Postionen sind keine
	 * Weltkoordinaten, sondern ganzen Einheiten, wie sie aus dem ASCII-File
	 * kommen
	 *
	 * @param x				Position in X-Richtung
	 * @param y				Position in Y-Richtung
	 * @param points		Punkte der Linie
	 * @param appearance	Art der Linie
	 */
	private void createLine(int x, int y, float[] points, Appearance appearance) {
		// zwei Polygone (Deckel und Boden) mit N Ecken
		float[] p = new float[points.length];
		int stripCounts[] = { points.length / 3 };
		// Zähler
		int n = 0;

		for (n = 0; n < points.length; n++) {
			p[n] = points[n] * parcours.getBlockSizeInM();
		}

		createFloor(x, y, getAppearance(' '));

		// Polygone in darstellbare Form umwandeln
		GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
		gi.setCoordinates(p);
		gi.setStripCounts(stripCounts);

		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		gi.recomputeIndices();

		Stripifier st = new Stripifier();
		st.stripify(gi);
		gi.recomputeIndices();

		// Hinzufügen der Ober- und Unterseite des Linien-Shape3D
		Shape3D ls = new Shape3D();
		ls.addGeometry(gi.getGeometryArray());

		ls.setAppearance(appearance);

		parcours.addFloor(ls, x + 0.5f, y + 0.5f, 0.002f);
	}


	/**
	 * Erzeugt eine Säule, auch mit Lichtquelle obendrauf möglich
	 *
	 * @param x					X-Koordinate (bewegliches Objekt) oder X-Achse im Parcours (unbewegliches Objekt)
	 * @param y					Y-Koordinate (bewegliches Objekt) oder Y-Achse im Parcours (unbewegliches Objekt)
	 * @param diameter			Durchmesser der Säule
	 * @param height			Höhe der Säule
	 * @param bodyAppearance	Säulen-Appearance
	 * @param lightAppearance	Licht-Appearance oder null
	 * @param moveable			Soll das Objekt bewegbar sein?
	 */
	private void createPillar(float x, float y, float diameter, float height, Appearance bodyAppearance,
			Appearance lightAppearance, boolean moveable) {
		Cylinder pillar = new Cylinder(diameter / 2.0f, height, bodyAppearance);
//		pillar.setName("Object");
		pillar.setCapability(javax.media.j3d.Node.ALLOW_PICKABLE_WRITE);

		TransformGroup tg = new TransformGroup();
		tg.addChild(pillar);

		Transform3D translate = new Transform3D();

		/* Drehen auf vertikal */
		Transform3D rot = new Transform3D();
		rot.rotX(0.5 * Math.PI);
		translate.mul(rot);

		/* unteres Ende auf Fussboden "hochschieben" */
		translate.setTranslation(new Vector3f(0, 0, + height / 2.0f - 0.2f));
		tg.setTransform(translate);

		if (moveable) {
			parcours.addMoveableObstacle(tg, x, y);
		} else {
			parcours.addObstacle(tg, x + 0.5f, y + 0.5f);
		}
		if (lightAppearance != null) {
			createLight(new BoundingSphere(new Point3d(0d, 0d, 0d), 10d), new Color3f(1.0f, 1.0f, 0.9f),
					(int) x, (int) y, lightAppearance);
		}
	}

	/**
	 * Erzeugt ein bewegliches Objekt
	 *
	 * @param x	X-Koordinate
	 * @param y	Y-Koordinate
	 */
	public void createMovableObject(float x, float y) {
		createPillar(x, y, 0.03f, 0.08f, getAppearance('o'), null, true);
	}

	/**
	 * Fügt ein Licht ein
	 *
	 * @param pointLightBounds
	 * @param pointLightColor
	 * @param x					X-Koordinate
	 * @param y					Y-Koordinate
	 * @param appearance		Die Appearance
	 */
	private void createLight(BoundingSphere pointLightBounds, Color3f pointLightColor, int x, int y,
			Appearance appearance) {
		// Lichter bestehen aus dem echten Licht
		PointLight pointLight = new PointLight();
		pointLight.setColor(pointLightColor);
		pointLight.setPosition((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM(),
				LIGHTZ);
		pointLight.setInfluencingBounds(pointLightBounds);
		pointLight.setAttenuation(1f, 3f, 0f);
		pointLight.setEnable(true);
		parcours.addLight(pointLight);

		// und einer gelben Kugel, um es zu visualisieren
		Sphere lightSphere = new Sphere(0.07f);
		lightSphere.setAppearance(appearance);
		parcours.addLight(lightSphere, x + 0.5f, y + 0.5f, LIGHTZ);
	}

	/**
	 * Fügt eine BPS-Landmarke ein
	 *
	 * @param x				X-Koordinate [Parcours-Block]
	 * @param y				Y-Koordinate [Parcours-Block]
	 * @param appearance	Die Appearance
	 */
	private void createBPSBeacon(int x, int y, Appearance appearance) {
		PointLight pointBPSLight = new PointLight();
//		pointBPSLight.setColor(new Color3f(0.5f, 0.5f, 0.5f));
		pointBPSLight.setPosition((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM(),
				BPS.BPSZ);

//		pointBPSLight.setInfluencingBounds(new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 1.0));
//		pointBPSLight.setAttenuation(1f, 3f, 0f);
		pointBPSLight.setEnable(true);
		parcours.addBPSLight(pointBPSLight);

		Sphere bpsSphere = new Sphere(0.02f);
		bpsSphere.setAppearance(appearance);
		parcours.addBPSLight(bpsSphere, x + 0.5f, y + 0.5f, BPS.BPSZ);
	}

	/**
	 * Prüft die angrenzenden Felder (ohne diagonalen), ob mindestens eines davon den übergebenen Wert hat
	 *
	 * @param x	X-Koordinate des mittelfeldes
	 * @param y	Y-Koordinate des mittelfeldes
	 * @param c	Der zu suchende Feldtyp
	 * @return -1 wenn kein Feld den Wert hat. Wenn er einen Nachbarn findet
	 *         dann die Richtung in Grad. 0 = (x=1, y=0) ab da im Uhrzeigersinn
	 */
	private int checkNeighbours(int x, int y, char c) {
		if ((y > 0) && (parcoursMap[x][y - 1] == c)) {
			return 90;
		}
		if ((y < parcours.getHeightInBlocks() - 1) && (parcoursMap[x][y + 1] == c)) {
			return 270;
		}
		if ((x > 0) && (parcoursMap[x - 1][y] == c)) {
			return 180;
		}
		if ((x < parcours.getWidthInBlocks() - 1) && (parcoursMap[x + 1][y] == c)) {
			return 0;
		}

		return -1;
	}

	/** Liest die parcourMap ein und baut daraus einen Parcour zusammen */
	public void parse() {
		int l;
		int d;

		if (parcoursMap != null) {
			for (int y = 0; y < parcours.getHeightInBlocks(); y++) {
				for (int x = 0; x < parcours.getWidthInBlocks(); x++) {
					switch (parcoursMap[x][y]) {
					case 'X':
						createWall(x, y, 1, 1, getAppearance(parcoursMap[x][y]));
						break;
					case '=':
						l = 0;
						d = x;
						// ermittle die Länge der zusammenhängenden Wand
						while ((d < parcours.getWidthInBlocks()) && (parcoursMap[d][y] == '=')) {
							parcoursMap[d][y] = 'O';	// Feld ist schon bearbeitet
							l++;	// Länge hochzählen
							d++;
						}
						createWall(x, y, l, 1, getAppearance('='));
						break;
					case 'O':
						// Feld ist schon bearbeitet
						break;
					case '#':
						l = 0;
						d = y;
						// ermittle die Länge der zusammenhängenden Wand
						while ((d < parcours.getHeightInBlocks()) && (parcoursMap[x][d] == '#')) {
							parcoursMap[x][d] = 'O';	// Feld ist schon bearbeitet
							l++;	// Länge hochzählen
							d++;
						}
						createWall(x, y, 1, l, getAppearance('#'));
						break;
					case '*':	// Licht
						createPillar(x, y, 0.1f, LIGHTZ, getAppearance('X'), getAppearance('*'), false);
						// sind wir im Startbereich ?
						if (checkNeighbours(x, y, '.') != -1) {
							createFloor(x, y, getAppearance('.'));
						} else {
							createFloor(x, y, getAppearance(' '));
						}
						break;
					case 'o':	// bewegliches Objekt
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					case 'l':	// Landmarke
//						if (Beacon.checkParcoursPosition(this.parcours, x, y)) {
							createBPSBeacon(x, y, getAppearance('l'));
//						} else {
//							lg.warn("Parcours enthält Landmarke an Position (" + x + "|" + y + "), " +
//							"dort ist aber keine Landmarke zugelassen, ignoriere sie.");
//						}
						break;
					case '.':
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						break;
					case ' ':
						break;
					case 'L':
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						parcours.addHole(x, y);
						break;
					case '0':
						parcours.setStartPosition(0, x, y);
						parcours.setStartHeading(0, checkNeighbours(x, y, '.'));
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						break;
					case '1':
						parcours.setStartPosition(1, x, y);
						parcours.setStartHeading(1, checkNeighbours(x, y, '.'));
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						break;
					case '2':
						parcours.setStartPosition(2, x, y);
						parcours.setStartHeading(2, checkNeighbours(x, y, '.'));
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						break;
					case 'Z':
						// this.parcours.setFinishPosition(x, y);
						parcours.addFinishPosition(x, y);
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						break;
					case '-':
						createLine(x, y, LINE_HORIZ, getAppearance(parcoursMap[x][y]));
						break;
					case '|':
						createLine(x, y, LINE_VERT, getAppearance(parcoursMap[x][y]));
						break;
					case '$':
						parcours.setStartPosition(1, x, y);
						parcours.setStartHeading(1, checkNeighbours(x, y, '.'));
						createFloor(x, y, getAppearance(parcoursMap[x][y]));
						createLine(x, y, LINE_VERT, getAppearance(parcoursMap[x][y]));
						break;
					case '/':
						createLine(x, y, LINE_CORNER_NW, getAppearance(parcoursMap[x][y]));
						break;
					case '\\':
						createLine(x, y, LINE_CORNER_NE, getAppearance(parcoursMap[x][y]));
						break;
					case '+':
						createLine(x, y, LINE_CORNER_SW, getAppearance(parcoursMap[x][y]));
						break;
					case '~':
						createLine(x, y, LINE_CORNER_SE, getAppearance(parcoursMap[x][y]));
						break;
					case '[':
						createLine(x, y, LINE_CROSSING_X, getAppearance(parcoursMap[x][y]));
						break;
					case 'T':
						createLine(x, y, LINE_CROSSING_T, getAppearance(parcoursMap[x][y]));
						break;
					case ']':
						createLine(x, y, LINE_CROSSING_T_MIRR, getAppearance(parcoursMap[x][y]));
						break;
					case '}':
						createLine(x, y, LINE_CROSSING_T_ROT_UNCLOCKWISE, getAppearance(parcoursMap[x][y]));
						break;
					case '{':
						createLine(x, y, LINE_CROSSING_T_ROT_CLOCKWISE, getAppearance(parcoursMap[x][y]));
						break;
					case '!':
						createLine(x, y, LINE_BREAK_VERT, getAppearance(parcoursMap[x][y]));
						break;
					case '%':
						createLine(x, y, LINE_BREAK_HOR, getAppearance(parcoursMap[x][y]));
						break;
					case '(':
						createLine(x, y, LINE_VERT, getAppearance(parcoursMap[x][y]));
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					case '_':
						createLine(x, y, LINE_HORIZ, getAppearance(parcoursMap[x][y]));
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					case 'p':
						createLine(x, y, LINE_CORNER_NW, getAppearance(parcoursMap[x][y]));
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					case 'q':
						createLine(x, y, LINE_CORNER_NE, getAppearance(parcoursMap[x][y]));
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					case 'b':
						createLine(x, y, LINE_CORNER_SW, getAppearance(parcoursMap[x][y]));
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					case 'd':
						createLine(x, y, LINE_CORNER_SE, getAppearance(parcoursMap[x][y]));
						createMovableObject((x + 0.5f) * parcours.getBlockSizeInM(), (y + 0.5f) * parcours.getBlockSizeInM());
						break;
					default:
						lg.warn("Unbekannter Typ '" + (char)parcoursMap[x][y] + "' in Parcours an Position (" + x + "|" + y + ") gefunden, wird ignoriert.");
						break;
						/** Achtung: Bei Aenderungen/Erweiterung die Liste verfuegbarer Kartenbauelemente in ctSim/parcours/template.xml und ctSim/develop/documentation/map-parts.md bitte ebenfalls aktualisieren! */
					}
				}
			}

			/* für den Rest Boden aus einem Stück einfügen */
			createWholeFloor(getAppearance(' '));

			/* Hat mit dem Einzeichnen des Wegs bis zum Ziel zu tun; sollte ordentlich integriert werden:
			 * Menüeintrag in GUI, der das Einzeichnen ein-/ausschaltet. Aus Gründen der Klarheit sollten
			 * die Linien vorher ihre eigene BranchGroup bekommen
			 */


//			for (int i=0; i<Parcours.BOTS; i++){
//				double dist=this.parcours.getShortestDistanceToFinish(this.parcours.getStartPosition(i));
//				if (dist>=0)
//					System.out.println("Distanz zum Ziel von Startpunkt " + i + " = " + dist + " m");
//				else
//					System.out.println("Kein Weg zum Ziel von Startpunkt "+i);
//
//				// finde die kürzeste Verbindung Vector<TurningPoint>
//				shortestPath=this.parcours.getShortestPath(this.parcours.getStartPosition(i));
//				if(shortestPath==null || shortestPath.size()<2){
//				}else{
//					for(int q=1;q<shortestPath.size();q++){
//					createLine(0,0,shortestPath.get(q-1).returnLineTo(shortestPath.get(q)),getAppearance('-'));
//					}
//				}
//			}

		}
	}

	/**
	 * @return Liefert das soeben aufgebaute Parcours-Objekt zurück
	 */
	public Parcours getParcours() {
		return parcours;
	}

	/**
	 * <p>
	 * Lädt einen Parcours aus einer InputSource.
	 *
	 * @param source	Xerces-InputSource-Objekt, aus dem der Parcours geholt werden kann.
	 *            		Der Sinn ist, dass beliebige Eingabequellen übergeben werden können
	 *            		und daher nicht mehr nur aus Dateien, sondern auch aus Strings oder
	 *            		von sonstwo gelesen werden kann.</p>
	 *
	 * @param resolver	Der zu verwendende Xerces-EntityResolver, oder <code>null</code>,
	 *            		wenn der Standard-Resolver verwendet werden soll. Der DocumentBuilder,
	 *            		der dieser Methode zugrundeliegt, verwendet den Resolver während dem
	 *            		Verarbeiten der im XML vorkommenden "system identifier" und
	 *            		"public identifier". Diese treten in unseren Parcoursdateien nur an
	 *            		einer Stelle auf, nämlich in Zeile 2:
	 *            		<code><!DOCTYPE collection SYSTEM "parcours.dtd"></code>.
	 *           		Der system identifier ist dabei <code>parcours.dtd</code>.
	 *            		Für Parcours-Dateien ist kein Resolver nötig (= es kann <code>null</code>
	 *            		übergeben werden), weil sie im gleichen Verzeichnis liegen wie die Datei
	 *            		parcours.dtd. Für Parcours, die aus XML-Strings gelesen werden, ist ein
	 *            		Resolver nötig, da der Parser sonst nur im ctSim-Verzeichnis sucht (nicht
	 *            		im Verzeichnis ctSim/parcours), und daher die Datei ctSim/parcours/parcours.dtd
	 *            		nicht findet.
	 *
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public void loadParcours(InputSource source, EntityResolver resolver)
			throws SAXException, IOException, ParserConfigurationException {
		// Ein DocumentBuilder liest ein XML-File ein
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;

		try {
			builder = factory.newDocumentBuilder();
			if (resolver != null) {
				builder.setEntityResolver(resolver);
			}
			// einlesen und umwandeln in ein Document
			Document doc = builder.parse(source);

			// und anfangen mit dem Abarbeiten

			// als erstes suchen wir uns den Parcours-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while ((n != null) && (!n.getNodeName().equals("parcours"))) {
				n = n.getNextSibling();
			}
			// jetzt haben wir ihn

			int y = 0;	// Anzahl der Zeilen im File
			int x = 0;	// Anzahl der Spalten im File

			// eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			if (n == null) {
				throw new SAXException("kein Node gefunden!");
			}
			NodeList children = n.getChildNodes();

			// Anzahl der Zeilen und Spalten bestimmen
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeName().equals("line")) {
					y++;
					if (x < child.getChildNodes().item(0).getNodeValue().length()) {
						x = child.getChildNodes().item(0).getNodeValue().length();
					}
				}
			}

			// Parcors vorbereiten
			parcours.setDimX(x);
			parcours.setDimY(y);

			// und eine Map anlegen
			parcoursMap = new int[x][y];

			parcours.setParcoursMap(parcoursMap);

			x = 0;
			y = 0;
			// ParcoursMap aufbauen
			for (int i = children.getLength() - 1; i >= 0; i--) {
				Node child = children.item(i);
				if (child.getNodeName().equals("line")) {
					char c[] = child.getChildNodes().item(0).getNodeValue().toCharArray();
					for (x = 0; x < c.length; x++) {
						parcoursMap[x][y] = c[x];
					}
					y++;
				}
			}

			// *** Appearances aus dem Document lesen ***

			// suchen wir uns den Otptics-Block ...
			n = doc.getDocumentElement().getFirstChild();
			while ((n != null) && (!n.getNodeName().equals("optics"))) {
				n = n.getNextSibling();
			}
			// ... jetzt haben wir ihn

			// eine Liste aller Kinder des Parcours-Eintrags organsisieren
			if (n == null) {
				throw new SAXException("kein Node gefunden!");
			}
			children = n.getChildNodes();

			// HashMap mit den Apearances aufbauen
			for (int i = 0; i < children.getLength() - 1; i++) {
				Node appearance = children.item(i);
				if (appearance.getNodeName().equals("appearance")) {
					// Zuerst den Type extrahieren
					char item = appearance.getAttributes().getNamedItem("type").getNodeValue().toCharArray()[0];

					String texture = null;
					String clone = null;

					HashMap<String, String> colors = Misc.newMap();

					NodeList features = appearance.getChildNodes();
					for (int j = 0; j < features.getLength(); j++) {
						if (features.item(j).getNodeName().equals("texture")) {
							texture = features.item(j).getChildNodes().item(0).getNodeValue();
						}
						if (features.item(j).getNodeName().equals("color")) {
							colors.put(features.item(j).getAttributes().getNamedItem("type").getNodeValue(),
								features.item(j).getChildNodes().item(0).getNodeValue());
						}
						if (features.item(j).getNodeName().equals("clone")) {
							clone = features.item(j).getChildNodes().item(0).getNodeValue();
						}
					}

					addAppearance(item, colors, texture, clone);
				}
			}

			// soweit fertig...
			parse();	// Parcours zusammenbauen

		} catch (SAXException e) {
			lg.warn(e, "Probleme beim Parsen des XML");
			throw e;
		} catch (IOException e) {
			lg.warn(e, "Probleme beim Parsen des XML");
			throw e;
		} catch (ParserConfigurationException e) {
			lg.warn(e, "Probleme beim Parsen des XML");
			throw e;
		}
	}

	/**
	 * Liefert eine Appearance aus der Liste zurück
	 *
	 * @param key	Der Schlüssel, mit dem sie abgelegt wurde
	 * @return Die Appearance
	 */
	private Appearance getAppearance(int key) {
		Appearance app = (Appearance) this.appearances.get((char) key);
		if (app == null) {
			lg.warn("Appearance für '" + (char) key + "' nicht gefunden!");
		}
		return app;
	}

	/**
	 * Fügt die der Liste hinzu

	 * @param item			der Key, iunter dem diese Apperance abgelegt wird
	 * @param colors		HashMap mit je Farbtyp und ASCII-Represenation der Farbe
	 * @param textureFile	der Name des Texture-Files
	 * @param clone			Referenz auf einen schon bestehenden Eintrag, der geclonet werden soll
	 */
	private void addAppearance(char item, HashMap colors, String textureFile, String clone) {
		if (clone != null) {
			appearances.put(item, appearances.get(clone.toCharArray()[0]));
			return;
		}

		Appearance appearance = new Appearance();

		if (colors != null) {
			Material mat = new Material();

			Iterator it = colors.keySet().iterator();
			while (it.hasNext()) {
				String colorType = (String) it.next();
				String colorName = (String) colors.get(colorType);

				if (colorType.equals("ambient")) {
					mat.setAmbientColor(new Color3f(Color.decode(colorName)));
				}
				if (colorType.equals("diffuse")) {
					mat.setDiffuseColor(new Color3f(Color.decode(colorName)));
				}
				if (colorType.equals("specular")) {
					mat.setSpecularColor(new Color3f(Color.decode(colorName)));
				}
				if (colorType.equals("emmissive")) {
					mat.setEmissiveColor(new Color3f(Color.decode(colorName)));
				}
			}
			appearance.setMaterial(mat);
		}

		if (textureFile != null) {
			TexCoordGeneration tcg = new TexCoordGeneration(TexCoordGeneration.OBJECT_LINEAR,
					TexCoordGeneration.TEXTURE_COORDINATE_3, new Vector4f(1.0f, 1.0f, 0.0f, 0.0f),
					new Vector4f(0.0f, 1.0f, 1.0f, 0.0f), new Vector4f(1.0f, 0.0f, 1.0f, 0.0f));
			appearance.setTexCoordGeneration(tcg);

			try {
				TextureLoader loader = new TextureLoader(ClassLoader.getSystemResource(textureFile), null);
				Texture2D texture = (Texture2D) loader.getTexture();
				texture.setBoundaryModeS(Texture.WRAP);
				texture.setBoundaryModeT(Texture.WRAP);

				// mache die Textur lesbar
				texture.setCapability(Texture.ALLOW_IMAGE_READ);
				ImageComponent[] imgs = texture.getImages();
				for (int i = 0; i < imgs.length; i++) {
					imgs[i].setCapability(ImageComponent.ALLOW_IMAGE_READ);
				}

				appearance.setTexture(texture);
				appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);

			} catch (Exception e) {
				lg.warn(e, "Probleme beim Laden der Texturdatei '%s'", textureFile);
			}
		}

		appearances.put(item, appearance);
	}

	/**
	 * Debug-Methode
	 *
	 * @param node	Node
	 * @param out	Output-Stream
	 */
	static void print(Node node, PrintStream out) {
		int type = node.getNodeType();
		switch (type) {
		case Node.ELEMENT_NODE:
			out.print("<" + node.getNodeName());
			NamedNodeMap attrs = node.getAttributes();
			int len = attrs.getLength();
			for (int i = 0; i < len; i++) {
				Attr attr = (Attr) attrs.item(i);
				out.print(" " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"");
			}
			out.print('>');
			NodeList children = node.getChildNodes();
			len = children.getLength();
			for (int i = 0; i < len; i++) {
				print(children.item(i), out);
			}
			out.print("</" + node.getNodeName() + ">");
			break;
		case Node.ENTITY_REFERENCE_NODE:
			out.print("&" + node.getNodeName() + ";");
			break;
		case Node.CDATA_SECTION_NODE:
			out.print("<![CDATA[" + node.getNodeValue() + "]]>");
			break;
		case Node.TEXT_NODE:
			out.print(node.getNodeValue());
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			out.print("<?" + node.getNodeName());
			String data = node.getNodeValue();
			if (data != null && data.length() > 0) {
				out.print(" " + data);
			}
			out.println("?>");
			break;
		}
	}
}
