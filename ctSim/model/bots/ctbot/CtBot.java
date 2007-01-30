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

import java.awt.Color;

import ctSim.model.bots.BasicBot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.Sensors;

/**
 * Abstrakte Oberklasse fuer alle c't-Bots
 */
public abstract class CtBot extends BasicBot {
	private static final Color[] ledColors = {
		new Color(  0,  84, 255), // blau
		new Color(  0,  84, 255), // blau
		Color.RED,
		new Color(255, 200,   0), // orange
		Color.YELLOW,
		Color.GREEN,
		new Color(  0, 255, 210), // tuerkis
		Color.WHITE,
	};

	//$$ Alle Konstanten: verwendet?
	/** Abstand vom Zentrum zur Aussenkante des Bots [m] */
	protected static final double BOT_RADIUS = 0.060d;

	/** Hoehe des Bots [m] */
	protected static final double BOT_HEIGHT = 0.120d;

	/** Bodenfreiheit des Bots [m] */
	protected static final double BOT_GROUND_CLEARANCE = 0.015d;

	public CtBot(String name) {
		super(name);

		components.add(
			new Actuators.Governor(true),
			new Actuators.Governor(false),
			new Actuators.LcDisplay(20, 4),
			new Actuators.Log(),
			new Actuators.DoorServo(),
			new Sensors.Encoder(true),
			new Sensors.Encoder(false),
			new Sensors.Distance(true),
			new Sensors.Distance(false),
			new Sensors.Line(true),
			new Sensors.Line(false),
			new Sensors.Border(true),
			new Sensors.Border(false),
			new Sensors.Light(true),
			new Sensors.Light(false),
			new Sensors.Mouse(true),
			new Sensors.Mouse(false),
			new Sensors.RemoteControl(),
			new Sensors.Door(),
			new Sensors.Trans(),
			new Sensors.Error()
		);

		// LEDs
		int numLeds = ledColors.length;
		for (int i = 0; i < numLeds; i++) {
			String ledName = "LED " + (i + 1)
					 + (i == 0 ? " (vorn rechts)" :
						i == 1 ? " (vorn links)" : "");
			components.add(
				new Actuators.Led(ledName, i, ledColors[i]));
		}

		//$$$ Toter Code
		// Einfachen Konstruktor aufrufen:
//		Vector3f vec = new Vector3f(pos);
//		// TODO: Was das!?
//		vec.z += getHeight() / 2 + getGroundClearance();
//		setPos(vec);
//		setHeading(head);
	}
}
