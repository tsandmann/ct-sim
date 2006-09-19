package ctSim.view.contestConductor;

import static ctSim.view.contestConductor.ContestConductor.State.MAIN_ROUND_BETWEEN_GAMES;
import static ctSim.view.contestConductor.ContestConductor.State.NOT_INITIALIZED;
import static ctSim.view.contestConductor.ContestConductor.State.PRELIM_ROUND_BETWEEN_GAMES;
import static ctSim.view.contestConductor.ContestConductor.State.PRELIM_ROUND_DONE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
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
import ctSim.view.gui.Debug;
import ctSim.view.gui.sensors.RemoteControlGroupGUI;

// $$ doc gesamte Klasse

/**
 * <p>
 * Erm&ouml;glicht die Durchf&uuml;hrung eines ctBot-Wettbewerbs wie den im
 * Oktober 2006.
 * </p>
 * <p>
 * <strong>Architektur</strong> des ContestConductor-Subsystems: <!-- Ascii-Art
 * NICHT neu formatieren oder "reparieren"! Die Verschiebungen durch die
 * Link-Tags sind okay, siehe Ausgabe im Browser -->
 *
 * <pre>
 *                                hat einen
 *       {@link ContestConductor} -------------------------&gt; {@link TournamentPlanner}
 *              |                                            |
 *              |                                            |
 *              | hat einen                                  | hat einen
 *              |                                            |
 *              v                                            v
 *     {@link ConductorToDatabaseAdapter} ----.     .---- {@link PlannerToDatabaseAdapter}
 *                                    |     |
 *                                    |     |
 *                 ist abgeleitet von |     | ist abgeleitet von
 *                                    |     |
 *                                    v     v
 *                                {@link DatabaseAdapter}
 *                                       |
 *                                       |
 *                                       | verbunden mit
 *                                       |
 *                                       v
 *                                MySQL-Datenbank
 * </pre>
 *
 * </p>
 *
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ContestConductor implements View {
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	public class ContestJudge extends Judge {
		public ContestJudge(Controller controller) {
			super((DefaultController)controller); //$$ Das ist Mist. Judge und die davon abgeleiteten Klassen muessen aufgeraeumt und vereinfacht werden
	    }

		@SuppressWarnings("synthetic-access")
		private boolean isAnyoneOnFinishTile() {
			for(Bot b : botIds.keySet()) {
				if(world.finishReached(new Vector3d(b.getPosition()))) {
					// Zustand: wir haben einen Gewinner
					Debug.out.println("Zieleinlauf \""+b.getName()+"\" nach "+
							SimUtils.millis2time(world.getSimTimeInMs()));
					kissArrivingBot(b, world.getSimTimeInMs());
					return true;
				}
			}
			return false;
		}

		@SuppressWarnings("synthetic-access")
        private boolean isGameTimeoutElapsed() {
            try {
                return System.currentTimeMillis() - startTimeCurrentGame >=
                	db.getMaxGameLengthInMs();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                lg.severe(e, "Problem beim Lesen des Spiel-Timeouts aus der " +
                		"Datenbank");
                return false;
            }
		}

        @Override
		public boolean isSimulationFinished() {
			return isAnyoneOnFinishTile() || isGameTimeoutElapsed();
		}
	}

	/** Status dieses Judge. Typischer Ablauf:
	 * <pre>
	 *    NOT_INITIALIZED
	 * -> PRELIM_ROUND_BETWEEN_GAMES -> PRELIM_ROUND_IN_GAME
	 * -> PRELIM_ROUND_BETWEEN_GAMES -> PRELIM_ROUND_IN_GAME
	 *    ... viele Spiele ...
	 * -> PRELIM_ROUND_BETWEEN_GAMES -> PRELIM_ROUND_IN_GAME
	 * -> PRELIM_ROUND_DONE
	 * -> MAIN_ROUND_BETWEEN_GAMES -> MAIN_ROUND_IN_GAME
	 * -> MAIN_ROUND_BETWEEN_GAMES -> MAIN_ROUND_IN_GAME
	 *    ... viele Spiele ...
	 * -> MAIN_ROUND_BETWEEN_GAMES -> MAIN_ROUND_IN_GAME -> (Schluss)</pre>
	 */
	enum State {
		NOT_INITIALIZED,
		PRELIM_ROUND_BETWEEN_GAMES,
		PRELIM_ROUND_IN_GAME,
		PRELIM_ROUND_DONE,
		MAIN_ROUND_BETWEEN_GAMES,
		MAIN_ROUND_IN_GAME,
	}

	/** Siehe {@link #State} */
	private State state = NOT_INITIALIZED;

	/** Abstrahiert die Datenbank */
	private ConductorToDatabaseAdapter db;

	/** H&auml;lt die DB-Prim&auml;rschl&uuml;ssel der Bots */
	private Map<Bot, Integer> botIds = new HashMap<Bot, Integer>();

	private TournamentPlanner planner;

	/** Referenz auf den Controller */
	private Controller ctrlr;
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
		ctrlr = controller;
		db = new ConductorToDatabaseAdapter();
		planner = new TournamentPlanner();
		ctrlr.setJudge(new ContestJudge(ctrlr));
	}

	public void update(@SuppressWarnings("unused") long simTimeInMs) {
		doWork();
	}

	/** Wird von au&szlig;en einmal pro Simulatorschritt aufgerufen. Hier
	 * verrichtet der Judge die haupts&auml;chliche Arbeit.
	 */
	public synchronized void doWork() {
		lg.fine("Eintritt in doWork(); Status "+state);
		try {
			while (true) {
				switch (state) {
					case NOT_INITIALIZED:
		                planner.planPrelimRound();
		                setState(PRELIM_ROUND_BETWEEN_GAMES);
						break;

					case PRELIM_ROUND_BETWEEN_GAMES: {
							ResultSet games = db.getReadyGames();
							if (games.next())
								sleepAndStartGame(games);
							else
								setState(PRELIM_ROUND_DONE);
							break;
						}

					case PRELIM_ROUND_DONE:
						planner.planMainRound();
						setState(MAIN_ROUND_BETWEEN_GAMES);
						break;

					case MAIN_ROUND_BETWEEN_GAMES: {
							ResultSet games = db.getReadyGames();
							if (games.next())
								sleepAndStartGame(games);
							else {
								lg.info("Es gibt keine weiteren " +
									"Rennen mehr. Beende das " +
									"Programm");
								System.exit(0);
							}
							break;
						}

					case PRELIM_ROUND_IN_GAME:
					case MAIN_ROUND_IN_GAME:
						logGameStateToDb();
						break;
				}
			}
		} catch (Exception e) {
			lg.severe(e, "Problem im Ablauf des Wettbewerbs");
			//$$ Besseres Error-handling
		}
	}

	private void setState(State state) {
		lg.fine("Gehe \u00FCber zu Status "+state);
		this.state = state;
	}

	/**
	 *
	 * @param game
	 * @throws SQLException
	 * @throws IOException
	 */
	private synchronized void sleepAndStartGame(ResultSet game)
	throws SQLException, IOException {
		Timestamp scheduled = game.getTimestamp("scheduled");
		// lokales Kalendersystem mit aktueller Zeit
		Calendar now = Calendar.getInstance();

		long timeTilGameMillisec = scheduled.getTime() - now.getTimeInMillis();
		if (timeTilGameMillisec > 0){
			try {
				lg.fine("Warte "+
					timeTilGameMillisec+" ms auf naechsten Wettkampf ("+
					scheduled+")");
				Thread.sleep(timeTilGameMillisec);
			} catch (InterruptedException e) {
				//$$ InterruptedExcp? was hier machen?
			}
		} else
			// Zustand: wir wollen ein Spiel starten
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
		lg.fine("Schreibe Bot nach '"+f.getAbsolutePath()+"'");
		InputStream in = b.getBinaryStream();
		FileOutputStream out = new FileOutputStream(f);
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
		out.close();

		// Datei ausfuehren
		ctrlr.invokeBot(f);
		// Warten bis uns der Controller auf den neuen Bot hinweist
		try {
			synchronized (botArrivalLock) {
				// Schutz vor spurious wakeups (siehe Java-API-Doku zu wait())
				//$$ Schoener waere Verwendung von java.util.concurrent.Future
				while (newlyArrivedBot == null)
					botArrivalLock.wait();
			}
			Bot rv = newlyArrivedBot;
			lg.fine("Gestarteter Bot '"+rv.getName()+"' ist korrekt " +
					"angemeldet; Freigabe f\u00FCr ContestConductor");
			newlyArrivedBot = null;
			return rv;
		} catch (InterruptedException e) {
			// $$ was soll man hier machen? return was?
			e.printStackTrace();
			return null;
		}
	}

	public void addBot(Bot bot) {
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
		int gameId = game.getInt("game");
		int levelId= game.getInt("level");
		lg.info(String.format("Starte Spiel; Level %d, Spiel %d, " +
				"geplante Startzeit %s",
				levelId, gameId, game.getTimestamp("scheduled")));
		startTimeCurrentGame = System.currentTimeMillis();
		db.setGameRunning(levelId, gameId);

		lg.fine("Lade Parcours");
		ctrlr.openWorldFromXmlString(db.getParcours(levelId));

		lg.fine("Starte Bot 1");
		// Bots laden
		botIds.put(executeBot(db.getBot1Binary()), db.getBot1Id());
		// Wenn kein Vorrundenspiel, auch zweiten Spieler holen
		if (db.isCurrentGameMainRound()) {
			lg.fine("Starte Bot 2");
			botIds.put(executeBot(db.getBot2Binary()), db.getBot2Id());
		}

		lg.fine("Go f\u00FCr Bots");
		// Bots starten
		//$$ sollte nicht hier sein
		for(Bot b : botIds.keySet()) {
			if (b instanceof CtBotSimTcp) //$$ sollte ctBot sein, wo sendRCCommand() auch hingehoert
				((CtBotSimTcp)b).sendRCCommand(
					RemoteControlGroupGUI.RC5_CODE_5);
		}

		lg.fine("Go f\u00FCr Controller");
		ctrlr.unpause();
	}

	/**
	 *
	 * @param bot
	 * @param time	Simulatorzeit [ms] seit Start des Spiels
	 */
	private void kissArrivingBot(Bot bot, long time) {
		lg.info("Bot "+bot.getName()+" hat das Ziel nach "+
				time+" ms erreicht!");
		if (db.isCurrentGameMainRound())
			setState(MAIN_ROUND_BETWEEN_GAMES);
		else
			setState(PRELIM_ROUND_BETWEEN_GAMES);

		try {
			// Spiel beenden
			db.setWinner(botIds.get(bot), time);
		} catch (SQLException e) {
			lg.warning(e, "Probleme beim Sichern der Zieldaten");
		} catch (TournamentPlanException e) {
			lg.warning(e, "Probleme beim Fortschreiben des Spielplans");
		}

		// Alle Bots entfernen
		//$$ wieso macht sowas nicht der Controller?
		for (Bot b : botIds.keySet())
			b.die();
	}

	private void logGameStateToDb() {
		Iterator<Bot> it = botIds.keySet().iterator();
		Bot b1 = it.next();
		Bot b2 = it.next();
		db.log(b1, b2, world.getSimTimeInMs());
	}

	public void openWorld(World w) {
	    this.world = w;
    }

	public void removeBot(@SuppressWarnings("unused") Bot bot) {
	    // $$ removeBot()
    }

	public void onApplicationInited() {
	    doWork();
    }
}
