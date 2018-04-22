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

import javax.swing.UIManager;

import ctSim.util.FmtLogger;

/** 
 * Initialisierungen für ct-Sim
 */
public class Init {
	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger("ctSim.controller.Init");
	
	/**
	 * Setzt das Design auf Java-System oder Metal für Linux.
	 */
	public static void setLookAndFeel() {
		// Ubuntu 6.10 + Gnome: Stelle fest, dass c't-Sim absolut bekackt 
		// aussieht mit dem Look+Feel des Systems, daher lieber gleich Metal
		// nehmen
		if (System.getProperty("os.name").equals("Linux"))
			useMetalLookAndFeel();
		else {
			try {
				UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
				lg.fine("Verwende Look and Feel des Systems");
			} catch (Exception e) {
				useMetalLookAndFeel();
			}
		}
	}
	
	/** Metal-Design einstellen. */
	private static void useMetalLookAndFeel() {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
			lg.fine("Verwende Metal als Look and Feel");
		} catch (Exception e) {
			lg.fine(e, "Problem beim Setzen des Look and Feel");
		}
	}
}
