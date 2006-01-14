package ctSim.Model;

//import java.util.Vector;

import javax.vecmath.*;
import javax.media.j3d.*;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;

import ctSim.Controller.Controller;
import ctSim.View.CtControlPanel;


/**
 * Diese Klasse f�gt dem AbstractBot Felder f�r die Zust�nde der Sensoren und Aktuatoren des c't-Bots hinzu.
 * Sie kann ebenfalls nicht instanziiert werden und muss daher in weiteren Klassen abgeleitet werden. 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 */

public abstract class CtBot extends Bot {

	// �ffentliche Konstanten: Wertebereiche f�r Motoren und Sensoren
	/** Abstand Zentrum Aussenkante des Bots [m]*/
	public static final double BOT_RADIUS = 0.060d;

	/** Höhe des Bots [m]*/
	public static final double BOT_HEIGHT = 0.100d;
	
	/** Breite des Faches [m]*/
	public static final double FACH_LENGTH = 0.030d;
	
	/** maximale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MAX = 20;

	/** minimale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MIN = -20;

	/** maximale Geschwindigkeit in Umdrehungen pro Sekunde */
	public static final float UPS_MAX = (float) 151 / (float) 60;

	/* maximaler Wert IR-Sensor bei realistischer Bot-Modellierung */
	// public static final short IRMAX = 1024;

	/* minimaler Wert IR-Sensor bei realistischer Bot-Modellierung */
	// public static final short IRMIN = 0;

	/** Anzahl an Encoder-Markierungen auf einem Rad */
	public static final short ENCODER_MARKS = 40;

	/** Umfang eines Rades [m] */
	public static final double RAD_UMFANG = Math.PI *0.050d;

	/** Abstand Mittelpunkt zu Rad [m] */
	public static final double RAD_ABSTAND = 0.050d;

	/** Abstand Zentrum IR-Sensoren in Achsrichtung (X)[m]*/
	public static final double SENS_IR_ABSTAND_X = 0.045d;

	/** Abstand Zentrum IR-Sensoren in Vorausrichtung (Y) [m]*/
	public static final double SENS_IR_ABSTAND_Y = BOT_RADIUS;

	/** Abstand Zentrum IR-Sensoren in Hochrichtung (Y) [m]*/
	public static final double SENS_IR_ABSTAND_Z = 0.0d;
	
	/* Capabilities -- Flags, die anzeigen, welche internen Zustände von Sensoren 
	 * oder Aktuatoren über das ControlPanel beeinlussbar sind 
	 */
	
	/** Können die Abgrund-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_BORDER = false;

	/** Kann der Klappen-Sensor beeinflusst werden? */
	public boolean CAP_SENS_DOOR  = false;

	/** Können die Radencoder-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_ENC  = false;

	/** Kann der Fehler-Sensor beeinflusst werden? */
	public boolean CAP_SENS_ERROR  = false;
	
	/** Können die IR-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_IR  = false;
	
	/** Können die Helligkeits-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_LDR  = false;
	
	/** Können die Linien-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_LINE  = false;
	
	/** Können die Maus-Sensoren beeinflusst werden? */
	public boolean CAP_SENS_MOUSE  = false;
	
	/** Kann der Fernbedienungs-Sensor beeinflusst werden? */
	public boolean CAP_SENS_RC5  = true;

	/** Kann der Fach-Sensor beeinflusst werden? */
	public boolean CAP_SENS_TRANS  = true;

	/** Kann der Zustand der Motoren beeinflusst werden? */
	public boolean CAP_AKT_MOT = false;

	/** Kann der Zustande der LEDs beeinflusst werden? */
	public boolean CAP_AKT_LED = false;

	/** Kann der Aktuator für die Klappe beeinflusst werden? */
	public boolean CAP_AKT_DOOR = false;

	/** Kann der Reserve-Servo beeinflusst werden? */
	public boolean CAP_AKT_SERVO = false;

	/** Kann die Richtung des Bots beeinflusst werden? */
	public boolean CAP_HEAD = true;

	/** Kann die Position des Bots beeinflusst werden? */
	public boolean CAP_POS = true;
	// Felder f�r den Zustand des Roboters:

	// Aktuatoren:
	
	/** gewuenschte Motorengeschwindigkeit rechts */
	private Short aktMotR = new Short((short)0);

	/** gewuenschte Motorengeschwindigkeit links */
	private Short aktMotL= new Short((short)0);

	/** Servo Klappe */
	private Integer aktDoor;

	/** Reserve Servo */
	private Integer aktServo;

	/** Zustand der LEDs */
	private Integer aktLed;

	//Sensoren:
	
	/** von IR-Sensoren gemessene Werte, 
	 * momentan der Abstand zum n�chsten Objekt in Millimetern! */
	private Short sensIrL= new Short((short)0);

	/** von IR-Sensoren gemessene Werte, 	 
	 * momentan der Abstand zum n�chsten Objekt in Millimetern! */

	private Short sensIrR= new Short((short)0);

	/** Radencoder links */
	private Short sensEncL= new Short((short)0);

	/** Radencoder rechts */
	private Short sensEncR= new Short((short)0);

	/** Abgrundsensor links */
	private Integer sensBorderL = new Integer (0);

	/** Abgrundsensor rechts */
	private Integer sensBorderR = new Integer (0);

	/** Liniensensor links */
	private Integer sensLineL = new Integer (0);

	/** Liniensensor rechts */
	private Integer sensLineR = new Integer (0);

	/** Helligkeitssensor links */
	private Integer sensLdrL = new Integer (0);

	/** Helligkeitssensor rechts */
	private Integer sensLdrR = new Integer (0);

	/** Ueberwachung Transportfach */
	private Integer sensTrans = new Integer (0);

	/** Ueberwachung Klappe */
	private Integer sensDoor = new Integer (0);

	/** Maussensor DeltaX*/
	private Integer sensMouseDX = new Integer (0);

	/** Maussensor DeltaY*/
	private Integer sensMouseDY = new Integer (0);

	/** TRC5-Fernbedienung */
	private Integer sensRc5 = new Integer (0);

	/** Batterie oder Servofehler */
	private Integer sensError = new Integer (0);

	public CtBot() {
		super();
		world = Controller.getWorld();
		createBranchGroup();
	}	
	
	public CtBot(Point3f pos, Vector3f head) {
		super();
		world = Controller.getWorld();
		createBranchGroup();		
		setPos(new Vector3f(pos));
		setHeading(head);
	}

	public void providePanel(){
		this.setPanel(new CtControlPanel(this));
	}
	
	/**
	 *  Erzeugt die 3D-Repraesentation eines Bots 
	 */
	private void createBranchGroup(){
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
        tg.addChild(rg);	// Die Rotation hängt unterhalb der Translation
            
        // Bot erzeugen
        transform = new Transform3D();
        transform.setRotation(new AxisAngle4f((float)1,(float)0,(float)0,(float)Math.PI/2));
        TransformGroup tmp = new TransformGroup(transform);
        
        // Montage aus Zylinder
        Cylinder cyl = new Cylinder((float)BOT_RADIUS, (float)BOT_HEIGHT);
        cyl.setAppearance(world.getWorldView().getBotAppear());
        cyl.setName(getName()+" Cylinder");
        // TODO: Bots sind momentan nicht pickable, erkennen sich
        // daher nicht gegenseitig als Hindernisse. 
        // Setzt man die Bots pickable, dann stellen sie fortlaufend
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
        transform.setTranslation(new Vector3f((float)(BOT_RADIUS-FACH_LENGTH),0f,0f));
        tmp = new TransformGroup(transform);

        Box box = new Box((float)FACH_LENGTH,(float)FACH_LENGTH,(float)BOT_HEIGHT,world.getWorldView().getPlaygroundAppear());
        box.setName(getName()+" Fach");
        box.setPickable(false);
        tmp.addChild(box);        
        rg.addChild(tmp);
        
        // Und alles nett verpacken
		BranchGroup bg = new BranchGroup();
        bg.addChild(tg);
        bg.compile();
        
        setTranslationGroup(tg);
        setRotationGroup(rg);
        setBotBG(bg);        			
	}
	
	
	
	// Get()- und Set()-Methoden. 
	// Alle Set()-Methoden sind gegen konkurrierende Manipulationen gesch�tzt.

	/* (non-Javadoc)
	 * @see ctSim.Model.AbstractBot#init()
	 */
	protected void init() {
		// TODO Auto-generated method stub
		
	}

	/* Bewegt den Bot und sein 3D-Modell
	 * @see ctSim.Model.AbstractBot#setPos()
	 */
	
	
	/* (non-Javadoc)
	 * @see ctSim.Model.AbstractBot#work()
	 */
	protected void work() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return Distanz zum naechsten Objekt in mm
	 */
	public short getSensIrL() {
		return sensIrL.shortValue();
	}

	/**
	 * Trägt die Sensorwerte ein 
	 * @param irL Abstand in [m]
	 */	
	public void setSensIrL(double distance) {
		// irL arbeitet mit der Distanz im mm !
		short irL = (short)(distance*1000); 
		// TODO an die Sensorkennlinie anpassen
		
/*		if (irL >= IRMAX) {
			irL = IRMAX;
		}
		if (irL <= IRMIN) {
			irL = IRMIN;
		}
*/		
		
		synchronized (sensIrL){
			this.sensIrL = new Short(irL);
		}
	}

	public short getAktMotL() {
		return aktMotL.shortValue();
	}

	public void setAktMotL(short speedL) {
		if (speedL >= PWM_MAX) {
			speedL = PWM_MAX;
		}
		if (speedL <= PWM_MIN) {
			speedL = PWM_MIN;
		}
		synchronized (aktMotL){
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
	 * Trägt die Sensorwerte ein 
	 * @param irL Abstand in [m]
	 */	
	public void setSensIrR(double distance) {
		// irL arbeitet mit der Distanz im mm !
		short irR = (short)(distance*1000); 
		// TODO an die Sensorkennlinie anpassen
/*		if (irR >= IRMAX) {
			irR = IRMAX;
		}
		if (irR <= IRMIN) {
			irR = IRMIN;
		}
*/		synchronized (sensIrR){
			this.sensIrR = new Short(irR);
		}
	}

	public short getAktMotR() {
		return aktMotR.shortValue();
	}

	public void setAktMotR(short speedR) {		
		if (speedR >= PWM_MAX) {
			speedR = PWM_MAX;
		}
		if (speedR <= PWM_MIN) {
			speedR = PWM_MIN;
		}
		synchronized (aktMotR){
			this.aktMotR = new Short(speedR);
		}
	}

	/**
	 * @return Returns the encL.
	 */
	public short getSensEncL() {
		return sensEncL.shortValue();
	}

	/**
	 * @param encL The encL to set.
	 */
	public void setSensEncL(short encL) {
		synchronized (sensEncL){
		this.sensEncL = new Short(encL);
		}
	}

	/**
	 * @return Returns the encR.
	 */
	public short getSensEncR() {
		return sensEncR.shortValue();
	}

	/**
	 * @param encR The encR to set.
	 */
	public void setSensEncR(short encR) {
		synchronized (sensEncR){
		this.sensEncR = new Short(encR);
		}
	}

	/**
	 * @return Returns the border.
	 */
	public int getSensBorderL() {
		return sensBorderL.intValue();
	}

	/**
	 * @param border The border to set.
	 */
	public void setSensBorderL(int borderL) {
		synchronized (sensBorderL){
			this.sensBorderL = new Integer(borderL);
		}
	}

	/**
	 * @return Returns the door.
	 */
	public int getSensDoor() {
		return sensDoor.intValue();
	}

	/**
	 * @param door The door to set.
	 */
	public void setSensDoor(int door) {
		synchronized (sensDoor){
		this.sensDoor = new Integer(door);
		}
	}

	/**
	 * @return Returns the ldrL.
	 */
	public int getSensLdrL() {
		return sensLdrL.intValue();
	}

	/**
	 * @param ldrL The ldrL to set.
	 */
	public void setSensLdrL(int ldrL) {
		synchronized (sensLdrL) {
		this.sensLdrL = new Integer(ldrL);
		}
	}

	/**
	 * @return Returns the ldrR.
	 */
	public int getSensLdrR() {
		return sensLdrR.intValue();
	}

	/**
	 * @param ldrR The ldrR to set.
	 */
	public void setSensLdrR(int ldrR) {
		synchronized (sensLdrR) {
		this.sensLdrR = new Integer(ldrR);
		}
	}

	/**
	 * @return Returns the led.
	 */
	public int getAktLed() {
		return aktLed.intValue();
	}

	/**
	 * @param led The led to set.
	 */
	public void setAktLed(int led) {
		synchronized (aktLed){
			this.aktLed = new Integer(led);
		}
	}

	/**
	 * @return Returns the lineL.
	 */
	public int getSensLineL() {
		return sensLineL.intValue();
	}

	/**
	 * @param lineL The lineL to set.
	 */
	public void setSensLineL(int lineL) {
		synchronized (sensLineL) {
		this.sensLineL = new Integer(lineL);
		}
	}

	/**
	 * @return Returns the lineR.
	 */
	public int getSensLineR() {
		return sensLineR.intValue();
	}

	/**
	 * @param lineR The lineR to set.
	 */
	public void setSensLineR(int lineR) {
		synchronized (sensLineR) {
		this.sensLineR = new Integer(lineR);
		}
	}

	/**
	 * @return Returns the mouseDX.
	 */
	public int getSensMouseDX() {
		return sensMouseDX.intValue();
	}

	/**
	 * @param mouseDX The mouseDX to set.
	 */
	public void setSensMouseDX(int mouseDX) {
		synchronized (sensMouseDX) {
		this.sensMouseDX = new Integer(mouseDX);
		}
	}

	/**
	 * @return Returns the mouseDY.
	 */
	public int getSensMouseDY() {
		return sensMouseDY.intValue();
	}

	/**
	 * @param mouseDY The mouseDY to set.
	 */
	public void setSensMouseDY(int mouseDY) {
		synchronized (sensMouseDY) {
		this.sensMouseDY = new Integer(mouseDY);
		}
	}

	/**
	 * @return Returns the trans.
	 */
	public int getSensTrans() {
		return sensTrans.intValue();
	}

	/**
	 * @param trans The trans to set.
	 */
	public void setSensTrans(int trans) {
		synchronized (sensTrans) {
		this.sensTrans = new Integer(trans);
		}
	}

	/**
	 * @return Returns the borderR.
	 */
	public int getSensBorderR() {
		return sensBorderR.intValue();
	}

	/**
	 * @param borderR The borderR to set.
	 */
	public void setSensBorderR(int borderR) {
		synchronized (sensBorderR){
			this.sensBorderR = new Integer(borderR);
		}
	}

	/**
	 * @return Returns the servoDoor.
	 */
	public int getAktDoor() {
		return aktDoor.intValue();
	}

	/**
	 * @param servoDoor The servoDoor to set.
	 */
	public void setAktDoor(int servoDoor) {
		synchronized (aktDoor) {
			this.aktDoor = new Integer(servoDoor);
		}
	}

	/**
	 * @return Returns the servoRes.
	 */
	public int getAktServo() {
		return aktServo.intValue();
	}

	/**
	 * @param servoRes The servoRes to set.
	 */
	public void setAktServo(int servoRes) {
		synchronized (aktServo){
			this.aktServo = new Integer(servoRes);
		}
	}

	/**
	 * @return Returns the sensRc5.
	 */
	public int getSensRc5() {
		return sensRc5.intValue();
	}

	/**
	 * @param sensRc5 The sensRc5 to set.
	 */
	public void setSensRc5(int sensRc5in) {
		synchronized (sensRc5) {
		this.sensRc5 = new Integer(sensRc5in);
		}
	}

	/**
	 * @return Returns the sensError.
	 */
	public int getSensError() {
		return sensError.intValue();
	}

	/**
	 * @param sensError The sensError to set.
	 */
	public void setSensError(int sensErr) {
		synchronized (sensError){
		this.sensError = new Integer(sensErr);
		}
	}

	/**
	 *  Der c't-Bot ist rund, daher wird als Bounds hier eine BoundingSphere zur�ckgegeben,
	 *  deren Mittelpunkt die aktuelle Position des Roboters ist.
	 */  
/*	public Bounds getBounds(){
		Bounds bounds = new BoundingSphere(new Point3d(this.getPos().x, this.getPos().y, 0), BOT_RADIUS);
		return bounds;
	}
*/
	
	/** 
	 * liefert die Höhe des Bots zurück
	 * @return Höhe in [m] 
	 * @see ctSim.Model.AbstractBot#getHeight()
	 */
	public float getHeight() {
		return (float)BOT_HEIGHT;
	}

	/**
	 * Liefert die Bounds des Bots zurück 
	 * @return Bounds, die den Bot umschliessen
	 * @see ctSim.Model.AbstractBot#getBounds()
	 */
/*	public Bounds getBounds() {
		return bounds
		
//		PickCylinderSegment pickShape = new PickCylinderSegment();
		Vector3f lower= new Vector3f(0f,0f,-getHeight()/2);
		Vector3f upper= new Vector3f(0f,0f,100f);//getHeight()/2);
		
		lower.add(getPos());
		upper.add(getPos());
	
		Cylinder cyl= new Cylinder()
		
//		pickShape.set(new Point3d(lower),new Point3d(upper),(double)BOT_RADIUS);
		
		Transform3D transform = new Transform3D();
	
		Bounds bounds = new getBotBG().getBounds();
		
//		getRotationGroup().getTransform(transform);
//		bounds.transform(transform);
//		getTranslationGroup().getTransform(transform);
//		bounds.transform(transform);
		
//		PickBounds pickShape = new PickBounds(getBotBG().getBounds());
		
		return bounds;
	}
*/	
}
