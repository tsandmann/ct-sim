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

package ctSim.view.gui;

import java.awt.BorderLayout;
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
import ctSim.model.rules.Judge;
import ctSim.util.ClosableTabsPane;
import ctSim.util.FmtLogger;
import ctSim.util.Runnable1;

//$$$ BotViewer zu klein, scheint nur Breite von String Botname zu haben
//$$ Wenn man BotViewer groesser gezogen hat und noch einen Bot hinzufuegt, springt BotViewer wieder auf Standardgroesse
//LODO Crasht, wenn Icon-Dateien nicht da
//$$ doc
/**
 * Die GUI-Hauptklasse fuer den c't-Sim
 *
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class MainWindow extends JFrame implements ctSim.view.View {
    private static final long serialVersionUID = 3689470428407624063L;
    final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui");

	//////////////////////////////////////////////////////////////////////
	// GUI-Components:
	private StatusBar statusBar;

	private JSplitPane split;

	/**
	 * <p>
	 * Enth&auml;lt ein Tab pro Bot. Wenn einer am Tab das Schlie&szlig;en-Icon
	 * (&quot;X&quot;) klickt, gilt das Prinzip &quot;zust&auml;ndig f&uuml;r
	 * Bot entfernen ist der Bot, sonst niemand&quot;. Das f&uuml;hrt zu einem
	 * nicht ganz offensichtlichen Ablauf:
	 * <ol>
	 * <li>ClosableTabsPane ruft den CloseListener auf, den MainWindow bei der
	 * ClosableTabsPane angemeldet hat</li>
	 * <li>CloseListener sagt Bot &quot;verkr&uuml;mel dich&quot; ({@code dispose()}),
	 * tut aber sonst nichts &ndash; entfernt keine Tabs oder so </li>
	 * <li>Bot geht seine Deinitialisierungs-Routine durch</li>
	 * <li>Bot ruft seine DisposeListener auf. Einer davon ist der, den
	 * MainWindow beim Bot angemeldet hat: Dieser sagt schlie&szlig;lich dem Tab
	 * &quot;zeig dich nicht mehr an"</li>
	 * </ol>
	 * </p>
	 * <p>
	 * Grund: Der CloseListener <em>k&ouml;nnte</em> auch direkt das Tab
	 * entfernen. Allerdings muss der DisposeListener in jedem Fall das Tab
	 * entfernen, schon weil das Bot.dispose() ja von woanders ausgel&ouml;st
	 * werden kann. Daher kommt es zu Problemen, wenn nach einem
	 * "Schlie&szlig;en"-Klick der CloseListener ein Tab entfernt und der
	 * DisposeListener auch eins. Deswegen die klare Regelung, dass nur im
	 * DisposeListener Tabs entfernt werden.
	 * </p>
	 */
	private final ClosableTabsPane botTabs;

	//////////////////////////////////////////////////////////////////////
	private World world;

	private WorldViewer worldViewer = new WorldViewer();

	private MainWinMenuBar menuBar;

	public MainWindow(final Controller controller) {
		super("c't-Sim " + Main.VERSION);

    	/*
		 * Riesenspass: Swing (also auch unsere GUI) ist "lightweight", Java3D
		 * ist jedoch "heavyweight". Daher erscheint die Java3D-Anzeige immer
		 * _vor_ allen GUI-Komponenten: Wenn z.B. ein Menue aufgeklappt wird,
		 * das die 3D-Anzeige ueberlappt, _sollte_ natuerlich das Menue im
		 * Vordergrund sein, es _ist_ aber die 3D-Anzeige im Vordergrund (d.h.
		 * das Menue wird abgeschnitten, wo die 3D-Anzeige beginnt). -- Abhilfe
		 * wenigstens fuer Menues und Tooltips: folgende zwei Aufrufe. --
		 * Naeheres: http://java3d.j3d.org/faq/swing.html
		 * http://java3d.j3d.org/tutorials/quick_fix/swing.html
		 * http://java.sun.com/products/jfc/tsc/articles/mixing/index.html
		 */
    	JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    	ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@SuppressWarnings("synthetic-access")
            @Override
			public void windowClosing(
					@SuppressWarnings("unused") WindowEvent e) {
				controller.closeWorld();
				dispose();
				System.exit(0);
			}
		});

		menuBar = new MainWinMenuBar(controller, this);
        setJMenuBar(menuBar);
		add(menuBar.getToolBar(), BorderLayout.NORTH);
		add(buildStatusBar(), BorderLayout.SOUTH);

		botTabs = new ClosableTabsPane(Config.getIcon("schliessen"),
			Config.getIcon("schliessen-hover"));
		// Listener wenn einer aufm Tab das Schliessen-Icon klickt
		botTabs.addCloseListener(new Runnable1<Integer>() {
			@SuppressWarnings("synthetic-access")
			public void run(Integer index) {
				BotViewer bv = (BotViewer)botTabs.getComponentAt(index);
				bv.bot.dispose();
			}
		});

		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			botTabs, buildWorldAndConsole());
		split.setOneTouchExpandable(true);

		add(split, BorderLayout.CENTER);

		setSize(1000, 800);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private ConsoleComponent buildLogViewer() {
    	ConsoleComponent rv = new ConsoleComponent();
    	Debug.registerDebugWindow(rv); //$$ Legacy: Debug-Klasse
    	// Wir melden uns als Handler fuer den Root-Logger an
    	Handler h = rv.createLoggingHandler();
    	String logLevel = Config.getValue("LogLevel");
    	h.setLevel(Level.parse(logLevel));
    	FmtLogger.getLogger("").addHandler(h);
    	return rv;
    }

	private JSplitPane buildWorldAndConsole() {
		JSplitPane rv = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT, worldViewer, buildLogViewer());
		rv.setResizeWeight(1);
		rv.setOneTouchExpandable(true);
		return rv;
	}

	private StatusBar buildStatusBar() {
		statusBar = new StatusBar(this);
		return statusBar;
	}

	//$$ Methode vorlaeufig; weiss nicht, ob die im Endeffekt drin sein soll
	public void onScreenshot() {
		try {
	        ImageIO.write(worldViewer.getScreenshot(), "png",
	        	File.createTempFile("screenshot", ".png"));
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
	}

	/**
	 * Vom Controller aufzurufen, wenn sich die Welt &auml;ndert. Schlie&szlig;t
	 * die alte Welt und zeigt die neue an.
	 */
	public void onWorldOpened(final World w) {
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				closeWorld();
				world = w;
				worldViewer.show(world);
				validate(); //$$ validate() noetig?
			}
		});
	}

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

	protected void writeParcoursToFile(File file) {
		world.writeParcoursToFile(file);
	}

	/**
	 * @param rate Die neue Zeitbasis fuer den Simulator in Aufrufen alle xxx ms
	 */
	protected void setTickRate(int rate) {
		world.setSimStepIntervalInMs(rate);
	}

	protected void updateLayout() {
		split.resetToPreferredSizes();
	}

	/**
	 * Aktualisiert die GUI
	 * @param time Die Zeit, die zur Simulatorzeit hinzugezaehlt wird
	 */
	public void onSimulationStep(final long time) {
		// TODO: alles ganz haesslich:
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				statusBar.updateTime(time);
			}
		});
	}

	/** F&uuml;gt einen neuen Bot hinzu */
	public void onBotAdded(final Bot bot) {
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				String tabTitle = bot.toString();
				JComponent tabContent = new BotViewer(bot);
				String tabTitleTooltip = bot.getDescription()+" (Klasse "+
					bot.getClass().getSimpleName()+")";
				String tabIconTooltip = (bot instanceof ThreeDBot
					? "Bot l\u00F6schen"
					: "Verbindung zu Bot beenden");
				botTabs.addClosableTab(
					tabTitle,
					tabContent,
					tabTitleTooltip,
					tabIconTooltip);
				// Listener fuer "Wenn Bot stirbt, Tab weg"
				bot.addDisposeListener(new Runnable() {
					@SuppressWarnings("synthetic-access")
					public void run() {
						removeBotTab(bot);
						updateLayout();
					}
				});
				botTabs.setSelectedIndex(botTabs.getTabCount()-1);

				updateLayout();

				Debug.out.println("Bot \""+bot+"\" wurde hinzugefuegt."); //$$$ Debug msg
			}
		});
	}

	private void removeBotTab(final Bot bot) {
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				for (int i = 0; i < botTabs.getTabCount(); i++) {
					BotViewer bv = (BotViewer)botTabs.getComponentAt(i);
					if (bot == bv.bot)
						botTabs.remove(i);
				}
				if (world != null) {
					world.deleteBot(((ThreeDBot)bot).getSimBot());
				}
			}
		});
	}

	public void onApplicationInited() {
	    //TODO Schoen waere: Bis hierhin Splashscreen anzeigen mit Konsole und sonst nur einen Unendlich-Prozentbalken. Ab dem Eintritt in diese Methode dann das Hauptfenster sichtbar machen.
    }

	public void onSimulationFinished() {
	    //TODO Ueber diese Methode kriegt das MainWindow mit, wenn die Simulation anhaelt. Schoen waere: Knoepfe fuer Play/Pause/Stop ausgrauen, wenn nicht bedienbar. (Man kann nicht was stoppen, was schon gestoppt ist; starten, was schon gestartet ist; usw.)
    }

	public void onJudgeSet(final Judge judge) {	
//		SwingUtilities.invokeLater(new Runnable() {
//			@SuppressWarnings("synthetic-access")
//			public void run() {
//				menuBar.onJudgeSet(judge);
//			}
//		});
    }
}
