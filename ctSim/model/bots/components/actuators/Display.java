package ctSim.model.bots.components.actuators;

import java.awt.Color;
import java.awt.Font;

import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;

public abstract class Display extends LogScreen {
	
	public Display(String name, String relativePosition, double relativeHeading) {
		
		super(name, relativePosition, relativeHeading);
	}
	
	@Override
	public ActuatorGroupGUI getActuatorGroupGUI() {
		
		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
		gui.addActuator(this);
		return gui;
	}
	
	@Override
	public String getType() {
		
		return "Display";
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Color getBackgroundColor() {
		return new Color(120, 150, 90);
	}
	
	@Override
	public Font getFont() {
		return new Font("Monospaced", Font.BOLD, 12);
	}
	
	@Override
	public boolean hasToRewrite() {
		return true;
	}
}
