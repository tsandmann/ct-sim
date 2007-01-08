package ctSim.view.gui;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import ctSim.ConfigManager;
import ctSim.controller.Controller;
import ctSim.model.rules.Judge;
import ctSim.util.Enumerations;
import ctSim.util.IconHashMap;

/**
 * <p>
 * Men&uuml;leiste und Toolbar des c't-Sim. Zust&auml;ndig f&uuml;r:
 * <ul>
 * <li>Men&uuml;leiste</li>
 * <li>die einzelnen Men&uuml;s</li>
 * <li>die Kn&ouml;pfe der Toolbar</li>
 * <li>Event-Handling-Code, der ausgef&uuml;hrt wird, wenn der Benutzer einen
 * Men&uuml;punkt / einen Toolbar-Knopf klickt</li>
 * <li>die Dialogfenster hinter den Men&uuml;punkten (Welt &ouml;ffnen,
 * speichern usw.)</li>
 * </ul>
 * </p>
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class MainWinMenuBar extends JMenuBar {
	private static final long serialVersionUID = - 5927950169956191902L;

	/**
	 * Je nach dem, was der Benutzer im Men&uuml; klickt, m&uuml;ssen wir oft im
	 * Controller eine Aktion anschubsen. Default-Sichtbarkeit, um
	 * Eclipses synthetic-access-Warnungen zu vermeiden.
	 */
	final Controller controller;

	/**
	 * Aufgebohrter JFileChooser: Zust&auml;ndig f&uuml;r die &quot;Parcours
	 * &ouml;ffnen&quot;- und &quot;Parcours speichern&quot;-Dialoge
	 */
	private final WorldFileChooser worldChooser = new WorldFileChooser();

	/**
	 * Mit der Toolbar der Applikation haben wir indirekt zu tun: Es werden
	 * zuerst alle Men&uuml;s gebaut, dann die Toolbar, die den Inhalt von
	 * einigen der Men&uuml;s wiederholt. Weil in dieser Klasse die Men&uuml;s
	 * wohnen, wird die Toolbar miterzeugt und sp&auml;ter vom MainWindow per
	 * getToolBar() abgeholt.
	 */
	private final JToolBar toolBar;

	/**
	 * Siehe {@link #MainWinMenuBar(Controller, MainWindow, IconHashMap)}.
	 * Default-Sichtbarkeit, um Eclipses synthetic-access-Warnungen zu
	 * vermeiden.
	 */
	final MainWindow mainWindow;

	//$$ Nach Judge-Umbau: kann weg
	private static final String[] judgeClassNames = { //LODO Judges hardcoded
		"ctSim.model.rules.DefaultJudge",
		"ctSim.model.rules.LabyrinthJudge"};

	//$$ Nach Judge-Umbau: kann weg
	private ButtonGroup judgesButtonGroup = new ButtonGroup();

	/**
	 * @param controller
	 * @param mainWindow Als 'parent' der modalen Dialoge und f&uuml;r das
	 * gelegentliche Event, was auch im mainWindow verarbeitet werden muss.
	 * @param icons
	 */
	public MainWinMenuBar(Controller controller, MainWindow mainWindow,
		IconHashMap icons) {

		this.controller = controller;
		this.mainWindow = mainWindow;

		// Prinzip: Menue machen; auf dessen Basis dann Toolbar, die
		// einige der Menues widerspiegelt
		JMenu worldMenu = buildMenu("Welt",
			action("\u00D6ffnen ...", icons.get("Open16"), onOpenWorld),
			action("Generieren", icons.get("New16"), onOpenRandomWorld),
			action("Speichern als ...", icons.get("SaveAs16"), onSaveWorld),
			action("Schlie\u00DFen", icons.get("Delete16"), onCloseWorld));
		add(worldMenu);
	    add(buildMenu("Verbinde mit Bot",
    		action("Per USB (COM)", onAddComBot),
    		action("Per TCP ...", onAddTcpBot)));
	    add(buildMenu("Simuliere Bot",
    		action("Testbot", onAddTestBot),
    		//$$ os.name-Kram verlagern in eine Klasse fuer plattformabhaengiges Zeug
    		action(((System.getProperty("os.name").indexOf("Windows") != -1)
				? ".exe" : ".elf") + " starten ...",
    			onInvokeExecutable)));

	    JMenu m = new JMenu("Schiedsrichter");
	    for (JMenuItem item : buildJudgeMenuItems())
	    	m.add(item);
	    add(m);

	    JMenu simulationMenu = buildMenu("Simulation",
    		action("Start", icons.get("Play16"), onStartSimulation),
    		action("Stop", icons.get("Stop16"), onResetSimulation),
    		action("Pause", icons.get("Pause16"), onPauseSimulation));
	    add(simulationMenu);

	    toolBar = buildToolBar(worldMenu, simulationMenu);
	}

	/** Genauso wie {@link #action(Sring, Icon, Runnable)}, nur mit ohne Icon */
	private Action action(String name, Runnable code) {
		return action(name, null, code);
	}

	/**
	 * Meth&ouml;dchen, um
	 * {@link #MainWinMenuBar(Controller, MainWindow, IconHashMap) MainWinMenuBar()}
	 * lesbarer zu machen. Konstruiert eine {@link Action} aus Beschriftung,
	 * Icon und auszuf&uuml;hrendem Code.
	 */
	private Action action(String name, Icon icon, final Runnable code) {
		return new AbstractAction(name, icon) {
			private static final long serialVersionUID = 8468636621500013742L;

			public void actionPerformed(
				@SuppressWarnings("unused") ActionEvent e) {

				code.run();
			}
		};
	}

	///////////////////////////////////////////////////////////////////////////
	// Event-Handling-Code der Menuepunkte / Toolbar-Knoepfe

	// Waehlt der Benutzer einen Menuepunkt oder klickt einen Toolbar-Knopf,
	// dann laeuft einer der folgenden Runnables. Die Zuordnung welcher
	// Menuepunkt -> welches Runnable findet im Konstruktor statt. (Sind
	// Runnables, haben mit Threading aber nichts zu tun an der Stelle.)

	private Runnable onOpenWorld = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			File f = worldChooser.showOpenWorldDialog();
			if (f != null)
				controller.openWorldFromFile(f);
		}
	};

	private Runnable onOpenRandomWorld = new Runnable() {
		public void run() {
			controller.openRandomWorld();
		}
	};

	private Runnable onSaveWorld = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			File file = worldChooser.showSaveWorldDialog();
			if (file.exists()) {
				int result = JOptionPane.showConfirmDialog(
					mainWindow,
					// Meldung
					"Die Datei '"+file.getName()+"' existiert " +
					"bereits. Soll sie \u00FCberschrieben werden?",
					"\00DCberschreiben?", // Dialogtitel
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
				if (result != JOptionPane.YES_OPTION)
					return;
			}
			mainWindow.writeParcoursToFile(file);
		}
	};

	private Runnable onCloseWorld = new Runnable() {
		public void run() {
			controller.closeWorld();
			mainWindow.closeWorld();
		}
	};

	private Runnable onAddComBot = new Runnable() {
		public void run() {
			controller.addComBot();
		}
	};

	private Runnable onAddTcpBot = new Runnable() {
		public void run() {
			String address = (String)JOptionPane.showInputDialog(
				mainWindow,
				"Adresse:",
				"Wohin verbinden?",
				JOptionPane.QUESTION_MESSAGE,
				null, // Icon
				null, // Moeglichkeiten == null -> Freitextfeld
				""); //$$$ Welche IP/Port standardmaessig ins Textfeld?

			if (address == null)
				// Benutzer hat abgebrochen
				return;

			controller.connectToTcp(address);
		}
	};

	private Runnable onAddTestBot = new Runnable() {
		public void run() {
			controller.addTestBot();
		}
	};

	private Runnable onInvokeExecutable = new Runnable() {
		private final JFileChooser botChooser = new JFileChooser(
			ConfigManager.path2Os(ConfigManager.getValue("botdir")));

		{ // "Konstruktor" des Runnable sozusagen
			//$$ Sollte nur exes zeigen (Win) bzw. nur elfs (Linux)
			botChooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return (f.isDirectory() || f.getName().endsWith(".exe")
							|| f.getName().endsWith(".elf"));
				}

				@Override
				public String getDescription() {
					return "Bot-Controller (*.exe, *.elf)";
				}
			});
		}

		public void run() {
			if (botChooser.showOpenDialog(mainWindow) ==
				JFileChooser.APPROVE_OPTION) {

				controller.invokeBot(botChooser.getSelectedFile());
			}
		}
	};

	private Runnable onStartSimulation = new Runnable() {
		public void run() {
			controller.unpause();
		}
	};

	private Runnable onPauseSimulation = new Runnable() {
		public void run() {
			controller.pause();
		}
	};

	private Runnable onResetSimulation = new Runnable() {
		public void run() {
			controller.reset();
			mainWindow.reset();
		}
	};

	///////////////////////////////////////////////////////////////////////////
	// Hilfsmethoden

	private JMenu buildMenu(String title, Action... items) {
    	JMenu rv = new JMenu(title);
    	for (Action a : items)
    		rv.add(a);
    	return rv;
    }

	//$$ Nach Judge-Umbau: kann weg
	private JMenuItem[] buildJudgeMenuItems() {
		JMenuItem[] rv = new JMenuItem[judgeClassNames.length];
		for (int i = 0; i < judgeClassNames.length; i++) {
			rv[i] = new JudgeMenuItem(judgeClassNames[i]);
		    judgesButtonGroup.add(rv[i]);
		}
		return rv;
	}

	private JToolBar buildToolBar(JMenu... menus) {
		JToolBar rv = new JToolBar();
		for (JMenu menu : menus) {
			for (int i = 0; i < menu.getItemCount(); i++)
				rv.add(menu.getItem(i).getAction());
			rv.addSeparator();
		}
		// Letzter Separator wieder weg ("fence post error")
		rv.remove(rv.getComponent(rv.getComponentCount() - 1));
		return rv;
	}

	public JToolBar getToolBar() {
		return toolBar;
	}

	//$$ Nach Judge-Umbau: Kann weg
	public void onJudgeSet(Judge judge) {
		for (AbstractButton b : Enumerations.asIterable(
			judgesButtonGroup.getElements())) {
			if (judge.getClass().getName().equals(
				((JudgeMenuItem)b).fqJudgeClassName))
				b.setSelected(true);
		}
    }

	///////////////////////////////////////////////////////////////////////////
	// Hilfsklasse

	/** Siehe {@link #worldChooser} */
	class WorldFileChooser extends JFileChooser {
		private static final long serialVersionUID = 6693056925110674157L;

		public File showOpenWorldDialog() {
			if (showOpenDialog(mainWindow) == APPROVE_OPTION)
				return getSelectedXmlFile();
			return null;
		}

		public File showSaveWorldDialog() {
			if (showSaveDialog(mainWindow) == APPROVE_OPTION)
				return getSelectedXmlFile();
			return null;
		}

		public WorldFileChooser() {
			super(ConfigManager.getValue("worlddir"));
			setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return (f.isDirectory() || f.getName().endsWith(".xml"));
				}

				@Override
				public String getDescription() {
					return "Parcours-Dateien (*.xml)";
				}
			});
		}

		private File getSelectedXmlFile() {
			File f = getSelectedFile();
			if (! f.exists() && ! f.getName().endsWith(".xml"))
				f = new File(f.getPath() + ".xml");
			return f;
		}
	}

	//$$ Nach Judge-Umbau: Kann weg
    class JudgeMenuItem extends JRadioButtonMenuItem {
        private static final long serialVersionUID = - 8177774672896579874L;

        public final String fqJudgeClassName;

		public JudgeMenuItem(final String fqName) {
			// Fuers Anzeigen Packagename weg, nur Klassenname
	        super(new AbstractAction(fqName.replaceAll("^.*\\.(.*)$", "$1"))
	        	 {
					private static final long serialVersionUID =
						-1873920690635293756L;

					@SuppressWarnings("synthetic-access")
	                public void actionPerformed(
	                	@SuppressWarnings("unused") ActionEvent e) {

						controller.setJudge(fqName);
					}
				}
	        );
	        this.fqJudgeClassName = fqName;
        }
    }
}
