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

package ctSim.view.gui;
import javax.swing.Box;

import ctSim.model.bots.components.BotComponent;

/**
 * Abstrakte Oberklasse fuer GUI von BotComponent-Gruppen
 * @author Felix Beckwermert
 * @param <E> Der Typ der Bot-Komponenten in der Gruppe
 */
public abstract class ComponentGroupGUI<E extends BotComponent> extends Box {
	/**
	 * @param axis bestimmt, an welcher Achse die Gruppe ausgerichtet werden
	 * soll
	 */
	public ComponentGroupGUI(int axis) {
		super(axis);
	}

	@Override
	public boolean equals(Object o) {
		return (this.getClass().equals(o.getClass())); //$$ eigentlich falsch
	}

	/**
	 * @return Die Sortierungs-ID (kleine Zahlen werden im Fenster oben
	 * angezeigt, grosse weiter unten)
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
