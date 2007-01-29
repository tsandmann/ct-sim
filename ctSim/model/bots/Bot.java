package ctSim.model.bots;

//$$ doc
public interface Bot {
	public void accept(BotBuisitor buisitor);
	public String toString();
	public String getDescription();
	public int getInstanceNumber();
	public void dispose();
	public void addDisposeListener(Runnable runsWhenAObstDisposes);
}
