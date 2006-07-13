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
package ctSim.model.bots.components;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.view.actuators.ActuatorGroupGUI;

/**
 * Abstrakte Oberklasse aller Aktuatoren
 * @author Felix Beckwermert
 *
 * @param <E> Der Werttyp der Aktuatoren
 */
public abstract class Actuator<E> extends BotComponent {
	
	/**
	 * Der Konstruktor
	 * @param name Aktuator-Name
	 * @param relativePosition relative Position zum Bot
	 * @param relativeHeading relative Blickrichtung zum Bot
	 */
	public Actuator(String name, Point3d relativePosition, Vector3d relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @return Gibt die GUI fuer eine Aktuatorgruppe zurueck
	 */
	public abstract ActuatorGroupGUI getActuatorGroupGUI();
//	public ActuatorGroupGUI getActuatorGroupGUI() {
//		
//		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
//		gui.addActuator(this);
//		return gui;
//	}
	
	/**
	 * Setzt den Aktuator-Wert
	 * @param value Der Wert fuer den Aktuator
	 */
	public abstract void setValue(E value);
	
	/**
	 * @return Der Wert der Aktuatoren
	 */
	public abstract E getValue();
}
