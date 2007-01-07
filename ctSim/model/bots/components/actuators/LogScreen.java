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

import java.awt.Color;
import java.awt.Font;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;
import ctSim.view.gui.actuators.ActuatorGroupGUI;
import ctSim.view.gui.actuators.Actuators;

/**
 * Klasse aller LogScreens
 * 
 * @author Felix Beckwermert
 * 
 */
public class LogScreen extends Actuator<String> {
	
	private String val;
	
	/**
	 * Der Konstruktor
	 * @param name Screen-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
	public LogScreen(String name, Point3d relPos, Vector3d relHead) {
		
		super(name, relPos, relHead);
	}
	
	/** 
	 * @see ctSim.model.bots.components.Actuator#getActuatorGroupGUI()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ActuatorGroupGUI getActuatorGroupGUI() {
		
		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
		gui.addActuator(this);
		return gui;
	}
	
	/** 
	 * @see ctSim.model.bots.components.BotComponent#getType()
	 */
	@Override
	public String getType() {
		return "Log"; //$NON-NLS-1$
	}
	
	/** 
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * @return null
	 */
	public Color getBackgroundColor() {
		return null;
	}
	
	/**
	 * @return null
	 */
	public Font getFont() {
		return null;
	}
	
	/**
	 * @return false
	 */
	public boolean hasToRewrite() {
		return false;
	}

	/**
	 * @param value Der Wert zu setzen
	 */
	@Override
	public void setValue(String value) {
		
		this.val = value;
	}

	/** 
	 * @see ctSim.model.bots.components.Actuator#getValue()
	 */
	@Override
	public String getValue() {
		
		return this.val;
	}
}