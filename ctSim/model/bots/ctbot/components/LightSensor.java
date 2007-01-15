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

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.sensors.SimpleSensor;

/**
 * Klasse der Lichtsensoren
 *
 * @author Felix Beckwermert
 */
public class LightSensor extends SimpleSensor<Integer> {

	// TODO:
	private World world;
	private Bot bot;


	private double angle = Math.PI / 180 * 180;  // 180°

	/**
	 * Der Konstruktor
	 * @param w Welt
	 * @param b Bot
	 * @param name Sensor-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
public LightSensor(World w, Bot b, String name, Point3d relPos, Vector3d relHead) {

		super(name, relPos, relHead);

		// TODO:
		this.world = w;
		this.bot   = b;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO: Lichtsensor?
		return "Infrarot Licht-Sensor: "+this.getName(); //$NON-NLS-1$
	}

	/**
	 * Liefert die Position eines Lichtsensors zurueck
	 *
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	// Altlast...
	private Point3d getSensLdrPosition(String side) {

		/** Hoehe des Bots [m] */
		double BOT_HEIGHT = 0.120d;

		/** Abstand Zentrum Lichtsensoren in Achsrichtung (X)[m] */
		double SENS_LDR_DIST_X = 0.032d;

		/** Abstand Zentrum Lichtsensoren in Vorausrichtung (Y) [m] */
		double SENS_LDR_DIST_Y = 0.048d;

		/** Abstand Zentrum Lichtsensoren in Hochrichtung (Z) [m] */
		double SENS_LDR_DIST_Z = 0.060d - BOT_HEIGHT / 2;

		double ang = SimUtils.getRotation(this.bot.getHeadingInWorldCoord());
		Transform3D rotation = new Transform3D();
		rotation.rotZ(ang);
		Point3d ptLdr;
		if (side == "LightL") { //$NON-NLS-1$
			ptLdr = new Point3d(-SENS_LDR_DIST_X, SENS_LDR_DIST_Y,
					SENS_LDR_DIST_Z);
		} else {
			ptLdr = new Point3d(SENS_LDR_DIST_X, SENS_LDR_DIST_Y,
					SENS_LDR_DIST_Z);
		}
		rotation.transform(ptLdr);
		ptLdr.add(new Point3d(this.bot.getPositionInWorldCoord()));
		return ptLdr;
	}

	/**
	 * @see ctSim.model.bots.components.Sensor#updateValue()
	 */
	@SuppressWarnings("boxing")
	@Override
	public Integer updateValue() {

//		Debug.out.println("Robbi steht an: "+this.bot.getPosition()+" | "+this.bot.getHeading());
//		Debug.out.println("Sensor ist an relativer Pos.: "+this.getRelPosition()+ " | "+this.getRelHeading());
//		Debug.out.println("Summe ist: "+this.getAbsPosition(this.bot.getPosition(), this.bot.getHeading())
//				+ " | "+this.getAbsHeading(this.bot.getPosition(), this.bot.getHeading()));

		// TODO: Richtig so?
		return this.world.sensLight(
				// TODO: Haesslich:
				//this.getAbsPosition(this.bot.getPosition(), this.bot.getHeading()),
				this.getSensLdrPosition(this.getName()),
				//this.getAbsHeading(this.bot.getPosition(), this.bot.getHeading()),
				this.bot.getHeadingInWorldCoord(),
				this.angle);

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
}