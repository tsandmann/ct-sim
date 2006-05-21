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

package ctSim.View;

import ctSim.Model.Bots.Bot;

/**
 * Abstrakte Oberklasse fuer Kontrolltafeln simulierter Bots; muss abgeleitet
 * werden, um instanziiert werden zu koennen
 * 
 * @author pek (pek@heise.de)
 * 
 */
public abstract class ControlPanel extends SimPanel {


	// Zu welchem Bot gehoert das Panel?
	private Bot bot;

	/**
	 * Erzeugt ein neues Panel
	 * 
	 * @param bot
	 *            Referenz auf den Bot, zu dem das Panel gehoert
	 */
	public ControlPanel(Bot bot) {
		super();
		this.bot = bot;
		initGUI();

	}


	/** liefert den zugehoerigen Bot zurueck 
	 * @return der Bot
	 */
	public Bot getBot() {
		return bot;
	}

	/**
	 * @param bot
	 *            Referenz auf den Bot, die gesetzt werden soll
	 */
	public void setBot(Bot bot) {
		this.bot = bot;
	}
}
