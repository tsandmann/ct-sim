package ctSim.model.bots.components.actuators;

import java.util.Set;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;
import ctSim.view.actuators.SimpleActuatorGroupGUI;


public abstract class SimpleActuator<E extends Number> extends Actuator<E> {
	
	//private SimpleActuatorGroupGUI groupGUI = new SimpleActuatorGroupGUI();
	
	public SimpleActuator(String name, String relativePosition, double relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	/* Hacky: The groupGUI-Member must not be the GUI-Comp. showing
	 *        this Actuator (see 'join' in 'ActuatorGroupGUI'
	 *        and 'initGUI' in 'DefBotPanel')...
	 */
//	public ActuatorGroupGUI getActuatorGroupGUI() {
//		
//		this.groupGUI.addActuator(this);
//		return groupGUI;
//	}
	
	public ActuatorGroupGUI getActuatorGroupGUI() {
		
		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
		gui.addActuator(this);
		return gui;
	}
}
