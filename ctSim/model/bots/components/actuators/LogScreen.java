package ctSim.model.bots.components.actuators;

import java.awt.Color;
import java.awt.Font;

import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;

public class LogScreen extends Actuator<String> {
	
	private String val;
	
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

	@Override
	public void setValue(String value) {
		
		this.val = value;
	}

	@Override
	public String getValue() {
		
		return this.val;
	}

	@Override
	public Shape3D getShape() {
		
		return new Shape3D();
	}
}