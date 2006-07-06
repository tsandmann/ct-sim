/**
 * 
 */
package ctSim.model.bots.components;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.view.positions.PositionGroupGUI;


/**
 * @author p-king
 *
 */
public abstract class Position<E> extends BotComponent {

	/**
//	 * @param name
//	 * @param relativePosition
//	 * @param relativeHeading
//	 */
	public Position(String name, Point3d relativePosition, Vector3d relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	public Position() {
		super("keiner", new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d));
		// TODO Auto-generated constructor stub
	}
	
	public abstract PositionGroupGUI getPositionGroupGUI();
//	public PositionGroupGUI getSensorGroupGUI() {
//		
//		PositionGroupGUI gui = Position.getGuiFor(this);
//		gui.addPosition(this);
//		return gui;
//	}
	
	public abstract E getValue();
	
	public abstract void setValue(E value);

	
	
}
