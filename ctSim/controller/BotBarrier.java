package ctSim.controller;

/**
 * Bot-Barrier Interface
 */
public interface BotBarrier {
	/**
	 * Wartet auf die naechste Runde
	 * @throws InterruptedException
	 */
	public void awaitNextSimStep() throws InterruptedException;
}
