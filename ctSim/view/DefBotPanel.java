package ctSim.view;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;

import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Position;
import ctSim.model.bots.components.Sensor;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.positions.PositionGroupGUI;
import ctSim.view.positions.Positions;
import ctSim.view.sensors.SensorGroupGUI;
import ctSim.view.sensors.Sensors;

public class DefBotPanel extends BotPanel {
	
	private List<ComponentGroupGUI> compList;
	
	public DefBotPanel() {
		
		// Leerer Konstruktor automatisch:
		super();
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
	}
	
	// do not call this function directly!
	// call 'init' instead
	@SuppressWarnings("unchecked")
	protected void initGUI() {
		
		List<PositionGroupGUI> posList = new ArrayList<PositionGroupGUI>();		
		List<ActuatorGroupGUI> actsList = new ArrayList<ActuatorGroupGUI>();
		List<SensorGroupGUI>   sensList = new ArrayList<SensorGroupGUI>();

		for(Position p : this.getBotInfo().getPositions()) {
			
			PositionGroupGUI gGUI = p.getPositionGroupGUI();
			
			int idx = posList.indexOf(gGUI);
			
			if(idx < 0) {
				posList.add(gGUI);
			} else {
				posList.get(idx).join(gGUI);
			}
		}

		
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
		this.compList.addAll(posList);
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
	
	// do not call this function directly!
	// call 'update' instead
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
