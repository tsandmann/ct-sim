package ctSim.view.gui;

import ctSim.model.bots.components.Actuator;

//$$ doc
public class Actuators extends TableOfSpinners {
	private static final long serialVersionUID = - 7560450995618737095L;

	//$$$ Actuator sollte ein i/f sein, hier dann einfacher

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
