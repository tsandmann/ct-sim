package ctSim.model.bots;

import java.util.ArrayList;
import java.util.List;

import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.components.actuators.Governor;
import ctSim.model.bots.components.actuators.Indicator;
import ctSim.model.bots.components.sensors.Switch;

public class Bot_Ref {
	
	private List<Actuator> acts;
	private List<Sensor> sens;
	
	Bot_Ref() {
		
		this.acts = new ArrayList<Actuator>();
		this.sens = new ArrayList<Sensor>();
	}
	
	public final List<Actuator> getActuators() {
		
		return this.acts;
	}
	
	public final List<Sensor> getSensors() {
		
		return this.sens;
	}
	
	protected final void addActuator(Actuator act) {
		
		this.acts.add(act);
	}
	
	protected final void addSensor(Sensor sens) {
		
		this.sens.add(sens);
	}
	
	//////////////////////////////////////////////////////////////////////
	// Testfälle:
	
	// Test-Bot 1:
	public static Bot getTestBot1() {
		
		Bot b1 = new Bot();
		
		b1.addSensor(Switch.getTestSensor1("Sens 1"));
		b1.addSensor(Switch.getTestSensor1("Sens 2"));
		b1.addActuator(Indicator.getTestActuator1("Indi 1"));
		b1.addActuator(Governor.getTestActuator1("Act 2"));
		b1.addActuator(Indicator.getTestActuator2("Indi 2"));
		b1.addActuator(Indicator.getTestActuator1("Indi 4"));
		b1.addActuator(Indicator.getTestActuator2("Indi 3"));
		b1.addActuator(Indicator.getTestActuator1("Indi 5"));
		
		return b1;
	}
	
	// Test-Bot 2:
	public static Bot getTestBot2() {
		
		Bot b2 = new Bot();
		
		b2.addSensor(Switch.getTestSensor1("Sens 3"));
		
		return b2;
	}
}