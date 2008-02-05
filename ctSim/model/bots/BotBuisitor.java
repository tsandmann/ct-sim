package ctSim.model.bots;

/**
 * Interface fuer alle BotBuisitors
 */
public interface BotBuisitor {
	/**
	 * @param o		das Objekt
	 * @param bot	der Bot
	 */
	public void visit(Object o, Bot bot);
}
