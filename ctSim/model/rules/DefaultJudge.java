package ctSim.model.rules;

import ctSim.controller.DefaultController;
import ctSim.view.gui.Debug;

/**  Das ist der "Ich-mache-nix"-Judge fuer den "Normal-Betrieb" mit 
 * einem einzelnen Bot
 */
public class DefaultJudge extends Judge {
	
	private DefaultController controller;
	
	/**
	 * Der Konstruktor
	 * @param ctrl Der DefaultController
	 */
	public DefaultJudge(DefaultController ctrl) {
		super(ctrl);
		this.controller = ctrl;
	}
	
	@Override
	public boolean isAddAllowed() {
		return true;
	}
	
	@Override
	public boolean isStartAllowed() {
		if (controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte.");
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isModifyingAllowed() {
		return true;
	}
	
	@Override
	protected boolean check() {
		return true;
	}
}
