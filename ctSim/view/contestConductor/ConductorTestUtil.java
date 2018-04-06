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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ctSim.view.contestConductor.DatabaseAdapter.GameState;

/** Hilfsklasse für die Unit-Tests in diesem Package */
public abstract class ConductorTestUtil {
    /**
     * @return DB
     */
    protected abstract DatabaseAdapter getDbFromChildClass();

    /**
     * TestDatabase
     */
    public static class TestDatabase extends ContestDatabase {
        /**
         * @see ctSim.view.contestConductor.ContestDatabase#getConnection()
         */
        
        @Override
		public Connection getConnection() {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                return DriverManager.getConnection(
                    "jdbc:mysql://10.10.22.179:3306/ctbot-contest-unittests",
                    "root", "geheimdienst");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Neuer Bot
     * @param id ID
     * @param name Name
     * @throws SQLException
     */
    protected void makeBot(int id, String name) throws SQLException {
        getDbFromChildClass().execSql(
            "insert into ctsim_bot (id, name, team, bin) " +
            "values (?, ?, ?, ?)",
            id, name, 42, "wurst");
    }

    /**
     * Neues Level
     * @param levelId ID
     * @throws SQLException
     */
    protected void makeLevel(int levelId) throws SQLException {
    	makeLevel(levelId, 42);
    }

    /**
     * Neues Level
     * @param levelId ID
     * @param gametime_real Zeit
     * @throws SQLException
     */
    protected void makeLevel(int levelId, int gametime_real)
    throws SQLException {
    	getDbFromChildClass().execSql(
    		"insert into ctsim_level (id, gametime_real) values ("+
    		levelId+", "+gametime_real+")");
    }

    /**
     * Test-Methode
     * @param i
     * @param state
     * @param finishtime
     * @throws SQLException
     */
    protected void makePrelimGame(int i, GameState state, int finishtime)
    throws SQLException {
        getDbFromChildClass().execSql("insert into ctsim_game " +
            "(level, game, bot1, state, finishtime) " +
            "values (?, ?, ?, ?, ?)",
            -1, i, i, state, finishtime);
    }

    /**
     * Test-Methode
     * @param i
     * @param state
     * @param winner
     * @param finishtime
     * @throws SQLException
     */
    protected void makePrelimGame(int i, GameState state, int winner,
        int finishtime) throws SQLException {
        getDbFromChildClass().execSql("insert into ctsim_game " +
                "(level, game, bot1, state, winner, finishtime) " +
                "values (?, ?, ?, ?, ?, ?)",
                -1, i, i, state, winner, finishtime);
    }
}
