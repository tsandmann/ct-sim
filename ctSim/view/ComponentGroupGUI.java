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

package ctSim.view;
import javax.swing.Box;

import ctSim.model.bots.components.BotComponent;

/**
 * Abstrakte Oberklasse fuer GUI von BotComponent-Gruppen
 * @author Felix Beckwermert
 * @param <E> Der Typ der Bot-Komponenten in der Gruppe
 */
public abstract class ComponentGroupGUI<E extends BotComponent> extends Box {
	
//	private Set<E> components;
	
	/**
	 * Der Konstruktor
	 * @param axis bestimmt, an welcher Achse die Gruppe ausgerichtet werden soll
	 */
	public ComponentGroupGUI(int axis) {
		
		super(axis);
		
//		this.components = new TreeSet<E>(new Comparator<E>() {
//			
//			public int compare(E comp1, E comp2) {
//				
//				if(comp1.getId() < comp2.getId())
//					return -1;
//				if(comp1.getId() > comp2.getId())
//					return 1;
//				return 0;
//			}
//		});
	}
	
//	protected Set<E> getAllComponents() {
//		
//		return this.components;
//	}
//	
//	public void addComponent(E act) {
//		
//		this.components.add(act);
//	}
//	
//	public void join(ComponentGroupGUI<E> compGUI) {
//		
//		this.components.addAll(compGUI.getAllComponents());
//	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		
		return (this.getClass().equals(o.getClass()));
	}
	
	/**
	 * @return Die Sortierungs-ID (kleine Zahlen werden im Fenster oben angezeigt, 
	 * grosse weiter unten)
	 */
	public abstract int getSortId();
	/**
	 * Initialisiert die GUI
	 */
	public abstract void initGUI();
	/**
	 * Aktualisiert die GUI
	 */
	public abstract void updateGUI();
}
