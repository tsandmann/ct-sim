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

package ctSim;

import ctSim.util.FmtLogger;

/** OS-spezifische Anpassungen für das Einlesen der Config */
public class ConfigManager {
	/** Logger */
	static FmtLogger lg = FmtLogger.getLogger("ctSim.ConfigManager");

	/**
	 * Wandelt einen Pfad mit Bot-Binary von Windows zu Linux
	 * 
	 * @param in	der Originalstring
	 * @return der "Linux-String"
	 */
	private static String botPathWin2Lin(String in){
		if (in == null)
			return null;
		String tmp = in.replace('\\','/');
		tmp= tmp.replace("exe","elf");
		return tmp.replace("Debug-W32","Debug-Linux");
	}

	/**
	 * Wandelt einen Pfad mit Bot-Binary von Linux zu Windows
	 * 
	 * @param in	der Originalstring
	 * @return der "Windows-String"
	 */
	private static String botPathLin2Win(String in){
		if (in == null)
			return null;
		String tmp = in.replace('/','\\');
		tmp= tmp.replace("elf","exe");
		return tmp.replace("Debug-Linux","Debug-W32");
	}

	/**
	 * Passt einen Pfad an das aktuelle OS an
	 * 
	 * @param in	der Originalstring
	 * @return der angepasste String
	 */
	public static String path2Os(String in){
		if (System.getProperty("os.name").indexOf("Linux") >=0)
			return botPathWin2Lin(in);
		else if (System.getProperty("os.name").indexOf("OS X") >=0)
			return botPathWin2Lin(in);
		else
			return botPathLin2Win(in);
	}
}