package ctSim.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.view.BotInfo;
import ctSim.view.CtSimFrame;

public class BotManager {
	private static World world;
	private static CtSimFrame sim;
	
	private static List<BotInfo> bots = new ArrayList<BotInfo>();
	private static List<BotInfo> botsToStart = new ArrayList<BotInfo>();
	private static List<BotInfo> botsToStop = new ArrayList<BotInfo>();
	
	protected static void setWorld(World wrld) {
		
		world = wrld;
	}
	
	public static void setSim(CtSimFrame simFrame) {
		
		sim = simFrame;
	}
	
	protected static synchronized void startNstopBots() {
		
		for(BotInfo b : botsToStart) {
			b.getBot().start();
			System.out.println("Bot gestartet: "+b.getName()); //$NON-NLS-1$
			bots.add(b);
		}
		
		for(BotInfo b : botsToStop) {
			b.getBot().stop();
			System.out.println("Bot beendett: "+b.getName()); //$NON-NLS-1$
			
			// TODO: in removeBot
			//world.removeAliveObstacle(b.getBot());
		}
		
		botsToStart = new ArrayList<BotInfo>();
		botsToStop  = new ArrayList<BotInfo>();
	}
	
	protected static synchronized int getSize() {
		
		return bots.size();
	}
	
	protected static synchronized int getNewSize() {
		
		return bots.size() + botsToStart.size();
	}
	
	public static synchronized boolean addBot(BotInfo bot) {
		
		world.addBot(bot.getBot());
		sim.addBot(bot);
		
		return botsToStart.add(bot);
	}
	
	public static synchronized boolean removeBot(BotInfo bot) {
		
		world.removeAliveObstacle(bot.getBot());
		sim.removeBot(bot);
		
		return (bots.remove(bot) || botsToStart.remove(bot)) && botsToStop.add(bot);
	}
	
	private static synchronized void removeAllBots() {
		
		for(BotInfo b : botsToStart) {
			b.getBot().stop();
			world.removeAliveObstacle(b.getBot());
		}
		
		for(BotInfo b : bots) {
			b.getBot().stop();
			world.removeAliveObstacle(b.getBot());
		}
		
		for(BotInfo b : botsToStop) {
			b.getBot().stop();
			world.removeAliveObstacle(b.getBot());
		}
	}
	
	protected static synchronized void cleanup() {
		
		removeAllBots();
	}
	
	protected static synchronized void reinit() {
		
		removeAllBots();
		
		bots = new ArrayList<BotInfo>();
		botsToStart = new ArrayList<BotInfo>();
		botsToStop  = new ArrayList<BotInfo>();
	}
	
	protected static synchronized void reset() {
		
		reinit();
		world = null;
	}

	/** Liefert die Menge der dem BotManager bekannten Bots. 
	 * Methode ist nicht f&uuml;r performance-kritische Punkte gedacht, da 
	 * einiges Herumeiern stattfindet.
	 */
	//TODO Koennte schoener / schneller sein: wir laufen jedes Mal wieder durch 'bots' und machen jedes Mal wieder eine ArrayList
	public static Set<Bot> getBots() {
		Set<Bot> rv = new HashSet<Bot>();
		for (BotInfo b : bots)
			rv.add(b.getBot());
		return rv;
	}
}
