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

package ctSim.Model;

import javax.vecmath.*;
import javax.media.j3d.*;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;

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
	public static final double BOT_HEIGHT = 0.100d;

	/** Breite des Faches [m] */
	public static final double FACH_LENGTH = 0.030d;

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
	public static final double SENS_IR_ABSTAND_X = 0.045d;

	/** Abstand Zentrum IR-Sensoren in Vorausrichtung (Y) [m] */
	public static final double SENS_IR_ABSTAND_Y = BOT_RADIUS;

	/** Abstand Zentrum IR-Sensoren in Hochrichtung (Y) [m] */
	public static final double SENS_IR_ABSTAND_Z = 0.0d;

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
		setPos(new Vector3f(pos));
		setHeading(head);
	}

	/*
	 * (non-Javadoc) Der Aufruf dieser Methode direkt nach dem Erzeugen sorgt
	 * dafuer, dass der Bot über ein passendes ControlPanel verfuegt
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
		transform = new Transform3D();
		transform.setRotation(new AxisAngle4f((float) 1, (float) 0, (float) 0,
				(float) Math.PI / 2));
		TransformGroup tmp = new TransformGroup(transform);

		// Grundform ist ein Zylinder
		Cylinder cyl = new Cylinder((float) BOT_RADIUS, (float) BOT_HEIGHT);
		cyl.setAppearance(world.getWorldView().getBotAppear());
		cyl.setName(getName() + " Cylinder");
		// TODO: Bots sind momentan nicht "pickable", erkennen sich
		// daher nicht gegenseitig als Hindernisse.
		// Setzt man die Bots "pickable", dann stellen sie fortlaufend
		// Kollisionen mit sich selbst fest.
		cyl.setPickable(false);
		tmp.addChild(cyl);

		// Die Grenzen (Bounds) des Bots sind wichtig
		// fuer die Kollisionserkennung.
		// Die Grenze des Roboters wird vorlaefig definiert ueber
		// eine Sphaere mit Radius der Bot-Grundplatte um die Position des Bot
		setBounds(new BoundingSphere(new Point3d(super.getPos()), BOT_RADIUS));
		rg.addChild(tmp);

		// Das Fach vorne im Bot ist derzeit ein Quader, der sich durch
		// die gleiche Optik wie der Boden tarnt
		transform = new Transform3D();
		transform.setTranslation(new Vector3f(
				(float) (BOT_RADIUS - FACH_LENGTH), 0f, 0f));
		tmp = new TransformGroup(transform);

		Box box = new Box((float) FACH_LENGTH, (float) FACH_LENGTH,
				(float) BOT_HEIGHT, world.getWorldView().getPlaygroundAppear());
		box.setName(getName() + " Fach");
		box.setPickable(false);
		tmp.addChild(box);
		rg.addChild(tmp);

		// Jetzt wird noch alles nett verpackt
		BranchGroup bg = new BranchGroup();
		bg.addChild(tg);
		bg.compile();

		setTranslationGroup(tg);
		setRotationGroup(rg);
		setBotBG(bg);
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
		short irL = (short) (distance * 1000);
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
		short irR = (short) (distance * 1000);
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
	 * @param encr
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
