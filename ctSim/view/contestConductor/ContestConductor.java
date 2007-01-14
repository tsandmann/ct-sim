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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ctSim.ConfigManager;
import ctSim.controller.Config;
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
			BotView winner = null;
			HashMap<BotView, Double> distToFinish =
				new HashMap<BotView, Double>();

			@SuppressWarnings("synthetic-access")
			GameOutcome() {
				for (BotView b : BotView.getAll()) {
					distToFinish.put(
						b,
						concon.world.getShortestDistanceToFinish(
							b.modelObj.getLastSafePos()));
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
			for (BotView b : BotView.getAll()) {
				if (concon.world.finishReached(b.modelObj.getPosition())) {
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
			o.winner = BotView.getAll().get(0);
			for (Map.Entry<BotView, Double> d : o.distToFinish.entrySet()) {
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
			concon.db.logUnconditionally(BotView.getAllModelObjects(),
				concon.world.getSimTimeInMs());

			// Restwege schreiben
			for (Map.Entry<BotView, Double> d :
				outcome.distToFinish.entrySet()) {
				concon.db.writeDistanceToFinish(d.getKey().idInDatabase,
					d.getValue());
			}

			// Spiel beenden
			concon.db.setWinner(outcome.winner.idInDatabase,
				concon.world.getSimTimeInMs());
		}
	}

	/**
	 * Merkt sich zu einem Bot (aus dem Model) die Datenbank-ID und, ob es sich
	 * um bot1 oder bot2 handelt (wichtig in der ctsim_game-Tabelle / in der
	 * ctsim_log-Tabelle).
	 *
	 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
	 */
	static class BotView {
		private static ArrayList<BotView> instances = new ArrayList<BotView>();

		static {
			instances.add(null);
			instances.add(null);
		}

		public final int idInDatabase;
		public final Bot modelObj;

		BotView(ctSim.model.bots.Bot modelObj, int idInDatabase, int bot0or1) {
			assert bot0or1 == 0 || bot0or1 == 1
			: "Parameter muss 0 oder 1 sein, ist aber " + bot0or1;
			assert instances.get(bot0or1) == null;

			this.modelObj = modelObj;
			this.idInDatabase = idInDatabase;
			instances.set(bot0or1, this);
		}

		static void remove(ctSim.model.bots.Bot b) {
			for (int i = 0; i < instances.size(); i++) {
				if (instances.get(i) != null && instances.get(i).modelObj == b) {
					instances.set(i, null);
					return;
				}
			}
			assert false;
		}

		static List<BotView> getAll() {
			ArrayList<BotView> rv = new ArrayList<BotView>();
			for (BotView v : instances) {
				if (v != null)
					rv.add(v);
			}
			return rv;
		}

		static List<ctSim.model.bots.Bot> getAllModelObjects() {
			ArrayList<Bot> rv = new ArrayList<Bot>();
			for (BotView v : getAll())
				rv.add(v.modelObj);
			return rv;
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
			if (db.gamesExist())
				recoverFromCrash();
			else
				// Turnier faengt neu an
				planner.planPrelimRound();
		} catch (SQLException e) {
			lg.severe(e, "Low-level-Datenbankproblem");
			assert false;
		}

		try {
			proceedWithNextGame();
		} catch (Exception e) {
			lg.severe(e, "Problem beim Durchf\u00FChren des ersten Spiels");
			assert false;
		}
	}

	private void recoverFromCrash()
	throws IllegalArgumentException, SQLException {
		lg.info("Abgebrochenes Turnier gefunden; versuche Wiederaufnahme");
		db.resetRunningGames();
		if (db.wasCrashDuringMainRound()) {
			lg.info("Vorrundenspiele abgeschlossen und Hauptrundenspiele " +
					"existieren -- Steige in Hauptrunde ein");
			currentPhase = MAIN_ROUND;
		}
	}

	public void onSimulationStep(
		@SuppressWarnings("unused") long simTimeInMs) {
		try {
			db.log(BotView.getAllModelObjects(), world.getSimTimeInMs());
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
			proceedWithNextGame();
		} catch (Exception e) {
			lg.severe(e, "Problem mit der Durchf\u00FChrung des Wettbewerbs");
			assert false;
		}
	}

	private void proceedWithNextGame()
	throws SQLException, IOException, TournamentPlanException {
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
				lg.info("Warte %d ms auf n\u00E4chsten Wettkampf (bis %s)",
					timeTilGameInMs, scheduled);
				Thread.sleep(timeTilGameInMs);
			} catch (InterruptedException e) {
				lg.warn(e, "ContestConductor aufgeweckt. Schlafe weiter.");
			}
		}
		// Zustand: Startzeitpunkt des Spiels erreicht
		startNextGame();
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
		String host = Config.getValue("host"+nextHost);
		String user = Config.getValue("host"+nextHost+"_username");

		String server = Config.getValue("ctSimIP");
		if (server == null)
			server = "localhost"; //$$ umziehen: Sollte in Config

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
			Config.getValue("contestBotFileNamePrefix"),
			ConfigManager.path2Os(Config.getValue("contestBotFileNameSuffix")),
			new File(Config.getValue("contestBotTargetDir")));
		f.deleteOnExit(); //$$ deleteOnExit() scheint nicht zu klappen; Theorie:Prozesse noch offen wenn VM das aufrufen will
		lg.fine("Schreibe Bot nach '"+f.getAbsolutePath()+"'");
		Misc.copyStreamToStream(b.getBinaryStream(), new FileOutputStream(f));

		// Datei ausfuehren + warten bis auf den neuen Bot hingewiesen werden
		executeBot(f);
		synchronized (botArrivalLock) {
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
	 * @throws SQLException
	 * @throws IOException
	 */
	private synchronized void startNextGame()
	throws SQLException, IOException {
		// Zu startendes Spiel koennte auch von unserem Aufrufer
		// (sleepAndStartNextGame()) uebergeben werden, aber wir fordern das
		// neu von der DB an, weil: Wenn sleepAndStartNextGame() das anfordert
		// und dann lang wartet, koennte in dieser Wartezeit die Verbindung
		// abreissen, was das ResultSet ungueltig macht -- ist in Tests auch
		// so passiert
		ResultSet game = db.getReadyGames();
		game.next();
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
		new BotView(executeBot(db.getBot1Binary()), db.getBot1Id(), 0);
		if (currentPhase == MAIN_ROUND) {
			lg.fine("Starte Bot 2");
			new BotView(executeBot(db.getBot2Binary()), db.getBot2Id(), 1);
		}

		lg.fine("Go f\u00FCr Bots");
		// Bots starten
		//$$ sollte nicht hier sein
		for(Bot b : BotView.getAllModelObjects()) {
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

	public void onBotRemoved(Bot bot) {
		BotView.remove(bot);
	}

	public void onJudgeSet(@SuppressWarnings("unused") Judge j) {
		// $$ onJudgeSet()
	}
}
