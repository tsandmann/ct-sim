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
package ctSim.view.actuators;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ctSim.model.bots.components.actuators.Indicator;

/**
 * Klasse fuer grafische Anzeige von Indikatorgruppen
 * @author Felix Beckwermert
 */
public class IndicatorGroupGUI extends ActuatorGroupGUI<Indicator> {
	
	private ArrayList<JPanel> leds;
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#initGUI()
	 * 	@Override
	 */
	public void initGUI() {
		
		//System.out.println("Indicator: "+this.getAllActuators().size());
		this.setBorder(new TitledBorder(new EtchedBorder(), "Indikatoren"));
		
		Set<Indicator> acts = this.getAllActuators();
		
		this.leds = new ArrayList<JPanel>(acts.size());
		
		JPanel ledPanel = new JPanel(new GridLayout(1, acts.size(), 5, 5));
		
		Iterator<Indicator> it = acts.iterator();
		while(it.hasNext()) {
			JPanel led = new JPanel();
			// TODO: siehe Indicator
			led.setBackground(it.next().getColor());
			led.setMinimumSize(new Dimension(15, 15));
			//led.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			led.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.leds.add(led);
			ledPanel.add(led);
		}
		
		this.add(ledPanel);
	}
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#updateGUI()
	 * @Override
	 */	
	public void updateGUI() {
		// TODO
		//System.out.println("Indicator: "+this.getAllActuators().size());
		Iterator<Indicator> it = this.getAllActuators().iterator();
		for(int i=0; it.hasNext(); i++) {
			// TODO: siehe Indicator: getColor
			this.leds.get(i).setBackground(it.next().getColor());
		}
	}

	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#getSortId()
	 * 	@Override
	 */
	public int getSortId() {
		return 1;
	}
}