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

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.World;

/**
 * Abstrakte Oberklasse fuer alle simulierten c't-Bots
 * 
 * @author Felix Beckwermert
 *
 */
public abstract class CtBotSim extends CtBot {
	
	/**
	 * Der Konstruktor
	 * @param world Die Welt
	 * @param name Name des Bot
	 * @param pos Position
	 * @param head Blickrichtung
	 */
	public CtBotSim(@SuppressWarnings("unused") World world, String name, Point3d pos, Vector3d head) {
		super(name, pos, head);
	}

	/* 
	 * Hier erfolgt die Aktualisierung der gesamten Simualtion
	 * @see ctSim.model.AliveObstacle#updateSimulation(long)
	 */
	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime);
		// TODO Diese Funktion ist noch leer, da der gesamt kram leider noch in ctbotsimtcp steht
	}
	
	
}