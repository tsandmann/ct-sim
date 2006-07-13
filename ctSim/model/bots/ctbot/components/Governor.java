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
 */package ctSim.model.bots.ctbot.components;

import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.actuators.SimpleActuator;

// TODO: anpassen... nicht abstract
// TODO: sollte double sein?
/**
 * Klasse der Motoransteuerung
 * 
 * @author Felix Beckwermert
 */
public class Governor extends SimpleActuator<Integer> {
	
	private Integer val;
	
	/**
	 * Der Konstruktor
	 * @param world Welt
	 * @param bot Bot
	 * @param name Sensor-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
@SuppressWarnings("boxing")
public Governor(String name, Point3d relPos, Vector3d relHead) {
		
		super(name, relPos, relHead);
		
		this.val = 0;
	}
	
	/**
	 * @see ctSim.model.bots.components.BotComponent#getType()
	 */
	// TODO: Ueberfluessig?
	@Override
	public String getType() {
		
		return "Regulator"; //$NON-NLS-1$
	}
	
	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	// TODO: should be abstract (?)
	@Override
	public String getDescription() {
		// TODO:
		return null;
	}
	
	/**
	 * @see ctSim.model.bots.components.Actuator#setValue(E)
	 */
	// TODO: should be abstract (?)
	@Override
	public void setValue(Integer value) {
		
		this.val = value;
	}
	
	/**
	 * @see ctSim.model.bots.components.Actuator#getValue()
	 */
	// TODO: should be abstract (?)
	@Override
	public Integer getValue() {
		
		return this.val;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getShape()
	 */
	@Override
	public Shape3D getShape() {
		// TODO Auto-generated method stub
		return new Shape3D();
	}
}