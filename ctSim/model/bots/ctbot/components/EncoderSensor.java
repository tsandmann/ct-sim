package ctSim.model.bots.ctbot.components;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.QuadArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TriangleStripArray;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.sensors.SimpleSensor;

public class EncoderSensor extends SimpleSensor<Integer> {
	
	/** maximale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MAX = 255;
	
	/** maximale Geschwindigkeit in Umdrehungen pro Sekunde */
	public static final float UPS_MAX = (float) 151 / (float) 60;
	
	/**
	 * Interne Zeitbasis in Millisekunden -- Zeitaenderung seit letztem
	 * Simulationschritt
	 */
	protected long deltaT = 10;
	
	/**
	 * Nach der letzten Simulation noch nicht verarbeitete Teil-Encoder-Schritte
	 * (links)
	 */
	private double encoderRest = 0d;
	
	/** Anzahl an Encoder-Markierungen auf einem Rad */
	public static final short ENCODER_MARKS = 60;
	
	
	/* **********************************************************************
	 * **********************************************************************
	 * 
	 */
	
	// TODO:
	private World world;
	private Bot bot;
	

	//private double angle = Math.PI / 180 * 180;  // 180°
	
	private Shape3D shape;
	
	// TODO:
	private Actuator governor;
	
	public EncoderSensor(World world, Bot bot, String name, Point3d relPos, Vector3d relHead, /* TODO */ Actuator gov) {
		
		super(name, relPos, relHead);
		
		// TODO:
		this.world = world;
		this.bot   = bot;
		
		this.governor = gov;
	}
	
	@Override
	public String getType() {
		// TODO: Kodiersensor?
		return "Infrarot";
	}

	@Override
	public String getDescription() {
		// TODO: Kodiersensor?
		return "Infrarot Kodier-Sensor: "+this.getName();
	}
	
	/**
	 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde
	 * 
	 * @param motPWM PWM-Verhaeltnis 
	 * @return Umdrehungen pro Sekunde
	 */
	private float calculateWheelSpeed(int motPWM) {
		float tmp = ((float) motPWM / (float) PWM_MAX);
		tmp = tmp * UPS_MAX;
		return tmp;
		// TODO Die Kennlinien der echten Motoren ist nicht linear
	}
	
	@Override
	public Integer updateValue() {
		
		// Anzahl der Umdrehungen der Raeder
		double turns = calculateWheelSpeed((Integer)this.governor.getValue());
		turns = turns * deltaT / 1000.0f;
		
		// Encoder-Schritte als Gleitzahl errechnen:
		// Anzahl der Drehungen mal Anzahl der Markierungen,
		// dazu der Rest der letzten Runde
		double tmp = (turns * ENCODER_MARKS) + encoderRest;
		// Der Bot bekommt nur ganze Schritte zu sehen,
		int encoderSteps = (int) Math.floor(tmp);
		// aber wir merken uns Teilschritte intern
		encoderRest = tmp - encoderSteps;
		// und speichern sie.
		//this.setSensEncL((short) (this.getSensEncL() + encoderSteps));
		
//		System.out.println(this.getName()+":  "+encoderSteps);
		
		// TODO: Achtung! Value kann 'null' sein
		//return this.getValue()+encoderSteps;
		return encoderSteps;
	}
	
	public Shape3D getShape() {
		
		// TODO: ?
		return new Shape3D();
	}
}