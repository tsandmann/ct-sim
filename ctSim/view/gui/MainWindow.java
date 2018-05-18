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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;

import ctSim.controller.Config;
import ctSim.controller.Controller;
import ctSim.controller.Main;
import ctSim.model.ThreeDBot;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.model.rules.Judge;
import ctSim.util.ClosableTabsPane;
import ctSim.util.FmtLogger;
import ctSim.util.Runnable1;

/**
 * Die GUI-Hauptklasse für den c't-Sim
 *
 * @author Felix Beckwermert
 * @author Hendrik Krauß
 */
public class MainWindow extends JFrame implements ctSim.view.View {
    /** UID */
	private static final long serialVersionUID = 3689470428407624063L;
    /** Logger */
    final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui");

	/* ============================================================ */

	// GUI-Components:
	
    /** Statusbar */
    private StatusBar statusBar;
    /** Split-Pane */
	private JSplitPane split;

	/**
	 * <p>
	 * Enthält ein Tab pro Bot. Wenn einer am Tab das Schließen-Icon ("X") klickt, gilt das Prinzip "zuständig
	 * für Bot entfernen ist der Bot, sonst niemand". Das führt zu einem nicht ganz offensichtlichen Ablauf:
	 * <ol>
	 * <li>ClosableTabsPane ruft den CloseListener auf, den MainWindow bei der ClosableTabsPane angemeldet
	 * hat</li>
	 * <li>CloseListener sagt Bot "verkrümel dich" ({@code dispose()}), macht aber sonst nichts; entfernt keine
	 * Tabs etc. </li>
	 * <li>Der Bot geht seine Deinitialisierungs-Routine durch.</li>
	 * <li>Der Bot ruft seine DisposeListener auf. Einer davon ist der, welchen MainWindow beim Bot angemeldet
	 * hat: Dieser sagt schließlich dem Tab "zeig dich nicht mehr an"</li>
	 * </ol>
	 * </p>
	 * <p>
	 * Grund: Der CloseListener <em>könnte</em> auch direkt das Tab entfernen. Allerdings muss der
	 * DisposeListener in jedem Fall das Tab entfernen, schon weil das Bot.dispose() ja von woanders ausgelöst
	 * werden kann. Daher kommt es zu Problemen, wenn nach einem "Schließen"-Klick der CloseListener ein Tab
	 * entfernt und der DisposeListener ebenfalls eines entfernt. Deswegen die klare Regelung, dass nur im
	 * DisposeListener Tabs entfernt werden.
	 * </p>
	 */
	private final ClosableTabsPane botTabs;

	/* ============================================================ */
	
	/** Welt */
	private World world;
	/** Welt-Viewer */
	private WorldViewer worldViewer = new WorldViewer();
	/** Menuzeile */
	private MainWinMenuBar menuBar;

	/**
	 * Main-Fenster
	 * 
	 * @param controller	Controller für das Fenster
	 */
	public MainWindow(final Controller controller) {
		super("c't-Sim " + Main.VERSION);

    	/*
		 * Riesenspaß: Swing (also auch unsere GUI) ist "lightweight", Java3D ist jedoch "heavyweight".
		 * Daher erscheint die Java3D-Anzeige immer _vor_ allen GUI-Komponenten: Wenn z.B. ein Menü aufgeklappt
		 * wird, welches die 3D-Anzeige überlappt, _sollte_ natürlich das Menü im Vordergrund sein, stattdessen
		 * _ist_ aber die 3D-Anzeige im Vordergrund (d.h. das Menü wird dort abgeschnitten, wo die 3D-Anzeige
		 * beginnt). - Abhilfe gibt es zumindest für Menüs und Tooltips:
		 * 
		 * Näheres unter:
		 * <a href="http://java3d.j3d.org/faq/swing.html">Swing FAQ</a>
		 * <a href="http://java3d.j3d.org/tutorials/quick_fix/swing.html">Swing Quick Fix</a>
		 * <a href="http://java.sun.com/products/jfc/tsc/articles/mixing/index.html">Mixing</a>
		 */
    	JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    	ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				controller.closeWorld();
				dispose();
				System.exit(0);
			}
		});

		menuBar = new MainWinMenuBar(controller, this);
        setJMenuBar(menuBar);
		add(menuBar.getToolBar(), BorderLayout.NORTH);
		add(buildStatusBar(), BorderLayout.SOUTH);

		botTabs = new ClosableTabsPane(Config.getIcon("schließen"),
			Config.getIcon("schließen-hover"));
		// Listener wenn jemand auf dem Tab das Schließen-Icon klickt
		botTabs.addCloseListener(new Runnable1<Integer>() {
			public void run(Integer index) {
				BotViewer bv = (BotViewer)botTabs.getComponentAt(index);
				bv.bot.dispose();
			}
		});
		/* Workaround für Java3D Canvas Bug unter Mac OS X 10.11 */
		botTabs.setMinimumSize(new Dimension(230, getHeight()));

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, botTabs, buildWorldAndConsole());
		split.setOneTouchExpandable(true);

		add(split, BorderLayout.CENTER);

		setSize(1000, 800);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * Baut den LogViewer
	 * 
	 * @return LogViewer
	 */
	private ConsoleComponent buildLogViewer() {
    	ConsoleComponent rv = new ConsoleComponent();
    	Debug.registerDebugWindow(rv);	//$$ Legacy: Debug-Klasse
    	// wir melden uns als Handler für den Root-Logger an
    	Handler h = rv.createLoggingHandler();
    	String logLevel = Config.getValue("LogLevel");
    	h.setLevel(Level.parse(logLevel));
    	FmtLogger.getLogger("").addHandler(h);
    	return rv;
    }

	/**
	 * Baut Welt und LogViwer
	 * 
	 * @return Split-Pane
	 */
	private JSplitPane buildWorldAndConsole() {
		JSplitPane rv = new JSplitPane(JSplitPane.VERTICAL_SPLIT, worldViewer, buildLogViewer());
		rv.setResizeWeight(1);
		rv.setOneTouchExpandable(true);
		return rv;
	}

	/**
	 * Baut die Statuszeile
	 * 
	 * @return Statuszeile
	 */
	private StatusBar buildStatusBar() {
		statusBar = new StatusBar(this);
		return statusBar;
	}

	/** Screenshot-Handler */
	public void onScreenshot() {
		try {
	        ImageIO.write(worldViewer.getScreenshot(), "png", File.createTempFile("screenshot", ".png"));
        } catch (IOException e) {
	        e.printStackTrace();
        }
	}

	/**
	 * Vom Controller aufzurufen, wenn sich die Welt ändert; schließt die alte Welt und zeigt die Neue an.
	 * 
	 * @param w	die Welt
	 */
	public void onWorldOpened(final World w) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				closeWorld();
				world = w;
				worldViewer.show(world);
//				validate();
			}
		});
	}

	/** Schließt die Welt */
	protected void closeWorld() {
		if (world == null)
			return;
		world = null;
		worldViewer.show(null);
		statusBar.reinit();
		botTabs.removeAll();
		updateLayout();
		lg.info("Welt geschlossen");
	}

	/**
	 * Schreibt den Parcours in eine Datei
	 * 
	 * @param file	Ausgabedatei
	 */
	protected void writeParcoursToFile(File file) {
		world.writeParcoursToFile(file);
	}

	/**
	 * @param rate	die neue Zeitbasis für den Simulator; in Aufrufen alle xxx ms
	 */
	protected void setTickRate(int rate) {
		try {
			world.setSimStepIntervalInMs(rate);
		} catch (NullPointerException e) {
			// egal
		}
	}

	/** Updated das Layout */
	protected void updateLayout() {
		// kein resize, falls BotViewer vergrößert wurde
		if (split.getSize().width <= split.getPreferredSize().width) {
			split.resetToPreferredSizes();
		}
	}

	/**
	 * Aktualisiert die GUI
	 * 
	 * @param time	die Zeit, die zur Simulatorzeit hinzugezählt wird
	 */
	public void onSimulationStep(final long time) {
		statusBar.updateTime(time);
	}

	/** 
	 * Fügt einen neuen Bot hinzu
	 * 
	 * @param bot	der neue Bot
	 * */
	public void onBotAdded(final Bot bot) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String tabTitle = bot.toString();
				final JComponent tabContent = new BotViewer(bot);
				String tabTitleTooltip = bot.getDescription()+" (Klasse " + bot.getClass().getSimpleName() + ")";
				String keyinfo = " (";
				if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
					keyinfo += "Strg";
				} else if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
					keyinfo += "Strg";
				} else if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
					keyinfo += "Cmd";
				}
				keyinfo += " + e)";
				String tabIconTooltip = (bot instanceof ThreeDBot
					? "Bot löschen" + keyinfo 
					: "Verbindung zu Bot beenden" + keyinfo);
				botTabs.addClosableTab(
					tabTitle,
					tabContent,
					tabTitleTooltip,
					tabIconTooltip);
				// Listener für "Wenn Bot stirbt, Tab entfernen"
				bot.addDisposeListener(new Runnable() {
					public void run() {
						removeBotTab(bot);
						updateLayout();
					}
				});
				botTabs.setSelectedIndex(botTabs.getTabCount()-1);

				updateLayout();

				lg.info("Bot \"" + bot + "\" wurde hinzugefügt.");
			}
		});
	}

	/**
	 * Entfernt einen Bot
	 * 
	 * @param bot	Bot
	 */
	private void removeBotTab(final Bot bot) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i = 0; i < botTabs.getTabCount(); i++) {
					BotViewer bv = (BotViewer)botTabs.getComponentAt(i);
					if (bot == bv.bot) {
						botTabs.remove(i);
					}
				}
				if (world != null) {
					if (bot instanceof ThreeDBot) {
						world.deleteBot(((ThreeDBot)bot).getSimBot());
					}
				}
			}
		});
	}

	/**
	 * @see ctSim.view.View#onApplicationInited()
	 */
	public void onApplicationInited() {
		// No-op
    }

	/**
	 * @see ctSim.view.View#onSimulationFinished()
	 */
	public void onSimulationFinished() {
		// No-op
	}

	/**
	 * @see ctSim.view.View#onJudgeSet(ctSim.model.rules.Judge)
	 */
	public void onJudgeSet(final Judge judge) {

//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				menuBar.onJudgeSet(judge);
//			}
//		});

    }
	
	/**
	 * Veranlasst einen Reset aller Bots
	 * 
	 * Die Bots werden über ihren BotViewer gefunden. TCP-Bots (simuliert und real) erhalten das
	 * Reset-Signal per RC5-Code. Anschließend werden alle Bots in der Welt zurück auf ihre Startplätze
	 * gesetzt. 
	 */
	public void onResetAllBots() {
		/* Bots das Reset-Signal schicken */
		for (int i=0; i<botTabs.getTabCount(); i++) {
			// BotViewer des Tabs holen...
			BotViewer bv = (BotViewer)botTabs.getComponentAt(i);
			// ...dann Bot zum BotViewer verwenden
			Bot bot = bv.bot;
			if (bot instanceof ThreeDBot) {
				/* Sim-Bots das Reset-Signal senden */
				Bot b = ((ThreeDBot) bot).getSimBot();
				if (b instanceof CtBotSimTcp) {
					((CtBotSimTcp) b).sendRC5Code("CH*P/P");
				}
				
			} else if (bot instanceof RealCtBot) {
				/* Realen Bots das Reset-Signal senden */
				((RealCtBot) bot).sendRC5Code("CH*P/P");
			}

		}
		// alle Bots in der Welt auf die Startplätze zurück
		world.resetAllBots();
	}
}
