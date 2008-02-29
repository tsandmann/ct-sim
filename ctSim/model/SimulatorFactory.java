package ctSim.model;

import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.bots.ctbot.MasterSimulator;

/**
 * Erzeugt einen Simulator fuer einen Bot, der zu einer Welt gehoert und eine 3D-Darstellung besitzt 
 */
public abstract class SimulatorFactory {
	/**
	 * SimulatorFactory
	 */
	private SimulatorFactory() {
		// No-op
	}

	/**
	 * Erstellt einen neuen Simulator
	 * @param world			Welt, zu der der Bot gehoert
	 * @param botWrapper	3D-Darstellung des Bots
	 * @param bot			Bot-Instanz, die simuliert werden soll
	 * @return				Simulator fuer Bot bot
	 */
	public static Runnable createFor(World world, ThreeDBot botWrapper,
	SimulatedBot bot) {
		/* ct-Bot per TCP-Verbindung (C-Binary) */
		if (bot instanceof CtBotSimTcp) {
			return new MasterSimulator(world, botWrapper);
		}
		
		/* Testbot (Java-Klasse) */
		if (bot instanceof CtBotSimTest) {
			return new MasterSimulator(world, botWrapper);
		}

		return new Runnable() {
			public void run() {
				// No-op
			}
		};
	}
}
