package ctSim.view.contestConductor;

import static org.junit.Assert.assertEquals;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import org.junit.Test;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.util.Misc;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

/**
 * <p>
 * Stellt der Klasse ContestConductor Methoden zur Verf&uuml;gung, um mit der
 * Wettbewerbs-Datenbank zu arbeiten. Mit anderen Worten: Sitzt zwischen der
 * Datenbank und dem ContestConductor.
 * </p>
 * <p>
 * N&auml;heres in der Dokumentation der Klasse {@link DatabaseAdapter}.
 * </p>
 * <p>
 * Diese Klasse wei&szlig; als einzige, was gerade gespielt wird (den
 * Schl&uuml;ssel des zur Zeit laufenden Spiels). Das ist so, weil diese
 * Information von den meisten in dieser Klasse wohnenden Methoden gebraucht
 * wird, und au&szlig;erhalb dieser Klasse nie.
 * </p>
 *
 * @see DatabaseAdapter
 * @see ContestConductor
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
class ConductorToDatabaseAdapter extends DatabaseAdapter {
    /** 8 fuer Achtelfinale, 4 fuer Viertelfinale usw. Ist zusammen mit
     * 'currentGameId' DB-Schl&uuml;ssel f&uuml;r ein Spiel.
     *
     * Integer (nicht int), um Zugriffe im uninitialisierten Zustand per
     * NullPointerException mitgeteilt zu bekommen (sonst w&uuml;rde im SQL
     * einfach id == 0 auftauchen) */
    private Integer currentLevelId = null;

    /** ID des aktuellen Spiels innerhalb des momentanen Level. Ist zusammen
     * mit 'currentLevelId' DB-Schl&uuml;ssel f&uuml;r ein Spiel.
     *
     * Integer (nicht int), um Zugriffe im uninitialisierten Zustand per
     * NullPointerException mitgeteilt zu bekommen (sonst w&uuml;rde im SQL
     * einfach id == 0 auftauchen) */
    private Integer currentGameId = null;

	/** Hat dieselbe Funktion wie {@link DatabaseAdapter#DatabaseAdapter()}. */
	public ConductorToDatabaseAdapter()
	throws SQLException, ClassNotFoundException {
	    super();
    }

	/** Hat dieselbe Funktion wie {@link
	 * DatabaseAdapter#DatabaseAdapter(Connection)}. */
	protected ConductorToDatabaseAdapter(Connection db) {
		super(db);
    }

    //$$ doc
    private List<Object> getLoggableBotCoord(Bot b, int n) {
        Point3d pos = b.getPosition();
        Vector3d head = b.getHeading();
        return Arrays.asList(new Object[] {
	    	"pos" + n + "x",  pos.x,
	    	"pos" + n + "y",  pos.y,
	    	"head"+ n + "x",  head.x,
	    	"head"+ n + "y",  head.y,
	    	"state" + n,      b.getObstState(),
        });
    }

    //$$ doc
    //$$ Verwendung in PlannerToDb
    private static String buildInsert(
    	String tableName, Object... namesAndValues) {
        assert namesAndValues.length % 2 == 0;
        String[] colNames = new String[namesAndValues.length / 2];
        String[] values   = new String[namesAndValues.length / 2];

        for (int i = 0; i < namesAndValues.length / 2; i++) {
            colNames[i] = ""+namesAndValues[i * 2];
            values  [i] = ""+namesAndValues[i * 2 + 1];
        }

        return
            "INSERT INTO " + tableName + " ( " +
            Misc.join(Misc.intersperse(", ", colNames)) +
            " ) VALUES ( '" +
            Misc.join(Misc.intersperse("', '", values)) +
            "' );";
    }

    //$$ irgendwie zu buerokratisch
    /**
     * Macht einen Log-Eintrag in die Datenbank. Das Log wird von der
     * JavaScript-Applikation der Messieurs von der Online-Redaktion
     * benutzt, um den aktuellen Spielstand im Web anzuzeigen. Ausgabeformat
     * nur nach R&uuml;cksprache &auml;ndern.
     *
     * @param bots Ein Set, das ein oder zwei Bots enth�lt. Position,
     * Blickrichtung und Status der Bots wird in die Datenbank geloggt.
     * @param simTimeElapsed Simulatorzeit [ms] seit Beginn des Spiels
     * @throws IllegalArgumentException Falls der Parameter <code>bots</code>
     * weniger als ein oder mehr als zwei Elemente enth&auml;lt.
     *
     * @see World#getSimTimeInMs()
     */
    public void log(Set<Bot> bots, long simTimeElapsed)
    throws IllegalArgumentException, SQLException {
    	if (bots.size() != 1 && bots.size() != 2) {
    		throw new IllegalArgumentException("Falsche Anzahl von Bots im " +
    				"Set: erwarteter Wert 1 oder 2, tats\u00E4chlicher Wert "+
    				bots.size());
    	}
        ArrayList<Object> insertParams = new ArrayList<Object>(
        	Arrays.asList(new Object[] {
			"logtime", simTimeElapsed,
			"game", currentGameId}));

        Iterator<Bot> it = bots.iterator();
        insertParams.addAll(getLoggableBotCoord(it.next(), 1));
        if (it.hasNext())
        	// Wir loggen ein Spiel mit 2 Bots
        	insertParams.addAll(getLoggableBotCoord(it.next(), 2));

   		execSql(buildInsert("ctsim_log", insertParams.toArray()));
    }

    /** Liefert das aus der Datenbank kommende XML, das einen Parcours
     * beschreibt.
     *
     * @param levelId	Der Schl&uuml;ssel des Levels, dessen Parcours
     * angefordert wird.
     * @return	Das XML-Dokument, das den Parcours beschreibt (selbes Schema
     * wie das on-disk-Parcours-Schema).
     */
    public String getParcours(int levelId) throws SQLException {
        ResultSet rs = execSql(
        	"SELECT * FROM ctsim_level WHERE id = " + levelId);
        rs.next();
        return rs.getString("parcours");
    }

    /**
     * Zeichnet in der Datenbank auf, wer Gewinner des aktuellen Spiels war,
     * und beendet das aktuelle Spiel. Setzt au&szlig;erdem den Gewinner
     * ein Level weiter.
     *
     * @param winnerBotId	Prim&auml;rschl&uuml;ssel des Bots, der das Spiel
     * gewonnen hat.
     * @param finishSimTime	Nach wieviel Simulatorzeit [ms] seit Beginn des
     * Spiels wurde die Ziellinie &uuml;berschritten?
     * @throws TournamentPlanException Falls das aktuelle Spiel ein
     * Hauptrundenspiel ist und der Gewinner in ein Level weitergesetzt wird,
     * f&uuml;r das schon zwei Spieler eingetragen sind. Mit anderen Worten,
     * falls der Turnierbaum vermurkst ist.
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    public void setWinner(int winnerBotId, long finishSimTime)
    throws SQLException, TournamentPlanException, NullPointerException {
    	lg.finer("Schreibe Gewinner (Bot-ID %d, Simzeit %dms) in die DB",
    		winnerBotId, finishSimTime);
        execSql(
            "UPDATE ctsim_game " +
            "SET winner='"+winnerBotId+"', " +
            "finishtime="+finishSimTime+", "+
            "state= '"+GameState.GAME_OVER+"' "+
            "WHERE level ="+currentLevelId+" AND game ="+currentGameId);

        propagateWinner(winnerBotId);

        currentLevelId = null;
        currentGameId = null;
    }

    /** <p>Liefert die ID des Bots, der der Verlierer des aktuellen Spiels ist,
     * wenn man die &uuml;bergebene Zahl als die ID des Gewinners annimmt.</p>
     *
     * <p>Mit anderen Worten, liefert aus dem aktuellen Spiel den
     * Spielpartner des &uuml;bergebenen Spielers.</p>
     *
     * <p>Funktioniert nur in Hauptrundenspielen.</p>
     *
     * @param winnerBotId Bot-ID des Gewinners des aktuellen Spiels
     * @return Bot-ID des Verlierers des aktuellen Spiels
     * @throws IllegalStateException Wenn in den beiden botIds des aktuellen
     * Spiels der Wert NULL in der Datenbank auftaucht. (Wirft daher auch
     * diese Exception, wenn die Methode aufgerufen wird, w&auml;hrend ein
     * Vorrundenspiel l&auml;uft.)
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    private int getCurrentGameLoserId(int winnerBotId)
    throws SQLException, IllegalStateException, NullPointerException {
        ResultSet currentGame = execSql(
                "SELECT * FROM ctsim_game WHERE level = %d AND game = %d",
				currentLevelId, currentGameId);
        currentGame.next();
        int b1 = currentGame.getInt("player1botId");
        if (currentGame.wasNull())
            throw new IllegalStateException();
        int b2 = currentGame.getInt("player2botId");
        if (currentGame.wasNull())
            throw new IllegalStateException();

        if (b1 == winnerBotId)
            return b2;
        else
            return b1;
    }

    /**
     * Setzt den Gewinner des Spiels ein Level h&ouml;her. Tut nichts, falls
     * das aktuelle Spiel ein Vorrundenspiel ist.
     *
     * @throws TournamentPlanException Falls in dem Spiel, in das der Gewinner
     * des aktuellen Spiels gesetzt werden soll, bereits zwei Spieler
     * eingetragen sind (= falls der Turnierbaum verkorkst ist).
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    private void propagateWinner(int winnerBotId)
    throws SQLException, TournamentPlanException, NullPointerException {
        if (! isCurrentGameMainRound())
            return; // kein Weitersetzmechanismus fuer die Vorrunde
        lg.finer("Setze Gewinner ein Spiel weiter");
        switch(currentLevelId) {
            case 1: // Finale wurde gespielt
                // no-op; kein Weiterruecken mehr
                break;
            case 2: // Halbfinale wurde gespielt
                // Aus dem Halbfinale kommen die Gewinner ins Finale und die
                // Verlierer ins Spiel um den 3. Platz (sog. Semifinale)
                placeBot(winnerBotId, 1, 1);
                // 0 == Sonderwert fuer Spiel um den 3.
                placeBot(getCurrentGameLoserId(winnerBotId), 0, 1);
                break;
            default: // jedes andere Level
                placeBot(winnerBotId, currentLevelId/2, (currentGameId+1)/2);
        }
    }

    /** Liefert die Binary eines der beiden Bots im aktuellen Spiel.
     *
     * @param whichPlayer Name des DB-Felds, das angibt, welcher Bot. Kann im
     * gegenw&auml;rtigen DB-Schema "player1botId" oder "player2botId" sein.
     * @return Die mit dem gew&auml;hlten Bot im aktuellen Spiel assoziierte
     * Binary (ELF).
     *
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    private Blob getBotBinary(String whichPlayer)
    throws SQLException, NullPointerException {
        ResultSet binary = execSql(
                "SELECT bin " +
                "FROM ctsim_bot AS bot, ctsim_game AS game " +
                "WHERE game.%s = bot.id " +
                "AND game.level = '%d' "+
                "AND game.game  = '%d';",
                whichPlayer, currentLevelId, currentGameId);
        binary.next();
        return binary.getBlob("bin");
    }

    /** Liefert die Binary von Spieler 1 des aktuellen Spiels.
     *
     * @return Die mit Teilnehmer 1 des aktuellen Spiels assoziierte
     * Binary (ELF).
     *
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    public Blob getBot1Binary() throws SQLException {
        return getBotBinary("player1botId");
    }

    /** Liefert die Binary von Spieler 2 des aktuellen Spiels.
     *
     * @return Die mit Teilnehmer 2 des aktuellen Spiels assoziierte
     * Binary (ELF).
     *
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    public Blob getBot2Binary() throws SQLException {
        return getBotBinary("player2botId");
    }

    /** Liefert den Schl&uuml;ssel eines der Spieler im zur Zeit laufenden
     * Spiel.
     *
     * @param whichPlayer Gibt das DB-Feld an, das den gew&uuml;nschten
     * Schl&uuml;ssel enth&auml;lt. Kann "player1BotId" oder "player2BotId"
     * sein.
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    private int getBotId(String whichPlayer)
    throws SQLException, NullPointerException {
        ResultSet rs = execSql(
            "SELECT * FROM ctsim_game WHERE level = '%d' AND game = '%d';",
            currentLevelId, currentGameId);
        rs.next();
        return rs.getInt(whichPlayer);
    }

    /** Liefert den DB-Schl&uuml;ssel von Spieler 1 des zur Zeit laufenden
     * Spiels.
     */
    public int getBot1Id() throws SQLException {
        return getBotId("player1BotId");
    }

    /** Liefert den DB-Schl&uuml;ssel von Spieler 2 des zur Zeit laufenden
     * Spiels.
     */
    public int getBot2Id() throws SQLException {
        return getBotId("player2BotId");
    }

    //$$ doc Method
    public int getMaxGameLengthInMs() throws SQLException {
        return getMaxGameLengthInMs(currentLevelId);
    }

    /**
     * Liefert die Spiele, die bereit zur Durchf&uuml;hrung sind.
     *
     * @return Ein ResultSet, das auf das Spiel zeigt. Das Format ist das der
     * Zeilen in der Tabelle ctsim_game. Wenn der erste next()-
     * Aufruf auf dem ResultSet den Wert <code>false</code> liefert, gibt es
     * keine Spiele
     * mehr, die bereit zur Durchf&uuml;hrung w&auml;ren.
     */
    public ResultSet getReadyGames() throws SQLException {
        // Nur Spiele waehlen, die READY_TO_RUN haben
        return execSql(
            "SELECT * " +
            "FROM ctsim_game " +
            "WHERE state ='" + GameState.READY_TO_RUN + "'" +
            "ORDER BY scheduled");
    }

    /** Startet ein Spiel, d.h. setzt seinen Status in der Datebank
     * entsprechend.
     *
     * @param levelId Level des zu startenden Spiels.
     * @param gameId Nummer des zu startenden Spiels innerhalb seines Levels.
     */
    public void setGameRunning(int levelId, int gameId) throws SQLException {
        execSql("UPDATE ctsim_game " +
                "SET state= '"+GameState.RUNNING+"' "+
                "WHERE level ="+levelId+" AND game ="+gameId);
        currentLevelId = levelId;
        currentGameId = gameId;
    }

    /** Zeigt an, ob das zur Zeit laufende Spiel ein Hauptrundenspiel ist.
     *
     * @return <code>true</code>, falls das aktuelle Spiel zur Hauptrunde
     * geh&ouml;rt (ein anderes Level als -1 hat); <code>false</code>, falls
     * das aktuelle Spiel zur Vorrunde geh&ouml;rt (ein Level von -1 hat).
     *
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     */
    private boolean isCurrentGameMainRound() throws NullPointerException {
        return currentLevelId != -1;
    }

    public static class UnitTest {
        @SuppressWarnings("synthetic-access")
        @Test
        public void buildInsertTest() {
        	// Achtung JUnit-Bug: Die Anzeige der Strings (bei Fehlschlag)
        	// enthaelt unerklaerliche eckige Klammern; der Vergleich der
        	// Strings klappt aber
        	assertEquals("INSERT INTO wurst ( A, B ) VALUES ( 'a', 'b' );",
        		buildInsert("wurst", "A", "a", "B", "b"));

        	assertEquals("INSERT INTO wurst ( 42, true ) " +
        		"VALUES ( '24', 'false' );",
        		buildInsert("wurst", 42, 24, true, false));

        	assertEquals("INSERT INTO wurst ( A, B, 42, true ) " +
        		"VALUES ( 'a', 'b', '24', 'false' );",
        		buildInsert("wurst", "A", "a", "B", "b", 42, 24, true, false));
        }
    }
}