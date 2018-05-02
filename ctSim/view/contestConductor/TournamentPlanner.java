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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import ctSim.util.FmtLogger;
import ctSim.view.contestConductor.DatabaseAdapter.GameState;

/**
 * <p>
 * High-Level-Klasse, die die Spiele eines Turniers plant. Arbeitet eng mit der Klasse
 * {@link ContestConductor} zusammen. Plant einen vollständigen Wettbewerb: Schreibt den Turnierbaum in
 * die Datenbank (<a href="package-summary.html#turnierbaum">Was ist der Turnierbaum?</a>)
 * </p>
 * <p>
 * <strong>Verwendungsbeispiel:</strong>
 * <ol>
 * <li><code>TournamentPlanner planner = new TournamentPlanner();</code></li>
 * <li><code>planner.{@link #planPrelimRound()};</code></li>
 * <li>Durchführen der Vorrundenspiele (durch die Klasse ContestConductor)</li>
 * <li><code>planner.{@link #planMainRound()};</code></li>
 * <li>Durchführen der Hauptrundenspiele (durch die Klasse ContestConductor)</li>
 * </ol>
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class TournamentPlanner {
	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	/** Datenbank */
	private PlannerToDatabaseAdapter db;

	/**
	 * Konstruiert einen TournamentPlanner, der mit der übergebenen Datenbank verbunden ist. Nützlich für
	 * Unit-Tests, die dem Planner über diesen Konstruktor eine Testdatenbank unterschieben können.
	 *
	 * @param database
	 */
	public TournamentPlanner(PlannerToDatabaseAdapter database) {
		this.db = database;
	}

	/**
	 * Hilfsmethode: Denkt sich Spielzeiten für alle Spiele in einem Level aus und schreibt sie in die DB.
	 *
	 * @param levelId	Primärschlüssel des Levels, für das die Zeiten gesetzt werden sollen
	 * @throws SQLException
	 */
	private void scheduleGames(int levelId) throws SQLException {
		ResultSet games = db.getGames(levelId, "game");
		games.last();
		if (games.getRow() == 0)
			return;	// keine Spiele in diesem Level, also nichts zu tun
		games.beforeFirst();

		Timestamp gameBegin = db.getLevelBegin(levelId);
		while (games.next()) {
			db.scheduleGame(levelId, games.getInt("game"), gameBegin);
			gameBegin.setTime(
					gameBegin.getTime() + db.getGameIntervalInS(levelId) * 1000);
		}
	}

	/**
	 * Löscht alle bisher geplanten Spiele und plant die Vorrunde
	 *
	 * Für jeden Bot, für den eine Binary in der Datenbank steht, wird ein Vorrundenspiel angelegt.
	 * Jeder Bot durchläuft ein Vorrundenspiel einzeln.
	 *
	 * @throws SQLException
	 * @throws IllegalStateException	falls in der Datenbank-Tabelle ctsim_level keine Angaben über die
	 * 			Vorrunde zu finden sind, oder falls weniger als zwei Bots mit Binary in der Datenbank zu
	 * 			finden sind.
	 */
	public void planPrelimRound() throws SQLException {
		lg.fine("Plane Vorrunde");
		ResultSet bots = db.getBotsWithBinary();
		// Gesundheitscheck
		if (! db.doesLevelExist(-1)) {
			throw new IllegalStateException("Level -1 " +
					"(Vorrunde) in der Datenbank nicht gefunden. Vorrunde " +
					"kann nicht geplant werden.");
		}
		bots.last();
		if (bots.getRow() < 2) {
			throw new IllegalStateException("Weniger als zwei Bots mit " +
					"Binary in der Datenbank. Vorrunde kann nicht " +
					"geplant werden.");
		}

		// Hauptcode
		db.clearGames();
		bots.beforeFirst();
		int i = 1;
		while (bots.next()) {
			// Level -1 heißt Vorrunde
			db.createPrelimGame(i++, bots.getInt("id"));
		}
		scheduleGames(-1);
		lg.fine("Vorrunde geplant");
	}

	/**
	 * Plant die Hauptrunde
	 *
	 * Sie besteht aus $$ doc planMainRound.
	 *
	 * @throws SQLException
	 * @throws TournamentPlanException
	 * @throws IllegalStateException
	 */
	public void planMainRound()
			throws SQLException, TournamentPlanException {
		lg.fine("Hauptrunde geplant");
		// Vorrunde holen
		ResultSet prelimRound = db.getGames(-1, "finishtime, bot1restweg");
		// Gesundheitscheck
		while (prelimRound.next()) {
			if (! GameState.GAME_OVER.toString().equals(
					prelimRound.getString("state"))) {
				throw new TournamentPlanException("Eins oder mehrere der " +
						"Vorrundenspiele haben noch nicht den Status '"+
						GameState.GAME_OVER+"'. Hauptrunde kann nicht geplant " +
						"werden.");
			}
		}
		prelimRound.last();
		if (prelimRound.getRow() == 0) {
			throw new IllegalStateException("Keine Vorrundenspiele in der " +
					"Datenbank gefunden. Hauptrunde kann nicht geplant " +
					"werden.");
		}

		// Hauptcode
		prelimRound.beforeFirst();
		TournamentTree tree = new TournamentTree();
		while(prelimRound.next()) {
			tree.add(prelimRound.getInt("winner"));
			if (prelimRound.wasNull()) {
				throw new TournamentPlanException("Eins oder mehrere der " +
						"Vorrundenspiele haben winner == NULL. Hauptrunde kann " +
						"nicht geplant werden.");
			}
		}

		// Gesundheitscheck
		if (! db.doesLevelExist(0)) {
			throw new IllegalStateException("Level 0 (Spiel um den 3. " +
					"Platz) in der Datenbank nicht gefunden. Planung der " +
					"Hauptrunde fehlgeschlagen.");
		}
		for (int i = 1; i <= tree.getLowestLevelId(); i *= 2) {
			if (! db.doesLevelExist(i)) {
				throw new IllegalStateException(String.format(
						"Level %d " +
								"in der Datenbank nicht gefunden. Für die gegebene " +
								"Anzahl Spiele werden alle Levels bis inkl. %d " +
								"benötigt. Planung der Hauptrunde fehlgeschlagen.",
								i, tree.getLowestLevelId()));
			}
		}

		// Baum in die Datenbank und Zeitplanung erstellen
		for (int i = 1; i <= tree.getLowestLevelId(); i *= 2) {
			writeLevelToDb(tree.getTournamentPlan(i), i);
			scheduleGames(i);
		}

		// Extrawurst für Spiel um den 3. Platz (sog. Semifinale)
		db.createMainGame(0, 1);
		scheduleGames(0);
		lg.fine("Hauptrunde geplant");
	}

	/**
	 * Schreibt ein Level aus einem TournamentTree in die Datenbank. Manche oder alle der Spieler, die in
	 * dem Level sitzen, können <code>null</code> sein (d.h. anfangs steht nicht fest, wer im Achtelfinale
	 * spielt, erst nach Spielen des Sechzehntelfinales wird das nach und nach klar). Wenn Spieler
	 * <code>null</code> sind, werden die zugehörigen Spiele trotzdem angelegt.
	 *
	 * @param players	eine Liste von Bot-IDs, die die Spieler repräsentieren. <code>null</code> bedeutet,
	 * 			dass für dieses Spiel (noch) kein Spieler vorgesehen ist.
	 * @param levelId	Nummer des zu schreibenden Levels.
	 * @throws SQLException
	 * @throws TournamentPlanException	falls {@link DatabaseAdapter#placeBot(Integer, int, int)} diese
	 * 			Exception wirft.
	 */
	private void writeLevelToDb(ArrayList<Integer> players, int levelId)
			throws SQLException, TournamentPlanException {
		int gameId = 1;
		// Spieler auf diesem Level; kann null enthalten
		assert players.size() % 2 == 0;
		for (int i = 0; i < players.size(); i += 2) {
			db.createMainGame(levelId, gameId);
			db.placeBot(players.get(i), levelId, gameId);
			db.placeBot(players.get(i + 1), levelId, gameId);
			gameId++;
		}
	}

	/**
	 * Diese Exception tritt auf, wenn etwas mit dem Turnierplan nicht stimmt.
	 *
	 * @author Benjamin Benz (bbe@heise.de)
	 * @author Hendrik Krauß (hkr@heise.de)
	 */
	static class TournamentPlanException extends Exception {
		/** UID */
		private static final long serialVersionUID = -9195564639022108463L;

		/** Exception */
		public TournamentPlanException() { super(); }

		/**
		 * Exception
		 *
		 * @param message	Text
		 */
		public TournamentPlanException(String message) { super(message); }

		/**
		 * Exception
		 *
		 * @param cause
		 */
		public TournamentPlanException(Throwable cause) { super(cause); }

		/**
		 * Exception
		 *
		 * @param message	Text
		 * @param cause
		 */
		public TournamentPlanException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
