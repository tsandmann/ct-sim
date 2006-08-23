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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;

import ctSim.controller.BotManager;

/**
 * Der Rahmen, der die Steuertafeln der einzelnen Bots enthaelt
 * @author Felix Beckwermert
 *
 */
public final class ControlBar extends JPanel {
	
	private static final long serialVersionUID = 1L;

	private final Dimension PREF_SIZE = new Dimension(180, 600);
	
	private List<BotInfo> botList;
	
	private JTabbedPane botTabs;
	
	private CtSimFrame parent;
	
	/**
	 * Der Konstruktor
	 */
	ControlBar(CtSimFrame parent) {
		
		super();
		
		this.parent = parent;
		
		this.botList = new ArrayList<BotInfo>();
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		//this.botList = new ArrayList<BotInfo>();
		
		//this.botTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		//this.botTabs = new JTabbedPane();
		this.botTabs = new JTabbedPaneWithCloseIcons(this);
		
		//this.add(Box.createVerticalGlue());
		this.add(this.botTabs);
		
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
				"Bot '"+botInfo.getName()+"' with type '"+botInfo.getType()+"'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		this.botTabs.addMouseListener(new PopupListener());
		
		botPanel.invalidate();
		//this.invalidate();
	}
	
	protected void removeBot(BotInfo botInfo) {
		
		this.botList.remove(botInfo);
		if(botInfo.getBotPanel() != null)
			this.botTabs.remove(botInfo.getBotPanel());
	}
	
	// TODO: Called by 'actionPerformed(..)' in 'PopupListener'
	//       and 'mouseClicked(..)' in 'JTabbedPaneWithCloseIcons'
	public void remBot(int idx) {
		
		//this.parent.removeBot(this.botList.get(idx));
		BotManager.removeBot(this.botList.get(idx));
	}
	
	/** 
	 * @see java.awt.Component#getPreferredSize()
	 */
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
	
	class PopupListener extends MouseAdapter implements ActionListener {
		
        JPopupMenu popup;
        int idx;

        PopupListener() {
        	
        	initMenu();
        }
        
        private void initMenu() {
        	
        	popup = new JPopupMenu();
        	JMenuItem kill = new JMenuItem("Kill...");
        	kill.addActionListener(this);
        	popup.add(kill);
        }

        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
            	
            	idx = botTabs.indexAtLocation(e.getX(), e.getY());
            	
            	if(idx < 0)
            		return;
            	
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }
        
        public void actionPerformed(ActionEvent e) {
			
			System.out.println("Töte "+botList.get(idx).getName());
			
			//parent.removeBot(botList.get(idx));
			remBot(idx);
		}
    }
}