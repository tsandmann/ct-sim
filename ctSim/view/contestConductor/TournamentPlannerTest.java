package ctSim.view.contestConductor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import ctSim.view.contestConductor.DatabaseAdapter.GameState;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

public class TournamentPlannerTest extends ConductorTestUtil {
	private PlannerToDatabaseAdapter db;
	private TournamentPlanner testee;

	@Override
    protected DatabaseAdapter getDbFromChildClass() {
	    return db;
    }

	protected TournamentPlannerTest()
	throws ClassNotFoundException, SQLException {
		db = new PlannerToDatabaseAdapter(getTestDb());
		testee = new TournamentPlanner(db);
    }


	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithMissingLevel() throws SQLException {
		db.execSql("delete from ctsim_bot");
		makeBot(1, "Friedrich-Wilhelm");
		makeBot(2, "Friedrich-Wilhelm II");
		makeBot(3, "Friedrich-Wilhelm III");
		db.execSql("delete from ctsim_level");
		testee.planPrelimRound();
	}

	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithMissingBots() throws SQLException {
		db.execSql("delete from ctsim_bot");
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		testee.planPrelimRound();
	}

	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithTooFewBots() throws SQLException {
		db.execSql("delete from ctsim_bot");
		makeBot(1, "Wittgenstein");
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
		testee.planPrelimRound();
	}

	@Test
	public void planPrelimRound() throws SQLException {
		ResultSet rs;
		// 3 Bots => 2 Spiele
		db.execSql("delete from ctsim_game");
		db.execSql("delete from ctsim_level");
		makeLevel(-1);
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
	}

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

	/** Pr&uuml;ft 1. ob die playerIds ungleich 0 sind und 2. ob alle playerIds
	 * unterschiedlich sind. (Es w&auml;re falsch, wenn der Planner einen Bot
	 * f&uuml;r zwei Spiele vorsehen w&uuml;rde.) */
	private void checkPlayerId(int whichPlayerId) throws SQLException {
		// zur Erinnerung: count() und count(distinct) zaehlen NULL nicht mit
		ResultSet rs = db.execSql("select count(player%sbotId) from " +
		"`ctsim_game` where level != -1", whichPlayerId);
		rs.next();
		int playerIdCount = rs.getInt(1);
		assertNotSame(0, playerIdCount);

		rs = db.execSql("select count(distinct player%sbotId) from " +
			"`ctsim_game` where level != -1", whichPlayerId);
		rs.next();
		int playerIdDistinctCount = rs.getInt(1);

		assertEquals(playerIdCount, playerIdDistinctCount);
	}

	@Test
	public void mainRound() throws SQLException, TournamentPlanException {
		// paar Spiele + die noetigen Levels anlegen
		db.execSql("delete from ctsim_game");
		for (int i = 0; i < 42; i++)
			makePrelimGame(i, GameState.GAME_OVER, i, Math.abs(30 - i));
		makeLevel(0); // Spiel um den 3. Platz
		for (int i = 1; i <= 32; i *= 2)
			makeLevel(i);

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
			ResultSet rs = db.execSql("select * from ctsim_game where level = %d", i);
			assertTrue(String.format("Planner hat Level %d nicht angelegt", i),
					rs.next());
		}

		// mind. ein Spiel ready to run?
		assertTrue("Kein Spiel ist 'ready to run'",
				db.execSql("select * from ctsim_game where " +
						"state = '%s'", GameState.READY_TO_RUN).next());

		// machen die playerIDs Sinn?
		checkPlayerId(1);
		checkPlayerId(2);

		// ueberall scheduled-Zeiten gesetzt?
		assertFalse("Ein oder mehrere Spiele haben scheduled == NULL", db.execSql(
				"select * from ctsim_game where level != -1 " +
				"and scheduled is NULL").next());
	}
}
