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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import ctSim.controller.Main;
import ctSim.view.contestConductor.DatabaseAdapter.GameState;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

/**
 * Testklasse
 */
public class TournamentPlannerTest extends ConductorTestUtil {
	/** Datenbank */
	private PlannerToDatabaseAdapter db;
	/** Test */
	private TournamentPlanner testee;

	/**
	 * @see ctSim.view.contestConductor.ConductorTestUtil#getDbFromChildClass()
	 */
	
    @Override
	protected DatabaseAdapter getDbFromChildClass() {
	    return db;
    }

	// Vorsicht, wenn hier Exceptions auftreten, verschluckt
	// JUnit die und meldet nur unverständlich "No runnable methods". Im
	// Zweifel main()-Methode schreiben, die das hier aufruft, und als
	// Applikation (nicht Unit-Test) laufenlassen
	/**
	 * Tests
	 */
	public TournamentPlannerTest() {
		Main.dependencies.reRegisterImplementation(ContestDatabase.class,
			TestDatabase.class);
		db = Main.dependencies.get(PlannerToDatabaseAdapter.class);
		testee = Main.dependencies.get(TournamentPlanner.class);
    }

	/**
	 * Tests
	 * @throws SQLException
	 */
	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithMissingLevel() throws SQLException {
		db.execSql("delete from ctsim_bot");
		makeBot(1, "Friedrich-Wilhelm");
		makeBot(2, "Friedrich-Wilhelm II");
		makeBot(3, "Friedrich-Wilhelm III");
		db.execSql("delete from ctsim_level");
		testee.planPrelimRound();
	}

	/**
	 * Tests
	 * @throws SQLException
	 */
	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithMissingBots() throws SQLException {
		db.execSql("delete from ctsim_bot");
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		testee.planPrelimRound();
	}

	/**
	 * Tests
	 * @throws SQLException
	 */
	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithTooFewBots() throws SQLException {
		db.execSql("delete from ctsim_bot");
		makeBot(1, "Wittgenstein");
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		testee.planPrelimRound();
	}

	/**
	 * Tests
	 * @throws SQLException
	 */
	@Test
	public void planPrelimRound() throws SQLException {
		int gameIntervalInS = 9876;
		ResultSet rs;
		// 3 Bots => 2 Spiele
		db.execSql("delete from ctsim_game");
		db.execSql("delete from ctsim_level");
		makeLevel(-1, gameIntervalInS);
		db.execSql("delete from ctsim_bot");
		makeBot(1, "Friedrich-Wilhelm I");
		makeBot(2, "Friedrich-Wilhelm II");
		makeBot(3, "Friedrich-Wilhelm III");
		testee.planPrelimRound();
		rs = db.execSql("select * from ctsim_game");
		assertTrue(rs.next());
		assertEquals(-1, rs.getInt("level"));
		assertEquals(1, rs.getInt("game"));
		rs.last();
		assertEquals(3, rs.getRow());

		// keine doppelten Spiele?
		rs = db.execSql("select * from ( " +
			"select level, game, count(*) as c " +
			"from ctsim_game group by level, game ) as x " +
			"where c > 1");
		if (rs.next()) {
			fail(String.format("Mehrere Spiele haben Level %d Game %d. " +
					"(Level-Game-Kombinationen sollten immer eindeutig sein)",
					rs.getInt("level"), rs.getInt("game")));
		}

		// richtige zeitliche Abstände?
		assertScheduledRightInterval(-1, gameIntervalInS);
	}

	/**
	 * Tests
	 * @throws SQLException
	 * @throws TournamentPlanException
	 */
	@Test(expected=IllegalStateException.class)
	public void mainRoundWithNoPrelim()
	throws SQLException, TournamentPlanException {
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		makeLevel(1);
		makeLevel(2);
		db.execSql("delete from ctsim_game");
		testee.planMainRound();
	}

	/**
	 * Tests
	 * @throws SQLException
	 * @throws TournamentPlanException
	 */
	@Test(expected=TournamentPlanException.class)
	public void mainRoundWithIncompletePrelim()
	throws SQLException, TournamentPlanException {
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		makeLevel(1);
		makeLevel(2);
		db.execSql("delete from ctsim_game");
		makePrelimGame(1, GameState.GAME_OVER, 0);
		makePrelimGame(2, GameState.RUNNING, 0);
		makePrelimGame(3, GameState.GAME_OVER, 0);
		testee.planMainRound();
	}

	/**
	 * Tests
	 * @throws SQLException
	 * @throws TournamentPlanException
	 */
	@Test(expected=TournamentPlanException.class)
	public void mainRoundWithMissingWinners()
	throws SQLException, TournamentPlanException {
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		makeLevel(1);
		makeLevel(2);
		db.execSql("delete from ctsim_game");
		makePrelimGame(1, GameState.GAME_OVER, 0);
		makePrelimGame(2, GameState.GAME_OVER, 0);
		makePrelimGame(3, GameState.GAME_OVER, 0);
		testee.planMainRound();
	}

	/**
	 * Tests
	 * @throws SQLException
	 * @throws TournamentPlanException
	 */
	@Test(expected=IllegalStateException.class)
	public void mainRoundWithMissingLevels()
	throws SQLException, TournamentPlanException {
		db.execSql("delete from ctsim_game");
		makePrelimGame(1, GameState.GAME_OVER, 42, 42);
		makePrelimGame(2, GameState.GAME_OVER, 42, 42);
		makePrelimGame(3, GameState.GAME_OVER, 42, 42);
		db.execSql("delete from ctsim_level");
		testee.planMainRound();
	}

	/** 
	 * Prüft 1. ob die playerIds ungleich 0 sind und 2. ob alle playerIds
	 * unterschiedlich sind. (Es wäre falsch, wenn der Planner einen Bot
	 * für zwei Spiele vorsehen würde.) 
	 * @param playerIdField playerID
	 * @throws SQLException 
	 */
	private void checkPlayerId(String playerIdField) throws SQLException {
		// zur Erinnerung: count() und count(distinct) zählen NULL nicht mit
		ResultSet rs = db.execSql("select count(" + playerIdField + ") " +
			"from `ctsim_game` where level != -1");
		rs.next();
		int playerIdCount = rs.getInt(1);
		assertNotSame(0, playerIdCount);

		rs = db.execSql("select count(distinct "+playerIdField+") from " +
			"`ctsim_game` where level != -1");
		rs.next();
		int playerIdDistinctCount = rs.getInt(1);

		assertEquals(playerIdCount, playerIdDistinctCount);
	}

	/**
	 * Tests
	 * @throws SQLException
	 * @throws TournamentPlanException
	 */
	@Test
	public void mainRound() throws SQLException, TournamentPlanException {
		int gameIntervalInS = 1234;
		// paar Spiele + die nötigen Levels anlegen
		db.execSql("delete from ctsim_game");
		for (int i = 0; i < 42; i++)
			makePrelimGame(i, GameState.GAME_OVER, i, Math.abs(30 - i));
		db.execSql("delete from ctsim_level");
		makeLevel(0, gameIntervalInS); // Spiel um den 3. Platz
		for (int i = 1; i <= 32; i *= 2)
			makeLevel(i, gameIntervalInS);

		testee.planMainRound();

		{ // keine doppelten Spiele?
			ResultSet rs = db.execSql("select * from ( " +
					"select level, game, count(*) as c " +
					"from ctsim_game group by level, game ) as x " +
					"where c > 1");
			if (rs.next()) {
				fail(String.format("Mehrere Spiele haben Level %d Game %d. " +
						"(Level-Game-Kombinationen sollten immer eindeutig " +
						"sein)",
						rs.getInt("level"), rs.getInt("game")));
			}
		}

		// alle erwarteten Level da?
		for (int i = 1; i <= 32; i *= 2) {
			ResultSet rs = db.execSql("select * from ctsim_game " +
					"where level = ?", i);
			assertTrue(String.format("Planner hat Level %d nicht angelegt", i),
					rs.next());
		}

		// mind. ein Spiel ready to run?
		assertTrue("Kein Spiel ist 'ready to run'",
				db.execSql("select * from ctsim_game where " +
						"state = ?", GameState.READY_TO_RUN).next());

		// machen die playerIDs Sinn?
		checkPlayerId("bot1");
		checkPlayerId("bot2");

		// überall scheduled-Zeiten gesetzt?
		assertFalse("Ein oder mehrere Spiele haben scheduled == NULL",
			db.execSql(
				"select * from ctsim_game where level != -1 " +
				"and scheduled is NULL").next());

		// scheduled-Zeiten sind im richtigen Intervall?
		for (int i = 1; i <= 32; i *= 2)
			assertScheduledRightInterval(i, gameIntervalInS);
	}

	/**
	 * Testmethode
	 * @param levelId
	 * @param gameIntervalExpected
	 * @throws SQLException
	 */
	private void assertScheduledRightInterval(int levelId,
		long gameIntervalExpected)
	throws SQLException {
		// 2x dasselbe; previous hinkt 1 hinterher
		ResultSet rs = db.execSql("select * from ctsim_game " +
			"where level = ? order by scheduled", levelId);
		rs.next();
		int prevLevel = rs.getInt("level");
		int prevGame = rs.getInt("game");
		long prevTime = rs.getTimestamp("scheduled").getTime();
		while (rs.next()) {
			long gameIntervalActual = (rs.getTimestamp("scheduled").getTime()
				- prevTime) / 1000;
			assertEquals(
				"Level " + prevLevel +
				" Game " + prevGame +
				": falscher zeitlicher Abstand zu " +
				"Level " + rs.getInt("level") +
				" Game " + rs.getInt("game"),
				gameIntervalExpected, gameIntervalActual);
			prevLevel = rs.getInt("level");
			prevGame = rs.getInt("game");
			prevTime = rs.getTimestamp("scheduled").getTime();
		}
    }
}
