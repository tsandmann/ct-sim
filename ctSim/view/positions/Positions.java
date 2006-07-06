package ctSim.view.positions;

import ctSim.model.bot.components.positions.SimplePosition;
import ctSim.view.*;

public class Positions {
	
	public static PositionGroupGUI getGuiFor(SimplePosition pos) {
		
		return new SimplePositionGroupGUI();
	}
}
