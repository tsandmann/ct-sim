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

		List bots = world.getBots();
		
		if (bots== null)
			return;
		
		
		if (raceStartet==false) { 
			if (bots.size() == participants){
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
				Bot bot = (Bot) it.next();
				// Puefen, ob Ziel erreicht
				
				if (world.finishReached(bot.getPos())){
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(getRunTime());
					String timeString= (cal.get(Calendar.HOUR)-1)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+":"+cal.get(Calendar.MILLISECOND);
	
					getPanel().addResult("Zieleinlauf "+bot.getBotName()+" nach "+timeString);
					bot.die();
				}
			}
		}
	}

	protected void init(){
		suspendWorld(true);
	}
	
	/**
	 * Hier geschieht die eigentliche Arbeit des judges
	 */
	protected void work() {
		// TODO Auto-generated method stub
		check();
	}

	public int getParticipants() {
		return participants;
	}

	public void setParticipants(int participants) {
		this.participants = participants;
	}



}
