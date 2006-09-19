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

import static java.util.logging.Level.INFO;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Handler;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import ctSim.ConfigManager;
import ctSim.controller.BotManager;
import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.util.FmtLogger;
import ctSim.util.IconHashMap;

/**
 * Die GUI-Hauptklasse fuer den c't-Sim
 *
 * @author Felix Beckwermert
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class CtSimFrame extends JFrame implements ctSim.view.View {
    private static final long serialVersionUID = 3689470428407624063L;
    private FmtLogger lg;

    private IconHashMap icons;
	private static final String[] judgeClassNames = { //LODO Judges hardcoded
		"ctSim.model.rules.DefaultJudge",
		"ctSim.model.rules.LabyrinthJudge"};

	//////////////////////////////////////////////////////////////////////
	// GUI-Components:
	// TODO
	private StatusBar statusBar;

	// TODO: Weg!?
	private JSplitPane split, consoleSplit;

	private ControlBar controlBar;
	private WorldPanel worldPanel;

	private JFileChooser worldChooser;

	//////////////////////////////////////////////////////////////////////
	private World world;
	private Controller controller;
	private JMenu worldMenu;
	private JMenu simulationMenu;


	private ConsoleComponent console;

	private JFileChooser botChooser;

	private void initLogging() {
		console = new ConsoleComponent();
		Debug.registerDebugWindow(console); //$$ Legacy
		lg = FmtLogger.getLogger("ctSim.view.gui");
		// Wir melden uns als Handler fuer den Root-Logger an;
		Handler h = console.new LoggingHandler();
		h.setLevel(INFO);
		FmtLogger.getLogger("").addHandler(h);
	}

	/**
	 * Der Konstruktor
	 * @param title Die Titelzeile des Fensters
	 */
	public CtSimFrame(Controller c, String title) {
		super(title);
		this.controller = c;
		initLogging();

		try {
	        icons = new IconHashMap(new File("images")); //LODO Pfad hardcoded
        } catch (Exception e) {
        	lg.warning(e, "Problem beim Laden der Icons:");
        }

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

		worldChooser = new JFileChooser(ConfigManager.getValue("worlddir"));
		worldChooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().endsWith(".xml"));
			}

			@Override
			public String getDescription() {
				return "Parcours-Dateien (*.xml)";
			}});
		botChooser = new JFileChooser(ConfigManager.path2Os(
				ConfigManager.getValue("botdir")));
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

		// Prinzip: Menue machen; auf dessen Basis dann Toolbar, die
		// einige der Menues widerspiegelt
		try {
	        setJMenuBar(buildMenuBar());
        } catch (Exception e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
		add(buildToolBar(worldMenu, simulationMenu), BorderLayout.NORTH);
		add(buildStatusBar(), BorderLayout.SOUTH);
		initControlBar();
		initWorldView();

		this.consoleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		this.consoleSplit.setResizeWeight(1);
		this.consoleSplit.setOneTouchExpandable(true);
		this.consoleSplit.setTopComponent(this.worldPanel);
		this.consoleSplit.setBottomComponent(console);

		this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.split.setLeftComponent(this.controlBar);
		this.split.setRightComponent(this.consoleSplit);
		this.split.setDividerLocation(0);
		this.split.setOneTouchExpandable(true);

		add(split, BorderLayout.CENTER);

		setSize(1000, 800);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private JMenuBar buildMenuBar()
    throws SecurityException, NoSuchMethodException {
    	JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    	JMenuBar rv = new JMenuBar();
    	worldMenu =
    		buildMenu("Welt",
    			buildMenuItem("\u00D6ffnen ...",
    					icons.get("Open16"), "onOpenWorld"),
    			buildMenuItem("Generieren",
    					icons.get("New16"), "onRandomWorld"),
    			buildMenuItem("Speichern als ...",
    					icons.get("SaveAs16"), "onSaveWorld"),
    			buildMenuItem("Schlie\u00DFen",
    					icons.get("Delete16"), "onCloseWorld"));
    	rv.add(worldMenu);
        rv.add(
        	buildMenu("Bot hinzuf\u00FCgen",
        		buildMenuItem("Testbot", "onAddTestbot"),
        		buildMenuItem("C-Bot ...", "onAddCBot")));
        rv.add(buildMenu("Schiedsrichter", buildJudgeMenuItems())); //$$ Das sollte immer bei Klick aufs Menue gemacht werden
        simulationMenu =
        	buildMenu("Simulation",
        		buildMenuItem("Start", icons.get("Play16"), "startWorld"),
        		buildMenuItem("Stop", icons.get("Stop16"), "resetWorld"),
        		buildMenuItem("Pause", icons.get("Pause16"), "onPause"));
        rv.add(simulationMenu);
    	return rv;
    }

	private JMenu buildMenu(String title, JMenuItem... items) {
    	JMenu rv = new JMenu(title);
    	for (JMenuItem it : items)
    		rv.add(it);
    	return rv;
    }

	private JMenuItem buildMenuItem(String label, String targetMethodName)
	throws SecurityException, NoSuchMethodException {
		return buildMenuItem(label, null, targetMethodName);
	}

	private JMenuItem buildMenuItem(String label, Icon icon,
			String targetMethodName)
	throws SecurityException, NoSuchMethodException {
		final CtSimFrame self = this;
		final Method targetMethod =
			getClass().getMethod(targetMethodName, new Class[] {});
		return new JMenuItem(new AbstractAction(label, icon) {
            private static final long serialVersionUID = -2329833776949451651L;

			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
					@SuppressWarnings("unused") ActionEvent evt) {
				try {
					targetMethod.invoke(self, new Object[] {});
                } catch (Exception e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
			}});
	}

	private JMenuItem[] buildJudgeMenuItems() {
		JMenuItem[] rv = new JMenuItem[judgeClassNames.length];
		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i < judgeClassNames.length; i++) {
			final String fullName = judgeClassNames[i];
			String simpleName = fullName.replaceAll("^.*\\.(.*)$", "$1");
			rv[i] = new JRadioButtonMenuItem(new AbstractAction(simpleName) {
                private static final long serialVersionUID = -1873920690635293756L;

				@SuppressWarnings("synthetic-access")
                public void actionPerformed(
                		@SuppressWarnings("unused") ActionEvent e) {
					controller.setJudge(fullName);
				}});
			bg.add(rv[i]);
			if (fullName.equals(controller.getJudge()))
				bg.setSelected(rv[i].getModel(), true);
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

	private StatusBar buildStatusBar() {
		statusBar = new StatusBar(this);
		return statusBar;
	}

	private void initWorldView() {

		// TODO:
		// Initialize WorldViewPanel
		this.worldPanel = new WorldPanel();
	}

	private void initControlBar() {

		// Initialize ControlBarPanel
		this.controlBar = new ControlBar();
	}

	public void onRandomWorld() {
    	controller.openRandomWorld();
    }

	public void onCloseWorld() {
    	controller.closeWorld();
    	closeWorld();
    }

	public void onOpenWorld() {
		if (worldChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File f = worldChooser.getSelectedFile();
			if (! f.exists() && ! f.getName().endsWith(".xml"))
				f = new File(f.getPath() + ".xml");
			controller.openWorldFromFile(f);
		}
	}

	public void onSaveWorld() {
		if (worldChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = this.worldChooser.getSelectedFile();
			if(! file.getAbsolutePath().endsWith(".xml")) {
				file = new File(file.getPath() + ".xml");
			}
			if(file.exists()) {
				if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
						this, "Die Datei '"+file.getName()+"' existiert " +
						"bereits. Soll sie \u00FCberschrieben werden?",
						"\00DCberschreiben?",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE))
					return;
			}
			world.writeParcoursToFile(file);
		}
	}

	public void onPause() {
    	controller.pause();
    }

	public void onAddTestbot() {
		controller.addTestBot();
	}

	public void onAddCBot() {
		if(botChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			controller.invokeBot(botChooser.getSelectedFile());
	}

	// TODO: Geschwindigkeit setzen:
	public void startWorld() {

		//this.world.setHaveABreak(false);
		this.controller.unpause();
	}

	public void resetWorld() {

		this.controller.reset();

		this.statusBar.reinit();
		this.controlBar.reinit();

		updateLayout();

		//Ausgabe macht nur Sinn, wenn überhaupt ein Bot da ist.
		if (BotManager.getBots().size() > 1)
			Debug.out.println("Alle Bots entfernt.");
	}

	/** Vom Controller aufzurufen, wenn sich die Welt &auml;ndert.
	 * Schlie&szlig;t die alte Welt und zeigt die neue an.*/
	public void openWorld(World w) {
		this.closeWorld();
		this.world = w;
		// TODO: [Was ist mit dem leeren Todo hier gemeint? --hkr]
		this.worldPanel.setWorld(this.world);
		this.validate();
	}

	// TODO: Close Controller: [Was bedeutet dieses Todo? --hkr]
	public void closeWorld() {
		if(this.world == null)
			return;

		// TODO: ganz haesslich! [Was ist haesslich? --hkr]
		//this.split.remove(this.worldPanel);
		this.consoleSplit.remove(this.worldPanel);
		this.world = null;
		this.worldPanel = null;
		initWorldView();
		this.statusBar.reinit();
		this.controlBar.reinit();
		this.consoleSplit.setTopComponent(this.worldPanel);
		this.updateLayout();

		Debug.out.println("Welt wurde geschlossen.");
	}

	/**
	 * @param rate Die neue Zeitbasis fuer den Simulator in Aufrufen alle xxx ms
	 */
	protected void setTickRate(int rate) {

		world.setSimStepIntervalInMs(rate);
	}

	public void updateLayout() {

		this.split.resetToPreferredSizes();
	}

	/**
	 * Aktualisiert die GUI
	 */
	public void update() {

		// TODO: Groesse sichern...
		//this.setPreferredSize(this.getSize());

		//this.setVisible(false);
		//this.pack();
		//this.setVisible(true);

		// --> this.validate();

		this.controlBar.update();

		this.worldPanel.update();
	}

	/**
	 * Aktualisiert die GUI
	 * @param time Die Zeit, die zur Simulatorzeit hinzugezaehlt wird
	 */
	public void update(long time) {

		// TODO: alles ganz haesslich:
		this.statusBar.updateTime(time);
		this.update();
	}

	/**
	 * Fuegt einen neuen Bot hinzu
	 */
	public void addBot(Bot bot) {
		this.controlBar.addBot(bot);

		this.update();
		this.updateLayout();

		Debug.out.println("Bot \""+bot.getName()+
				"\" wurde hinzugefuegt.");
	}

	public void removeBot(Bot bot) {
		this.controlBar.removeBot(bot);
		this.update();
		updateLayout();

		Debug.out.println("Bot \""+bot.getName()+"\" wurde gelöscht.");
	}

	public void onApplicationInited() {
	    //TODO Schoen waere: Splashscreen anzeigen mit Konsole und sonst nur einen Unendlich-Prozentbalken. Ab dem Eintritt in diese Methode dann das Hauptfenster sichtbar machen.
    }
}
