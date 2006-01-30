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

import javax.vecmath.*;
import javax.media.j3d.*;

import java.lang.Math;

import com.sun.j3d.utils.geometry.*;

import ctSim.Controller.Controller;
import ctSim.View.CtControlPanel;

/**
 * Diese Klasse fuegt dem Bot Felder fuer die Zustaende der Sensoren und
 * Aktuatoren des c't-Bot hinzu. Sie kann ebenfalls nicht instanziiert werden
 * und muss daher in weiteren Klassen abgeleitet werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 */

public abstract class CtBot extends Bot {

	// Oeffentliche Konstanten: Wertebereiche fuer Motoren und Sensoren

	/** Abstand Zentrum Aussenkante des Bots [m] */
	public static final double BOT_RADIUS = 0.060d;

	/** Hoehe des Bots [m] */
	public static final double BOT_HEIGHT = 0.120d;

	/** Breite des Faches [m] */
	public static final double FACH_LENGTH = 0.050d;
	
	/** Tiefe des Faches [m] */
	public static final double FACH_DEPTH = BOT_RADIUS - 0.015d;
	
	/** Bodenfreiheit des Bots [m] */
	public static final double BOT_GROUND_CLEARANCE = 0.015d;
	
	/** Abstand Zentrum Schleifsporns in Achsrichtung (X) [m] */
	public static final double BOT_SKID_X = 0d;
	
	/** Abstand Zentrum Schleifsporns in Vorausrichtung (Y) [m] */
	public static final double BOT_SKID_Y = -0.050d;
	
	/** maximale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MAX = 255;

	/** minimale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MIN = -255;

	/** maximale Geschwindigkeit in Umdrehungen pro Sekunde */
	public static final float UPS_MAX = (float) 151 / (float) 60;

	/** Anzahl an Encoder-Markierungen auf einem Rad */
	public static final short ENCODER_MARKS = 40;

	/** Umfang eines Rades [m] */
	public static final double RAD_UMFANG = Math.PI * 0.050d;

	/** Abstand Mittelpunkt Bot zum Rad [m] */
	public static final double RAD_ABSTAND = 0.050d;

	/** Abstand Zentrum IR-Sensoren in Achsrichtung (X)[m] */
	public static final double SENS_IR_ABSTAND_X = 0.036d;

	/** Abstand Zentrum IR-Sensoren in Vorausrichtung (Y) [m] */
	public static final double SENS_IR_ABSTAND_Y = 0.0554d;

	/** Abstand Zentrum IR-Sensoren in Hochrichtung (Y) [m] */
	public static final double SENS_IR_ABSTAND_Z = 0.035d - BOT_HEIGHT/2;

	/** Oeffnungswinkel der beiden IR-Abstandssensoren [Rad] */
	public static final double SENS_IR_ANGLE = Math.PI / 180 * 3; 
	
	/** Abstand Zentrum Maussensor in Achsrichtung (X)[m] */
	public static final double SENS_MOUSE_ABSTAND_X = 0d;

	/** Abstand Zentrum Maussensor in Vorausrichtung (Y) [m] */
	public static final double SENS_MOUSE_ABSTAND_Y = -0.015d;
	
	/** DPI des Maussensors [m] */
	public static final int SENS_MOUSE_DPI = 400;
	
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
	public boolean CAP_AKT_MOT = false;

	/** Kann der Zustande der LEDs beeinflusst werden? */
	public boolean CAP_AKT_LED = false;

	/** Kann der Aktuator fuer die Klappe beeinflusst werden? */
	public boolean CAP_AKT_DOOR = false;

	/** Kann der Reserve-Servo beeinflusst werden? */
	public boolean CAP_AKT_SERVO = false;

	/** Kann die Richtung des Bots beeinflusst werden? */
	public boolean CAP_HEAD = true;

	/** Kann die Position des Bots beeinflusst werden? */
	public boolean CAP_POS = true;

	// Felder fuer den Zustand des Roboters:

	// Aktuatoren:

	/** gewuenschte Motorengeschwindigkeit rechts */
	private Short aktMotR = new Short((short) 0);

	/** gewuenschte Motorengeschwindigkeit links */
	private Short aktMotL = new Short((short) 0);

	/** Servo Klappe */
	private Integer aktDoor;

	/** Reserve Servo */
	private Integer aktServo;

	/** Zustand der LEDs */
	private Integer aktLed;

	// Sensoren:

	/**
	 * vom linken IR-Sensor gemessener Wert, momentan der Abstand zum naechsten
	 * Objekt in Millimetern
	 */
	private Short sensIrL = new Short((short) 0);

	/**
	 * vom rechten IR-Sensor gemessener Wert, momentan der Abstand zum naechsten
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
	public CtBot() {
		super();
		world = Controller.getWorld();
		createBranchGroup();
	}

	/**
	 * Alternativer Konstruktor
	 * 
	 * @param pos
	 *            Die initiale Position
	 * @param head
	 *            Die initiale Blickrichtung
	 */
	public CtBot(Point3f pos, Vector3f head) {
		super();
		world = Controller.getWorld();
		createBranchGroup();
		Vector3f vec = new Vector3f(pos);
		vec.z += getHeight()/2 + getGroundClearance();
		setPos(vec);
		setHeading(head);
	}

	/*
	 * (non-Javadoc) Der Aufruf dieser Methode direkt nach dem Erzeugen sorgt
	 * dafuer, dass der Bot ï¿½ber ein passendes ControlPanel verfuegt
	 * 
	 * @see ctSim.Model.Bot#providePanel()
	 */
	public void providePanel() {
		this.setPanel(new CtControlPanel(this));
	}

	/**
	 * Erzeugt die 3D-Repraesentation eines Bots
	 */
	private void createBranchGroup() {
		// Translationsgruppe fuer den Bot
		TransformGroup tg = new TransformGroup();
		Transform3D transform = new Transform3D();
		tg = new TransformGroup(transform);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);

		// Rotationsgruppe fuer den Bot
		TransformGroup rg = new TransformGroup();
		transform = new Transform3D();
		rg = new TransformGroup(transform);
		rg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		rg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		tg.addChild(rg);
		// Die Rotation haengt im Baum des Szenegraphen unterhalb der
		// Translation

		// Bot erzeugen
		Shape3D realBot = createBotShape();
		realBot.setAppearance(world.getWorldView().getBotAppear());
		realBot.setName(getName() + " Body");
		// Körper "pickable" machen um Kollisionen mit anderen Bots
		// zu erkennen
		realBot.setPickable(true);
		// "Pickable" muss für die eigene Kollisionsabfrage abschaltbar sein
		realBot.setCapability(Cylinder.ALLOW_PICKABLE_WRITE);
		// Körper merken um später bei der eigenen Kollisionsabfrage die 
		// "Pickable"-Eigenschaft ändern zu können
		botBody = realBot;
		rg.addChild(realBot);
		
		// Die Grenzen (Bounds) des Bots sind wichtig
		// fuer die Kollisionserkennung.
		// Die Grenze des Roboters wird vorlaefig definiert ueber
		// eine Sphaere mit Radius der Bot-Grundplatte um die Position des Bot
		setBounds(new BoundingSphere(new Point3d(super.getPos()), BOT_RADIUS));

		// Jetzt wird noch alles nett verpackt
		BranchGroup bg = new BranchGroup();
		bg.addChild(tg);
		bg.compile();

		setTranslationGroup(tg);
		setRotationGroup(rg);
		setBotBG(bg);
	}

	/**
	 * Baut die 3D-Representation des Bot Körpers aus 2D Polygonen zusammen
	 *  
	 * @return Körper des Bots 
	 */
	private Shape3D createBotShape() {
		
		Shape3D bs = new Shape3D();
		// Anzahl der Ecken um den Kreis des Bots zu beschreiben.
		// Mehr sehen besser aus, benötigen aber auch mehr Rechenzeit.
		int N = 10;
		// Anzahl der verwendeten Punkte
		int totalN = 2 * (N + 2);
		// data muss pro Punkt die Werte x, y und z speichern
		float[] data = new float[totalN * 3];
		// zwei Polygone(Deckel und Boden) mit je N+2 Ecken
		int stripCounts[] = { N + 2, N + 2 };
		float r = (float) BOT_RADIUS;
		float h = (float) BOT_HEIGHT / 2;
		// Zähler
		int n;
		// Koordinaten
		float x, y;
		// Winkel
		double alpha = 0.0;
		
		// Bot Deckel erzeugen
		//
		// Winkel des vollen Kreises (in Bogenmaß)
		double circle = 2.0 * Math.PI;
		// halber Winkel der Öffnung des Bots
		double opening = Math.asin((FACH_LENGTH / 2) / r);
		
		// Rand des Deckels erzeugen, beachte dabei die Öffnung
		for (n = 0; n < N; n++) {
			alpha = opening + (((circle - 2 * opening) / (N - 1)) * n);
			x = (float) (r * Math.cos(alpha));
			y = (float) (r * Math.sin(alpha));
			data[3 * n] = x;
			data[3 * n + 1] = y;
			data[3 * n + 2] = h; // 0
		}
		
		// Fach in die Öffnung des Deckels einbauen
		data[3 * n] = r - (float) FACH_DEPTH;
		data[3 * n + 1] = (float) -FACH_LENGTH / 2;
		data[3 * n + 2] = h; // 2
		n++;
		data[3 * n] = r - (float) FACH_DEPTH;
		data[3 * n + 1] = (float) FACH_LENGTH / 2;
		data[3 * n + 2] = h; // 2
		n++;
		
		// Bot Deckel als Kopieren um ihn als Boden zu verwenden.
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
		
		// Hinzufügen des Deckels und des Bodens zur Bot-Shape3D 
		bs.addGeometry(gi.getGeometryArray());
		
		
		// Erzeugen der äußeren Seitenverkleidung
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

		// Hinzufügen der äußern Seitenverkleidung zur Bot-Shape3D
		bs.setGeometry(tsa);
		
		// Erzeugen der Wände des Faches
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
		
		// Hinzufügen der Fachwände zur Bot-Shape3D 
		bs.addGeometry(qa);
		
		// Die folgenden Zeilen führen dazu, das die Hülle des
		// Bots durchsichtig wird und nur die Wireframe gezeichnet
		// wird. Weiterhin werden auch die Rückseiten gezeichnet.
		
		// PolygonAttributes polyAppear = new PolygonAttributes();
		// polyAppear.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		// polyAppear.setCullFace(PolygonAttributes.CULL_NONE);
		// Appearance twistAppear = new Appearance();
		// twistAppear.setPolygonAttributes(polyAppear);
		// bs.setAppearance(twistAppear);
		
		return bs;
	}
	
	
	// Get()- und Set()-Methoden.

	// Alle Set()-Methoden sind gegen konkurrierende Manipulationen geschuetzt.

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
		if(distance * 1000 > Short.MAX_VALUE)
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
	public short getAktMotL() {
		return aktMotL.shortValue();
	}

	/**
	 * @param speedL
	 *            Einzutragender Wert fuer speedL
	 */
	public void setAktMotL(short speedL) {
		if (speedL >= PWM_MAX) {
			speedL = PWM_MAX;
		}
		if (speedL <= PWM_MIN) {
			speedL = PWM_MIN;
		}
		synchronized (aktMotL) {
			this.aktMotL = new Short(speedL);
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
		if(distance * 1000 > Short.MAX_VALUE)
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
	public short getAktMotR() {
		return aktMotR.shortValue();
	}

	/**
	 * @param speedR
	 *            Einzutragender Wert fuer speedR
	 */
	public void setAktMotR(short speedR) {
		if (speedR >= PWM_MAX) {
			speedR = PWM_MAX;
		}
		if (speedR <= PWM_MIN) {
			speedR = PWM_MIN;
		}
		synchronized (aktMotR) {
			this.aktMotR = new Short(speedR);
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
	 * @return Gibt den Wert von aktDoor zurueck
	 */
	public int getAktDoor() {
		return aktDoor.intValue();
	}

	/**
	 * @param aktDoor
	 *            Der Wert von aktDoor, der gesetzt werden soll
	 */
	public void setAktDoor(int aktDoor) {
		this.aktDoor = new Integer(aktDoor);
	}

	/**
	 * @return Gibt den Wert von aktLed zurueck
	 */
	public int getAktLed() {
		return aktLed.intValue();
	}

	/**
	 * @param aktLed
	 *            Der Wert von aktLed, der gesetzt werden soll
	 */
	public void setAktLed(int aktLed) {
		this.aktLed = new Integer(aktLed);
	}

	/**
	 * @return Gibt den Wert von aktServo zurueck
	 */
	public int getAktServo() {
		return aktServo.intValue();
	}

	/**
	 * @param aktServo
	 *            Der Wert von aktServo, der gesetzt werden soll
	 */
	public void setAktServo(int aktServo) {
		this.aktServo = new Integer(aktServo);
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

}
