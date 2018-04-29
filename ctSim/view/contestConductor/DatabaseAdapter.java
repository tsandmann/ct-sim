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

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import ctSim.controller.Main;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.contestConductor.TournamentPlanner.TournamentPlanException;

/** <p>Diese Klasse (und die von ihr abgeleiteten) abstrahieren die Datenbank, d.h. sitzen zwischen der
 * Datenbank für den Wettbewerb Oktober 2006 einerseits und ContestConductor / TournamentPlanner
 * andererseits.</p>
 *
 * <p>Diese Klasse besteht, um den ContestConductor und den TournamentPlanner um SQL zu entlasten.
 * (Ein Aufruf <code>db.setWinner(42);</code> ist übersichtlicher als ein halbes dutzend Zeilen
 * Datenbank-Getue).</p>
 *
 * <p>Alle Methoden dieser Klasse und alle Methoden der abgeleiteten Klassen werfen Instanzen von
 * java.sql.SQLException, falls die darunterliegenden Methoden, die auf die Datenbank zugreifen,
 * eine solche Exception werfen. üblicherweise deutet das darauf hin, dass die Datenbank nach einem
 * anderen Schema aufgebaut ist als der Code erwartet.</p>
 *
 * @see "Von dieser Klasse abgeleitete Klassen"
 * @see ContestConductor
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
class DatabaseAdapter {
	/** HashMap für Statements */
	class StatementCache extends HashMap<String, PreparedStatement> {
        /** UID */
		private static final long serialVersionUID =
        	- 293074803104060506L;

		/**
		 * @see java.util.HashMap#get(java.lang.Object)
		 */
		@Override
		public PreparedStatement get(Object key) {
			// Wenn nicht drin, hinzufügen, damit super.get garantiert was zurückliefert
			if (! containsKey(key)) {
				/*
				 * Cast ist okay, weil wir eine HashMap<String, ...> sind und der Compiler sicherstellt,
				 * dass nur Strings in uns geputtet werden
				 */
				String s = (String)key;
				try {
	                put(s, dbConn.prepareStatement(s));
                } catch (SQLException e) {
                	// In non-checked Excp einwickeln, um mit der geerbten Signatur kompatibel zu bleiben
                	throw new RuntimeException(e);
                }
			}
		    return super.get(key);
		}
	}

	/**
	 * <p>
	 * Repräsentiert die möglichen Zustände eines Spiels. Die Enum funktioniert Map-artig: sie umfasst
	 * Paare aus einem Symbol im Code und einem String zugeordnet. Letzterer ist die Repräsentation,
	 * wie der Spielzustand in der Datenbank dargestellt wird.
	 * </p>
	 * Für ein Beispiel und zur Verwendung siehe Methode {@link #toString()}.
	 */
	enum GameState {
		/** Zeigt an, dass (noch) keine Spieler für das Spiel eingetragen sind. */
		NOT_INITIALIZED("not init"),
		/** Zeigt an, dass ein Spieler, aber (noch) kein zweiter für das Spiel eingetragen ist. */
		WAITING_FOR_BOT2("wait for bot2"),
		/** Zeigt an, dass das Spiel gestartet werden kann (es sind so viele Spieler eingetragen wie
		 * nötig).
		 */
		READY_TO_RUN ("ready to run"),
		/**
		 * Zeigt an, dass das Spiel gestartet, aber noch nicht abgeschlossen ist. Normalerweise hat zu
		 * jedem Zeitpunkt nur höchstens ein Spiel diesen Zustand.
		 */
		RUNNING ("running"),

		/** Zeigt an, dass das Spiel abgeschlossen ist. */
		GAME_OVER("game over");

		/** Gamestate in DB */
		private final String representationInDb;

		/**
		 * Zustand des Spiels
		 * 
		 * @param representationInDb	Zustand
		 */
		GameState(String representationInDb) {
			this.representationInDb = representationInDb;
		}

		/** <p>Diese Methode erlaubt es, Objekte dieser Klasse (dieser Enum) auf unbürokratische Art in
		 * Strings einzubauen, die SQL-Queries repräsentieren. Beispiel:</p>
		 * <code>"SELECT * FROM games WHERE state = '"+GameState.RUNNING+"'"</code>
		 *
		 * @return Der String, der in der Datenbank das Objekt dieser Klasse repräsentiert. Beispielsweise
		 * liefert GameState.NOT_INITIALIZED.toString() den unter
		 * <a href="#enum_constant_summary">Enum Constant Summary</a> aufgeführten Wert.
		 */
		@Override
		public String toString() {
			return representationInDb;
		}
	}

	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.contestConductor");

	static {
		Main.dependencies.registerImplementation(ContestDatabase.class);
	}

	/** Die Verbindung zu der Datenbank, die alles rund um den Wettbewerb weiß. */
	private Connection dbConn = null;

	/** Statement-Cache */
	protected StatementCache statementCache = new StatementCache();
	
	/** Datenbank */
	private ContestDatabase connFactory;


	/**
	 * DB-Verbidung
	 * 
	 * @param connFactory	DB
	 */
	protected DatabaseAdapter(ContestDatabase connFactory) {
		this.connFactory = connFactory;
		try {
			acquireConn();
            lg.info("Verwende Datenbank " + dbConn.getMetaData().getURL());
        } catch (SQLException e) {
        	// No-op, da nur ne Info-Meldung nicht geht
        }
	}

	/**
	 * Connection setzen
	 * 
	 * @throws SQLException
	 */
	protected void acquireConn() throws SQLException {
		statementCache.clear();
		if (dbConn != null)
			dbConn.close();
		dbConn = connFactory.getConnection();
	}

	/**
	 * @param sqlWithInParams
	 * @param inValues
	 * @return PreparedStatement
	 * @throws SQLException
	 */
	protected PreparedStatement buildPreparedStatement(String sqlWithInParams,
		Object... inValues) throws SQLException {
		PreparedStatement rv = statementCache.get(sqlWithInParams);
		// Sicherheitsmaßnahme, nicht dass einer auf teilweise alten Daten operiert
		rv.clearParameters();
		for (int i = 0; i < inValues.length; i++) {
			// PreparedStatement ist 1-based, inValues ist 0-based
			if (inValues[i] instanceof GameState)
				rv.setString(i + 1, inValues[i].toString());	//$$ GameState-Hack
			else if (inValues[i] instanceof RenderedImage) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				try {
	                ImageIO.write((RenderedImage)inValues[i], "png", out);
                } catch (IOException e) {
                	throw new IllegalArgumentException(e);	//$$ doc IllegalArg
                }
				rv.setBinaryStream(i + 1,
					new ByteArrayInputStream(out.toByteArray()), out.size());
			}
			else
				rv.setObject(i + 1, inValues[i]);
		}
		return rv;
	}

	/**
	 * <p>
	 * Führt einen SQL-Befehl aus, unter Benutzung eines Caches von Befehlen. Falls der SQL-Befehl ein
	 * SELECT ist, gibt die Methode die selektierten Daten zurück.
	 * </p>
	 * <p>
	 * <strong>Beispielaufruf:</strong>
	 * <code>ResultSet r = execSql("select * from gurken where id = ? and length = ?", gurkenId, 42);</code>
	 * - Die Methode verwendet SQL-Strings mit IN-Parametern (dieselbe Syntax wie in {@link PreparedStatement}
	 * dokumentiert). Im wesentlichen gilt: Die Fragezeichen im String werden reihenfolgenrichtig durch die
	 * Parameter ersetzt: Erstes Fragezeichen = erster Parameter, zweites = zweiter Parameter, usw.
	 * Für die Ersetzung wird {@link PreparedStatement#setObject(int, Object)} verwendet; Details siehe
	 * dessen Dokumentation und dessen Beschreibung in der
	 * <a href="http://java.sun.com/products/jdbc/download.html">JDBC-Spezifikation</a>
	 * (für Java 1.5 gilt JDBC 3.0, Sektion 13.2.2.2 und Anhang B-4).
	 * </p>
	 * <p>
	 * <strong>Wichtig</strong> bei der Verwendung der Methode ist, die Zahl der verschiedenen SQL-Strings
	 * klein zu halten.
	 * <ul>
	 * <li>Doof:
	 * <code>execSql("select * from rueben where id = " + ruebenId + " and mass > " + minMass);</code></li>
	 * <li>Clever:
	 * <code>execSql("select * from rueben where id = ? and mass > ?", ruebenId, minMass);</code></li>
	 * </ul>
	 * Bei wiederholten Aufrufen (etwa mit wechselnden <code>ruebenId</code>-Werten) ist der Speicherbedarf
	 * im ersteren Fall linear ansteigend, da die Strings und daher die PreparedStatement-Objekte immer
	 * wieder neu erzeugt werden. Im letzteren Fall ist der Speicherbedarf konstant, da die Methode den
	 * String schon kennt und das PreparedStatement-Objekt des vorherigen Aufrufs wiederverwendet.
	 * </p>
	 * <p>
	 * <strong>Wozu?</strong> Die Methode mit ihrem Statement-Cache wurde aufgrund von Speicherlecks im
	 * ctSim erforderlich. Es hat sich herausgestellt, dass bisher die laufend erzeugten Statements
	 * offenbar erst zur Garbage Collection freigegeben werden, wenn das Connection-Objekt, von dem sie
	 * erzeugt wurden, geschlossen wurde - d.h. am Programmende. Korrekt angewendet löst diese Methode
	 * das Speicherproblem. $$ doc update close()
	 * </p>
	 * <p>
	 * Die vorliegende Implementierung unterstützt nur SELECT, INSERT, UPDATE und DELETE. Unterstützung
	 * für weitere SQL-Befehle sollte leicht nachzurüsten sein.
	 * </p>
	 *
	 * @param sqlWithInParams	(siehe Beispielaufruf oben)
	 * @param inValues			(siehe Beispielaufruf oben)
	 * @return Falls der Parameter <code>sqlWithParams</code> mit "select" beginnt, das ResultSet,
	 * wie es ein Aufruf von executeQuery mit demselben SQL-Befehl zurückgegeben hätte. Falls der
	 * genannte Parameter mit "insert", "update" oder "delete" beginnt, wird <code>null</code>
	 * zurückgegeben.
	 * @throws SQLException 
	 * @throws IllegalArgumentException	falls der Parameter <code>sqlWithParams</code> weder mit SELECT
	 * 				noch mit INSERT, UPDATE oder DELETE beginnt. (Groß-/Kleinschreibung irrelevant.)
	 */
	protected ResultSet execSql(String sqlWithInParams, Object... inValues)
	throws SQLException, IllegalArgumentException {
		/* Idee: SQL-Kommando ausführen; wenn's nicht klappt, Verbindung neu herstellen und nochmal
		 * ausführen. Fix gegen abreißende DB-Verbindungen (wenn die Verbindung abreißt, bekommt man von
		 * MySQL mit dem nächsten SQL-Befehl eine com.mysql.jdbc.CommunicationsException, die von
		 * SQLException abgeleitet ist)
		 */
		try {
			return tryExecSql(sqlWithInParams, inValues);
		} catch (SQLException e) {
			lg.warn(e, "SQLException; stelle DB-Verbindung neu her und " +
					"probiere nochmal");
			acquireConn();
			/* Wenn jetzt nochmal eine Exception kommt, reichen wir sie nach oben durch - es liegt nicht
			 * an einer abgerissenen Verbindung, sondern an falscher SQL-Syntax oder so - da hilft auch
			 * keine neue Verbindung.
			 */
			return tryExecSql(sqlWithInParams, inValues);
		}
	}

	/**
	 * Führt ein SQL-Kommando aus
	 * 
	 * @param sqlWithInParams	Kommando
	 * @param inValues			Werte
	 * @return Result
	 * @throws SQLException
	 * @throws IllegalArgumentException
	 */
	private ResultSet tryExecSql(String sqlWithInParams, Object... inValues)
	throws SQLException, IllegalArgumentException {
		if (sqlWithInParams.toLowerCase().startsWith("select")) {
			return buildPreparedStatement(sqlWithInParams, inValues).
				executeQuery();
		} else if (Misc.startsWith(sqlWithInParams.toLowerCase(),
			"insert", "update", "delete")) {
			buildPreparedStatement(sqlWithInParams, inValues).executeUpdate();
			return null;
		} else {
			throw new IllegalArgumentException("Kann keine SQL-Befehle " +
					"außer SELECT, INSERT, UPDATE und DELETE");
		}
	}

	/** Trägt einen Bot für ein Spiel ein. Aufzurufen entweder vor dem Wettbewerb in der Planungsphase
	 * oder im Wettbewerb, wenn sich durch die Spielergebnisse herausstellt, welche Bots um ein Level
	 * weiterkommen.
     *
     * @param botId
     * 			Bot, repräsentiert von seinem Primärschlüssel, der eingettragen werden soll.
     * 			Falls <code>null</code>, tut die Methode nichts.
     * @param levelId	das Level, in das der Bot platziert werden soll
     * @param gameId	die Nummer des Spiels im jeweiligen Level
	 * @throws SQLException 
     * @throws TournamentPlanException	wenn für das übergebene Spiel bereits zwei Spieler eingetragen sind
     */
    public void placeBot(Integer botId, int levelId, int gameId)
    throws SQLException, TournamentPlanException {
    	if (botId == null)
    		return;

    	lg.fine("Trage Bot ID = '"+botId+"' für Level "+levelId+
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