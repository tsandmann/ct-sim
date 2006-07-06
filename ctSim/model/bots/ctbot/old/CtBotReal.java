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

package ctSim.model.bots.ctbot.old;


import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;


/**
 * Die abstrakte Oberklasse fuer Repraesentationen aller Bots, die ausserhalb
 * der Grenzen des Simulators existieren.
 * 
 */

abstract public class CtBotReal extends CtBot {
//	public CtBotReal(Controller controller, Point3f pos, Vector3f head) {
//		super(controller, pos, head);
//	}
	public CtBotReal(Point3f pos, Vector3f head) {
		super(pos, head);
	}	
}
