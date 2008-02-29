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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

/**
 * Zentrale Controller-Klasse des c't-Sim
 */
public class DefaultController
implements Controller, BotBarrier, Runnable, BotReceiver {
	/** Logger */
	protected FmtLogger lg = FmtLogger.getLogger("ctSim.controller");

	/** Flag fuer Pause */
	private volatile boolean pause;

	/** Latch fuer Start */
	private CountDownLatch startSignal;
	/** Latch fuer Done */
	private CountDownLatch doneSignal;

	/** Jugde */
    private Judge judge;
    /** View */
    private View view;
    /** Welt */
    private World world;
    /** Thread, der die Simulation macht: Siehe {@link #run()}. */
    private Thread sequencer;

    /**
     * Setzt das View und initialisiert alles noetige dafuer
     * @param view	unser View
     */
    public void setView(View view) {
	    this.pause = true;
        this.view = view;
        setJudge(Config.getValue("judge"));
        TcpConnection.startListening(this);
        try {
	        if (Config.getValue("serialport") == null) {
	        	lg.fine("Einstellung 'serialport' nicht gesetzt; Unterstützung " +
	        			"für serielle Schnittstellen deaktiviert");
	        } else
	        	ComConnection.startListening(this);
        } catch (Error e) {
        	lg.warn("Serielle Schnittstelle konnte nicht geladen werden:");
        	lg.warn(e.toString());
        } finally {
        	init();
        }
    }

    /**
     * Initialiserung
     */
    private void init() {
        try {
            String parcFile = Config.getValue("parcours");
    		InputStream openStream = ClassLoader.getSystemResource(parcFile).openStream();
    		BufferedReader in = new BufferedReader(new InputStreamReader(openStream));
    	    String line;
    	    String sourceString = new String();
    		while ((line = in.readLine()) != null) {
    			sourceString += line + "\r\n";
    		}
    		in.close();
    		openWorldFromXmlString(sourceString);
        } catch(NullPointerException e) {
            lg.fine("Kein Standardparcours konfiguriert");
        } catch(Exception e) {
            lg.warning(e, "Problem beim Instanziieren des Standardparcours");
        }

        String botBin = ConfigManager.path2Os(Config.getValue("botbinary"));
        if (botBin == null)
            lg.fine("Kein Standardbot konfiguriert");
        else
            invokeBot(new File(botBin));	// mit botBin als String funktioniert es nicht 
    }

	/**
	 * Sequencer: F&uuml;hrt die Simulation aus. W&auml;hrend der Simulation
	 * erledigt er pro Simschritt zwei Hauptaufgaben:
	 * <ul>
	 * <li>"updateSimulation": Sagt der Welt bescheid, die den ThreeDBots
	 * bescheidsagt, die den Simulatoren bescheidsagen. Die Simulation rechnen
	 * einen Simschritt, z.B. wird abh&auml;ngig von der Motorgeschwindigkeit
	 * die Position des Bot weitergesetzt</li>
	 * <li>Die Bots &uuml;bertragen die Sensordaten zum C-Code, warten auf
	 * Antwort, und verarbeiten die Antwort. </li>
	 * </ul>
	 */
	public void run() {
		int timeout = 10000;

		// Sequencer-Thread hat eigene Referenz auf die Welt -- siehe Bug 55
		final World sequencersWorld = world;

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

		while(this.sequencer == thisThread) {
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
				if (judge.isSimulationFinished(sequencersWorld.getSimTimeInMs())) {
					lg.fine("Sequencer: Simulationsende");
					// Spiel ist beendet
					pause();
					// Alle Bots entfernen
					sequencersWorld.removeAllBotsNow();
					view.onSimulationFinished();
				}

				// View(s) bescheidsagen
				//TODO Wieso wird das nochmal gemacht, wenn die Simulation (per Judge-Urteil) schon beendet wurde?
				view.onSimulationStep(sequencersWorld.getSimTimeInMs());

				if(this.pause) {
					lg.fine("Pause beginnt: Sequencer blockiert");
					synchronized(this) {
						wait();
					}
					lg.fine("Pause beendet: Sequencer freigegeben");
				}

				// Die ganze Simulation aktualisieren
				//TODO Warum _nach_ dem view.update()? Heisst das nicht, die Anzeige hinkt der Simulation immer um einen Schritt hinterher?
				sequencersWorld.updateSimulation();

				// Fix fuer Bug 12
				if (sequencersWorld.getFutureNumOfBots() == 0) {
					lg.info("Keine Bots mehr in der Simulation, pausiere");
					pause();
				}

				// Add/Start new Bot
				// + neue Runde einleuten
				// Ueberschreibt startSignal, aber wir haben mit oldStartSignal
				// eine Referenz aufgehoben
				doneSignal  = new CountDownLatch(sequencersWorld
					.getFutureNumOfBots());
				startSignal = new CountDownLatch(1);

				sequencersWorld.startBots();

				// Alle Bots wieder freigeben
				oldStartSignal.countDown();

				// Schlafe nur, wenn nicht schon zuviel Zeit "verbraucht" wurde
				// Felix: !!!Finger weg von den folgenden Zeilen !!!
				long timeToSleep = sequencersWorld.getSimStepIntervalInMs() -
						(System.currentTimeMillis() - realTimeBeginInMs);
				if (timeToSleep > 0)
					Thread.sleep(timeToSleep);
	        } catch (InterruptedException e) {
	            // Wird von wait() geworfen, wenn jemand closeWorld() macht
				// waehrend wait() noch laeuft (closeWorld() ruft interrupt()
				// auf). Man bittet uns, unsere while-Bedingung auszuwerten,
				// weil die false geworden ist. Also normal weitergehen
	        }
	    }
		sequencersWorld.cleanup();
		lg.fine("Sequencer beendet");
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
	 * L&auml;sst den Sequencer (= die Simulation) pausieren. Keine Wirkung,
	 * falls keine Welt geladen ist.
	 */
	public void pause() {
		lg.fine("Pause angefordert");
		this.pause = true;
	}

	/**
	 * Beendet die Pause des Sequencers (= der Simulation)
	 */
	public synchronized void unpause() {
		lg.fine("Pausenende angefordert");
		if(this.world == null)
			return;

		// Wenn world != null haben wir einen Sequencer-Thread laufen

		if(this.pause && this.judge.isStartingSimulationAllowed()) {
			this.pause = false;
			this.notify();
		}
	}

	/**
	 * Setzt die Welt. Prinzip: Sequencer und Welt werden gemeinsam erstellt und
	 * gemeinsam gekillt. setWorld() startet einen Sequencer-Thread, der
	 * pausiert bleibt bis jemand unpause() aufruft.
	 * @param world Die Welt
	 */
	private void setWorld(World world) {
		if (world == null)
			throw new NullPointerException();

		closeWorld(); // Beendet Thread
		this.world = world;
		judge.setWorld(world);
		view.onWorldOpened(world);
		lg.info("Neue Welt ge\u00F6ffnet");

		lg.fine("Initialisiere Sequencer");
		pause = true; // Immer pausiert starten
		sequencer = new Thread(this, "ctSim-Sequencer");
		sequencer.start();
    }

	/** H&auml;lt den Sequencer-Thread an */
    public void closeWorld() {
    	if (sequencer == null) // Keine Welt geladen, d.h. kein Sequencer läuft
    		return;

    	lg.fine("Terminieren des Sequencer angefordert");

    	Thread dummy = sequencer;
    	sequencer = null;
    	if (! dummy.getState().equals(State.TERMINATED))
    		dummy.interrupt();

    	judge.reinit();
    }

    /**
     * Verbindet zu Host:Port
     * @param hostname	Ziel der Verbindung (Name)
     * @param port		Ziel der Verbindung (Port)
     */
    public void connectToTcp(String hostname, String port) {
    	int p = 10002;
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
    	if (sequencer == null) {
    		try {
    			InputStream openStream;
				openStream = ClassLoader.getSystemResource("parcours/testbots_home.xml").openStream();
	    		BufferedReader in = new BufferedReader(new InputStreamReader(openStream));
	    	    String line;
	    	    String sourceString = new String();
	    		while ((line = in.readLine()) != null) {
	    			sourceString += line + "\r\n";
	    		}
	    		in.close();            
	    		openWorldFromXmlString(sourceString);
			} catch (IOException e) {
				lg.info("Testbot-Home konnte nicht geladen werden");
			}
    	}
        onBotAppeared(new CtBotSimTest());
    }

    /**
     * Startet einen externen Bot
     * @param file	File-Objekt des Bots
     */
	public void invokeBot(File file) {
		invokeBot(file.getAbsolutePath());
	}

	/**
	 * Startet einen externen Bot
	 * @param filename	Pfad zum Bot als String
	 */
    public void invokeBot(String filename) {
        if (! new File(filename).exists()) {
            lg.warning("Bot-Datei '"+filename+"' nicht gefunden");
            return;
        }
        lg.info("Starte externen Bot '"+filename+"'");
        try {
    		if (System.getProperty("os.name").indexOf("Linux") >= 0){
    			Process p = Runtime.getRuntime().exec(
    				new String[] { "chmod", "ugo+x", filename });
    			p.waitFor(); // Warten bis der gelaufen ist
    			if (p.exitValue() != 0) {
    				lg.warning("Fehler beim Setzen der execute-Permission: " +
    						"chmod lieferte %d zur\u00FCck", p.exitValue());
    			}
    		}
    		// Bot ausführen
    		// String[] weil sonst trennt er das nach dem ersten Leerzeichen ab,
    		// dann geht's nicht, wenn der Pfad mal ein Leerzeichen enthält
    		File dir = new File(filename).getAbsoluteFile().getParentFile();
            Runtime.getRuntime().exec(new String[] { filename }, null, dir);
        } catch (Exception e){
            lg.warning(e, "Fehler beim Starten von Bot '"+filename+"'");
        }
    }

    /**
     * Handler, falls neuer Bot hinzugefuegt wurde
     * @param bot	Der neue Bot
     */
    public synchronized void onBotAppeared(Bot bot) {
    	if (bot instanceof SimulatedBot) {
    		if (sequencer == null) {
    			lg.info("Weise "+bot.toString()+" ab: Es gibt keine Welt, zu " +
    					"der man ihn hinzuf\u00FCgen k\u00F6nnte");
    			bot.dispose(); // Bot abweisen
    			return;
    		}
    		if (judge.isAddingBotsAllowed()) {
    			Bot b = world.addBot((SimulatedBot)bot, this);
    			if (b == null) {
    				bot.dispose();	// Bot abweisen, weil kein Platz mehr
    				return;
    			}
    			view.onBotAdded(b);
    		} else {
    			bot.dispose(); // Bot abweisen
    		}
    	}
    	else
    		view.onBotAdded(bot);
    }

    /**
     * Gibt die Anzahl der zurzeit geladenen Bots zurueck
     * @return	Anzahl der Bots
     */
    public int getParticipants() {
        return world.getFutureNumOfBots();
    }

    /**
     * @param judgeClassName Die Art des Schiedrichters zu setzen
     * Stellt sicher, dass immer ein sinnvoller Judge gesetzt ist
     */
    public void setJudge(String judgeClassName) {
    	/* Kein Jugde-Wechsel, wenn eine Welt offen ist */
    	if (sequencer != null) {
    		lg.info("Kein Wechsel erlaubt, weil noch eine Welt offen ist");
    		return;
    	}    	
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

    /**
     * Setzt den Schiedsrichter
     * @param judge	gewuenschte Judge-Instanz
     */
    public void setJudge(Judge judge) {
    	if (judge == null)
            throw new NullPointerException();
        this.judge = judge;
//      view.onJudgeSet(judge);
    }
    
    /**
     * Laedt eine Welt aus einer Datei
     * @param sourceFile	File-Objekt der Welt
     */
    public void openWorldFromFile(File sourceFile) {
        try {
            setWorld(World.buildWorldFromFile(sourceFile));
        } catch (Exception e) {
            lg.warning(e, "Probleme beim Parsen der Parcours-Datei '"+
                    sourceFile.getAbsolutePath() + "'");
        }
    }

    /**
     * Laedt eine Welt aus einem String
     * @param parcoursAsXml	String mit den XML-Dater der Welt
     */
    public void openWorldFromXmlString(String parcoursAsXml) {
        try {
            setWorld(World.buildWorldFromXmlString(parcoursAsXml));
        } catch (Exception e) {
            lg.warning(e, "Probleme beim Parsen des Parcours");
        }
    }

    /**
     * Erzeugt eine zufaellige Welt
     */
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

    /**
     * Init-Handler
     */
    public void onApplicationInited() {
        view.onApplicationInited();
    }
}