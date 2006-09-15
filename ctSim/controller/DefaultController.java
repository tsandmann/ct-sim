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

import java.io.File;
import java.io.IOException;
import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.ConfigManager;
import ctSim.Connection;
import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.model.Command;
import ctSim.model.ParcoursGenerator;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.rules.Judge;
import ctSim.view.View;
import ctSim.view.gui.Debug;

/**
 * Zentrale DefaultController-Klasse des c't-Sim
 */
public final class DefaultController implements Runnable, Controller {
	private SocketListener botListener;
	
	private Thread ctrlThread;
	private volatile boolean pause;
	
	private CountDownLatch startSignal, doneSignal;
	
	private Judge judge;
	private View view;
	private World world;
	
	/** Anzahl der Bots im System */
	private HashMap<String,Integer> numberBots = new HashMap<String,Integer>();	
	
	protected DefaultController() {
		this.pause = true;
		setJudge(ConfigManager.getValue("judge"));
		this.startBotListener();
	}
	
	public void setView(View view) {
		this.view = view;
		BotManager.setView(view);
		init();
	}
	
	private void init() {
		try {
			String parcFile = ConfigManager.getValue("parcours");
			openWorldFromFile(new File(parcFile));
		} catch(NullPointerException e) {
			ErrorHandler.error("Keine Welt vorgesehen.");
		} catch(Exception e) {
			ErrorHandler.error("Probleme beim Instanziieren der Welt: "+e);
		}
		
		try {
			String botBin = ConfigManager.path2Os(
					ConfigManager.getValue("botbinary"));
			if (botBin == null)
				ErrorHandler.error("Kein Bot vorgesehen.");
			invokeBot(botBin);
		} catch(Exception e) {
			ErrorHandler.error("Probleme beim Ausfuehren des Bot: "+e);
		}
	}
	
	private void start() {
		if(this.world == null)
			return;
		
		this.ctrlThread = new Thread(this, "DefaultController");
		this.ctrlThread.start();
	}
	
	/**
	 * Haelt den DefaultController an
	 */
	public void stop() {
		Thread dummy = this.ctrlThread;
		
		if(dummy == null)
			return;
		
		this.ctrlThread = null;
		
		if(!dummy.getState().equals(State.TERMINATED))
			dummy.interrupt();
	}
	
	/** 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Thread thisThread = Thread.currentThread();
		
		startSignal = new CountDownLatch(1);
		doneSignal = new CountDownLatch(BotManager.getSize());
		
		while(this.ctrlThread == thisThread) {
			try {
			
				// Warte, bis alle Bots fertig sind und auf die nächste Aktualisierung warten
				// breche ab, wenn die Bots zu lange brauchen !
				if(!doneSignal.await(10000, TimeUnit.MILLISECONDS))
					Debug.out.println("Bot-Probleme: Ein oder mehrere Bots waren (viel) zu lansgam...");
				
				CountDownLatch startSig = this.startSignal;
				
				// Judge pruefen:
				judge.update(world.getSimulTime());
				// Update World
				view.update(world.getSimulTime());
				
				if(this.pause) {
					synchronized(this) {
						wait();
					}
				}
				
				// Die ganze Simulation aktualisieren
				world.updateSimulation();
				
				// Add/Start new Bot
				// + neue Runde einleuten (CountDownLatches neu setzen)
				startBots();
				
				// Alle Bots wieder freigeben
				startSig.countDown();
				
				Thread.sleep(this.world.getSimStepIntervalInMs());
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		cleanup();
	}
	
	private synchronized void cleanup() {
		BotManager.reinit();
	}
	
	private synchronized void startBots() {
		this.doneSignal  = new CountDownLatch(BotManager.getNewSize());
		this.startSignal = new CountDownLatch(1);
		
		BotManager.startNstopBots();
	}
	
	/**	 
	 * @throws InterruptedException
	 */
	public void waitOnController() throws InterruptedException {
		CountDownLatch doneSig = this.doneSignal;
		CountDownLatch startSig = this.startSignal;
		
		doneSig.countDown();
		startSig.await();
	}
	
	/**
	 * Laesst DefaultController pausieren
	 */
	
	public void pause() {
		this.pause = true;
	}
	
	/**
	 * Beendet die Pause
	 */	
	public synchronized void unpause() {
		if(this.world == null)
			return;
		
		if(this.ctrlThread == null)
			this.start();
		
		if(this.pause && this.judge.isStartAllowed()) {
			this.pause = false;
			this.notify();
		}
	}
	
	private void setWorld(World world) {
		this.pause();
		
		BotManager.reset();
		BotManager.setWorld(world);
		
		this.world = world;
		judge.setWorld(world);
		
		view.openWorld(world);
		Debug.out.println("Neue Welt geoeffnet.");
	}
	
	/* (non-Javadoc)
     * @see ctSim.controller.Controller#closeWorld()
     */
	public void closeWorld() {
		this.reset();
		this.world = null;
	}
	
	public void reset() {
		this.start();
		this.pause = false;
		this.stop();
		this.judge.reinit();
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.pause();
	}
	
	// TODO
	void startBotListener() {
		/*
		 * Der Sim sollte auch auf die TCP-Verbindung lauschen, wenn es Testbots
		 * gibt. Aus diesem Grund ist der thread, der auf TCP-Verbindungen
		 * lauscht, an dieser Stelle eingebaut.
		 */

		int p = 10001;
		
		try {
			String port = ConfigManager.getValue("botport"); //$NON-NLS-1$
			p = new Integer(port).intValue();
		} catch(NumberFormatException nfe) {
			nfe.printStackTrace();
		}
		
		System.out.println("Warte auf Verbindung vom c't-Bot auf Port "+p); //$NON-NLS-1$
		this.botListener = new BotSocketListener(p);
		this.botListener.start();
	}
	
	/**
	 * Basisklasse, die auf einem TCP-Port lauscht
	 * 
	 * @author bbe (bbe@heise.de)
	 */
	private abstract class SocketListener extends Thread {
		int port = 0;

		boolean listen = true;

		int num = 0;

		/**
		 * Eroeffnet einen neuen Lauscher
		 * 
		 * @param p Port zum Lauschen
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
		 * Laesst den DefaultController "sterben"
		 */
		public void die() {
			this.listen = false;
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
	 * Bennent einen neuen Bot
	 * Achtung, die Namensvergabe wird nicht zurueckgesetzt, wenn ein bot stirbt
	 * @param type Typ des Bots, z.B. der Klassenname
	 * @return Name des neuen Bots
	 */
	public String getNewBotName(String type){
		String name;
		
		// Schaue nach, wieviele Bots von der Sorte wir schon haben
		Integer bots = numberBots.get(type);
		if (bots == null){
			bots = new Integer(0);
		}

		name=type +"_"+ bots.intValue();

		bots = new Integer(bots.intValue()+1);	// erhoehen
		numberBots.put(type, bots);				// sichern
		
		return name;
		
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
						if (cmd.getSubcommand() != Command.SUB_WELCOME_REAL) {
							String name = getNewBotName("ctSim.model.bots.CtBotSimTcp");
							bot = new CtBotSimTcp(this.world, name,
									new Point3d(0.5d, 0d, 0.075d),
									new Vector3d(1.0f, -0.5f, 0f),
									con);
							System.out.println("Virtueller Bot nimmt Verbindung auf"); //$NON-NLS-1$
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
		
		if (bot != null && this.world != null && this.judge.isAddAllowed()) {
			// TODO:
			addBot(bot);
			//this.ctSim.addBot(new BotInfo("CTest", bot, new DefBotPanel()));  //$NON-NLS-1$//$NON-NLS-2$
			//BotManager.addBot(new BotInfo("CTest", bot, new DefBotPanel()));
		} else
			try {
				con.disconnect();
			} catch (Exception ex) {
				// Wenn jetzt noch was schief geht interessiert es uns nicht mehr
			}
	}
	
	/**
	 * Fuegt der Welt einen neuen Bot der Klasse CtBotSimTest hinzu
	 */
	public void addTestBot() {
		String name = getNewBotName("CtBotSimTest");
		Bot bot = new CtBotSimTest(this.world, name, 
				new Point3d(0d, 0d, 0.075d), new Vector3d());
		addBot(bot);
	}
	
	public void invokeBot(File file) {
		invokeBot(file.getAbsolutePath());
	}
	
	public void invokeBot(String filename) {
		if (filename == null)
			return;
		
		if (! new File(filename).exists()) {
			System.err.println("Bot-Datei '"+filename+"' nicht gefunden");
			return;
		}
		
		System.out.println("Starte externen Bot: "+filename);
		try {
			Runtime.getRuntime().exec(filename);
		} catch (Exception e){
			System.err.println("Fehler beim Starten von Bot '"+filename+"':");
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private synchronized Bot addBot(Bot bot){
		if (bot != null && this.world != null && this.judge.isAddAllowed()) {
//			this.world.addBot(bot);
			
			// TODO: (?)
			//bot.start();
			
//			this.botsToStart.add(bot);
			
			bot.setController(this);
			bot.setWorld(this.world);
			
			BotManager.addBot(bot);
			
			return bot;
		}
		return null;
	}
	
	public int getParticipants() {
		return BotManager.getNewSize();
	}
	
	/**
     * @param judgeClassName Die Art des Schiedrichters zu setzen
     * Stellt sicher, dass immer ein sinnvoller Judge gesetzt ist
     */
    public void setJudge(String judgeClassName) {
   		//$$ wenn ein spiel laeuft: throw new IllegalStateException("Aendern des Judge ist waehrend eines Spiels nicht moeglich")
    	try {
    		Class<?> cl = Class.forName(judgeClassName);
    		Constructor<?> c = cl.getConstructor(this.getClass());
    		judge = (Judge)c.newInstance(this);
    	} catch(ClassNotFoundException e) {
    		System.err.println("Die Judge-Klasse '"+judgeClassName+
    				"' wurde nicht gefunden: ");
    		e.printStackTrace();
    	} catch (Exception e) {
    		ErrorHandler.error("Probleme beim Instanziieren der Judge-Klasse: "+e); //$NON-NLS-1$
    	}
    }

	public String getJudge() {
		return judge.getClass().getCanonicalName();
	}
	
	public void openWorldFromFile(File sourceFile) {
		// TODO: Wenn kein DTD-file gegeben, besser Fehlermeldung!
		try {
			setWorld(World.buildWorldFromFile(sourceFile));
		} catch (Exception e) {
			ErrorHandler.error("Probleme beim Parsen der Parcours-Datei "+
					sourceFile.getAbsolutePath());
			e.printStackTrace();
		}
	}
	
	public void openWorldFromXmlString(String parcoursAsXml) {
		try {
			setWorld(World.buildWorldFromXmlString(parcoursAsXml));
		} catch (Exception e) {
			ErrorHandler.error("Probleme beim Parsen des Parcours");
			e.printStackTrace();
		}
	}
	
	public void openRandomWorld() {
		try {
			String p = ParcoursGenerator.generateParc();
			Debug.out.println("Parcours generiert");
			openWorldFromXmlString(p);
		} catch (Exception e) {
			Debug.out.println("Probleme beim Oeffnen des generierten " +
					"Parcours.");
			e.printStackTrace();
		}
	}

	//TODO Doku genauer: "Menge der Bots"? Menge *welcher* Bots?
	/** Liefert die Menge der Bots.
	 * Methode ist nicht f&uuml;r performance-kritische Punkte gedacht, da 
	 * einiges Herumeiern stattfindet.
	 */
	public Set<Bot> getBots() {
		return BotManager.getBots();
	}
}