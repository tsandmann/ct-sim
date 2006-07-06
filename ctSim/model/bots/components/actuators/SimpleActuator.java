package ctSim.model.bots.components.actuators;

import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;
import ctSim.view.actuators.SimpleActuatorGroupGUI;


public abstract class SimpleActuator<E extends Number> extends Actuator<E> {
	
	//private SimpleActuatorGroupGUI groupGUI = new SimpleActuatorGroupGUI();
	
	public SimpleActuator(String name, Point3d relPos, Vector3d relHead) {
		super(name, relPos, relHead);
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
