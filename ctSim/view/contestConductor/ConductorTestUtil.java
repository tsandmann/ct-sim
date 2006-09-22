package ctSim.view.contestConductor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ctSim.view.contestConductor.DatabaseAdapter.GameState;

public abstract class ConductorTestUtil {
	protected abstract DatabaseAdapter getDbFromChildClass();

	protected static Connection getTestDb()
	throws ClassNotFoundException, SQLException {
    	Class.forName("com.mysql.jdbc.Driver");
    	return DriverManager.getConnection(
    		"jdbc:mysql://localhost:3306/ctjudge-test", "root", "");
    }

	protected void makeBot(int id, String name) throws SQLException {
		getDbFromChildClass().execSql(
			"insert into ctsim_bot (id, name, team, bin) " +
    		"values (%d, '%s', %d, '%s')",
    		id, name, 42, "wurst");
    }

	protected void makeLevel(int levelId) throws SQLException {
		getDbFromChildClass().execSql(
			"insert into ctsim_level (id) values ("+levelId+")");
    }

	protected void makePrelimGame(int i, GameState state, int finishtime)
	throws SQLException {
		getDbFromChildClass().execSql("insert into ctsim_game " +
    		"(level, game, player1botId, state, finishtime)" +
    		"values (%d, %d, %d, '%s', %d)",
    		-1, i, i, state, finishtime);
    }

	protected void makePrelimGame(int i, GameState state, int winner,
		int finishtime) throws SQLException {
		getDbFromChildClass().execSql("insert into ctsim_game " +
    			"(level, game, player1botId, state, winner, finishtime)" +
    			"values (%d, %d, %d, '%s', %d, %d)",
    			-1, i, i, state, winner, finishtime);
    }
}
