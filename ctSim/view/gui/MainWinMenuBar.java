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
import ctSim.controller.Main;
import ctSim.util.GridBaggins;
import ctSim.util.Menu;
//import ctSim.util.Runnable1;
//import ctSim.util.Menu.Checkbox;
import ctSim.util.Menu.Entry;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

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
	/** UID */
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

	/** Jugde-Namen */
	private static final String[] judgeClassNames = {
		"ctSim.model.rules.DefaultJudge",
		"ctSim.model.rules.LabyrinthJudge"};
	
	/** Buttons fuer Jugde */
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
		JMenu connectMenu = new Menu("Verbinde mit Bot",
	    	new Entry("Per TCP ...", Config.getIcon("tcpbot16"), onAddTcpBot)/*,
	    	// Die Checkbox hat nen Haken und ist unveraenderbar disabled
	    	// (ausgegraut). Sinn: Benutzer wissen lassen, dass ctSim das
	    	// automatisch macht
	    	new Checkbox("Per USB (COM) automatisch", noOp).disable().check()*/);
		add(connectMenu);
		JMenu botMenu = new Menu("Simuliere Bot",
	    	new Entry("Testbot", onAddTestBot),
	    	new Entry("c't-Bot", onInvokeExecutable));
		add(botMenu);

	    JMenu m = new JMenu("Schiedsrichter");
	    for (JMenuItem item : buildJudgeMenuItems())
	    	m.add(item);
	    add(m);
	    
	    JMenu simulationMenu = new Menu("Simulation", 
	    		new Entry("Start", Config.getIcon("Play16"), onStartSimulation), 
	    		new Entry("Pause", Config.getIcon("Pause16"), onPauseSimulation),
	    		new Entry("Reset", Config.getIcon("reset16"), onResetBots)
	    );
	    add(simulationMenu);

		JMenu supportMenu = new Menu("Support",
			new Entry("Webseite", Config.getIcon("website16"), onSiteLink),
			new Entry("Trac", Config.getIcon("trac16"), onTracLink),
			new Entry("Forum", Config.getIcon("forum16"), onForumLink),
			new Entry("About", Config.getIcon("about16"), onAbout));
		add(supportMenu);
	    
	    toolBar = buildToolBar(worldMenu, connectMenu, botMenu, simulationMenu, supportMenu);
	}

	///////////////////////////////////////////////////////////////////////////
	// Event-Handling-Code der Menuepunkte / Toolbar-Knoepfe

	// Waehlt der Benutzer einen Menuepunkt oder klickt einen Toolbar-Knopf,
	// dann laeuft einer der folgenden Runnables. Die Zuordnung welcher
	// Menuepunkt -> welches Runnable findet im Konstruktor statt. (Sind
	// Runnables, haben mit Threading aber nichts zu tun an der Stelle.)
		
	/**
	 * Handler fuer Welt Oeffnen
	 */
	private Runnable onOpenWorld = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			File f = worldChooser.showOpenWorldDialog();
			if (f != null)
				controller.openWorldFromFile(f);
		}
	};

	/**
	 * Handler fuer neue Zufallswelt
	 */
	private Runnable onOpenRandomWorld = new Runnable() {
		public void run() {
			controller.openRandomWorld();
		}
	};

	/**
	 * Handler fuer Welt speichern
	 */
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

	/**
	 * Handler fuer Welt schliessen
	 */
	private Runnable onCloseWorld = new Runnable() {
		public void run() {
			controller.closeWorld();
			mainWindow.closeWorld();
		}
	};

	/**
	 * Handler fuer neuen Bot
	 */
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

//	/**
//	 * NOP
//	 */
//	private Runnable1<Boolean> noOp = new Runnable1<Boolean>() {
//		public void run(@SuppressWarnings("unused") Boolean argument) {
//			// No-Op
//		}
//	};

	/**
	 * Handler fuer neuen Testbot
	 */
	private Runnable onAddTestBot = new Runnable() {
		public void run() {
			controller.addTestBot();
		}
	};

	/**
	 * Handler zum ausfuehren eines Binaries
	 */
	private Runnable onInvokeExecutable = new Runnable() {
		private final JFileChooser botChooser = new JFileChooser(
			ConfigManager.path2Os(Config.getValue("botdir")));

		{ // "Konstruktor" des Runnable sozusagen
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

	/**
	 * Handler fuer Simulation starten
	 */
	private Runnable onStartSimulation = new Runnable() {
		public void run() {
			controller.unpause();
		}
	};

	/**
	 * Handler fuer Simulation anhalten
	 */
	private Runnable onPauseSimulation = new Runnable() {
		public void run() {
			controller.pause();
		}
	};
	
	/**
	 * Handler fuer About-Eintrag
	 */
	private Runnable onAbout = new Runnable() {
		public void run() {
			/* Splash-Screen anzeigen */
			java.net.URL url = ClassLoader.getSystemResource("images/splash.jpg");
			SplashWindow.splash(url, "Version " + Main.VERSION);
			SplashWindow.setMessage("                                                      " +
				"c't-Bot Projekt 2006-2008");
		}
	};
	
	/**
	 * Handler fuer Webseite-Link
	 */
	private Runnable onSiteLink = new Runnable() {
		public void run() {
			try {
				BrowserLauncher launcher = new BrowserLauncher();
				launcher.openURLinBrowser("http://www.ct-bot.de");
			} catch (BrowserLaunchingInitializingException e) {
				e.printStackTrace();
			} catch (UnsupportedOperatingSystemException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Handler fuer Trac-Link
	 */
	private Runnable onTracLink = new Runnable() {
		public void run() {
			try {
				BrowserLauncher launcher = new BrowserLauncher();
				launcher.openURLinBrowser("http://www.heise.de/ct/projekte/machmit/ctbot/wiki");
			} catch (BrowserLaunchingInitializingException e) {
				e.printStackTrace();
			} catch (UnsupportedOperatingSystemException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Handler fuer Forum-Link
	 */
	private Runnable onForumLink = new Runnable() {
		public void run() {
			try {
				BrowserLauncher launcher = new BrowserLauncher();
				launcher.openURLinBrowser("http://www.heise.de/ct/foren/go.shtml?list=1&forum_id=89813");
			} catch (BrowserLaunchingInitializingException e) {
				e.printStackTrace();
			} catch (UnsupportedOperatingSystemException e) {
				e.printStackTrace();
			}
		}
	};
	
	/**
	 * Handler fuer Bots resetten
	 */
	private Runnable onResetBots = new Runnable() {
		public void run() {
			controller.resetAllBots();
		}
	};

	///////////////////////////////////////////////////////////////////////////
	// Hilfsmethoden

	/**
	 * Baut das Jugde-Menue
	 * @return Menue
	 */
	private JMenuItem[] buildJudgeMenuItems() {
		JMenuItem[] rv = new JMenuItem[judgeClassNames.length];
		for (int i = 0; i < judgeClassNames.length; i++) {
			rv[i] = new JudgeMenuItem(judgeClassNames[i]);
		    judgesButtonGroup.add(rv[i]);
		}
		return rv;
	}

	/**
	 * Baut die Toolbar
	 * @param menus Menues
	 * @return Toolbar
	 */
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

	/**
	 * @return Toolbar
	 */
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
		/** UID */
		private static final long serialVersionUID = 6693056925110674157L;

		/**
		 * @return Datei zum Welt-Oeffnen
		 */
		public File showOpenWorldDialog() {
			if (showOpenDialog(mainWindow) == APPROVE_OPTION)
				return getSelectedXmlFile();
			return null;
		}

		/**
		 * @return Datei zum Welt-Speichern
		 */
		public File showSaveWorldDialog() {
			if (showSaveDialog(mainWindow) == APPROVE_OPTION)
				return getSelectedXmlFile();
			return null;
		}

		/**
		 * Waehlt eine Welt-Datei aus
		 */
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

		/**
		 * @return File-Objekt einer XML-Datei
		 */
		private File getSelectedXmlFile() {
			File f = getSelectedFile();
			if (! f.exists() && ! f.getName().endsWith(".xml"))
				f = new File(f.getPath() + ".xml");
			return f;
		}
	}

    /**
     * Judge-Menue
     */
    private class JudgeMenuItem extends JMenuItem {
        /** UID */
    	private static final long serialVersionUID = - 8177774672896579874L;

        /**
         * Judge-Klassenname
         */
        public final String fqJudgeClassName;

		/**
		 * @param fqName Judge-Name
		 */
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
