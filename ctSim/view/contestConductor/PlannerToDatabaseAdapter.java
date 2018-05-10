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

/**
 * <p>
 * Stellt der Klasse {@link TournamentPlanner} Methoden zur Verfügung, um mit der Wettbewerbs-Datenbank
 * zu arbeiten. Mit anderen Worten: Sitzt zwischen der Datenbank und dem TournamentPlanner.
 * </p>
 * <p>
 * Näheres findet sich in der Dokumentation der Klasse {@link DatabaseAdapter}.
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class PlannerToDatabaseAdapter extends DatabaseAdapter {
	/** Hat dieselbe Funktion wie {@link DatabaseAdapter#DatabaseAdapter(ContestDatabase)}. */
	public PlannerToDatabaseAdapter(ContestDatabase db) {
		super(db);
	}

	/**
	 * Liefert den Zeitpunkt, für den das Spielen eines Levels angesetzt ist
	 *
	 * @param levelId	Primärschlüssel des Levels, dessen Startzeitpunkt geliefert werden soll
	 * @return Geplanter Zeitpunkt des Beginns des ersten Spiels des Levels.
	 * @throws SQLException
	 */
	public Timestamp getLevelBegin(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * from ctsim_level WHERE id ="+levelId);
		rs.next();
		return rs.getTimestamp("scheduled");
	}

	/**
	 * Getter
	 *
	 * @param levelId
	 * @return Sekunden
	 * @throws SQLException
	 */
	public int getGameIntervalInS(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * from ctsim_level WHERE id = ?",
				levelId);
		rs.next();
		return rs.getInt("gametime_real");
	}

	/**
	 * Legt ein Vorrundenspiel in der Datenbank an
	 *
	 * @param gameId		die Nummer des Spiels (innerhalb der Vorrunde).
	 * @param player1botId	Bot-Primärschlüssel des Spielers (Vorrundenspiele haben nur einen Spieler.)
	 * @throws SQLException
	 */
	public void createPrelimGame(int gameId, int player1botId)
			throws SQLException {
		execSql("INSERT INTO ctsim_game " +
				"(level, game, bot1, state) " +
				"VALUES (?, ?, ?, ?);",
				-1, gameId, player1botId, GameState.READY_TO_RUN);
	}

	/**
	 * Legt ein Hauptrundenspiel an
	 *
	 * Die Methode weist ihm den GameState#NOT_INITIALIZED zu.
	 *
	 * @param levelId	Schlüssel des Levels, auf dem das Spiel angelegt werden soll
	 * @param gameId	Schlüssel des Spiels innerhalb eines Levels
	 * @throws SQLException
	 */
	public void createMainGame(int levelId, int gameId)
			throws SQLException {
		execSql("INSERT INTO ctsim_game " +
				"(level, game, state) " +
				"VALUES (?, ?, ?);",
				levelId, gameId, GameState.NOT_INITIALIZED);
	}

	/**
	 * Löscht alle Spiele
	 *
	 * @throws SQLException
	 */
	public void clearGames() throws SQLException {
		execSql("DELETE FROM ctsim_game");
	}

	/**
	 * Liefert diejenigen Bots, die bereit zum Spielen sind (die über ausführbaren Code verfügen)
	 *
	 * @return Ein ResultSet, das die Bots repräsentiert, die als Spieler bereitstehen. Das ResultSet
	 * 				folgt dem Schema der Tabelle ctsim_bot.
	 * @throws SQLException
	 */
	public ResultSet getBotsWithBinary() throws SQLException {
		return execSql("SELECT * from ctsim_bot WHERE bin !=''");
	}

	/**
	 * Setzt die geplante Startzeit eines Spiels
	 *
	 * @param levelId	Schlüssel des Levels des Spiels
	 * @param gameId	Schlüssel des Spiels innerhalb eines Levels
	 * @param time		Zeit, zu der das Spiel beginnen soll
	 * @throws SQLException
	 */
	public void scheduleGame(int levelId, int gameId, Timestamp time)
			throws SQLException {
		execSql("UPDATE ctsim_game "+
				"SET scheduled='"+time+"' "+
				"WHERE level ="+levelId+ " AND game = "+gameId);
	}

	/**
	 * Zeigt an, ob ein Level in der Tabelle ctsim_level existiert oder nicht
	 *
	 * @param levelId	Primärschlüssel des Levels
	 * @return <code>true</code>, falls das Level existiert; <code>false</code> falls nicht
	 * @throws SQLException
	 */
	public boolean doesLevelExist(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * FROM ctsim_level WHERE id = "+levelId);
		return rs.next();
	}

	/**
	 * Liefert die Spiele eines Levels, sortiert nach einem Schlüssel
	 *
	 * @param levelId	Schlüssel des gewünschten Levels.
	 * @param sortKey	ein Spaltenname in der Datenbanktabelle ctsim_game, nach dem der Rückgabewert sortiert wird
	 * @return Ein ResultSet im Format der DB-Tabelle ctsim_game.
	 * @throws SQLException
	 */
	public ResultSet getGames(int levelId, String sortKey) throws SQLException {
		return execSql("SELECT * FROM ctsim_game" +
				" WHERE level = ? " +
				" ORDER BY " + sortKey, levelId);
	}
}
