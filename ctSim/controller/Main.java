package ctSim.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.LogManager;

import javax.swing.UIManager;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import ctSim.util.FmtLogger;
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
	public static final String VERSION = "2.1b";
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
     * @param args Siehe {@link #handleCommandLineArgs(String[])}
     */
    public static void main(String... args) {
    	handleCommandLineArgs(args);
        initLogging();
        loadConfig();
        setupViewAndController();
    }

	//TODO "Usage"-Meldung waere gut
	/**
	 * Behandelt die Kommandozeilenargumente. Momentan zul&auml;ssig:
	 * <ul>
	 * <li><code>-conf pfad/zur/konfigdatei.xml</code>: Andere
	 * Konfigurationsdatei als die standardm&auml;&szlig;ige config/ct-sim.xml
	 * verwenden</li>
	 * </ul>
	 *
	 * @param args Kommandozeilenargumente, wie sie main() &uuml;bergeben
	 *            bekommen hat
	 */
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

	private static void initLogging() {
		try {
	        LogManager.getLogManager().readConfiguration(new FileInputStream(
	        	"config/logging.conf"));
		} catch (Exception e) {
			System.err.println("Logging konnte nicht initialisiert werden");
			e.printStackTrace();
			System.exit(1);
		}
        lg = FmtLogger.getLogger("");
		lg.fine("Logging-Subsystem initialisiert");
    }

	private static void loadConfig() {
		Config.SourceFile configFile = dependencies.get(
			Config.SourceFile.class);
		try {
			Config.loadConfigFile(configFile);
			return;
		} catch (FileNotFoundException e) {
			lg.severe(e, "Konfigurationsdatei '"+configFile+"' nicht gefunden");
		} catch (SAXException e) {
			lg.severe(e, "Fehler beim Parsen der Konfigurationsdatei '%s'",
				configFile);
		} catch (IOException e) {
			lg.severe(e, "E/A-Fehler beim Parsen der Konfigurationsdatei '%s'",
				configFile);
		} catch (ParserConfigurationException e) {
			lg.severe(e, "Fehler beim Parsen der Konfigurationsdatei '%s'",
				configFile);
		}
		System.exit(1);
	}

	private static void setupViewAndController() {
		//$$ kann einfacher werden mit pico (Startable ifc)
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			lg.warning(e, "Problem beim Setzen des Look and Feel");
		}

		Controller c = dependencies.get(Controller.class);

		// View der Applikation ist mindestens das MainWindow
		View view = new MainWindow(c);
		try {
			// View um ContestConductor erweitern falls so konfiguriert
			if (Config.getValue("useContestConductor").
					equalsIgnoreCase("true")) {
				view = ViewYAdapter.newInstance(view,
					dependencies.get(ContestConductor.class),
					new TimeLogger());
			}
		} catch (Exception e) {
			lg.warn(e, "Probleme beim Instanziieren des ContestConductor");
		}

		c.setView(view);
		c.onApplicationInited();
    }
}
