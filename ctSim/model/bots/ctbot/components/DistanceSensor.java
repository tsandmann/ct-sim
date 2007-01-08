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
import ctSim.model.bots.components.sensors.SimpleSensor;

/**
 * Klasse der Distanzsensoren
 *
 * @author Felix Beckwermert
 */
public class DistanceSensor extends SimpleSensor<Double> {

	// TODO:
	private World world;
	private Bot bot;

	private double angle = Math.PI / 180 * 3;  // 3°

	/**
	 * Der Konstruktor
	 * @param w Welt
	 * @param b Bot
	 * @param name Sensor-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
	public DistanceSensor(World w, Bot b, String name, Point3d relPos, Vector3d relHead) {

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
		// TODO Auto-generated method stub
		return "Infrarot Abstands-Sensor: "+this.getName(); //$NON-NLS-1$
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
		if (side.equals("IrL")) { //$NON-NLS-1$
			vecX = new Vector3d(-this.bot.getHeading().y, this.bot.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_DIST_Z));
		} else
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

	/**
	 * @see ctSim.model.bots.components.Sensor#updateValue()
	 */
	@SuppressWarnings("boxing")
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
}