/*
 * c't-Sim - Robotersimulator für den c't-Bot
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
package ctSim.Model;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;

/**
 * Die Oberklasse fuer Repraesentationen aller Bots, die ausserhalb der Grenzen
 * des Simulators existieren und mit diesem ueber TCP kommunizieren.
 */
public class CtBotRealTcp extends CtBotReal {

	private TcpConnection tcpCon;

	/**
	 * Erzeugt einen neuen Bot
	 * 
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 * @param tc
	 *            Kommunikationsverbindung
	 */
	public CtBotRealTcp(Point3f pos, Vector3f head, TcpConnection tc) {
		super(pos, head);
		tcpCon = tc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#work()
	 */
	public void work() {
		// TODO noch zu implementieren
		ErrorHandler.error("BotRealTcp.run is missing");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#init()
	 */
	protected void init() {
		// TODO noch zu implementieren

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#cleanup()
	 */
	protected void cleanup() {
		try {
			if (tcpCon != null)
				tcpCon.disconnect();
		} catch (Exception ex) {
		}
	}
}
