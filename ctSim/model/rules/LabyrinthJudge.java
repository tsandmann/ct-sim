package ctSim.model.rules;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.vecmath.Vector3d;

import ctSim.controller.Controller;
import ctSim.model.AliveObstacle;
import ctSim.model.World;
import ctSim.view.Debug;

public class LabyrinthJudge extends Judge {
	
	private Controller controller;
	
	private World world;
	
	private int participants = 2;
	
	
	public LabyrinthJudge(Controller ctrl, World world) {
		
		super(ctrl, world);
		
		this.controller = ctrl;
		this.world = world;
	}
	
	@Override
	public boolean isAddAllowed() {
		
		// TODO: Bot-Anzahl prüfen
		if(this.controller.getParticipants() == this.participants) {
			Debug.out.println("Fehler: Es sind schon "+this.participants+" Bots auf der Karte.");
			return false;
		}
		
		return super.isAddAllowed();
	}
	
	@Override
	public boolean isStartAllowed() {
		
		// TODO: Bot-Anzahl prüfen
		if(this.controller.getParticipants() != this.participants) {
			Debug.out.println("Fehler: Noch nicht genügend Bots auf der Karte.");
			return false;
		}
		
		return super.isStartAllowed();
	}
	
	/**
	 * Prueft die Einhaltung der Regeln
	 *
	 */
	@Override
	public boolean check(){
		
		if(world == null)
			return false;
		
		Set<AliveObstacle> obsts = this.world.getAliveObstacles();
		
		if (obsts == null || obsts.isEmpty())
			return true;
		
		for(AliveObstacle obst : obsts) {
			
			if(this.world.finishReached(new Vector3d(obst.getPosition()))) {
				
				Debug.out.println("Bot \""+obst.getName()+"\" erreicht nach "+this.getTime()+"\" als erster das Ziel!");
				
				return false;
			}
		}
//		
//		Bot finishCrossed = null;
//		
//		if (raceStartet==false) { 
//			if (getActiveParticipants() == participants){
//				suspendWorld(false);	
//				takeStartTime();
//				raceStartet= true;
//				Iterator it = bots.iterator();
//				while (it.hasNext()) {
//					Bot bot = (Bot) it.next();
//					((CtBot)bot).setSensRc5(CtBot.RC5_CODE_5);
//				}
//			}
//		} else {	// Wir sind im Rennen
//			
//			Iterator it = bots.iterator();
//			while (it.hasNext()) {
//				Object obj = it.next();
//				if (obj instanceof Bot){
//					Bot bot = (Bot) obj; 
//					// Puefen, ob Ziel erreicht
//					
//					if (world.finishReached(bot.getPos())){
//						finishCrossed= bot;
//						break;
//					}
//				}
//			}
//
//			if (finishCrossed != null){
//				kissArrivingBot(finishCrossed,getRunTime());
//				finishCrossed=null;
//			}
//		}
		
		return true;
	}
	
	@Override
	protected void init() {
		// TODO Auto-generated method stub
		
	}
}
