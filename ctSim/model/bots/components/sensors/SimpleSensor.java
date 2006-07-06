package ctSim.model.bots.components.sensors;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Sensor;
import ctSim.view.sensors.SensorGroupGUI;
import ctSim.view.sensors.Sensors;


public abstract class SimpleSensor<E extends Number> extends Sensor<E> {
	
	public SimpleSensor(String name, Point3d relPos, Vector3d relHead) {
		super(name, relPos, relHead);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public SensorGroupGUI getSensorGroupGUI() {
		
		SensorGroupGUI gui = Sensors.getGuiFor(this);
		gui.addSensor(this);
		return gui;
	}
}
