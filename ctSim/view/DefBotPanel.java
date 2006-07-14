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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BoxLayout;

import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Sensor;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.sensors.SensorGroupGUI;

/**
 * @author Felix Beckwermert
 *
 */
public class DefBotPanel extends BotPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<ComponentGroupGUI> compList;
	
	/**
	 * Der Konstruktor
	 */
	public DefBotPanel() {
		
		// Leerer Konstruktor automatisch:
		super();
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	}
	
	/**
	 * Nicht direkt aufrufen, statt dessen 'init' benutzen!
	 * @see ctSim.view.BotPanel#initGUI()
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void initGUI() {
		
//		List<PositionGroupGUI> posList = new ArrayList<PositionGroupGUI>();		
		List<ActuatorGroupGUI> actsList = new ArrayList<ActuatorGroupGUI>();
		List<SensorGroupGUI>   sensList = new ArrayList<SensorGroupGUI>();

//		for(Position p : this.getBotInfo().getPositions()) {
//			
//			PositionGroupGUI gGUI = p.getPositionGroupGUI();
//			
//			int idx = posList.indexOf(gGUI);
//			
//			if(idx < 0) {
//				posList.add(gGUI);
//			} else {
//				posList.get(idx).join(gGUI);
//			}
//		}

		
		for(Actuator a : this.getBotInfo().getActuators()) {
			
			ActuatorGroupGUI gGUI = a.getActuatorGroupGUI();
			
			int idx = actsList.indexOf(gGUI);
			
			if(idx < 0) {
				actsList.add(gGUI);
			} else {
				actsList.get(idx).join(gGUI);
			}
		}
		
		for(Sensor s : this.getBotInfo().getSensors()) {
			
			SensorGroupGUI gGUI = s.getSensorGroupGUI();
			
			int idx = sensList.indexOf(gGUI);
			
			if(idx < 0) {
				sensList.add(gGUI);
			} else {
				sensList.get(idx).join(gGUI);
			}
		}
		
		this.compList = new ArrayList<ComponentGroupGUI>();
//		this.compList.addAll(posList);
		this.compList.add(this.getBotInfo().getBotPosition().getGUI());
		this.compList.addAll(actsList);
		this.compList.addAll(sensList);
		Collections.sort(this.compList, new Comparator<ComponentGroupGUI>() {
			
			public int compare(ComponentGroupGUI gui1, ComponentGroupGUI gui2) {
				
				if(gui1.getSortId() < gui2.getSortId())
					return -1;
				if(gui1.getSortId() > gui2.getSortId())
					return 1;
				return 0;
			}
		});
		
		for(ComponentGroupGUI gui : this.compList) {
			
			gui.initGUI();
			this.add(gui);
		}
		
		//this.add(Box.createVerticalGlue());
		
		// TODO:
		//this.setPreferredSize(new Dimension(200, 400));
	}
	
	/**
	 * Nicht direkt aufrufen, stattdessen 'update' benutzen!
	 * 
	 * @see ctSim.view.BotPanel#updateGUI()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void updateGUI() {
		
		for(ComponentGroupGUI gui : this.compList) {
			
			gui.updateGUI();
		}
	}
	
//	@Override
//	public String getName() {
//		
//		// TODO
//		return "Bla";
//	}
}
