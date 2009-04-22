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

package ctSim.model.bots;

import java.net.ProtocolException;

import ctSim.controller.Controller;

/**
 * Interface fuer alle Bots
 */
public interface Bot {
	/**
	 * @param buisitor BotBuisitor
	 */
	public void accept(BotBuisitor buisitor);
	
	/**
	 * @return Name des Bots
	 */
	public String toString();
	
	/**
	 * @return Beschreibung des Bots
	 */
	public String getDescription();
	
	/**
	 * @return Nummer des Bots
	 */
	public int getInstanceNumber();
	
	/**
	 * View des Bots updaten
	 * @throws InterruptedException
	 */
	public void updateView() throws InterruptedException;
	
	/**
	 * Bot zerstoeren
	 */
	public void dispose();
	
	/**
	 * Fuegt einen Handler hinzu, der beim Ableben eines Bots gestartet wird
	 * @param runsWhenAObstDisposes
	 */
	public void addDisposeListener(Runnable runsWhenAObstDisposes);
	
	/**
	 * Liefert den Controller zurueck, der den Bot verwaltet
	 * @return Controller Der Controller
	 */
	public Controller getController();
	
	/**
	 * Setzt den Controller, der den Bot verwaltet
	 * @param controller Der Controller
	 * @throws ProtocolException 
	 */
	public void setController(Controller controller) throws ProtocolException;
}
