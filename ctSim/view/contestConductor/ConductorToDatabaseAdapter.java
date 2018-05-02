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

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.ThreeDBot;
import ctSim.model.World;
import ctSim.util.Misc;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

/**
 * <p>
 * Stellt der Klasse ContestConductor Methoden zur Verfügung, um mit der Wettbewerbs-Datenbank zu
 * arbeiten. Mit anderen Worten: Sitzt zwischen der Datenbank und dem ContestConductor.
 * </p>
 * <p>
 * Näheres in der Dokumentation der Klasse {@link DatabaseAdapter}.
 * </p>
 * <p>
 * Diese Klasse weiß als einzige, was gerade gespielt wird (den Schlüssel des zur Zeit laufenden Spiels).
 * Das ist so, weil diese Information von den meisten in dieser Klasse wohnenden Methoden gebraucht wird,
 * und außerhalb dieser Klasse nie.
 * </p>
 *
 * @see DatabaseAdapter
 * @see ContestConductor
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class ConductorToDatabaseAdapter extends DatabaseAdapter {
	// Annahme: Keiner modifiziert ctsim_game während wir laufen
	/**
	 * 8 für Achtelfinale, 4 für Viertelfinale usw. Ist zusammen mit 'currentGameId' DB-Schlüssel für
	 * ein Spiel.
	 *
	 * Integer (nicht int), um Zugriffe im uninitialisierten Zustand per NullPointerException mitgeteilt
	 * zu bekommen (sonst würde im SQL einfach id == 0 auftauchen)
	 */

	/**
	 * ID des aktuellen Spiels innerhalb des momentanen Level. Ist zusammen mit 'currentLevelId'
	 * DB-Schlüssel für ein Spiel.
	 *
	 * Integer (nicht int), um Zugriffe im uninitialisierten Zustand per NullPointerException mitgeteilt
	 * zu bekommen (sonst würde im SQL einfach id == 0 auftauchen)
	 */
	class Game {
		/** Game-ID */
		private Integer gameId;
		/** Level-ID */
		private Integer levelId;
		/** UID */
		private Integer cachedUniqueId;
		/** Cache-Zeit */
		private Integer cachedMaxLengthInMs;

		/**
		 * @return LevelID
		 */
		public Integer getLevelId() {
			return levelId;
		}

		/**
		 * @return GameID
		 */
		public Integer getGameId() {
			return gameId;
		}

		/**
		 * Setter
		 *
		 * @param levelId
		 * @param gameId
		 */
		public void set(Integer levelId, Integer gameId) {
			if ((levelId == null && gameId != null)
					|| (levelId != null && gameId == null)) {
				throw new IllegalArgumentException();
			}

			// Caches löschen
			cachedUniqueId = null;
			cachedMaxLengthInMs = null;
			this.levelId = levelId;
			this.gameId = gameId;
		}

		/**
		 * @return ID
		 * @throws SQLException
		 * @throws IllegalStateException
		 */
		public Integer getUniqueId()
				throws SQLException, IllegalStateException {
			if (gameId == null)
				throw new IllegalStateException();

			if (cachedUniqueId == null) {
				ResultSet rs = execSql("SELECT * FROM ctsim_game " +
						"WHERE game = ? AND level = ?", gameId, levelId);
				rs.next();
				cachedUniqueId = rs.getInt("id");
				assert ! rs.wasNull();
			}
			return cachedUniqueId;
		}

		/**
		 * Liefert die Zeit, wie lange das Spiel maximal dauern darf. Nach dieser Zeit soll das Spiel vom
		 * Judge als abgebrochen werden.
		 *
		 * @return Geplante Simzeit [ms], wie lange ein Spiel des Levels dieses Spiels maximal dauern darf.
		 * @throws SQLException
		 * @throws IllegalStateException
		 * @see World#getSimTimeInMs()
		 */
		public int getMaxLengthInMs()
				throws SQLException, IllegalStateException {
			if (gameId == null)
				throw new IllegalStateException();

			if (cachedMaxLengthInMs == null) {
				ResultSet rs = execSql("SELECT * from ctsim_level WHERE id = ?",
						getLevelId());
				rs.next();
				cachedMaxLengthInMs = rs.getInt("gametime");
				assert ! rs.wasNull();
			}
			return cachedMaxLengthInMs;
		}
	}

	/** TBD */
	private static final int logOneIn = 20;

	/** max Log-Entries */
	private int discardedLogEntries = 0;

	/** Game */
	private final Game currentGame = new Game();

	/** Hat dieselbe Funktion wie {@link DatabaseAdapter#DatabaseAdapter(ContestDatabase)}. */
	public ConductorToDatabaseAdapter(ContestDatabase db) {
		super(db);
	}

	/**
	 * Macht einen Log-Eintrag in die Datenbank. Das Log wird von der JavaScript-Applikation der Autoren
	 * von der Online-Redaktion benutzt, um den aktuellen Spielstand im Web anzuzeigen.
	 *
	 * @param bots	Ein Set, das ein oder zwei Bots enthält. Position, Blickrichtung und Status der Bots
	 * 				wird in die Datenbank geloggt.
	 * @param simTimeElapsed	Simulatorzeit [ms] seit Beginn des Spiels
	 * @throws IllegalArgumentException	falls der Parameter <code>bots</code> weniger als ein oder mehr
	 * 				als zwei Elemente enthält.
	 * @throws SQLException
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 *
	 * @see World#getSimTimeInMs()
	 */
	public void log(List<ThreeDBot> bots, long simTimeElapsed)
			throws IllegalArgumentException, SQLException, NullPointerException {
		if (discardedLogEntries < logOneIn - 1) {
			discardedLogEntries++;
			return;
		}

		discardedLogEntries = 0;
		logUnconditionally(bots, simTimeElapsed);
	}

	/**
	 * @param bots
	 * @param simTimeElapsed
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 * @throws NullPointerException
	 */
	public void logUnconditionally(List<ThreeDBot> bots, long simTimeElapsed)
			throws IllegalArgumentException, SQLException, NullPointerException {
		// Gesundheitscheck
		if (bots.size() != 1 && bots.size() != 2) {
			throw new IllegalArgumentException("Falsche Anzahl von Bots im " +
					"Set: erwarteter Wert 1 oder 2, tatsächlicher Wert "+
					bots.size());
		}

		// Hauptcode
		String common =
				"insert into ctsim_log (" +
						"game, logtime, " +
						"pos1x, pos1y, head1x, head1y, state1";
		String oneBot =
				") " +
						"values (?, ?, ?, ?, ?, ?, ?)";
		String twoBots =
				", " +
						"pos2x, pos2y, head2x, head2y, state2) " +
						"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		ArrayList<Object> values = Misc.newList();
		values.add(currentGame.getUniqueId());
		values.add(simTimeElapsed);
		Iterator<ThreeDBot> it = bots.iterator();

		ThreeDBot b1 = it.next();
		values.addAll(getFieldValues(b1));

		if (it.hasNext()) {
			// wir haben zwei Bots
			ThreeDBot b2 = it.next();
			values.addAll(getFieldValues(b2));
			execSql(common + twoBots, values.toArray());
		} else {
			// wir haben einen Bot
			execSql(common + oneBot, values.toArray());
		}
	}

	/**
	 * Gibt die Daten eines Bots zurück
	 *
	 * @param b	Bot
	 * @return Liste der Daten
	 */
	private List<Object> getFieldValues(ThreeDBot b) {
		Point3d pos = b.getPositionInWorldCoord();
		Vector3d head = b.getHeadingVectorInWorldCoord();
		return Arrays.asList(new Object[] {
				pos.x,
				pos.y,
				head.x,
				head.y,
				b.getLegacyObstState()});
	}


	/**
	 * Reset
	 *
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 */
	public void resetRunningGames()
			throws IllegalArgumentException, SQLException {
		ResultSet rs = execSql(
				"SELECT * FROM ctsim_game WHERE state = ?", GameState.RUNNING);
		while (rs.next()) {
			execSql("DELETE FROM ctsim_log " +
					"WHERE game = ? ", rs.getInt("id"));
		}

		execSql("UPDATE ctsim_game SET state = ? WHERE state = ?",
				GameState.READY_TO_RUN, GameState.RUNNING);
	}

	/**
	 * @return true/false
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 */
	public boolean gamesExist()
			throws IllegalArgumentException, SQLException {
		return execSql("SELECT * FROM ctsim_game").next();
	}

	/**
	 * @return Crash?
	 * @throws IllegalArgumentException
	 * @throws SQLException
	 */
	public boolean wasCrashDuringMainRound()
			throws IllegalArgumentException, SQLException {
		boolean incompletePrelimGamesExist = execSql(
				"SELECT * FROM ctsim_game WHERE level = -1 AND state != ?",
				GameState.GAME_OVER).next();
		boolean isMainRoundPlanned = execSql(
				"SELECT * FROM ctsim_game WHERE level != -1").next();
		return ! incompletePrelimGamesExist && isMainRoundPlanned;
	}

	/**
	 * Liefert das aus der Datenbank kommende XML, das einen Parcours beschreibt.
	 *
	 * @param levelId	Der Schlüssel des Levels, dessen Parcours angefordert wird.
	 * @return Das XML-Dokument, das den Parcours beschreibt (selbes Schema wie das on-disk-Parcours-Schema).
	 * @throws SQLException
	 */
	public String getParcours(int levelId) throws SQLException {
		ResultSet rs = execSql(
				"SELECT * FROM ctsim_level WHERE id = ?", levelId);
		rs.next();
		return rs.getString("parcours");
	}

	/**
	 * @param botId
	 * @param distanceToFinishInM
	 * @throws SQLException
	 */
	public void writeDistanceToFinish(int botId, double distanceToFinishInM)
			throws SQLException {
		// Feld "bot1restweg" oder "bot2restweg" setzen
		execSql(
				"UPDATE ctsim_game SET " + getColumnName(botId) + "restweg = ? " +
						"WHERE level = ? AND game = ?",
						distanceToFinishInM,
						currentGame.getLevelId(), currentGame.getGameId());
	}

	/**
	 * @param botId
	 * @return Spaltenname
	 * @throws SQLException
	 */
	private String getColumnName(int botId) throws SQLException {
		ResultSet rs = execSql("SELECT bot1, bot2 FROM ctsim_game " +
				"WHERE level = ? AND game = ?",
				currentGame.getLevelId(), currentGame.getGameId());
		rs.next();

		int b1 = rs.getInt("bot1");
		if (rs.wasNull())
			// Fehlerzustand sowohl in Hauptrunden- als auch Vorrundenspiel
			throw new IllegalStateException();

		if (b1 == botId)
			return "bot1";
		else {
			int b2 = rs.getInt("bot2");
			// b2 == NULL heißt Vorrundenspiel, d.h. Fehler weil botId nicht gefunden
			if (rs.wasNull() || b2 != botId)
				throw new IllegalStateException();
			return "bot2";
		}
	}


	/**
	 * Zeichnet in der Datenbank auf, wer Gewinner des aktuellen Spiels war, und beendet das aktuelle Spiel.
	 * Setzt außerdem den Gewinner ein Level weiter.
	 *
	 * @param winnerBotId	Primärschlüssel des Bots, der das Spiel gewonnen hat.
	 * @param finishSimTime	Nach wieviel Simulatorzeit [ms] seit Beginn des Spiels wurde die Ziellinie
	 * 				überschritten?
	 * @throws SQLException
	 * @throws TournamentPlanException	falls das aktuelle Spiel ein Hauptrundenspiel ist und der Gewinner
	 * 				in ein Level weitergesetzt wird, für das schon zwei Spieler eingetragen sind. Mit
	 * 				anderen Worten, falls der Turnierbaum verunstaltet ist.
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	public void setWinner(int winnerBotId, long finishSimTime)
			throws SQLException, TournamentPlanException, NullPointerException {
		lg.finer("Schreibe Gewinner (Bot-ID %d, Simzeit %dms) in die DB",
				winnerBotId, finishSimTime);
		execSql(
				"UPDATE ctsim_game " +
						"SET winner = ?, finishtime = ?, state = ? "+
						"WHERE level = ? AND game = ?",
						winnerBotId, finishSimTime, GameState.GAME_OVER,
						currentGame.getLevelId(), currentGame.getGameId());

		propagateWinner(winnerBotId);

		currentGame.set(null, null);
	}

	/**
	 * <p>
	 * Liefert die ID des Bots, der der Verlierer des aktuellen Spiels ist, wenn man die übergebene Zahl
	 * als die ID des Gewinners annimmt. Mit anderen Worten, liefert aus dem aktuellen Spiel den
	 * Spielpartner des übergebenen Spielers. Funktioniert nur in Hauptrundenspielen.
	 * </p>
	 *
	 * @param winnerBotId	Bot-ID des Gewinners des aktuellen Spiels
	 * @return Bot-ID		Bot-ID des Verlierers des aktuellen Spiels
	 * @throws SQLException
	 * @throws IllegalStateException	wenn in den beiden botIds des aktuellen Spiels der Wert NULL in
	 * 				der Datenbank auftaucht. (Wirft daher auch diese Exception, wenn die Methode aufgerufen
	 * 				wird, während ein Vorrundenspiel läuft.)
	 * @throws NullPointerException		falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	private int getCurrentGameLoserId(int winnerBotId)
			throws SQLException, IllegalStateException, NullPointerException {
		ResultSet currentGameRs = execSql(
				"SELECT * FROM ctsim_game WHERE level = ? AND game = ?",
				currentGame.getLevelId(), currentGame.getGameId());
		currentGameRs.next();
		int b1 = currentGameRs.getInt("bot1");
		if (currentGameRs.wasNull())
			throw new IllegalStateException();
		int b2 = currentGameRs.getInt("bot2");
		if (currentGameRs.wasNull())
			throw new IllegalStateException();

		if (b1 == winnerBotId)
			return b2;
		else
			return b1;
	}

	/**
	 * Setzt den Gewinner des Spiels ein Level höher
	 *
	 * Tut nichts, falls das aktuelle Spiel ein Vorrundenspiel ist.
	 *
	 * @param winnerBotId	Bot-ID des Gewinners
	 * @throws SQLException
	 * @throws TournamentPlanException	falls in dem Spiel, in das der Gewinner des aktuellen Spiels
	 * 				gesetzt werden soll, bereits zwei Spieler eingetragen sind (= falls der Turnierbaum
	 * 				verunstaltet ist).
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	private void propagateWinner(int winnerBotId)
			throws SQLException, TournamentPlanException, NullPointerException {
		if (! isCurrentGameMainRound())
			return;	// kein Weitersetz-Mechanismus für die Vorrunde
		switch (currentGame.getLevelId()) {
		case 0:	// Spiel um den 3. Platz wurde gespielt
		case 1:	// Finale wurde gespielt
			lg.finer("Gewinner des Finales oder Spiels um den 3. Platz " +
					"wird im Turnierbaum nicht weitergesetzt");
			break;
		case 2:	// Halbfinale wurde gespielt
			lg.finer("Setze Gewinner ins Finale");
			placeBot(winnerBotId, 1, 1);
			lg.finer("Setze Verlierer ins Spiel um den 3. Platz");
			// 0 == Sonderwert für Spiel um den 3. Platz
			placeBot(getCurrentGameLoserId(winnerBotId), 0, 1);
			break;
		default:	// jedes andere Level
			lg.finer("Setze Gewinner ein Level weiter");
			placeBot(winnerBotId,
					currentGame.getLevelId()/2,
					(currentGame.getGameId()+1)/2);
		}
	}

	/**
	 * Liefert die Binary eines der beiden Bots im aktuellen Spiel
	 *
	 * @param whichPlayer	Name des DB-Felds, das angibt, welcher Bot. Kann im gegenwärtigen DB-Schema
	 * 				"bot1" oder "bot2" sein.
	 * @return Die mit dem gewählten Bot im aktuellen Spiel assoziierte Binary (ELF).
	 * @throws SQLException
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	private Blob getBotBinary(String whichPlayer)
			throws SQLException, NullPointerException {
		String sql = String.format(
				"SELECT bin " +
						"FROM ctsim_bot AS bot, ctsim_game AS game " +
						"WHERE game.%s = bot.id " +
						"AND game.level = ? " +
						"AND game.game  = ?;",
						whichPlayer);
		ResultSet binary = execSql(sql,
				currentGame.getLevelId(), currentGame.getGameId());
		binary.next();
		return binary.getBlob("bin");
	}

	/**
	 * Liefert die Binary von Spieler 1 des aktuellen Spiels
	 *
	 * @return Die mit Teilnehmer 1 des aktuellen Spiels assoziierte Binary (ELF).
	 * @throws SQLException
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	public Blob getBot1Binary() throws SQLException {
		return getBotBinary("bot1");
	}

	/**
	 * Liefert die Binary von Spieler 2 des aktuellen Spiels
	 *
	 * @return Die mit Teilnehmer 2 des aktuellen Spiels assoziierte Binary (ELF).
	 * @throws SQLException
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	public Blob getBot2Binary() throws SQLException {
		return getBotBinary("bot2");
	}

	/**
	 * Liefert den Schlüssel eines der Spieler im zur Zeit laufenden Spiel
	 *
	 * @param whichPlayer	Gibt das DB-Feld an, das den gewünschten Schlüssel enthält.
	 * 				Dies kann "bot1" oder "bot2" sein.
	 * @return Bot-ID
	 * @throws SQLException
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	private int getBotId(String whichPlayer)
			throws SQLException, NullPointerException {
		ResultSet rs = execSql(
				"SELECT * FROM ctsim_game WHERE level = ? AND game = ?;",
				currentGame.getLevelId(), currentGame.getGameId());
		rs.next();
		return rs.getInt(whichPlayer);
	}

	/**
	 * Liefert den DB-Schlüssel von Spieler 1 des zur Zeit laufenden Spiels
	 *
	 * @return Bot-ID
	 * @throws SQLException
	 */
	public int getBot1Id() throws SQLException {
		return getBotId("bot1");
	}

	/**
	 * Liefert den DB-Schlüssel von Spieler 2 des zur Zeit laufenden Spiels
	 *
	 * @return Bot-ID
	 * @throws SQLException
	 */
	public int getBot2Id() throws SQLException {
		return getBotId("bot2");
	}

	/**
	 * @param botId
	 * @return Botname
	 * @throws SQLException
	 */
	public String getBotName(int botId) throws SQLException {
		ResultSet rs = execSql("SELECT * FROM ctsim_bot WHERE id = ?", botId);
		if (rs.next())
			return rs.getString("name");
		else
			return null;
	}

	/**
	 * @return Game-Länge in ms
	 * @throws SQLException
	 */
	public int getMaxGameLengthInMs() throws SQLException {
		return currentGame.getMaxLengthInMs();
	}

	/**
	 * Liefert die Spiele, die bereit zur Durchführung sind
	 *
	 * @return Ein ResultSet, das auf das Spiel zeigt. Das Format ist das der Zeilen in der Tabelle
	 * 				ctsim_game. Wenn der erste next()-Aufruf auf dem ResultSet den Wert <code>false</code>
	 * 				liefert, gibt es keine Spiele mehr, die bereit zur Durchführung wären.
	 * @throws SQLException
	 */
	public ResultSet getReadyGames() throws SQLException {
		// Nur Spiele wählen, die READY_TO_RUN haben
		return execSql(
				"SELECT * FROM ctsim_game " +
						"WHERE state = ? ORDER BY scheduled",
						GameState.READY_TO_RUN);
	}

	/**
	 * Startet ein Spiel, d.h. setzt seinen Status in der Datenbank entsprechend
	 *
	 * @param levelId	Level des zu startenden Spiels
	 * @param gameId	Nummer des zu startenden Spiels innerhalb seines Levels
	 * @throws SQLException
	 */
	public void setGameRunning(int levelId, int gameId) throws SQLException {
		execSql("UPDATE ctsim_game " +
				"SET state= ? "+
				"WHERE level = ? AND game = ?",
				GameState.RUNNING, levelId, gameId);
		currentGame.set(levelId, gameId);
		discardedLogEntries = 0;
	}

	/**
	 * Zeigt an, ob das zur Zeit laufende Spiel ein Hauptrundenspiel ist
	 *
	 * @return <code>true</code>, falls das aktuelle Spiel zur Hauptrunde gehört (ein anderes Level als
	 * 				-1 hat); <code>false</code>, falls das aktuelle Spiel zur Vorrunde gehört (ein Level
	 * 				von -1 hat).
	 * @throws NullPointerException	falls zur Zeit kein Spiel läuft, d.h. falls noch nie
	 * 				<code>setGameRunning</code> aufgerufen wurde oder falls seit dem letzten
	 * 				<code>setGameRunning</code>-Aufruf das Spiel durch Aufruf von <code>setWinner</code>
	 * 				beendet wurde.
	 */
	private boolean isCurrentGameMainRound() throws NullPointerException {
		return currentGame.getLevelId() != -1;
	}
}