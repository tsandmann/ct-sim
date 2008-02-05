package ctSim;

import javax.swing.UIManager;

import ctSim.util.FmtLogger;

/**
 * Initialisierungen fuer ct-Sim 
 */
public class Init {
	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger("ctSim.controller.Init");
	
	/**
	 * Setzt das Design auf Java-System oder Metal fuer Linux
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
	
	/**
	 * Metal-Design einstellen
	 */
	private static void useMetalLookAndFeel() {
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
			lg.fine("Verwende Metal als Look and Feel");
		} catch (Exception e) {
			lg.fine(e, "Problem beim Setzen des Look and Feel");
		}
	}
}
