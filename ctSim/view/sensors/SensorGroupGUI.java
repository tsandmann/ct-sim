package ctSim.view.sensors;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;

import ctSim.model.bots.components.Sensor;
import ctSim.view.ComponentGroupGUI;

public abstract class SensorGroupGUI<E extends Sensor> extends ComponentGroupGUI<E> {
	
	private Set<E> sensors;
	
	SensorGroupGUI() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.sensors = new TreeSet<E>(new Comparator<E>() {
			
			public int compare(E sens1, E sens2) {
				
				if(sens1.getId() < sens2.getId())
					return -1;
				if(sens1.getId() > sens2.getId())
					return 1;
				return 0;
			}
		});
	}
	
	protected Set<E> getAllSensors() {
		
		return this.sensors;
	}
	
	public void addSensor(E act) {
		
		this.sensors.add(act);
	}
	
	public void join(SensorGroupGUI<E> actGUI) {
		
		this.sensors.addAll(actGUI.getAllSensors());
	}
}
