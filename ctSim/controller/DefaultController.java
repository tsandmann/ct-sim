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
import java.lang.Thread.State;
import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ctSim.ComConnection;
import ctSim.ConfigManager;
import ctSim.TcpConnection;
import ctSim.model.ParcoursGenerator;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;
import ctSim.view.View;

//TODO Es passiert Mist, wenn TestBots hinzugefuegt werden, bevor ein Parcours existiert. Untersuchen.

/**
 * Zentrale Controller-Klasse des c't-Sim
 */
public class DefaultController
implements Controller, BotBarrier, Runnable, BotReceiver {
	protected FmtLogger lg = FmtLogger.getLogger("ctSim.controller");

	private Thread ctrlThread;
	private volatile boolean pause;

	private CountDownLatch startSignal, doneSignal;

    private Judge judge;
    private View view;
    private World world;

    //$$ Idiotischerweise ist die hauptsaechliche Initialisierung hier
    public void setView(View view) {
	    this.pause = true;
        this.view = view;
        setJudge(Config.getValue("judge"));
        TcpConnection.startListening(this);
        ComConnection.startListening(this);
        init();
    }

    private void init() {
        try {
            String parcFile = Config.getValue("parcours");
            openWorldFromFile(new File(parcFile));
        } catch(NullPointerException e) {
            lg.fine("Kein Standardparcours konfiguriert");
        } catch(Exception e) {
            lg.warning(e, "Problem beim Instanziieren des Standardparcours");
        }

        String botBin = ConfigManager.path2Os(Config.getValue("botbinary"));
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
		int timeout = 10000; //$$ Default lieber in Klasse Config

        try {
        	timeout = Integer.parseInt(Config.getValue("ctSimTimeout"));
        } catch(NumberFormatException nfe) {
            lg.warning(nfe, "Problem beim Parsen der Konfiguration: " +
                    "Parameter 'ctSimTimeout' ist keine Ganzzahl");
        }

		lg.fine("Sequencer gestartet");
		lg.fine("ctSimTimeout ist %d ms", timeout);
		Thread thisThread = Thread.currentThread();

		startSignal = new CountDownLatch(1);
		doneSignal = new CountDownLatch(0);

		while(this.ctrlThread == thisThread) {
	        try {
				long realTimeBeginInMs = System.currentTimeMillis();

				// Warte, bis alle Bots fertig sind und auf die naechste
	        	// Aktualisierung warten
				// breche ab, wenn die Bots zu lange brauchen !
				if(! doneSignal.await(timeout, TimeUnit.MILLISECONDS)) {
					lg.warn("Bot-Probleme: Ein oder mehrere Bots waren " +
							"viel zu langsam (>"+timeout+" ms)");
				}

				CountDownLatch oldStartSignal = this.startSignal;

				// Judge pruefen:
				if (judge.isSimulationFinished(world.getSimTimeInMs())) {
					lg.fine("Sequencer: Simulationsende");
					// Spiel ist beendet
					pause();
					// Alle Bots entfernen
					world.removeAllBotsNow();
					view.onSimulationFinished();
				}

				// View(s) bescheidsagen
				//TODO Wieso wird das nochmal gemacht, wenn die Simulation (per Judge-Urteil) schon beendet wurde?
				view.onSimulationStep(world.getSimTimeInMs());

				if(this.pause) {
					lg.fine("Pause beginnt: Sequencer blockiert");
					synchronized(this) {
						wait();
					}
					lg.fine("Pause beendet: Sequencer freigegeben");
				}

				// Die ganze Simulation aktualisieren
				//TODO Warum _nach_ dem view.update()? Heisst das nicht, die Anzeige hinkt der Simulation immer um einen Schritt hinterher?
				world.updateSimulation();

				// Add/Start new Bot
				// + neue Runde einleuten
				// Ueberschreibt startSignal, aber wir haben mit oldStartSignal eine Referenz aufgehoben
				this.doneSignal  = new CountDownLatch(world.getNumAliveObsts());
				this.startSignal = new CountDownLatch(1);

				world.startBots();

				// Alle Bots wieder freigeben
				oldStartSignal.countDown();

				// Schlafe nur, wenn nicht schon zuviel Zeit "verbraucht" wurde
				// Felix: !!!Finger weg von den folgenden Zeilen !!!
				long timeToSleep = world.getSimStepIntervalInMs() -
						(System.currentTimeMillis() - realTimeBeginInMs);
				if (timeToSleep > 0)
					Thread.sleep(timeToSleep);
	        } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }
		cleanup();
		lg.fine("Sequencer beendet");
	}

	private synchronized void cleanup() {
		world.cleanup();
    }

	/**
	 * @throws InterruptedException
	 */
	public void awaitNextSimStep() throws InterruptedException {
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
		pause();

		if (this.world != null)
			this.world.cleanup();

		this.world = world;
        judge.setWorld(world);
        view.onWorldOpened(world);
        lg.info("Neue Welt ge\u00F6ffnet");
    }

    public void closeWorld() {
		this.reset();
		this.world = null;
    }

	//TODO Um Himmels Willen, was soll das denn sein?
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

    public void connectToTcp(String hostname, String port) {
    	int p = 10002; //$$ in config tun
    	try {
    		p = Integer.parseInt(port);
    	} catch (NumberFormatException e) {
    		lg.warn("'%s' ist eine doofe TCP-Port-Nummer; ignoriere", port);
    	}
    	TcpConnection.connectTo(hostname, p, this);
    }

    /**
     * F&uuml;gt der Welt einen neuen Bot der Klasse CtBotSimTest hinzu
     */
    public void addTestBot() {
        onBotAppeared(new CtBotSimTest());
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
    		if (System.getProperty("os.name").indexOf("Linux") >= 0){
    			Process p = Runtime.getRuntime().exec("chmod ugo+x "+filename);
    			p.waitFor(); // Warten bis der gelaufen ist
    			if (p.exitValue() != 0) {
    				lg.warning("Fehler beim Setzen der execute-Permission: " +
    						"chmod lieferte %d zur\u00FCck", p.exitValue());
    			}
    		}
    		// Bot ausfuehren
            Runtime.getRuntime().exec(filename);
        } catch (Exception e){
            lg.warning(e, "Fehler beim Starten von Bot '"+filename+"'");
        }
    }

    public synchronized void onBotAppeared(Bot bot) {
    	if (bot instanceof SimulatedBot) {
    		if (world == null)
    			throw new NullPointerException();
    		if (judge.isAddingBotsAllowed()) {
    			Bot b = world.addBot((SimulatedBot)bot, this);
    			view.onBotAdded(b);
    		}
    	}
    	else
    		view.onBotAdded(bot);
    }

    public int getParticipants() {
        return world.getNumAliveObsts();
    }

	//$$ Nach Judge-Umbau: kann weg
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