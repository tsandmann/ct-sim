package ctSim.controller;

import java.util.ArrayList;
import java.util.List;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.util.FmtLogger;
import ctSim.view.View;

public class BotManager {
	protected static FmtLogger lg = FmtLogger.getLogger(
		"ctSim.controller.BotManager");

	private static World world;
	private static View view;

	private static List<Bot> bots;
	private static List<Bot> botsToStart;
	private static List<Bot> botsToStop;

	static {
		init();
	}

	private static synchronized void init() {
		bots = new ArrayList<Bot>();
		botsToStart = new ArrayList<Bot>();
		botsToStop = new ArrayList<Bot>();
	}

	protected static synchronized void setWorld(World wrld) {
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
			removeBotNow(b);

			// TODO: in removeBot
			//world.removeAliveObstacle(b.getBot());
		}

		botsToStart = new ArrayList<Bot>();
		botsToStop  = new ArrayList<Bot>();
	}

	public static synchronized int getSize() {
		return bots.size();
	}

	protected static synchronized int getNewSize() {
		return bots.size() + botsToStart.size();
	}

	public static synchronized boolean addBot(Bot bot) {
		boolean rv = botsToStart.add(bot);

		world.addBot(bot);
		view.onBotAdded(bot);

		return rv;
	}

	public static synchronized boolean removeBotOnNextSimStep(Bot bot) {
		world.removeAliveObstacle(bot); //$$ Wieso das hier sein muss, weiss auch keiner
		return (bots.remove(bot) || botsToStart.remove(bot))
			&& botsToStop.add(bot);
	}

	private static synchronized void removeBotNow(Bot b) {
		lg.info("Stoppe Bot " + b);
		b.stop();
		world.removeAliveObstacle(b);
		view.onBotRemoved(b);
	}

	private static synchronized void deinit() {
		for(Bot b : botsToStart)
			removeBotNow(b);
		for(Bot b : bots)
			removeBotNow(b);
		for(Bot b : botsToStop)
			removeBotNow(b);
	}

	protected static synchronized void reinit() {
		lg.fine("Neuinitialisierung BotManager");
		deinit();
		init();
	}

	protected static synchronized void reset() {
		reinit();
		world = null;
	}
}
