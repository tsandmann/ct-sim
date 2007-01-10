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
package ctSim.model.bots.ctbot.components;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.sensors.SimpleSensor;

/**
 * Klasse der Rad-Encoder
 *
 * @author Felix Beckwermert
 */
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
	//private World world;
	//private Bot bot;


	//private double angle = Math.PI / 180 * 180;  // 180°

	//private Shape3D shape;

	// TODO:
	private Actuator.Governor governor;

	/**
	 * Der Konstruktor
	 * @param w Die Welt
	 * @param bot Der Bot
	 * @param name Name
	 * @param relPos relative Position
	 * @param relHead relative Blickrichtung
	 * @param gov Aktuator
	 */
	public EncoderSensor(String name, Point3d relPos, Vector3d relHead, 
		/* TODO */ Actuator.Governor gov) {

		super(name, relPos, relHead);

		// TODO:
		//this.world = w;
		//this.bot   = bot;

		this.governor = gov;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO: Kodiersensor?
		return "Infrarot Kodier-Sensor: "+this.getName(); //$NON-NLS-1$
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

	/**
	 * @see ctSim.model.bots.components.Sensor#updateValue()
	 */
	@SuppressWarnings({"unchecked","boxing"})
	@Override
	public Integer updateValue() {

		// Anzahl der Umdrehungen der Raeder
		double turns = calculateWheelSpeed(
			governor.getModel().getValue().intValue()); //$$$ umstaendlich
		turns = turns * this.deltaT / 1000.0f;

		// Encoder-Schritte als Gleitzahl errechnen:
		// Anzahl der Drehungen mal Anzahl der Markierungen,
		// dazu der Rest der letzten Runde
		double tmp = (turns * ENCODER_MARKS) + this.encoderRest;
		// Der Bot bekommt nur ganze Schritte zu sehen,
		int encoderSteps = (int) Math.floor(tmp);
		// aber wir merken uns Teilschritte intern
		this.encoderRest = tmp - encoderSteps;
		// und speichern sie.
		//this.setSensEncL((short) (this.getSensEncL() + encoderSteps));

//		System.out.println(this.getName()+":  "+encoderSteps);

		// TODO: Achtung! Value kann 'null' sein
		//return this.getValue()+encoderSteps;
		return encoderSteps;
	}
}