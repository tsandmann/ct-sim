package ctSim.view.contestConductor;

import static ctSim.view.contestConductor.ContestConductor.Phase.MAIN_ROUND;
import static ctSim.view.contestConductor.ContestConductor.Phase.PRELIM_ROUND;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3d;

import ctSim.ConfigManager;
import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.controller.Main;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.View;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;
import ctSim.view.gui.sensors.RemoteControlGroupGUI;

// $$ doc gesamte Klasse
// $$ die staendigen "assert false" sind nicht so doll. Schoener waere: Exceptions werfen und den Controller exit machen lassen (verbessert auch Testbarkeit (Unit-Tests) des ContestConductor). Ich zieh mir aber nicht den Schuh an, Exception-Handling in den z.Zt. noch chaotischen Controller reinzupopeln.
// $$ doc ConCon-Package
/**
 * <p>
 * Erm&ouml;glicht die Durchf&uuml;hrung eines ctBot-Wettbewerbs wie den im
 * Oktober 2006.
 * </p>
 * <p>
 * <strong>Architektur</strong> des ContestConductor-Subsystems: <!-- Ascii-Art
 * NICHT "reparieren"! Die Verschiebungen durch die
 * Link-Tags sind okay, siehe Ausgabe im Browser -->
 *
 * <pre>
 *   Au&szlig;enwelt
 *   (Controller)
 *      |
 *      |
 *      |
 *      |
 *      v                     hat einen
 *   {@link ContestConductor} -------------------------&gt; {@link TournamentPlanner}
 *          |                                            |
 *          |                                            |
 *          | hat einen                                  | hat einen
 *          |                                            |
 *          v                                            v
 * {@link ConductorToDatabaseAdapter} ----.     .---- {@link PlannerToDatabaseAdapter}
 *                                |     |
 *                                |     |
 *             ist abgeleitet von |     | ist abgeleitet von
 *                                |     |
 *                                v     v
 *                            {@link DatabaseAdapter}
 *                                   |
 *                                   |
 *                                   | ist verbunden mit
 *                                   |
 *                                   v
 *                            MySQL-Datenbank
 * </pre>
 *
 * </p>
 *
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ContestConductor implements View {
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	public static class NoMoreGamesException extends Exception {
		private static final long serialVersionUID = - 930001102842406374L;
	}

	public static class ContestJudge extends Judge {
		class GameOutcome {
			Bot winner = null;
			HashMap<Bot, Double> distToFinish =
				new HashMap<Bot, Double>();

			@SuppressWarnings("synthetic-access")
            GameOutcome() {
	            for (Bot b : concon.botIds.keySet()) {
	            	distToFinish.put(
	            		b,
	            		concon.world.getShortestDistanceToFinish(
	            			b.getLastSafePos()));
	            }
			}
		}

		protected ContestConductor concon;

		public ContestJudge(Controller controller, ContestConductor concon) {
			super((DefaultController)controller); //$$ Das ist Mist. Judge und die davon abgeleiteten Klassen muessen aufgeraeumt und vereinfacht werden
			this.concon = concon;
	    }

		@Override
        public boolean isStartingSimulationAllowed() {
			//$$ Doofe Methode, keine Ahnung wofuer die ueberhaupt da ist
	        return true;
        }

		@Override
		public boolean isSimulationFinished() {
            try {
	            return isAnyoneOnFinishTile() || isGameTimeoutElapsed();
            } catch (NullPointerException e) {
	            concon.lg.severe(e, "Inkonsistenter Zustand: Es l\u00E4uft " +
	            		"laut Datenbank kein Spiel, laut Controller aber " +
	            		"schon");
	            assert false;
            } catch (SQLException e) {
            	concon.lg.severe(e, "Low-Level-Datenbankproblem");
	            assert false;
            } catch (TournamentPlanException e) {
				concon.lg.severe(e, "Probleme beim Fortschreiben des " +
						"Spielplans");
				assert false;
            }
            // unerreichbarer Code, aber man will ja den Compiler bei
            // Laune halten
            return false;
        }

		@SuppressWarnings("synthetic-access")
        private boolean isAnyoneOnFinishTile()
		throws NullPointerException, SQLException, TournamentPlanException {
			for(Bot b : concon.botIds.keySet()) {
				if (concon.world.finishReached(new Vector3d(b.getPosition()))) {
					GameOutcome o = new GameOutcome();
					o.winner = b;
					o.distToFinish.put(b, 0d); // ueberschreiben

					setWinner(o);
					return true;
				}
			}
			return false;
		}

        @SuppressWarnings("synthetic-access")
        private boolean isGameTimeoutElapsed()
        throws SQLException, TournamentPlanException {
			if (concon.world.getSimTimeInMs() <
				concon.db.getMaxGameLengthInMs()) {
				// Spielzeit ist noch nicht um
				return false;
			}

			concon.lg.info("Spielzeit abgelaufen; Ermittle Bot, der dem " +
					"Ziel am n\u00E4chsten ist");

			GameOutcome o = new GameOutcome();
			o.winner = concon.botIds.keySet().iterator().next();
            for (Map.Entry<Bot, Double> d : o.distToFinish.entrySet()) {
        		if (d.getValue() < o.distToFinish.get(o.winner))
        			 o.winner = d.getKey();
            }

            setWinner(o);
			return true;
		}

        @SuppressWarnings("synthetic-access")
        protected void setWinner(GameOutcome outcome)
		throws NullPointerException, SQLException, TournamentPlanException {
        	concon.lg.info("Gewinner ist Bot %s nach einem Spiel von %d ms",
        		outcome.winner, concon.world.getSimTimeInMs());

        	// Letzten Schritt loggen //$$ Das ist nicht so toll: Macht die Annahme, dass der DefaultController so bleibt, wie er ist
        	concon.db.log(concon.botIds.keySet(),
        		concon.world.getSimTimeInMs());

        	// Restwege schreiben
            for (Map.Entry<Bot, Double> d :
            	outcome.distToFinish.entrySet()) {
            	concon.db.writeDistanceToFinish(concon.botIds.get(d.getKey()),
            		d.getValue());
            }

			// Spiel beenden
        	concon.db.setWinner(concon.botIds.get(outcome.winner),
        		concon.world.getSimTimeInMs());
		}
	}

	/**
	 * Status des ContestConductor-Subsystems. Typischer Ablauf:
	 *
	 * <pre>
	 * PRELIM_ROUND &rarr; MAIN_ROUND &rarr; (Programmende)
	 * </pre>
	 */
	enum Phase {
		PRELIM_ROUND,
		MAIN_ROUND,
	}

	static {
		Main.dependencies.registerImplementations(
				ContestJudge.class,
				TournamentPlanner.class,
				ConductorToDatabaseAdapter.class,
				PlannerToDatabaseAdapter.class //$$ in seine Klasse?
			);
	}

	/** Siehe {@link #Phase} */
	private Phase currentPhase = PRELIM_ROUND;

	/** Abstrahiert die Datenbank */
	private ConductorToDatabaseAdapter db;

	/** H&auml;lt die DB-Prim&auml;rschl&uuml;ssel der Bots */
	protected Map<Bot, Integer> botIds = new HashMap<Bot, Integer>();

	private TournamentPlanner planner;

	/** Referenz auf den Controller */
	private Controller controller;
	private World world;

	private Object botArrivalLock = new Object();
	private Bot newlyArrivedBot = null;

	public ContestConductor(Controller controller,
		ConductorToDatabaseAdapter db, TournamentPlanner planner) {
		this.controller = controller;
		this.db = db;
		this.planner = planner;
	}

	public void onApplicationInited() {
		controller.setJudge(Main.dependencies.get(ContestJudge.class));

		try {
	        if (db.gamesExist()) // sort key ist eigentlich wurst
	        	recoverFromCrash();
	        else
	        	// Turnier faengt neu an
	        	planner.planPrelimRound();
        } catch (SQLException e) {
	        lg.severe(e, "Low-level-Datenbankproblem");
	        assert false;
        }

		try {
	        sleepAndStartNextGame();
        } catch (Exception e) {
	        lg.severe(e, "Problem beim Durchf\u00FChren des ersten Spiels");
	        assert false;
        }
	}

	private void recoverFromCrash()
	throws IllegalArgumentException, SQLException {
	    lg.info("Abgebrochenes Turnier gefunden; versuche Wiederaufnahme");
	    db.resetRunningGames();
	    if (! db.isPrelimIncomplete()) {
	    	lg.info("Vorrunde wurde komplett gespielt; steige in " +
	    			"Hauptrunde ein");
	    	currentPhase = MAIN_ROUND;
	    }
    }

	public void onSimulationStep(
		@SuppressWarnings("unused") long simTimeInMs) {
		try {
	        db.log(botIds.keySet(), world.getSimTimeInMs());
        } catch (Exception e) {
    		lg.warn(e, "Probleme beim Loggen des Spielzustands in die DB");
        }
	}

	public void onSimulationFinished() {
		lg.info("Spiel beendet; 10 Sekunden Unterbrechung");
		try {
	        Thread.sleep(10000);
        } catch (InterruptedException e) {
	        lg.warning(e, "ContestConductor aufgeweckt");
        }

		try {
			try {
				sleepAndStartNextGame();
			} catch (NoMoreGamesException e) {
				switch (currentPhase) {
	                case PRELIM_ROUND:
	                	planner.planMainRound();
	                	// Dank Planner sind jetzt wieder Spiele "ready"
	                	currentPhase = MAIN_ROUND;
	                	try {
	                        sleepAndStartNextGame();
                        } catch (NoMoreGamesException e1) {
	                        lg.severe(e1, "Planer hat versagt: Hauptrunde " +
	                        		"nicht oder falsch geplant");
	                        assert false;
                        }
		                break;

	                case MAIN_ROUND:
	                	lg.info("Turnier erfolgreich abgeschlossen. Beende " +
	                			"den ctSim");
	                	System.exit(0);
	                	break;
				}
			}
		} catch (Exception e) {
	        lg.severe(e, "Problem mit der Durchf\u00FChrung des Wettbewerbs");
	        assert false;
        }
    }

	/**
	 *
	 * @param game
	 * @throws SQLException
	 * @throws IOException
	 * @throws NoMoreGamesException
	 */
	private synchronized void sleepAndStartNextGame()
	throws SQLException, IOException, NoMoreGamesException {
		ResultSet game = db.getReadyGames();
		if (! game.next())
			throw new NoMoreGamesException();

		Timestamp scheduled = game.getTimestamp("scheduled");

		long timeTilGameInMs;
		while ((timeTilGameInMs =
			scheduled.getTime() - System.currentTimeMillis()) > 0) {
			try {
				lg.fine("Warte %d ms auf n\u00E4chsten Wettkampf (bis %s)",
					timeTilGameInMs, scheduled);
				Thread.sleep(timeTilGameInMs);
			} catch (InterruptedException e) {
				lg.warn(e, "ContestConductor aufgeweckt. Schlafe weiter.");
			}
		}
		// Zustand: Startzeitpunkt des Spiels erreicht
		startGame(game);
    }

	private Process execute(String commandAndArgs) throws IOException {
    	lg.fine("Starte " + commandAndArgs);
		return Runtime.getRuntime().exec(commandAndArgs);
	}

	/**
	 * Welcher Rechner soll den n&auml;chsten Bot ausf&uuml;hren
	 */
	private int nextHost = 1;

	/**
	 * Startet einen Bot, entweder lokal oder remote
	 * @param f
	 */
	private void executeBot(File f){
		String host = ConfigManager.getValue("host"+nextHost);
		String user = ConfigManager.getValue("host"+nextHost+"_username");

		String server = ConfigManager.getValue("ctSimIP");
		if (server == null)
			server = "localhost"; //$$ umziehen

		// Nur wenn ein Config-Eintrag fuer den entstprechenden Remote-Host
		// existiert starten wir auch remote, sonst lokal
		if ((user == null) || (host == null)){
			lg.fine("Host oder Username f\u00FCr Remote-Ausf\u00FChrung " +
					"(Rechner " + nextHost + ") nicht gesetzt. Starte lokal");
			// Datei ausfuehren + warten bis auf den neuen Bot hingewiesen
			// werden
			controller.invokeBot(f);
		} else {
            try {
            	execute("scp "+f.getAbsolutePath()+" "+user+"@"+host+":. ").
            		waitFor();
				execute("ssh "+user+"@"+host+" chmod u+x "+f.getName()).
					waitFor();
				execute("ssh "+user+"@"+host+" ./"+f.getName()+" -t "+server);
			} catch (Exception e) {
				lg.warn(e, "Probleme beim Remote-Starten von Bot: "+
					f.getAbsolutePath()+" auf Rechner: "+user+"@"+host);
			}
		}

		nextHost++;
		nextHost %= 3;
	}

	/**
	 * Annahme: Keiner ausser uns startet Bots. Wenn jemand gleichzeitig
	 * @param b
	 * @throws SQLException
	 * @throws IOException
	 */
	private Bot executeBot(Blob b) throws SQLException, IOException {
		// Blob in Datei
		File f = File.createTempFile(
				ConfigManager.getValue("contestBotFileNamePrefix"),
				ConfigManager.path2Os(ConfigManager.getValue("contestBotFileNameSuffix")),
				new File(ConfigManager.getValue("contestBotTargetDir")));
		f.deleteOnExit(); //$$ deleteOnExit() scheint nicht zu klappen; Theorie:Prozesse noch offen wenn VM das aufrufen will
		lg.fine("Schreibe Bot nach '"+f.getAbsolutePath()+"'");
		Misc.copyStreamToStream(b.getBinaryStream(), new FileOutputStream(f));

		// Datei ausfuehren + warten bis auf den neuen Bot hingewiesen werden
		executeBot(f);
		synchronized (botArrivalLock) {
			//$$ Schoener waere vielleicht Verwendung von java.util.concurrent.Future
			// Schutz vor spurious wakeups (siehe Java-API-Doku zu wait())
			while (newlyArrivedBot == null) {
				try {
					botArrivalLock.wait();
				} catch (InterruptedException e) {
					lg.fine(e, "Wurde aufgeweckt. Kommt nur unter seltsamen " +
							"Umst\u00E4nden vor, aber f\u00FCr sich allein " +
							"unkritisch. Schlafe weiter.");
				}
			}
		}
		Bot rv = newlyArrivedBot;
		lg.fine("Gestarteter Bot '"+rv.getName()+"' ist korrekt " +
				"angemeldet; Go f\u00FCr ContestConductor");
		newlyArrivedBot = null;
		return rv;
	}

	public void onBotAdded(Bot bot) {
		synchronized (botArrivalLock) {
			newlyArrivedBot = bot;
			botArrivalLock.notifyAll();
        }
	}

	/**
	 * Startet ein Spiel
	 * @param game Verweis auf das Spiel
	 * @param isMainGame
	 * @throws SQLException
	 * @throws IOException
	 */
	private synchronized void startGame(ResultSet game)
	throws SQLException, IOException {
		int gameId  = game.getInt("game");
		int levelId = game.getInt("level");
		lg.info(
			"Starte Spiel; Level %d, Spiel %d, Bots %s (\"%s\") und %s " +
			"(\"%s\"), geplante Startzeit %s",
			levelId, gameId,
			game.getString("bot1"), db.getBotName(game.getInt("bot1")),
			game.getString("bot2"), db.getBotName(game.getInt("bot2")),
			game.getTimestamp("scheduled"));
		db.setGameRunning(levelId, gameId);

		lg.fine("Lade Parcours");
		controller.openWorldFromXmlString(db.getParcours(levelId));

		lg.fine("Starte Bot 1");
		botIds.put(executeBot(db.getBot1Binary()), db.getBot1Id());
		if (currentPhase == MAIN_ROUND) {
			lg.fine("Starte Bot 2");
			botIds.put(executeBot(db.getBot2Binary()), db.getBot2Id());
		}

		lg.fine("Go f\u00FCr Bots");
		// Bots starten
		//$$ sollte nicht hier sein
		for(Bot b : botIds.keySet()) {
			if (b instanceof CtBotSimTcp) //$$ sollte CtBot sein, wo sendRCCommand() auch hingehoert
				((CtBotSimTcp)b).sendRCCommand(
					RemoteControlGroupGUI.RC5_CODE_5);
		}

		lg.fine("Go f\u00FCr Controller");
		controller.unpause();
	}

	public void onWorldOpened(World newWorld) {
	    this.world = newWorld;
    }

	public void onBotRemoved(@SuppressWarnings("unused") Bot bot) {
		assert botIds.size() >= 1;
		assert botIds.size() <= 2;
		assert botIds.containsKey(bot);
		botIds.remove(bot);
    }

	public void onJudgeSet(@SuppressWarnings("unused") Judge j) {
		// $$ onJudgeSet()
    }
}
