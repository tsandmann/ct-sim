package ctSim.controller;

import java.awt.Color;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
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
import ctSim.model.bots.CtBot;
import ctSim.model.Command;
import ctSim.model.bots.CtBotSimTcp;
import ctSim.model.bots.CtBotRealCon;
import ctSim.model.bots.CtBotSimTest;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.view.BotInfo;
import ctSim.view.CtSimFrame;
import ctSim.view.DefBotPanel;

public class Controller implements Runnable {
	
	private Thread ctrlThread;
	private volatile boolean pause;
	private boolean run = true;
	private long tickRate;
	// TODO: CyclicBarrier (?)
	private CountDownLatch startSignal, doneSignal;
	
	private CtSimFrame ctSim;
	private World world;
	private List<Bot> botList, botsToStart;
	
	private Controller(CtSimFrame ctSim, World world) {
		
		this.ctSim = ctSim;
		this.world = world;
		this.botList = new ArrayList<Bot>();
		this.botsToStart = new ArrayList<Bot>();
		
		this.pause = true;
		this.tickRate = 500;
	}
	
	private void start() {
		
		this.ctrlThread = new Thread(this, "Controller");
		
		this.ctrlThread.start();
	}
	
	public static Controller start(CtSimFrame frame, World world) {
		
		Controller ctrl = new Controller(frame, world);
		
		//TODO:
		ctrl.startBotListener();
		
		ctrl.start();
		
		return ctrl;
	}
	
	public void stop() {
		
		Thread dummy = this.ctrlThread;
		this.ctrlThread = null;
		dummy.interrupt();
	}
	
	public void run() {
		Thread thisThread = Thread.currentThread();
		
		this.startSignal = new CountDownLatch(1);
		this.doneSignal = new CountDownLatch(this.botList.size());
		
		long t = 0;
		
		while(this.ctrlThread == thisThread) {
			try {
				//System.out.println("Rein: "+this.doneSignal.getCount()+" / "+this.botList.size());
				this.doneSignal.await();
				//System.out.println("Rein: "+this.doneSignal.getCount()+" / "+this.botList.size());
				CountDownLatch startSig = this.startSignal;
				
				// Update World
				// TODO: ganz dirty!
				this.ctSim.update(t);
				// Update GUI
				t = this.world.increaseSimulTime();
				
				// TODO: Vor Bots adden?
				if(this.pause) {
					synchronized(this) {
						wait();
					}
				}
				
				// Add/Start new Bot
				// + neue Runde einleuten (CountDownLatch neu setzen)
				startBots();
				
				// Start a new round (-> Starten der neuen Bots)
				//this.doneSignal = new CountDownLatch(this.botList.size());
				//this.startSignal = new CountDownLatch(1);
				
				//System.out.println("Raus: "+this.doneSignal.getCount()+" / "+this.botList.size());
				
				// vor sleep?
				startSig.countDown();
				
				Thread.sleep(this.tickRate);
				
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
			b.interrupt();
		
		this.world.cleanup();
	}
	
	public void setTickRate(long rate) {
		
		this.tickRate = rate;
	}
	
	private synchronized void startBots() {
		
		// TODO: Shuold be a set?
		for(Bot b : this.botsToStart)
			this.botList.add(b);
		
		this.doneSignal = new CountDownLatch(this.botList.size());
		this.startSignal = new CountDownLatch(1);
		
		for(Bot b : this.botsToStart) {
			b.start();
			// TODO:
			((CtBot)b).setSensRc5(CtBot.RC5_CODE_RED);
			System.out.println("Bot gestartet: "+b.getName());
		}
		
		this.botsToStart = new ArrayList<Bot>();
	}
	
	public void waitOnController() throws InterruptedException {
		
		CountDownLatch doneSig = this.doneSignal;
		CountDownLatch startSig = this.startSignal;
		
		doneSig.countDown();
		
		startSig.await();
	}
	
	public void pause() {
		
		this.pause = true;
	}
	
	public synchronized void unpause() {
		
		if(this.pause) {
			this.pause = false;
			this.notify();
		}
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

		//port = config.get("botport");
		int p = 10001;
		
		//if (port != null){
			try {
				//int p = new Integer(port).intValue();
				
				System.out.println("Warte auf Verbindung vom c't-Bot auf Port "+p);
				SocketListener BotListener = new BotSocketListener(p);
				BotListener.start();
				
			} catch (Exception ex) {
				ErrorHandler.error("Kann den botPort ("+p+ ") nicht dekodieren "+ex);
			}
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
		@Override
		public abstract void run();
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
		 * @param port Der TCP-Port auf dem er horcht
		 */
		public BotSocketListener(int port) {
			super(port);
		}

		/**
		 * Diese Routine wartet auf Bots
		 */
		@Override
		public void run() {
			TcpConnection tcp = null;
			try {
				ServerSocket server = new ServerSocket(port);
				
				while (listen) {
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
					System.out.println("Incomming Connection on Bot-Port");
					
					addBot(tcp);
				}
			} catch (IOException ioe) {
				System.err.format("Kann nicht an port %d binden.", new Integer(
						port));
				System.err.println(ioe.getMessage());
			}
		}
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
				System.out.println("Waiting for Welcome-String...");
				
				if (cmd.readCommand(con) == 0) {
					
					if (cmd.getCommand() == Command.CMD_WELCOME) {
						if (cmd.getSubcommand() == Command.SUB_WELCOME_REAL) {
							bot = new CtBotRealCon(
									new Point3f(0.5f, 0f, 0f),
									new Vector3f(1.0f, -0.5f, 0f),
									con);
							System.out.println("Real Bot comming up");
						} else {
							bot = new CtBotSimTcp(
									new Point3f(0.5f, 0f, 0f),
									new Vector3f(1.0f, -0.5f, 0f),
									con);
							System.out.println("Virtual Bot comming up");
						}
					} else {
						System.out.print("Non-Welcome-Command found: \n"
										+ cmd.toString()+ "\n"+
										" ==> Bot is already running or deprecated bot \n"+
										"Sending Welcome again\n");
						// Hallo sagen
						con.send((new Command(Command.CMD_WELCOME, 0,	0, 0)).getCommandBytes());
					}
				} else
					System.out.print("Broken Command found: \n");

			}
		} catch (IOException ex) {
			ErrorHandler.error("TCPConnection broken - not possible to connect: " + ex);
		}
		
		if (bot != null) {
			// TODO:
			addBot(bot);
			this.ctSim.addBot(new BotInfo("Test"+Math.round(Math.random()*10), "CTest", bot, new DefBotPanel()));
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
	 * @param name Name des Bots
	 */
	public Bot addBot(String type) {
		
		Bot bot = null;
		
		if (type.equalsIgnoreCase("CtBotSimTest")) {
			bot = new CtBotSimTest(new Point3f(), new Vector3f());
		}
		
		if (type.equalsIgnoreCase("CtBotRealJD2XX")) {
			Connection com = waitForJD2XX();
			if (com != null) {
				bot = new CtBotRealCon(new Point3f(), new Vector3f(), com);
				System.out.println("Real Bot via JD2XX startet");
			}
		}
		return addBot(bot);
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
	
	private synchronized Bot addBot(Bot bot){
		
		if (bot != null) {
			
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
			
			this.world.addBot(bot);
			
			// TODO: (?)
			//bot.start();
			
			this.botsToStart.add(bot);
			
			bot.setController(this);
			
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
			System.out.println("Lade Bot-Aussehen aus: "+configFile);

			parser.parse(configFile);
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
				        			
				        			HashMap<String,String> colors = new HashMap<String,String>();
				        			
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
	
	public static void main(String[] args) {
		
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
		
		CtSimFrame ctSim = new CtSimFrame("CtSim");
		
		ctSim.setVisible(true);
	}
}
