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

import ctSim.view.sensors.SensorGroupGUI;

/* not synchronized!! */

/** Abstrakte Oberklasse aller Sensoren
* @author Felix Beckwermert
*
* @param <E> Der Werttyp der Sensoren
*/
public abstract class Sensor<E> extends BotComponent {
	
	private E value;
	private Characteristic characteristic;
	private boolean useGuiValue = false;
	private boolean setable = true;
	
	/**
	 * Der Konstruktor
	 * @param name Sensor-Name
	 * @param relPos relative Position zum Bot
	 * @param relHead relative Blickrichtung zum Bot
	 */
	public Sensor(String name, Point3d relPos, Vector3d relHead) {
		super(name, relPos, relHead);
	}
	
	/**
	 * Setzt eine Kennlinie fuer den Sensor
	 * @param ch
	 */
	public final void setCharacteristic(Characteristic ch) {
		
		this.characteristic = ch;
	}
	
	/**
	 * @return true, falls Sensorwert setzbar ist
	 */
	public boolean isSetable() {
		
		return this.setable;
	}
	
	/**
	 * Bestimmt, ob Sensorwert setzbar ist
	 * @param b
	 */
	public void setIsSetable(boolean b) {
		
		this.setable = b;
	}
	
	/**
	 * @return Der Wert der Sensoren
	 */
	public final E getValue() {
		
		return this.value;
	}
	
	/* Sollte nur von GUI aufgerufen werden:
	 * 
	 * - Setzen ist nur erlaubt, wenn entsprechendes flag gesetzt ist
	 * - Wert wird dann beim naechsten update zurueckgegeben (kein updateValue()!)
	 * - naechster Aufruf von getVal() geschieht ohne Kennlinien-lookup
	 * 
	 * Vorsicht: Wenn update oefter als get geschieht, bekommt "man" die entsprechende Eingabe
	 *           von Hand/ ueber die GUI eventuell gar nicht mit (da Wert bereits wieder ueberschrieben)
	 */

	/**
	 * Setzt einen Wert fuer den Sensor
	 * @param val Der Wert zu setzen
	 * @return true, wenn Wert setzbar ist
	 */
	public final boolean setValue(E val) {
		
		if(!this.isSetable())
			return false;
		
		this.value = val;
		this.useGuiValue = true;
		
		return true;
	}
	
	/**
	 * Aktualsiert den Sensor
	 */
	@SuppressWarnings({"boxing","unchecked"})
	public final void update() {
		
		if(this.useGuiValue) {
			
			this.useGuiValue = false;
			return;
		}
		
		this.value = updateValue();
		
		// TODO: Aeusserst haesslich:
		if(this.characteristic != null) {
//			System.out.print(this.getName()+" :  "+this.value+"  ->  ");
			this.value = (E)((Double)((Integer)this.characteristic.lookup((((Number)this.value).intValue())/10)).doubleValue());
//			System.out.println(this.value);
		}
	}
	
	/**
	 * @return Datentyp, der fuer das Update des Sensors benoetigt wird
	 */
	public abstract E updateValue();
	
	/**
	 * @return Gibt die GUI fuer eine Sensorgruppe zurueck
	 */
	public abstract SensorGroupGUI getSensorGroupGUI();
//	public SensorGroupGUI getSensorGroupGUI() {
//		
//		SensorGroupGUI gui = Sensors.getGuiFor(this);
//		gui.addSensor(this);
//		return gui;
//	}
}
