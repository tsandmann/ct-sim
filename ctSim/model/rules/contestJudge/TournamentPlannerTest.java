package ctSim.model.rules.contestJudge;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;

import ctSim.model.rules.contestJudge.DatabaseAdapter.GameState;
import ctSim.model.rules.contestJudge.TournamentPlanner.TournamentPlanException;

public class TournamentPlannerTest {
	private TournamentPlanner testee;
	private Connection db;
	
	@Before
	public void setUp() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		db = DriverManager.getConnection(
				"jdbc:mysql://localhost:3306/ctjudge-test", "root", "");
		testee = new TournamentPlanner(new PlannerToDatabaseAdapter(db));
	}
	
	private ResultSet sql(String sql) throws SQLException {
		Statement s = db.createStatement();
		if (sql.toLowerCase().startsWith("select"))
			return s.executeQuery(sql);
		else {
			s.executeUpdate(sql);
			return null;
		}
	}
	
	private ResultSet sqlf(String sqlFormat, Object... values)
	throws SQLException {
		return sql(String.format(sqlFormat, values));
	}
	
	private void makeBot(int id, String name) throws SQLException
	{
		sqlf("insert into ctsim_bot (id, name, team, bin) " +
				"values (%d, '%s', %d, '%s')", 
				id, name, 42, "wurst");
	}

	private void makeLevel(int levelId) throws SQLException {
		sqlf("insert into ctsim_level (id) values ("+levelId+")");
	}
	
	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithMissingLevel() throws SQLException {
		sql("delete from ctsim_bot");
		makeBot(1, "Friedrich-Wilhelm");
		makeBot(2, "Friedrich-Wilhelm II");
		makeBot(3, "Friedrich-Wilhelm III");
		sql("delete from ctsim_level");
		testee.planPrelimRound();
	}
	
	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithMissingBots() throws SQLException {
		sql("delete from ctsim_bot");
		sql("delete from ctsim_level");
		makeLevel(-1);
		testee.planPrelimRound();
	}
	
	@Test(expected=IllegalStateException.class)
	public void prelimRoundWithTooFewBots() throws SQLException {
		sql("delete from ctsim_bot");
		makeBot(1, "Wittgenstein");
		sql("delete from ctsim_level");
		makeLevel(-1);
		testee.planPrelimRound();
	}
	
	@Test
	public void planPrelimRound() throws SQLException {
		ResultSet rs;
		// 3 Bots => 2 Spiele
		sql("delete from ctsim_game");
		sql("delete from ctsim_level");
		makeLevel(-1);
		sql("delete from ctsim_bot");
		makeBot(1, "Friedrich-Wilhelm I");
		makeBot(2, "Friedrich-Wilhelm II");
		makeBot(3, "Friedrich-Wilhelm III");
		testee.planPrelimRound();
		rs = sql("select * from ctsim_game");
		assertTrue(rs.next());
		assertEquals(-1, rs.getInt("level"));
		assertEquals(1, rs.getInt("game"));
		rs.last();
		assertEquals(3, rs.getRow());
		
		// keine doppelten Spiele?
		rs = sql("select * from ( " +
			"select level, game, count(*) as c " +
			"from ctsim_game group by level, game ) as x " +
			"where c > 1");
		if (rs.next()) {
			fail(String.format("Mehrere Spiele haben Level %d Game %d. " +
					"(Level-Game-Kombinationen sollten immer eindeutig sein)",
					rs.getInt("level"), rs.getInt("game")));
		}
	}
	
	// mit winner == NULL
	private void makePrelimGame(int i, GameState state, int finishtime) 
	throws SQLException {
		sqlf("insert into ctsim_game " +
			"(level, game, player1botId, state, finishtime)" +
			"values (%d, %d, %d, '%s', %d)",
			-1, i, i, state, finishtime);
	}
	
	// mit winner != NULL
	private void makePrelimGame(int i, GameState state, int winner, 
			int finishtime) throws SQLException {
		sqlf("insert into ctsim_game " +
				"(level, game, player1botId, state, winner, finishtime)" +
				"values (%d, %d, %d, '%s', %d, %d)",
				-1, i, i, state, winner, finishtime);
	}
	
	@Test(expected=IllegalStateException.class)
	public void mainRoundWithNoPrelim() 
	throws SQLException, TournamentPlanException {
		sql("delete from ctsim_level");
		makeLevel(-1);
		makeLevel(1);
		makeLevel(2);
		sql("delete from ctsim_game");
		testee.planMainRound();
	}

	@Test(expected=TournamentPlanException.class)
	public void mainRoundWithIncompletePrelim() 
	throws SQLException, TournamentPlanException {
		sql("delete from ctsim_level");
		makeLevel(-1);
		makeLevel(1);
		makeLevel(2);
		sql("delete from ctsim_game");
		makePrelimGame(1, GameState.GAME_OVER, 0);
		makePrelimGame(2, GameState.RUNNING, 0);
		makePrelimGame(3, GameState.GAME_OVER, 0);
		testee.planMainRound();
	}
	
	@Test(expected=TournamentPlanException.class)
	public void mainRoundWithMissingWinners() 
	throws SQLException, TournamentPlanException {
		sql("delete from ctsim_level");
		makeLevel(-1);
		makeLevel(1);
		makeLevel(2);
		sql("delete from ctsim_game");
		makePrelimGame(1, GameState.GAME_OVER, 0);
		makePrelimGame(2, GameState.GAME_OVER, 0);
		makePrelimGame(3, GameState.GAME_OVER, 0);
		testee.planMainRound();
	}
	
	@Test(expected=IllegalStateException.class)
	public void mainRoundWithMissingLevels() 
	throws SQLException, TournamentPlanException {
		sql("delete from ctsim_game");
		makePrelimGame(1, GameState.GAME_OVER, 42, 42);
		makePrelimGame(2, GameState.GAME_OVER, 42, 42);
		makePrelimGame(3, GameState.GAME_OVER, 42, 42);
		sql("delete from ctsim_level");
		testee.planMainRound();
	}
	
	/** Pr&uuml;ft 1. ob die playerIds ungleich 0 sind und 2. ob alle playerIds
	 * unterschiedlich sind. (Es w&auml;re falsch, wenn der Planner einen Bot
	 * f&uuml;r zwei Spiele vorsehen w&uuml;rde.) */
	private void checkPlayerId(int whichPlayerId) throws SQLException {
		// zur Erinnerung: count() und count(distinct) zaehlen NULL nicht mit
		ResultSet rs = sqlf("select count(player%sbotId) from " +
		"`ctsim_game` where level != -1", whichPlayerId);
		rs.next();
		int playerIdCount = rs.getInt(1);
		assertNotSame(0, playerIdCount);
	
		rs = sqlf("select count(distinct player%sbotId) from " +
			"`ctsim_game` where level != -1", whichPlayerId);
		rs.next();
		int playerIdDistinctCount = rs.getInt(1);
	
		assertEquals(playerIdCount, playerIdDistinctCount);
	}
	
	@Test
	public void mainRound() throws SQLException, TournamentPlanException {
		// paar Spiele + die noetigen Levels anlegen
		sql("delete from ctsim_game");
		for (int i = 0; i < 42; i++)
			makePrelimGame(i, GameState.GAME_OVER, i, Math.abs(30 - i));
		makeLevel(0); // Spiel um den 3. Platz
		for (int i = 1; i <= 32; i *= 2)
			makeLevel(i);
		
		testee.planMainRound();
		
		{ // keine doppelten Spiele?
			ResultSet rs = sql("select * from ( " +
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
			ResultSet rs = sqlf("select * from ctsim_game where level = %d", i);
			assertTrue(String.format("Planner hat Level %d nicht angelegt", i), 
					rs.next());
		}
		
		// mind. ein Spiel ready to run?
		assertTrue("Kein Spiel ist 'ready to run'",
				sqlf("select * from ctsim_game where " +
						"state = '%s'", GameState.READY_TO_RUN).next());
		
		// machen die playerIDs Sinn?
		checkPlayerId(1);
		checkPlayerId(2);
		
		// ueberall scheduled-Zeiten gesetzt?
		assertFalse("Ein oder mehrere Spiele haben scheduled == NULL", sql(
				"select * from ctsim_game where level != -1 " +
				"and scheduled is NULL").next());
	}
}
