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
package ctSim;

import java.io.InputStream;

/**
 * Fuehrt ein externes Programm aus
 * @author bbe (bbe@heise.de)
 *
 */
public class CmdExec {

	/**
	 * Fuehrt ein externes Programm aus
	 * @param cmdline
	 * @return Den Ausgabestrom des Programms
	 * @throws Exception Es kann ja soviel schiefgehen
	 */
	public InputStream exec(String cmdline) throws Exception {
		try {
			Process p = Runtime.getRuntime().exec(cmdline);

			return p.getInputStream();
		} catch (Exception err) {
			ErrorHandler.error("Probleme beim ausfuehren von: "+cmdline+" :"+err);
			throw err;
		}
	}
}