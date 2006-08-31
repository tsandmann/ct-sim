package ctSim.model.rules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Set;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.ErrorHandler;
import ctSim.SimUtils;
import ctSim.controller.Controller;
import ctSim.model.AliveObstacle;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.view.Debug;
import ctSim.view.sensors.RemoteControlGroupGUI;

/** Judge, der einen kompletten Labyrinthwettbwerb ausrichten kann */
public class LabyrinthContestJudge extends Judge {
	int state = STATE_WAIT_FOR_GAME;

	// status vom judge
	static final String STATE_NOT_INIT ="not init";
	static final String STATE_WAIT_FOR_BOT1 ="wait for bot1";
	static final String STATE_WAIT_FOR_BOT2 = "wait for bot2";
	static final String STATE_READY_TO_RUN  = "ready to run";
	static final String STATE_RUNNING  = "running";
	static final String STATE_GAME_OVER = "game over";
	static final int STATE_WAIT_FOR_GAME =1;
	static final int STATE_GAME_RUNNING =2;
	static final int STATE_DONE =3;
	
	String databaseUrl = "jdbc:mysql://localhost:3306/ctjudge";
	Connection databaseConnection = null;

	/** 8 fuer Achtelfinale, 4 fuer Viertelfinale usw. */
	//$$ vielliecht unnötig
	int runningLevel;
	/** Nummer des aktuellen Spiels im runningLevel */  
	int runningGame;
	
	/** Typen, die gegeneinander laufen */
	Bot bot1 = null;
	Bot bot2 = null;
	
	String bot1Name, bot2Name; //$$
	
	private Controller ctrl;
	private World world;
	
	private boolean first = true;
	private boolean second = false;
	
	/**
	 * Konstruktor
	 *
	 */
	public LabyrinthContestJudge(Controller ctrl) {
		
		super(ctrl);
		this.ctrl = ctrl;
		this.init();
	}
	
	private boolean check2() throws Exception{
		
		Set<AliveObstacle> obsts = this.world.getAliveObstacles(); //$$ schöner: getBots()
		
		Bot finishCrossed = null; // Typ, der gewonnen hat
		
		if(first) { // Vorbereiten eines Spiels
			
			// Bots adden
			Statement statement = databaseConnection.createStatement();
			
//			 Die Welt wird pausiert, bis alle Spieler da sind
			//this.ctrl.pause();
//			Thread.sleep(200);

			ResultSet bots= statement.executeQuery(
					"SELECT * " +
					"FROM ctsim_bot " +
					"WHERE name = '"+bot1Name+"'");
			bots.next();
			
			
			
			
			// Hole Binary aus DB und kopiere es in eine Datei
			blob2File("bot12.exe");
			// Starte Bot aus Binary
			//this.ctrl.invokeBot(bot1Name,"bot12.exe");
			this.ctrl.invokeBot("bot12.exe");
			
			
			
			while (this.ctrl.getParticipants() != 1){
				Thread.sleep(100);
				System.out.println("Warte auf Bot: "+bot1+" ("+bot1Name+")");
			}
			//bot1= (Bot) this.ctrl.getWorld().getAliveObstacle(bot1Name);
			
			
			bots= statement.executeQuery(
					"SELECT * " +
					"FROM ctsim_bot " +
					"WHERE name = '"+bot2Name+"'");
			bots.next();
			
		
			
			// Hole Binary aus DB und kopiere es in eine Datei
			blob2File("bot22.exe");
			// Starte Bot aus Binary
			//this.ctrl.invokeBot(bot2Name,"bot22.exe");
			this.ctrl.invokeBot("bot22.exe");

			// Eigentlich koennte man hier auf den 2. Bot warten, aber das ist egal, da check() das macht
			while (this.ctrl.getParticipants() != 2){
				Thread.sleep(100);
				System.out.println("Warte auf Bot: "+bot2+" ("+bot2Name+")");
			}
			//bot2=(Bot) this.ctrl.getWorld().getAliveObstacle(bot2Name);
			
			Iterator<AliveObstacle> it = this.world.getAliveObstacles().iterator();
			
			bot1 = (Bot)it.next();
			bot2 = (Bot)it.next();
			
			this.ctrl.unpause();
			
			second = true;
			first = false;
			return false;
		}
		
		for(AliveObstacle obst : obsts) {
			
			if (second ==true){ // "Initialisierung Teil 2"
				if (obst instanceof CtBotSimTcp) //$$ sollte ctBot sein, wo sendRCCommand() auch hingehoert
					((CtBotSimTcp)obst).sendRCCommand(RemoteControlGroupGUI.RC5_CODE_5);
			}
			
			if(this.world.finishReached(new Vector3d(obst.getPosition()))) {
				
				//Debug.out.println("Bot \""+obst.getName()+"\" erreicht nach "+this.getTime()+" ms als erster das Ziel!");
				//Debug.out.println("Zieleinlauf \""+obst.getName()+"\" nach "+ this.getTime()+" ms.");
				Debug.out.println("Zieleinlauf \""+obst.getName()+"\" nach "+ SimUtils.millis2time(this.getTime()));  //$NON-NLS-1$//$NON-NLS-2$
				
				finishCrossed= (Bot)obst;
				break;
			}
		}
		if (finishCrossed != null){
			// Zustand: wir haben einen Gewinner
			kissArrivingBot(finishCrossed,getTime());
			finishCrossed=null;
		}
		
		second=false;
		
		return true;
	}
	
	//@Override
	public boolean check() {
		try {
			switch (state){
				case STATE_WAIT_FOR_GAME:
						ResultSet rs = getNextGame();
						if (rs.next()){
							Timestamp scheduled = rs.getTimestamp("sceduled");
							Calendar now = Calendar.getInstance();                         // lokales Kalendersystem mit aktueller Zeit
							
							long ms= scheduled.getTime()-now.getTimeInMillis();
							if (ms >0){
								try {
									System.out.println(now.getTime()+": Warte "+ms/1000+" s auf naechsten Wettkampf ("+scheduled+")");
									Thread.sleep(ms);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							} else {
								// Zustand: wir wollen ein Spiel starten
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
					try {
						if (check2())
							log();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;

				case STATE_DONE:		
					System.out.println("Es gibt keine weiteren Rennen mehr. Beende den Judge! -> das Programm");
					//die();
					System.exit(0);
					break;
			}
		
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}

	/**
	 * Macht einen Log-Eintrag in die Datenbank
	 */
	private void log() {
		try {
			Point3d pos1 = bot1.getPosition();
			Vector3d head1 = bot1.getHeading();
			Point3d pos2 = bot2.getPosition();
			Vector3d head2 = bot1.getHeading();
			
			int state1 =bot1.getObstState();
			int state2 =bot2.getObstState();
			
			Statement statement;
			statement = databaseConnection.createStatement();
			statement.executeUpdate(
					"INSERT INTO ctsim_log" +
					" ( logtime, game, " +
					"   pos1x, pos1y, head1x, head1y, state1, " +
					"   pos2x, pos2y, head2x, head2y, state2 ) " +
					"VALUES ( "+
					    getTime()+ ", " + runningGame + ", " +
					    pos1.x + ", " + pos1.y +", " + head1.x + ", " + head1.y + ", " + state1 + ", " +
					    pos2.x + ", " + pos2.y +", " + head2.x + ", " + head2.y + ", " + state2 + " " +
					" )" 
					);
			
		} catch (SQLException e) {
			ErrorHandler.error("Probleme beim loggen "+e);
			e.printStackTrace();
		} 		
	}
	
	private void gameTearDown()
	{
		state=STATE_WAIT_FOR_GAME;
		this.ctrl.pause();
		// Alle Bots entfernen
		while (this.ctrl.getParticipants() > 0){
	
			Iterator it = this.ctrl.getWorld().getAliveObstacles().iterator();
			while (it.hasNext()){
				Object obst = it.next();
				if (obst instanceof Bot)
					((Bot) obst).die();
			}
	
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Auch egal
			}
			System.out.println("Warte bis alle Bots gestorben sind!");
		}
	}

	/**
	 * 
	 * @param bot
	 * @param time	Simulatorzeit [ms] seit Start des Spiels 
	 */
	protected void kissArrivingBot(Bot bot, long time) {
		System.out.println("Bot "+bot.getName()+" hat das Ziel nach "+time+" ms erreicht!");
		
		try {
			Statement statement;
			statement = databaseConnection.createStatement();
			statement.executeUpdate(
					"UPDATE ctsim_game " +
					"SET winner='"+bot.getName()+"', " + //$$ soll ID sein, nicht Name
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
		
		//$$ gameTearDown()
		
		// Spiel de-initialisieren
		bot1=null;
		bot2=null;
		// Rennen laueft nicht mehr
		state=STATE_WAIT_FOR_GAME;
	}

	/**
	 * Startet ein Rennen
	 * @param game Verweis auf das game
	 * @throws Exception 
	 */
	private void startGame(ResultSet game) throws Exception {
		bot1Name = game.getString("bot1");
		bot2Name = game.getString("bot2");
		
		runningGame = game.getInt("game");
		runningLevel= game.getInt("level");

		Statement statement = databaseConnection.createStatement();
		ResultSet level= statement.executeQuery(
				"SELECT * " +
				"FROM ctsim_level " +
				"WHERE id = "+runningLevel);
		
		level.next();
		String newParcours = level.getString("parcours");

		System.out.println("Starte Level="+ runningLevel +" Game="+runningGame+" Bot1="+bot1Name+" Bot2="+bot2Name+ " Parcours="+newParcours);

		//$$ es ist fraglich, ob die Welt (ctrl.openWorld()) korrekt neuinitialisiert wird. Moeglicherweise brauchen wir diesen Code wieder.
//		if (!newParcours.equals(runningParcours)){
//			System.out.println("Muss den Parcours wechseln!");
//			this.ctrl.changeParcours(newParcours);
//			runningParcours=newParcours;
//		}

		statement.executeUpdate(
				"UPDATE ctsim_game " +
				"SET state= '"+STATE_RUNNING+"' "+
				"WHERE level ="+runningLevel+" AND game ="+runningGame
				);
		
		this.ctrl.openWorld(newParcours);
		
		state = STATE_GAME_RUNNING;
		
		this.ctrl.unpause();
	}

	/**
	 * Schreibt ein Blob in ein File
	 * @param blob Referenz auf das Blob
	 * @param filename Dateiname
	 * @throws Exception 
	 */
	private void blob2File(String fileName) throws Exception{
		
		Statement statement = databaseConnection.createStatement();
		
		File file = new File(fileName);
		FileOutputStream fos = new FileOutputStream( file );
		
		ResultSet rs = statement.executeQuery("Select * from ctsim_bot");
		
		rs.next();
		
		InputStream is = rs.getBinaryStream("bin");
		        
		int c;
		byte[] buffer=new byte[100000];
		
		while ((c =is.read(buffer)) != -1){
		        fos.write(buffer,0,c);
		        }
		        
		fos.close();        
	}
	
	/**
	 * Erzeugt ein paar Testdaten
	 * $$ Testmethode
	 * Achtung loescht die Tabellen zuvor
	 * @throws FileNotFoundException 
	 */
	private void createTestData() throws FileNotFoundException{
		try {
			int gametime=60;
			int savetime=10;
		
			String fileName="bot1.exe";
			
			Statement statement = databaseConnection.createStatement();
			
			statement.executeUpdate("DELETE from ctsim_team");
			statement.executeUpdate("DELETE from ctsim_bot");
			for (int i=0; i< 13; i++){
				statement.executeUpdate("INSERT INTO ctsim_team (name) values('team"+i+"')");

				PreparedStatement pStatement= databaseConnection.prepareStatement(
						"INSERT INTO ctsim_bot " +
						"(name, team, bin) " +
						"VALUES ('bot"+i+"','team"+(i%2+1)+"', ?)");
				
				File file = new File(fileName);
				FileInputStream fis = new FileInputStream( file );
				
				System.out.println("Store "+fileName+" Size= "+ file.length());
				pStatement.setBinaryStream(1, fis, (int)file.length());
				
				pStatement.executeUpdate();
			}
			
			
			
			Calendar cal = Calendar.getInstance();                         // lokales Kalendersystem mit aktueller Zeit
			cal.set(2006, Calendar.MAY, 27, 20,50,0);
			
			statement.executeUpdate("DELETE from ctsim_level");

			Timestamp t = new Timestamp(cal.getTimeInMillis());
			statement.executeUpdate("INSERT INTO ctsim_level (id, parcours, gametime, sceduled) " +
					"values ("+1+",'parcours/testparcours0.xml',"+gametime+",'"+t+"')");
			cal.add(Calendar.SECOND,-gametime-savetime);
			
			t = new Timestamp(cal.getTimeInMillis());
			statement.executeUpdate("INSERT INTO ctsim_level (id, parcours, gametime, sceduled) " +
					"values ("+0+",'parcours/testparcours0.xml',300,'"+t+"')");
			cal.add(Calendar.SECOND,-gametime*2-savetime*2);
			
			
			for (int i=2; i< 16; i*=2){
				t = new Timestamp(cal.getTimeInMillis());
				statement.executeUpdate("INSERT INTO ctsim_level (id, parcours, gametime, sceduled) " +
						"values ("+i+",'parcours/testparcours0.xml',"+gametime+",'"+t+"')");
				cal.add(Calendar.SECOND,-gametime*i*2 - savetime*i*2);
			}			
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	
	//$$ Hier auch Vorrunde behandeln
	/**
	 * Gameplan anlegen $$
	 * Bereitet die eintraege in der Tabelle Games vor
	 * Achtung, loescht alle bisherigen eintraege
	 * @param maxLevel hoechstes Level, fuer das vorbereitet wird
	 * @throws SQLException
	 */
	private void prepareGames(int maxLevel) throws SQLException{
		Statement statement = databaseConnection.createStatement();

		// Cleanup
		statement.executeUpdate("DELETE FROM ctsim_game"); 
	
		// Fuer jedes Level Games anlegen
		for (int level=1; level <= maxLevel; level*=2){
			for (int game=1; game<= level; game++){
				statement.executeUpdate("INSERT INTO ctsim_game (level, game) " +
						"values ("+level+","+game+")"); 
			}
		}
		// per default stehen jetzt alle auf state == "not init"
		
		// Das Semifinale faellt aus dem Schema:
		statement.executeUpdate("INSERT INTO ctsim_game (level, game) " +
				"values ("+0+","+1+")"); 
		
		// Alle Level, bis auf das letzte werden auf jeden Fall gebraucht, daher state setzen
		statement.executeUpdate("UPDATE ctsim_game " +
				"SET state='"+STATE_WAIT_FOR_BOT1+"'"+
				"WHERE level<"+maxLevel);
	}
	
	/**
	 * Traegt einen Bots in ein Game ein
	 * @param bot Bot der platziert werden soll //$$ sollte ID sein
	 * @param level Das Level in das das Game eingefuegt wird
	 * @param game Die Nummer des Spieles im jeweiligen Level
	 * @throws SQLException 
	 * @throws GamePlanException Wenn das Spiel bereits voll ist 
	 */
	private void placeBot(String bot, int level, int game) throws SQLException, GamePlanException{
		System.out.println("Placing Bot Name= '"+bot+"' in Level "+level+" Spiel "+game);
		
		Statement statement = databaseConnection.createStatement();
		// Suche das Spiel heraus
		ResultSet rs = statement.executeQuery("SELECT * from ctsim_game WHERE level="+level+" AND game="+game);
		
		// Bots auslesen
		rs.next();
		int bot1= rs.getInt("bot1");
		int bot2= rs.getInt("bot2");
		
		rs = statement.executeQuery("SELECT id from ctsim_bot WHERE name='"+bot+"'");
		rs.next();
		int botId= rs.getInt("id");
		
		// Bots aktualisieren
		if (bot1== 0)
			statement.executeUpdate("UPDATE ctsim_game " +
					"SET bot1="+botId+","+
					"state='"+STATE_WAIT_FOR_BOT2 +"' "+
					"WHERE level="+level+" AND game="+game); 
		else {
			if (bot2== 0)
				statement.executeUpdate("UPDATE ctsim_game " +
						"SET bot2="+botId+","+
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
		ResultSet rs = databaseConnection.createStatement().executeQuery(
				"SELECT * " +
				"FROM ctsim_game " +
				"WHERE level ="+level+" AND game ="+game
				);
		
		rs.next();
		String winner = rs.getString("winner"); //$$ sollte ID sein
		
		String loser = rs.getString("bot1"); //$$ sollte ID sein
		if (loser.equals(winner))
			loser = rs.getString("bot2"); //$$ sollte ID sein
		
		if (winner != null)
			if (level > 2)	
				placeBot(winner,level/2,(game+1)/2); //$$ game: altes ID-Schema -> anpassen
			else {
				// Aus dem Halbfinale kommen die Gewinner ins Finale und die Verlierer ins Semifinale
				if (level ==2) {
					placeBot(winner,1,1);
					placeBot(loser,0,1);
				}
			}
			// und fuer level == 1 (Finale wurde gespielt) wird nichts gemacht; kein weiteres Propagieren
		else {
			// "kann nicht passieren"
			GamePlanException e = new GamePlanException("Fehler: Versuch einen nicht ermittelten Sieger zu propagieren: Level= "+level+" Game= "+game);
			ErrorHandler.error(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Gehoert zur Gameplan-Vorbereitung $$
	 * Setzt die Spielzeiten fuer alle Games in einem Level
	 * @param level Das Level, fuer das die Zeiten gesetzt werden sollen
	 * @throws SQLException
	 */
	private void schedule(int level) throws SQLException{
		Statement statement = databaseConnection.createStatement();

		// Nur Bots mit binary waehlen
		ResultSet rs = statement.executeQuery("SELECT * from ctsim_level WHERE id ="+level);
		
		rs.next();
		Timestamp baseTime = rs.getTimestamp("sceduled");
		int gameTime = rs.getInt("gametime");
		
		rs = statement.executeQuery(
				"SELECT * " +
				"FROM ctsim_game " +
				"WHERE level ="+level+" AND state !='"+STATE_NOT_INIT+"' " +
				"ORDER BY game"
				);
		
		while (rs.next()){
			statement.executeUpdate("UPDATE ctsim_game "+
					"SET sceduled='"+baseTime+"' "+
					"WHERE level ="+level+ " AND game = "+rs.getInt("game"));
			baseTime.setTime(baseTime.getTime()+gameTime*1000);
		}
	}
	
	/**
	 * Alle laufenden spiele Zuruecksetzen, damit sie neu begonnen werden
	 * $$ Testmethode
	 * @throws SQLException
	 */
	private void recover() throws SQLException{
		Statement statement = databaseConnection.createStatement();

		statement.executeUpdate(
				"UPDATE ctsim_game " +
				"SET state= '"+STATE_READY_TO_RUN+"' "+
				"WHERE state = '"+STATE_RUNNING+"'"
				);
	}
	
	//$$ Vorrundenkram hier rein
	private void createGamePlan() throws SQLException{
		try {
			Statement statement = databaseConnection.createStatement();

			// Nur Bots mit binary waehlen
			ResultSet rs = statement.executeQuery("SELECT * from ctsim_bot WHERE bin !=''");
			rs.last();
			
			// Anzahl bestimmen
			int bots = rs.getRow();
			System.out.println(bots+" Bots sind mit korrekten Binaries in der Datenbank");
			
			// Nur Bots mit binary waehlen
			rs = statement.executeQuery("SELECT * from ctsim_bot WHERE bin !=''");
			
			
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
				//placeBot(rs.getString("bot"),level,game);
				placeBot(rs.getString("name"),level,game);
				rs.next();
				placeBot(rs.getString("name"),level,game);
			}
			
			
			// Nun die Bots mit greencard ins naechste Level schubsen
			level /=2;
			int game=level;	// Wir muessen hinten anfangen zu platzieren
			// alle restlichen Bots
			while (rs.next()){
				placeBot(rs.getString("name"),level,game);
				if (rs.next())
					placeBot(rs.getString("name"),level,game);
				game--;
			}

			
			level*=2;
			// Zeitplaene setzen
			for (;level >=1;level/=2)
				schedule(level);
			// Finale und Semifinale sind sonderfaelle
			schedule(0);
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
				"FROM ctsim_game " +
				"WHERE state ='"+STATE_READY_TO_RUN+"'" +
				"ORDER BY sceduled " +
				"LIMIT 1");
		
		return rs;
	}
	
	
	
//	@Override
	protected void init() {
		
		//	Register the JDBC driver for MySQL.
	    try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		try {
			databaseConnection =DriverManager.getConnection(databaseUrl,"ctbot", "ctbot");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Connection: " + databaseConnection);

		try {
			recover();

			try {
				createTestData();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
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
		private static final long serialVersionUID = -9195564639022108463L;

		/**
		 * Konstruktor
		 * @param string
		 */
		public GamePlanException(String string) {
			super(string);
		}
	}
	
	//$$ koennte man vielleicht anders machen: Controller, World und Judge muessen sich alle gegenseitig kennen oder so; vielleicht ginge das weniger verstrickt
	public void setWorld(World world) {
		
		this.world = world;
		super.setWorld(world);
	}
	
	public boolean isAddingBotsAllowed() {
		
		return true;
	}
	
	public boolean isStartAllowed() {
		
		return true;
	}
	
	public void reinit() {
		
		super.reinit();
		this.first = true;
	}
}