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
package ctSim.model.bots.components.actuators;


import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;


/**
 * Abstrakte Oberklasse aller einfachen Aktuatoren (Aktuatoren, die nur durch 
 * eine Groesse angesteuert werden)
 * 
 * @author Felix Beckwermert
 * 
 * @param <E>
 *            Der Werttyp der Sensoren (Verfeinerungen von Number)
 */public abstract class SimpleActuator<E extends Number> extends Actuator<E> {
	
	//private SimpleActuatorGroupGUI groupGUI = new SimpleActuatorGroupGUI();
	
	/**
	 * Der Konstruktor
	 * @param name Aktuator-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
	public SimpleActuator(String name, Point3d relPos, Vector3d relHead) {
		super(name, relPos, relHead);
	}
	
	/** 
	 * @see ctSim.model.bots.components.Actuator#getActuatorGroupGUI()
	 */
	/* Hacky: The groupGUI-Member must not be the GUI-Comp. showing
	 *        this Actuator (see 'join' in 'ActuatorGroupGUI'
	 *        and 'initGUI' in 'DefBotPanel')...
	 */
//	public ActuatorGroupGUI getActuatorGroupGUI() {
//		
//		this.groupGUI.addActuator(this);
//		return groupGUI;
//	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ActuatorGroupGUI getActuatorGroupGUI() {
		
		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
		gui.addActuator(this);
		return gui;
	}
}
