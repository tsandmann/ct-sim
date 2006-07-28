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
package ctSim.model.bots.ctbot;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

import ctSim.model.bots.Bot;

/**
 * Abstrakte Oberklasse fuer alle c't-Bots
 *
 */
public abstract class CtBot extends Bot {
	
	/** Abstand vom Zentrum zur Aussenkante des Bots [m] */
	public static final double BOT_RADIUS = 0.060d;

	/** Hoehe des Bots [m] */
	public static final double BOT_HEIGHT = 0.120d;

	/** Breite des Faches [m] */
	public static final double POCKET_LENGTH = 0.050d;

	/** Tiefe des Faches [m] */
	public static final double POCKET_DEPTH = BOT_RADIUS - 0.015d;
	
	/** Bodenfreiheit des Bots [m] */
	public static final double BOT_GROUND_CLEARANCE = 0.015d;
	
	@SuppressWarnings("unused")
	private Bounds bounds;
	
	private Shape3D shape;
	
	/* TODO:
	 * Pos. u. Head. in Klassenhierarchie weiter nach oben:
	 * -> Jedes (Alive)Obstacle braucht (initiale) Pos.
	 * 
	 */
	/**
	 * Der Konstruktor
	 * @param n Name
	 * @param pos Position
	 * @param head Blickrichtung
	 */
	public CtBot(String n, Point3d pos, Vector3d head) {
		
		super(n, pos, head);
		
		initBounds();
		initShape();
		
		this.setShape(this.shape);
		
		// Einfachen Konstruktor aufrufen:
//		Vector3f vec = new Vector3f(pos);
//		// TODO: Was das!?
//		vec.z += getHeight() / 2 + getGroundClearance();
//		setPos(vec);
//		setHeading(head);
		
		//initSensors();
		//initActutaors();
	}
	
	private void initBounds() {
		
		this.bounds = new BoundingSphere(new Point3d(this.getPosition()), BOT_RADIUS);
	}
	
	/**
	 * Baut die 3D-Repraesentation des Bot-Koerpers aus 2D-Polygonen zusammen
	 *  
	 */
	private void initShape() {

		this.shape = new Shape3D();
		// Anzahl der Ecken, um den Kreis des Bots zu beschreiben.
		// Mehr sehen besser aus, benoetigen aber auch mehr Rechenzeit.
		int N = 10;
		// Anzahl der verwendeten Punkte
		int totalN = 2 * (N + 2);
		// data muss pro Punkt die Werte x, y und z speichern
		float[] data = new float[totalN * 3];
		// zwei Polygone (Deckel und Boden) mit je N+2 Ecken
		int stripCounts[] = { N + 2, N + 2 };
		float r = (float) BOT_RADIUS;
		float h = (float) BOT_HEIGHT / 2;
		// Zaehler
		int n;
		// Koordinaten
		float x, y;
		// Winkel
		double alpha = 0.0;

		// Bot-Deckel erzeugen
		//
		// Winkel des vollen Kreises (im Bogenmass)
		double circle = 2.0 * Math.PI;
		// halber Winkel der Oeffnung des Bots
		double opening = Math.asin((POCKET_LENGTH / 2) / r);

		// Rand des Deckels erzeugen, beachte dabei die Oeffnung
		for (n = 0; n < N; n++) {
			alpha = opening + (((circle - 2 * opening) / (N - 1)) * n);
			x = (float) (r * Math.cos(alpha));
			y = (float) (r * Math.sin(alpha));
			data[3 * n] = x;
			data[3 * n + 1] = y;
			data[3 * n + 2] = h; // 0
		}

		// Fach in die Oeffnung des Deckels einbauen
		data[3 * n] = r - (float) POCKET_DEPTH;
		data[3 * n + 1] = (float) -POCKET_LENGTH / 2;
		data[3 * n + 2] = h; // 2
		n++;
		data[3 * n] = r - (float) POCKET_DEPTH;
		data[3 * n + 1] = (float) POCKET_LENGTH / 2;
		data[3 * n + 2] = h; // 2
		n++;

		// Bot-Deckel kopieren, um ihn als Boden zu verwenden
		for (int i = (N + 2) - 1; i >= 0; i--) {
			data[3 * n] = data[3 * i];
			data[3 * n + 1] = data[3 * i + 1];
			data[3 * n + 2] = -data[3 * i + 2];
			n++;
		}

		// Deckel und Boden in darstellbare Form umwandeln
		GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
		gi.setCoordinates(data);
		gi.setStripCounts(stripCounts);

		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		gi.recomputeIndices();

		Stripifier st = new Stripifier();
		st.stripify(gi);
		gi.recomputeIndices();

		// Hinzufuegen des Deckels und des Bodens zur Bot-Shape3D 
		this.shape.addGeometry(gi.getGeometryArray());

		// Erzeugen der aeusseren Seitenverkleidung
		TriangleStripArray tsa;
		Point3f coords[] = new Point3f[N * 2];
		int stripCountTsa[] = { N * 2 };

		for (int i = 0; i < (N); i++) {
			coords[i * 2] = new Point3f(data[i * 3], data[i * 3 + 1],
					data[i * 3 + 2]);
			coords[i * 2 + 1] = new Point3f(data[i * 3], data[i * 3 + 1],
					-data[i * 3 + 2]);
		}

		tsa = new TriangleStripArray(2 * N, GeometryArray.COORDINATES,
				stripCountTsa);
		tsa.setCoordinates(0, coords);

		tsa.setCapability(BranchGroup.ALLOW_DETACH);
		tsa.setCapability(Group.ALLOW_CHILDREN_WRITE);

		
		// Hinzufuegen der aeusseren Seitenverkleidung zur Bot-Shape3D
		this.shape.setGeometry(tsa);

		// Erzeugen der Waende des Faches
		QuadArray qa;
		Point3f quadCoords[] = new Point3f[3 * 4];

		n = N - 1;
		// aussen rechts
		quadCoords[0] = new Point3f(data[n * 3], data[n * 3 + 1],
				data[n * 3 + 2]);
		quadCoords[1] = new Point3f(data[n * 3], data[n * 3 + 1],
				-data[n * 3 + 2]);
		n++;
		// innen rechts
		quadCoords[2] = new Point3f(data[n * 3], data[n * 3 + 1],
				-data[n * 3 + 2]);
		quadCoords[3] = new Point3f(data[n * 3], data[n * 3 + 1],
				data[n * 3 + 2]);

		quadCoords[4] = new Point3f(data[n * 3], data[n * 3 + 1],
				data[n * 3 + 2]);
		quadCoords[5] = new Point3f(data[n * 3], data[n * 3 + 1],
				-data[n * 3 + 2]);
		n++;
		// innen links
		quadCoords[6] = new Point3f(data[n * 3], data[n * 3 + 1],
				-data[n * 3 + 2]);
		quadCoords[7] = new Point3f(data[n * 3], data[n * 3 + 1],
				data[n * 3 + 2]);

		quadCoords[8] = new Point3f(data[n * 3], data[n * 3 + 1],
				data[n * 3 + 2]);
		quadCoords[9] = new Point3f(data[n * 3], data[n * 3 + 1],
				-data[n * 3 + 2]);
		n = 0;
		// aussen links
		quadCoords[10] = new Point3f(data[n * 3], data[n * 3 + 1],
				-data[n * 3 + 2]);
		quadCoords[11] = new Point3f(data[n * 3], data[n * 3 + 1],
				data[n * 3 + 2]);

		qa = new QuadArray(3 * 4, GeometryArray.COORDINATES);
		qa.setCoordinates(0, quadCoords);

		qa.setCapability(BranchGroup.ALLOW_DETACH);
		qa.setCapability(Group.ALLOW_CHILDREN_WRITE);

		
		// Hinzufuegen der Fachwaende zur Bot-Shape3D 
		this.shape.addGeometry(qa);

		this.shape.setCapability(BranchGroup.ALLOW_DETACH);
		this.shape.setCapability(Group.ALLOW_CHILDREN_WRITE);
	}
	
	/** 
	 * @see ctSim.model.Obstacle#getBounds()
	 */
	public Bounds getBounds() {
		
		//return this.bounds;
		return new BoundingSphere(new Point3d(this.getPosition()), BOT_RADIUS);
	}
	
	/**
	 * @return Die Bodenfreiheit des Bot in [m]
	 */
//	@Override
//	public float getGroundClearance() {
//		return (float) BOT_GROUND_CLEARANCE;
//	}

	/**
	 * @return Hoehe des Bot in [m]
	 */
//	@Override
//	public float getHeight() {
//		return (float) BOT_HEIGHT;
//	}

	/**
	 * Erzeugt die 3D-Repraesentation eines Bots
	 * Nicht von aussen aufrufen!
	 */
//	//@Override
//	public void createBranchGroup() {
//		// Translationsgruppe fuer den Bot
//		TransformGroup tg = new TransformGroup();
//		Transform3D transform = new Transform3D();
//		tg = new TransformGroup(transform);
//		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
//		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
//		tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
//		
//		// Bot erzeugen
//		Shape3D realBot = createBotShape();
//		realBot.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
//		setAppearance("normal");
//				
//		realBot.setName(getName() + " Body");
//		// Koerper "pickable" setzen, um Kollisionen mit anderen Bots
//		// zu erkennen
//		realBot.setPickable(true);
//		// "Pickable" muss fuer die eigene Kollisionsabfrage abschaltbar sein
//		realBot.setCapability(Cylinder.ALLOW_PICKABLE_WRITE);
//		// Referenz auf Koerper merken, um spaeter bei der eigenen Kollisionsabfrage die 
//		// "Pickable"-Eigenschaft aendern zu koennen
//		tg.addChild(realBot);
//		
//		// Referenz im Bot ablegen
//		addNodeReference(BOTBODY,realBot);
//			
//		// Die Grenzen (Bounds) des Bots sind wichtig
//		// fuer die Kollisionserkennung.
//		// Die Grenze des Roboters wird vorlaefig definiert ueber
//		// eine Sphaere mit Radius der Bot-Grundplatte um die Position des Bot
//		setBounds(new BoundingSphere(new Point3d(super.getPos()), BOT_RADIUS));
//
//		// Jetzt wird noch alles nett verpackt
//		BranchGroup bg = new BranchGroup();
//		bg.setCapability(BranchGroup.ALLOW_DETACH);
//		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
//		bg.addChild(tg);
//
//		// Referenz im Bot ablegen
//		addNodeReference(TG,tg);
//		addNodeReference(BG,bg);
//	}
}
