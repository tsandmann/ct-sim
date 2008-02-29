package ctSim.controller;

import ctSim.model.bots.Bot;

/**
 * Bot-Receiver-Interface
 */
public interface BotReceiver {
	/**
	 * Handler fuer neuer Bot da
	 * @param b Bot
	 */
	public void onBotAppeared(Bot b);
}
