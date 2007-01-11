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

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;

import ctSim.controller.BotManager;
import ctSim.controller.Controller;
import ctSim.controller.Main;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;
import ctSim.util.ClosableTabsPane;
import ctSim.util.Closure;
import ctSim.util.FmtLogger;
import ctSim.util.IconHashMap;

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
	// TODO
	private StatusBar statusBar;

	// TODO: Weg!?
	private JSplitPane split;

	private final ClosableTabsPane botTabs;

	//////////////////////////////////////////////////////////////////////
	private World world;

	private WorldViewer worldViewer = new WorldViewer();

	private MainWinMenuBar menuBar; //$$ Nach Judge-Umbau: Kann lokale Var werden

	public MainWindow(final Controller controller) {
		super("c't-Sim " + Main.VERSION);

		IconHashMap icons;
		try {
	        icons = new IconHashMap(new File("images")); //LODO Pfad hardcoded
        } catch (Exception e) {
			throw new AssertionError(e);
        }

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
				controller.stop();
				dispose();
				System.exit(0);
			}
		});

		menuBar = new MainWinMenuBar(controller, this, icons);
        setJMenuBar(menuBar);
		add(menuBar.getToolBar(), BorderLayout.NORTH);
		add(buildStatusBar(), BorderLayout.SOUTH);

		botTabs = new ClosableTabsPane(icons.get("schliessen"),
			icons.get("schliessen-hover"));
		botTabs.addCloseListener(new Closure<Integer>() {
			@SuppressWarnings("synthetic-access")
			public void run(Integer index) {
				BotViewer bv = (BotViewer)botTabs.getComponentAt(index);
				BotManager.removeBotOnNextSimStep(bv.bot);
			}
		});

		//$$$ SplitPane laesst sich nicht umbegroessen
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
    	//$$$ Temporaer auskommentiert, da terminate-Bug damit zusammenhing (sehr seltsame Abhaengigkeit)
    	// Wir melden uns als Handler fuer den Root-Logger an;
//    	Handler h = rv.new LoggingHandler();
//    	h.setLevel(INFO);
//    	FmtLogger.getLogger("").addHandler(h);
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
	public void onWorldOpened(World w) {
		closeWorld();
		world = w;
		worldViewer.show(world);
		validate(); //$$ validate() noetig?
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

	protected void reset() {
		statusBar.reinit();
		botTabs.removeAll();

		updateLayout();

		//Ausgabe macht nur Sinn, wenn ueberhaupt ein Bot da ist.
		if (BotManager.getSize() > 1)
			Debug.out.println("Alle Bots entfernt.");
	}

	/**
	 * @param rate Die neue Zeitbasis fuer den Simulator in Aufrufen alle xxx ms
	 */
	protected void setTickRate(int rate) {
		world.setSimStepIntervalInMs(rate);
	}

	public void updateLayout() {
		split.resetToPreferredSizes();
	}

	/** Aktualisiert die GUI */
	public void update() {
		for (int i = 0; i < botTabs.getTabCount(); i++)
			((Updatable)botTabs.getComponentAt(i)).update();
		worldViewer.update();
	}

	/**
	 * Aktualisiert die GUI
	 * @param time Die Zeit, die zur Simulatorzeit hinzugezaehlt wird
	 */
	public void onSimulationStep(long time) {
		// TODO: alles ganz haesslich:
		statusBar.updateTime(time);
		update();
	}

	/** F&uuml;gt einen neuen Bot hinzu */
	public void onBotAdded(final Bot bot) {
		botTabs.addClosableTab(bot.getName(), new BotViewer(bot),
			"Bot '"+bot.getName()+
			"' vom Typ '"+bot.getClass().getSimpleName()+"'");
		// Listener fuer "Wenn Bot stirbt, Tab weg"
		bot.addDeathListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() { //$$$ SplitPane klappt nicht zusammen, wenn letzter Bot entfernt
				/*
				 * Zugehoerigen Tab finden und entfernen -- Nicht "optimieren":
				 * Wir muessen in diesem Listener die Tabs durchsuchen. Sich den
				 * Index des Tabs beim Bot-hinzufuegen zu merken geht nicht
				 * (Index wenn Bot hinzugefuegt moeglicherweise ungleich Index
				 * wenn Bot stirbt)
				 */
				for (int i = 0; i < botTabs.getTabCount(); i++) {
					if (bot == ((BotViewer)botTabs.getComponentAt(i)).bot)
						botTabs.remove(i);
				}
			}
		});

		update();
		updateLayout();

		Debug.out.println("Bot \""+bot.getName()+"\" wurde hinzugefuegt.");
	}

	public void onBotRemoved(Bot bot) {
		update();
		updateLayout();

		Debug.out.println("Bot \""+bot.getName()+"\" wurde gel\uu00F6scht.");
	}

	public void onApplicationInited() {
	    //TODO Schoen waere: Bis hierhin Splashscreen anzeigen mit Konsole und sonst nur einen Unendlich-Prozentbalken. Ab dem Eintritt in diese Methode dann das Hauptfenster sichtbar machen.
    }

	public void onSimulationFinished() {
	    // TODO Ueber diese Methode kriegt das MainWindow mit, wenn die Simulation anhaelt. Schoen waere: Knoepfe fuer Play/Pause/Stop ausgrauen, wenn nicht bedienbar. (Man kann nicht was stoppen, was schon gestoppt ist; starten, was schon gestartet ist; usw.)
    }

	public void onJudgeSet(Judge judge) {
		menuBar.onJudgeSet(judge);
    }
}