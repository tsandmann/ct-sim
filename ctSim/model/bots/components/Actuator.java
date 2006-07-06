package ctSim.model.bots.components;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.view.actuators.ActuatorGroupGUI;

public abstract class Actuator<E> extends BotComponent {
	
	public Actuator(String name, Point3d relativePosition, Vector3d relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	public abstract ActuatorGroupGUI getActuatorGroupGUI();
//	public ActuatorGroupGUI getActuatorGroupGUI() {
//		
//		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
//		gui.addActuator(this);
//		return gui;
//	}
	
	protected abstract void setValue(E value);
	
	public abstract E getValue();
}
