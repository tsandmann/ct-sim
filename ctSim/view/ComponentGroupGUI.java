package ctSim.view;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;

import ctSim.model.bots.components.BotComponent;
import ctSim.view.sensors.SensorGroupGUI;

public abstract class ComponentGroupGUI<E extends BotComponent> extends Box {
	
//	private Set<E> components;
	
	public ComponentGroupGUI(int axis) {
		
		super(axis);
		
//		this.components = new TreeSet<E>(new Comparator<E>() {
//			
//			public int compare(E comp1, E comp2) {
//				
//				if(comp1.getId() < comp2.getId())
//					return -1;
//				if(comp1.getId() > comp2.getId())
//					return 1;
//				return 0;
//			}
//		});
	}
	
//	protected Set<E> getAllComponents() {
//		
//		return this.components;
//	}
//	
//	public void addComponent(E act) {
//		
//		this.components.add(act);
//	}
//	
//	public void join(ComponentGroupGUI<E> compGUI) {
//		
//		this.components.addAll(compGUI.getAllComponents());
//	}
	
	public boolean equals(Object o) {
		
		return (this.getClass().equals(o.getClass()));
	}
	
	public abstract int getSortId();
	public abstract void initGUI();
	public abstract void updateGUI();
}
