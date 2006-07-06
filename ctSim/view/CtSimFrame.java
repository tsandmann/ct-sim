/**
 * 
 */
package ctSim.view;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;

import org.xml.sax.SAXException;

import ctSim.controller.Controller;
import ctSim.model.ParcoursGenerator;
import ctSim.model.World;
import ctSim.model.bots.Bot;

/**
 * @author FelixB
 *
 */
public class CtSimFrame extends JFrame {
	
	//////////////////////////////////////////////////////////////////////
	// Icons:
	private MediaTracker tracker;
	private Image openImg, closeImg, randomImg, saveImg;
	private Image stopImg, pauseImg, playImg; // refreshImg;
	//private Image zoomInImg, zoomOutImg, zoomImg;
	//private Image leftImg, rightImg, downImg, upImg; // impImg, expImg;
	/*
	private Image knubbelImg, kastenImg, wandImg, sichtImg, startImg, endeImg,
			pfadImg, leerImg;
			*/
	
	//////////////////////////////////////////////////////////////////////
	// Actions:
	private Action openWorld, randomWorld, closeWorld, saveWorld;
	private Action selectJudge, addBot, configBots;
	private Action start, stop, pause;
	
//	private Action zoomIn, zoomOut;
//	private Action rotLeft, rotRight, rotUp, rotDown;
//	private Action toggleStrafe; // <-- ???
//	private Action strafeUp, strafeDown, strafeLeft, strafeRight;
	
	
	
	//////////////////////////////////////////////////////////////////////
	// GUI-Components:
	private JMenuBar menuBar;
	private JToolBar toolBar;
	
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
	
	private final String TMP_PARCOURS_PATH = "tmp";
	private final String TMP_PARCOURS_FILE_NAME = "tmpParcoursFile";
	private File tmpParcoursFile;
	
	
	public CtSimFrame(String title) {
		
		// TODO: Titel setzen (?)
		super(title);
		
		loadImages();
		
		// TODO:
		//this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			
			public void windowClosing(WindowEvent e) {
				
				cmdExitClicked();
			}
		});
		
		this.worldChooser = new JFileChooser(".");
		this.worldChooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().endsWith(".xml"));
			}

			@Override
			public String getDescription() {
				return "World-Files (*.xml)";
			}
		});
		
		initActions();
		initMenuBar();
		initToolBar();
		initStatusBar();
		initControlBar();
		initWorldView();
		
		this.setJMenuBar(this.menuBar);
		this.add(this.toolBar, BorderLayout.PAGE_START);
		this.add(this.statusBar, BorderLayout.SOUTH);
		
		Console console = new Console();
		Debug.registerDebugWindow(console);
		
		this.consoleSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		consoleSplit.setResizeWeight(1.0);
		consoleSplit.setOneTouchExpandable(true);
		consoleSplit.setTopComponent(this.worldPanel);
		consoleSplit.setBottomComponent(console);
		
		//JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		split.setLeftComponent(this.controlBar);
		//split.setRightComponent(this.worldPanel);
		split.setRightComponent(consoleSplit);
		split.setDividerLocation(0);
		//split.setContinuousLayout(true);
		//split.setOneTouchExpandable(true);
		
		this.add(split, BorderLayout.CENTER);
		
		this.setPreferredSize(new Dimension(800, 650));
		//this.setMinimumSize(new Dimension(900, 650));
		
		this.pack();
		//this.show();
		//this.setVisible(true);
	}
	
	private void loadImages() {
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Toolkit tk = Toolkit.getDefaultToolkit();

		openImg    = tk.getImage(cl.getResource("images/Open16.gif"));
		closeImg   = tk.getImage(cl.getResource("images/Delete16.gif"));
		saveImg    = tk.getImage(cl.getResource("images/SaveAs16.gif"));
		randomImg  = tk.getImage(cl.getResource("images/New16.gif"));
		
		//refreshImg = tk.getImage(cl.getResource("images/Refresh16.gif"));
		stopImg    = tk.getImage(cl.getResource("images/Stop16.gif"));
		pauseImg   = tk.getImage(cl.getResource("images/Pause16.gif"));
		playImg    = tk.getImage(cl.getResource("images/Play16.gif"));
		
		/*
		zoomInImg  = tk.getImage(cl.getResource("images/ZoomIn16.gif"));
		zoomOutImg = tk.getImage(cl.getResource("images/ZoomOut16.gif"));
		zoomImg    = tk.getImage(cl.getResource("images/Zoom16.gif"));
		
		leftImg    = tk.getImage(cl.getResource("images/Back16.gif"));
		rightImg   = tk.getImage(cl.getResource("images/Forward16.gif"));
		downImg    = tk.getImage(cl.getResource("images/Down16.gif"));
		upImg      = tk.getImage(cl.getResource("images/Up16.gif"));
		*/
		
		//impImg = tk.getImage(cl.getResource("images/Import16.gif"));
		//expImg = tk.getImage(cl.getResource("images/Export16.gif"));
		
		/*
		knubbelImg = tk.getImage(cl.getResource("knubbel32.png"));
		kastenImg = tk.getImage(cl.getResource("kasten32.png"));
		wandImg = tk.getImage(cl.getResource("wand32.png"));
		sichtImg = tk.getImage(cl.getResource("sicht32.png"));
		leerImg = tk.getImage(cl.getResource("leer32.png"));
		startImg = tk.getImage(cl.getResource("start32.png"));
		endeImg = tk.getImage(cl.getResource("ende32.png"));
		pfadImg = tk.getImage(cl.getResource("pfad32.png"));
		*/

		tracker = new MediaTracker(this);
		tracker.addImage(openImg, 0);
		tracker.addImage(closeImg, 0);
		tracker.addImage(randomImg, 0);
		tracker.addImage(saveImg, 0);
		
		//tracker.addImage(refreshImg, 0);
		tracker.addImage(stopImg, 0);
		tracker.addImage(pauseImg, 0);
		tracker.addImage(playImg, 0);
		/*
		tracker.addImage(zoomInImg, 0);
		tracker.addImage(zoomOutImg, 0);
		tracker.addImage(zoomImg, 0);
		
		tracker.addImage(leftImg, 0);
		tracker.addImage(rightImg, 0);
		tracker.addImage(downImg, 0);
		tracker.addImage(upImg, 0);
		*/
		//tracker.addImage(impImg, 0);
		//tracker.addImage(expImg, 0);
		
		/*
		tracker.addImage(knubbelImg, 0);
		tracker.addImage(kastenImg, 0);
		tracker.addImage(wandImg, 0);
		tracker.addImage(sichtImg, 0);
		tracker.addImage(leerImg, 0);
		tracker.addImage(startImg, 0);
		tracker.addImage(endeImg, 0);
		tracker.addImage(pfadImg, 0);
		*/
	}
	
	private void initActions() {
		
		try {
			tracker.waitForAll();
		} catch (InterruptedException e) {
			System.err.println("Interrupted!");
		}
		
		// TODO:
		// Initialize Actions for MenuBar and ToolBar
		// - Icons adden
		
		//////////////////////////////////////////////////////////////////////
		// World-Actions
		this.openWorld = new AbstractAction("Öffnen...",
				new ImageIcon(this.openImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdOpenWorldClicked();
			}
		};
		this.randomWorld = new AbstractAction("Generieren...",
				new ImageIcon(this.randomImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdRandomWorldClicked();
			}
		};
		this.closeWorld = new AbstractAction("Schließen",
				new ImageIcon(this.closeImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdCloseWorldClicked();
			}
		};
		this.saveWorld = new AbstractAction("Speichern als...",
				new ImageIcon(this.saveImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdSaveWorldClicked();
			}
		};
		
		//////////////////////////////////////////////////////////////////////
		// Bot-Actions
		this.selectJudge = new AbstractAction("Schiedsrichter wählen...") {
			
			public void actionPerformed(ActionEvent e) {
				
				cmdSetJudgeClicked();
			}
		};
		this.addBot = new AbstractAction("Bot hinzufügen...") {

			public void actionPerformed(ActionEvent e) {
				
				cmdAddBotClicked();
			}
		};
		this.configBots = new AbstractAction("Bots konfigurieren...") {

			public void actionPerformed(ActionEvent e) {
				
				cmdConfigureBotsClicked();
			}
		};
		
		//////////////////////////////////////////////////////////////////////
		// Control-Options
		this.start = new AbstractAction("Start",
				new ImageIcon(this.playImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdStartClicked();
			}
		};
		this.stop = new AbstractAction("Stop",
				new ImageIcon(this.stopImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdStopClicked();
			}
		};
		this.pause = new AbstractAction("Pause",
				new ImageIcon(this.pauseImg)) {

			public void actionPerformed(ActionEvent e) {
				
				cmdPauseClicked();
			}
		};
		
//		this.zoomIn;
//		this.zoomOut;
//		this.rotLeft;
//		this.rotRight;
//		this.rotUp;
//		this.rotDown;
//		this.toggleStrafe; // <-- ???
//		this.strafeUp;
//		this.strafeDown;
//		this.strafeLeft;
//		this.strafeRight;
	}
	
	private void initMenuBar() {
		
		// TODO:
		// Set GUI-Elements for the Menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		// for tooltips:
		//ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
		
		this.menuBar = new JMenuBar();
		
		JMenu worldMenu = new JMenu("Welt");
		//worldMenu.getPopupMenu().setLightWeightPopupEnabled(false);
		worldMenu.add(this.openWorld);
		worldMenu.add(this.randomWorld);
		worldMenu.add(this.closeWorld);
		worldMenu.add(this.saveWorld);
		
		JMenu botMenu = new JMenu("Bearbeiten");
		//botMenu.getPopupMenu().setLightWeightPopupEnabled(false);
		botMenu.add(this.selectJudge);
		botMenu.addSeparator();
		botMenu.add(this.addBot);
		botMenu.add(this.configBots);
		
		JMenu controlMenu = new JMenu("Ansicht");
		//controlMenu.getPopupMenu().setLightWeightPopupEnabled(false);
		controlMenu.add(this.start);
		controlMenu.add(this.pause);
		controlMenu.add(this.stop);
		
		this.menuBar = new JMenuBar();
		this.menuBar.add(worldMenu);
		this.menuBar.add(botMenu);
		this.menuBar.add(controlMenu);
		
		//this.setJMenuBar(this.menuBar);
	}
	
	private void initToolBar() {
		
		// TODO:
		// Set GUI-Elements for the ToolBar
		
		this.toolBar = new JToolBar();
		
		this.toolBar.add(this.openWorld);
		this.toolBar.add(this.randomWorld);
		this.toolBar.add(this.closeWorld);
		this.toolBar.add(this.saveWorld);
		
		this.toolBar.addSeparator(); // <-- mit Dim (?)
		this.toolBar.add(this.stop);
		this.toolBar.add(this.pause);
		this.toolBar.add(this.start);
	}
	
	private void initStatusBar() {
		
		// TODO:
		// Set GUI-Elements for the StatusBar
		this.statusBar = new StatusBar(this);
	}
	
	private void initWorldView() {
		
		// TODO:
		// Initialize WorldViewPanel
		this.worldPanel = new WorldPanel();
	}
	
	private void initControlBar() {
		
		// TODO:
		// Initialize ControlBarPanel
		this.controlBar = new ControlBar();
	}
	
	protected void cmdExitClicked() {
		
		// TODO:
		this.dispose();
		System.exit(0);
	}
	
	private void cmdStartClicked() {
		
		// TODO
		this.startWorld();
	}
	
	private void cmdStopClicked() {
		
		// TODO
		this.stopWorld();
	}
	
	private void cmdPauseClicked() {
		
		// TODO
		this.stopWorld();
	}
	
	private void cmdOpenWorldClicked() {
		
		File file = null;
		
		while(file == null || !file.exists()) {
			
			int k = this.worldChooser.showOpenDialog(this);
			
			if(k == JFileChooser.CANCEL_OPTION)
				return;
			
			if(k == JFileChooser.APPROVE_OPTION) {
				
				file = worldChooser.getSelectedFile();
				
				if(		! file.exists()
					 && ! file.getName().endsWith(".xml")) {
					
					file = new File(file.getAbsolutePath()+".xml");
				}
			}
		}
		
		// TODO: Alte Welt entladen -> Schließen
		this.closeWorld();
		
		// TODO: Exception-Handling, ...
		try {
			// TODO: Wenn kein DTD-file gegeben, besser Fehlermeldung!
			this.world = World.parseWorldFile(file);
			//this.world = new World(file.getAbsolutePath());
			
			// TODO:
			this.worldPanel.setWorld(this.world);
			
			this.controller = Controller.start(this, this.world);
			
			this.validate();
			
			Debug.out.println("Neue Welt geöffnet.");
			
		} catch (SAXException e) {
			Debug.out.println("Fehler beim Öffnen der Welt-Datei.");
			e.printStackTrace();
		} catch (IOException e) {
			Debug.out.println("Fehler beim Öffnen der Welt-Datei.");
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: Über?
			Debug.out.println("Fehler beim Öffnen der Welt-Datei.");
			e.printStackTrace();
		}
	}
	
	private void cmdRandomWorldClicked() {
		
		this.closeWorld();
		
		ParcoursGenerator parcGen = new ParcoursGenerator();
		
		String fileContent = parcGen.generateParc();
		
		this.tmpParcoursFile = new File("./"+TMP_PARCOURS_PATH+"/"+TMP_PARCOURS_FILE_NAME+".xml");
		
		for(int i=1; this.tmpParcoursFile.exists(); i++) {
			
			this.tmpParcoursFile = new File("./"+TMP_PARCOURS_PATH+"/"+TMP_PARCOURS_FILE_NAME+i+".xml");
		}
		
		try {
			FileWriter fw = new FileWriter(this.tmpParcoursFile);
			//BufferedWriter bw = new BufferedWriter(fw);
			
			fw.write(fileContent);
			fw.flush();
			fw.close();
			
			// TODO: kopiert von openWorld:
			
			this.world = World.parseWorldFile(this.tmpParcoursFile);
			//this.world = new World(this.tmpParcoursFile.getAbsolutePath());
			
			// TODO:
			this.worldPanel.setWorld(this.world);
			
			this.controller = Controller.start(this, this.world);
			
			this.validate();
			
			Debug.out.println("Zufällige Welt wurde generiert..");
			
		} catch (SAXException e) {
			Debug.out.println("Fehler beim Öffnen der Welt-Datei.");
			e.printStackTrace();
		} catch (IOException e) {
			Debug.out.println("Fehler beim Öffnen der Welt-Datei.");
			e.printStackTrace();
		} catch (Exception e) {
			Debug.out.println("Fehler beim Öffnen der Welt-Datei.");
			// TODO: Über?
			e.printStackTrace();
		}
	}
	
	private void cmdCloseWorldClicked() {
		
		// TODO
		this.closeWorld();
	}
	
	private void cmdSaveWorldClicked() {
		
		if(this.worldChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			
			File file = this.worldChooser.getSelectedFile();
			
			if(! file.getAbsolutePath().endsWith(".xml"))
				file = new File(file.getAbsolutePath()+".xml");
			
			if(file.exists()) {
				if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
																			"Do you really want to overwrite file:\n"
																			+file.getName(),
																			"Overwrite file?",
																			JOptionPane.YES_NO_OPTION,
																			JOptionPane.WARNING_MESSAGE)) {
					return;
				}
			}
			
			// TODO: Hässlich!
			// XML-Stream zwischenspeichern und in Datei schreiben; keine tmp-Dateien mehr (?)
			try {
				FileInputStream fin = new FileInputStream(this.tmpParcoursFile);
				FileOutputStream fout = new FileOutputStream(file);
				
				BufferedInputStream bin = new BufferedInputStream(fin);
				BufferedOutputStream bout = new BufferedOutputStream(fout);
				
				for(int b = bin.read(); b != -1; b = bin.read()) {
					bout.write(b);
				}
//				byte[] b = new byte[bin.available()];
//				
//				bin.read(b);
//				bout.write(b);
				
				bout.flush();
				
				bin.close();
				bout.close();
				
				fin.close();
				fout.close();
				
			} catch(Exception e) {
				
				Debug.out.println("Fehler: Datei konnte nicht gespeichert werden!");
				e.printStackTrace();
				return;
			}
			
			
			//Debug.out.println("  !!! Funktioniert noch nicht !!!");
			Debug.out.println("Welt wurde gespeichert als \""+file.getName()+"\".");
			
			// TODO: XML-Geblubber schreiben, bzw. Datei kopieren (siehe randomWorld)...
		}
	}
	
	private void cmdSetJudgeClicked() {
		
		JudgeChooser.showJudgeChooserDialog(this);
	}
	
	private void cmdAddBotClicked() {
		
		if(this.controller == null)
			return;
		
		// TODO
		//Bot bot = this.controller.addBot("CtBotSimTest");
		Bot bot = BotChooser.showBotChooserDialog(this, this.controller);
		
		if(bot == null)
			return;
		
		BotInfo info = new BotInfo("Test"+Math.round(Math.random()*10), "SimTest", bot, new DefBotPanel());
		
		this.addBot(info);
	}
	
	private void cmdConfigureBotsClicked() {
		
		Debug.out.println("  !!! Funktioniert noch nicht !!!");
		// TODO
	}
	
	// TODO: Geschwindigkeit setzen:
	private void startWorld() {
		
		//this.world.setHaveABreak(false);
		if(this.controller != null)
			this.controller.unpause();
	}
	
	// TODO: Geschwindigkeit setzen:
	private void stopWorld() {
		
		//this.world.setHaveABreak(true);
		if(this.controller != null)
			this.controller.pause();
	}
	
	// TODO: Close Controller:
	private void closeWorld() {
		
		if(this.world == null)
			return;
		
		this.stopWorld();
		
		//this.world.die();
		
		// TODO: ganz hässliche!
		//this.split.remove(this.worldPanel);
		this.consoleSplit.remove(this.worldPanel);
		this.world = null;
		this.worldPanel = null;
		initWorldView();
		this.statusBar.reinit();
		this.controlBar.reinit();
		//this.split.setRightComponent(this.worldPanel);
		this.consoleSplit.setTopComponent(this.worldPanel);
		this.split.resetToPreferredSizes();
		//this.consoleSplit.resetToPreferredSizes();
		
		Debug.out.println("Welt wurde geschlossen.");
	}
	
	protected void setTickRate(long rate) {
		
		this.controller.setTickRate(rate);
	}
	
	public void update() {
		
		// TODO: Größe sichern...
		//this.setPreferredSize(this.getSize());
		
		//this.setVisible(false);
		//this.pack();
		//this.setVisible(true);
		
		// --> this.validate();
		
		// TODO: ControlBar-Update, WorldView-Update
		this.controlBar.update();
		
		this.worldPanel.update();
	}
	
	public void update(long time) {
		
		// TODO: alles ganz hässlich:
		this.statusBar.updateTime(time);
		this.update();
	}
	
	
	// TODO:
	public void addBot(BotInfo botInfo) {
		
		this.controlBar.addBot(botInfo);
		
		this.update();
		this.split.resetToPreferredSizes();
		
		//this.validate();
		//this.doLayout();
		
		Debug.out.println("Bot \""+botInfo.getName()+"\" wurde hinzugefügt.");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// TODO:
		// - set DefaultLocale
		// - set DefaultLook&Feel
		//
		// - instantiate SimFrame
		// - show SimFrame
		//
		// ...
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
			//UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
			
			/*
			LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
			
			for(int i=0; i<info.length; i++) {
				
				System.out.println(info[i].getName());
				System.out.println(info[i].getClassName());
			}
			*/
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//JFrame.setDefaultLookAndFeelDecorated(true);
		
		CtSimFrame simFrame = new CtSimFrame("Ct-Sim-Frame");
		
		simFrame.setVisible(true);
		
//		testfall2(simFrame);
	}
	
	//////////////////////////////////////////////////////////////////////
	// TestfÃ¤lle:
	
	// Testfall 1:
//	public static void testfall1(CtSimFrame simFrame) {
//		
//		simFrame.addBot(new BotInfo("Hallo", "Mama", null, new DefBotPanel()));
//		simFrame.addBot(new BotInfo("Bla", "Blubb", null, new DefBotPanel()));
//	}
//	
//	// Testfall 2:
//	public static void testfall2(CtSimFrame simFrame) {
//		
//		Bot b1 = Bot.getTestBot1();
//		Bot b2 = Bot.getTestBot2();
//		
//		simFrame.addBot(new BotInfo("TestBot 1", "TestBot", b1, new DefBotPanel()));
//		simFrame.addBot(new BotInfo("TestBot 2", "TestBot", b2, new DefBotPanel()));
//	}
}
