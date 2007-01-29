package ctSim.controller;

//$$ doc
public interface BotBarrier {
	public void awaitNextSimStep() throws InterruptedException;
}
