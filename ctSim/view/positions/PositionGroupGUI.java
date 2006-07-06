package ctSim.view.positions;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;

import ctSim.model.bots.components.Position;
import ctSim.view.ComponentGroupGUI;



public abstract class PositionGroupGUI<E extends Position> extends ComponentGroupGUI<E> {
	
	private Set<E> positions;
	
	PositionGroupGUI() {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.positions = new TreeSet<E>(new Comparator<E>() {
			
			public int compare(E pos1, E pos2) {
				
				if(pos1.getId() < pos2.getId())
					return -1;
				if(pos1.getId() > pos2.getId())
					return 1;
				return 0;
			}
		});
	}
	
	protected Set<E> getAllPositions() {
		
		return this.positions;
	}
	
	public void addPosition(E act) {
		
		this.positions.add(act);
	}
	
	public void join(PositionGroupGUI<E> actGUI) {
		
		this.positions.addAll(actGUI.getAllPositions());
	}
}


