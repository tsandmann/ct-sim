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

package ctSim.view.sensors;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BoxLayout;

import ctSim.model.bots.components.Sensor;
import ctSim.view.ComponentGroupGUI;

/**
 * Abstrakte Klasse für grafische Anzeige von Sensorgruppen
 * @author Felix Beckwermert
 * @param <E>
 */
public abstract class SensorGroupGUI<E extends Sensor> extends ComponentGroupGUI<E> {
	
	private Set<E> sensors;
	
	/**
	 * Der Konstruktor
	 */
	SensorGroupGUI() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.sensors = new TreeSet<E>(new Comparator<E>() {
			
			public int compare(E sens1, E sens2) {
				
				if(sens1.getId() < sens2.getId())
					return -1;
				if(sens1.getId() > sens2.getId())
					return 1;
				return 0;
			}
		});
	}
	
	/**
	 * @return alle Sensoren der Gruppe
	 */
	protected Set<E> getAllSensors() {
		
		return this.sensors;
	}
	
	/**
	 * Fuegt einen Sensor hinzu
	 * @param act Der Sensor
	 */
	public void addSensor(E act) {
		
		this.sensors.add(act);
	}
	
	/**
	 * Fuegt verschiedene Gruppen zusammen
	 * @param actGUI Die Gruppe, deren Komponenten hinzugefuegt werden sollen
	 */
	public void join(SensorGroupGUI<E> actGUI) {
		
		this.sensors.addAll(actGUI.getAllSensors());
	}
}
