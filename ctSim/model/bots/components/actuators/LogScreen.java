package ctSim.model.bots.components.actuators;

import java.awt.Color;
import java.awt.Font;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;

public abstract class LogScreen extends Actuator<String> {
	
	public LogScreen(String name, Point3d relPos, Vector3d relHead) {
		
		super(name, relPos, relHead);
	}
	
	@Override
	public ActuatorGroupGUI getActuatorGroupGUI() {
		
		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
		gui.addActuator(this);
		return gui;
	}
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Log";
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Color getBackgroundColor() {
		return null;
	}
	
	public Font getFont() {
		return null;
	}
	
	public boolean hasToRewrite() {
		return false;
	}
}