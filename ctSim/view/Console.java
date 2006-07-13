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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

/**
 * Eine Textkonsole in der c't-Sim-GUI
 * 
 * @author Felix Beckwermert
 *
 */
public class Console extends Box implements DebugWindow {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea console;
	
	/**
	 * Der Konstruktor
	 */
	Console() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.console = new JTextArea(3, 40);
		this.console.setEditable(false);
		this.console.setBorder(BorderFactory.createEtchedBorder());
		
		JScrollPane scroll = new JScrollPane(this.console, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//this.console.setBorder(BorderFactory.create)
		
		this.add(scroll);
		
		//this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
		this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		
	}
	
	/** (non-Javadoc)
	 * @see ctSim.view.DebugWindow#print(java.lang.String)
	 */
	public void print(String str) {
		
		this.console.append(str);
	}

	/** (non-Javadoc)
	 * @see ctSim.view.DebugWindow#println(java.lang.String)
	 */
	public void println(String str) {
		
		this.console.append(str);
		this.console.append("\n");
	}
}
