package ctSim.model.bots.components;

import ctSim.view.sensors.SensorGroupGUI;
import ctSim.view.sensors.Sensors;


public abstract class Sensor<E> extends BotComponent {

	public Sensor(String name, String relativePosition, double relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	public abstract SensorGroupGUI getSensorGroupGUI();
//	public SensorGroupGUI getSensorGroupGUI() {
//		
//		SensorGroupGUI gui = Sensors.getGuiFor(this);
//		gui.addSensor(this);
//		return gui;
//	}
	
	public abstract E getValue();
	
	public abstract void setValue(E value);
}
