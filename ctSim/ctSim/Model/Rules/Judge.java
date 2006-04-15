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

import java.util.Iterator;
import java.util.List;

import ctSim.Controller.Controller;
import ctSim.Model.Bot;
import ctSim.Model.World;

/**
 * Prueft, ob die Regeln eingehalten werden
 * @author bbe (bbe@heise.de)
 *
 */
public class Judge {
	Controller controller;
	
	/**
	 * Zeit zu Beginn des Wettkampfes [ms]
	 */
	long startTime;
	
	
	/**
	 * 
	 */
	public Judge(Controller controller) {
		super();
		this.controller= controller;
		// TODO Auto-generated constructor stub
	}

	/**
	 * Nimmt die Startzeit
	 */
	public void start(){
		World world =controller.getWorld(); 
		startTime = world.getSimulTimeUnblocking();
	}
	
	/**
	 * Prueft die Einhaltung der Regeln
	 *
	 */
	public void check(){
		World world =controller.getWorld(); 
		List bots = world.getBots();
		
		Iterator it = bots.iterator();
		while (it.hasNext()) {
			Bot bot = (Bot) it.next();

			// Puefen, ob Ziel erreicht
			if (world.finishReached(bot.getPos())){
					long finishTime= world.getSimulTimeUnblocking();
					long runTime= finishTime-startTime;
					System.out.println("Bot: "+bot.getBotName()+" hat das Ziel nach: "+runTime+" ms erreicht");
			}
		}
	}
}
