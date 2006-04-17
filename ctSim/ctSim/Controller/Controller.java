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

package ctSim.Controller;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.j3d.utils.image.TextureLoader;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import ctSim.Model.*;
import ctSim.Model.Bots.Bot;
import ctSim.Model.Bots.CtBotRealCon;
import ctSim.Model.Bots.CtBotSimTcp;
import ctSim.Model.Bots.CtBotSimTest;
import ctSim.Model.Rules.Judge;
import ctSim.Model.Scene.SceneUpdate;
import ctSim.View.*;
import ctSim.*;

/**
 * Die Startklasse des c't-Sim; stellt die Welt und die grafischen
 * Anzeigefenster bereit, kontrolliert den gesamten Ablauf des Simulators.
 * 
 * @author Peter Koenig (pek@heise.de)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Benjamin Benz (bbe@heise.de)
 * 
 */
public class Controller {
	private static final String CONFIGFILE = "config/ct-sim.xml";

	/** Das Modell der Welt */
	private World world;

	private WorldView worldView;

	private ControlFrame controlFrame;
	
	/* Markus Lang 2006-03-17:
	 * Instanzen fuer das JSplitPane Hauptfenster
	 */
	private CtSimFrame ctSimFrame;

	/** Eine Liste aller verbundenen Remote-Views */
	private List<RemoteView> remoteViews;

	/**
	 * Anzahl der Bots im System
	 */
	private HashMap numberBots = new HashMap();

	/**
	 * Enthaelt die gesamten Einzelparameter der Konfiguration 
	 */
	private HashMap config = new HashMap();
	
	/** Der Schiedsrichter, falls vorhanden */
	private Judge judge= null; 
	
	/**
	 * Liest die Konfiguration des Sims ein
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private void parseConfig() throws Exception{
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			parser.parse(CONFIGFILE);
			// umwandeln in ein Document
			Document doc = parser.getDocument();
			
			// Und Anfangen mit dem abarbeiten
			
			//als erster suchen wir uns den Parameter-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while (n != null){
				if (n.getNodeName().equals("parameter")){
					String name = n.getAttributes().getNamedItem("name").getNodeValue();
					String value = n.getAttributes().getNamedItem("value").getNodeValue();
					config.put(name,value);
				}
				n=n.getNextSibling();
			}
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+ex);
			throw ex;
		}
		
	}
	
	/**
	 * Benachrichtige alle Views, dass sich Daten geaendert haben
	 */
	public void update() {
		List<RemoteView> toRemove = null;

		SceneUpdate sU= null ;
		
		Iterator it = remoteViews.iterator();
		if (it.hasNext())
			sU = new SceneUpdate(world.getSceneLight());

		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			try {
				rV.update(sU);
			} catch (IOException ex) {
				ErrorHandler.error("Remote View gestorben");

				// Alle Views merken, die zu entfernen sind
				if (toRemove == null)
					toRemove = new LinkedList<RemoteView>();
				toRemove.add(rV);
			}
		}

		// Views entfernen, die tot sind
		if (toRemove != null) {
			it = toRemove.iterator();
			while (it.hasNext()) {
				removeView((RemoteView) it.next());
			}
		}

		// sc.update(world.getSceneLight().getBots());

		worldView.repaint();
	}

	/**
	 * Bennent einen neuen Bot
	 * Achtung, die Namensvergabe wird nicht zurueckgesetzt, wenn ein bot stirbt
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String getNewBotName(String type){
		// TODO Namensvergabe sollte die Namen von gestorbenen Bots neu verteilen

		// Schaue nach, wieviele Bots von der Sorte wir schon haben
		Integer bots = (Integer) numberBots.get(type);
		if (bots == null){
			bots = new Integer(0);
		}

		String name=type +"_"+ bots.intValue();

		bots = new Integer(bots.intValue()+1);	// erhoehen
		numberBots.put(type, bots);				// sichern
		
		return name;
		
	}
	
	/*
	 * Initilaisiert das gesamte Framework
	 */
	private void init() {
		remoteViews = new LinkedList<RemoteView>();
		
		try {
			parseConfig();
		} catch (Exception ex){
			ErrorHandler.error("Einlesen der c't-Sim-Konfiguration nicht moeglich. Abburch!");
			return;
		}
		
		String parcours = (String)config.get("parcours");
		if (parcours==null){
			
			// Nach einem Parcours fragen
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Bitte einen Parcours waehlen");
	        fc.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().toLowerCase().endsWith(".xml") || f.isDirectory();
                }
                public String getDescription() {
                		return "XML-Dateien (*.xml)";
                }
            });
	        if (fc.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION)
				parcours = fc.getSelectedFile().getAbsolutePath();
		}
		
		if (parcours==null) {
			ErrorHandler.error("Kein Parcours gewaehlt. Abburch!");
			return;			
		}
		
		try {
			world = new World(this,parcours);
		} catch (Exception ex){
			ErrorHandler.error("Probleme beim Aufbau der Welt. Abburch!");
			return;
		}

		controlFrame = new ControlFrame(this);

		// sc= world.exportSceneGraph();

		/* Eine lokale WorldView braucht man */
		worldView = new WorldView();
		worldView.setScene(world.getSceneLight().getScene(),world.getPlaygroundDimX(),world.getPlaygroundDimY());
		worldView.initGUI();
		
		/* Markus Lang 2006-03-17:
		 * Hauptfenster erstellen mit den bereits vorhandenen 
		 * ControlFRame und WoldView Instanzen.
		 */
		this.ctSimFrame = new CtSimFrame(controlFrame, this.worldView);
		this.controlFrame.setCtSimFrameInstance(ctSimFrame);

		/*
		 * WorldView worldView2 = new WorldView();
		 * worldView2.setScene(sc.getScene()); 
		 * worldView2.initGUI();
		 */


		controlFrame.setVisible(true);
		world.setControlFrame(controlFrame);
		world.start();

		if (config.get("judge")!= null) {
			try {
				judge = (Judge) Class.forName((String)config.get("judge")).newInstance();
			} catch (ClassNotFoundException e) {
				ErrorHandler.error("Die Judge-Klasse wurde nicht gefunden: "+e);
			} catch (Exception e) {
				ErrorHandler.error("Probleme beim instantiieren der Judge-Klasse: "+e);
			}

			judge.setController(this);
			judge.start();
			controlFrame.addJudge(judge);
		}
		
		String port = (String)config.get("viewport");
		if (port != null){
			try {
				int p = new Integer(port).intValue();
				
				System.out.println("Warte auf Verbindung von View-Clients auf Port "+p); 
				SocketListener ViewListener = new
				ViewSocketListener(p); 
				ViewListener.start();
				
			} catch (Exception ex) {
				ErrorHandler.error("Kann den ViewPort ("+port+ ") nicht dekodieren");
			}
		}
		
		
		
		/*
		 * Der Sim sollte auch auf die TCP-Verbindung lauschen, wenn es Testbots
		 * gibt. Aus diesem Grund ist der thread, der auf TCP-Verbindungen
		 * lauscht, an dieser Stelle eingebaut.
		 */

		port = (String)config.get("botport");
		if (port != null){
			try {
				int p = new Integer(port).intValue();
				
				System.out.println("Warte auf Verbindung vom c't-Bot auf Port "+p);
				SocketListener BotListener = new BotSocketListener(p);
				BotListener.start();
				
			} catch (Exception ex) {
				ErrorHandler.error("Kann den botPort ("+port+ ") nicht dekodieren");
			}
		} else {
			ErrorHandler.error("Kein botPort in der Config-Datei gefunden. Es wird nicht auf Bots gelauscht!");			
		}
		
		
//		try {
//			ClientView clientView = new ClientView("localhost",10002);
//		} catch (Exception ex){
//			ErrorHandler.error("Fehler beim oeffnen der ClientView "+ex);
//		}
	}

	/**
	 * Startet den c't-Sim
	 * 
	 * @param args
	 *            Argumente werden nicht eingelesen
	 */
	public static void main(String[] args) {
		System.out.println("Simulator startet");

		Controller controller = new Controller();
		controller.init();
	}

	/**
	 * Fuegt einen Bot in alle Views ein
	 * 
	 * @param id Bezeichner des Bot
	 * @param bg Branchgruppe des Bot
	 * @param map Liste mit Einstiegspunkten
	 */
	public void addToView(String id, HashMap map) {
		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			rV.addBotToView(id, map);
		}
	}

	/**
	 * Entfernt einen Bot aus allen Views
	 * 
	 * @param id
	 *            Bezeichner des Bot
	 */
	public void removeFromView(String id) {
		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			rV.removeBot(id);
		}
		
		ErrorHandler	.error("removeBotFromView nicht vollstaendig implementiert");
		// TODO Was ist mit der lokalen View?
		// TODO Was ist mit den Panels ?
	}

	/**
	 * Fuegt einen Bot in die Welt ein
	 * @param bot
	 * @param name
	 */
	private void addBot(Bot bot){
		if (bot != null) {
			bot.providePanel();
			
			// TODO Sinnvolle Zuordnung von Bot-Name zu Konfig
			HashMap botConfig = getBotConfig("config/ct-sim.xml",bot.getName());
			if (botConfig == null){
				ErrorHandler.error("Keine BotConfig fuer: "+bot.getName()+" in der XML-Config-Datei gefunden. Lade Defaults.");
				botConfig = getBotConfig("config/ct-sim.xml","default");
			}
			
			if (botConfig == null){
				ErrorHandler.error("Keine Default-BotConfig in der XML-Config-Datei gefunden. Starte ohne.");
			}

			bot.setAppearances(botConfig);
			
			world.addBot(bot);
			controlFrame.addBot(bot);
			// Dann wird der eigene Bot-Thread gestartet:
			bot.start();
		}		
	}
	

	/**
	 * Fuegt der Welt einen neuen Bot des gewuenschten Typs hinzu
	 * 
	 * Typ ist entweder "CtBotSimTest" oder "CtBotRealJD2XX"
	 * 
	 * Crimson 2006-06-13: Wird vorlaeufig noch fuer die Testbots und "serial
	 * bots" verwendet. TCPBots koennen sich immer von aussen verbinden
	 * 
	 * @param type	Was fuer ein Bot (testbot CtBotRealTcp)
	 * @param name Name des Bots
	 */
	public void addBot(String type) {
		Bot bot = null;

		if (type.equalsIgnoreCase("CtBotSimTest")) {
			bot = new CtBotSimTest(this, new Point3f(), new Vector3f());
		}

		if (type.equalsIgnoreCase("CtBotRealJD2XX")) {
			Connection com = waitForJD2XX();
			if (com != null) {
				bot = new CtBotRealCon(this, new Point3f(), new Vector3f(), com);
				System.out.println("Real Bot via JD2XX startet");
			}
		}
		addBot(bot);
	}

	/**
	 * Wartet auf eine eingehende JD2XX-Verbindung
	 * @return
	 */
	private Connection waitForJD2XX(){
		JD2xxConnection com = new JD2xxConnection();
		try {
			com.connect();
			System.out.println("JD2XX-Connection aufgebaut");
			return com;
		} catch (Exception ex) {
			ErrorHandler.error("JD2XX Connection nicht moeglich: " + ex);
			return null;
		}
	}
	
	/**
	 * Startet den Standard externen Bot, falls vorhanden, fragt sonst nach
	 */
	public void invokeBot(){
		String filename = (String)config.get("botbinary");
		
		if (filename==null){
			
			// Nach einem Parcours fragen
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Bitte einen Bot waehlen");
	        if (fc.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION)
	        		filename = fc.getSelectedFile().getAbsolutePath();
		}

		if (filename!=null)
			invokeBot(filename);
		else
			ErrorHandler.error("Kein BotBinary definiert oder gewaehlt");
	}	
	
	
	/**
	 * Startet einen externen Bot
	 * @param filename Pfad zum Binary des Bots
	 */
	public void invokeBot(String filename){
		System.out.println("Starte externen Bot: "+filename);
		CmdExec executor = new CmdExec();
		try {
			executor.exec(filename);
		} catch (Exception ex){
			ErrorHandler.error("Kann Bot: "+filename+" nicht ausfuehren: "+ex);
		}
	}
	
	/**
	 * Entfernt eine View aus der Liste
	 * 
	 * @param remoteView
	 */
	public void removeView(RemoteView remoteView) {
		remoteViews.remove(remoteView);
	}

	/**
	 * Beendet die Simulation, wird durch den "Beenden"-Knopf des Fensters
	 * ControlFrame aufgerufen
	 */
	public void endSim() {
		world.die();
		ctSimFrame.dispose();

		if (judge != null)
			judge.die();
		
		// Alle Views schliessen
		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			rV.die();
		}

		System.exit(0);
	}

	/**
	 * Gibt der Welt den Hinweis, dass sich ihr Zustand geaendert hat
	 */
	public void reactToChange() {
		world.reactToChange();
	}

	/**
	 * @return Gibt eine Referenz auf das Fenster mit den Kontrolltafeln
	 *         zurueck.
	 */
	public ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @return Gibt eine Referenz auf die simulierte Welt zurueck.
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * Liefert die Config zu einem Bot zurueck
	 * @param filename
	 * @param botId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private HashMap getBotConfig(String filename, String botId){
		boolean found = false;
		
		HashMap botConfig = new HashMap();
		
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			parser.parse(filename);
			// umwandeln in ein Document
			Document doc = parser.getDocument();
			
			// Und Anfangen mit dem abarbeiten
			
			//als erster suchen wir uns den Parcours-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while ((n != null)&& (!n.getNodeName().equals("bots")))
				n=n.getNextSibling();

			//	Eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			NodeList bots=n.getChildNodes();	
	
			for(int b=0; b<bots.getLength()-1;b++){
				Node botSection = bots.item(b);
				// Ist das ueberhaupt ein Bot-Eintrag?
				if (botSection.getNodeName().equals("bot"))
					// Und ist es auch der gesuchte
					if (botSection.getAttributes().getNamedItem("name").getNodeValue().equals(botId)){
						found=true;
						NodeList children = botSection.getChildNodes();
						
						//	HashMap mit den Apearances aufbauen
				        for (int i=0; i<children.getLength()-1; i++){
				        		Node appearance =children.item(i);
				        		if (appearance.getNodeName().equals("appearance")){
				        			// Zuerst den Type extrahieren
				        			String item = appearance.getAttributes().getNamedItem("type").getNodeValue();
				        			
				        			String texture = null;
				        			String clone = null;
				        			
				        			HashMap colors = new HashMap();
				        			
				        			NodeList features = appearance.getChildNodes();
				        			for (int j=0; j< features.getLength(); j++){
				        				if (features.item(j).getNodeName().equals("texture"))
				        					texture= features.item(j).getChildNodes().item(0).getNodeValue();
//				        				 // TODO wir nutzen nur noch eine farbe, daher kann die auflistung von ambient und Co entfallen				        				
				        				if (features.item(j).getNodeName().equals("color"))
				        					colors.put(features.item(j).getAttributes().getNamedItem("type").getNodeValue(),features.item(j).getChildNodes().item(0).getNodeValue());
				        				if (features.item(j).getNodeName().equals("clone"))
				        					clone= features.item(j).getChildNodes().item(0).getNodeValue();
				        			}
				        				   
				        			addAppearance(botConfig, item, colors, texture, clone);
				        		}
				        }
				}
			}
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+ex);
		}
		
		if (found == true)
			return botConfig;
		else 
			return null;
	}

	/**
	 * Erzeugt eine Appearnace und fuegt die der Liste hinzu
	 * @param appearances Die Hashmap in der das Pappearance eingetragen wird
	 * @param item Der Key, iunter dem diese Apperance abgelegt wird
	 * @param colors HashMap mit je Farbtyp und ASCII-Represenation der Farbe
	 * @param textureFile Der Name des Texture-Files
	 * @param clone Referenz auf einen schon bestehenden Eintrag, der geclonet werden soll
	 */
	@SuppressWarnings("unchecked")
	private void addAppearance(HashMap appearances, String item, HashMap colors, String textureFile, String clone){
		
		if (clone != null){
			appearances.put(item, appearances.get(clone));
			return;
		}
		
		Appearance appearance = new Appearance();
		
		if (colors != null){
			Material mat = new Material();

			Iterator it = colors.keySet().iterator();
			while (it.hasNext()) {
				String colorType = (String)it.next();
				String colorName = (String)colors.get(colorType);

				// TODO wir nutzen nur noch eine farbe, daher kann die auflistung von ambient und Co entfallen
				if (colorType.equals("ambient"))
					appearance.setColoringAttributes(new ColoringAttributes(new Color3f(Color.decode(colorName)), ColoringAttributes.FASTEST));
			}
			appearance.setMaterial(mat);

			
		}
				
		if (textureFile != null){
			TexCoordGeneration tcg = new TexCoordGeneration(
					TexCoordGeneration.OBJECT_LINEAR,
					TexCoordGeneration.TEXTURE_COORDINATE_3, 
					new Vector4f(1.0f, 1.0f, 0.0f, 0.0f),
					new Vector4f(0.0f, 1.0f, 1.0f, 0.0f), 
					new Vector4f(1.0f, 0.0f, 1.0f, 0.0f));
			appearance.setTexCoordGeneration(tcg);

			try {
				TextureLoader loader = new TextureLoader(ClassLoader.getSystemResource(textureFile), null);
				Texture2D texture = (Texture2D) loader.getTexture();
				texture.setBoundaryModeS(Texture.WRAP);
				texture.setBoundaryModeT(Texture.WRAP);
				appearance.setTexture(texture);
			} catch (Exception ex) {
				ErrorHandler.error("Textur: "+textureFile+"nicht gefunden "+ex);
			}
			
		}
		
		appearances.put(item,appearance);
	}

	
	
	
	/**
	 * Basisklasse, die auf einem TCP-Port lauscht
	 * 
	 * @author bbe (bbe@heise.de)
	 * 
	 */
	private abstract class SocketListener extends Thread {
		int port = 0;

		boolean listen = true;

		int num = 0;

		/**
		 * Eroeffnet einen neuen Lauscher
		 * 
		 * @param port
		 *            Port zum Lauschen
		 */
		public SocketListener(int port) {
			super();
			this.port = port;
		}

		/**
		 * Kuemmert sich um die Bearbeitung eingehnder Requests
		 */
		public abstract void run();
	}

	/**
	 * Lauscht auf einem Port und instanziiert dann einen Bot
	 * 
	 * @author bbe (bbe@heise.de)
	 * 
	 */
	private class BotSocketListener extends SocketListener {
		public BotSocketListener(int port) {
			super(port);
		}

		public void run() {
			Bot bot = null;
			TcpConnection tcp = null;
			try {
				ServerSocket server = new ServerSocket(port);
				while (listen) {
					bot = null;
					tcp = new TcpConnection();
					/*
					 * Da die Klasse TcpConnection staendig den ServerSocket neu
					 * aufbaut und wieder zerstoert, wird die eingebaute Methode
					 * listen() uebergangen und der Socket selbst uebergeben. Da
					 * die TcpConncetion den ServerSocket aber auch nicht wieder
					 * frei gibt, wurde der urspruenglich vorhandene Aufruf
					 * gaenzlich entfernt.
					 */
					tcp.connect(server.accept());

					Command cmd = new Command();
					try {
						while (bot == null) {
							if (cmd.readCommand(tcp) == 0) {
								System.out.print("Seq: " + cmd.getSeq()
										+ " CMD: " + cmd.getCommand() + "SUB: "
										+ cmd.getSubcommand() + "\n");
								if (cmd.getCommand() == Command.CMD_WELCOME) {
									if (cmd.getSubcommand() == Command.SUB_WELCOME_REAL) {
										bot = new CtBotRealCon(Controller.this,
												new Point3f(0.5f, 0f, 0f),
												new Vector3f(1.0f, -0.5f, 0f),
												tcp);
										System.out.print("Real Bot comming up");
									} else {
										bot = new CtBotSimTcp(Controller.this,
												new Point3f(0.5f, 0f, 0f),
												new Vector3f(1.0f, -0.5f, 0f),
												tcp);
										System.out
												.print("Virtual Bot comming up");
									}
								} else {
									System.out
											.print("Non-Welcome-Command found: \n"
													+ cmd.toString()
													+ "\n ==> Bot is already running or deprecated bot \nDefaulting to virtual Bot\n");
									// TODO Strenger pruefen und alle nicht
									// ordentlich angemeldeten Bots abweisen!
									bot = new CtBotSimTcp(Controller.this,
											new Point3f(0.5f, 0f, 0f),
											new Vector3f(1.0f, -0.5f, 0f), tcp);
								}
							} else
								System.out.print("Broken Command found: \n");

						}
					} catch (IOException ex) {
						ErrorHandler
								.error("TCPConnection broken - not possibple to connect: "
										+ ex);
					}

					
					if (bot != null) {
						addBot(bot);
					} else
						try {
							tcp.disconnect();
						} catch (Exception ex) {
						}

				}
			} catch (IOException ioe) {
				System.err.format("Kann nicht an port %d binden.", new Integer(
						port));
				System.err.println(ioe.getMessage());
			}
		}

	}

	/**
	 * Lauscht auf einem Port und versorgt dann eine View mit den initialen
	 * Daten
	 * 
	 * @author bbe (bbe@heise.de)
	 */
	private class ViewSocketListener extends SocketListener {
		/**
		 * Instanziiere den Listener
		 * 
		 * @param port
		 */
		public ViewSocketListener(int port) {
			super(port);
		}

		/**
		 * Lauscht auf eine eingehende Verbindung.
		 */
		public void run() {
			TcpConnection tcp = null;
			try {
				ServerSocket server = new ServerSocket(port);
				while (listen) {
					tcp = new TcpConnection();
					tcp.connect(server.accept());

					RemoteView rV = new RemoteView(tcp);
					rV.init(world.exportSceneGraph());
					remoteViews.add(rV);

				}
			} catch (IOException ioe) {
				System.err.format("Kann nicht an port %d binden.", new Integer(
						port));
				System.err.println(ioe.getMessage());
			}
		}
	}

}