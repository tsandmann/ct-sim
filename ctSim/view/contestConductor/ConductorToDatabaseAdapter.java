package ctSim.view.contestConductor;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.World;
import ctSim.model.bots.Bot;
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
public class ConductorToDatabaseAdapter extends DatabaseAdapter {
	//$$ doc Game, 2 Methoden high-performance
	// Annahme: Keiner modifiziert ctsim_game waehrend wir laufen
	//$$ lock her
	/** 8 fuer Achtelfinale, 4 fuer Viertelfinale usw. Ist zusammen mit
     * 'currentGameId' DB-Schl&uuml;ssel f&uuml;r ein Spiel.
     *
     * Integer (nicht int), um Zugriffe im uninitialisierten Zustand per
     * NullPointerException mitgeteilt zu bekommen (sonst w&uuml;rde im SQL
     * einfach id == 0 auftauchen) */
    /** ID des aktuellen Spiels innerhalb des momentanen Level. Ist zusammen
     * mit 'currentLevelId' DB-Schl&uuml;ssel f&uuml;r ein Spiel.
     *
     * Integer (nicht int), um Zugriffe im uninitialisierten Zustand per
     * NullPointerException mitgeteilt zu bekommen (sonst w&uuml;rde im SQL
     * einfach id == 0 auftauchen) */
	public class Game {
		// null-bedeutung
		private Integer gameId;
		// null-bedeutung
		private Integer levelId;
		// null-bedeutung
		private Integer cachedUniqueId;
		// null-bedeutung
		private Integer cachedMaxLengthInMs;

		public Integer getLevelId() {
        	return levelId;
        }

		public Integer getGameId() {
			return gameId;
		}

		public void set(Integer levelId, Integer gameId) {
			if ((levelId == null && gameId != null)
				|| (levelId != null && gameId == null)) {
				throw new IllegalArgumentException();
			}

			// Caches loeschen
			cachedUniqueId = null;
			cachedMaxLengthInMs = null;
			this.levelId = levelId;
			this.gameId = gameId;
        }

		public Integer getUniqueId()
		throws SQLException, IllegalStateException {
			if (gameId == null)
				throw new IllegalStateException();

			if (cachedUniqueId == null) {
				ResultSet rs = execSql("SELECT * FROM ctsim_game " +
		    		"WHERE game = ? AND level = ?", gameId, levelId);
		    	rs.next();
		    	assert ! rs.wasNull();
		    	cachedUniqueId = rs.getInt("id");
			}
			return cachedUniqueId;
        }

		public int getMaxLengthInMs()
		throws SQLException, IllegalStateException {
			if (gameId == null)
				throw new IllegalStateException();

			if (cachedMaxLengthInMs == null) {
				cachedMaxLengthInMs = getMaxGameLengthInMs(getLevelId());
			}
			return cachedMaxLengthInMs;
		}
	}

	private final Game currentGame = new Game();

	/** Hat dieselbe Funktion wie {@link
	 * DatabaseAdapter#DatabaseAdapter(ContestDatabase)}. */
    public ConductorToDatabaseAdapter(ContestDatabase db) {
	    super(db);
    }

    //$$ irgendwie zu buerokratisch
    /**
     * Macht einen Log-Eintrag in die Datenbank. Das Log wird von der
     * JavaScript-Applikation der Messieurs von der Online-Redaktion
     * benutzt, um den aktuellen Spielstand im Web anzuzeigen.
     *
     * @param bots Ein Set, das ein oder zwei Bots enthält. Position,
     * Blickrichtung und Status der Bots wird in die Datenbank geloggt.
     * @param simTimeElapsed Simulatorzeit [ms] seit Beginn des Spiels
     * @throws IllegalArgumentException Falls der Parameter <code>bots</code>
     * weniger als ein oder mehr als zwei Elemente enth&auml;lt.
     * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
     * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
     * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
     * Aufruf von <code>setWinner</code> beendet wurde.
     *
     * @see World#getSimTimeInMs()
     */
    public void log(Set<Bot> bots, long simTimeElapsed)
    throws IllegalArgumentException, SQLException, NullPointerException {
    	// Gesundheitscheck
    	if (bots.size() != 1 && bots.size() != 2) {
    		throw new IllegalArgumentException("Falsche Anzahl von Bots im " +
    				"Set: erwarteter Wert 1 oder 2, tats\u00E4chlicher Wert "+
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
    		"pos2x, pos2y, head2x, head2y, state2) " +
    		"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    	ArrayList<Object> values = new ArrayList<Object>();
    	values.add(currentGame.getUniqueId());
    	values.add(simTimeElapsed);
        Iterator<Bot> it = bots.iterator();

    	Bot b1 = it.next();
    	values.addAll(getFieldValues(b1));

    	if (it.hasNext()) {
    		// wir haben zwei Bots
    		Bot b2 = it.next();
    		values.addAll(getFieldValues(b2));
    		execSql(common + twoBots, values.toArray());
    	} else {
    		// wir haben einen Bot
    		execSql(common + oneBot, values.toArray());
    }
    }

    //$$ doc
    private List<Object> getFieldValues(Bot b) {
        Point3d pos = b.getPosition();
        Vector3d head = b.getHeading();
        return Arrays.asList(new Object[] {
        	pos.x,
        	pos.y,
        	head.x,
        	head.y,
        	b.getObstState()});
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
        	"SELECT * FROM ctsim_level WHERE id = ?", levelId);
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
            "SET winner = ?, finishtime = ?, state = ? "+
            "WHERE level = ? AND game = ?",
            winnerBotId, finishSimTime, GameState.GAME_OVER,
            currentGame.getLevelId(), currentGame.getGameId());

        propagateWinner(winnerBotId);

        currentGame.set(null, null);
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
        switch (currentGame.getLevelId()) {
        	case 0: // Spiel um den 3. Platz wurde gespielt
            case 1: // Finale wurde gespielt
            	lg.finer("Gewinner des Finales oder Spiels um den 3. Platz " +
            			"wird im Turnierbaum nicht weitergesetzt");
                break;
            case 2: // Halbfinale wurde gespielt
            	lg.finer("Setze Gewinner ins Finale");
                placeBot(winnerBotId, 1, 1);
                lg.finer("Setze Verlierer ins Spiel um den 3. Platz");
                // 0 == Sonderwert fuer Spiel um den 3.
                placeBot(getCurrentGameLoserId(winnerBotId), 0, 1);
                break;
            default: // jedes andere Level
            	lg.finer("Setze Gewinner ein Level weiter");
                placeBot(winnerBotId,
                	currentGame.getLevelId()/2,
                	(currentGame.getGameId()+1)/2);
        }
    }

    /**
	 * Liefert die Binary eines der beiden Bots im aktuellen Spiel.
	 *
	 * @param whichPlayer Name des DB-Felds, das angibt, welcher Bot. Kann im
	 * gegenw&auml;rtigen DB-Schema "bot1" oder "bot2" sein.
	 * @return Die mit dem gew&auml;hlten Bot im aktuellen Spiel assoziierte
	 * Binary (ELF).
	 * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
	 * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
	 * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
	 * Aufruf von <code>setWinner</code> beendet wurde.
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
        return getBotBinary("bot1");
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
        return getBotBinary("bot2");
    }

    /**
	 * Liefert den Schl&uuml;ssel eines der Spieler im zur Zeit laufenden Spiel.
	 *
	 * @param whichPlayer Gibt das DB-Feld an, das den gew&uuml;nschten
	 * Schl&uuml;ssel enth&auml;lt. Kann "bot1" oder "bot2" sein.
	 * @throws NullPointerException Falls zur Zeit kein Spiel l&auml;ft, d.h.
	 * falls noch nie <code>setGameRunning</code> aufgerufen wurde oder falls
	 * seit dem letzten <code>setGameRunning</code>-Aufruf das Spiel durch
	 * Aufruf von <code>setWinner</code> beendet wurde.
	 */
    private int getBotId(String whichPlayer)
    throws SQLException, NullPointerException {
        ResultSet rs = execSql(
            "SELECT * FROM ctsim_game WHERE level = ? AND game = ?;",
            currentGame.getLevelId(), currentGame.getGameId());
        rs.next();
        return rs.getInt(whichPlayer);
    }

    /** Liefert den DB-Schl&uuml;ssel von Spieler 1 des zur Zeit laufenden
     * Spiels.
     */
    public int getBot1Id() throws SQLException {
        return getBotId("bot1");
    }

    /** Liefert den DB-Schl&uuml;ssel von Spieler 2 des zur Zeit laufenden
     * Spiels.
     */
    public int getBot2Id() throws SQLException {
        return getBotId("bot2");
    }

    //$$ doc Method, NullP und alles
    public int getMaxGameLengthInMs() throws SQLException {
        return currentGame.getMaxLengthInMs();
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
            "SELECT * FROM ctsim_game " +
            "WHERE state = ? ORDER BY scheduled",
            GameState.READY_TO_RUN);
    }

    /** Startet ein Spiel, d.h. setzt seinen Status in der Datebank
     * entsprechend.
     *
     * @param levelId Level des zu startenden Spiels.
     * @param gameId Nummer des zu startenden Spiels innerhalb seines Levels.
     */
    public void setGameRunning(int levelId, int gameId) throws SQLException {
        execSql("UPDATE ctsim_game " +
                "SET state= ? "+
                "WHERE level = ? AND game = ?",
                GameState.RUNNING, levelId, gameId);
        currentGame.set(levelId, gameId);
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
        return currentGame.getLevelId() != -1;
    }
        }
