package ctSim.model.rules;

import ctSim.controller.Controller;
import ctSim.view.Debug;

/**  Das ist der "Ich-mache-nix"-Judge fuer den "Normal-Betrieb" mit einem einzelnen Bot
 * 
 */
public class DefaultJudge extends Judge {
	
	private Controller controller;
	
	/**
	 * Der Konstruktor
	 * @param ctrl Der Controller
	 * @param world Die Welt
	 */
	public DefaultJudge(Controller ctrl) {
		
		super(ctrl);
		
		this.controller = ctrl;
	}
	
	/** 
	 * @see ctSim.model.rules.Judge#isAddAllowed()
	 */
	@Override
	public boolean isAddAllowed() {
		
		return true;
	}
	
	/**
	 * @see ctSim.model.rules.Judge#isStartAllowed()
	 */
	@Override
	public boolean isStartAllowed() {
		
		if(this.controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte."); //$NON-NLS-1$
			return false;
		}
		
		return true;
	}
	
	/**
	 * @see ctSim.model.rules.Judge#isModifyingAllowed()
	 */
	@Override
	public boolean isModifyingAllowed() {
		
		return true;
	}
	
	@Override
	protected boolean check() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	protected void init() {
		
	}
	
	public void reinit() {
		
		super.reinit();
	}
}
