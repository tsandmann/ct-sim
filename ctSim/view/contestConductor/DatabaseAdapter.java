package ctSim.view.contestConductor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import ctSim.controller.Main;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

/** <p>Diese Klasse (und die von ihr abgeleiteten) abstrahieren die Datenbank,
 * d.h. sitzen zwischen der Datenbank f&uuml;r den Wettbewerb Oktober 2006
 * einerseits und ContestConductor / TournamentPlanner andererseits.</p>
 *
 * <p>Diese Klasse besteht, um den ContestConductor und den TournamentPlanner um
 * SQL zu entlasten. (Ein Aufruf <code>db.setWinner(42);</code> ist
 * &uuml;bersichtlicher als ein halbes dutzend Zeilen Datenbank-Getue).</p>
 *
 * <p>Alle Methoden dieser Klasse und alle Methoden der abgeleiteten Klassen
 * werfen Instanzen von java.sql.SQLException,
 * falls die darunterliegenden Methoden, die auf die Datenbank zugreifen,
 * eine solche Exception werfen. &Uuml;blicherweise deutet das darauf hin, dass
 * die Datenbank nach einem anderen Schema aufgebaut ist als der Code
 * erwartet.</p>
 *
 * @see "Von dieser Klasse abgeleitete Klassen"
 * @see ContestConductor
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
class DatabaseAdapter {
	//$$ doc StatementCache
	class StatementCache extends HashMap<String, PreparedStatement> {
        private static final long serialVersionUID =
        	- 293074803104060506L;

        private final int maxSize;

		public StatementCache(final int maxSize) {
	        super();
	        this.maxSize = maxSize;
        }

		@SuppressWarnings("synthetic-access")
        @Override
		public PreparedStatement get(Object key) {
			if (size() >= maxSize)
				clear();
			// Wenn nicht drin, hinzufuegen, damit super.get garantiert was
			// zurueckliefert
			if (! containsKey(key)) {
				/*
				 * Cast ist okay, weil wir eine HashMap<String, ...> sind
				 * und der Compiler sicherstellt, dass nur Strings in uns
				 * geputtet werden
				 */
				String s = (String)key;
				try {
	                put(s, dbConn.prepareStatement(s));
                } catch (SQLException e) {
                	// In non-checked Excp einwickeln, um mit der geerbten
                	// Signatur kompatibel zu bleiben
                	throw new RuntimeException(e);
                }
			}
		    return super.get(key);
		}
	}

	/**
	 * <p>
	 * Repr&auml;sentiert die möglichen Zust&auml;nde eines Spiels. Die Enum
	 * funktioniert Map-artig: sie umfasst Paare aus einem Symbol im Code und
	 * einem String zugeordnet. Letzterer ist die Repr&auml;sentation, wie der
	 * Spielzustand in der Datenbank dargestellt wird.
	 * </p>
	 * F&uuml;r ein Beispiel und zur Verwendung siehe Methode
	 * {@link #toString()}.
	 */
	enum GameState {
		/**
		 * Zeigt an, dass (noch) keine Spieler f&uuml;r das Spiel eingetragen
		 * sind.
		 */
		NOT_INITIALIZED("not init"),
		/**
		 * Zeigt an, dass ein Spieler, aber (noch) kein zweiter f&uuml;r das
		 * Spiel eingetragen ist.
		 */
		WAITING_FOR_BOT2("wait for bot2"),
		/**
		 * Zeigt an, dass das Spiel gestartet werden kann (es sind so viele
		 * Spieler eingetragen wie n&ouml;tig).
		 */
		READY_TO_RUN ("ready to run"),
		/**
		 * Zeigt an, dass das Spiel gestartet, aber noch nicht abgeschlossen
		 * ist. Normalerweise hat zu jedem Zeitpunkt nur h&ouml;chstens ein
		 * Spiel diesen Zustand.
		 */
		RUNNING ("running"),

		/** Zeigt an, dass das Spiel abgeschlossen ist. */
		GAME_OVER("game over");

		private final String representationInDb;

		GameState(String representationInDb) {
			this.representationInDb = representationInDb;
		}

		/** <p>Diese Methode erlaubt es, Objekte dieser Klasse (dieser Enum)
		 * auf unb&uuml;rokratische Art in Strings einzubauen, die SQL-Queries
		 * repr&auml;sentieren. Beispiel:</p>
		 * <code>"SELECT * FROM games WHERE state =
		 * '"+GameState.RUNNING+"'"</code>
		 *
		 * @return Der String, der in der Datenbank das Objekt dieser Klasse
		 * repr&auml;sentiert. Beispielsweise liefert
		 * GameState.NOT_INITIALIZED.toString() den unter
		 * <a href="#enum_constant_summary">Enum Constant Summary</a>
		 * aufgef&uuml;hrten Wert.
		 */
		@Override
		public String toString() {
			return representationInDb;
		}
	}

	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	static {
		Main.dependencies.registerImplementation(ContestDatabase.class);
	}

	/** Die Verbindung zu der Datenbank, die alles rund um den Wettbewerb
	 * weiss. */
	private Connection dbConn;

	//$$ doc statementCache
	protected StatementCache statementCache = new StatementCache(100);


	protected DatabaseAdapter(ContestDatabase db) {
		dbConn = db.getConnection();
	}

	//$$$ doc getPS
	private PreparedStatement buildPreparedStatement(String sqlWithInParams,
		Object... inValues) throws SQLException {
		PreparedStatement rv = statementCache.get(sqlWithInParams);
		for (int i = 0; i < inValues.length; i++) {
			// PreparedStatement ist 1-based, inValues ist 0-based
			if (inValues[i] instanceof GameState)
				rv.setString(i + 1, inValues[i].toString()); //$$ GameState-Hack
			else
				rv.setObject(i + 1, inValues[i]);
		}
		return rv;
	}

	//$$$ doc execSql()
	protected ResultSet execSql(String sqlWithInParams, Object... inValues)
	throws SQLException {
		if (sqlWithInParams.toLowerCase().startsWith("select")) {
			return buildPreparedStatement(sqlWithInParams, inValues).
				executeQuery();
		}
		else if (Misc.startsWith(sqlWithInParams.toLowerCase(),
			"insert", "update", "delete")) {
			buildPreparedStatement(sqlWithInParams, inValues).executeUpdate();
			return null;
		} else {
			throw new SQLException("Kann keine SQL-Befehle au\u00DFer " +
					"SELECT, INSERT, UPDATE und DELETE");
		}
	}

	/** Liefert die Zeit, wie lange ein Spiel maximal dauern darf. Nach dieser
	 * Zeit wird das Spiel vom Judge als unentschieden abgebrochen.
	 *
	 * @param levelId Prim&auml;rschl&uuml;ssel des Levels
	 * @return Geplante Zeit [ms], wie lange ein Spiel dieses Levels maximal
	 * dauern darf.
	 * @throws SQLException
	 */
	protected int getMaxGameLengthInMs(int levelId) throws SQLException {
		ResultSet rs = execSql("SELECT * from ctsim_level WHERE id = ?",
			levelId);
		rs.next();
		return rs.getInt("gametime");
	}

	/** Tr&auml;gt einen Bot f&uuml;r ein Spiel ein. Aufzurufen entweder
	 * vor dem
     * Wettbewerb in der Planungsphase oder im Wettbewerb, wenn sich durch
     * die Spielergebnisse herausstellt, welche Bots um ein Level weiterkommen.
     *
     * @param botId Bot, repr&auml;sentiert von seinem
     * Prim&auml;rschl&uuml;ssel, der eingettragen werden soll. Falls
     * <code>null</code> tut die Methode nichts.
     * @param levelId Das Level, in das der Bot platziert werden soll
     * @param gameId Die Nummer des Spiels im jeweiligen Level
     * @throws TournamentPlanException Wenn f&uuml;r das &uuml;bergebene Spiel
     * bereits zwei Spieler eingetragen sind
     */
    public void placeBot(Integer botId, int levelId, int gameId)
    throws SQLException, TournamentPlanException {
    	if (botId == null)
    		return;

    	lg.fine("Trage Bot ID = '"+botId+"' fuer Level "+levelId+
    		" Spiel "+gameId+" ein");

    	// Suche das Spiel heraus
    	ResultSet rs = execSql(
    		"SELECT * from ctsim_game WHERE level = ? AND game = ?",
    		levelId, gameId);

    	// Bots holen; Erwartung: einer oder beide sind NULL
    	rs.next();
    	Integer bot1Id = rs.getInt("bot1");
    	if (rs.wasNull())
    		bot1Id = null;
    	Integer bot2Id = rs.getInt("bot2");
    	if (rs.wasNull())
    		bot2Id = null;

    	// Bots aktualisieren
    	if (bot1Id == null) {
    		execSql("UPDATE ctsim_game " +
    				"SET bot1 = ?, state = ? " +
    				"WHERE level = ? AND game = ?",
    				botId, GameState.WAITING_FOR_BOT2, levelId, gameId);
    	} else if (bot2Id == null) {
    		execSql("UPDATE ctsim_game " +
    				"SET bot2 = ?, state = ? " +
    				"WHERE level = ? AND game = ?",
    				botId, GameState.READY_TO_RUN, levelId, gameId);
    	} else {
    		throw new TournamentPlanException(
    			"Kein Platz in Spiel "+gameId+" Level: "+levelId);
    	}
    }
}
