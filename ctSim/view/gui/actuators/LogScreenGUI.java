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
 */package ctSim.view.gui.actuators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ctSim.model.bots.components.actuators.LogScreen;

/**
 * Klasse fuer grafische Anzeige von LogScreens
 * @author Felix Beckwermert
 */
public class LogScreenGUI extends ActuatorGroupGUI<LogScreen> {
	private static final long serialVersionUID = - 5285809588248723389L;

	private List<JTextArea> lcds;

	@Override
	public void initGUI() {
		this.setBorder(new TitledBorder(new EtchedBorder(), "LogScreens"));

		JTabbedPane tab = new JTabbedPane();

		Set<LogScreen> acts = this.getAllActuators();

		this.lcds = new ArrayList<JTextArea>(acts.size());

		Iterator<LogScreen> it = acts.iterator();
		while(it.hasNext()) {

			LogScreen log = it.next();

			// Groesse?? Unterscheiden zw. LogScreen u. Disp.???
			JTextArea lcd = new JTextArea(4, 20);
			lcd.setBorder(new EtchedBorder());
			lcd.setEditable(false);
			if(log.getFont()!=null)
				lcd.setFont(log.getFont());
			if(log.getBackgroundColor()!=null)
				lcd.setBackground(log.getBackgroundColor());

			JScrollPane scroll = new JScrollPane(lcd); //, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			tab.addTab(log.getName(), scroll); // + Desc. (?)

			//scroll.setMinimumSize(lcd.getPreferredSize());
			scroll.setMinimumSize(lcd.getPreferredScrollableViewportSize());

			this.lcds.add(lcd);
		}

		this.add(tab);
	}

	@Override
	public void updateGUI() {
		Iterator<LogScreen> it = this.getAllActuators().iterator();
		for(int i=0; it.hasNext(); i++) {

			LogScreen log = it.next();

			if(log.hasToRewrite()) {
				this.lcds.get(i).setText(log.getValue());
			} else {
				this.lcds.get(i).append(log.getValue());
			}
		}
	}

	@Override
	public int getSortId() {
		return 100;
	}
}
