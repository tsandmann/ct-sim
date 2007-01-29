package ctSim.model;

import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.bots.ctbot.MasterSimulator;

//$$ doc
public abstract class SimulatorFactory {
	private SimulatorFactory() {
		// No-op
	}

	public static Runnable createFor(World world, ThreeDBot botWrapper,
	SimulatedBot bot) {
		if (bot instanceof CtBotSimTcp)
			return new MasterSimulator(world, botWrapper);

		if (bot instanceof CtBotSimTest) {
			return new Runnable() {
				public void run() {

				}
			};
		}

		return new Runnable() {
			public void run() {
				// No-op
			}
		};
	}
}
