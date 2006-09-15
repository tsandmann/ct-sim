package ctSim.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;

import javax.swing.UIManager;

import ctSim.ConfigManager;
import ctSim.view.View;
import ctSim.view.ViewYAdapter;
import ctSim.view.contestConductor.ContestConductor;
import ctSim.view.gui.CtSimFrame;

//$$ grobe Architektur; wer verdrahtet M, V und C?
/** <p>Haupt-Einsprungpunkt in den ctSim. Der normale Weg, das Programm zu 
 * starten, ist der Aufruf der {@link #main(String[])}-Methode dieser 
 * Klasse.</p>
 * 
 * <p>Inhaltlich befasst sich die Klasse mit grundlegenden 
 * Initialisierungs-Geschichten.</p> */
public class Main {
	private static final String DEFAULT_CONFIGFILE = "config/ct-sim.xml";
	private static String configFile = DEFAULT_CONFIGFILE;

	/** Behandelt die Kommandozeilenargumente. Momentan zul&auml;ssig:
	 * <ul>
	 * <li><code>-conf pfad/zur/konfigdatei.xml</code>: Andere 
	 * Konfigurationsdatei als die standardm&auml;&szlig;ige config/ct-sim.xml 
	 * verwenden</li>
	 * </ul>
	 * @param args	Kommandozeilenargumente, wie sie main() &uuml;bergeben 
	 * bekommen hat
	 */
	private static void handleCommandLineArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase().equals("-conf")) {
				i++;
				configFile = args[i]; // Default ueberschreiben
			} else {
				System.out.println("Ungueltiges Argument '"+args[i]+"'");
				System.exit(1);
			}
		}
	}

	/**
	 * @param args Siehe {@link #handleCommandLineArgs(String[])}
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	public static void main(String[] args) 
	throws SQLException, ClassNotFoundException {
		handleCommandLineArgs(args);
		try {
	        ConfigManager.loadConfigFile(new File(configFile));
	        initViewAndController();
        } catch (FileNotFoundException e) {
	        System.err.println(e.getMessage());
        }
	}

	private static void initViewAndController() 
	throws SQLException, ClassNotFoundException {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Controller c = new DefaultController();

		// View der Applikation ist mindestens der CtSimFrame
		View view = new CtSimFrame(c, "CtSim");
		
		// View erweitern um ContestConductor falls so konfiguriert
		if (ConfigManager.getValue("useContestConductor").
				equalsIgnoreCase("true"))
			view = ViewYAdapter.createInstance(view, new ContestConductor(c));
		
		c.setView(view);
    }
}
