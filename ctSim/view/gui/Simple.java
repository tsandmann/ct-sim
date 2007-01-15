package ctSim.view.gui;

import javax.swing.SpinnerModel;

import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.SimpleActuator;
import ctSim.model.bots.components.BotComponent.SimpleSensor;
import ctSim.util.Buisitor.Buisit;

//$$ doc
public abstract class Simple {
	public static class Actuators extends TableOfSpinners {
		private static final long serialVersionUID = - 7560450995618737095L;

		//$$$ Casts sind superdoof

		@Buisit
		public void buisit(SimpleActuator a) {
			model.addBotComponent((BotComponent<? extends SpinnerModel>)a);
		}

		@Override protected String getPanelTitle() { return "Aktuatoren"; }
	}

	public static class Sensors extends TableOfSpinners {
		private static final long serialVersionUID = - 1275101280635052797L;

		@Buisit
		public void buisit(SimpleSensor s) {
			model.addBotComponent((BotComponent<? extends SpinnerModel>)s);
		}

		@Override protected String getPanelTitle() { return "Sensoren"; }
	}
}