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
package ctSim.Model.Rules;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import ctSim.Model.Bots.Bot;
import ctSim.Model.World;
import ctSim.Model.Bots.CtBot;

/**
 * Prueft, ob die Regeln eingehalten werden
 * @author bbe (bbe@heise.de)
 *
 */
public class LabyrinthJudge extends Judge{
	/**
	 * Rennen gestartet?
	 */
	boolean raceStartet = false;
	
	/** Anzahl der Teilnehmer am Rennen */
	int participants = 2;
	
	/** Konstruktor */
	public LabyrinthJudge(){
		super();
	}

	/**
	 * Prueft die Einhaltung der Regeln
	 *
	 */
	public void check(){
		World world =getController().getWorld(); 
		if (world== null)
			return;

		List bots = world.getAliveObstacles();
		
		if (bots== null)
			return;
		
		Bot finishCrossed = null;
		
		if (raceStartet==false) { 
			if (getActiveParticipants() == participants){
				suspendWorld(false);	
				takeStartTime();
				raceStartet= true;
				Iterator it = bots.iterator();
				while (it.hasNext()) {
					Bot bot = (Bot) it.next();
					((CtBot)bot).setSensRc5(CtBot.RC5_CODE_5);
				}
			}
		} else {	// Wir sind im Rennen
			
			Iterator it = bots.iterator();
			while (it.hasNext()) {
				Object obj = it.next();
				if (obj instanceof Bot){
					Bot bot = (Bot) obj; 
					// Puefen, ob Ziel erreicht
					
					if (world.finishReached(bot.getPos())){
						finishCrossed= bot;
						break;
					}
				}
			}

			if (finishCrossed != null){
				kissArrivingBot(finishCrossed,getRunTime());
				finishCrossed=null;
			}
		}
	}

	/**
	 * Diese Routine wird aufgerufen, wenn ein Bot ueber die Ziellinie kommt
	 * @param bot Der Bot, der gerade ankommt
	 * @param time Die Zeit, die der Bot gebraucht hat
	 */
	protected void kissArrivingBot(Bot bot, long time){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		String timeString= (cal.get(Calendar.HOUR)-1)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+":"+cal.get(Calendar.MILLISECOND);

		getPanel().addResult("Zieleinlauf "+bot.getName()+" nach "+timeString);
		bot.die();
	}
	
	@Override
	protected void init(){
		suspendWorld(true);
	}
	
	/**
	 * Hier geschieht die eigentliche Arbeit des judges
	 */
	@Override
	protected void work() {
		// TODO Auto-generated method stub
		check();
	}

	/**
	 * liefert die Anzahl der Teilnehmer am Rennen
	 * @return Teilnehmer
	 */
	public int getParticipants() {
		return participants;
	}

	/**
	 * Setze die Anzahl der Teilnehmer am Rennen
	 * @param participants Teilnehmer
	 */
	public void setParticipants(int participants) {
		this.participants = participants;
	}

	/**
	 * Liefert die Anzahl der im Rennen befindlichen Bots zurueck
	 * @return Anzahl der im Rennen befindlichen Bots
	 */
	public int getActiveParticipants(){
		int count=0;
		List bots =getController().getWorld().getAliveObstacles();
		
		if (bots== null) 
			return 0;
		
		Iterator it = bots.iterator();
		while (it.hasNext()){
			Object obst = it.next();
			if (obst instanceof Bot)
				count++;
		}
		return count;
	}

	/**
	 * @return Gibt eine Referenz auf raceStartet zurueck
	 */
	public boolean isRaceStartet() {
		return raceStartet;
	}

	/**
	 * @param raceStartet Referenz auf raceStartet, die gesetzt werden soll
	 */
	public void setRaceStartet(boolean raceStartet) {
		this.raceStartet = raceStartet;
	}


}
