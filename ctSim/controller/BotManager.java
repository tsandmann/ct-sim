package ctSim.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.view.View;

public class BotManager {
	private static World world;
	private static View view;

	private static List<Bot> bots = new ArrayList<Bot>();
	private static List<Bot> botsToStart = new ArrayList<Bot>();
	private static List<Bot> botsToStop = new ArrayList<Bot>();

	protected static void setWorld(World wrld) {

		world = wrld;
	}

	public static void setView(View v) {
		view = v;
	}

	protected static synchronized void startNstopBots() {

		for(Bot b : botsToStart) {
			b.start();
			System.out.println("Bot gestartet: "+b.getName()); //$NON-NLS-1$
			bots.add(b);
		}

		for(Bot b : botsToStop) {
			b.stop();
			System.out.println("Bot beendet: "+b.getName()); //$NON-NLS-1$

			// TODO: in removeBot
			//world.removeAliveObstacle(b.getBot());
		}

		botsToStart = new ArrayList<Bot>();
		botsToStop  = new ArrayList<Bot>();
	}

	protected static synchronized int getSize() {

		return bots.size();
	}

	protected static synchronized int getNewSize() {

		return bots.size() + botsToStart.size();
	}

	public static synchronized boolean addBot(Bot bot) {
		boolean rv = botsToStart.add(bot);

		world.addBot(bot);
		view.addBot(bot);

		return rv;
	}

	public static synchronized boolean removeBot(Bot bot) {

		world.removeAliveObstacle(bot);
		view.removeBot(bot);

		return (bots.remove(bot) || botsToStart.remove(bot)) && botsToStop.add(bot);
	}

	private static synchronized void removeAllBots() {

		for(Bot b : botsToStart) {
			b.stop();
			world.removeAliveObstacle(b);
		}

		for(Bot b : bots) {
			b.stop();
			world.removeAliveObstacle(b);
		}

		for(Bot b : botsToStop) {
			b.stop();
			world.removeAliveObstacle(b);
		}
	}

	protected static synchronized void cleanup() {

		removeAllBots();
	}

	protected static synchronized void reinit() {

		removeAllBots();

		bots = new ArrayList<Bot>();
		botsToStart = new ArrayList<Bot>();
		botsToStop  = new ArrayList<Bot>();
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
		for (Bot b : bots)
			rv.add(b);
		return rv;
	}
}
