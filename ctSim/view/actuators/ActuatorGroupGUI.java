package ctSim.view.actuators;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;

import ctSim.model.bots.components.Actuator;
import ctSim.view.ComponentGroupGUI;

public abstract class ActuatorGroupGUI<E extends Actuator> extends ComponentGroupGUI<E> {
	
	private Set<E> actuators;
	
	ActuatorGroupGUI() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.actuators = new TreeSet<E>(new Comparator<E>() {
			
			public int compare(E act1, E act2) {
				
				if(act1.getId() < act2.getId())
					return -1;
				if(act1.getId() > act2.getId())
					return 1;
				return 0;
			}
		});
	}
	
	protected Set<E> getAllActuators() {
		
		return this.actuators;
	}
	
	public void addActuator(E act) {
		
		this.actuators.add(act);
	}
	
	public void join(ActuatorGroupGUI<E> actGUI) {
		
		this.actuators.addAll(actGUI.getAllActuators());
	}
}
