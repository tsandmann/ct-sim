package ctSim.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.UIManager;

import ctSim.ConfigManager;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.View;
import ctSim.view.ViewYAdapter;
import ctSim.view.contestConductor.ContestConductor;
import ctSim.view.gui.CtSimFrame;

//$$ Doc: Grobe Architektur beschreiben; wer verdrahtet M, V und C?
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

	private static FmtLogger lg;

	private static final String DEFAULT_CONFIGFILE = "config/ct-sim.xml";

	private static String configFile = DEFAULT_CONFIGFILE;

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
				configFile = args[i]; // Default ueberschreiben
			} else {
				System.out
				        .println("Ung\u00FCltiges Argument '" + args[i] + "'");
				System.exit(1);
			}
		}
	}

	/**
	 * Haupt-Einsprungpunkt in den ctSim. Das hier starten, um den ctSim zu
	 * starten.
	 *
	 * @param args Siehe {@link #handleCommandLineArgs(String[])}
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static void main(String[] args)
	throws SQLException, ClassNotFoundException {
		handleCommandLineArgs(args);
		initLogging();

		try {
	        ConfigManager.loadConfigFile(new File(configFile));
	        initViewAndController();
        } catch (FileNotFoundException e) {
	        lg.severe(e, "Konfigurationsdatei '"+configFile+"' nicht gefunden");
        }
	}

	private static void initLogging() {
		// $$ FmtLogger systemweit verwenden
		lg = FmtLogger.getLogger("");
		// Sowohl Root-Logger als auch der standardmaessige ConsoleHandler
		// stehen noch auf INFO
		lg.setLevel(Level.ALL);
		Handler console = lg.getHandlers()[0];
		console.setLevel(Level.ALL);

		// Seltsamerweise kommen Logausgaben (Level FINE) von
		// java.awt.sonstwie und sun.irgendwas
		// Seltsamerweise klappt der Filter auch nur, wenn man ihn an console
		// haengt (beim besseren lg geht's nicht)
		console.setFilter(new Filter() {
			public boolean isLoggable(LogRecord r) {
	            if (r.getLoggerName() == null)
	            	return true;
	            return ! Misc.beginsWith(r.getLoggerName(), "java", "sun");
            }});
		console.setFormatter(new Formatter() {
			private SimpleDateFormat df = new SimpleDateFormat(
			    "d.MM.y H:mm:ss.SSS");

			@Override
			public String format(LogRecord r) {
				String throwable = "";
				if (r.getThrown() != null) {
					StringWriter s = new StringWriter();
					r.getThrown().printStackTrace(new PrintWriter(s));
					throwable = s.toString();
				}
				// $$ Thread-Informationen beobachten. Ich bin mir nicht sicher -- die koennten schlicht falsch sein
				// $$ "* 2" ist quick and dirty
				Thread[] threads = new Thread[Thread.activeCount() * 2];
				Thread.enumerate(threads);
				String threadName = "";
				for (Thread t : threads) {
					if (t != null && t.getId() == r.getThreadID()) {
						threadName = t.getName() + " ";
						break;
					}
				}
				return r.getLevel() + ": " + r.getMessage() + " ["
				    + df.format(r.getMillis()) + "] " + r.getLoggerName() + "."
				    + r.getSourceMethodName() + "() " + "Thread " + threadName
				    + "(" + r.getThreadID() + ")" + "\n" + throwable;
            }});
		lg.fine("Logging-Subsystem initialisiert");
    }

	private static void initViewAndController()
	throws SQLException, ClassNotFoundException {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			lg.warning(e, "Problem beim Setzen des Look and Feel");
		}

		Controller c = new DefaultController();

		// View der Applikation ist mindestens der CtSimFrame
		View view = new CtSimFrame(c, "CtSim");

		// View um ContestConductor erweitern falls so konfiguriert
		if (ConfigManager.getValue("useContestConductor").
				equalsIgnoreCase("true"))
			view = ViewYAdapter.newInstance(view, new ContestConductor(c));

		c.setView(view);
		c.onApplicationInited();
    }
}
