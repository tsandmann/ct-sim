package ctSim.view.contestConductor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ctSim.ConfigManager;
import ctSim.ErrorHandler;
import ctSim.util.FmtLogger;
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
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
class DatabaseAdapter {
	/** <p>Repr&auml;sentiert die möglichen Zust&auml;nde eines Spiels.
	 * Die Enum
	 * funktioniert Map-artig: sie umfasst Paare aus einem Symbol im Code und
	 * einem String zugeordnet. Letzterer ist die Repr&auml;sentation, wie der
	 * Spielzustand in der Datenbank dargestellt wird.</p>
	 *
	 * F&uuml;r ein Beispiel und zur Verwendung siehe Methode
	 * {@link #toString()}. */
	enum GameState {
		/** Zeigt an, dass (noch) keine Spieler f&uuml;r das Spiel eingetragen
		 * sind. */
		NOT_INITIALIZED("not init"),
		/** Zeigt an, dass ein Spieler, aber (noch) kein zweiter f&uuml;r das
		 * Spiel eingetragen ist. */
		WAITING_FOR_BOT2("wait for bot2"),
		/** Zeigt an, dass das Spiel gestartet werden kann (es sind so viele
		 * Spieler eingetragen wie n&ouml;tig). */
		READY_TO_RUN ("ready to run"),
		/** Zeigt an, dass das Spiel gestartet, aber noch nicht abgeschlossen
		 * ist. Normalerweise hat zu jedem Zeitpunkt nur h&ouml;chstens ein
		 * Spiel diesen Zustand.*/
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

	/** Die Verbindung zu der Datenbank, die alles rund um den Wettbewerb
	 * weiss. */
	protected Connection dbConn = null;

	/** Teil der Initialisierung. Baut eine Verbindung auf zu der
	 * Datenbank, die in der ct-sim-Konfigurationsdatei angegeben ist. */
	private static Connection getDbConn()
	throws SQLException, ClassNotFoundException
	{
		// sieht sinnlos aus, aber erzwingt, dass die angegebene Treiberklasse
		// gesucht, geladen und initialisiert wird
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection(
			ConfigManager.getValue("contest-database-url"),
			ConfigManager.getValue("contest-database-user"),
			ConfigManager.getValue("contest-database-password"));
	}

	/** Konstruiert eine Instanz, die mit der in der ct-sim-Konfigurationsdatei
	 * angegebenen Datenbank verbunden ist.
	 *
	 * @throws SQLException Falls DriverManager.getConnection() eine
	 * SQLException wirft.
	 * @throws ClassNotFoundException Falls der Datenbanktreiber
	 * com.mysql.jdbc.Driver nicht geladen werden kann.
	 *
	 * @see java.sql.DriverManager#getConnection(String, String, String)
	 */
	public DatabaseAdapter()
	throws SQLException, ClassNotFoundException {
		this(getDbConn());
	}

	/**
	 * <p>Konstruiert eine Instanz, die mit der &uuml;bergebenen Datenbank
	 * verbunden ist. N&uuml;tzlich f&uuml;r Unit-Tests, die der Klasse
	 * durch diesen Konstruktor eine Test-Datenbank unterschieben k&ouml;nnen.
	 *
	 * @param dbConn Verbindung zu der Datenbank, die f&uuml;r
	 * benutzt werden soll.
	 */
	public DatabaseAdapter(Connection dbConn) {
		this.dbConn = dbConn;
	}

	/** Liefert die Zeit, wie lange ein Spiel maximal dauern darf. Nach dieser
	 * Zeit wird das Spiel vom Judge als unentschieden abgebrochen.
	 *
	 * @param levelId Prim&auml;rschl&uuml;ssel des Levels
	 * @return Geplante Zeit [ms], wie lange ein Spiel dieses Levels maximal
	 * dauern darf.
	 * @throws SQLException
	 */
	public int getMaxGameLengthInMs(int levelId) throws SQLException {
		ResultSet rs = dbConn.createStatement().executeQuery(
				"SELECT * from ctsim_level WHERE id ="+levelId);
		rs.next();
		return rs.getInt("game_length");
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
    	Statement s = dbConn.createStatement();
    	ResultSet rs = s.executeQuery(
    		"SELECT * from ctsim_game WHERE level="+levelId+
    		" AND game="+gameId);

    	// Bots holen; Erwartung: einer oder beide sind NULL
    	rs.next();
    	Integer bot1Id = rs.getInt("player1botId");
    	if (rs.wasNull())
    		bot1Id = null;
    	Integer bot2Id = rs.getInt("player2botId");
    	if (rs.wasNull())
    		bot2Id = null;

    	// Bots aktualisieren
    	if (bot1Id == null)
    		s.executeUpdate("UPDATE ctsim_game " +
    				"SET player1botId="+botId+","+
    				"state='"+GameState.WAITING_FOR_BOT2 +"' "+
    				"WHERE level="+levelId+" AND game="+gameId);
    	else if (bot2Id == null)
    		s.executeUpdate("UPDATE ctsim_game " +
    				"SET player2botId="+botId+","+
    				"state='"+GameState.READY_TO_RUN +"' "+
    				"WHERE level="+levelId+" AND game="+gameId);
    	else {
    		TournamentPlanException e = new TournamentPlanException(
    			"Kein Platz in Level: "+levelId+" Game: "+gameId);
    		ErrorHandler.error("Fehler"+e);
    		throw e;
    	}
    }
}
