/*
 * c't-Sim - Robotersimulator für den c't-Bot
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

import javax.swing.JPanel;

import ctSim.model.bots.Bot;
import ctSim.model.bots.BotBuisitor;
import ctSim.util.Buisitor;

/** GUI der Buisitors */
public abstract class GuiBotBuisitor extends JPanel implements BotBuisitor {
	/** UID */
	private static final long serialVersionUID = 1654996309645415223L;
	/** Buisitor */
	private final Buisitor buisitor = new Buisitor(this);
	/** Anzeige? */
	private boolean shouldBeDisplayed = false;

	/** Neuer GUI-Buisitor */
	public GuiBotBuisitor() {
		super(true);	// Double-Buffering an
	}

	/**
	 * @param o		Objekt
	 * @param bot	Bot
	 */
	@Override
	public void visit(Object o, Bot bot) {
		if (buisitor.dispatchBuisit(o) > 0)
			shouldBeDisplayed = true;
		if (buisitor.dispatchBuisit(o, bot) > 0)
			shouldBeDisplayed = true;
	}

	/**
	 * @return shouldBeDisplayed
	 */
	public boolean shouldBeDisplayed() {
		return shouldBeDisplayed;
	}
}
