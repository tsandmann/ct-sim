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
package ctSim.view.positions;

import ctSim.model.bot.components.positions.SimplePosition;

/**
 * Hilfsklasse fuer Sensoren
 * @author Felix Beckwermert
 */
public class Positions {
	
	/**
	 * Erzeugt eine GUI fuer einen einfachen Positionsanzeiger
	 * @param pos Der Positionsanzeiger
	 * @return Die GUI fuer den Positionsanzeiger
	 */
	public static PositionGroupGUI getGuiFor(SimplePosition pos) {
		
		return new SimplePositionGroupGUI();
	}
}
