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

package ctSim.view.gui;

import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

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
import ctSim.model.Map.MapException;
import ctSim.util.FmtLogger;
import ctSim.util.GridBaggins;
import ctSim.util.Menu;
import ctSim.util.Menu.Entry;


/**
 * <p>
 * Menüleiste und Toolbar des c't-Sim, zuständig für:
 * <ul>
 * <li>Menüleiste</li>
 * <li>die einzelnen Menüs</li>
 * <li>die Knöpfe der Toolbar</li>
 * <li>Event-Handling-Code, der ausgeführt wird, wenn der Benutzer einen Menüpunkt / einen Toolbar-Knopf
 * klickt</li>
 * <li>die Dialogfenster hinter den Menüpunkten (Welt öffnen, speichern usw.)</li>
 * </ul>
 * </p>
 *
 * @author Hendrik Krauß
 */
public class MainWinMenuBar extends JMenuBar {
	/** UID */
	private static final long serialVersionUID = - 5927950169956191902L;

	 /** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.MainWinMenuBar");

	/**
	 * Je nach dem, was der Benutzer im Menü; klickt, müssen wir oft im Controller eine Aktion anschieben;
	 * Default-Sichtbarkeit um Eclipses synthetic-access-Warnungen zu vermeiden.
	 */
	final Controller controller;

	/** Aufgebohrter JFileChooser: Zuständig für die "Parcours öffnen"- und "Parcours speichern"-Dialoge */
	private final WorldFileChooser worldChooser = new WorldFileChooser();

	/**
	 * Mit der Toolbar der Applikation haben wir indirekt zu tun: Es werden zuerst alle Menüs gebaut,
	 * dann die Toolbar, die den Inhalt von einigen der Menüs wiederholt. Weil in dieser Klasse die Menüs
	 * enthalten sind, wird die Toolbar mit erzeugt und später vom MainWindow per getToolBar() abgeholt.
	 */
	private final JToolBar toolBar;

	/**
	 * Siehe {@link #MainWinMenuBar(Controller, MainWindow)};
	 * Default-Sichtbarkeit um Eclipses synthetic-access-Warnungen zu vermeiden.
	 */
	final MainWindow mainWindow;

	/** Jugde-Namen */
	private static final String[] judgeClassNames = {
		"ctSim.model.rules.DefaultJudge",
		"ctSim.model.rules.LabyrinthJudge"};

	/** Buttons für Jugde */
	private ButtonGroup judgesButtonGroup = new ButtonGroup();

	/**
	 * @param controller
	 * @param mainWindow	als 'parent' der modalen Dialoge und für das gelegentliche Event, was auch im
	 * 			mainWindow verarbeitet werden muss.
	 */
	public MainWinMenuBar(Controller controller, MainWindow mainWindow) {
		this.controller = controller;
		this.mainWindow = mainWindow;

		// Prinzip: Menü machen; auf dessen Basis dann Toolbar, die einige der Menüs widerspiegelt
		JMenu worldMenu = new Menu("Welt",
			new Entry("Öffnen ...", Config.getIcon("Open16"), onOpenWorld),
			new Entry("Generieren", Config.getIcon("New16"), onOpenRandomWorld),
			new Entry("Speichern als ...", Config.getIcon("SaveAs16"), onSaveWorld),
			new Entry("Als Map exportieren...", Config.getIcon("ToMap16"), onWorldToMap),
			new Entry("Schließen", Config.getIcon("Delete16"), onCloseWorld)
		);
		add(worldMenu);

		JMenu connectMenu = new Menu("Verbinde mit Bot",
			new Entry("Per TCP ...", Config.getIcon("tcpbot16"), onAddTcpBot) /*,
			// Die Checkbox hat nen Haken und ist unveränderbar disabled (ausgegraut).
			// Sinn dahinter ist es den Benutzer wissen zu lassen, dass ctSim das automatisch macht.
			new Checkbox("Per USB (COM) automatisch", noOp).disable().check() */
		);
		add(connectMenu);

		JMenu botMenu = new Menu("Simuliere Bot",
			new Entry("Testbot", onAddTestBot),
			new Entry("c't-Bot", onInvokeExecutable)
		);
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
			new Entry("Github", Config.getIcon("github16"), onGithubLink),
			new Entry("Forum", Config.getIcon("forum16"), onForumLink),
			new Entry("About", Config.getIcon("about16"), onAbout)
		);
		add(supportMenu);

		toolBar = buildToolBar(worldMenu, connectMenu, botMenu, simulationMenu, supportMenu);
	}

	// Event-Handling-Code der Menüpunkte / Toolbar-Knöpfe

	/**
	 * Wählt der Benutzer einen Menüpunkt oder klickt einen Toolbar-Knopf, dann läuft einer der folgenden
	 * Runnables. Die Zuordnung welcher Menüpunkt -> welches Runnable findet im Konstruktor statt.
	 * (Es sind Runnables, diese haben mit Threading aber nichts zu tun an der Stelle.)
	 */

	/** Handler für Welt öffnen */
	private Runnable onOpenWorld = new Runnable() {
		public void run() {
			File f = worldChooser.showOpenWorldDialog();
			if (f != null)
				controller.openWorldFromFile(f);
		}
	};

	/** Handler für neue Zufallswelt */
	private Runnable onOpenRandomWorld = new Runnable() {
		public void run() {
			controller.openRandomWorld();
		}
	};

	/** Handler für Welt speichern */
	private Runnable onSaveWorld = new Runnable() {
		public void run() {
			File file = worldChooser.showSaveWorldDialog();
			if (file.exists()) {
				int result = JOptionPane.showConfirmDialog(
					mainWindow,
					// Meldung
					"Die Datei '" + file.getName() + "' existiert bereits. Soll sie überschrieben werden?",
					"überschreiben?",	// Dialogtitel
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
				if (result != JOptionPane.YES_OPTION)
					return;
			}
			mainWindow.writeParcoursToFile(file);
		}
	};

	/** Handler für Welt schließen */
	private Runnable onCloseWorld = new Runnable() {
		public void run() {
			controller.closeWorld();
			mainWindow.closeWorld();
		}
	};

	/** Handler für neuen Bot */
	private Runnable onAddTcpBot = new Runnable() {
		private final JTextField host = new JTextField(Config.getValue("ipForConnectByTcp"), 12);
		private final JTextField port = new JTextField(Config.getValue("portForConnectByTcp"), 5);
		private JDialog tcpEntryDialog = null;
		private JOptionPane optionPane = null;

		public void run() {
			if (tcpEntryDialog == null) {
				JPanel p = new JPanel();
				p.setLayout(new GridBagLayout());
				p.add(new JLabel("Host"), new GridBaggins().west().epadx(2).epady(4));
				p.add(new JLabel("Port"), new GridBaggins().col(2).west().epadx(2).epady(4));

				p.add(host, new GridBaggins().row(1));
				p.add(new JLabel(":"), new GridBaggins().row(1).epadx(3));
				p.add(port, new GridBaggins().row(1));

				optionPane = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				tcpEntryDialog = optionPane.createDialog(
					mainWindow,				// parent
					"Wohin verbinden?");	// Dialog-Titel
			}

			tcpEntryDialog.setVisible(true);
			if (((Integer)optionPane.getValue()) == JOptionPane.YES_OPTION) {
//				parseInt() muss funktionieren dank MaskFormatter
				controller.connectToTcp(host.getText(), port.getText());
			}
		}
	};

	/** Handler für neuen Testbot */
	private Runnable onAddTestBot = new Runnable() {
		public void run() {
			controller.addTestBot();
		}
	};

	/** Handler zum ausführen eines Binaries */
	private Runnable onInvokeExecutable = new Runnable() {
		private final JFileChooser botChooser = new JFileChooser(ConfigManager.path2Os(Config.getValue("botdir")));

		{	// der "Konstruktor" des Runnables sozusagen
			botChooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return (f.isDirectory() || f.getName().endsWith(".exe") || f.getName().endsWith(".elf")
							|| f.getName().lastIndexOf('.') == -1);
				}

				@Override
				public String getDescription() {
					return "Bot-Controller (*.exe, *.elf)";
				}
			});
		}

		public void run() {
			if (botChooser.showOpenDialog(mainWindow) == JFileChooser.APPROVE_OPTION) {
				controller.invokeBot(botChooser.getSelectedFile());
			}
		}
	};

	/** Handler für Simulation starten */
	private Runnable onStartSimulation = new Runnable() {
		public void run() {
			controller.unpause();
		}
	};

	/** Handler für Simulation anhalten */
	private Runnable onPauseSimulation = new Runnable() {
		public void run() {
			controller.pause();
		}
	};

	/** Handler für About-Eintrag */
	private Runnable onAbout = new Runnable() {
		public void run() {
			/* Splash-Screen anzeigen */
			java.net.URL url = ClassLoader.getSystemResource("images/splash.jpg");
			SplashWindow.splash(url, "Version " + Main.VERSION);
			SplashWindow.setMessage("                                                      c't-Bot Projekt 2006-2018");
		}
	};

	/** Handler für Webseite-Link */
	private Runnable onSiteLink = new Runnable() {
		public void run() {
			try {
				Desktop.getDesktop().browse(
						new URL("https://www.heise.de/ct/artikel/c-t-Bot-und-c-t-Sim-284119.html").toURI());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/**
	 * Handler für Github-Link
	 */
	private Runnable onGithubLink = new Runnable() {
		public void run() {
			try {
				Desktop.getDesktop().browse(
						new URL("https://github.com/tsandmann/ct-sim").toURI());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/** Handler für Forum-Link */
	private Runnable onForumLink = new Runnable() {
		public void run() {
			try {
				Desktop.getDesktop().browse(
						new URL("https://www.ctbot.de").toURI());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	/** Handler für Bots resetten */
	private Runnable onResetBots = new Runnable() {
		public void run() {
			controller.resetAllBots();
		}
	};

	/** Handler für Welt als Bot-Map exportieren */
	private Runnable onWorldToMap = new Runnable() {
		private final JTextField bot = new JTextField("1", 8);
		private final JTextField free = new JTextField("100", 5);
		private final JTextField occupied = new JTextField("-100", 5);
		private JDialog mapDialog = null;
		private JOptionPane optionPane = null;

		public void run() {
			if (mapDialog == null) {
				JPanel p = new JPanel();
				p.setLayout(new GridBagLayout());
				p.add(new JLabel("Startfeld"), new GridBaggins().west().epadx(2).epady(4));
				p.add(new JLabel("Wert frei"), new GridBaggins().col(1).west().epadx(2).epady(4));
				p.add(new JLabel("Wert belegt"), new GridBaggins().col(2).west().epadx(2).epady(4));

				p.add(bot, new GridBaggins().row(1));
				p.add(free, new GridBaggins().row(1));
				p.add(occupied, new GridBaggins().row(1));

				optionPane = new JOptionPane(p, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
				mapDialog = optionPane.createDialog(
					mainWindow,							// parent
					"Parcours als Map exportieren");	// Dialog-Titel
			}

			mapDialog.setVisible(true);
			if (((Integer)optionPane.getValue()) == JOptionPane.YES_OPTION) {
				try {
					controller.worldToMap(Integer.parseInt(bot.getText()), Integer.parseInt(free.getText()),
							Integer.parseInt(occupied.getText()));
					lg.info("Welt wurde korrekt als Bot-Map exportiert");
				} catch (NumberFormatException e) {
					lg.warn("Ungültige Eingabewerte: " + e.getMessage());
				} catch (IOException e) {
					lg.warn("Fehler beim Schreiben der Datei: " + e.getMessage());
				} catch (MapException e) {
					lg.warn("Map enthält keine Daten, die exportiert werden könnten");
				}
			}
		}
	};

	// Hilfsmethoden

	/**
	 * Baut das Judge-Menü
	 *
	 * @return Menü
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
	 *
	 * @param menus	Menüs
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
//	}

	// Hilfsklasse

	/** Siehe {@link #worldChooser} */
	private class WorldFileChooser extends JFileChooser {
		/** UID */
		private static final long serialVersionUID = 6693056925110674157L;

		/**
		 * @return Datei zum Welt-Öffnen
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

		/** Wählt eine Welt-Datei aus */
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

	/** Judge-Menü */
	private class JudgeMenuItem extends JMenuItem {
		/** UID */
		private static final long serialVersionUID = - 8177774672896579874L;

		/** Judge-Klassenname */
		@SuppressWarnings("unused")
		public final String fqJudgeClassName;

		/**
		 * @param fqName	Judge-Name
		 */
		public JudgeMenuItem(final String fqName) {
			// für das Anzeigen den Packagename wegnehmen, nur den Klassenname angeben
			super(new AbstractAction(fqName.replaceAll("^.*\\.(.*)$", "$1"))

				{
					private static final long serialVersionUID = -1873920690635293756L;

					public void actionPerformed(ActionEvent e) {
						controller.setJudge(fqName);
					}
				}

			);
			this.fqJudgeClassName = fqName;
		}
	}
}
