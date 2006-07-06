package ctSim.model.bot.components.positions;


import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Position;
import ctSim.view.positions.PositionGroupGUI;
import ctSim.view.positions.Positions;


public abstract class SimplePosition<E extends Number> extends Position<E> {
	
	public SimplePosition(String name, Point3d relativePosition, Vector3d relativeHeading) {
		super(name, relativePosition, relativeHeading);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public PositionGroupGUI getPositionGroupGUI() {
		
		PositionGroupGUI gui = Positions.getGuiFor(this);
		gui.addPosition(this);
		return gui;
	}
}
