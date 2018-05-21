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

package ctSim.model;

import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.bots.ctbot.MasterSimulator;

/**
 * Erzeugt einen Simulator für einen Bot, der zu einer Welt gehört
 * und eine 3D-Darstellung besitzt 
 */
public abstract class SimulatorFactory {
	/** SimulatorFactory */
	private SimulatorFactory() {
		// No-op
	}

	/**
	 * Erstellt einen neuen Simulator
	 * 
	 * @param world			Welt, zu der der Bot gehört
	 * @param botWrapper	3D-Darstellung des Bots
	 * @param bot			Bot-Instanz, die simuliert werden soll
	 * @return Simulator für Bot bot
	 */
	public static Runnable createFor(World world, ThreeDBot botWrapper,	SimulatedBot bot) {
		/* ct-Bot per TCP-Verbindung (C-Binary) */
		if (bot instanceof CtBotSimTcp) {
			return new MasterSimulator(world, botWrapper);
		}
		
		/* Testbot (Java-Klasse) */
		if (bot instanceof CtBotSimTest) {
			return new MasterSimulator(world, botWrapper);
		}

		return new Runnable() {
			public void run() {
				// No-op
			}
		};
	}
}
