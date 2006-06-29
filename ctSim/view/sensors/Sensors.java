package ctSim.view.sensors;

import ctSim.model.bots.components.sensors.SimpleSensor;

public class Sensors {
	
	public static SensorGroupGUI getGuiFor(SimpleSensor sens) {
		
		return new SimpleSensorGroupGUI();
	}
}
