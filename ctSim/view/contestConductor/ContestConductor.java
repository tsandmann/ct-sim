package ctSim.view.contestConductor;

import static ctSim.view.contestConductor.ContestConductor.Phase.MAIN_ROUND;
import static ctSim.view.contestConductor.ContestConductor.Phase.PRELIM_ROUND;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3d;

import ctSim.ConfigManager;
import ctSim.SimUtils;
import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;
import ctSim.view.View;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;
import ctSim.view.gui.sensors.RemoteControlGroupGUI;

// $$ doc gesamte Klasse
// $$ die staendigen "assert false" sind nicht so doll. Schoener waere: Exceptions werfen und den Controller exit machen lassen (verbessert auch Testbarkeit (Unit-Tests) des ContestConductor). Ich zieh mir aber nicht den Schuh an, Exception-Handling in den z.Zt. noch chaotischen Controller reinzupopeln.

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

	public class NoMoreGamesException extends Exception {
		private static final long serialVersionUID = - 930001102842406374L;
	}

	public class ContestJudge extends Judge {
		public ContestJudge(DefaultController controller) {
			super(controller); //$$ Das ist Mist. Judge und die davon abgeleiteten Klassen muessen aufgeraeumt und vereinfacht werden
	    }

		@Override
        public boolean isStartingSimulationAllowed() {
			//$$ Doofe Methode, keine Ahnung wofuer die ueberhaupt da ist
	        return true;
        }

		@SuppressWarnings("synthetic-access")
        protected void setWinner(Bot winner)
		throws NullPointerException, SQLException, TournamentPlanException {
			lg.info("Zieleinlauf von Bot %s nach %s", winner.getName(),
				SimUtils.millis2time(world.getSimTimeInMs()));
			// Spiel beenden
			db.setWinner(botIds.get(winner), world.getSimTimeInMs());
		}

		@SuppressWarnings("synthetic-access")
		private boolean isAnyoneOnFinishTile()
		throws NullPointerException, SQLException, TournamentPlanException {
			for(Bot b : botIds.keySet()) {
				if (world.finishReached(new Vector3d(b.getPosition()))) {
					setWinner(b);
					return true;
				}
			}
			return false;
		}

		@SuppressWarnings("synthetic-access")
        private boolean isGameTimeoutElapsed() throws SQLException {
            return System.currentTimeMillis() - startTimeCurrentGame >=
            	db.getMaxGameLengthInMs();
		}

        @Override
		public boolean isSimulationFinished() {
            try {
	            return isAnyoneOnFinishTile() || isGameTimeoutElapsed();
            } catch (NullPointerException e) {
	            lg.severe(e, "Inkonsistenter Zustand: Es l\u00E4uft laut " +
	            		"Datenbank kein Spiel, laut Controller aber schon");
	            assert false;
            } catch (SQLException e) {
	            lg.severe(e, "Low-Level-Datenbankproblem");
	            assert false;
            } catch (TournamentPlanException e) {
				lg.severe(e, "Probleme beim Fortschreiben des Spielplans");
				assert false;
            }
            // unerreichbarer Code, aber man will ja den Compiler bei
            // Laune halten
            return false;
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

	/** Siehe {@link #Phase} */
	private Phase currentPhase = PRELIM_ROUND;

	/** Abstrahiert die Datenbank */
	private ConductorToDatabaseAdapter db;

	/** H&auml;lt die DB-Prim&auml;rschl&uuml;ssel der Bots */
	protected Map<Bot, Integer> botIds = new HashMap<Bot, Integer>();

	private TournamentPlanner planner;

	/** Referenz auf den Controller */
	private Controller controller;
	/** Referenz auf die Welt*/
	private World world;

	private Object botArrivalLock = new Object();
	private Bot newlyArrivedBot = null;

	/** Zeitpunkt, zu dem das aktuelle Spiel gestartet wurde. Einheit
	 * Millisekunden seit Beginn der Unix-&Auml;ra. */
	private long startTimeCurrentGame;

	/**
	 * Konstruktor
	 * @throws SQLException Falls die Verbindung zur Datenbank nicht
	 * hergestellt werden kann
	 * @throws ClassNotFoundException
	 */
	public ContestConductor(Controller controller)
	throws SQLException, ClassNotFoundException {
		this(controller,
			new ConductorToDatabaseAdapter(),
			new TournamentPlanner());
	}

	protected ContestConductor(Controller controller,
		ConductorToDatabaseAdapter db, TournamentPlanner planner) {
		this.controller = controller;
		this.db = db;
		this.planner = planner;
	}

	/**
	 * <p>
	 * Liefert den zu verwendenden Judge.
	 * </p>
	 * <p>
	 * Warum ist das eine eigene Methode? &ndash; Test-Cases k&ouml;nnen in den
	 * ctSim auf einfache Art ihren eigenen Test-Judge injizieren, indem eine
	 * Subklasse dieser Klasse gebildet wird. In der muss nicht mehr getan
	 * werden als diese Methode zu &uuml;berschreiben und den Test-Judge
	 * zur&uuml;ckzuliefern. Beispiel: {@link ContestConductorTest}.
	 * </p>
	 */
	protected Judge buildOurJudge(Controller c) {
		return new ContestJudge((DefaultController)c); //$$ Cast unschoen
	}

	public void onApplicationInited() {
		controller.setJudge(buildOurJudge(controller));
		try {
			planner.planPrelimRound();
		} catch (SQLException e) {
			lg.severe(e, "Vorrunde konnte nicht geplant werden");
			assert false;
		}
		try {
	        sleepAndStartNextGame();
        } catch (Exception e) {
	        lg.severe(e, "Problem beim Durchf\u00FChren des ersten Spiels");
	        assert false;
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
		lg.fine("Spiel beendet");
		try {
			try {
				sleepAndStartNextGame();
			} catch (NoMoreGamesException e) {
				switch (currentPhase) {
	                case PRELIM_ROUND:
	                	planner.planMainRound();
	                	// Dank Planner sind jetzt wieder Spiele "Ready"
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
				ConfigManager.getValue("contestBotFileNameSuffix"),
				new File(ConfigManager.getValue("contestBotTargetDir")));
		f.deleteOnExit(); //$$ deleteOnExit() scheint nicht zu klappen
		lg.fine("Schreibe Bot nach '"+f.getAbsolutePath()+"'");
		InputStream in = b.getBinaryStream();
		FileOutputStream out = new FileOutputStream(f);
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
		out.close();

		// Datei ausfuehren + warten bis auf den neuen Bot hingewiesen werden
		controller.invokeBot(f);
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
	private synchronized void startGame(ResultSet game) throws SQLException, IOException {
		int gameId  = game.getInt("game");
		int levelId = game.getInt("level");
		lg.info("Starte Spiel; Level %d, Spiel %d, geplante Startzeit %s",
				levelId, gameId, game.getTimestamp("scheduled"));
		db.setGameRunning(levelId, gameId);

		lg.fine("Lade Parcours");
		controller.openWorldFromXmlString(db.getParcours(levelId));

		lg.fine("Starte Bot 1");
		botIds.put(executeBot(db.getBot1Binary()), db.getBot1Id());
		if (currentPhase == MAIN_ROUND) {
			lg.fine("Starte Bot 2");
			botIds.put(executeBot(db.getBot2Binary()), db.getBot2Id());
		}

		startTimeCurrentGame = System.currentTimeMillis();

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

	public void onWorldOpened(World w) {
	    this.world = w;
    }

	public void onBotRemoved(@SuppressWarnings("unused") Bot bot) {
		assert botIds.size() >= 1;
		assert botIds.size() <= 2;
		assert botIds.containsKey(bot);
		botIds.remove(bot);
    }

	public void onJudgeSet(@SuppressWarnings("unused") Judge j) {
		// $$ onJudgeChanged()
    }
}
