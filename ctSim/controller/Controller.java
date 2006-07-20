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

package ctSim.controller;

import java.awt.Color;
import java.io.IOException;
import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.VirtualUniverse;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4f;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.j3d.utils.image.TextureLoader;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import ctSim.CmdExec;
import ctSim.Connection;
import ctSim.ErrorHandler;
import ctSim.JD2xxConnection;
import ctSim.TcpConnection;
// import ctSim.model.bots.ctbot.CtBot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.Command;
import ctSim.model.World;
import ctSim.model.bots.Bot;
//import ctSim.model.bots.ctbot.CtBotRealCon;
//import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.rules.DefaultJudge;
import ctSim.model.rules.Judge;
import ctSim.view.BotInfo;
import ctSim.view.CtSimFrame;
import ctSim.view.DefBotPanel;

/**
 * Zentrale Controller-Klasse des c't-Sim
 *
 */
public final class Controller implements Runnable {
	
	/**
	 * Enthaelt die gesamten Einzelparameter der Konfiguration 
	 */
	private HashMap<String,String> config = new HashMap<String,String>();
	
	private static final String CONFIGFILE = "config/ct-sim.xml"; //$NON-NLS-1$
	
	private SocketListener botListener;
	
	private Thread ctrlThread;
	private volatile boolean pause;
	//private boolean run = true;
//	private long tickRate;
	// TODO: CyclicBarrier (?)
	private CountDownLatch startSignal, doneSignal;
	
	private Judge judge;
	private CtSimFrame ctSim;
	private World world;
	private List<Bot> botList, botsToStart;
	
	private Controller(CtSimFrame ctS, World w) {
		
		this.ctSim = ctS;
		this.world = w;
		this.botList = new ArrayList<Bot>();
		this.botsToStart = new ArrayList<Bot>();
		
		this.pause = true;
//		this.tickRate = 500;
		
		init();
	}
	
	private void init() {
		
		try {
			parseConfig();
			
		} catch (Exception ex){
			ErrorHandler.error("Einlesen der c't-Sim-Konfiguration nicht moeglich. Abburch!"); //$NON-NLS-1$
			return;
		}
		
		try {
			
			Class<?> cl = Class.forName(this.config.get("judge")); //$NON-NLS-1$
			
			Constructor<?> c = cl.getConstructor(this.getClass(), this.world.getClass());
			
			this.judge = (Judge) c.newInstance(this, this.world);
			
			//judge = (Judge) Class.forName(config.get("judge")).newInstance();
			//judge.setController(this);
			//judge.start();
			//controlFrame.addJudge(judge);
			
		} catch(ClassNotFoundException e) {
			ErrorHandler.error("Die Judge-Klasse wurde nicht gefunden: "+e); //$NON-NLS-1$
		} catch(SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NoClassDefFoundError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NullPointerException e) {
			ErrorHandler.error("Kein Schiedsrichter vorgesehen.");				 //$NON-NLS-1$
		} catch(Exception e) {
			ErrorHandler.error("Probleme beim Instantiieren der Judge-Klasse: "+e); //$NON-NLS-1$
		}
	}
	
	private void start() {
		
		if(this.judge == null) {
			
			this.judge = new DefaultJudge(this, this.world);
		}
		
		this.ctrlThread = new Thread(this, "Controller"); //$NON-NLS-1$
		
		this.ctrlThread.start();
	}
	
	/**
	 * Startet den Controller
	 * @param frame Der Rahmen zur Anzeige der Simulator-GUI
	 * @param world Die Welt
	 * @return Der Controller
	 */
	public static Controller start(CtSimFrame frame, World world) {
		
		Controller ctrl = new Controller(frame, world);
		
		//TODO:
		ctrl.startBotListener();
		
		ctrl.start();
		
		return ctrl;
	}
	
	/**
	 * Haelt den Controller an
	 */
	public void stop() {
		
		if(this.botListener != null && !this.botListener.equals(State.TERMINATED))
			this.botListener.die();
		
		Thread dummy = this.ctrlThread;
		this.ctrlThread = null;
		while(!dummy.getState().equals(State.TERMINATED))
			dummy.interrupt();
	}
	
	/** 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Thread thisThread = Thread.currentThread();
		
		startSignal = new CountDownLatch(1);
		doneSignal = new CountDownLatch(botList.size());
			
		// TODO bitte den Zeit-Thread evtl. wieder in die Welt zurück verschieben
		while(this.ctrlThread == thisThread) {
			try {
				long realTimeBegin = world.getRealTime();

				//System.out.println("Rein: "+this.doneSignal.getCount()+" / "+this.botList.size());
				
				// Judge pruefen:
				judge.update(world.getSimulTime());
				// Update World
				// TODO: ganz dirty!
				ctSim.update(world.getSimulTime());
								
				// TODO: Vor Bots adden?
				if(this.pause) {
					synchronized(this) {
						wait();
					}
				}
				
			
//				ctSim.getWorldView().getWorldCanvas().stopRenderer();
				// Die ganze Simulation aktualisieren
				world.updateSimulation();
				
				// Add/Start new Bot
				// + neue Runde einleuten (CountDownLatch neu setzen)
				startBots();
				
				// Start a new round (-> Starten der neuen Bots)
				//this.doneSignal = new CountDownLatch(this.botList.size());
				//this.startSignal = new CountDownLatch(1);
				
				//System.out.println("Raus: "+this.doneSignal.getCount()+" / "+this.botList.size());
				
				long simTime = (world.getRealTime()-realTimeBegin);
				
				// DoneSignal vorbereiten
				doneSignal = new CountDownLatch(this.botList.size());
				
//				System.out.println("Release AliveObstacles");
				// Alle Bots wieder freigeben
				startSignal.countDown();
				// und startsignal wieder scharf machen
				startSignal = new CountDownLatch(1);
				
				
				// Warte, bis alle Bots fertig sind und auf die nächste Aktualisierung warten
				// breche ab, wenn die Bots zu lange brauchen !
//				doneSignal.await();
				if (!doneSignal.await(world.getBaseTimeVirtual(), TimeUnit.MILLISECONDS))
					System.out.println("Bots hatten Timeout nach: "+world.getBaseTimeVirtual()+" ms");
//				else
//					System.out.println("AliveObstacles done");
				
//				ctSim.getWorldView().getWorldCanvas().startRenderer();
//				long waitTime = (world.getRealTime()-realTimeBegin) - simTime;
				
				long elapsedTime= world.getRealTime()-realTimeBegin;
				long timeToSleep = world.getBaseTimeReal() - elapsedTime;
				if ( timeToSleep >=0)
					Thread.sleep(timeToSleep);
				else {
					System.out.println("Zyklus braucht " +elapsedTime+" ms (Sim="+simTime+" ms)" + "Zeitfenster ist aber nur "+world.getBaseTimeReal()+" ms ==> kein sleep");
					//		""+ -timeToSleep + "ms laenger als baseTimeReal! ==> no sleep");
					//ErrorHandler.error
				}
				
				//System.out.println("Zyklus brauchte: "+ (world.getRealTime()-realTimeBegin) +" ms. ("+simTime+" ms simul, "+waitTime+" ms wait, " +timeToSleep+" ms sleep)");
				//System.out.println("Raus: "+this.doneSignal.getCount()+" / "+this.botList.size());
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		cleanup();
	}
	
	private synchronized void cleanup() {
		
		for(Bot b : this.botList)
			//b.interrupt();
			b.stop();
		
		// TODO:
		//this.world.cleanup();
	}
	
	private synchronized void startBots() {
		
		// TODO: Shuold be a set?
		for(Bot b : this.botsToStart)
			this.botList.add(b);
				
		for(Bot b : this.botsToStart) {
			b.start();
			// TODO:
			//((CtBot)b).setSensRc5(CtBot.RC5_CODE_5);
			System.out.println("Bot gestartet: "+b.getName()); //$NON-NLS-1$
		}
		
		this.botsToStart = new ArrayList<Bot>();
	}
	
	/**	 
	 * @throws InterruptedException
	 */
	public void waitOnController() throws InterruptedException {
		// liefer ein Done ab
		doneSignal.countDown();
		
		// und warte auf die erlaubnis weiterzu machen
		startSignal.await();
	}
	
	/**
	 * Laesst Controller pausieren
	 */
	public void pause() {
		
		this.pause = true;
	}
	
	/**
	 * Beendet die Pause
	 */	
	public synchronized void unpause() {
//		System.out.println("Da bin ich doch...");
		if(this.pause && this.judge.isStartAllowed()) {
			this.pause = false;
			this.notify();
		}
	}
	
	/**
	 * @param judgeClass Die Art des Schiedrichters zu setzen
	 * @return true, falls alles geklappt hat
	 */
	public boolean setJudge(String judgeClass) {
		
		try {
			Class<?> cl = Class.forName(judgeClass);
			Constructor<?> c = cl.getConstructor(this.getClass(), this.world.getClass());
			
			return this.setJudge((Judge) c.newInstance(this, this.world));
			
		} catch(ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch(NoClassDefFoundError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * @param j Der Schiedsrichter zu setzen
	 * @return true, wenn alles geklappt hat
	 */
	public boolean setJudge(Judge j) {
		
		if(this.judge == null || this.judge.getTime() == 0) {
			this.judge = j;
			return true;
		}
		return false;
	}
	
	// TODO:
//	public void reset() {
//		
//	}
	
	// TODO
	void startBotListener() {
		
		/*
		 * Der Sim sollte auch auf die TCP-Verbindung lauschen, wenn es Testbots
		 * gibt. Aus diesem Grund ist der thread, der auf TCP-Verbindungen
		 * lauscht, an dieser Stelle eingebaut.
		 */

		int p = 10001;
		
		try {
			String port = this.config.get("botport"); //$NON-NLS-1$
			p = new Integer(port).intValue();
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		}
		
		System.out.println("Warte auf Verbindung vom c't-Bot auf Port "+p); //$NON-NLS-1$
		this.botListener = new BotSocketListener(p);
		this.botListener.start();
		
		//} else {
		//	ErrorHandler.error("Kein botPort in der Config-Datei gefunden. Es wird nicht auf Bots gelauscht!");			
		//}
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
		 * @param p
		 *            Port zum Lauschen
		 */
		public SocketListener(int p) {
			super();
			this.port = p;
		}

		/**
		 * Kuemmert sich um die Bearbeitung eingehnder Requests
		 */
		@Override
		public abstract void run();
		
		/**
		 * Laesst den Controller "sterben"
		 */
		public void die() {
			this.listen = false;
			//this.interrupt();
		}
	}
	
	/**
	 * Lauscht auf einem Port und instanziiert dann einen Bot
	 * 
	 * @author bbe (bbe@heise.de)
	 * 
	 */
	private class BotSocketListener extends SocketListener {
		/**
		 * Konstruktor
		 * @param p Der TCP-Port auf dem er horcht
		 */
		public BotSocketListener(int p) {
			super(p);
		}

		/**
		 * Diese Routine wartet auf Bots
		 */
		@Override
		public void run() {
			TcpConnection tcp = null;
			try {
				ServerSocket server = new ServerSocket(this.port);
				
				while (this.listen) {
					tcp = new TcpConnection();
					/*
					 * Da die Klasse TcpConnection staendig den ServerSocket neu
					 * aufbaut und wieder zerstoert, wird die eingebaute Methode
					 * listen() uebergangen und der Socket selbst uebergeben. Da
					 * die TcpConncetion den ServerSocket aber auch nicht wieder
					 * frei gibt, wurde der urspruenglich vorhandene Aufruf
					 * gaenzlich entfernt.
					 */
					try {
					server.setSoTimeout(1000);
					tcp.connect(server.accept());
					System.out.println("Eingehende Verbindung auf dem Bot-Port"); //$NON-NLS-1$
					
					addBot(tcp);
					} catch(SocketTimeoutException e) {
						// Noch nicht implementiert
					}
				}
			} catch (IOException ioe) {
				System.err.format("Kann nicht an port %d binden.", new Integer( //$NON-NLS-1$
						this.port));
				System.err.println(ioe.getMessage());
			}
		}
	}
	
	/**
	 * @return Die Anzahl der Bots
	 */
	public int getParticipants() {
		
		return this.botList.size()+this.botsToStart.size();
	}
	
	/** 
	 * Fuegt einen Bot dazu, sobald die Connection steht
	 * @param con Die Verbindung
	 */
	public void addBot(Connection con) {
		Bot bot = null;
		Command cmd = new Command();
		try {
			// Hallo sagen
			con.send((new Command(Command.CMD_WELCOME, 0,	0, 0)).getCommandBytes());
			// TODO Timeout einfuegen!!
			while (bot == null) {
				System.out.println("Warte auf Willkommen..."); //$NON-NLS-1$
				
				if (cmd.readCommand(con) == 0) {
					
					if (cmd.getCommand() == Command.CMD_WELCOME) {
						if (cmd.getSubcommand() == Command.SUB_WELCOME_REAL) {
//							bot = new CtBotRealCon(
//									new Point3f(0.5f, 0f, 0f),
//									new Vector3f(1.0f, -0.5f, 0f),
//									con);
//							System.out.println("Real Bot comming up");
						} else {
							bot = new CtBotSimTcp(this.world, "Test C-Bot", //$NON-NLS-1$
									new Point3d(0.5d, 0d, 0.075d),
									new Vector3d(1.0f, -0.5f, 0f),
									con);
							System.out.println("Virtueller Bot nimmt Verbindung auf"); //$NON-NLS-1$
							//System.exit(0);  // <<------------------------ !!!!!!!!!!!!
						}
					} else {
						System.out.print("Bot ist nicht willkommen: \n" //$NON-NLS-1$
										+ cmd.toString()+ "\n"+ //$NON-NLS-1$
										" ==> Bot laeuft schon oder ist veraltet \n"+ //$NON-NLS-1$
										"Schicke Willkommen nochmals\n"); //$NON-NLS-1$
						// Hallo sagen
						con.send((new Command(Command.CMD_WELCOME, 0,	0, 0)).getCommandBytes());
					}
				} else
					System.out.print("Fehlerhaftes Kommando gefunden: \n"); //$NON-NLS-1$

			}
		} catch (IOException ex) {
			ErrorHandler.error("TCPConnection unterbrochen - Verbindung nicht moeglich: " + ex); //$NON-NLS-1$
		}
		
		if (bot != null && this.judge.isAddAllowed()) {
			// TODO:
			addBot(bot);
			this.ctSim.addBot(new BotInfo("Test"+Math.round(Math.random()*10), "CTest", bot, new DefBotPanel()));  //$NON-NLS-1$//$NON-NLS-2$
		} else
			try {
				con.disconnect();
			} catch (Exception ex) {
				// Wenn jetzt noch was schief geht interessiert es uns nicht mehr
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
	 * @return Der Bot
	 */
	public Bot addBot(String type) {
		
		Bot bot = null;
		
		if (type.equalsIgnoreCase("CtBotSimTest")) { //$NON-NLS-1$
			//bot = new CtBotSimTest(new Point3f(), new Vector3f());
			bot = new CtBotSimTest(this.world, "Test", new Point3d(0d, 0d, 0.075d), new Vector3d()); //$NON-NLS-1$
		}
		
//		if (type.equalsIgnoreCase("CtBotRealJD2XX")) {
//			Connection com = waitForJD2XX();
//			if (com != null) {
//				bot = new CtBotRealCon(new Point3f(), new Vector3f(), com);
//				System.out.println("Real Bot via JD2XX startet");
//			}
//		}
		return addBot(bot);
	}
	
	/**
	 * Startet einen externen Bot
	 * @param filename Pfad zum Binary des Bots
	 */
	public void invokeBot(String filename){
		
		System.out.println("Starte externen Bot: "+filename); //$NON-NLS-1$
		CmdExec executor = new CmdExec();
		try {
			executor.exec(filename);
		} catch (Exception ex){
			ErrorHandler.error("Kann Bot: "+filename+" nicht ausfuehren: "+ex);  //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	@SuppressWarnings("unchecked")
	private synchronized Bot addBot(Bot bot){
		
		if (bot != null && this.judge.isAddAllowed()) {
			
			// TODO Sinnvolle Zuordnung von Bot-Name zu Konfig
			HashMap botConfig = getBotConfig("config/ct-sim.xml",bot.getName()); //$NON-NLS-1$
			if (botConfig == null){
				ErrorHandler.error("Keine BotConfig fuer: "+bot.getName()+" in der XML-Config-Datei gefunden. Lade Defaults.");  //$NON-NLS-1$//$NON-NLS-2$
				botConfig = getBotConfig("config/ct-sim.xml","default");  //$NON-NLS-1$//$NON-NLS-2$
			}
			
			if (botConfig == null){
				ErrorHandler.error("Keine Default-BotConfig in der XML-Config-Datei gefunden. Starte ohne."); //$NON-NLS-1$
			}
			
			// TODO:
			bot.setAppearances(botConfig);
			
			this.world.addBot(bot);
			
			// TODO: (?)
			//bot.start();
			
			this.botsToStart.add(bot);
			
			bot.setController(this);
			bot.setWorld(this.world);
			
			
//			ctSim.getView().setMinimumFrameCycleTime(100);
//			System.out.println("dT="+ctSim.getView().getMinimumFrameCycleTime()+" ms");

			
			return bot;
			
//			controlFrame.addBot(bot);
//			// Dann wird der eigene Bot-Thread gestartet:
//			bot.start();
//			
//			controlFrame.addViewItem(bot.getName());
			
			
		}
		return null;
	}
	
	/*
	 * Wartet auf eine eingehende JD2XX-Verbindung
	 * @return
	 */
	@SuppressWarnings("unused")
	private Connection waitForJD2XX(){
		JD2xxConnection com = new JD2xxConnection();
		try {
			com.connect();
			System.out.println("JD2XX-Verbindung aufgebaut"); //$NON-NLS-1$
			return com;
		} catch (Exception ex) {
			ErrorHandler.error("JD2XX-Verbindung nicht moeglich: " + ex); //$NON-NLS-1$
			return null;
		}
	}
	
	/**
	 * Liest die Konfiguration des Sims ein
	 * @throws Exception
	 */
	private void parseConfig() throws Exception{
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			String configFile= ClassLoader.getSystemResource(CONFIGFILE).toString();
			System.out.println("Lade Konfiguration aus: "+configFile); //$NON-NLS-1$
			
			parser.parse(configFile);
			// umwandeln in ein Document
			Document doc = parser.getDocument();
			
			// Und Anfangen mit dem abarbeiten
			
			//als erster suchen wir uns den Parameter-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while (n != null){
				if (n.getNodeName().equals("parameter")){ //$NON-NLS-1$
					String name = n.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
					String value = n.getAttributes().getNamedItem("value").getNodeValue(); //$NON-NLS-1$
					this.config.put(name,value);
				}
				n=n.getNextSibling();
			}
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+ex); //$NON-NLS-1$
			throw ex;
		}
	}
	
	/*
	 * Liefert die Config zu einem Bot zurueck
	 * @param filename
	 * @param botId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private HashMap getBotConfig(String filename, String botId) {
		
		boolean found = false;
		
		HashMap botConfig = new HashMap();
		
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			String configFile= ClassLoader.getSystemResource(filename).toString();
			System.out.println("Lade Bot-Aussehen aus: "+configFile); //$NON-NLS-1$

			parser.parse(configFile);
			// umwandeln in ein Document
			Document doc = parser.getDocument();
			
			// Und Anfangen mit dem abarbeiten
			
			//als erster suchen wir uns den Parcours-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while ((n != null)&& (!n.getNodeName().equals("bots"))) { //$NON-NLS-1$
				n=n.getNextSibling();
			}

			//	Eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			NodeList bots=n.getChildNodes();	
	
			for(int b=0; b<bots.getLength()-1;b++){
				Node botSection = bots.item(b);
				// Ist das ueberhaupt ein Bot-Eintrag?
				if (botSection.getNodeName().equals("bot")) //$NON-NLS-1$
					// Und ist es auch der gesuchte
					if (botSection.getAttributes().getNamedItem("name").getNodeValue().equals(botId)){ //$NON-NLS-1$
						found=true;
						NodeList children = botSection.getChildNodes();
						
						//	HashMap mit den Apearances aufbauen
				        for (int i=0; i<children.getLength()-1; i++){
				        		Node appearance =children.item(i);
				        		if (appearance.getNodeName().equals("appearance")){ //$NON-NLS-1$
				        			// Zuerst den Type extrahieren
				        			String item = appearance.getAttributes().getNamedItem("type").getNodeValue(); //$NON-NLS-1$
				        			
				        			String texture = null;
				        			String clone = null;
				        			
				        			HashMap<String,String> colors = new HashMap<String,String>();
				        			
				        			NodeList features = appearance.getChildNodes();
				        			for (int j=0; j< features.getLength(); j++){
				        				if (features.item(j).getNodeName().equals("texture")) //$NON-NLS-1$
				        					texture= features.item(j).getChildNodes().item(0).getNodeValue();
//				        				 // TODO wir nutzen nur noch eine farbe, daher kann die auflistung von ambient und Co entfallen				        				
				        				if (features.item(j).getNodeName().equals("color")) //$NON-NLS-1$
				        					colors.put(features.item(j).getAttributes().getNamedItem("type").getNodeValue(),features.item(j).getChildNodes().item(0).getNodeValue()); //$NON-NLS-1$
				        				if (features.item(j).getNodeName().equals("clone")) //$NON-NLS-1$
				        					clone= features.item(j).getChildNodes().item(0).getNodeValue();
				        			}
				        				   
				        			addAppearance(botConfig, item, colors, texture, clone);
				        		}
				        }
				}
			}
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+ex); //$NON-NLS-1$
		}
		
		if (found == true)
			return botConfig;
		return null;
	}
	
	/*
	 * Erzeugt eine Appearnace und fuegt die der Liste hinzu
	 * @param appearances Die Hashmap in der das Pappearance eingetragen wird
	 * @param item Der Key, iunter dem diese Apperance abgelegt wird
	 * @param colors HashMap mit je Farbtyp und ASCII-Represenation der Farbe
	 * @param textureFile Der Name des Texture-Files
	 * @param clone Referenz auf einen schon bestehenden Eintrag, der geclonet werden soll
	 */
	private void addAppearance(HashMap<String,Appearance> appearances, String item, HashMap colors, String textureFile, String clone){
		
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
				if (colorType.equals("ambient")) //$NON-NLS-1$
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
				ErrorHandler.error("Textur: "+textureFile+"nicht gefunden "+ex); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		}
		
		appearances.put(item,appearance);
	}
	
	/**
	 * Hauptmethode
	 * @param args keine Argumente
	 */
	public static void main(String[] args) {
		VirtualUniverse.setJ3DThreadPriority(1);
		// TODO:
		// - args checken
		// - UIManager, Locale, ... setzen (?)
		// - Controller starten
		
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
		
		CtSimFrame ctSim = new CtSimFrame("CtSim"); //$NON-NLS-1$
		
		ctSim.setVisible(true);
	}

	/**
	 * @return Returns the world.
	 */
	public World getWorld() {
		return world;
	}
}