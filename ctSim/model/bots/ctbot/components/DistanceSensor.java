package ctSim.model.bots.ctbot.components;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.sensors.SimpleSensor;
import ctSim.view.Debug;

public class DistanceSensor extends SimpleSensor<Double> {
	
	// TODO:
	private World world;
	private Bot bot;
	
	private double angle = Math.PI / 180 * 3;  // 3°
	
	private Shape3D shape;
	
	public DistanceSensor(World world, Bot bot, String name, Point3d relPos, Vector3d relHead) {
		
		super(name, relPos, relHead);
		
		// TODO:
		this.world = world;
		this.bot   = bot;
		
		initShape();
	}

	/**
	 * Baut die 3D-Repraesentation des Bot-Koerpers aus 2D-Polygonen zusammen
	 *  
	 * @return Koerper des Bots 
	 */
	// TODO: Testshape vom Bot
	private void initShape() {
		
		/** Abstand vom Zentrum zur Aussenkante des Bots [m] */
		double BOT_RADIUS = 0.070d;

		/** Hoehe des Bots [m] */
		double BOT_HEIGHT = 0.140d;

		/** Breite des Faches [m] */
		double POCKET_LENGTH = 0.050d;

		/** Tiefe des Faches [m] */
		double POCKET_DEPTH = BOT_RADIUS - 0.015d;
		
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

		tsa = new TriangleStripArray(2 * N, TriangleStripArray.COORDINATES,
				stripCountTsa);
		tsa.setCoordinates(0, coords);

		tsa.setCapability(BranchGroup.ALLOW_DETACH);
		tsa.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

		
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

		qa = new QuadArray(3 * 4, QuadArray.COORDINATES);
		qa.setCoordinates(0, quadCoords);

		qa.setCapability(BranchGroup.ALLOW_DETACH);
		qa.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);

		
		// Hinzufuegen der Fachwaende zur Bot-Shape3D 
		this.shape.addGeometry(qa);

		this.shape.setCapability(BranchGroup.ALLOW_DETACH);
		this.shape.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
	}
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Infrarot";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Infrarot Abstands-Sensor: "+this.getName();
	}
	
	/**
	 * Liefert die Position eines IR-Sensors zurueck
	 * 
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	// TODO: Alte IR-Pos-Berechnugn...
	private Point3d getSensIRPosition(String side) {
		
		/** Hoehe des Bots [m] */
		double BOT_HEIGHT = 0.120d;
		
		/** Abstand Zentrum IR-Sensoren in Achsrichtung (X)[m] */
		double SENS_IR_DIST_X = 0.036d;

		/** Abstand Zentrum IR-Sensoren in Vorausrichtung (Y) [m] */
		double SENS_IR_DIST_Y = 0.0554d;

		/** Abstand Zentrum IR-Sensoren in Hochrichtung (Z) [m] */
		double SENS_IR_DIST_Z = 0.035d - BOT_HEIGHT / 2;
		
		// Vektor vom Ursprung in Axial-Richtung
		Vector3d vecX;
		if (side.equals("IrL"))
			vecX = new Vector3d(-this.bot.getHeading().y, this.bot.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_DIST_Z));
		else
			vecX = new Vector3d(this.bot.getHeading().y, -this.bot.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_DIST_Z));

		vecX.scale((float) SENS_IR_DIST_X, vecX);

		// Vektor vom Ursprung in Voraus-Richtung
		Vector3d vecY = new Vector3d(this.bot.getHeading());
		vecY.scale((float) SENS_IR_DIST_Y, vecY);

		// Ursprung
		Vector3d pos = new Vector3d(this.bot.getPosition());
		pos.add(vecX); // Versatz nach links
		pos.add(vecY); // Versatz nach vorne

		return new Point3d(pos);
	}
	
	@Override
	public Double updateValue() {
		
//		Debug.out.println("Robbi steht an: "+this.bot.getPosition()+" | "+this.bot.getHeading());
//		Debug.out.println("Sensor ist an relativer Pos.: "+this.getRelPosition()+ " | "+this.getRelHeading());
//		Debug.out.println("Summe ist: "+this.getAbsPosition(this.bot.getPosition(), this.bot.getHeading())
//				+ " | "+this.getAbsHeading(this.bot.getPosition(), this.bot.getHeading()));
		
		// TODO: Richtig so?
		
		double dist = this.world.watchObstacle(
				//this.getAbsPosition(this.bot.getPosition(), this.bot.getHeading()),
				this.getSensIRPosition(this.getName()),
				//this.bot.getPosition(),
				//this.getAbsHeading(this.bot.getPosition(), this.bot.getHeading()),
				this.bot.getHeading(),
				//this.bot.getHeading(),
				// TODO: (in Radius)
				this.angle,
				// TODO:
				//(Shape3D)this.bot.getNodeReference(BOTBODY));
				this.bot.getShape());
				//(Shape3D)this.bot.getNodeReference(Bot.BOTBODY));
		
		// TODO: Meter -> Millimeter; Obergrenze beachten?
//		System.out.println(this.getName()+" :  "+dist+" -> "+dist*1000);
		return dist*1000;
		
		/* TODO:
		 * 
		 * Wert updaten aus Welt...
		 * 
		 * Was wenn Wert von Bot stammen soll?
		 *    <-!!-> aus XML-Datei einlesen...
		 * 
		 * Wert aus Welt bestimmen:
		 *     Pos, Heading reicht (nicht!?)
		 *     -> Wert an Bot geben
ss
		 *     -> ueber Namen?
		 * Wert ueber Bot bestimmen:
		 *     ueber Namen (?)
		 *     -> Bot-Command (o.ae.)
		 *     -> ... (gleiches Problem wie beim Senden -> s.o.)
		 * 
		 * -> Kommando in XML-Datei angeben?
		 * 
		 */
		//return null;
	}
	
	public Shape3D getShape() {
		
		// TODO:
		//this.shape.setPickable(false);
		return new Shape3D();
		//return this.shape;
	}
}