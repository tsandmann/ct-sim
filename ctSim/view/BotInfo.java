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

import java.util.List;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.BotPosition;
import ctSim.model.bots.components.Sensor;

/**
 * Buendelt Informationen zum Bot
 * 
 * @author Felix Beckwermert
 */
public final class BotInfo {
	
	private String name;
	private String type;
	
	// TODO: auch view (?)
	private Bot      bot;
	private BotPanel botPanel;
	
	
	
	/**
	 * Der Konstruktor
	 * @param name Name des Bot
	 * @param type Typ des Bot
	 * @param bot Der Bot selbst
	 * @param panel Die passende Anzeigetafel fuer den Bot
	 */
	public BotInfo(String name, String type, Bot bot, BotPanel panel) {
		
		// TODO: Bot adden!
		this.name     = name;
		this.type     = type;
		this.bot      = bot;
		this.botPanel = panel;
	}
	
	/**
	 * @return Name des Bot
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @param name Der zu setzende Bot-Name
	 */
	protected void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return der Bot
	 */
	public Bot getBot() {
		return this.bot;
	}
	
	/**
	 * @return die Steuertafel des Bot
	 */
	public BotPanel getBotPanel() {
		return this.botPanel;
	}
	
//	protected void setBotPanel(BotPanel panel) {
//		this.botPanel = panel;
//	}
	
	/**
	 * @return der Typ des Bot
	 */
	public String getType() {
		return this.type;
	}
	
	/**
	 * @return die Liste aller Aktuatoren des Bot
	 */
	public List<Actuator> getActuators() {
		
		return this.bot.getActuators();
	}

//	/**
//	 * @return die Liste aller Positionsanzeiger des Bot
//	 */
//	public List<Position> getPositions() {
//		
//		return this.bot.getPositions();
//	}
	
	/**
	 * @return die Liste aller Sensoren des Bot
	 */
	public List<Sensor> getSensors() {
		
		return this.bot.getSensors();
	}
	
	/**
	 * @return Das Positions-Objekt des Bot
	 */
	public BotPosition getBotPosition() {
		
		return this.bot.getBotPosition();
	}
	
//	public List<BotComponent> getBotComponents() {
//		
//		return this.bot.getComponents();
//	}
}
