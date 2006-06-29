package ctSim.model.bots.components.sensors;

import ctSim.model.bots.components.Sensor;
import ctSim.view.sensors.SensorGroupGUI;
import ctSim.view.sensors.Sensors;


public abstract class SimpleSensor<E extends Number> extends Sensor<E> {
	
	public SimpleSensor(String name, String relativePosition, double relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public SensorGroupGUI getSensorGroupGUI() {
		
		SensorGroupGUI gui = Sensors.getGuiFor(this);
		gui.addSensor(this);
		return gui;
	}
}
