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
package ctSim.view.gui;

/**
 * Interface für Debug-Fenster
 * 
 * @author Felix Beckwermert
 *
 */
public interface DebugWindow {
	
	/**
	 * @param str Text, der in das Debug-Fenster geschrieben werden soll
	 */
	public void print(String str);
	
	/**
	 * @param str Textzeile, die in das Debug-Fenster geschrieben werden soll
	 */
	public void println(String str);
}
