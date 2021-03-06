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
import ctSim.model.ThreeDBot;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.View;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

/** ContestConductor-View */
public class ContestConductor implements View {
	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	/** NoMoreGamesException */
	public static class NoMoreGamesException extends Exception {
		/** UID */
		private static final long serialVersionUID = - 930001102842406374L;
	}

	/** ContestJudge */
	public static class ContestJudge extends Judge {
		/** Für die Entfernung zum Ziel */
		class GameOutcome {
			/** Gewinner */
			BotView winner = null;
			/** Ziel-Entfernungen */
			HashMap<BotView, Double> distToFinish = Misc.newMap();

			/** Alle Entfernungen zum Ziel berechnen */
			GameOutcome() {
				for (BotView b : BotView.getAll()) {
					distToFinish.put(b, concon.world.getShortestDistanceToFinish(b.modelObj.getLastSafePos()));
				}
			}
		}

		/** Conductor */
		protected ContestConductor concon;

		/**
		 * @param controller	Controller des Sims
		 * @param concon		ContestConductor
		 */
		public ContestJudge(Controller controller, ContestConductor concon) {
			super((DefaultController)controller);
			this.concon = concon;
		}

		/**
		 * @see ctSim.model.rules.Judge#isStartingSimulationAllowed()
		 */
		@Override
		public boolean isStartingSimulationAllowed() {
			return true;
		}

		/**
		 * @see ctSim.model.rules.Judge#isSimulationFinished()
		 */
		@Override
		public boolean isSimulationFinished() {
			try {
				return isAnyoneOnFinishTile() || isGameTimeoutElapsed();
			} catch (NullPointerException e) {
				concon.lg.severe(e, "Inkonsistenter Zustand: Es läuft laut Datenbank kein Spiel, " +
						"laut Controller aber schon");
				assert false;
			} catch (SQLException e) {
				concon.lg.severe(e, "Low-Level-Datenbankproblem");
				assert false;
			} catch (TournamentPlanException e) {
				concon.lg.severe(e, "Probleme beim Fortschreiben des Spielplans");
				assert false;
			}
			// unerreichbarer Code, aber der Compiler sollte zufrieden gestellt werden
			return false;
		}

		/**
		 * Ist jemand am Ziel?
		 *
		 * @return true / false
		 * @throws NullPointerException
		 * @throws SQLException
		 * @throws TournamentPlanException
		 */
		private boolean isAnyoneOnFinishTile()
		throws NullPointerException, SQLException, TournamentPlanException {
			ThreeDBot winner = concon.world.whoHasWon();
			if (winner == null)
				return false;
			else {
				GameOutcome o = new GameOutcome();
				BotView winnerView = BotView.getViewOf(winner);
				o.winner = winnerView;
				o.distToFinish.put(winnerView, 0d);	// überschreiben

				setWinner(o);
				return true;
			}
		}

		/**
		 * Timeout eingetreten?
		 *
		 * @return true / false
		 * @throws SQLException
		 * @throws TournamentPlanException
		 */
		private boolean isGameTimeoutElapsed()
		throws SQLException, TournamentPlanException {
			if (concon.world.getSimTimeInMs() < concon.db.getMaxGameLengthInMs()) {
				// Spielzeit ist noch nicht um
				return false;
			}

			concon.lg.info("Spielzeit abgelaufen; Ermittle Bot, der dem Ziel am nächsten ist");

			GameOutcome o = new GameOutcome();
			o.winner = BotView.getAll().get(0);
			for (Map.Entry<BotView, Double> d : o.distToFinish.entrySet()) {
				if (d.getValue() < o.distToFinish.get(o.winner))
					o.winner = d.getKey();
			}

			setWinner(o);
			return true;
		}

		/**
		 * Setzt den Gewinner
		 *
		 * @param outcome	GameOutcome
		 * @throws NullPointerException
		 * @throws SQLException
		 * @throws TournamentPlanException
		 */
		protected void setWinner(GameOutcome outcome)
		throws NullPointerException, SQLException, TournamentPlanException {
			concon.lg.info("Gewinner ist Bot %s nach einem Spiel von %d ms", outcome.winner,
					concon.world.getSimTimeInMs());

			// letzten Schritt loggen
			// $$$ Das ist nicht so toll: Macht die Annahme, dass der DefaultController so bleibt, wie er ist
			concon.db.logUnconditionally(BotView.getAllModelObjects(),
					concon.world.getSimTimeInMs());

			// Restwege schreiben
			for (Map.Entry<BotView, Double> d : outcome.distToFinish.entrySet()) {
				concon.db.writeDistanceToFinish(d.getKey().idInDatabase, d.getValue());
			}

			// Spiel beenden
			concon.db.setWinner(outcome.winner.idInDatabase,
					concon.world.getSimTimeInMs());
		}
	}

	/**
	 * Merkt sich zu einem Bot (aus dem Model) die Datenbank-ID und, ob es sich um bot1 oder bot2 handelt
	 * (wichtig in der ctsim_game-Tabelle und in der ctsim_log-Tabelle).
	 */
	static class BotView {
		/** Bot-Instanzen */
		private static ArrayList<BotView> instances = Misc.newList();

		static {
			instances.add(null);
			instances.add(null);
		}

		/** ID für die Datenbank */
		public final int idInDatabase;

		/** 3D-Bot */
		public final ThreeDBot modelObj;

		/**
		 * @param modelObj		ThreeDBot
		 * @param idInDatabase	ID für die Datenbank
		 * @param bot0or1		Welcher Bot?
		 */
		BotView(ThreeDBot modelObj, int idInDatabase, int bot0or1) {
			assert bot0or1 == 0 || bot0or1 == 1 : "Parameter muss 0 oder 1 sein, ist aber " + bot0or1;
			assert instances.get(bot0or1) == null;

			this.modelObj = modelObj;
			this.idInDatabase = idInDatabase;
			instances.set(bot0or1, this);
		}

		/**
		 * Enterfernt einen Bot
		 *
		 * @param b	Bot
		 */
		static void remove(ctSim.model.bots.Bot b) {
			for (int i = 0; i < instances.size(); i++) {
				if (instances.get(i) != null && instances.get(i).modelObj == b) {
					instances.set(i, null);
					return;
				}
			}
			assert false;
		}

		/**
		 * @return Alle BotViews
		 */
		static List<BotView> getAll() {
			ArrayList<BotView> rv = Misc.newList();
			for (BotView v : instances) {
				if (v != null)
					rv.add(v);
			}
			return rv;
		}

		/**
		 * @return Alle ThreeDBots
		 */
		static List<ThreeDBot> getAllModelObjects() {
			List<ThreeDBot> rv = Misc.newList();
			for (BotView v : getAll())
				rv.add(v.modelObj);
			return rv;
		}

		/**
		 * @param modelObj	ThreeDBot
		 * @return BotView eines ThreeDBots
		 */
		static BotView getViewOf(ThreeDBot modelObj) {
			for (BotView v : instances) {
				if (v.modelObj == modelObj)
					return v;
			}
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Status des ContestConductor-Subsystems
	 * Typischer Ablauf:
	 *
	 * <pre>
	 * PRELIM_ROUND -&gt; MAIN_ROUND -&gt; (Programmende)
	 * </pre>
	 */
	enum Phase {
		/** Status des ContestConductor-Subsystems */
		PRELIM_ROUND,
		/** Status des ContestConductor-Subsystems */
		MAIN_ROUND,
	}

	static {
		Main.dependencies.registerImplementations(
			ContestJudge.class,
			TournamentPlanner.class,
			ConductorToDatabaseAdapter.class,
			PlannerToDatabaseAdapter.class	// $$$ in seine Klasse?
		);
	}

	/** Siehe {@link Phase} */
	private Phase currentPhase = PRELIM_ROUND;

	/** Abstrahiert die Datenbank */
	private ConductorToDatabaseAdapter db;
	/** Planer */
	private TournamentPlanner planner;

	/** Referenz auf den Controller */
	private Controller controller;
	/** Welt */
	private World world;
	/** Lock */
	private Object botArrivalLock = new Object();
	/** neuester Bot */
	private ThreeDBot newlyArrivedBot = null;

	/**
	 * @param controller
	 * @param db
	 * @param planner
	 */
	public ContestConductor(Controller controller, ConductorToDatabaseAdapter db, TournamentPlanner planner) {
		this.controller = controller;
		this.db = db;
		this.planner = planner;
	}

	/**
	 * @see ctSim.view.View#onApplicationInited()
	 */
	public void onApplicationInited() {
		controller.setJudge(Main.dependencies.get(ContestJudge.class));

		try {
			if (db.gamesExist())
				recoverFromCrash();
			else
				// Turnier fängt neu an
				planner.planPrelimRound();
		} catch (SQLException e) {
			lg.severe(e, "Low-level-Datenbankproblem");
			assert false;
		}

		try {
			proceedWithNextGame();
		} catch (Exception e) {
			lg.severe(e, "Problem beim Durchführen des ersten Spiels");
			assert false;
		}
	}

	/**
	 * Crash-Recover
	 *
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 */
	private void recoverFromCrash()
	throws IllegalArgumentException, SQLException {
		lg.info("Abgebrochenes Turnier gefunden; versuche Wiederaufnahme");
		db.resetRunningGames();
		if (db.wasCrashDuringMainRound()) {
			lg.info("Vorrundenspiele abgeschlossen und Hauptrundenspiele existieren -- Steige in Hauptrunde ein");
			currentPhase = MAIN_ROUND;
		}
	}

	/**
	 * @see ctSim.view.View#onSimulationStep(long)
	 */
	public void onSimulationStep(long simTimeInMs) {
		try {
			db.log(BotView.getAllModelObjects(), world.getSimTimeInMs());
		} catch (Exception e) {
			lg.warn(e, "Probleme beim Loggen des Spielzustands in die DB");
		}
	}

	/**
	 * @see ctSim.view.View#onSimulationFinished()
	 */
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
			lg.severe(e, "Problem mit der Durchführung des Wettbewerbs");
			assert false;
		}
	}

	/**
	 * Weiter mit nächstem Spiel
	 *
	 * @throws SQLException
	 * @throws IOException
	 * @throws TournamentPlanException
	 */
	@SuppressWarnings("incomplete-switch")
	private void proceedWithNextGame() throws SQLException, IOException, TournamentPlanException {
		try {
			sleepAndStartNextGame();
		} catch (NoMoreGamesException e) {
			switch (currentPhase) {
				case PRELIM_ROUND:
					planner.planMainRound();
					// dank Planner sind jetzt wieder Spiele "ready"
					currentPhase = MAIN_ROUND;
					try {
						sleepAndStartNextGame();
					} catch (NoMoreGamesException e1) {
						lg.severe(e1, "Planer hat versagt: Hauptrunde nicht oder falsch geplant");
						assert false;
					}
					break;

				case MAIN_ROUND:
					lg.info("Turnier erfolgreich abgeschlossen. Beende den ctSim");
					System.exit(0);
					break;
			}
		}
	}

	/**
	 * Neues Spiel
	 *
	 * @throws SQLException
	 * @throws IOException
	 * @throws NoMoreGamesException
	 */
	private synchronized void sleepAndStartNextGame() throws SQLException, IOException, NoMoreGamesException {
		ResultSet game = db.getReadyGames();
		if (! game.next())
			throw new NoMoreGamesException();

		Timestamp scheduled = game.getTimestamp("scheduled");

		long timeTilGameInMs;
		while ((timeTilGameInMs =
			scheduled.getTime() - System.currentTimeMillis()) > 0) {
			try {
				lg.info("Warte %d ms auf nächsten Wettkampf (bis %s)", timeTilGameInMs, scheduled);
				Thread.sleep(timeTilGameInMs);
			} catch (InterruptedException e) {
				lg.warn(e, "ContestConductor aufgeweckt. Schlafe weiter.");
			}
		}
		// Zustand: Startzeitpunkt des Spiels erreicht
		startNextGame();
	}

	/**
	 * Startet einem Prozess
	 *
	 * @param commandAndArgs	Binary-Name und Argumente
	 * @return Prozess zum Binary
	 * @throws IOException
	 */
	private Process execute(String commandAndArgs) throws IOException {
		lg.fine("Starte " + commandAndArgs);
		return Runtime.getRuntime().exec(commandAndArgs);
	}

	/** Welcher Rechner soll den nächsten Bot ausführen */
	private int nextHost = 1;

	/**
	 * Startet einen Bot, entweder lokal oder remote
	 *
	 * @param f
	 */
	private void executeBot(File f){
		String host = Config.getValue("host" + nextHost);
		String user = Config.getValue("host" + nextHost + "_username");

		String server = Config.getValue("ctSimIP");
		if (server == null)
			server = "localhost";	// $$$ umziehen: Sollte in Config

		/*
		 * Nur wenn ein Config-Eintrag für den entsprechenden Remote-Host existiert starten wir auch remote,
		 * sonst lokal
		 */
		if ((user == null) || (host == null)){
			lg.fine("Host oder Username für Remote-Ausführung (Rechner " + nextHost + ") nicht gesetzt. Starte lokal");
			// Datei ausführen und warten bis auf den neuen Bot hingewiesen wird
			controller.invokeBot(f);
		} else {
			try {
				execute("scp " + f.getAbsolutePath() + " " + user + "@" + host + ":. ").waitFor();
				execute("ssh " + user + "@" + host + " chmod u+x " + f.getName()).waitFor();
				execute("ssh " + user + "@" + host + " ./" + f.getName() + " -t " + server);
			} catch (Exception e) {
				lg.warn(e, "Probleme beim Remote-Starten von Bot: " + f.getAbsolutePath() + " auf Rechner: " +
						user + "@" + host);
			}
		}

		nextHost++;
		nextHost %= 3;
	}

	/**
	 * Annahme: Niemand außer uns startet Bots, falls jemand gleichzeitig aktiv wird
	 *
	 * @param b
	 * @return ThreeDBot-Instanz des Bots
	 * @throws SQLException
	 * @throws IOException
	 */
	private ThreeDBot executeBot(Blob b) throws SQLException, IOException {
		// Binary Large Object in Datei
		File f = File.createTempFile(
			Config.getValue("contestBotFileNamePrefix"),
			ConfigManager.path2Os(Config.getValue("contestBotFileNameSuffix")),
			new File(Config.getValue("contestBotTargetDir")));
		f.deleteOnExit();
		// $$$ deleteOnExit() scheint nicht zu klappen; Theorie:Prozesse noch offen wenn VM das aufrufen will
		lg.fine("Schreibe Bot nach '" + f.getAbsolutePath() + "'");
		Misc.copyStreamToStream(b.getBinaryStream(), new FileOutputStream(f));

		// Datei ausführen und warten bis auf den neuen Bot hingewiesen wird
		executeBot(f);
		synchronized (botArrivalLock) {
			// Schutz vor spurious wakeups (siehe Java-API-Doku zu wait())
			while (newlyArrivedBot == null) {
				try {
					botArrivalLock.wait();
				} catch (InterruptedException e) {
					lg.fine(e, "Wurde aufgeweckt. Kommt nur unter seltsamen Umständen vor, " +
							"aber für sich allein unkritisch. Schlafe weiter.");
				}
			}
		}
		final ThreeDBot rv = newlyArrivedBot;
		rv.addDisposeListener(new Runnable() {
			public void run() {
				BotView.remove(rv);
			}
		});
		lg.fine("Gestarteter Bot '" + rv + "' ist korrekt angemeldet; Go für ContestConductor");
		newlyArrivedBot = null;
		return rv;
	}

	/**
	 * Handler für neuer Bot ist da
	 *
	 * @param bot	Bot
	 */
	public void onBotAdded(Bot bot) {
		if (! (bot instanceof ThreeDBot)) {
			throw new IllegalStateException("Bot angemeldet, ist aber kein ThreeDBot");
		}
		synchronized (botArrivalLock) {
			newlyArrivedBot = (ThreeDBot)bot;
			botArrivalLock.notifyAll();
		}
	}

	/**
	 * Startet ein Spiel
	 *
	 * @throws SQLException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private synchronized void startNextGame() throws SQLException, IOException {
		/**
		 * Zu startendes Spiel könnte auch von unserem Aufrufer (sleepAndStartNextGame()) übergeben werden,
		 * aber wir fordern das neu von der DB an, weil: Wenn sleepAndStartNextGame() das anfordert
		 * und dann lang wartet, könnte in dieser Wartezeit die Verbindung abreißen, was das ResultSet
		 * ungültig macht - ist in Tests auch so passiert
		 */
		ResultSet game = db.getReadyGames();
		game.next();
		int gameId  = game.getInt("game");
		int levelId = game.getInt("level");
		lg.info("Starte Spiel; Level %d, Spiel %d, Bots %s (\"%s\") und %s (\"%s\"), geplante Startzeit %s",
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

		lg.fine("Go für Controller");
		controller.unpause();
	}

	/**
	 * @see ctSim.view.View#onWorldOpened(ctSim.model.World)
	 */
	public void onWorldOpened(World newWorld) {
		this.world = newWorld;
	}

	/**
	 * @see ctSim.view.View#onJudgeSet(ctSim.model.rules.Judge)
	 */
	public void onJudgeSet(Judge j) {
		// No-op
	}

	/**
	 * @see ctSim.view.View#onResetAllBots()
	 */
	public void onResetAllBots() {
		// No-op
	}
}
