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

package ctSim.view;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import javax.media.j3d.View;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import org.xml.sax.SAXException;

import ctSim.ConfigManager;
import ctSim.controller.Controller;
import ctSim.model.ParcoursGenerator;
import ctSim.model.World;
import ctSim.model.bots.Bot;

/**
 * Die GUI-Hauptklasse fuer den c't-Sim
 * 
 * @author Felix Beckwermert
 *
 */
public class CtSimFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
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
	@SuppressWarnings("unused")
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
	
	private final String TMP_PARCOURS_PATH = "tmp"; //$NON-NLS-1$
	private final String TMP_PARCOURS_FILE_NAME = "tmpParcoursFile"; //$NON-NLS-1$
	private File tmpParcoursFile;
	
	
	/**
	 * Der Konstruktor
	 * @param title Die Titelzeile des Fensters
	 */
	private CtSimFrame(Controller ctrl, String title) {
		
		// TODO: Titel setzen (?)
		super(title);
		
		this.controller = ctrl;
		
		loadImages();
		
		// TODO:
		//this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			
			/** 
			 * @see java.awt.event.WindowListener#windowClosing(java.awt.event.WindowEvent)
			 */
			@Override
			public void windowClosing(@SuppressWarnings("unused") WindowEvent e) {
				
				cmdExitClicked();
			}
		});
		
		if(ConfigManager.getConfigValue("worlddir") != null)
			this.worldChooser = new JFileChooser(ConfigManager.getConfigValue("worlddir")); //$NON-NLS-1$
		else
			this.worldChooser = new JFileChooser("."); //$NON-NLS-1$
		
		this.worldChooser.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return (f.isDirectory() || f.getName().endsWith(".xml")); //$NON-NLS-1$
			}

			@Override
			public String getDescription() {
				return "World-Files (*.xml)"; //$NON-NLS-1$
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
		this.consoleSplit.setResizeWeight(1.0);
		this.consoleSplit.setOneTouchExpandable(true);
		this.consoleSplit.setTopComponent(this.worldPanel);
		this.consoleSplit.setBottomComponent(console);
		
		//JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.split.setLeftComponent(this.controlBar);
		//split.setRightComponent(this.worldPanel);
		this.split.setRightComponent(this.consoleSplit);
		this.split.setDividerLocation(0);
		//split.setContinuousLayout(true);
		//split.setOneTouchExpandable(true);
		
		this.add(this.split, BorderLayout.CENTER);
		
		//this.setPreferredSize(new Dimension(800, 650));
		this.setPreferredSize(new Dimension(1000, 800));
		//this.setMinimumSize(new Dimension(900, 650));
		
		this.pack();
		//this.show();
		this.setVisible(true);
	}
	
	private void loadImages() {
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Toolkit tk = Toolkit.getDefaultToolkit();

		this.openImg    = tk.getImage(cl.getResource("images/Open16.gif")); //$NON-NLS-1$
		this.closeImg   = tk.getImage(cl.getResource("images/Delete16.gif")); //$NON-NLS-1$
		this.saveImg    = tk.getImage(cl.getResource("images/SaveAs16.gif")); //$NON-NLS-1$
		this.randomImg  = tk.getImage(cl.getResource("images/New16.gif")); //$NON-NLS-1$
		
		//refreshImg = tk.getImage(cl.getResource("images/Refresh16.gif"));
		this.stopImg    = tk.getImage(cl.getResource("images/Stop16.gif")); //$NON-NLS-1$
		this.pauseImg   = tk.getImage(cl.getResource("images/Pause16.gif")); //$NON-NLS-1$
		this.playImg    = tk.getImage(cl.getResource("images/Play16.gif")); //$NON-NLS-1$
		
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

		this.tracker = new MediaTracker(this);
		this.tracker.addImage(this.openImg, 0);
		this.tracker.addImage(this.closeImg, 0);
		this.tracker.addImage(this.randomImg, 0);
		this.tracker.addImage(this.saveImg, 0);
		
		//tracker.addImage(refreshImg, 0);
		this.tracker.addImage(this.stopImg, 0);
		this.tracker.addImage(this.pauseImg, 0);
		this.tracker.addImage(this.playImg, 0);
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
			this.tracker.waitForAll();
		} catch (InterruptedException e) {
			System.err.println("Unterbrechung!"); //$NON-NLS-1$
		}
		
		// TODO:
		// Initialize Actions for MenuBar and ToolBar
		// - Icons adden
		
		//////////////////////////////////////////////////////////////////////
		// World-Actions
		this.openWorld = new AbstractAction("Oeffnen...", //$NON-NLS-1$
				new ImageIcon(this.openImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdOpenWorldClicked();
			}
		};
		this.randomWorld = new AbstractAction("Generieren...", //$NON-NLS-1$
				new ImageIcon(this.randomImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdRandomWorldClicked();
			}
		};
		this.closeWorld = new AbstractAction("Schliessen", //$NON-NLS-1$
				new ImageIcon(this.closeImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdCloseWorldClicked();
			}
		};
		this.saveWorld = new AbstractAction("Speichern als...", //$NON-NLS-1$
				new ImageIcon(this.saveImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdSaveWorldClicked();
			}
		};
		
		//////////////////////////////////////////////////////////////////////
		// Bot-Actions
		this.selectJudge = new AbstractAction("Schiedsrichter waehlen...") { //$NON-NLS-1$
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdSetJudgeClicked();
			}
		};
		this.addBot = new AbstractAction("Bot hinzufuegen...") { //$NON-NLS-1$

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings({"synthetic-access","synthetic-access"})
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdAddBotClicked();
			}
		};
		this.configBots = new AbstractAction("Bots konfigurieren...") { //$NON-NLS-1$

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdConfigureBotsClicked();
			}
		};
		
		//////////////////////////////////////////////////////////////////////
		// Control-Options
		this.start = new AbstractAction("Start", //$NON-NLS-1$
				new ImageIcon(this.playImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdStartClicked();
			}
		};
		this.stop = new AbstractAction("Stopp", //$NON-NLS-1$
				new ImageIcon(this.stopImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				cmdStopClicked();
			}
		};
		this.pause = new AbstractAction("Pause", //$NON-NLS-1$
				new ImageIcon(this.pauseImg)) {

			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
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
		
		JMenu worldMenu = new JMenu("Welt"); //$NON-NLS-1$
		//worldMenu.getPopupMenu().setLightWeightPopupEnabled(false);
		worldMenu.add(this.openWorld);
		worldMenu.add(this.randomWorld);
		worldMenu.add(this.closeWorld);
		worldMenu.add(this.saveWorld);
		
		JMenu botMenu = new JMenu("Optionen"); //$NON-NLS-1$
		//botMenu.getPopupMenu().setLightWeightPopupEnabled(false);
		botMenu.add(this.selectJudge);
		botMenu.addSeparator();
		botMenu.add(this.addBot);
		// botMenu.add(this.configBots);
		
		JMenu controlMenu = new JMenu("Simulation"); //$NON-NLS-1$
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
	
	public View getView(){
		return worldPanel.getWorldView().getUniverse().getViewer().getView();
	}
	
	public WorldView getWorldView(){
		return worldPanel.getWorldView();
	}
	
	private void initControlBar() {
		
		// Initialize ControlBarPanel
		this.controlBar = new ControlBar(this);
	}
	
	protected void cmdExitClicked() {
		
		this.controller.stop();
		this.dispose();
		System.exit(0);
	}
	
	private void cmdStartClicked() {
		
		this.startWorld();
	}
	
	private void cmdStopClicked() {
		
		this.resetWorld();
	}
	
	private void cmdPauseClicked() {
		
		this.stopWorld();
	}
	
	private void cmdOpenWorldClicked() {
		
		File file = null;
		
		while(file == null || !file.exists()) {
			
			int k = this.worldChooser.showOpenDialog(this);
			
			if(k == JFileChooser.CANCEL_OPTION)
				return;
			
			if(k == JFileChooser.APPROVE_OPTION) {
				
				file = this.worldChooser.getSelectedFile();
				
				if(		! file.exists()
					 && ! file.getName().endsWith(".xml")) { //$NON-NLS-1$
					
					file = new File(file.getAbsolutePath()+".xml"); //$NON-NLS-1$
				}
			}
		}
		
		this.openWorld(file);
	}
	
	private void cmdRandomWorldClicked() {
		
		this.closeWorld();
				
		String fileContent = ParcoursGenerator.generateParc();
		
		this.tmpParcoursFile = new File("./"+this.TMP_PARCOURS_PATH+"/"+this.TMP_PARCOURS_FILE_NAME+".xml");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		
		for(int i=1; this.tmpParcoursFile.exists(); i++) {
			
			this.tmpParcoursFile = new File("./"+this.TMP_PARCOURS_PATH+"/"+this.TMP_PARCOURS_FILE_NAME+i+".xml");   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
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
			
			//this.controller = Controller.start(this, this.world);
			this.controller.setWorld(this.world);
			
			this.validate();
			
			Debug.out.println("Labyrinth generiert"); //$NON-NLS-1$
			
		} catch (SAXException e) {
			Debug.out.println("Fehler beim Oeffnen der Welt-Datei."); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			Debug.out.println("Fehler beim Oeffnen der Welt-Datei."); //$NON-NLS-1$
			e.printStackTrace();
		} catch (Exception e) {
			Debug.out.println("Fehler beim Oeffnen der Welt-Datei."); //$NON-NLS-1$
			// TODO: Ueber?
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
			
			if(! file.getAbsolutePath().endsWith(".xml")) { //$NON-NLS-1$
				file = new File(file.getAbsolutePath()+".xml"); //$NON-NLS-1$
			}
			
			if(file.exists()) {
				if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(this,
																			"Do you really want to overwrite file:\n" //$NON-NLS-1$
																			+file.getName(),
																			"Overwrite file?", //$NON-NLS-1$
																			JOptionPane.YES_NO_OPTION,
																			JOptionPane.WARNING_MESSAGE)) {
					return;
				}
			}
			
			// TODO: Haesslich!
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
				
				Debug.out.println("Fehler: Datei konnte nicht gespeichert werden!"); //$NON-NLS-1$
				e.printStackTrace();
				return;
			}
			
			
			//Debug.out.println("  !!! Funktioniert noch nicht !!!");
			Debug.out.println("Welt wurde gespeichert als \""+file.getName()+"\"."); //$NON-NLS-1$ //$NON-NLS-2$
			
			// TODO: XML-Geblubber schreiben, bzw. Datei kopieren (siehe randomWorld)...
		}
	}
	
	private void cmdSetJudgeClicked() {
		
//		if(this.controller == null) {
//			Debug.out.println("Fehler: Noch keine Welt geladen."); //$NON-NLS-1$
//			return;
//		}
		
		String judge = JudgeChooser.showJudgeChooserDialog(this);
		
		if(this.controller.setJudge(judge)) {
			
			Debug.out.println("Judge \""+judge+"\" wurde gewaehlt.");  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	private void cmdAddBotClicked() {
		
//		if(this.controller == null)
//			return;
		
		// TODO
		//Bot bot = this.controller.addBot("CtBotSimTest");
		Bot bot = BotChooser.showBotChooserDialog(this, this.controller);
		
		// TODO: hässlich: C-Bot wird indirekt gesetzt
		if(bot == null)
			return;
		
		BotInfo info = new BotInfo("Test"+Math.round(Math.random()*10), "SimTest", bot, new DefBotPanel()); //$NON-NLS-1$ //$NON-NLS-2$
		
		this.addBot(info);
	}
	
	private void cmdConfigureBotsClicked() {
		
		// TODO:
		Debug.out.println("  !!! Funktioniert noch nicht !!!"); //$NON-NLS-1$
	}
	
	// TODO: Geschwindigkeit setzen:
	private void startWorld() {
		
		//this.world.setHaveABreak(false);
		this.controller.unpause();
	}
	
	// TODO: Geschwindigkeit setzen:
	private void stopWorld() {
		
		//this.world.setHaveABreak(true);
		this.controller.pause();
	}
	
	private void resetWorld() {
		
		this.controller.reset();
		
		this.statusBar.reinit();
		this.controlBar.reinit();
		
		this.split.resetToPreferredSizes();
		
		Debug.out.println("Alle Bots entfernt.");
	}
	
	public void openWorld(File file) {
		
		this.closeWorld();
		
		// TODO: Exception-Handling, ...
		try {
			// TODO: Wenn kein DTD-file gegeben, besser Fehlermeldung!
			this.world = World.parseWorldFile(file);
			//this.world = new World(file.getAbsolutePath());
			
			// TODO:
			this.worldPanel.setWorld(this.world);
			
			//this.controller = Controller.start(this, this.world);
			this.controller.setWorld(this.world);
			
			this.validate();
			
			this.tmpParcoursFile = file;
			
			Debug.out.println("Neue Welt geoeffnet."); //$NON-NLS-1$
			
		} catch (SAXException e) {
			Debug.out.println("Fehler beim Oeffnen der Welt-Datei."); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			Debug.out.println("Fehler beim Oeffnen der Welt-Datei."); //$NON-NLS-1$
			e.printStackTrace();
		} catch (Exception e) {
			// TODO: Ueber?
			Debug.out.println("Fehler beim Oeffnen der Welt-Datei."); //$NON-NLS-1$
			e.printStackTrace();
		}
	}
	
	// TODO: Close Controller:
	private void closeWorld() {
		
		if(this.world == null)
			return;
		
		this.stopWorld();
		
		//this.world.die();
		
		// TODO: ganz haesslich!
		//this.split.remove(this.worldPanel);
		this.consoleSplit.remove(this.worldPanel);
		this.world = null;
		this.worldPanel = null;
		initWorldView();
		this.statusBar.reinit();
		this.controlBar.reinit();
		//this.split.setRightComponent(this.worldPanel);
		this.consoleSplit.setTopComponent(this.worldPanel);
		this.updateLayout();
		//this.consoleSplit.resetToPreferredSizes();
		
		//this.controller.stop();
		//this.controller = null;
		
		Debug.out.println("Welt wurde geschlossen."); //$NON-NLS-1$
	}
	
	/**
	 * @param rate Die neue Zeitbasis fuer den Simulator in Aufrufen alle xxx ms
	 */
	protected void setTickRate(int rate) {
		
		this.controller.getWorld().setBaseTimeReal(rate);
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
	 * @param botInfo Die Informationen rund um den neuen Bot
	 */
	public void addBot(BotInfo botInfo) {
		
		this.controlBar.addBot(botInfo);
		
		this.update();
		this.updateLayout();
		
		//this.validate();
		//this.doLayout();
		
		Debug.out.println("Bot \""+botInfo.getName()+"\" wurde hinzugefuegt.");  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	public void removeBot(BotInfo botInfo) {
		
		this.controlBar.removeBot(botInfo);
		this.update();
		this.split.resetToPreferredSizes();
		
		this.controller.removeBot(botInfo.getBot());
		
		Debug.out.println("Bot \""+botInfo.getName()+"\" wurde gelöscht.");  //$NON-NLS-1$//$NON-NLS-2$
	}
	
	public static CtSimFrame showSimGUI(Controller ctrl, String title) {
		
		return new CtSimFrame(ctrl, title);
	}
	
//	/**
//	 * Hauptmethode
//	 * @param args bleibt leer
//	 */
//	public static void main(String[] args) {
//		
//		// TODO:
//		// - set DefaultLocale
//		// - set DefaultLook&Feel
//		//
//		// - instantiate SimFrame
//		// - show SimFrame
//		//
//		// ...
//		
//		try {
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
//			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
//			//UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
//			
//			/*
//			LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
//			
//			for(int i=0; i<info.length; i++) {
//				
//				System.out.println(info[i].getName());
//				System.out.println(info[i].getClassName());
//			}
//			*/
//		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (UnsupportedLookAndFeelException e) {
//			e.printStackTrace();
//		}
//		
//		//JFrame.setDefaultLookAndFeelDecorated(true);
//		
//		CtSimFrame simFrame = new CtSimFrame("Ct-Sim-Frame"); //$NON-NLS-1$
//		
//		simFrame.setVisible(true);
//		
////		testfall2(simFrame);
//	}
	
	//////////////////////////////////////////////////////////////////////
	// Testfaelle:
	
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
