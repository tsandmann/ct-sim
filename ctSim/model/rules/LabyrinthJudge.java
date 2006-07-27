/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
 * 
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version. 
 * This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 * 
 */
package ctSim.model.rules;

import java.util.Set;

import javax.vecmath.Vector3d;

import ctSim.controller.Controller;
import ctSim.model.AliveObstacle;
import ctSim.model.World;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.view.Debug;
import ctSim.view.sensors.RemoteControlGroupGUI;
import ctSim.SimUtils;

/**
 * Schiedsrichter fuer Rennen von zwei Bots durch ein Labyrinth
 *
 */
public class LabyrinthJudge extends Judge {
	
	private Controller controller;
	
	private World world;
	
	private int participants = 2;

	/** Variable umd den ersten Start zu markieren */
	private boolean first = true;
	
	
	/**
	 * Der Konstruktor
	 * @param ctrl Der Controller
	 * @param w Die Welt
	 */
	public LabyrinthJudge(Controller ctrl) {
		
		super(ctrl);
		
		this.controller = ctrl;
	}
	
	public void setWorld(World world) {
		
		this.world = world;
		super.setWorld(world);
	}
	
	/** 
	 * @see ctSim.model.rules.Judge#isAddAllowed()
	 */
	@Override
	public boolean isAddAllowed() {
		
		// TODO: Bot-Anzahl pruefen
		if(this.controller.getParticipants() >= this.participants) {
			Debug.out.println("Fehler: Es sind schon "+this.participants+" Bots auf der Karte."); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		
		return super.isAddAllowed();
	}
	
	/**
	 * @see ctSim.model.rules.Judge#isStartAllowed()
	 */
	@Override
	public boolean isStartAllowed() {
		
		// TODO: Bot-Anzahl pruefen
		if(this.controller.getParticipants() < this.participants) {
			Debug.out.println("Fehler: Noch nicht genuegend Bots auf der Karte."); //$NON-NLS-1$
			return false;
		}
		
		if(this.controller.getParticipants() > this.participants) {
			Debug.out.println("Fehler: Es sind zu viele Bots auf der Karte."); //$NON-NLS-1$
			return false;
		}
		
		return super.isStartAllowed();
	}
	
	/**
	 * Prueft die Einhaltung der Regeln
	 * @return true, wenn aller Reglen eingehalten werden
	 */
	@Override
	public boolean check(){
		
		if(this.world == null)
			return false;
		
		Set<AliveObstacle> obsts = this.world.getAliveObstacles();
		
		if (obsts == null || obsts.isEmpty())
			return true;
		
		for(AliveObstacle obst : obsts) {
			
			if (first ==true){
				if (obst instanceof CtBotSimTcp)
					((CtBotSimTcp)obst).sendRCCommand(RemoteControlGroupGUI.RC5_CODE_5);
			}
			
			if(this.world.finishReached(new Vector3d(obst.getPosition()))) {
				
				//Debug.out.println("Bot \""+obst.getName()+"\" erreicht nach "+this.getTime()+" ms als erster das Ziel!");
				//Debug.out.println("Zieleinlauf \""+obst.getName()+"\" nach "+ this.getTime()+" ms.");
				Debug.out.println("Zieleinlauf \""+obst.getName()+"\" nach "+ SimUtils.millis2time(this.getTime()));  //$NON-NLS-1$//$NON-NLS-2$
				
				return false;
			}
		}
		first=false;
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
		
	}
	
	public void reinit() {
		
	}
}
