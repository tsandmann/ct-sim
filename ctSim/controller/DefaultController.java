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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.ConfigManager;
import ctSim.Connection;
import ctSim.TcpConnection;
import ctSim.model.Command;
import ctSim.model.ParcoursGenerator;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;
import ctSim.view.View;

//$$ Es passiert Mist, wenn TestBots hinzugefuegt werden, bevor ein Parcours existiert. Untersuchen.

/**
 * Zentrale Controller-Klasse des c't-Sim
 */
public class DefaultController implements Runnable, Controller {
	protected FmtLogger lg = FmtLogger.getLogger("ctSim.controller");

    private SocketListener botListener;

	private Thread ctrlThread;
	private volatile boolean pause;

	private CountDownLatch startSignal, doneSignal;

    private Judge judge;
    private View view;
    private World world;

    /** Anzahl der Bots im System */
    private HashMap<String,Integer> numberBots = new HashMap<String,Integer>();

    //$$ Idiotischerweise ist die hauptsaechliche Initialisierung hier
    public void setView(View view) {
	    this.pause = true;
        this.view = view;
        setJudge(ConfigManager.getValue("judge"));
        this.startBotListener();
        BotManager.setView(view);
        init();
    }

    private void init() {
        try {
            String parcFile = ConfigManager.getValue("parcours");
            openWorldFromFile(new File(parcFile));
        } catch(NullPointerException e) {
            lg.fine("Kein Standardparcours konfiguriert");
        } catch(Exception e) {
            lg.warning(e, "Problem beim Instanziieren des Standardparcours");
        }

        String botBin = ConfigManager.path2Os(
                ConfigManager.getValue("botbinary"));
        if (botBin == null)
            lg.fine("Kein Standardbot konfiguriert");
        else
            invokeBot(botBin);
    }

	private void start() {
		if(this.world == null)
			return;

		lg.fine("Initialisiere Sequencer");
		this.ctrlThread = new Thread(this, "ctSim/Sequencer");
		this.ctrlThread.start();
	}

    /**
	 * Haelt den DefaultController an
     */
	public void stop() {
		lg.fine("Terminieren des Sequencer angefordert");
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
		lg.fine("Sequencer gestartet");
		Thread thisThread = Thread.currentThread();

		startSignal = new CountDownLatch(1);
		doneSignal = new CountDownLatch(BotManager.getSize());

		while(this.ctrlThread == thisThread) {
	        try {
				// Warte, bis alle Bots fertig sind und auf die naechste
	        	// Aktualisierung warten
				// breche ab, wenn die Bots zu lange brauchen !
				if(! doneSignal.await(10, TimeUnit.SECONDS)) {
					lg.warn("Bot-Probleme: Ein oder mehrere Bots waren " +
							"viel zu langsam");
				}

				CountDownLatch oldStartSignal = this.startSignal;

				// Judge pruefen:
				if (judge.isSimulationFinished(world.getSimTimeInMs())) {
					lg.fine("Sequencer: Simulationsende");
					// Spiel ist beendet
					pause();
					// Alle Bots entfernen
					BotManager.reinit();
					view.onSimulationFinished();
				}

				// Update World
				//$$ Wieso wird das nochmal gemacht, wenn die Simulation (per Judge-Urteil) schon beendet wurde?
				view.onSimulationStep(world.getSimTimeInMs());

				if(this.pause) {
					lg.fine("Pause beginnt: Sequencer blockiert");
					synchronized(this) {
						wait();
					}
					lg.fine("Pause beendet: Sequencer freigegeben");
				}

				// Die ganze Simulation aktualisieren
				//$$ Warum _nach_ dem view.update()? Heisst das nicht, die Anzeige hinkt der Simulation immer um einen Schritt hinterher?
				world.updateSimulation();

				// Add/Start new Bot
				// + neue Runde einleuten
				// Ueberschreibt startSignal, aber wir haben mit oldStartSignal eine Referenz aufgehoben
				startBots();

				// Alle Bots wieder freigeben
				oldStartSignal.countDown();

				//$$ Falsch: Nicht die ganze Intervall-Zeit warten -- world.update() und Konsorten haben davon schon Zeit verbraucht, das muss einbezogen werden
				Thread.sleep(this.world.getSimStepIntervalInMs());

	        } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }
		cleanup();
		lg.fine("Sequencer beendet");
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
		lg.fine("Pause angefordert");
		this.pause = true;
	}

	/**
	 * Beendet die Pause
	 */
	public synchronized void unpause() {
		lg.fine("Pausenende angefordert");
		if(this.world == null)
			return;

		if(this.ctrlThread == null)
			this.start();

		if(this.pause && this.judge.isStartingSimulationAllowed()) {
			this.pause = false;
			this.notify();
		}
	}

	private void setWorld(World world) {
		this.pause();

		this.world = world;
        BotManager.reset();
        BotManager.setWorld(world);
        judge.setWorld(world);
        view.onWorldOpened(world);
        lg.info("Neue Welt ge\u00F6ffnet");
    }

    public void closeWorld() {
		this.reset();
		this.world = null;
    }

	//$$ Um Himmels Willen, was soll das denn sein?
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
        /* Der Sim sollte auch auf die TCP-Verbindung lauschen, wenn es Testbots
         * gibt. Aus diesem Grund ist der thread, der auf TCP-Verbindungen
         * lauscht, an dieser Stelle eingebaut. */
        int p = 10001; //$$ Default sollte hier weg und in ConfigManager umziehen

        try {
            p = Integer.parseInt(ConfigManager.getValue("botport"));
        } catch(NumberFormatException nfe) {
            lg.warning(nfe, "Problem beim Parsen der Konfiguration: " +
                    "Parameter 'botport' ist keine Ganzzahl");
        }

        lg.info("Warte auf Verbindung vom c't-Bot auf Port "+p);
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
            super("ctSim/SocketListener");
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
                        lg.fine("Eingehende Verbindung auf dem Bot-Port");
                        addBot(tcp);
                    } catch(SocketTimeoutException e) {
                        //TODO Noch nicht implementiert
                    }
                }
            } catch (IOException e) {
                lg.warning(e, "Kann nicht an Port "+port+" binden. " +
                        "M\u00F6glicherweise l\u00E4uft c't-Sim schon.");
            }
        }
    }

    /**
     * Bennent einen neuen Bot
     * Achtung, die Namensvergabe wird nicht zurueckgesetzt, wenn ein bot stirbt
     * @param type Typ des Bots, z.B. der Klassenname
     * @return Name des neuen Bots
     */
    public String getNewBotName(String type) {
        String name;

        // Schaue nach, wieviele Bots von der Sorte wir schon haben
        Integer bots = numberBots.get(type);
        if (bots == null)
            bots = 0;
        name=type +"_"+ bots;

        // erhoehen, sichern
        bots++;
        numberBots.put(type, bots);

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
                lg.fine("Warte auf Willkommen ...");

                if (cmd.readCommand(con) == 0) {

                    if (cmd.getCommand() == Command.CMD_WELCOME) {
                        if (cmd.getSubcommand() != Command.SUB_WELCOME_REAL) {
                            String name = getNewBotName("ctSim.model.bots.CtBotSimTcp");
                            bot = new CtBotSimTcp(this.world, name,
                                    new Point3d(0.5d, 0d, 0.075d),
                                    new Vector3d(1.0f, -0.5f, 0f),
                                    con);
                            lg.fine("Virtueller Bot nimmt Verbindung auf");
                        }
                    } else {
                        lg.info("Bot ist nicht willkommen: '"
                                        + cmd.toString()+ "'"+
                                        " ==> Bot l\u00E4uft schon oder " +
                                        "ist veraltet, "+
                                        "Schicke Willkommen nochmals");
                        // Hallo sagen
                        con.send((new Command(Command.CMD_WELCOME, 0,	0, 0)).getCommandBytes());
                    }
                } else
                    //LODO Fehlermeldung unklar: Was fuer ein Kommando?
                    lg.warning("Fehlerhaftes Kommando gefunden");
            }
        } catch (IOException e) {
            //LODO Fehlermeldung unverstaendlich
            lg.warning(e, "TCPConnection unterbrochen -- Verbindung nicht " +
            		"m\u00F6glich");
        }

        if (bot != null && this.world != null && this.judge.isAddingBotsAllowed()) {
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
            lg.warning("Bot-Datei '"+filename+"' nicht gefunden");
            return;
        }

        lg.info("Starte externen Bot '"+filename+"'");
        try {
            Runtime.getRuntime().exec(filename);
        } catch (Exception e){
            lg.warning(e, "Fehler beim Starten von Bot '"+filename+"'");
        }
    }

	@SuppressWarnings("unchecked")
	private synchronized Bot addBot(Bot bot){
		if (bot != null && world != null && judge.isAddingBotsAllowed()) {
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
    	//$$ Wenn ein Spiel laeuft vielleicht folgendes: throw new IllegalStateException("Aendern des Judge ist waehrend eines Spiels nicht moeglich")
    	Judge j = null;
        try {
            Class<?> cl = Class.forName(judgeClassName);
            Constructor<?> c = cl.getConstructor(DefaultController.class);
            j = (Judge)c.newInstance(this);
        } catch(ClassNotFoundException e) {
            lg.warning(e, "Die Judge-Klasse '"+judgeClassName+
                    "' wurde nicht gefunden");
            return;
        } catch(Exception e) {
            lg.warning(e, "Probleme beim Instanziieren der Judge-Klasse");
            return;
        }

        setJudge(j); // wird nur erreicht, wenn der try-Block geklappt hat
    }

    public void setJudge(Judge judge) {
        if (judge == null)
            throw new NullPointerException();
        this.judge = judge;
        view.onJudgeSet(judge);
    }

    public void openWorldFromFile(File sourceFile) {
        // TODO: Wenn kein DTD-file gegeben, besser Fehlermeldung!
        try {
            setWorld(World.buildWorldFromFile(sourceFile));
        } catch (Exception e) {
            lg.warning(e, "Probleme beim Parsen der Parcours-Datei '"+
                    sourceFile.getAbsolutePath() + "'");
        }
    }

    public void openWorldFromXmlString(String parcoursAsXml) {
        try {
            setWorld(World.buildWorldFromXmlString(parcoursAsXml));
        } catch (Exception e) {
            lg.warning(e, "Probleme beim Parsen des Parcours");
        }
    }

    public void openRandomWorld() {
        try {
            String p = ParcoursGenerator.generateParc();
            lg.info("Parcours generiert");
            openWorldFromXmlString(p);
        } catch (Exception e) {
            lg.warning("Probleme beim \u00D6ffnen des generierten " +
                    "Parcours.");
        }
    }

    public void onApplicationInited() {
        view.onApplicationInited();
    }
}