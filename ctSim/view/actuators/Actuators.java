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
package ctSim.view.actuators;

import ctSim.model.bots.components.actuators.Indicator;
import ctSim.model.bots.components.actuators.LogScreen;
import ctSim.model.bots.components.actuators.SimpleActuator;

/**
 * Hilfsklasse fuer Sensoren
 * @author Felix Beckwermert
 */
public class Actuators {
	
	/**
	 * Erzeugt eine GUI fuer einen einfachen Aktuator
	 * @param a Der Aktuator
	 * @return Die GUI fuer den Aktuator
	 */
	public static ActuatorGroupGUI getGuiFor(@SuppressWarnings("unused") SimpleActuator a) {
		
		//System.out.println("Simple");
		return new SimpleActuatorGroupGUI();
	}
	
	/**
	 * Erzeugt eine GUI fuer einen einfachen Indikator
	 * @param a Der Indikator
	 * @return Die GUI fuer den Indikator
	 */
	public static ActuatorGroupGUI getGuiFor(@SuppressWarnings("unused") Indicator a) {
		return new IndicatorGroupGUI();
	}
	
//	public static ActuatorGroupGUI getGuiFor(Display a) {
//		
//		return new DisplayGUI();
//	}
	
	/**
	 * Erzeugt eine GUI fuer einen LogScreen
	 * @param a Der LogScreen
	 * @return Die GUI fuer den LogScreen
	 */
	public static ActuatorGroupGUI getGuiFor(@SuppressWarnings("unused") LogScreen a) {
		
		return new LogScreenGUI();
	}
}
