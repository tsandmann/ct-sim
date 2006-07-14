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

import javax.swing.JPanel;

/**
 * Abstrakte Oberklasse fuer Bot-Anzeigetafeln
 * @author Felix Beckwermert
 */
public abstract class BotPanel extends JPanel {
	
	private BotInfo botInfo = null;
	private boolean updated = false;
	
	// TODO: erst Panel oder erst Info???
	/**
	 * Der Konstruktor
	 */
	BotPanel() {		
		super();
	}
	
//	BotPanel(BotInfo botInfo) {
//		
//		super();
//		
//		this.botInfo = botInfo;
//		botInfo.setBotPanel(this);
//	}
	
	/**
	 * @return Gibt Informationen zum betreffenden Bot zurueck
	 */
	protected final BotInfo getBotInfo() {
		
		return this.botInfo;
	}
	
	/**
	 * @param bI Die Bot-Informationen, die zu setzen sind
	 */
	private final void setBotInfo(BotInfo bI) {
		
		if(bI.getBotPanel() != this)
			return;
		
		// Ueberfluessig: (?)
		if(!this.updated)
			this.botInfo = bI;
		
		// TODO: Sonst Error
	}
	
	/**
	 * Initialisiert die Anzeigetafel
	 * @param bI Informationen zum betreffenden Bot
	 */
	protected final void init(BotInfo bI) {
		
		this.setBotInfo(bI);		
		this.initGUI();
	}
	
	/**
	 * Aktualisiert die Anzeigetafel
	 */
	protected final void update() {
		
		if(this.botInfo == null) {
			// TODO: Error
			System.err.println("Error: BotInfo not set!"); //$NON-NLS-1$
			System.exit(-1);
		}
		
		this.updated = true;
		updateGUI();
	}
	
	/**
	 * Startet die GUI der Anzeigetafel
	 */
	protected abstract void initGUI();
	/**
	 * Aktualisiert die GUI der Anzeigetafel
	 */
	protected abstract void updateGUI();
	
	// TODO: ???
//	@Override
//	public abstract String getName();
}
