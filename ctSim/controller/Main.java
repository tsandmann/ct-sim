package ctSim.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.logging.LogManager;

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

//$$ doc Grobe Architektur beschreiben; wer verdrahtet M, V und C? -> auch Pico beschreiben
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
	public static final String VERSION = "2.2";
	private static final String DEFAULT_CONFIGFILE = "config/ct-sim.xml";

	static FmtLogger lg;

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
    	handleCommandLineArgs(args);
        lg = initLogging();
        loadConfig();
//      loadIcons();	// on demand
        Init.setLookAndFeel();
        setupViewAndController();
    }

	//TODO "Usage"-Meldung waere gut
	/** Siehe {@link #main(String...)}. */
	private static void handleCommandLineArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-conf")) {
				i++;
				dependencies.reRegisterInstance(
					new Config.SourceFile(args[i]));
			} else {
				System.out.println(
					"Ung\u00FCltiges Argument '" + args[i] + "'");
				System.exit(1);
			}
		}
	}

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
		return rv;
    }

	private static void loadConfig() {
//		Config.SourceFile configFile = dependencies.get(
//			Config.SourceFile.class);
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
		System.exit(1);
	}

// wir machen das jetzt pro Icon, damit es auch aus dem Jar funktioniert.
//	private static void loadIcons() {
//		try {
//			java.net.URL url = ClassLoader.getSystemResource("images");
//			//Config.loadIcons(new File("images")); //LODO Pfad hardcoded
//			Config.loadIcons(new File(url.getFile() + "/"));
//			return;
//		} catch (Exception e) {
//			lg.severe(e, "Fehler beim Laden der Icons");
//		}
//		System.exit(1);
//	}

	private static void setupViewAndController() {
		//$$ kann einfacher werden mit pico (Startable ifc)
		Controller c = dependencies.get(Controller.class);

		List<View> v = Misc.newList();
		// View der Applikation ist mindestens das MainWindow
		v.add(new MainWindow(c));
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

		//$$ Optimierung: Wenn nur ein Ding in v kann man sich den Umstand mit YAdapter sparen
		c.setView(ViewYAdapter.newInstance(v));
		c.onApplicationInited();
    }
}
