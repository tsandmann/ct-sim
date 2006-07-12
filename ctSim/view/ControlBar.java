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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Der Rahmen, der die Steuertafeln der einzelnen Bots enthaelt
 * @author Felix Beckwermert
 *
 */
public final class ControlBar extends JPanel {
	
	private final Dimension PREF_SIZE = new Dimension(180, 600);
	
	private List<BotInfo> botList;
	
	private JTabbedPane botTabs;
	
	/**
	 * Der Konstruktor
	 */
	ControlBar() {
		
		super();
		
		this.botList = new ArrayList<BotInfo>();
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		//this.botList = new ArrayList<BotInfo>();
		
		//this.botTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		this.botTabs = new JTabbedPane();
		
		//this.add(Box.createVerticalGlue());
		this.add(botTabs);
		
		//TODO: ???
		//this.setPreferredSize(new Dimension(180, 500));
		//this.setMinimumSize(new Dimension(100, 150));
		//this.setPreferredSize(this.PREF_SIZE);
	}
	
	/**
	 * Fuegt die Kontrolltafel fuer einen Bot hinzu
	 * @param botInfo Die Informationen zum Bot
	 */
	protected void addBot(BotInfo botInfo) {
		
		// TODO: Erst Panel oder erst Info???
		if(botInfo.getBotPanel() == null)
			return;
		
		//botInfo.getBotPanel().setBotInfo(botInfo);
		BotPanel botPanel = botInfo.getBotPanel();
		botPanel.init(botInfo);
		
		this.botList.add(botInfo);
		
//		JScrollPane scroll = new JScrollPane(botPanel);
//		//, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//		
//		this.botTabs.addTab(botInfo.getName(), null, scroll,
		this.botTabs.addTab(botInfo.getName(), null, botPanel,
				"Bot '"+botInfo.getName()+"' with type '"+botInfo.getType()+"'");
		
		botPanel.invalidate();
		//this.invalidate();
	}
	
	@Override
	public Dimension getPreferredSize() {
		
		if(this.botList.isEmpty())
			return new Dimension(0,0);
		return this.PREF_SIZE;
	}
	
	/**
	 * Aktualisiert alle Bot-Steuertafeln
	 */
	public void update() {
		
		for(BotInfo bi : this.botList) {
			bi.getBotPanel().update();
		}
	}
	
	/**
	 * Loescht alle Bot-Steuertafeln
	 */
	protected void reinit() {
		
		this.botList = new ArrayList<BotInfo>();
		this.botTabs.removeAll();
	}
}