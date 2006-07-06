package ctSim.model.bot.components.positions;


import ctSim.model.bots.components.Position;
import ctSim.view.positions.PositionGroupGUI;
import ctSim.view.positions.Positions;


public abstract class SimplePosition<E extends Number> extends Position<E> {
	
	public SimplePosition(String name, String relativePosition, double relativeHeading) {
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
