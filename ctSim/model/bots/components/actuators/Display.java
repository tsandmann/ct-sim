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

import ctSim.view.gui.actuators.ActuatorGroupGUI;
import ctSim.view.gui.actuators.Actuators;

/**
 * Das Display der Bots im Simulator
 * @author Felix Beckwermert
 *
 */
public class Display extends LogScreen {
	
	/**
	 * Der Konstruktor
	 * @param name Display-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
	public Display(String name, Point3d relPos, Vector3d relHead) {
		
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
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/** 
	 * @see ctSim.model.bots.components.actuators.LogScreen#getBackgroundColor()
	 */
	@Override
	public Color getBackgroundColor() {
		return new Color(120, 150, 90);
	}
	
	/** 
	 * @see ctSim.model.bots.components.actuators.LogScreen#getFont()
	 */
	@Override
	public Font getFont() {
		return new Font("Monospaced", Font.BOLD, 12); //$NON-NLS-1$
	}
	
	/** 
	 * @see ctSim.model.bots.components.actuators.LogScreen#hasToRewrite()
	 */
	@Override
	public boolean hasToRewrite() {
		return true;
	}
}
