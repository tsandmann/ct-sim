/*
 * c't-Sim - Robotersimulator für den c't-Bot
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
import java.net.ProtocolException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import ctSim.ComConnection;
import ctSim.ConfigManager;
import ctSim.TcpConnection;
import ctSim.model.Command;
import ctSim.model.Map.MapException;
import ctSim.model.ParcoursGenerator;
import ctSim.model.World;
import ctSim.model.bots.BasicBot;
import ctSim.model.bots.Bot;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.ctbot.CtBot;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.rules.Judge;
import ctSim.util.BotID;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.View;

/** Zentrale Controller-Klasse des c't-Sim */
public class DefaultController
implements Controller, BotBarrier, Runnable, BotReceiver {
	/** Logger */
	protected FmtLogger lg = FmtLogger.getLogger("ctSim.controller");

	/** Flag für Pause */
	private volatile boolean pause;

	/** Latch für Start */
	private CountDownLatch startSignal;
	/** Latch für Done */
	private CountDownLatch doneSignal;

	/** Jugde */
    private Judge judge;
    /** View */
    private View view;
    /** Welt */
    private World world;
    /** Thread, der die Simulation macht: Siehe {@link #run()} */
    private Thread sequencer;

	/** Bots */
	private final List<Bot> bots = Misc.newList();

	/** Flag für Bot-Reset */
	private boolean reset = false;
	
	/**
	 * Setzt das View und initialisiert alles Nötige dafür
	 * 
	 * @param view	unser View
	 */
	public void setView(View view) {
	    this.pause = true;
	    this.view = view;
	    setJudge(Config.getValue("judge"));
	    TcpConnection.startListening(this);
	    try {
	        if (Config.getValue("serialport") == null) {
	        	lg.fine("Einstellung 'serialport' nicht gesetzt; Unterstützung für serielle Schnittstellen deaktiviert");
	        } else
	        	ComConnection.startListening(this);
        } catch (Error e) {
        	lg.warn("Serielle Schnittstelle konnte nicht geladen werden:");
        	lg.warn(e.toString());
        } finally {
        	init();
        }
    }

    /** Initialisierung */
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

        if (Config.getValue("BotAutoStart").equalsIgnoreCase("true")) {
        	String botBin = ConfigManager.path2Os(Config.getValue("botbinary"));
        	if (botBin != null) {
        		invokeBot(new File(botBin));	// mit botBin als String funktioniert es nicht
        	}
        }
    }

	/**
	 * Sequencer: Führt die Simulation aus. Während der Simulation erledigt er pro Simschritt zwei
	 * Hauptaufgaben:
	 * <ul>
	 * <li>"updateSimulation": Sagt der Welt bescheid, die den ThreeDBots bescheid sagt, die den
	 * Simulatoren Bescheid sagen. Die Simulation rechnen einen Sim-Schritt, z.B. wird abhängig von der
	 * Motorgeschwindigkeit die Position des Bot weitergesetzt</li>
	 * <li>Die Bots übertragen die Sensordaten zum C-Code, warten auf Antwort, und verarbeiten die Antwort.</li>
	 * </ul>
	 */
	public void run() {
		int timeout = 10000;

		// Sequencer-Thread hat eigene Referenz auf die Welt - siehe Bug 55
		final World sequencersWorld = world;

        try {
        	timeout = Integer.parseInt(Config.getValue("ctSimTimeout"));
        } catch(NumberFormatException nfe) {
            lg.warning(nfe, "Problem beim Parsen der Konfiguration: Parameter 'ctSimTimeout' ist keine Ganzzahl");
        }

		lg.fine("Sequencer gestartet");
		lg.fine("ctSimTimeout ist %d ms", timeout);
		Thread thisThread = Thread.currentThread();

		startSignal = new CountDownLatch(1);
		doneSignal = new CountDownLatch(0);

		while(this.sequencer == thisThread) {
	        try {
				long realTimeBeginInMs = System.currentTimeMillis();

				/* 
				 * Warte, bis alle Bots fertig sind und auf die nächste Aktualisierung warten;
				 * breche ab, wenn die Bots zu lange brauchen!
				 */
				if(! doneSignal.await(timeout, TimeUnit.MILLISECONDS)) {
					lg.warn("Bot-Probleme: Ein oder mehrere Bots waren viel zu langsam (>" + timeout + " ms)");
				}

				CountDownLatch oldStartSignal = this.startSignal;

				/* Reset gewünscht? */
				if (this.reset) {
					this.reset = false;
					view.onResetAllBots();
				}
				
				// Judge prüfen:
				if (judge.isSimulationFinished(sequencersWorld.getSimTimeInMs())) {
					lg.fine("Sequencer: Simulationsende");
					// Spiel ist beendet
					pause();
					// alle Bots entfernen
					sequencersWorld.removeAllBotsNow();
					view.onSimulationFinished();
				}

				// den View(s) Bescheid sagen
				view.onSimulationStep(sequencersWorld.getSimTimeInMs());

				if(this.pause) {
					lg.fine("Pause beginnt: Sequencer blockiert");
					synchronized(this) {
						wait();
					}
					lg.fine("Pause beendet: Sequencer freigegeben");
				}

				// die ganze Simulation aktualisieren
				sequencersWorld.updateSimulation();

				// Fix für Bug 12
				if (sequencersWorld.getFutureNumOfBots() == 0) {
					lg.info("Keine Bots mehr in der Simulation, pausiere");
					pause();
				}

				/* 
				 * Add/Start new Bot + neue Runde einläuten überschreibt startSignal,
				 * aber wir haben mit oldStartSignal eine Referenz aufgehoben
				 */
				doneSignal  = new CountDownLatch(sequencersWorld
					.getFutureNumOfBots());
				startSignal = new CountDownLatch(1);

				sequencersWorld.startBots();

				// alle Bots wieder freigeben
				oldStartSignal.countDown();

				// Schlafe nur, wenn nicht schon zuviel Zeit "verbraucht" wurde
				long timeToSleep = sequencersWorld.getSimStepIntervalInMs() -
						(System.currentTimeMillis() - realTimeBeginInMs);
				if (timeToSleep > 0)
					Thread.sleep(timeToSleep);
			} catch (InterruptedException e) {
				/* 
				 * Wird von wait() geworfen, wenn jemand closeWorld() macht während wait() noch läuft
				 * (closeWorld() ruft interrupt() auf). Man bittet uns, unsere while-Bedingung auszuwerten,
				 * weil die false geworden ist. Also normal weiter gehen.
				 */
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

	/** Lässt den Sequencer (= die Simulation) pausieren. Keine Wirkung, falls keine Welt geladen ist. */
	public void pause() {
		lg.fine("Pause angefordert");
		this.pause = true;
	}

	/** Beendet die Pause des Sequencers (= der Simulation) */
	public synchronized void unpause() {
		lg.fine("Pausenende angefordert");
		if(this.world == null)
			return;

		/** Wenn world != null haben wir einen Sequencer-Thread laufen */
		if(this.pause && this.judge.isStartingSimulationAllowed()) {
			this.pause = false;
			this.notify();
		}
	}

	/**
	 * Setzt die Welt. Prinzip: Sequencer und Welt werden gemeinsam erstellt und gemeinsam gekillt.
	 * setWorld() startet einen Sequencer-Thread, der pausiert bleibt bis jemand unpause() aufruft.
	 * 
	 * @param world	die Welt
	 */
	private void setWorld(World world) {
		if (world == null)
			throw new NullPointerException();

		closeWorld();	// beendet Thread
		this.world = world;
		judge.setWorld(world);
		view.onWorldOpened(world);
		lg.info("Neue Welt geöffnet");

		lg.fine("Initialisiere Sequencer");
		pause = true;	// immer pausiert starten
		sequencer = new Thread(this, "ctSim-Sequencer");
		sequencer.start();
    }

	/** Hält den Sequencer-Thread an */
	public void closeWorld() {
    	if (sequencer == null)	// keine Welt geladen, d.h. kein Sequencer läuft
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
	 * 
	 * @param hostname	Ziel der Verbindung (Name)
	 * @param port		Ziel der Verbindung (Port)
	 */
	public void connectToTcp(String hostname, String port) {
    	int p = 10002;
    	try {
    		p = Integer.parseInt(port);
    		TcpConnection.connectTo(hostname, p, this);
    	} catch (NumberFormatException e) {
    		lg.warn("'%s' ist eine doofe TCP-Port-Nummer; ignoriere", port);
    	}
    }

	/** Fügt der Welt einen neuen Bot der Klasse CtBotSimTest hinzu */
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
	 * 
	 * @param file	File-Objekt des Bots
	 */
	public void invokeBot(File file) {
		invokeBot(file.getAbsolutePath());
	}

	/**
	 * Startet einen externen Bot
	 * 
	 * @param filename	Pfad zum Bot als String
	 */
    public void invokeBot(String filename) {
        if (! new File(filename).exists()) {
            lg.warning("Bot-Datei '" + filename + "' nicht gefunden");
            return;
        }
        lg.info("Starte externen Bot '" + filename + "'");
        try {
    		if (System.getProperty("os.name").indexOf("Linux") >= 0){
    			Process p = Runtime.getRuntime().exec(
    				new String[] { "chmod", "ugo+x", filename });
    			p.waitFor();	// warten bis der gelaufen ist
    			if (p.exitValue() != 0) {
    				lg.warning("Fehler beim Setzen der execute-Permission: chmod lieferte %d zurück", p.exitValue());
    			}
    		}
    		/* 
    		 * Bot ausführen
    		 * String[], sonst trennt er das nach dem ersten Leerzeichen ab,
    		 * dann geht's nicht, wenn der Pfad mal ein Leerzeichen enthält
    		 */
    		File dir = new File(filename).getAbsoluteFile().getParentFile();
            Runtime.getRuntime().exec(new String[] { filename }, null, dir);
        } catch (Exception e){
            lg.warning(e, "Fehler beim Starten von Bot '" + filename + "'");
        }
    }

    /**
     * Handler, falls ein Bot stirbt
     * 
     * @param bot	der sterbende Bot
     */
    public synchronized void onBotDisappeared(Bot bot) {
    	if (bot != null) {
    		try {
    			lg.info("Bot " + bot.toString() + " (" + bot.getDescription() + ") meldet sich beim Controller ab!");
    		} catch (Exception e) {
    			// egal
    		} finally {
    			bots.remove(bot);
    		}
    	}
    }
    
    /**
	 * Handler, falls neuer Bot hinzugefügt wurde
	 * 
	 * @param bot	der neue Bot
	 */
	public synchronized void onBotAppeared(final Bot bot) {
		if (bot instanceof SimulatedBot) {
			if (sequencer == null) {
				lg.info("Weise " + bot.toString() + " ab: Es gibt keine Welt, zu der man ihn hinzufügen könnte");
				bot.dispose();	// Bot abweisen
				return;
			}
			if (judge.isAddingBotsAllowed()) {
				Bot b = world.addBot((SimulatedBot) bot, this);
				if (b == null) {
					bot.dispose();	// Bot abweisen, da kein Platz mehr
					return;
				}
				view.onBotAdded(b);
			} else {
				bot.dispose();	// Bot abweisen
			}
		} else
			view.onBotAdded(bot);

		try {
			bot.setController(this);
		} catch (ProtocolException e) {
			lg.severe("Fehler: Bot " + bot.toString() + " wurde vom Controller abgewiesen! Bot-ID falsch?");
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {	// invokeLater, weil sonst der dispose-Listener der GUI noch nicht eingetragen ist!
					bot.dispose();	// Bot ist unerwünscht => weg
				}
			});
			return;	// keine Exception, wir weisen den Bot einfach ab und
			// alles andere läuft normal weiter
		}

		// für den Kommunikationsproxy brauchen wir eine Liste aller Bots
		bots.add(bot);

		// und einen Dispose-Handler installieren, damit wir Bots auch wieder sauber beenden
		bot.addDisposeListener(new Runnable() {
			public void run() {
				onBotDisappeared(bot);
			}
		});
	}

    /**
     * Gibt die Anzahl der zurzeit geladenen Bots zurück
     * 
     * @return	Anzahl der Bots
     */
    public int getParticipants() {
        return world.getFutureNumOfBots();
    }

	/**
	 * @param judgeClassName	die Art des zu setzenden Schiedsrichters
	 * 
	 * Stellt sicher, dass immer ein sinnvoller Judge gesetzt ist.
	 */
	public void setJudge(String judgeClassName) {
    	/* kein Jugde-Wechsel wenn eine Welt offen ist */
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
            lg.warning(e, "Die Judge-Klasse '" + judgeClassName + "' wurde nicht gefunden");
            return;
        } catch(Exception e) {
            lg.warning(e, "Probleme beim Instanziieren der Judge-Klasse");
            return;
        }

        setJudge(j);	// wird nur erreicht, wenn der try-Block geklappt hat
    }

	/**
	 * Setzt den Schiedsrichter
	 * 
	 * @param judge	gewünschte Judge-Instanz
	 */
	public void setJudge(Judge judge) {
    	if (judge == null)
            throw new NullPointerException();
        this.judge = judge;
//      view.onJudgeSet(judge);
    }

	/**
	 * Lädt eine Welt aus einer Datei
	 * 
	 * @param sourceFile	File-Objekt der Welt
	 */
	public void openWorldFromFile(File sourceFile) {
        try {
            setWorld(World.buildWorldFromFile(sourceFile));
        } catch (Exception e) {
            lg.warning(e, "Probleme beim Parsen der Parcours-Datei '" + sourceFile.getAbsolutePath() + "'");
        }
    }

	/**
	 * Lädt eine Welt aus einem String
	 * 
	 * @param parcoursAsXml	String mit den XML-Dater der Welt
	 */
	public void openWorldFromXmlString(String parcoursAsXml) {
        try {
            setWorld(World.buildWorldFromXmlString(parcoursAsXml));
        } catch (Exception e) {
            lg.warning(e, "Probleme beim Parsen des Parcours");
        }
    }

	/** Erzeugt eine zufällige Welt */
	public void openRandomWorld() {
        try {
            String p = ParcoursGenerator.generateParc();
            lg.info("Parcours generiert");
            openWorldFromXmlString(p);
        } catch (Exception e) {
            lg.warning("Probleme beim Öffnen des generierten Parcours.");
        }
    }

	/** Init-Handler */
	public void onApplicationInited() {
        view.onApplicationInited();
    }
    
        
	/**
	 * Setzt alle Bots im nächsten Sim-Schritt zurück.
	 * Dadurch, dass hier nur das Reset-Flag gesetzt wird, erübrigt sich das Synchronisieren der
	 * (Bot-)Threads; der Reset erfolgt einfach beim nächsten Zeitschritt.
	 */
	public void resetAllBots() {
    	this.reset = true;
    }
    
	/**
	 * Liefert ein Kommando an einen Bot aus.
	 * Diese Routine kann dazu benutzt werden, um Bot-2-Bot-Kommunikation zu betreiben.
	 * Sender und Empfänger stehen in dem command drin.
	 *  
	 * @param command	das zu übertragende Kommando
	 * @throws ProtocolException	falls kein passender Empfänger gefunden wurde
	 */
	public void deliverMessage(Command command) throws ProtocolException {
		for (Bot b : bots) {
			// Wir betrachten hier nur CtBot.
			if (b instanceof CtBot) {
				/* direkte Nachrichten an Empfänger */
				if (((CtBot)b).getId().equals(command.getTo())) {
					((CtBot)b).receiveCommand(command);
					return;
				}
				/* Broadcasts an alle */
				if (command.getTo().equals(Command.getBroadcastId())) {
					((CtBot)b).receiveCommand(command);
				}
			}
		}	
		if (!command.getTo().equals(Command.getBroadcastId())) {
			// Es fühlt sich wohl kein Bot aus der Liste zuständig... -> Fehler 
			throw new ProtocolException("Nachricht an Empfänger " + command.getTo() + " nicht zustellbar. " +
					"Kein Bot mit passender Id angemeldet");
		}
	}
	
	/**
	 * Testet, ob bereits ein Bot diese Id hat
	 * 
	 * @param id	zu testende Id
	 * @return True, wenn noch kein Bot diese Id nutzt 
	 */
	public boolean isIdFree(BotID id){
		if (id.equals(Command.getBroadcastId()))
			// Broadcast ist immer frei
			return true;
		for (Bot b : bots) 
			if (b instanceof BasicBot) 			
				if (((BasicBot)b).getId().equals(id))
					return false;
		return true;
	}
	
	/** Offset für die Adressvergabe, damit wir nicht jedesmal mit den schon vergebenen Adressen beginnen */
	private int poolOffset = 0;
	
	/**
	 * Liefert eine Id aus dem Adresspoll zurück
	 * 
	 * @return Die neue Id
	 * @throws ProtocolException	wenn keine Adresse mehr frei
	 */
	public BotID generateBotId() throws ProtocolException{
		byte poolSize= (byte)64;
		byte poolAdress= (byte) 128;
		BotID newId = new BotID();
		
		for (byte i=0; i< poolSize; i++){
			newId.set(((i+ poolOffset) % poolSize) + poolAdress);
			if (isIdFree(newId)){
				poolOffset++;
				return newId;
			}				
		}
		
		throw new ProtocolException("Keine Id im Pool mehr frei");
	}
	
	/**
	 * Exportiert die aktuelle Welt in eine Bot-Map-Datei
	 * 
	 * @param bot		Bot-Nr. dessen Startfeld als Koordinatenursprung der Map benutzt wird 
	 * @param free		Wert mit dem freie Felder eingetragen werden (z.B. 100)
	 * @param occupied	Wert mit dem Hindernisse eingetragen werden (z.B. -100)
	 * @throws IOException	falls Fehler beim Schreiben der Datei
	 * @throws MapException	falls keine Daten in der Map
	 */
	public void worldToMap(int bot, int free, int occupied) throws IOException, MapException {
		this.world.toMap(bot, free, occupied);
	}

	/**
	 * @see ctSim.controller.Controller#getWorld()
	 */
	public World getWorld() {
		return this.world;
	}
}
