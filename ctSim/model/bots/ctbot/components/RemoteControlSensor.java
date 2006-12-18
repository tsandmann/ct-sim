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
package ctSim.model.bots.ctbot.components;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.sensors.SimpleSensor;
import ctSim.view.gui.sensors.SensorGroupGUI;
import ctSim.view.gui.sensors.Sensors;

/**
 * Klasse der Fernbedienung (auch eine Art Sensor)
 * 
 * @author Peter Koenig
 */
public class RemoteControlSensor extends SimpleSensor<Integer> {
	
	/**
	 * Der Konstruktor
	 * @param name Sensor-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
	@SuppressWarnings("boxing")
	public RemoteControlSensor(String name, Point3d relPos, Vector3d relHead) {
		
		super(name, relPos, relHead);
	}
	
	/** 
	 * 	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "RC5-Fernbedienung: "+this.getName(); //$NON-NLS-1$
	}

	/** 
	 * @see ctSim.model.bots.components.Sensor#updateValue()
	 */
	@SuppressWarnings("boxing")
	@Override
	public Integer updateValue() {
		return null;
	}
	
	/** 
	 * @see ctSim.model.bots.components.Sensor#getSensorGroupGUI()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public SensorGroupGUI getSensorGroupGUI() {
		
		SensorGroupGUI gui = Sensors.getGuiFor(this);
		gui.addSensor(this);
		return gui;
	}
}