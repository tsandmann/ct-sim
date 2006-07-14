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

import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;

// TODO: Should be off Enumeration-Type
/**
 * Abstrakte Oberklasse aller Indikatoren
 * 
 * @author Felix Beckwermert
 * 
 */
public abstract class Indicator extends Actuator<Boolean> {
	
	@SuppressWarnings("unused")
	private Boolean val;
	
	private Color off;
	private Color on;
	
	/**
	 * Der Konstruktor
	 * @param name Indikator-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 * @param of Farbe im inaktiven Zustand
	 * @param onn Farbe im aktivierten Zustand
	 */
	@SuppressWarnings("boxing")
	public Indicator(String name, Point3d relPos, Vector3d relHead, Color of, Color onn) {
		super(name, relPos, relHead);
		// TODO Auto-generated constructor stub
		
		this.off = of;
		this.on = onn;
		
		this.val = false;
	}

//	@Override
//	protected void setValue(Boolean value) {
//		
//		this.val = value;
//	}
//
//	@Override
//	public Boolean getValue() {
//		
//		return this.val;
//	}

	/** 
	 * @see ctSim.model.bots.components.BotComponent#getType()
	 */
	@Override
	public String getType() {
		
		return "Indicator"; //$NON-NLS-1$
	}

	/** 
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		// TODO:
		return null;
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
	 * @return Die Farbe des Indikators
	 */
	@SuppressWarnings("boxing")
	public Color getColor(boolean v) {
		
		if(v)
			return this.on;
		
		return this.off;
	}

//	@Override
//	public void setValue(Boolean value) {
//		
//		this.val = value;
//	}
//
//	@Override
//	public Boolean getValue() {
//		
//		return this.val;
//	}

	/** 
	 * @see ctSim.model.bots.components.BotComponent#getShape()
	 */
	@Override
	public Shape3D getShape() {
		
		return new Shape3D();
	}
	
	//////////////////////////////////////////////////////////////////////
	// Testfaelle:
	
	// Testfall 1:
//	public static Actuator getTestActuator1(String name) {
//		
//		Actuator act1 = new Indicator("Test:Actuator:Indicator - "+name,
//				"blubb", 0.342d,
//				colLed1, colLed1Akt);
//		
//		return act1;
//	}
//	
//	public static Actuator getTestActuator2(String name) {
//		
//		Actuator act1 = new Indicator("Test:Actuator:Indicator - "+name,
//				"blubb", 0.342d,
//				colLed3, colLed3Akt);
//		
//		return act1;
//	}
}