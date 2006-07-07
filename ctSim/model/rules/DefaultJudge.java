package ctSim.model.rules;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.view.Debug;

/* Das ist der "Ich-mache-nix"-Judge für den "Normal-Betrieb"...
 * 
 */
public class DefaultJudge extends Judge {
	
	private Controller controller;
	
	public DefaultJudge(Controller ctrl, World world) {
		
		super(ctrl, world);
		
		this.controller = ctrl;
	}
	
	public boolean isAddAllowed() {
		
		return true;
	}
	
	public boolean isStartAllowed() {
		
		if(this.controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte.");
			return false;
		}
		
		return true;
	}
	
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
		// TODO Auto-generated method stub
		
	}
}
