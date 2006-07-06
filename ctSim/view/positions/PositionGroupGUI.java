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
package ctSim.view.positions;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BoxLayout;

import ctSim.model.bots.components.Position;
import ctSim.view.ComponentGroupGUI;

/**
 * Abstrakte Klasse für grafische Anzeige von Positionsanzeigern
 * @author Peter Koenig
 * @param <E> Der Datentyp der Positionsanzeiger
 */
public abstract class PositionGroupGUI<E extends Position> extends ComponentGroupGUI<E> {
	
	private Set<E> positions;
	
	/**
	 * Der Konstruktor
	 */
	PositionGroupGUI() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.positions = new TreeSet<E>(new Comparator<E>() {
			
			public int compare(E pos1, E pos2) {
				
				if(pos1.getId() < pos2.getId())
					return -1;
				if(pos1.getId() > pos2.getId())
					return 1;
				return 0;
			}
		});
	}
	
	/**
	 * @return Alle Positionsanzeiger der Gruppe
	 */
	protected Set<E> getAllPositions() {
		
		return this.positions;
	}
	
	/**
	 * @param pos Der Positionsanzeiger, der hinzugefuegt werden soll 
	 */
	public void addPosition(E pos) {
		
		this.positions.add(pos);
	}
	
	/**
	 * Fuegt verschiedene Gruppen zusammen
	 * @param actGUI Die Gruppe, deren Komponenten hinzugefuegt werden sollen
	 */
	public void join(PositionGroupGUI<E> actGUI) {
		
		this.positions.addAll(actGUI.getAllPositions());
	}
}


