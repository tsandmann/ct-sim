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

package ctSim.controller;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.LogManager;

import javax.swing.SwingUtilities;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import ctSim.Init;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.TimeLogger;
import ctSim.view.View;
import ctSim.view.ViewYAdapter;
import ctSim.view.contestConductor.ContestConductor;
import ctSim.view.gui.MainWindow;
import ctSim.view.gui.SplashWindow;

/**
 * <p>
 * Haupt-Einsprungpunkt in den ctSim. Der normale Weg, das Programm zu starten,
 * ist der Aufruf der {@link #main(String[])}-Methode dieser Klasse.
 * </p>
 * <p>
 * Inhaltlich befasst sich die Klasse mit grundlegenden
 * Initialisierungs-Geschichten.
 * </p>
 */
public class Main {
	/** Versionsnummer */
	public static final String VERSION = "2.5";
	/** Konfigurationsdatei */
	private static final String DEFAULT_CONFIGFILE = "config/ct-sim.xml";
    /** Flag, welches die Anzeige des Splashscreens bewirkt (DEFAULT: TRUE) */
    private static boolean showSplash = true;

	/** Logger */
	static FmtLogger lg;

	/** Init-Container */
	public static final InitializingPicoContainer dependencies =
		new InitializingPicoContainer();


	static {
		dependencies.registerImplementation(ContestConductor.class);
		dependencies.registerImplementation(Controller.class,
			DefaultController.class);
		dependencies.registerInstance(
			new Config.SourceFile(DEFAULT_CONFIGFILE));
	}

	/**
	 * Haupt-Einsprungpunkt in den ctSim. Das hier starten, um den ctSim zu
	 * starten.
	 *
	 * @param args Als Kommandozeilenargumente sind momentan zulässig:
	 * <ul>
	 * <li>{@code -conf pfad/zur/konfigdatei.xml}: Andere Konfigurationsdatei
	 * als die standardm&auml;&szlig;ige config/ct-sim.xml verwenden</li>
	 * </ul>
	 */
    public static void main(String... args) {
    	final String[] cmdArgs = args;
		handleCommandLineArgs(cmdArgs);
	
		if (showSplash) {
		    /* Splash-Screen anzeigen */
		    java.net.URL url = ClassLoader.getSystemResource("images/splash.jpg");
		    SplashWindow.splash(url, "Version " + VERSION);
		    SplashWindow.setMessage("Initialisierung...");
		    /* Inits ausfuehren */
		    try {
				SwingUtilities.invokeAndWait(new Runnable() {
			    	public void run() {
						lg = initLogging();
						loadConfig();
						Init.setLookAndFeel();
						setupViewAndController();
				    }
				});
		    } catch (Exception e) {
				System.err.println("Initialisierungen in Main fehlgeschlagen");
				e.printStackTrace();
				/* Programm erst nach Klick auf Splash schliessen */
				MouseAdapter disposeOnClick = new MouseAdapter() {
				    @Override
					public void mouseClicked(MouseEvent evt) {
				    	System.exit(1);
				    }
				};
				SplashWindow.getWindow().addMouseListener(disposeOnClick);
				return;
		    }
		    /* Splash-Screen weg */
		    SplashWindow.disposeSplash();
		} else {
		    /* Starten von ct-Sim ohne Splash-Screen */
		    lg = initLogging();
		    loadConfig();
		    Init.setLookAndFeel();
		    setupViewAndController();
		}
    }

	/**
	 * Siehe {@link #main(String...)}.
	 * 
	 * @param args Command-Line-Argumente
	 */
	private static void handleCommandLineArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-conf")) {
				i++;
				dependencies.reRegisterInstance(new Config.SourceFile(args[i]));
		    } else if (args[i].toLowerCase().equals("-nosplash")) {
				showSplash = false;
			} else {
				System.out.println("Ung\u00FCltiges Argument '" + args[i] + "'");
				System.out.println("USAGE: ct-Sim [-conf configfile][-nosplash]");
				System.out.println("\t-conf configfile.xml\tPfad zu alternativer Konfigurationsdatei");
				System.out.println("\t-nosplash\t\tStartet ct-Sim ohne Splash-Screen");
				System.exit(1);
			}
		}
	}

	/**
	 * Initialisiert den Logger
	 * @return	Logger-Instanz
	 */
	public static FmtLogger initLogging() {
		try {
			java.net.URL url = ClassLoader.getSystemResource("config/logging.conf");
	        LogManager.getLogManager().readConfiguration(url.openStream());
		} catch (Exception e) {
			System.err.println("Logging konnte nicht initialisiert werden");
			e.printStackTrace();
			System.exit(1);
		}
		FmtLogger rv = FmtLogger.getLogger("");
		rv.fine("Logging-Subsystem initialisiert");

		if (showSplash) {
		    rv.addHandler(SplashWindow.getLogHandler());
		}
		return rv;
    }

	/**
	 * Laedt die Konfiguration
	 */
	private static void loadConfig() {
		try {
			Config.loadConfigFile(DEFAULT_CONFIGFILE);
			return;
		} catch (FileNotFoundException e) {
			lg.severe(e, "Konfigurationsdatei '"+DEFAULT_CONFIGFILE+"' nicht gefunden");
		} catch (SAXException e) {
			lg.severe(e, "Fehler beim Parsen der Konfigurationsdatei '%s'",
				DEFAULT_CONFIGFILE);
		} catch (IOException e) {
			lg.severe(e, "E/A-Fehler beim Parsen der Konfigurationsdatei '%s'",
				DEFAULT_CONFIGFILE);
		} catch (ParserConfigurationException e) {
			lg.severe(e, "Fehler beim Parsen der Konfigurationsdatei '%s'",
				DEFAULT_CONFIGFILE);
		}
	}

	/**
	 * Initialisierung von View und Controller
	 */
	private static void setupViewAndController() {
		Controller c = dependencies.get(Controller.class);

		List<View> v = Misc.newList();
		// View der Applikation ist mindestens das MainWindow
		try {
			v.add(new MainWindow(c));
		} catch (Exception e) {
			/* Fehler abfangen, z.B. von Java3D */
			lg.warn("Fenster konnte nicht erzeugt werden (Java3D-Fehler?)");
			lg.warn(e.getMessage());
			lg.warn("ct-Sim wird beendet");
			System.exit(1);
		}
		try {
			// View um ContestConductor erweitern falls so konfiguriert
			if (Config.getValue("useContestConductor").equalsIgnoreCase("true"))
				v.add(dependencies.get(ContestConductor.class));
		} catch (Exception e) {
			lg.warn(e, "Probleme beim Instanziieren des ContestConductor; " +
					"starte ohne");
		}

		if (Config.getValue("TimeLogger").equalsIgnoreCase("true"))
			v.add(new TimeLogger());

		c.setView(ViewYAdapter.newInstance(v));
		c.onApplicationInited();
    }
}
