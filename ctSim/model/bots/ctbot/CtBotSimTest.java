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

import java.util.Random;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.Sensors;

/**
 * Klasse aller simulierten c't-Bots ("Testbots"), die nur innerhalb des Simulators existieren
 *
 */
public class CtBotSimTest extends CtBot implements SimulatedBot {
	/**
	 * neuer Test-Bot
	 */
	public CtBotSimTest() {
		super("Test-Bot");
	
		components.add(new Sensors.Clock());	
	}

	/**
	 * @see ctSim.model.bots.BasicBot#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Simulierter, in Java geschriebener c't-Bot";
	}

	/** letzter Status */
	private int lastState = 0;
	/** Zufallsgenerator */
	private Random rand = new Random();
	
	/**
	 * @see ctSim.model.bots.SimulatedBot#doSimStep()
	 */
	public void doSimStep() throws InterruptedException {		
		double irl = 0;
		double irr = 0;

		Actuators.Governor govL = null;
		Actuators.Governor govR = null;
		
		for (BotComponent<?> c : components) {
			if (c.getName().equals("IrL")) {
				irl=((Sensors.Distance)(Object)c).get().doubleValue();
			}
			if (c.getName().equals("IrR")) {			
				irr=((Sensors.Distance)(Object)c).get().doubleValue();
			}
			if (c.getName().equals("GovL")) {
				govL=(Actuators.Governor)(Object)c;
			}
			if (c.getName().equals("GovR")) {
				govR=(Actuators.Governor)(Object)c;
			}
		}

		/* Ansteuerung fuer die Motoren in Abhaengigkeit vom Input der IR-Abstandssensoren */

		/* Solange die Wand weit weg ist, wird Stoff gegeben */
		double ll = 255, rr = 255;
		
		/* Falls Wand in Sicht, per Zufall nach links oder rechts drehen */
		if (irl > 160 || irr > 130) {
			switch (lastState) {
			/* Wenn wir bereits drehen, dann in die gleiche Richtung weiterdrehen */
			case 0: {
				if (rand.nextInt(42) < 25) {
					/* linksrum */
					ll = -255;
					rr = 200;
					lastState = 1;
				} else {
					/* rechtsrum */
					ll = 200;
					rr = -255;
					lastState = 2;
				}
				break;
			}
			case 1: {
				/* linksrum */
				ll = -255;
				rr = 200;
				break;
			}
			case 2: {
				/* rechtsrum */
				ll = 200;
				rr = -255;
				break;
			}
			}
		} else {
			/* Drehen beendet */
			lastState = 0;
		}
		
		/* Gewschwindigkeiten an die Motoren weitergeben */
		govL.set(ll);
		govR.set(rr);
	}
}