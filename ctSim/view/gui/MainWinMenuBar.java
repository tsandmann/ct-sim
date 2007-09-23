package ctSim.view.gui;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileFilter;

import ctSim.ConfigManager;
import ctSim.controller.Config;
import ctSim.controller.Controller;
import ctSim.util.GridBaggins;
import ctSim.util.Menu;
import ctSim.util.Runnable1;
import ctSim.util.Menu.Checkbox;
import ctSim.util.Menu.Entry;

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
	 * Siehe {@link #MainWinMenuBar(Controller, MainWindow)}.
	 * Default-Sichtbarkeit, um Eclipses synthetic-access-Warnungen zu
	 * vermeiden.
	 */
	final MainWindow mainWindow;

	//$$ Wenn Bug 22 erledigt: kann weg
	private static final String[] judgeClassNames = {
		"ctSim.model.rules.DefaultJudge",
		"ctSim.model.rules.LabyrinthJudge"};

	//$$ Wenn Bug 22 erledigt: kann weg
	private ButtonGroup judgesButtonGroup = new ButtonGroup();

	/**
	 * @param controller
	 * @param mainWindow Als 'parent' der modalen Dialoge und f&uuml;r das
	 * gelegentliche Event, was auch im mainWindow verarbeitet werden muss.
	 */
	public MainWinMenuBar(Controller controller, MainWindow mainWindow) {
		this.controller = controller;
		this.mainWindow = mainWindow;

		// Prinzip: Menue machen; auf dessen Basis dann Toolbar, die
		// einige der Menues widerspiegelt
		JMenu worldMenu = new Menu("Welt",
			new Entry("\u00D6ffnen ...", Config.getIcon("Open16"), onOpenWorld),
			new Entry("Generieren", Config.getIcon("New16"), onOpenRandomWorld),
			new Entry("Speichern als ...", Config.getIcon("SaveAs16"), onSaveWorld),
			new Entry("Schlie\u00DFen", Config.getIcon("Delete16"), onCloseWorld));
		add(worldMenu);
	    add(new Menu("Verbinde mit Bot",
	    	new Entry("Per TCP ...", onAddTcpBot),
	    	// Die Checkbox hat nen Haken und ist unveraenderbar disabled
	    	// (ausgegraut). Sinn: Benutzer wissen lassen, dass ctSim das
	    	// automatisch macht
	    	new Checkbox("Per USB (COM) automatisch", noOp).disable().check()));
	    add(new Menu("Simuliere Bot",
	    	new Entry("Testbot", onAddTestBot),
    		//$$ os.name-Kram verlagern in eine Klasse fuer plattformabhaengiges Zeug
	    	new Entry(((System.getProperty("os.name").indexOf("Windows") != -1)
				? ".exe" : ".elf") + " starten ...",
    			onInvokeExecutable)));

	    JMenu m = new JMenu("Schiedsrichter");
	    for (JMenuItem item : buildJudgeMenuItems())
	    	m.add(item);
	    add(m);

	    JMenu simulationMenu = new Menu("Simulation",
	    	new Entry("Start", Config.getIcon("Play16"), onStartSimulation),
	    	new Entry("Pause", Config.getIcon("Pause16"), onPauseSimulation));
	    add(simulationMenu);

	    toolBar = buildToolBar(worldMenu, simulationMenu);
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
				//$$ t Ist das jemals getestet worden?
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

	private Runnable onAddTcpBot = new Runnable() {
		private final JTextField host = new JTextField(
			Config.getValue("ipForConnectByTcp"), 12);
		private final JTextField port = new JTextField(
			Config.getValue("portForConnectByTcp"), 5);
		private JDialog tcpEntryDialog = null;
		private JOptionPane optionPane = null;

		public void run() {
			if (tcpEntryDialog == null) {
				JPanel p = new JPanel();
				p.setLayout(new GridBagLayout());
				p.add(new JLabel("Host"),
					new GridBaggins().west().epadx(2).epady(4));
				p.add(new JLabel("Port"),
					new GridBaggins().col(2).west().epadx(2).epady(4));

				p.add(host, new GridBaggins().row(1));
				p.add(new JLabel(":"), new GridBaggins().row(1).epadx(3));
				p.add(port, new GridBaggins().row(1));

				optionPane = new JOptionPane(
					p,
					JOptionPane.QUESTION_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION);
				tcpEntryDialog = optionPane.createDialog(
					mainWindow, // parent
					"Wohin verbinden?"); // Dialog-Titel
			}

			tcpEntryDialog.setVisible(true);
			if (((Integer)optionPane.getValue()) == JOptionPane.YES_OPTION) {
				// parseInt() muss funktionieren dank MaskFormatter
				controller.connectToTcp(host.getText(), port.getText());
			}
		}
	};

	private Runnable1<Boolean> noOp = new Runnable1<Boolean>() {
		public void run(@SuppressWarnings("unused") Boolean argument) {
			// No-Op
		}
	};

	private Runnable onAddTestBot = new Runnable() {
		public void run() {
			controller.addTestBot();
		}
	};

	private Runnable onInvokeExecutable = new Runnable() {
		private final JFileChooser botChooser = new JFileChooser(
			ConfigManager.path2Os(Config.getValue("botdir")));

		{ // "Konstruktor" des Runnable sozusagen
			//$$ Sollte nur exes zeigen (Win) bzw. nur elfs (Linux)
			botChooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return (f.isDirectory() || f.getName().endsWith(".exe")
							|| f.getName().endsWith(".elf") || f.getName().lastIndexOf('.') == -1);
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

	///////////////////////////////////////////////////////////////////////////
	// Hilfsmethoden

	//$$ Wenn Bug 22 erledigt: kann weg
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

//	public void onJudgeSet(Judge judge) {
//		if (!controller.setJudge(judge.toString())) return;
//		for (AbstractButton b : Enumerations.asIterable(
//			judgesButtonGroup.getElements())) {
//			if (judge.getClass().getName().equals(
//				((JudgeMenuItem)b).fqJudgeClassName))
//				b.setSelected(true);
//		}
//   }

	///////////////////////////////////////////////////////////////////////////
	// Hilfsklasse

	/** Siehe {@link #worldChooser} */
	private class WorldFileChooser extends JFileChooser {
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
			super(Config.getValue("worlddir"));
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

    private class JudgeMenuItem extends JMenuItem {
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
