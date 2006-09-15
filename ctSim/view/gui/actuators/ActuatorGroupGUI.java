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
package ctSim.view.gui.actuators;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;

import ctSim.model.bots.components.Actuator;
import ctSim.view.gui.ComponentGroupGUI;

/**
 * Abstrakte Klasse fuer grafische Anzeige von Aktuatorgruppen
 * @author Felix Beckwermert
 * @param <E> Der Datentyp der Aktuatoren
 */
public abstract class ActuatorGroupGUI<E extends Actuator> extends ComponentGroupGUI<E> {
	
	private Set<E> actuators;
	
	/**
	 * Der Konstruktor 
	 */
	ActuatorGroupGUI() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.actuators = new TreeSet<E>(new Comparator<E>() {
			public int compare(E act1, E act2) {
				return act1.getId() - act2.getId();
			}
		});
	}
	
	/**
	 * @return Alle Aktuatoren der Gruppe
	 */
	protected Set<E> getAllActuators() {
		
		return this.actuators;
	}
	
	/**
	 * @param act Der Aktuator, der der Gruppe hinzugefuegt werden soll
	 */
	public void addActuator(E act) {
		
		this.actuators.add(act);
	}

	/**
	 * Fuegt verschiedene Gruppen zusammen
	 * @param actGUI Die Gruppe, deren Komponenten hinzugefuegt werden sollen
	 */
	public void join(ActuatorGroupGUI<E> actGUI) {
		
		this.actuators.addAll(actGUI.getAllActuators());
	}
}
