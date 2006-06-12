package ctSim.Model.Rules;

import java.sql.*;
import java.util.Calendar;
import java.util.Iterator;

import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;
import ctSim.Model.Bots.Bot;

/** Judge, der einen kompletten Labyrinthwettbwerb ausrichten kann */
public class LabyrinthContestJudge extends LabyrinthJudge {
	static final String STATE_NOT_INIT ="not init";
	static final String STATE_WAIT_FOR_BOT1 ="wait for bot1";
	static final String STATE_WAIT_FOR_BOT2 = "wait for bot2";
	static final String STATE_READY_TO_RUN  = "ready to run";
	static final String STATE_RUNNING  = "running";
	static final String STATE_GAME_OVER = "game over";
	String databaseUrl = "jdbc:mysql://localhost:3306/JunkDB";
	Connection databaseConnection = null;

	
	static final int STATE_WAIT_FOR_GAME =1;
	static final int STATE_GAME_RUNNING =2;
	static final int STATE_DONE =3;

	int state = STATE_WAIT_FOR_GAME;

	int runningLevel;
	int runningGame;
	String runningParcours;
	
	Bot bot1 = null;
	Bot bot2 = null;
	
	/**
	 * Konstruktor
	 *
	 */
	public LabyrinthContestJudge() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void work() {
		try {
			switch (state){
				case STATE_WAIT_FOR_GAME:
						ResultSet rs = getNextGame();
						if (rs.next()){
							Timestamp sceduled = rs.getTimestamp("sceduled");
							Calendar now = Calendar.getInstance();                         // lokales Kalendersystem mit aktueller Zeit
							
							long ms= sceduled.getTime()-now.getTimeInMillis();
							if (ms >0){
								try {
									System.out.println(now.getTime()+": Warte "+ms/1000+" s auf naechsten Wettkampf ("+sceduled+")");
									sleep(ms);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							} else {
								state=STATE_GAME_RUNNING;
								System.out.println(now.getTime()+": Starte Rennen. Level= "+rs.getInt("level")+" Game= "+rs.getInt("game")+ " Sceduled= "+rs.getTimestamp("sceduled"));
								try {
									startGame(rs);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
						} else {
							state= STATE_DONE;
						}
					break;
					
				case STATE_GAME_RUNNING:
						check();
						log();
					break;

				case STATE_DONE:		
					System.out.println("Es gibt keine weiteren Rennen mehr. Beende den Judge!");
					die();
					break;
			}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

	/**
	 * Macht einen Log-Eintrag in die Datenbank
	 */
	private void log() {
		try {
			Vector3f pos1 = bot1.getPos();
			Vector3f head1 = bot1.getHeading();
			Vector3f pos2 = bot2.getPos();
			Vector3f head2 = bot1.getHeading();
			
			Statement statement;
			statement = databaseConnection.createStatement();
			statement.executeUpdate(
					"INSERT INTO log" +
					" ( logtime, game, pos1x, pos1y, head1x, head1y, pos2x, pos2y, head2x, head2y ) " +
					"VALUES ( "+
					    getRunTime()+ ", " + runningGame + ", " +
					    pos1.x + ", " + pos1.y +", " + head1.x + ", " + head1.y + ", " +
					    pos2.x + ", " + pos2.y +", " + head2.x + ", " + head2.y + ", " +
					" )" 
					);
			
		} catch (SQLException e) {
			ErrorHandler.error("Probleme beim loggen "+e);
			e.printStackTrace();
		} 		
	}

	@Override
	protected void kissArrivingBot(Bot bot, long time) {
		System.out.println("Bot "+bot.getName()+" hat das Ziel nach "+time+" ms erreicht!");
		
		try {
			Statement statement;
			statement = databaseConnection.createStatement();
			statement.executeUpdate(
					"UPDATE game " +
					"SET winner='"+bot.getName()+"', " +
					"finishtime="+time+", "+
					"state= '"+STATE_GAME_OVER+"' "+
					"WHERE level ="+runningLevel+" AND game ="+runningGame
					);

			propagateWinner(runningLevel,runningGame);
			
		} catch (SQLException e) {
			ErrorHandler.error("Probleme beim sichern der Zieldaten "+e);
			e.printStackTrace();
		} catch (GamePlanException e) {
			ErrorHandler.error("Probleme beim Fortschreiben des Spielplans "+e);
			e.printStackTrace();			
		}

		state=STATE_WAIT_FOR_GAME;
		suspendWorld(true);
		// Alle Bots entfernen
		while (getActiveParticipants() > 0){

			Iterator it = getController().getWorld().getAliveObstacles().iterator();
			while (it.hasNext()){
				Object obst = it.next();
				if (obst instanceof Bot)
					((Bot) obst).die();
			}

			try {
				sleep(100);
			} catch (InterruptedException e) {
				// Auch egal
			}
			System.out.println("Warte bis alle Bots gestorben sind!");
		}
		
		bot1=null;
		bot2=null;
		// Rennen laueft nicht mehr
		setRaceStartet(false);
	}

	/**
	 * Startet ein Rennen
	 * @param game Verweis auf das game
	 * @throws Exception 
	 */
	private void startGame(ResultSet game) throws Exception {
		setRaceStartet(false);
		
		String bot1Name = game.getString("bot1");
		String bot2Name = game.getString("bot2");
		
		runningGame = game.getInt("game");
		runningLevel= game.getInt("level");

		Statement statement = databaseConnection.createStatement();
		ResultSet level= statement.executeQuery(
				"SELECT * " +
				"FROM level " +
				"WHERE level = "+runningLevel);
		
		level.next();
		String newParcours = level.getString("parcours");

		System.out.println("Starte Level="+ runningLevel +" Game="+runningGame+" Bot1="+bot1Name+" Bot2="+bot2Name+ " Parcours="+newParcours);
		
		if (!newParcours.equals(runningParcours)){
			System.out.println("Muss den Parcours wechseln!");
			getController().changeParcours(newParcours);
			runningParcours=newParcours;
		}

		statement.executeUpdate(
				"UPDATE game " +
				"SET state= '"+STATE_RUNNING+"' "+
				"WHERE level ="+runningLevel+" AND game ="+runningGame
				);

		
		// Die Welt wird pausiert, bis alle Spieler da sind
		suspendWorld(true);

		ResultSet bots= statement.executeQuery(
				"SELECT * " +
				"FROM bot " +
				"WHERE bot = '"+bot1Name+"'");
		bots.next();
		String bin=bots.getString("bin");
		getController().invokeBot(bot1Name,bin);

		
		while (getActiveParticipants() != 1){
			sleep(100);
			System.out.println("Warte auf Bot: "+bot1Name+" ("+bin+")");
		}
		bot1= (Bot) getController().getWorld().getAliveObstacle(bot1Name);
		
		
		bots= statement.executeQuery(
				"SELECT * " +
				"FROM bot " +
				"WHERE bot = '"+bot2Name+"'");
		bots.next();
		getController().invokeBot(bot2Name,bots.getString("bin"));

		// Eigentlich koennte man hier auf den 2. Bot warten, aber das ist egal, da check() das macht
		while (getActiveParticipants() != 2){
			sleep(100);
			System.out.println("Warte auf Bot: "+bot2+" ("+bin+")");
		}
		bot2=(Bot) getController().getWorld().getAliveObstacle(bot2Name);
	}

	/**
	 * Erzeugt ein paar Testdaten
	 * Achtung loescht die Tabellen zuvor
	 */
	private void createTestData(){
		try {
			int gametime=60;
			int savetime=10;
			
			Statement statement = databaseConnection.createStatement();
			
			statement.executeUpdate("DELETE from team");
			statement.executeUpdate("DELETE from bot");
			for (int i=0; i< 13; i++){
				statement.executeUpdate("INSERT INTO team (team) values('team"+i+"')");
				statement.executeUpdate(
						"INSERT INTO bot " +
						"(bot, team, bin) " +
						"VALUES ('bot"+i+"','team"+(i%2+1)+"','../ct-Bot/Debug-Linux/ct-Bot.elf')");
			}
			
			Calendar cal = Calendar.getInstance();                         // lokales Kalendersystem mit aktueller Zeit
			cal.set(2006, Calendar.MAY, 27, 20,50,0);
			
			statement.executeUpdate("DELETE from level");

			Timestamp t = new Timestamp(cal.getTimeInMillis());
			statement.executeUpdate("INSERT INTO level (level, parcours, gametime, sceduled) " +
					"values ("+1+",'parcours/testparcours0.xml',"+gametime+",'"+t+"')");
			cal.add(Calendar.SECOND,-gametime-savetime);
			
			t = new Timestamp(cal.getTimeInMillis());
			statement.executeUpdate("INSERT INTO level (level, parcours, gametime, sceduled) " +
					"values ("+0+",'parcours/testparcours0.xml',300,'"+t+"')");
			cal.add(Calendar.SECOND,-gametime*2-savetime*2);
			
			
			for (int i=2; i< 16; i*=2){
				t = new Timestamp(cal.getTimeInMillis());
				statement.executeUpdate("INSERT INTO level (level, parcours, gametime, sceduled) " +
						"values ("+i+",'parcours/testparcours0.xml',"+gametime+",'"+t+"')");
				cal.add(Calendar.SECOND,-gametime*i*2 - savetime*i*2);
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Bereitet die eintraege in der Tabelle Games vor
	 * Achtung, loescht alle bisherigen eintraege
	 * @param maxLevel hoechstes Level, fuer das vorbereitet wird
	 * @throws SQLException
	 */
	private void prepareGames(int maxLevel) throws SQLException{
		Statement statement = databaseConnection.createStatement();

		// Cleanup
		statement.executeUpdate("DELETE FROM game"); 
	
		// Fuer jedes Level Games anlegen
		for (int level=1; level <= maxLevel; level*=2){
			for (int game=1; game<= level; game++){
				statement.executeUpdate("INSERT INTO game (level, game) " +
						"values ("+level+","+game+")"); 
			}
		}
		// Das Semifinale faellt aus dem Schema:
		statement.executeUpdate("INSERT INTO game (level, game) " +
				"values ("+0+","+1+")"); 
		
		// Alle Level, bis auf das letzte werden auf jedenfall gebraucht, daher state setzen
		statement.executeUpdate("UPDATE game " +
				"SET state='"+STATE_WAIT_FOR_BOT1+"'"+
				"WHERE level<"+maxLevel);
	}
	
	/**
	 * Traegt zwei Bots in ein Game ein
	 * @param bot Bot der platziert werden soll
	 * @param level Das Level in das das Game eingefuegt wird
	 * @param game Die Nummer des Spieles im jeweiligen Level
	 * @throws SQLException 
	 * @throws GamePlanException Wenn das Spiel bereits voll ist 
	 */
	private void placeBot(String bot, int level, int game) throws SQLException, GamePlanException{
		Statement statement = databaseConnection.createStatement();
		// Suche das Spiel heraus
		ResultSet rs = statement.executeQuery("SELECT * from game WHERE level="+level+" AND game="+game);
		
		// Bots auslesen
		rs.next();
		String bot1= rs.getString("bot1");
		String bot2= rs.getString("bot2");
		
		// Bots aktualisieren
		if (bot1== null)
			statement.executeUpdate("UPDATE game " +
					"SET bot1='"+bot+"',"+
					"state='"+STATE_WAIT_FOR_BOT2 +"' "+
					"WHERE level="+level+" AND game="+game); 
		else {
			if (bot2== null)
				statement.executeUpdate("UPDATE game " +
						"SET bot2='"+bot+"',"+
						"state='"+STATE_READY_TO_RUN +"' "+
						"WHERE level="+level+" AND game="+game); 
			else {
				GamePlanException e =new GamePlanException("Kein Platz in Level: "+level+" Game: "+game);
				ErrorHandler.error("Fehler"+e);
				throw e;
			}
		}
		
	}
	
	/**
	 * Setzt den Gewinner eines Games ein Level hoeher
	 * @param level
	 * @param game
	 * @throws SQLException 
	 * @throws GamePlanException 
	 */
	private void propagateWinner(int level, int game) throws SQLException, GamePlanException{
		Statement statement = databaseConnection.createStatement();
		ResultSet rs = statement.executeQuery(
				"SELECT * " +
				"FROM game " +
				"WHERE level ="+level+" AND game ="+game
				);
		
		rs.next();
		String winner = rs.getString("winner");
		
		String looser = rs.getString("bot1");
		if (looser.equals(winner))
			looser = rs.getString("bot2");
		
		if (winner != null)
			if (level > 2)	
				placeBot(winner,level/2,(game+1)/2);
			else {
				// Aus dem Halbfinale kommen die Gewinner ins Finale und die Verlierer ins Semifinale
				if (level ==2) {
					placeBot(winner,1,1);
					placeBot(looser,0,1);
				}
			}
		else {
			GamePlanException e = new GamePlanException("Fehler: Versuch einen nicht ermittelten Sieger zu propagieren: Level= "+level+" Game= "+game);
			ErrorHandler.error(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Setzt die Spielzeiten fuer alle Games in einem Level
	 * @param level Das Level, fuer das die Zeiten gesetzt werden sollen
	 * @throws SQLException
	 */
	private void scedule(int level) throws SQLException{
		Statement statement = databaseConnection.createStatement();

		// Nur Bots mit binary waehlen
		ResultSet rs = statement.executeQuery("SELECT * from level WHERE level ="+level);
		
		rs.next();
		Timestamp baseTime = rs.getTimestamp("sceduled");
		int gameTime = rs.getInt("gametime");
		
		rs = statement.executeQuery(
				"SELECT * " +
				"FROM game " +
				"WHERE level ="+level+" AND state !='"+STATE_NOT_INIT+"' " +
				"ORDER BY game"
				);
		
		while (rs.next()){
			statement.executeUpdate("UPDATE game "+
					"SET sceduled='"+baseTime+"' "+
					"WHERE level ="+level+ " AND game = "+rs.getInt("game"));
			baseTime.setTime(baseTime.getTime()+gameTime*1000);
		}
		
	}
	
	/**
	 * Alle laufenden spiele Zuruecksetzen, damit sie neu begonnen werden
	 * @throws SQLException
	 */
	private void recover() throws SQLException{
		Statement statement = databaseConnection.createStatement();

		statement.executeUpdate(
				"UPDATE game " +
				"SET state= '"+STATE_READY_TO_RUN+"' "+
				"WHERE state = '"+STATE_RUNNING+"'"
				);

		
	}
	
	private void createGamePlan() throws SQLException{
		try {
			Statement statement = databaseConnection.createStatement();

			// Nur Bots mit binary waehlen
			ResultSet rs = statement.executeQuery("SELECT * from bot WHERE bin !=''");
			rs.last();
			
			// Anzahl bestimmen
			int bots = rs.getRow();
			System.out.println(bots+" Bots sind mit korrekten Binaries in der Datenbank");
			
			// Nur Bots mit binary waehlen
			rs = statement.executeQuery("SELECT * from bot WHERE bin !=''");
			
			
			int level=1;
			// Stelle fest, in welchem Level der Wettkampf beginnt
			while (level*2 < bots){		// es muss Platz fuer alle bots sein
				level *=2;
			}

			// Bereite die Tabelle Games vor !!! Achtung loescht sie
			prepareGames(level);
			

			// Wieviele Bots bekommen eine Freikarte direkt ins naechste Level?
			int greencards = level*2 - bots;

			// Der Rest der Bots muss in die Vorrunde 
			// Diese Zahl ist per Definition gerade
			int rest = bots - greencards;
			for (int game=1; game<= rest/2; game++){
				rs.next();
				placeBot(rs.getString("bot"),level,game);
				rs.next();
				placeBot(rs.getString("bot"),level,game);
			}
			
			
			// Nun die Bots mit greencard ins naechste Level schubsen
			level /=2;
			int game=level;	// Wir muessen hinten anfangen zu platzieren
			// alle restlichen Bots
			while (rs.next()){
				placeBot(rs.getString("bot"),level,game);
				if (rs.next())
					placeBot(rs.getString("bot"),level,game);
				game--;
			}

			
			level*=2;
			// Zeitplaene setzen
			for (;level >=1;level/=2)
				scedule(level);
			// Finale und Semifinale sind sonderfaelle
			scedule(0);
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} catch (GamePlanException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Sucht das naechste Spiel aus der Datenbank heraus
	 * @return Ein Resultset, das auf das Spiel zeigt
	 * @throws SQLException
	 */
	private ResultSet getNextGame() throws SQLException{
		Statement statement = databaseConnection.createStatement();

		// Nur Spiele waehelen, die STATE_READY_TO_RUN haben
		ResultSet rs = statement.executeQuery(
				"SELECT * " +
				"FROM game " +
				"WHERE state ='"+STATE_READY_TO_RUN+"'" +
				"ORDER BY sceduled " +
				"LIMIT 1");
		
		return rs;
	}
	
	
	
	@Override
	protected void init() {
		//	Register the JDBC driver for MySQL.
	    try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			databaseConnection =DriverManager.getConnection(databaseUrl,"cttest", "cttest");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Connection: " + databaseConnection);

		try {
			recover();

			createTestData();
			
			createGamePlan();
		    
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Diese Exception tritt auf, wenn etwas mit dem Gameplan nicht stimmt
	 * @author bbe (bbe@heise.de)
	 */
	private class GamePlanException extends Exception{
		/**
		 * Konstruktor
		 * @param string
		 */
		public GamePlanException(String string) {
			super(string);
		}

		/**
		 */
		private static final long serialVersionUID = 1L;
	}
}
