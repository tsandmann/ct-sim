package ctSim.controller;

import java.net.ProtocolException;

import ctSim.model.bots.Bot;

/**
 * Bot-Receiver-Interface
 */
public interface BotReceiver {
	/**
	 * Handler fuer neuer Bot da
	 * @param b Bot
	 * @throws ProtocolException 
	 */
	public void onBotAppeared(Bot b) throws ProtocolException;
}
