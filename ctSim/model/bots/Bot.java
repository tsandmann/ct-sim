package ctSim.model.bots;

/**
 * Interface fuer alle Bots
 */
public interface Bot {
	/**
	 * @param buisitor BotBuisitor
	 */
	public void accept(BotBuisitor buisitor);
	
	/**
	 * @return Name des Bots
	 */
	public String toString();
	
	/**
	 * @return Beschreibung des Bots
	 */
	public String getDescription();
	
	/**
	 * @return Nummer des Bots
	 */
	public int getInstanceNumber();
	
	/**
	 * View des Bots updaten
	 * @throws InterruptedException
	 */
	public void updateView() throws InterruptedException;
	
	/**
	 * Bot zerstoeren
	 */
	public void dispose();
	
	/**
	 * Fuegt einen Handler hinzu, der beim Ableben eines Bots gestartet wird
	 * @param runsWhenAObstDisposes
	 */
	public void addDisposeListener(Runnable runsWhenAObstDisposes);
}
