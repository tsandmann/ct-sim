package ctSim.model.bots.components.sensors;

import ctSim.model.bots.components.Sensor;
import ctSim.view.sensors.SensorGroupGUI;


public class Switch extends SimpleSensor<Integer> {
	
	Switch(String name, String relativePosition, double relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}


	@Override
	public String getType() {
		
		return "Schalter";
	}

	@Override
	public String getDescription() {
		// TODO:
		return null;
	}
	
	//////////////////////////////////////////////////////////////////////
	// Testfälle:
	
	// Testfall 1:
	public static Sensor getTestSensor1(String name) {
		
		Sensor sens1 = new Switch("Test:Sensor:Switch - "+name, "bla", 0.342d);
		
		return sens1;
	}


	@Override
	public Integer updateValue() {
		// TODO Auto-generated method stub
		return null;
	}
}