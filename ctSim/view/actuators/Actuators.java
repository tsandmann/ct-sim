package ctSim.view.actuators;

import ctSim.model.bots.components.actuators.Display;
import ctSim.model.bots.components.actuators.Indicator;
import ctSim.model.bots.components.actuators.LogScreen;
import ctSim.model.bots.components.actuators.SimpleActuator;

public class Actuators {
	
	public static ActuatorGroupGUI getGuiFor(SimpleActuator a) {
		
		//System.out.println("Simple");
		return new SimpleActuatorGroupGUI();
	}
	
	public static ActuatorGroupGUI getGuiFor(Indicator a) {
		
		//System.out.println("Indi");
		return new IndicatorGroupGUI();
	}
	
//	public static ActuatorGroupGUI getGuiFor(Display a) {
//		
//		return new DisplayGUI();
//	}
	
	public static ActuatorGroupGUI getGuiFor(LogScreen a) {
		
		return new LogScreenGUI();
	}
}
