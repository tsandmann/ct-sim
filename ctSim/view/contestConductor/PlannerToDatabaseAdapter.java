package ctSim.view.contestConductor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;


/** <p>Stellt der Klasse {@link TournamentPlanner} Methoden zur
 * Verf&uuml;gung, um mit der Wettbewerbs-Datenbank zu arbeiten. Mit
 * anderen Worten: Sitzt zwischen der Datenbank und dem TournamentPlanner.</p>
 *
 * <p>N&auml;heres in der Dokumentation der Klasse {@link DatabaseAdapter}.</p>
 *
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class PlannerToDatabaseAdapter extends DatabaseAdapter {
	/** Hat dieselbe Funktion wie {@link
	 * DatabaseAdapter#DatabaseAdapter(ContestDatabase)}. */
    public PlannerToDatabaseAdapter(ContestDatabase db) {
	    super(db);
    }

	/** Liefert den Zeitpunkt, f&uuml;r den das Spielen eines Levels angesezt
	 * ist.
	 *
	 * @param levelId Prim&auml;rschl&uuml;ssel des Levels, dessen
	 * Startzeitpunkt geliefert werden soll.
	 * @return Geplanter Zeitpunkt des Beginns des ersten Spiels des Levels.
	 * @throws SQLException
	 */
	public Timestamp getLevelBegin(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * from ctsim_level WHERE id ="+levelId);
		rs.next();
		return rs.getTimestamp("scheduled");
	}
	
	//$$ doc getGameInterval
	public int getGameIntervalInS(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * from ctsim_level WHERE id = ?",
			levelId);
		rs.next();
		return rs.getInt("gametime_real");
	}

	/** Legt ein Vorrundenspiel in der Datenbank an.
	 *
	 * @param gameId Die Nummer des Spiels (innerhalb der Vorrunde).
	 * @param player1botId Bot-Prim&auml;rschl&uuml;ssel des Spielers.
	 * (Vorrundenspiele haben nur einen Spieler.)
	 */
	public void createPrelimGame(int gameId, int player1botId)
	throws SQLException {
		execSql("INSERT INTO ctsim_game " +
				"(level, game, bot1, state) " +
				"VALUES (?, ?, ?, ?);",
				-1, gameId, player1botId, GameState.READY_TO_RUN);
	}

	/** Legt ein Hauptrundenspiel an. Die Methode weist ihm den
	 * {@link DatabaseAdapter.GameState#NOT_INITIALIZED} zu.
	 *
	 * @param levelId Schl&uuml;ssel des Levels, auf dem das Spiel angelegt
	 * werden soll.
	 * @param gameId Schl&uuml;ssel des Spiels innerhalb eines Levels.
	 */
	public void createMainGame(int levelId, int gameId)
	throws SQLException {
		execSql("INSERT INTO ctsim_game " +
				"(level, game, state) " +
				"VALUES (?, ?, ?);",
				levelId, gameId, GameState.NOT_INITIALIZED);
	}

	/**L&ouml;scht alle Spiele.
	 */
	public void clearGames() throws SQLException {
		execSql("DELETE FROM ctsim_game");
	}

	/**Liefert diejenigen Bots, die bereit zum Spielen sind (die &uuml;ber
	 * ausf&uuml;hrbaren Code verf&uuml;gen).
	 *
	 * @return Ein ResultSet, das die Bots repr&auml;sentiert, die als
	 * Spieler bereitstehen. Das ResultSet folgt dem Schema
	 * der Tabelle ctsim_bot.
	 */
	public ResultSet getBotsWithBinary() throws SQLException {
		return execSql("SELECT * from ctsim_bot WHERE bin !=''");
	}

	/** Liefert die Spiele eines Levels, sortiert nach einem Schl&uuml;ssel.
	 *
	 * @param levelId Schl&uuml;ssel des gew&uuml;nschten Levels.
	 * @param sortKey Ein Spaltenname in der Datenbanktabelle ctsim_game,
	 * nach dem der R&uuml;ckgabewert sortiert wird.
	 * @return Ein ResultSet im Format der DB-Tabelle ctsim_game.
	 */
	public ResultSet getGames(int levelId, String sortKey)
	throws SQLException {
		return execSql("SELECT * FROM ctsim_game" +
				" WHERE level ="+levelId+
				" ORDER BY '" + sortKey + "'");
	}

	/** Setzt die geplante Startzeit eines Spiels.
	 *
	 * @param levelId Schl&uuml;ssel des Levels des Spiels.
	 * @param gameId Schl&uuml;ssel des Spiels innerhalb eines Levels.
	 * @param time Zeit, zu der das Spiel beginnen soll.
	 */
	public void scheduleGame(int levelId, int gameId, Timestamp time)
	throws SQLException {
		execSql("UPDATE ctsim_game "+
				"SET scheduled='"+time+"' "+
				"WHERE level ="+levelId+ " AND game = "+gameId);
	}

	/** Zeigt an, ob ein Level in der Tabelle ctsim_level existiert oder nicht.
	 * @param levelId Prim&auml;rschl&uuml;ssel des Levels.
	 * @return <code>true</code>, falls das Level existiert;
	 * <code>false</code> falls nicht.
	 */
	public boolean doesLevelExist(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * FROM ctsim_level WHERE id = "+levelId);
	    return rs.next();
    }
}
