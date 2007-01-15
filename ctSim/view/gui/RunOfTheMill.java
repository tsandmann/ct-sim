package ctSim.view.gui;

import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.ctbot.components.DistanceSensor;
import ctSim.model.bots.ctbot.components.EncoderSensor;

//$$ doc
public abstract class RunOfTheMill {
	
	public static class Actuators extends TableOfSpinners {
		private static final long serialVersionUID = - 7560450995618737095L;

		//$$$ ? sollten 2 i/fs sein, hier dann einfacher

		@Buisit
		public void buisit(Actuator.Governor a) {
			model.addBotComponent(a);
		}

		@Buisit
		public void buisit(Actuator.DoorServo a) {
			model.addBotComponent(a);
		}

		@Override protected String getPanelTitle() { return "Aktuatoren"; }
	}

	public static class Sensors extends TableOfSpinners {
		private static final long serialVersionUID = - 1275101280635052797L;

		@Buisit
		public void buisit(EncoderSensor s) {
			model.addBotComponent(s);
		}
		
		@Buisit
		public void buisit(DistanceSensor s) {
			model.addBotComponent(s);
		}

		@Override protected String getPanelTitle() { return "Sensoren"; }
	}
}