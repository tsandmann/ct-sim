package ctSim.model.rules;

import ctSim.controller.DefaultController;
import ctSim.view.gui.Debug;

/**  Das ist der "Ich-mache-nix"-Judge fuer den "Normal-Betrieb" mit
 * einem einzelnen Bot
 */
public class DefaultJudge extends Judge {
	/** Controller */
	private DefaultController controller;

	/**
	 * Der Konstruktor
	 * @param ctrl Der DefaultController
	 */
	public DefaultJudge(DefaultController ctrl) {
		super(ctrl);
		this.controller = ctrl;
	}

	/**
	 * @see ctSim.model.rules.Judge#isAddingBotsAllowed()
	 */
	@Override
	public boolean isAddingBotsAllowed() {
		return true;
	}

	/**
	 * @see ctSim.model.rules.Judge#isStartingSimulationAllowed()
	 */
	@Override
	public boolean isStartingSimulationAllowed() {
		if (controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte.");
			return false;
		}
		return true;
	}

	/**
	 * @see ctSim.model.rules.Judge#isSimulationFinished()
	 */
	@Override
	protected boolean isSimulationFinished() {
		return false;
	}
}
