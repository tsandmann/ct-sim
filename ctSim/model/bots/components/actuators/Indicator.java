package ctSim.model.bots.components.actuators;

import java.awt.Color;

import ctSim.model.bots.components.Actuator;
import ctSim.view.actuators.ActuatorGroupGUI;
import ctSim.view.actuators.Actuators;
import ctSim.view.actuators.IndicatorGroupGUI;

// TODO: Should be off Enumeration-Type
public abstract class Indicator extends Actuator<Boolean> {
	
	private Boolean val;
	
	private Color off;
	private Color on;
	
	public Indicator(String name, String relativePosition, double relativeHeading, Color off, Color on) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
		
		this.off = off;
		this.on = on;
		
		this.val = false;
	}

//	@Override
//	protected void setValue(Boolean value) {
//		
//		this.val = value;
//	}
//
//	@Override
//	public Boolean getValue() {
//		
//		return this.val;
//	}

	@Override
	public String getType() {
		
		return "Indicator";
	}

	@Override
	public String getDescription() {
		// TODO:
		return null;
	}
	
	@Override
	public ActuatorGroupGUI getActuatorGroupGUI() {
		
		ActuatorGroupGUI gui = Actuators.getGuiFor(this);
		gui.addActuator(this);
		return gui;
	}
	
	public Color getColor() {
		
		if(this.val)
			return this.on;
		
		return this.off;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Testfälle:
	
	// Testfall 1:
//	public static Actuator getTestActuator1(String name) {
//		
//		Actuator act1 = new Indicator("Test:Actuator:Indicator - "+name,
//				"blubb", 0.342d,
//				colLed1, colLed1Akt);
//		
//		return act1;
//	}
//	
//	public static Actuator getTestActuator2(String name) {
//		
//		Actuator act1 = new Indicator("Test:Actuator:Indicator - "+name,
//				"blubb", 0.342d,
//				colLed3, colLed3Akt);
//		
//		return act1;
//	}
}