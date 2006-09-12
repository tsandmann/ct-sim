package ctSim.model.rules.contestJudge;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.vecmath.Vector3d;

import ctSim.ErrorHandler;
import ctSim.SimUtils;
import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.rules.Judge;
import ctSim.model.rules.contestJudge.TournamentPlanner.TournamentPlanException;
import ctSim.view.Debug;
import ctSim.view.sensors.RemoteControlGroupGUI;

// $$ doc gesamte Klasse

/** Judge, der einen Wettbwerb ausrichten kann */
public class ContestJudge extends Judge {
	/** Siehe {@link #state} */
	enum State {
		NOT_INITIALIZED,
		PRELIM_ROUND_BETWEEN_GAMES,
		PRELIM_ROUND_IN_GAME,
		PRELIM_ROUND_DONE,
		MAIN_ROUND_BETWEEN_GAMES,
		MAIN_ROUND_IN_GAME,
	}
	
	/** Status dieses Judge. Typischer Ablauf:
	 * <pre>
	 *    NOT_INITIALIZED
	 * -> PRELIM_ROUND_BETWEEN_GAMES -> PRELIM_ROUND_IN_GAME
	 * -> PRELIM_ROUND_BETWEEN_GAMES -> PRELIM_ROUND_IN_GAME
	 *    ... viele Spiele ...
	 * -> PRELIM_ROUND_BETWEEN_GAMES -> PRELIM_ROUND_IN_GAME
	 * -> PRELIM_ROUND_DONE
	 * -> MAIN_ROUND_BETWEEN_GAMES -> MAIN_ROUND_IN_GAME
	 * -> MAIN_ROUND_BETWEEN_GAMES -> MAIN_ROUND_IN_GAME
	 *    ... viele Spiele ...
	 * -> MAIN_ROUND_BETWEEN_GAMES -> MAIN_ROUND_IN_GAME -> (Schluss)</pre>
	 */
	private State state = State.NOT_INITIALIZED;

	/** Abstrahiert die Datenbank */
	private JudgeToDatabaseAdapter db;
	
	/** H&auml;lt die DB-Prim&auml;rschl&uuml;ssel der Bots */
	Map<Bot, Integer> botIds = new HashMap<Bot, Integer>();

	private TournamentPlanner planner;
	
	/** Referenz auf den Controller */
	private Controller ctrlr;
	/** Referenz auf die Welt*/
	private World world;

	Object botArrivalLock = new Object();
	Bot newlyArrivedBot = null;
	
	/**
	 * Konstruktor
	 * @throws SQLException Falls die Verbindung zur Datenbank nicht 
	 * hergestellt werden kann 
	 * @throws ClassNotFoundException 
	 */
	public ContestJudge(Controller ctrlr)
	throws SQLException, ClassNotFoundException {
		super(ctrlr); //$$ leider haben wir *und* die Vaterklasse eine Referenz auf den controller ... sieht redundant aus
		this.ctrlr = ctrlr;
		
		db = new JudgeToDatabaseAdapter();
		planner = new TournamentPlanner();
	}
	
	/** Wird von au&szlig;en einmal pro Simulatorschritt aufgerufen. Hier 
	 * verrichtet der Judge die haupts&auml;chliche Arbeit.
	 */
	@Override
	public boolean check() {
		try {
			switch (state) {
				case NOT_INITIALIZED:
	                planner.planPrelimRound();
	                state = State.PRELIM_ROUND_BETWEEN_GAMES;
					break;
					
				case PRELIM_ROUND_BETWEEN_GAMES: {
						ResultSet games = db.getReadyGames();
						if (games.next())
							sleepAndStartGame(games);
						else
							state = State.PRELIM_ROUND_DONE;
						break;
					}
				
				case PRELIM_ROUND_DONE:
					planner.planMainRound();
					state = State.MAIN_ROUND_BETWEEN_GAMES;
					break;
					
				case MAIN_ROUND_BETWEEN_GAMES: {
						ResultSet games = db.getReadyGames();
						if (games.next())
							sleepAndStartGame(games);
						else {
							System.out.println("Es gibt keine weiteren " +
								"Rennen mehr. Beende den Judge! -> das " +
								"Programm");
							System.exit(0);
						}
						break;
					}
					
				case PRELIM_ROUND_IN_GAME:
				case MAIN_ROUND_IN_GAME:
					log();
					checkIfSomeoneWon();
					break;
			}
		} catch (Exception e) {
			//$$ Error handling
			e.printStackTrace();
		}
		
		return true;
	}
	
	/**
	 * 
	 * @param game
	 * @throws SQLException
	 * @throws IOException 
	 */
	private void sleepAndStartGame(ResultSet game)
	throws SQLException, IOException {
		Timestamp scheduled = game.getTimestamp("scheduled");
		// lokales Kalendersystem mit aktueller Zeit
		Calendar now = Calendar.getInstance();
		
		long timeTilGameMillisec = scheduled.getTime() - now.getTimeInMillis();
		if (timeTilGameMillisec > 0){
			try {
				System.out.println(now.getTime()+": Warte "+
					timeTilGameMillisec+" ms auf naechsten Wettkampf ("+
					scheduled+")");
				Thread.sleep(timeTilGameMillisec);
			} catch (InterruptedException e) {
				//$$ InterruptedExcp? was hier machen?
			}
		} else {
			// Zustand: wir wollen ein Spiel starten
			System.out.println(now.getTime()+": Starte Rennen. Level= "+
					game.getInt("level")+" Game= "+game.getInt("game")+
					" Scheduled= "+game.getTimestamp("scheduled"));
			startGame(game);
		}
    }

	/**
	 * Annahme: Keiner ausser uns startet Bots. Wenn jemand gleichzeitig 
	 * @param b
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	private Bot executeBot(Blob b) throws SQLException, IOException {
		// Blob in Datei
		String fn = "tmp.exe"; //$$ hardcoded
		InputStream in = b.getBinaryStream();
		FileOutputStream out = new FileOutputStream(fn);
		while (in.available() > 0)
			out.write(in.read());
		out.close();
		// Datei ausfuehren
		ctrlr.invokeBot(fn);
		// Warten bis uns der Controller auf den neuen Bot hinweist
		try {
			synchronized (botArrivalLock) {
				// Schutz vor spurious wakeups (siehe Java-API-Doku zu wait()) 
				while (newlyArrivedBot == null)
					botArrivalLock.wait();
			}
			Bot rv = newlyArrivedBot;
			newlyArrivedBot = null;
			return rv;
		} catch (InterruptedException e) {
			// $$ was soll man hier machen? return was?
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void newBotArrived(Bot bot) {
		synchronized (botArrivalLock) {
			newlyArrivedBot = bot;
			botArrivalLock.notifyAll();
        }
	}

	/**
	 * Startet ein Spiel
	 * @param game Verweis auf das Spiel
	 * @param isMainGame 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	private void startGame(ResultSet game) throws SQLException, IOException {
		int gameId = game.getInt("game");
		int levelId= game.getInt("level");
		db.setGameRunning(levelId, gameId);
		
		ctrlr.openWorldFromXmlString(db.getParcours(levelId));

		// Bots laden
		botIds.put(executeBot(db.getBot1Binary()), db.getBot1Id());
		// Wenn kein Vorrundenspiel, auch zweiten Spieler holen
		if (db.isCurrentGameMainRound())  
			botIds.put(executeBot(db.getBot2Binary()), db.getBot2Id());
		
		// Bots starten
		//$$ sollte nicht hier sein
		for(Bot b : ctrlr.getBots()) {
			if (b instanceof CtBotSimTcp) //$$ sollte ctBot sein, wo sendRCCommand() auch hingehoert
				((CtBotSimTcp)b).sendRCCommand(
					RemoteControlGroupGUI.RC5_CODE_5);
		}
		
		ctrlr.unpause();
	}
	
	private void checkIfSomeoneWon() {
		for(Bot b : ctrlr.getBots()) {
			if(this.world.finishReached(new Vector3d(b.getPosition()))) {
				// Zustand: wir haben einen Gewinner
				Debug.out.println("Zieleinlauf \""+b.getName()+"\" nach "+ 
						SimUtils.millis2time(this.getTime()));
				kissArrivingBot(b, getTime());
			}
		}
	}
	
	/**
	 * 
	 * @param bot
	 * @param time	Simulatorzeit [ms] seit Start des Spiels 
	 */
	private void kissArrivingBot(Bot bot, long time) {
		System.out.println("Bot "+bot.getName()+" hat das Ziel nach "+
				time+" ms erreicht!");
		if (db.isCurrentGameMainRound())
			state = State.MAIN_ROUND_BETWEEN_GAMES;
		else
			state = State.PRELIM_ROUND_BETWEEN_GAMES;
		
		try {
			// Spiel beenden
			db.setWinner(botIds.get(bot), time);
		} catch (SQLException e) {
			ErrorHandler.error("Probleme beim Sichern der Zieldaten "+e);
			e.printStackTrace();
		} catch (TournamentPlanException e) {
			ErrorHandler.error("Probleme beim Fortschreiben des Spielplans "+e);
			e.printStackTrace();			
		}
		
		ctrlr.pause(); //$$ ist das richtig?
		// Alle Bots entfernen
		//$$ wieso macht sowas nicht der Controller?
		for (Bot b : this.ctrlr.getBots())
			b.die();
	}

	private void log() {
		Iterator<Bot> it = botIds.keySet().iterator();
		Bot b1 = it.next();
		Bot b2 = it.next();
		db.log(b1, b2, getTime());
	}

	@Override
	public void setWorld(World world) {
		this.world = world;
		super.setWorld(world);
	}
	
	@Override
	public boolean isAddAllowed() {
		return true;
	}
	
	@Override
	public boolean isStartAllowed() {
		return true;
	}
	
	@Override
	public void reinit() {
		super.reinit();
	}
}
