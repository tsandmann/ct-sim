package ctSim.view.contestConductor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import ctSim.util.FmtLogger;
import ctSim.view.contestConductor.DatabaseAdapter.GameState;

/** <p>High-Level-Klasse, die die Spiele eines Turniers plant. Arbeitet eng
 * mit der Klasse {@link ContestConductor} zusammen.</p>
 *
 * <p><strong>Verwendungsbeispiel:</strong>
 * <ol>
 * <li><code>TournamentPlanner planner = new TournamentPlanner();</code></li>
 * <li><code>planner.{@link #planPrelimRound()};</code></li>
 * <li>Durchf&uuml;hren der Vorrundenspiele (durch die Klasse
 * ContestConductor)</li>
 * <li><code>planner.{@link #planMainRound()};</code></li>
 * <li>Durchf&uuml;hren der Hauptrundenspiele (durch die Klasse
 * ContestConductor)</li>
 * </ol>
 *
 * <p><a name="turnier-zwei-phasen" /><strong>Hintergrund:</strong> Das Turnier
 * wird in zwei Phasen abgewickelt: Zun&auml;chst wird in einer
 * Vorrunde jeder Bot einzeln durch einen Parcours geschickt und die
 * ben&ouml;tigte Zeit gestoppt, was eine Rangliste ergibt. In der
 * Hauptphase kommt dann das K.o.-System zum Einsatz, d.h. dass
 * sich jeweils der Gewinner eines Duells f&uuml;r das n&auml;chste
 * qualifiziert. Nur unter den vier besten werden alle Pl&auml;tze
 * ausgespielt.</p>
 *
 * <p>Die Rangliste aus der Vorrunde erm&ouml;glicht Ausgewogenheit im
 * Turnierplan der Hauptphase: Die Spieler werden so platziert, dass
 * sich der schnellste und der zweitschnellste aus der Vorrunde erst
 * im Finale begegnen k&ouml;nnen, der schnellste und der drittschnellste
 * erst im Halbfinale usw. So werden allzu verzerrte
 * Wettbewerbsergebnisse vermieden &ndash; g&auml;be es z.B. eine
 * einfache Auslosung statt einer Vorrunde, k&ouml;nnten zuf&auml;llig die
 * beiden besten Implementierungen in der ersten Runde aufeinandertreffen.
 * Das w&uuml;rde hei&szlig;en, dass einer der beiden Schnellsten schon
 * im ersten Durchgang ausscheidet, was seine tats&auml;chliche St&auml;rke
 * verf&auml;lscht widerspiegelt. Die Vorrunde soll das vermeiden
 * und helfen, die Spannung bis zuletzt aufrechtzuerhalten.</p>
 *
 * <p>F&uuml;r die Hauptphase gilt: Treten nicht gen&uuml;gend Teams an, um
 * alle Zweik&auml;mpfe des ersten Durchgangs zu f&uuml;llen, erhalten
 * m&ouml;glichst viele Bots ein Freilos, das sie automatisch f&uuml;r die
 * n&auml;chste Runde qualifiziert. Im Extremfall findet somit in der
 * ersten Runde des Turniers nur ein einziges Duell statt &ndash;
 * daf&uuml;r sind alle anschlie&szlig;enden Durchg&auml;nge voll besetzt.</p>
 *
 * @author Hendrik Krau&szlig; &lt;<a
 * href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class TournamentPlanner {
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	private PlannerToDatabaseAdapter db;

	//$$ doc
	/** Konstruiert einen TournamentPlanner, der mit der &uuml;bergebenen
	 * Datenbank verbunden ist. N&uuml;tzlich f&uuml;r Unit-Tests, die dem
	 * Planner &uuml;ber diesen Konstruktor eine Testdatenbank unterschieben
	 * k&ouml;nnen.
	 */
	public TournamentPlanner(PlannerToDatabaseAdapter database) {
		this.db = database;
	}

	/** Hilfsmethode: Denkt sich Spielzeiten aus f&uuml;r alle Spiele
	 * in einem Level und schreibt den Teilplan in die DB.
	 *
	 * @param levelId Prim&auml;rschl&uuml;ssel des Levels, f&uuml;r das
	 * die Zeiten gesetzt werden sollen.
	 */
	private void scheduleGames(int levelId) throws SQLException {
		ResultSet games = db.getGames(levelId, "game");
		games.last();
		if (games.getRow() == 0)
			return; // keine Spiele in diesem Level, nichts zu tun
		games.beforeFirst();

		// Spiele werden angesetzt in folgendem Intervall: Maximale
		// Spiellaenge plus ein bisschen Puffer
		int gameIntervalMillisec =
			db.getMaxGameLengthInMs(levelId) + 5*60*1000;
		Timestamp gameBegin = db.getLevelBegin(levelId);

		while (games.next()) {
			db.scheduleGame(levelId, games.getInt("game"), gameBegin);
			gameBegin.setTime(gameBegin.getTime() + gameIntervalMillisec);
		}
	}

	/** L&ouml;scht alle bisher geplanten Spiele und plant die Vorrunde.
	 * F&uuml;r jeden Bot, f&uuml;r den eine Binary in der Datenbank steht,
	 * wird ein Vorrundenspiel angelegt. Jeder Bot durchl&auml;uft ein
	 * Vorrundenspiel einzeln.
	 *
	 * @throws IllegalStateException Falls in der DB-Tabelle ctsim_level
	 * keine Angaben &uuml;ber die Vorrunde zu finden sind, oder falls
	 * weniger als zwei Bots mit Binary in der Datenbank zu finden sind.
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
			// Level -1 heisst Vorrunde
			db.createPrelimGame(i++, bots.getInt("id"));
		}
		scheduleGames(-1);
		lg.fine("Vorrunde geplant");
	}

	/** Hilfsmethode, um ohne Copy-Paste die gleiche Exception an
	 * mehreren Stellen verwenden zu k&ouml;nnen. */
	private static TournamentPlanException getWinnerNullExcp() {
		return new TournamentPlanException("Eins oder mehrere der " +
				"Vorrundenspiele haben winner == NULL. Hauptrunde kann " +
				"nicht geplant werden.");
	}

	/** Plant die Hauptrunde. Sie besteht aus $$ doc
	 *
	 * @throws TournamentPlanException
	 * @throws IllegalStateException
	 */
	public void planMainRound()
	throws SQLException, TournamentPlanException {
		lg.fine("Hauptrunde geplant");
		// Vorrunde holen
		ResultSet prelimRound = db.getGames(-1, "finishtime");
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
		prelimRound.first();
		TournamentTree<Integer> tree =
			new TournamentTree<Integer>(prelimRound.getInt("winner"));
		if (prelimRound.wasNull())
			throw getWinnerNullExcp();
		while(prelimRound.next()) {
			tree = tree.add(prelimRound.getInt("winner"));
			if (prelimRound.wasNull())
				throw getWinnerNullExcp();
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
						"in der Datenbank nicht gefunden. Fuer die gegebene " +
						"Anzahl Spiele werden alle Levels bis inkl. %d " +
						"benoetigt. Planung der Hauptrunde fehlgeschlagen.",
						i, tree.getLowestLevelId()));
			}
		}

		// Baum in die Datenbank und Zeitplanung erstellen
		for (int i = 1; i <= tree.getLowestLevelId(); i *= 2) {
			writeLevelToDb(tree, i);
			scheduleGames(i);
		}

		// Extrawurst fuer Spiel um den 3. Platz (sog. Semifinale)
		db.createMainGame(0, 1);
		scheduleGames(0);
		lg.fine("Hauptrunde geplant");
	}

	/** Schreibt ein Level aus einem TournamentTree in die Datenbank. Manche
	 * oder alle der Spieler, die im Baum sitzen, k&ouml;nnen
	 * <code>null</code> sein (d.h. erst,
	 * wenn die Ergebnisse niedrigerer Levels feststehen, r&uuml;cken Bots
	 * auf diese Pl&auml;tze vor). Wenn Spieler <code>null</code> sind, werden
	 * die zugeh&ouml;rigen Spiele trotzdem angelegt.
	 *
	 * @param tree Der TournamentTree, aus dem die Daten zu lesen sind.
	 * @param levelWanted Nummer des zu schreibenden Levels.
	 * @throws TournamentPlanException Falls
	 * {@link DatabaseAdapter#placeBot(Integer, int, int)} diese Exception
	 * wirft.
	 */
	private void writeLevelToDb(TournamentTree<Integer> tree, int levelWanted)
	throws SQLException, TournamentPlanException {
		int gameId = 1;
		// Spieler auf diesem Level; kann null enthalten
		ArrayList<Integer> players = tree.getTournamentPlan(levelWanted);
		assert players.size() % 2 == 0;
	    for (int i = 0; i < players.size(); i += 2) {
    		db.createMainGame(levelWanted, gameId);
   			db.placeBot(players.get(i), levelWanted, gameId);
   			db.placeBot(players.get(i + 1), levelWanted, gameId);
    		gameId++;
	    }
    }

	/** Diese Exception tritt auf, wenn etwas mit dem Turnierplan nicht stimmt.
	 * @author bbe (bbe@heise.de)
	 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
	 */
	static class TournamentPlanException extends Exception {
		private static final long serialVersionUID = -9195564639022108463L;

		public TournamentPlanException() { super(); }
		public TournamentPlanException(String message) { super(message); }
		public TournamentPlanException(Throwable cause) { super(cause); }
		public TournamentPlanException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
