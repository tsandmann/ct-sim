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
import javax.vecmath.Vector4f;

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
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

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
 * @author bbe (bbe@heise.de)
 */
public class ParcoursLoader {
	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.ParcoursLoader");

	/** Z-Koorndinate der Lampen */
	public static final float LIGHTZ = 0.5f;

	/**
	 * Linienbreite
	 */
	public static final float LINEWIDTH = 0.1f;

	/**
	 * Horizontales Liniensegment
	 */
	public static final float[] LINE_HORIZ = {
		-0.5f,	0f - LINEWIDTH/2,0f,
		0.5f,	0f - LINEWIDTH/2,0f,
		0.5f,	0f + LINEWIDTH/2,0f,
		-0.5f,	0f + LINEWIDTH/2,0f,
		-0.5f,	0f - LINEWIDTH/2,0f,
	};
	
	/**
	 * Vertikales Liniensegment
	 */
	public static final float[] LINE_VERT = {
		0f - LINEWIDTH/2,	-0.5f,	0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,	0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.5f,	0f,	// Lange Linie hoch
		0f - LINEWIDTH/2,	0.5f,	0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	-0.5f,	0f,	// lange Linie runter
	};

	/**
	 * Linie -- Suedostecke
	 */
	public static final float[] LINE_CORNER_SE = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Lange Linie hoch
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// Lange Linie nach rechts
		0.5f,				0.0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		0f -LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// Lange Linie nach links
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Lange Linie nach unten
	};
	
	/**
	 * Linie -- Suedwestecke
	 */
	public static final float[] LINE_CORNER_SW = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// Lange Linie hoch
		-0.5f,				0.0f + LINEWIDTH/2,	0f,	// Lange Linie nach links
		-0.5f,				0.0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f -LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Lange Linie nach links
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Lange Linie nach unten
	};
	
	/**
	 * Linie -- Nordwestecke
	 */
	public static final float[] LINE_CORNER_NW = {
		-0.5f,				0.0f + LINEWIDTH/2,	0f,	// Start Links oben
		-0.5f,				0.0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f +LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Lange Linie nach rechts
		0f +LINEWIDTH/2,	0.5f,				0f,	// Lange Linie nach oben
		0f -LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f -LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// lange Linie nach unten
		-0.5f,				0.0f + LINEWIDTH/2,	0f,	// Lange Linie nach links
	};

	/**
	 * Linie -- Nordostecke
	 */
	public static final float[] LINE_CORNER_NE = {
		0f +LINEWIDTH/2 , 0.5f               ,0f,	// Start oben rechts
		0f -LINEWIDTH/2 , 0.5f               ,0f,	// kurze Linie nach links
		0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,	// lange Linie nach unten
		0.5f			, 0.0f - LINEWIDTH/2 ,0f,	// Lange Linie nach rechts
		0.5f			, 0.0f + LINEWIDTH/2 ,0f,	// kurze Linie nach oben
		0f +LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,	// lange Linie nach links
		0f +LINEWIDTH/2 , 0.5f				 ,0f,	// lange Linie nach oben
	};

	/**
	 * X-Kreuzung
	 */
	public static final float[] LINE_CROSSING_X = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie hoch bis Mitte
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// Lange Linie nach rechts
		0.5f,				0.0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		0f + LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// Linie bis Mitte
		0f + LINEWIDTH/2,	0.5f,				0f,	// Linie bis Hoch
		0f - LINEWIDTH/2,	0.5f,				0f,	// kurze Linie nach links
		0f - LINEWIDTH/2,	0.0f + LINEWIDTH/2,	0f,	// Linie runter bis Mitte
		-0.5f,				0f + LINEWIDTH/2,	0f,	// Linie nach links
		-0.5f,				0f - LINEWIDTH/2,	0f,	// kurze Linie runter
		0f - LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie nach rechts bis Mitte
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Kreuz schliessen zum Ausgangspunkt
	};
	
	/**
	 * T-Kreuzung, Ausrichtung wie das T selbst, also Linie geht nach unten
	 */
	public static final float[] LINE_CROSSING_T = {
		0f - LINEWIDTH/2,	-0.5f,				0f,	// Start unten links
		0f + LINEWIDTH/2,	-0.5f,				0f,	// kurze Linie nach rechts
		0f + LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie hoch bis Mitte
		0.5f,				0.0f - LINEWIDTH/2,	0f,	// Lange Linie nach rechts
		0.5f,				0.0f + LINEWIDTH/2,	0f,	// kurze Linie hoch
		-0.5f,			0.0f + LINEWIDTH/2,		0f,	// lange Linie nach links
		-0.5f,			0f - LINEWIDTH/2,		0f,	// kurze Linie runter
		0f - LINEWIDTH/2,	0.0f - LINEWIDTH/2,	0f,	// Linie nach rechts bis Mitte
		0f - LINEWIDTH/2,	-0.5f,				0f,	// T schliessen zum Ausgangspunkt
	};
	
	/**
	 * gespiegelte T-Kreuzung, Ausrichtung wie gespiegeltes, also Linie geht nach oben
	 */
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
	
	/**
	 * T-Kreuzung, 90 Grad gedreht entgegen Uhrzeigersinn, also Linie geht nach rechts
	 */
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
	
	/**
	 * T-Kreuzung, 90 Grad gedreht in Uhrzeigersinn, also Linie geht nach links
	 */
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
	
	/**
	 * Linie -- mit Unterbrechung vertikal
	 * besteht aus 2 untereinander liegenden Teillinien
	 */
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
	
	/**
	 * Linie -- mit Unterbrechung horizontal
	 * besteht aus 2 nebeneinander liegenden Teillinien
	 */ 
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
	
	/** Wand-Hoehe */
	private static final float WALL_HEIGHT = 0.2f;

	/** Verwaltet alle Aussehen */
	@SuppressWarnings("unchecked")
	HashMap appearances = new HashMap();

	/** Parcours-Map */
	private int[][] parcoursMap = null;

	/** Der eigentliche Parcours */
	private Parcours parcours;

	/**
	 * Neuen ParcoursLoader instantiieren
	 */
	public ParcoursLoader() {
		super();
		this.parcours = new Parcours();
	}

	/**
	 * Erzeugt ein Wandsegment Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x
	 *            Position in X-Richtung
	 * @param y
	 *            Position in X-Richtung
	 * @param lengthX
	 *            Laenge der Wand in X-Richtung
	 * @param lengthY
	 *            Laenge der Wand in Y-Richtung
	 * @param appearance
	 *            Die Appearance
	 */
	private void createWall(int x, int y, int lengthX, int lengthY,
			Appearance appearance) {
		Box box = new Box(parcours.getBlockSizeInM() / 2 * lengthX, parcours
				.getBlockSizeInM()
				/ 2 * lengthY, WALL_HEIGHT, appearance);
		parcours.addObstacle(box, x + lengthX / 2.0f, y + lengthY / 2.0f);
	}

	/**
	 * Erzeugt ein Stueck Fussboden Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x
	 *            Position in X-Richtung
	 * @param y
	 *            Position in X-Richtung
	 * @param lengthX
	 *            Laenge der Flaeche in X-Richtung
	 * @param lengthY
	 *            Laenge der Flaeche in Y-Richtung
	 * @param app
	 *            Aussehen des Bodens
	 */
	@SuppressWarnings("unused")
	private void createFloor(int x, int y, int lengthX, int lengthY,
			Appearance app) {
		Box box = new Box(this.parcours.getBlockSizeInM() / 2 * lengthX,
				this.parcours.getBlockSizeInM() / 2 * lengthY,
				World.PLAYGROUND_THICKNESS, app);
		this.parcours.addFloor(box, x + lengthX / 2.0f, y + lengthY / 2.0f,
				-World.PLAYGROUND_THICKNESS + 0.001f);
	}

	/**
	 * Erzeugt ein Stueck Fussboden Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x
	 *            Position in X-Richtung
	 * @param y
	 *            Position in Y-Richtung
	 * @param app
	 *            Aussehen des Bodens
	 */
	private void createFloor(int x, int y, Appearance app) {
		Box box = new Box(this.parcours.getBlockSizeInM() * 0.5f, this.parcours
				.getBlockSizeInM() * 0.5f, World.PLAYGROUND_THICKNESS, app);
		this.parcours.addFloor(box, x + 0.5f, y + 0.5f,
				-World.PLAYGROUND_THICKNESS + 0.001f);
	}

	/**
	 * Erzeugt einen Fussboden aus einem Stueck Alle Postionen sind keine
	 * Weltkoordinaten, sondern ganzen Einheiten, wie sie aus dem ASCII-File
	 * kommen
	 * 
	 * @param app
	 *            Aussehen des Bodens
	 */
	private void createWholeFloor(Appearance app) {
		Box box = new Box(this.parcours.getWidthInBlocks()
				* this.parcours.getBlockSizeInM() * 0.5f, this.parcours
				.getHeightInBlocks()
				* this.parcours.getBlockSizeInM() * 0.5f,
				World.PLAYGROUND_THICKNESS, app);
		this.parcours.addFloor(box,
				((float) this.parcours.getWidthInBlocks()) / 2,
				((float) this.parcours.getHeightInBlocks()) / 2,
				-World.PLAYGROUND_THICKNESS - 0.005f); // +0.001f);
	}


	/**
	 * Erzeugt eine Linie auf dem Boden Alle Postionen sind keine
	 * Weltkoordinaten, sondern ganzen Einheiten, wie sie aus dem ASCII-File
	 * kommen
	 * 
	 * @param x
	 *            Position in X-Richtung
	 * @param y
	 *            Position in Y-Richtung
	 * @param points
	 *            Punkte der Linie
	 * @param appearance
	 *            Art der Linie
	 */
	private void createLine(int x, int y, float[] points, Appearance appearance) {
		// zwei Polygone (Deckel und Boden) mit N Ecken
		float[] p = new float[points.length];
		int stripCounts[] = { points.length / 3 };
		// Zaehler
		int n = 0;

		for (n = 0; n < points.length; n++)
			p[n] = points[n] * this.parcours.getBlockSizeInM();

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

		// Hinzufuegen der Ober- und Unterseite des Linien-Shape3D
		Shape3D ls = new Shape3D();
		ls.addGeometry(gi.getGeometryArray());

		ls.setAppearance(appearance);

		this.parcours.addFloor(ls, x + 0.5f, y + 0.5f, 0.002f);
	}


	/**
	 * Erzeugt eine Sauele mit Lichtquelle obendrauf
	 * 
	 * @param x
	 *            X-Koordinate
	 * @param y
	 *            Y-Koordinate
	 * @param wallAppearance
	 *            Saeulen-Appearance
	 * @param lightAppearance
	 *            Licht-Appearance
	 */
	private void createPillar(int x, int y, Appearance wallAppearance,
			Appearance lightAppearance) {
		Cylinder pillar = new Cylinder(0.05f, 0.5f, wallAppearance);
		pillar.setPickable(true);

		Transform3D translate = new Transform3D();
		translate.rotX(0.5 * Math.PI);
		TransformGroup tg = new TransformGroup(translate);
		tg.setCapability(javax.media.j3d.Node.ENABLE_PICK_REPORTING);
		tg.addChild(pillar);

		this.parcours.addObstacle(tg, x + 0.5f, y + 0.5f);
		createLight(new BoundingSphere(new Point3d(0d, 0d, 0d), 10d),
				new Color3f(1.0f, 1.0f, 0.9f), x, y, lightAppearance);
	}

	/**
	 * Fuegt ein Licht ein
	 * 
	 * @param pointLightBounds
	 * @param pointLightColor
	 * @param x
	 *            X-Koordinate
	 * @param y
	 *            Y-Koordinate
	 * @param appearance
	 *            Die Appearance
	 */
	private void createLight(BoundingSphere pointLightBounds,
			Color3f pointLightColor, int x, int y, Appearance appearance) {
		// Lichter bestehen aus dem echten Licht
		PointLight pointLight = new PointLight();
		pointLight.setColor(pointLightColor);
		pointLight.setPosition((x + 0.5f) * this.parcours.getBlockSizeInM(),
				(y + 0.5f) * this.parcours.getBlockSizeInM(), LIGHTZ);
		pointLight.setInfluencingBounds(pointLightBounds);
		pointLight.setAttenuation(1f, 3f, 0f);
		pointLight.setEnable(true);
		this.parcours.addLight(pointLight);

		// Und einer gelben Kugel, um es zu visulaisieren
		Sphere lightSphere = new Sphere(0.07f);
		lightSphere.setAppearance(appearance);
		this.parcours.addLight(lightSphere, x + 0.5f, y + 0.5f, 0.5f);
	}

	/**
	 * Prueft die angrenzenden Felder (ohne diagonalen), ob mindestens eines
	 * davon den uebergebenen Wert hat
	 * 
	 * @param x
	 *            X-Koordinate des mittelfeldes
	 * @param y
	 *            Y-Koordinate des mittelfeldes
	 * @param c
	 *            Der zu suchende Feldtyp
	 * 
	 * @return -1 wenn kein Feld den Wert hat. Wenn er einen Nachbarn findet
	 *         dann die Richtung in Grad. 0 = (x=1, y=0) ab da im Uhrzeigersinn
	 */
	private int checkNeighbours(int x, int y, char c) {
		if ((y > 0) && (this.parcoursMap[x][y - 1] == c))
			return 90;
		if ((y < this.parcours.getHeightInBlocks() - 1)
				&& (this.parcoursMap[x][y + 1] == c))
			return 270;
		if ((x > 0) && (this.parcoursMap[x - 1][y] == c))
			return 180;
		if ((x < this.parcours.getWidthInBlocks() - 1)
				&& (this.parcoursMap[x + 1][y] == c))
			return 0;

		return -1;
	}



	/**
	 * Liest die parcourMap ein und baut daraus einen Parcour zusammen
	 */
	public void parse() {
		int l;
		int d;

		if (this.parcoursMap != null) {

			for (int y = 0; y < this.parcours.getHeightInBlocks(); y++) {
				for (int x = 0; x < this.parcours.getWidthInBlocks(); x++)
					switch (this.parcoursMap[x][y]) {
					case 'X':
						createWall(x, y, 1, 1,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '=':
						l = 0;
						d = x;
						// ermittle die Laenge der zusammenhaengenden Wand
						while ((d < this.parcours.getWidthInBlocks())
								&& (this.parcoursMap[d][y] == '=')) {
							this.parcoursMap[d][y] = 'O'; // Feld ist schon
															// bearbeitet
							l++; // Laenge hochzaeheln
							d++;
						}
						createWall(x, y, l, 1, getAppearance('='));
						break;
					case '#':
						l = 0;
						d = y;
						// ermittle die Laenge der zusammenhaengenden Wand
						while ((d < this.parcours.getHeightInBlocks())
								&& (this.parcoursMap[x][d] == '#')) {
							this.parcoursMap[x][d] = 'O'; // Feld ist schon
															// bearbeitet
							l++; // Laenge hochzaeheln
							d++;
						}
						createWall(x, y, 1, l, getAppearance('#'));
						break;
					case '*':
						createPillar(x, y, getAppearance('X'),
								getAppearance('*'));
						// Sind wir im Startbereich
						if (checkNeighbours(x, y, '.') != -1)
							createFloor(x, y, getAppearance('.'));
						else
							createFloor(x, y, getAppearance(' '));
						break;
					case '.':
						// TODO Boden optimieren, kacheln zusammenfassen
						createFloor(x, y, getAppearance(this.parcoursMap[x][y]));
						break;
					case ' ':
						// TODO Boden optimieren, kacheln zusammenfassen
						// createFloor(x,
						// y,getAppearance(this.parcoursMap[x][y]));
						break;
					case 'L':
						// TODO Boden optimieren, kacheln zusammenfassen
						createFloor(x, y, getAppearance(this.parcoursMap[x][y]));
						parcours.addHole(x, y);
						break;

					case '0':
						this.parcours.setStartPosition(0, x, y);
						createFloor(x, y, getAppearance(this.parcoursMap[x][y]));
						break;
					case '1':
						this.parcours.setStartPosition(1, x, y);
						this.parcours.setStartHeading(1, checkNeighbours(x, y,
								'.'));
						createFloor(x, y, getAppearance(this.parcoursMap[x][y]));
						break;
					case '2':
						this.parcours.setStartPosition(2, x, y);
						this.parcours.setStartHeading(2, checkNeighbours(x, y,
								'.'));
						createFloor(x, y, getAppearance(this.parcoursMap[x][y]));
						break;
					case 'Z':
						// this.parcours.setFinishPosition(x,y);
						this.parcours.addFinishPosition(x, y);
						createFloor(x, y, getAppearance(this.parcoursMap[x][y]));
						break;

					case '-':
						createLine(x, y, LINE_HORIZ,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '|':
						createLine(x, y, LINE_VERT,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '/':
						createLine(x, y, LINE_CORNER_SE,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '\\':
						createLine(x, y, LINE_CORNER_SW,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '+':
						createLine(x, y, LINE_CORNER_NE,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '~':
						createLine(x, y, LINE_CORNER_NW,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '[':
						createLine(x, y, LINE_CROSSING_X,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case 'T':
						createLine(x, y, LINE_CROSSING_T,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case ']':
						createLine(x, y, LINE_CROSSING_T_MIRR,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '}':
						createLine(x, y, LINE_CROSSING_T_ROT_UNCLOCKWISE,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '{':
						createLine(x, y, LINE_CROSSING_T_ROT_CLOCKWISE,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '!':
						createLine(x, y, LINE_BREAK_VERT,
								getAppearance(this.parcoursMap[x][y]));
						break;
					case '%':
						createLine(x, y, LINE_BREAK_HOR,
								getAppearance(this.parcoursMap[x][y]));
						break;
					}

			}
			// TODO: Hier wird testweise ein Boden aus einem Stueck eingefuegt!
			createWholeFloor(getAppearance(' '));
			// TODO Hat mit dem Einzeichnen des Wegs bis zum Ziel zu tun; sollte
			// ordentlich integriert werden: Menueeintrag in GUI, der das
			// Einzeichnen ein-/ausschaltet. Aus Gruenden der Klarheit sollten
			// die Linien vorher ihre eigene BranchGroup bekommen
			/*
			 * for (int i=0; i<Parcours.BOTS; i++){ double dist=
			 * this.parcours.getShortestDistanceToFinish
			 * (this.parcours.getStartPosition(i));
			 * 
			 * if (dist>=0)
			 * System.out.println("Distanz zum Ziel von Starpunkt "+
			 * i+" = "+dist+" m"); else
			 * System.out.println("Kein Weg zum Ziel von Starpunkt "+i);
			 * 
			 * 
			 * // finde die kuerzeste Verbindung Vector<TurningPoint>
			 * shortestPath=
			 * this.parcours.getShortestPath(this.parcours.getStartPosition(i));
			 * 
			 * if(shortestPath==null || shortestPath.size()<2){ } else{ for(int
			 * q=1;q<shortestPath.size();q++){
			 * createLine(0,0,shortestPath.get(q-
			 * 1).returnLineTo(shortestPath.get(q)),getAppearance('-')); } } }
			 */
		}
	}



	/**
	 * @return Liefert das soeben aufgebaute Parcours-Objekt zurueck
	 */
	public Parcours getParcours() {
		return this.parcours;
	}

	//TODO Wette: Diese Methode (106 Zeilen) laesst sich durch Verwenden von XPath auf ca. 20 Zeilen vereinfachen
	//$$ EntityResolver-Kram ist zu kompliziert bei Aufruf dieser Methode; weg und ersetzen durch das, was in XmlDocument auch gemacht wird
	/**
	 * <p>
	 * L&auml;dt einen Parcours aus einer InputSource.
	 * 
	 * @param source
	 *            Xerces-InputSource-Objekt, aus dem der Parcours geholt werden
	 *            kann. Der Sinn ist, dass beliebige Eingabequellen
	 *            &uuml;bergeben werden k&ouml;nnen und daher nicht mehr nur aus
	 *            Dateien, sondern auch aus Strings oder von sonstwo gelesen
	 *            werden kann.
	 *            </p>
	 * 
	 * @param resolver
	 *            Der zu verwendende Xerces-EntityResolver, oder
	 *            <code>null</code>, wenn der Standard-Resolver verwendet werden
	 *            soll. Der DOMParser, der dieser Methode zugrundeliegt,
	 *            verwendet den Resolver w&auml;hrend dem Verarbeiten der im XML
	 *            vorkommenden "system identifier" und "public identifier".
	 *            Diese treten in unseren Parcoursdateien nur an einer Stelle
	 *            auf, n&auml;mlich in Zeile&nbsp;2:
	 *            <code>&lt;!DOCTYPE collection SYSTEM "parcours.dtd"></code>.
	 *            Der system identifier ist dabei <code>parcours.dtd</code>.
	 *            F&uuml;r Parcours-Dateien ist kein Resolver n&ouml;tig (= es
	 *            kann <code>null</code> &uuml;bergeben werden), weil sie im
	 *            gleichen Verzeichnis liegen wie die Datei parcours.dtd.
	 *            F&uuml;r Parcours, die aus XML-Strings gelesen werden, ist ein
	 *            Resolver n&ouml;tig, da der Parser sonst nur im
	 *            ctSim-Verzeichnis sucht (nicht im Verzeichnis ctSim/parcours),
	 *            und daher die Datei ctSim/parcours/parcours.dtd nicht findet.
	 * 
	 * @throws SAXException
	 * @throws IOException
	 */
	public void loadParcours(InputSource source, EntityResolver resolver)
			throws SAXException, IOException {
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			if (resolver != null)
				parser.setEntityResolver(resolver);
			// einlesen
			parser.parse(source);
			// umwandeln in ein Document
			Document doc = parser.getDocument();

			// Und anfangen mit dem abarbeiten

			// als erster suchen wir uns den Parcours-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while ((n != null) && (!n.getNodeName().equals("parcours")))
				n = n.getNextSibling();
			// jetzt haben wir ihn

			int y = 0; // Anzahl der Zeilen im File
			int x = 0; // Anzahl der Spalten im File

			// Eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			if (n == null) {
				throw new SAXException("kein Node gefunden!");
			}
			NodeList children = n.getChildNodes();

			// Anzahl der Zeilen und spalten bestimmen
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeName().equals("line")) {
					y++;
					if (x < child.getChildNodes().item(0).getNodeValue()
							.length())
						x = child.getChildNodes().item(0).getNodeValue()
								.length();
				}
			}

			// Parcors vorbereiten
			this.parcours.setDimX(x);
			this.parcours.setDimY(y);

			// Und eine Map anlegen
			this.parcoursMap = new int[x][y];

			parcours.setParcoursMap(parcoursMap);

			x = 0;
			y = 0;
			// ParcoursMap aufbauen
			for (int i = children.getLength() - 1; i >= 0; i--) {
				Node child = children.item(i);
				if (child.getNodeName().equals("line")) {
					char c[] = child.getChildNodes().item(0).getNodeValue()
							.toCharArray();
					for (x = 0; x < c.length; x++)
						this.parcoursMap[x][y] = c[x];
					y++;
				}
			}

			// ********** Appearances aus dem Document lesen

			// suchen wir uns den Otptics-Block
			n = doc.getDocumentElement().getFirstChild();
			while ((n != null) && (!n.getNodeName().equals("optics")))
				n = n.getNextSibling();
			// jetzt haben wir ihn

			// Eine Liste aller Kinder des Parcours-Eintrags organsisieren
			if (n == null) {
				throw new SAXException("kein Node gefunden!");
			}			
			children = n.getChildNodes();

			// HashMap mit den Apearances aufbauen
			for (int i = 0; i < children.getLength() - 1; i++) {
				Node appearance = children.item(i);
				if (appearance.getNodeName().equals("appearance")) {
					// Zuerst den Type extrahieren
					char item = appearance.getAttributes().getNamedItem("type")
							.getNodeValue().toCharArray()[0];

					String texture = null;
					String clone = null;

					HashMap<String, String> colors = Misc.newMap();

					NodeList features = appearance.getChildNodes();
					for (int j = 0; j < features.getLength(); j++) {
						if (features.item(j).getNodeName().equals("texture"))
							texture = features.item(j).getChildNodes().item(0)
									.getNodeValue();
						if (features.item(j).getNodeName().equals("color"))
							colors.put(features.item(j).getAttributes()
									.getNamedItem("type").getNodeValue(),
									features.item(j).getChildNodes().item(0)
											.getNodeValue());
						if (features.item(j).getNodeName().equals("clone"))
							clone = features.item(j).getChildNodes().item(0)
									.getNodeValue();
					}

					addAppearance(item, colors, texture, clone);
				}
			}

			// Soweit fertig.
			parse(); // Parcours Zusammenbauen

		} catch (SAXException e) {
			lg.warn(e, "Probleme beim Parsen des XML");
			throw e;
		} catch (IOException e) {
			lg.warn(e, "Probleme beim Parsen des XML");
			throw e;
		}
	}

	/**
	 * Liefert eine Appearance aus der Liste zurueck
	 * 
	 * @param key
	 *            Der Schluessel, mit dem sie abgelegt wurde
	 * @return Die Appearance
	 */
	@SuppressWarnings("boxing")
	private Appearance getAppearance(int key) {
		Appearance app = (Appearance) this.appearances.get((char) key);
		if (app == null)
			lg.warn("Appearance f\u00FCr '" + (char) key + "' nicht gefunden!");
		return app;
	}

	/**
	 * Erzeugt eine Appearnace und fuegt die der Liste hinzu

	 * @param item
	 *            Der Key, iunter dem diese Apperance abgelegt wird
	 * @param colors
	 *            HashMap mit je Farbtyp und ASCII-Represenation der Farbe
	 * @param textureFile
	 *            Der Name des Texture-Files
	 * @param clone
	 *            Referenz auf einen schon bestehenden Eintrag, der geclonet
	 *            werden soll
	 */
	@SuppressWarnings( { "unchecked", "boxing" })
	private void addAppearance(char item, HashMap colors, String textureFile,
			String clone) {

		if (clone != null) {
			this.appearances.put(item, this.appearances
					.get(clone.toCharArray()[0]));
			return;
		}

		Appearance appearance = new Appearance();

		if (colors != null) {
			Material mat = new Material();

			Iterator it = colors.keySet().iterator();
			while (it.hasNext()) {
				String colorType = (String) it.next();
				String colorName = (String) colors.get(colorType);

				if (colorType.equals("ambient"))
					mat.setAmbientColor(new Color3f(Color.decode(colorName)));
				if (colorType.equals("diffuse"))
					mat.setDiffuseColor(new Color3f(Color.decode(colorName)));
				if (colorType.equals("specular"))
					mat.setSpecularColor(new Color3f(Color.decode(colorName)));
				if (colorType.equals("emmissive"))
					mat.setEmissiveColor(new Color3f(Color.decode(colorName)));
			}
			appearance.setMaterial(mat);

		}

		if (textureFile != null) {
			TexCoordGeneration tcg = new TexCoordGeneration(
					TexCoordGeneration.OBJECT_LINEAR,
					TexCoordGeneration.TEXTURE_COORDINATE_3, new Vector4f(1.0f,
							1.0f, 0.0f, 0.0f), new Vector4f(0.0f, 1.0f, 1.0f,
							0.0f), new Vector4f(1.0f, 0.0f, 1.0f, 0.0f));
			appearance.setTexCoordGeneration(tcg);

			try {
				TextureLoader loader = new TextureLoader(ClassLoader
						.getSystemResource(textureFile), null);
				Texture2D texture = (Texture2D) loader.getTexture();
				texture.setBoundaryModeS(Texture.WRAP);
				texture.setBoundaryModeT(Texture.WRAP);

				// Mache die textur lesbar
				texture.setCapability(Texture.ALLOW_IMAGE_READ);
				ImageComponent[] imgs = texture.getImages();
				for (int i = 0; i < imgs.length; i++)
					imgs[i].setCapability(ImageComponent.ALLOW_IMAGE_READ);

				appearance.setTexture(texture);
				appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);

			} catch (Exception e) {
				lg.warn(e, "Probleme beim Laden der Texturdatei '%s'",
						textureFile);
			}

		}

		this.appearances.put(item, appearance);
	}

	/**
	 * Debug-Methode
	 * 
	 * @param node
	 *            Node
	 * @param out
	 *            Output-Stream
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
				out.print(" " + attr.getNodeName() + "=\""
						+ attr.getNodeValue() + "\"");
			}
			out.print('>');
			NodeList children = node.getChildNodes();
			len = children.getLength();
			for (int i = 0; i < len; i++)
				print(children.item(i), out);
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
			if (data != null && data.length() > 0)
				out.print(" " + data);
			out.println("?>");
			break;
		}
	}
}
