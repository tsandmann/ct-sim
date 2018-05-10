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

package ctSim.model.bots;

import java.net.ProtocolException;

import ctSim.controller.Controller;

/** Interface für alle Bots */
public interface Bot {
	/**
	 * @param buisitor	BotBuisitor
	 */
	public void accept(BotBuisitor buisitor);

	/**
	 * @return Name des Bots
	 */
	@Override
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
	 *
	 * @throws InterruptedException
	 */
	public void updateView() throws InterruptedException;

	/** Bot zerstören */
	public void dispose();

	/**
	 * Fügt einen Handler hinzu, der beim Ableben eines Bots gestartet wird
	 *
	 * @param runsWhenAObstDisposes
	 */
	public void addDisposeListener(Runnable runsWhenAObstDisposes);

	/**
	 * Liefert den Controller zurück, der den Bot verwaltet
	 *
	 * @return Controller
	 */
	public Controller getController();

	/**
	 * Setzt den Controller, der den Bot verwaltet
	 *
	 * @param controller	Der Controller
	 * @throws ProtocolException
	 */
	public void setController(Controller controller) throws ProtocolException;

	/**
	 * @return Hat der Bot eine Logausgabe aktiviert?
	 */
	public boolean get_feature_log();

	/**
	 * @return Hat der Bot eine Fernbedienung aktiviert?
	 */
	public boolean get_feature_rc5();

	/**
	 * @return Kann der Bot ABL-Programme empfangen?
	 */
	public boolean get_feature_abl_program();

	/**
	 * @return Kann der Bot Basic-Programme empfangen?
	 */
	public boolean get_feature_basic_program();


	/**
	 * @return Hat der Bot die Kartographie aktiviert?
	 */
	public boolean get_feature_map();

	/**
	 * @return Kann der Bot RemoteCalls empfangen?
	 */
	public boolean get_feature_remotecall();
}
