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

package ctSim.Model.Bots;

import javax.vecmath.*;
import javax.media.j3d.*;

import java.awt.Image;
import java.lang.Math;

import com.sun.j3d.utils.geometry.*;

import ctSim.Controller.Controller;
import ctSim.Model.MousePicture;
import ctSim.View.CtControlPanel;

/**
 * Diese Klasse fuegt dem Bot Felder fuer die Zustaende der Sensoren und
 * Aktuatoren des c't-Bot hinzu. Sie kann ebenfalls nicht instanziiert werden
 * und muss daher in weiteren Klassen abgeleitet werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */

public abstract class CtBot extends Bot {

	// Oeffentliche Konstanten: Wertebereiche fuer Motoren und Sensoren

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

	/** Abstand Zentrum Gleitpin in Achsrichtung (X) [m] */
	public static final double BOT_SKID_X = 0d;

	/** Abstand Zentrum Gleitpin in Vorausrichtung (Y) [m] */
	public static final double BOT_SKID_Y = -0.054d;

	/** maximale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MAX = 255;

	/** minimale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MIN = -255;

	/** maximale Geschwindigkeit in Umdrehungen pro Sekunde */
	public static final float UPS_MAX = (float) 151 / (float) 60;

	/** Anzahl an Encoder-Markierungen auf einem Rad */
	public static final short ENCODER_MARKS = 60;

	/** Umfang eines Rades [m] */
	public static final double WHEEL_PERIMETER = Math.PI * 0.059d;

	/** Abstand Mittelpunkt Bot zum Rad [m] */
	public static final double WHEEL_DIST = 0.0485d;

	/** Abstand Zentrum IR-Sensoren in Achsrichtung (X)[m] */
	public static final double SENS_IR_DIST_X = 0.036d;

	/** Abstand Zentrum IR-Sensoren in Vorausrichtung (Y) [m] */
	public static final double SENS_IR_DIST_Y = 0.0554d;

	/** Abstand Zentrum IR-Sensoren in Hochrichtung (Z) [m] */
	public static final double SENS_IR_DIST_Z = 0.035d - BOT_HEIGHT / 2;

	/** Oeffnungswinkel der beiden IR-Abstandssensoren [Rad] */
	public static final double SENS_IR_ANGLE = Math.PI / 180 * 3;

	/** Abstand Zentrum Maussensor in Achsrichtung (X)[m] */
	public static final double SENS_MOUSE_DIST_X = 0d;

	/** Abstand Zentrum Maussensor in Vorausrichtung (Y) [m] */
	public static final double SENS_MOUSE_DIST_Y = -0.015d;

	/** Aufloesung des Maussensors [DPI] */
	public static final int SENS_MOUSE_DPI = 400;

	/** Abstand Zentrum Liniensensoren in Achsrichtung (X)[m] */
	public static final double SENS_LINE_DIST_X = 0.004d;

	/** Abstand Zentrum Liniensensoren in Vorausrichtung (Y) [m] */
	public static final double SENS_LINE_DIST_Y = 0.009d;

	/** Abstand Zentrum Liniensensoren in Hochrichtung (Z) [m] */
	public static final double SENS_LINE_DIST_Z = -0.011d - BOT_HEIGHT / 2;

	/** Oeffnungswinkel der beiden Liniensensoren [Rad] */
	public static final double SENS_LINE_ANGLE = Math.PI / 180 * 80;

	/** Anzahl Strahlen, die von den Liniensensoren ausgesendet werden. 
	 *  Je mehr Strahlen verwendet werden, desto genauer wird das Ergebnis.
	 *  Mehr Strahlen kosten aber auch mehr Rechenzeit. */
	public static final short SENS_LINE_PRECISION = 10;

	/** Abstand Zentrum Abgrundsensoren in Achsrichtung (X)[m] */
	public static final double SENS_BORDER_DIST_X = 0.036d;

	/** Abstand Zentrum Abgrundsensoren in Vorausrichtung (Y) [m] */
	public static final double SENS_BORDER_DIST_Y = 0.0384d;

	/** Abstand Zentrum Abgrundsensoren in Hochrichtung (Z) [m] */
	public static final double SENS_BORDER_DIST_Z = 0d - BOT_HEIGHT / 2;

	/** Oeffnungswinkel der beiden Abgrundsensoren [Rad] */
	public static final double SENS_BORDER_ANGLE = Math.PI / 180 * 80;

	/** Anzahl Strahlen, die von den Abgrundsensoren ausgesendet werden. 
	 *  Je mehr Strahlen verwendet werden, desto genauer wird das Ergebnis.
	 *  Mehr Strahlen kosten aber auch mehr Rechenzeit. */
	public static final short SENS_BORDER_PRECISION = 10;

	/** Abstand Zentrum Lichtsensoren in Achsrichtung (X)[m] */
	public static final double SENS_LDR_DIST_X = 0.032d;

	/** Abstand Zentrum Lichtsensoren in Vorausrichtung (Y) [m] */
	public static final double SENS_LDR_DIST_Y = 0.048d;

	/** Abstand Zentrum Lichtsensoren in Hochrichtung (Z) [m] */
	public static final double SENS_LDR_DIST_Z = 0.060d - BOT_HEIGHT / 2;

	/** Ausrichtung der Lichtsensors 
	 *  (0d,0d,1d) ==> nach oben
	 *  (1d,0d,0d) ==> nach vorne
	 */
	public static final Vector3d SENS_LDR_HEADING = new Vector3d(1d, 0d, 0d);

	/** Oeffnungswinkel der beiden Lichtsensoren [Rad] */
	public static final double SENS_LDR_ANGLE = Math.PI / 180 * 180;

	/** Anzahl der Zeilen im LCD */
	public static final short LCD_LINES = 4;

	/** Anzahl der Zeichen pro Zeile im LCD */
	public static final short LCD_CHARS = 20;

	/*
	 * Capabilities -- Flags, die anzeigen, welche internen Zustaende von
	 * Sensoren oder Aktuatoren ueber das ControlPanel beeinlussbar sind
	 */

	/** Koennen die Abgrund-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_BORDER = false;

	/** Kann der Klappen-Sensor beeinflusst werden? */
	public boolean CAP_SENS_DOOR = false;

	/** Koennen die Radencoder-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_ENC = false;

	/** Kann der Fehler-Sensor beeinflusst werden? */
	public boolean CAP_SENS_ERROR = false;

	/** Koennen die IR-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_IR = false;

	/** Koennen die Helligkeits-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_LDR = false;

	/** Koennen die Linien-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_LINE = false;

	/** Koennen die Maus-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_MOUSE = false;

	/** Kann der Fernbedienungs-Sensor beeinflusst werden? */
	public boolean CAP_SENS_RC5 = true;

	/** Kann der Fach-Sensor beeinflusst werden? */
	public boolean CAP_SENS_TRANS = true;

	/** Kann der Zustand der Motoren beeinflusst werden? */
	public boolean CAP_ACT_MOT = false;

	/** Kann der Zustande der LEDs beeinflusst werden? */
	public boolean CAP_ACT_LED = false;

	/** Kann der Aktuator fuer die Klappe beeinflusst werden? */
	public boolean CAP_ACT_DOOR = false;

	/** Kann der Reserve-Servo beeinflusst werden? */
	public boolean CAP_ACT_SERVO = false;

	/** Kann die Richtung des Bots beeinflusst werden? */
	public boolean CAP_HEAD = true;

	/** Kann die Position des Bots beeinflusst werden? */
	public boolean CAP_POS = true;


	/** RC5-Taste 0*/
	public static final int RC5_CODE_0 = 0x3940; 

	/** RC5-Taste 1*/
	public static final int RC5_CODE_1 = 0x3941; 

	/** RC5-Taste 2*/
	public static final int RC5_CODE_2 = 0x3942; 

	/** RC5-Taste 3*/
	public static final int RC5_CODE_3 = 0x3943; 

	/** RC5-Taste 4*/
	public static final int RC5_CODE_4 = 0x3944; 

	/** RC5-Taste 5*/
	public static final int RC5_CODE_5 = 0x3945; 

	/** RC5-Taste 6*/
	public static final int RC5_CODE_6 = 0x3946; 

	/** RC5-Taste 7*/
	public static final int RC5_CODE_7 = 0x3947; 

	/** RC5-Taste 8*/
	public static final int RC5_CODE_8 = 0x3948; 

	/** RC5-Taste 9*/
	public static final int RC5_CODE_9 = 0x3949; 

	/** RC5-Taste Up*/
	public static final int RC5_CODE_UP = 0x2950; 

	/** RC5-Taste down*/
	public static final int RC5_CODE_DOWN = 0x2951; 

	/** RC5-Taste left*/
	public static final int RC5_CODE_LEFT = 0x2955; 

	/** RC5-Taste right*/
	public static final int RC5_CODE_RIGHT = 0x2956;

	/** RC5-Taste PWR*/
	public static final int RC5_CODE_PWR = 0x394C; 

	/** RC5-Taste RED*/
	public static final int RC5_CODE_RED = 0x100B; 

	/** RC5-Taste GREEN*/
	public static final int RC5_CODE_GREEN = 0x102E; 

	/** RC5-Taste YELLOW*/
	public static final int RC5_CODE_YELLOW = 0x1038; 

	/** RC5-Taste BLUE*/
	public static final int RC5_CODE_BLUE = 0x1029; 

	
	// Felder fuer den Zustand des Roboters:

	// Aktuatoren:

	/** gewuenschte Motorengeschwindigkeit rechts */
	private Short actMotR = new Short((short) 0);

	/** gewuenschte Motorengeschwindigkeit links */
	private Short actMotL = new Short((short) 0);

	/** Position Servo Klappe */
	private Integer actDoor;

	/** Position des Reserve-Servo */
	private Integer actServo;

	/** Zustand der LEDs */
	private Integer actLed = new Integer((int) 0);

	/** Zustand des LCD */
	private String[] lcdText = new String[LCD_LINES];

	/** Cursorposition des LCD
	 * X : vor welchem Zeichen steht der Cursor (0 .. LCD_CHARS-1)
	 * Y : in welcher Zeile steht der Cursor (0 .. LCD_LINES-1)
	 */
	private int lcdCursorX = 0;

	private int lcdCursorY = 0;

	/** Puffer fuer Logausgaben */
	public StringBuffer logBuffer = new StringBuffer("");
	
	// Sensoren:

	/**
	 * vom linken IR-Sensor gemessener Wert, entspricht dem Abstand zum naechsten
	 * Objekt in Millimetern
	 */
	private Short sensIrL = new Short((short) 0);

	/**
	 * vom rechten IR-Sensor gemessener Wert, entspricht dem Abstand zum naechsten
	 * Objekt in Millimetern
	 */
	private Short sensIrR = new Short((short) 0);

	/** Radencoder links */
	private Short sensEncL = new Short((short) 0);

	/** Radencoder rechts */
	private Short sensEncR = new Short((short) 0);

	/** Abgrundsensor links */
	private Integer sensBorderL = new Integer(0);

	/** Abgrundsensor rechts */
	private Integer sensBorderR = new Integer(0);

	/** Liniensensor links */
	private Integer sensLineL = new Integer(0);

	/** Liniensensor rechts */
	private Integer sensLineR = new Integer(0);

	/** Helligkeitssensor links */
	private Integer sensLdrL = new Integer(0);

	/** Helligkeitssensor rechts */
	private Integer sensLdrR = new Integer(0);

	/** Ueberwachung Transportfach */
	private Integer sensTrans = new Integer(0);

	/** Ueberwachung Klappe */
	private Integer sensDoor = new Integer(0);

	/** Maussensor DeltaX */
	private Integer sensMouseDX = new Integer(0);

	/** Maussensor DeltaY */
	private Integer sensMouseDY = new Integer(0);

	/** TRC5-Fernbedienung */
	private Integer sensRc5 = new Integer(0);

	/** Batterie oder Servofehler */
	private Integer sensError = new Integer(0);

	/**
	 * Einfacher Konstruktor
	 */
	public CtBot(Controller controller) {
		super(controller);
		world = getController().getWorld();
		createBranchGroup();
		//	 LCD initialisieren
		lcdClear();
	}

	/**
	 * Alternativer Konstruktor
	 * 
	 * @param pos
	 *            Die initiale Position
	 * @param head
	 *            Die initiale Blickrichtung
	 */
	public CtBot(Controller controller, Point3f pos, Vector3f head) {
		// Einfachen Konstruktor aufrufen:
		this(controller);
		Vector3f vec = new Vector3f(pos);
		vec.z += getHeight() / 2 + getGroundClearance();
		setPos(vec);
		setHeading(head);
	}

	/*
	 * (non-Javadoc) Der Aufruf dieser Methode direkt nach dem Erzeugen sorgt
	 * dafuer, dass der Bot ueber ein passendes ControlPanel verfuegt
	 * 
	 * @see ctSim.Model.Bot#providePanel()
	 */
	public void providePanel() {
		this.setPanel(new CtControlPanel(this));
	}

	/**
	 * Erzeugt die 3D-Repraesentation eines Bots
	 */
	@SuppressWarnings("unchecked")
	private void createBranchGroup() {
		// Translationsgruppe fuer den Bot
		TransformGroup tg = new TransformGroup();
		Transform3D transform = new Transform3D();
		tg = new TransformGroup(transform);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		tg.setCapability(TransformGroup.ALLOW_CHILDREN_WRITE);
		
		// Bot erzeugen
		Shape3D realBot = createBotShape();
		realBot.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		setAppearance("normal");
				
		realBot.setName(getName() + " Body");
		// Koerper "pickable" setzen, um Kollisionen mit anderen Bots
		// zu erkennen
		realBot.setPickable(true);
		// "Pickable" muss fuer die eigene Kollisionsabfrage abschaltbar sein
		realBot.setCapability(Cylinder.ALLOW_PICKABLE_WRITE);
		// Referenz auf Koerper merken, um spaeter bei der eigenen Kollisionsabfrage die 
		// "Pickable"-Eigenschaft aendern zu koennen
		tg.addChild(realBot);
		
		// Referenz im Bot ablegen
		addNodeReference(BOTBODY,realBot);
			
		// Die Grenzen (Bounds) des Bots sind wichtig
		// fuer die Kollisionserkennung.
		// Die Grenze des Roboters wird vorlaefig definiert ueber
		// eine Sphaere mit Radius der Bot-Grundplatte um die Position des Bot
		setBounds(new BoundingSphere(new Point3d(super.getPos()), BOT_RADIUS));

		// Jetzt wird noch alles nett verpackt
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		bg.addChild(tg);

		// Referenz im Bot ablegen
		addNodeReference(TG,tg);
		addNodeReference(BG,bg);
	}

	/**
	 * Baut die 3D-Repraesentation des Bot-Koerpers aus 2D-Polygonen zusammen
	 *  
	 * @return Koerper des Bots 
	 */
	private Shape3D createBotShape() {

		Shape3D bs = new Shape3D();
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
		bs.addGeometry(gi.getGeometryArray());

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
		bs.setGeometry(tsa);

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
		bs.addGeometry(qa);

		bs.setCapability(BranchGroup.ALLOW_DETACH);
		bs.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		
		return bs;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#init()
	 */
	protected void init() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#work()
	 */
	protected void work() {
		this.getPanel().reactToChange();
	}

	/**
	 * @return Distanz zum naechsten Objekt in mm
	 */
	public short getSensIrL() {
		return sensIrL.shortValue();
	}

	/**
	 * @param distance
	 *            Einzutragender Wert fuer irL -- Abstand in [m]
	 */
	public void setSensIrL(double distance) {
		// Intern wird mit der Distanz in mm gearbeitet
		short irL;
		if (distance * 1000 > Short.MAX_VALUE)
			irL = Short.MAX_VALUE;
		else
			irL = (short) (distance * 1000);
		synchronized (sensIrL) {
			this.sensIrL = new Short(irL);
		}
	}

	/**
	 * @return Aktuelle Motorgeschwindigkeit links in PWM
	 */
	public short getActMotL() {
		return actMotL.shortValue();
	}

	/**
	 * @param speedL
	 *            Einzutragender Wert fuer speedL
	 */
	public void setActMotL(short speedL) {
		if (speedL >= PWM_MAX) {
			speedL = PWM_MAX;
		}
		if (speedL <= PWM_MIN) {
			speedL = PWM_MIN;
		}
		synchronized (actMotL) {
			this.actMotL = new Short(speedL);
		}
	}

	/**
	 * @return Distanz zum naechsten Objekt in mm
	 */
	public short getSensIrR() {
		return sensIrR.shortValue();
	}

	/**
	 * @param distance
	 *            Einzutragender Wert fuer irR -- Abstand in [m]
	 */
	public void setSensIrR(double distance) {
		// Intern wird mit der Distanz in mm gearbeitet
		short irR;
		if (distance * 1000 > Short.MAX_VALUE)
			irR = Short.MAX_VALUE;
		else
			irR = (short) (distance * 1000);
		synchronized (sensIrR) {
			this.sensIrR = new Short(irR);
		}
	}

	/**
	 * @return Aktuelle Motorgeschwindigkeit rechts in PWM
	 */
	public short getActMotR() {
		return actMotR.shortValue();
	}

	/**
	 * @param speedR
	 *            Einzutragender Wert fuer speedR
	 */
	public void setActMotR(short speedR) {
		if (speedR >= PWM_MAX) {
			speedR = PWM_MAX;
		}
		if (speedR <= PWM_MIN) {
			speedR = PWM_MIN;
		}
		synchronized (actMotR) {
			this.actMotR = new Short(speedR);
		}
	}

	/**
	 * @return Radencoderstand links
	 */
	public short getSensEncL() {
		return sensEncL.shortValue();
	}

	/**
	 * @param encL
	 *            Neuer Radencoderstand links zu setzen
	 */
	public void setSensEncL(short encL) {
		synchronized (sensEncL) {
			this.sensEncL = new Short(encL);
		}
	}

	/**
	 * @return Radencoderstand rechts
	 */
	public short getSensEncR() {
		return sensEncR.shortValue();
	}

	/**
	 * @param encR
	 *            Neuer Radencoderstand rechts zu setzen
	 */
	public void setSensEncR(short encR) {
		synchronized (sensEncR) {
			this.sensEncR = new Short(encR);
		}
	}

	/**
	 * @return Status des linken Abgrundsensors
	 */
	public int getSensBorderL() {
		return sensBorderL.intValue();
	}

	/**
	 * @param borderL
	 *            Neuer Wert fuer den linken Abgrundsensor
	 */
	public void setSensBorderL(int borderL) {
		synchronized (sensBorderL) {
			this.sensBorderL = new Integer(borderL);
		}
	}

	/**
	 * @return Status der Klappenueberwachung
	 */
	public int getSensDoor() {
		return sensDoor.intValue();
	}

	/**
	 * @param door
	 *            Status der Klappenueberwachung, der gesetzt werden soll
	 */
	public void setSensDoor(int door) {
		synchronized (sensDoor) {
			this.sensDoor = new Integer(door);
		}
	}

	/**
	 * @return Die Bodenfreiheit des Bot in [m]
	 */
	public float getGroundClearance() {
		return (float) BOT_GROUND_CLEARANCE;
	}

	/**
	 * @return Hoehe des Bot in [m]
	 */
	public float getHeight() {
		return (float) BOT_HEIGHT;
	}

	/**
	 * @return Gibt den Wert von actDoor zurueck
	 */
	public int getActDoor() {
		return actDoor.intValue();
	}

	/**
	 * @param actDoor
	 *            Der Wert von actDoor, der gesetzt werden soll
	 */
	public void setActDoor(int actDoor) {
		this.actDoor = new Integer(actDoor);
	}

	/**
	 * @return Gibt den Wert von actLed zurueck
	 */
	public int getActLed() {
		return actLed.intValue();
	}

	/**
	 * @param actLed
	 *            Der Wert von actLed, der gesetzt werden soll
	 */
	public void setActLed(int actLed) {
		this.actLed = new Integer(actLed);
	}

	/**
	 * @return Gibt den Wert von actServo zurueck
	 */
	public int getActServo() {
		return actServo.intValue();
	}

	/**
	 * @param actServo
	 *            Der Wert von actServo, der gesetzt werden soll
	 */
	public void setActServo(int actServo) {
		this.actServo = new Integer(actServo);
	}

	/**
	 * @return Gibt den Wert von sensBorderR zurueck
	 */
	public int getSensBorderR() {
		return sensBorderR.intValue();
	}

	/**
	 * @param sensBorderR
	 *            Der Wert von sensBorderR, der gesetzt werden soll
	 */
	public void setSensBorderR(int sensBorderR) {
		this.sensBorderR = new Integer(sensBorderR);
	}

	/**
	 * @return Gibt den Wert von sensError zurueck
	 */
	public int getSensError() {
		return sensError.intValue();
	}

	/**
	 * @param sensError
	 *            Der Wert von sensError, der gesetzt werden soll
	 */
	public void setSensError(int sensError) {
		this.sensError = new Integer(sensError);
	}

	/**
	 * @return Gibt den Wert von sensLdrL zurueck
	 */
	public int getSensLdrL() {
		return sensLdrL.intValue();
	}

	/**
	 * @param sensLdrL
	 *            Der Wert von sensLdrL, der gesetzt werden soll
	 */
	public void setSensLdrL(int sensLdrL) {
		this.sensLdrL = new Integer(sensLdrL);
	}

	/**
	 * @return Gibt den Wert von sensLdrR zurueck
	 */
	public int getSensLdrR() {
		return sensLdrR.intValue();
	}

	/**
	 * @param sensLdrR
	 *            Der Wert von sensLdrR, der gesetzt werden soll
	 */
	public void setSensLdrR(int sensLdrR) {
		this.sensLdrR = new Integer(sensLdrR);
	}

	/**
	 * @return Gibt den Wert von sensLineL zurueck
	 */
	public int getSensLineL() {
		return sensLineL.intValue();
	}

	/**
	 * @param sensLineL
	 *            Der Wert von sensLineL, der gesetzt werden soll
	 */
	public void setSensLineL(int sensLineL) {
		this.sensLineL = new Integer(sensLineL);
	}

	/**
	 * @return Gibt den Wert von sensLineR zurueck
	 */
	public int getSensLineR() {
		return sensLineR.intValue();
	}

	/**
	 * @param sensLineR
	 *            Der Wert von sensLineR, der gesetzt werden soll
	 */
	public void setSensLineR(int sensLineR) {
		this.sensLineR = new Integer(sensLineR);
	}

	/**
	 * @return Gibt den Wert von sensMouseDX zurueck
	 */
	public int getSensMouseDX() {
		return sensMouseDX.intValue();
	}

	/**
	 * @param sensMouseDX
	 *            Der Wert von sensMouseDX, der gesetzt werden soll
	 */
	public void setSensMouseDX(int sensMouseDX) {
		this.sensMouseDX = new Integer(sensMouseDX);
	}

	/**
	 * @return Gibt den Wert von sensMouseDY zurueck
	 */
	public int getSensMouseDY() {
		return sensMouseDY.intValue();
	}

	/**
	 * @param sensMouseDY
	 *            Der Wert von sensMouseDY, der gesetzt werden soll
	 */
	public void setSensMouseDY(int sensMouseDY) {
		this.sensMouseDY = new Integer(sensMouseDY);
	}

	/**
	 * @return Gibt den Wert von sensRc5 zurueck
	 */
	public int getSensRc5() {
		return sensRc5.intValue();
	}

	/**
	 * @param sensRc5
	 *            Der Wert von sensRc5, der gesetzt werden soll
	 */
	public void setSensRc5(int sensRc5) {
		this.sensRc5 = new Integer(sensRc5);
	}

	/**
	 * @return Gibt den Wert von sensTrans zurueck
	 */
	public int getSensTrans() {
		return sensTrans.intValue();
	}

	/**
	 * @param sensTrans
	 *            Der Wert von sensTrans, der gesetzt werden soll
	 */
	public void setSensTrans(int sensTrans) {
		this.sensTrans = new Integer(sensTrans);
	}

	/**
	 * Setzt Text an eine bestimmte Position im LCD.
	 * 
	 * @param charPos Neue Cursorposition (Spalte 0..19)
	 * @param linePos Neue Cursorposition (Zeile 0..3)
	 * @param text    Der Text, der ab der neuen Cursorposition einzutragen ist
	 */
	public void setLcdText(int charPos, int linePos, String text) {
		setCursor(charPos, linePos);
		{
			String pre = "";
			String post = "";
			int max = Math.min(text.length(), LCD_CHARS - lcdCursorX - 1);

			// Der neue Zeilentext ist der alte bis zur Cursorposition, gefolgt 
			// vom uebergebenen String 'text' gefolgt von den nicht ueberschriebenen Zeichen, 
			// wenn sich die neue X-Position noch vor dem Zeilenende befindet.
			if (lcdCursorX > 0) {
				pre = new String(lcdText[lcdCursorY].substring(0,
						lcdCursorX - 1));
			}
			lcdCursorX += max;
			if (lcdCursorX < LCD_CHARS - 1) {
				post = new String(lcdText[lcdCursorY].substring(lcdCursorX));
			}
			synchronized (lcdText) {
				lcdText[lcdCursorY] = new String(pre + text + post);
			}
		}
	}

	/**
	 * Setzt Text in eine bestimmte Zeile im LCD.
	 * 
	 * @param linePos Neue Cursorposition (Zeile 0..3)
	 * @param text    Der Text, der ab der neuen Cursorposition einzutragen ist
	 */
	public void setLcdText(int linePos, String text) {
		setLcdText(0, linePos, text);
	}

	/**
	 * Setzt Text ins LCD.
	 * 
	 * @param text Der Text, der ab der neuen Cursorposition einzutragen ist	
	 */
	public void setLcdText(String text) {
		setLcdText(lcdCursorX, lcdCursorY, text);
	}

	/**
	 * Setzt den Cursor an eine bestimmte Position im LCD 
	 * @param charPos Neue Cursorposition (Spalte 0..19)
	 * @param linePos Neue Cursorposition (Zeile 0..3)
	 */
	public void setCursor(int charPos, int linePos) {
		if (charPos < 0) {
			charPos = 0;
		}
		if (charPos > LCD_CHARS - 1) {
			charPos = LCD_CHARS - 1;
		}
		if (linePos < 0) {
			linePos = 0;
		}
		if (linePos > LCD_LINES - 1) {
			linePos = LCD_LINES - 1;
		}

		this.lcdCursorX = charPos;
		this.lcdCursorY = linePos;
	}

	/**
	 * Liest eine Zeile aus dem LCD
	 * @param linePos Zeilennummer (0..3)
	 * @return Die Zeile aus dem Display
	 */
	public String getLcdText(int linePos) {
		if (linePos < 0) {
			linePos = 0;
		}
		if (linePos > LCD_LINES - 1) {
			linePos = LCD_LINES - 1;
		}
		return lcdText[linePos];
	}

	/**
	 * @return Zeilenposition des LCD-Cursors
	 */
	public int getLcdCursorY() {
		return lcdCursorY;
	}

	/**
	 * @return Spaltenposition des LCD-Cursors
	 */
	public int getLcdCursorX() {
		return lcdCursorX;
	}

	public void lcdClear() {
		synchronized (lcdText) {
			for (int i = 0; i < lcdText.length; i++) {
				lcdText[i] = new String("                    ");
			}
		}
	}

	/**
	 * Schreibt Logausgabe in den Puffer.
	 * @param str String fuer die Ausgabe
	 */
	public void setLog(String str) {
		synchronized(logBuffer) {
			logBuffer.append(str + "\n");
		}
	}
	
	/**
	 * Liefert Puffer fuer die Logausgabe.
	 * @return Logausgabe
	 */
	public StringBuffer getLog() {
		StringBuffer tempBuffer = new StringBuffer("");
		
		synchronized(logBuffer) {
			
			/* Puffer kopieren und leeren*/
			tempBuffer.append(logBuffer.toString());
			logBuffer.delete(0, logBuffer.length());
		}
		
		return tempBuffer;
	}
	
	
	/** Anzahl der Pixel des Maussensors */
	public static final int MOUSE_PIXEL_X = 18; 

	/** Anzahl der Pixel des Maussensors */
	public static final int MOUSE_PIXEL_Y = 18; 

	/** Anzahl der Pixel des Maussensors */
	public static final int MOUSE_PIXEL = MOUSE_PIXEL_X * MOUSE_PIXEL_Y; 

	/** Ein Bild vom Maussensor */
	//private MousePicture mousePicture = new MousePicture(MOUSE_PIXEL_X,MOUSE_PIXEL_Y);
	
	
	private MousePicture mousePicture = new MousePicture(MOUSE_PIXEL_X,MOUSE_PIXEL_Y);
	/**
	 * Liefert das aktuelle Bild des Maussensors, so es fertig ist, somst Null
	 * @return Gibt eine Referenz auf mousePicture zurueck
	 */
	public Image getMousePicture() {
		if (mousePicture.isComplete())
			return mousePicture.getImage(18*6,18*6);
		else 
			return null;
	}
	/**
	 * Fuegt Daten zum Maussensor-Bild hinzu
	 * @param start Nummer des ersten Pixels in data
	 * @param data Nutzdaten
	 */
	public void setMousePicture(int start, byte[] data){
		mousePicture.addPixels(start,data);
	}	

	/** Fordert ein Maussensorbild beim Bot an */
	abstract public void requestMousePicture();
}
