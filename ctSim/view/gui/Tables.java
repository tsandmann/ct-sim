package ctSim.view.gui;

import javax.swing.SpinnerModel;

import ctSim.model.ThreeDBot.HeadingCompnt;
import ctSim.model.ThreeDBot.PositionCompnt;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.SimpleActuator;
import ctSim.model.bots.components.BotComponent.SimpleSensor;

//$$ doc
public abstract class Tables {
	public static class Position extends TableOfSpinners {
		private static final long serialVersionUID = 4158210694642007178L;

		//$$ Reagieren Spinner auf das Mouse-Wheel? Wenn nein, ihnen beibringen
	    public void buisit(PositionCompnt c) {
			c.getExternalModel().setStepSize(0.05);
			// vorm Semikolon: Pattern fuer positive Zahlen, nachm ; negative
			model.addRow(c, "0.000;\u22120.000");
		}

		public void buisit(HeadingCompnt c) {
			c.getExternalModel().setStepSize(6);
			model.addRow(c, "0.0;\u22120.0");
		}

		@Override protected String getPanelTitle() { return "Position"; }
	}

	public static class Actuators extends TableOfSpinners {
		private static final long serialVersionUID = - 7560450995618737095L;

		@SuppressWarnings("unchecked")
		public void buisit(SimpleActuator a) {
			model.addRow((BotComponent<? extends SpinnerModel>)a);
		}

		@Override protected String getPanelTitle() { return "Aktuatoren"; }
	}

	public static class Sensors extends TableOfSpinners {
		private static final long serialVersionUID = - 1275101280635052797L;

		@SuppressWarnings("unchecked")
		public void buisit(SimpleSensor s) {
			model.addRow((BotComponent<? extends SpinnerModel>)s);
		}

		@Override protected String getPanelTitle() { return "Sensoren"; }
	}
}