package ctSim.view;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;

/**
 *  Zeit-Logger
 */
public class TimeLogger implements View {
	/** Intervall */
	private static final int intervalInSimMs = 10000;
	/** verkuerzte Ausgabe */
	private static final String minimalMsg =
		"Simzeit %d ms; Armbanduhrenzeit %tT.%<tL";
	/** komplette Ausgabe */
	private static final String normalMsg = minimalMsg +
			"; Verh\u00E4ltnis 1 : %.1f seit letzter " +
			"TimeLogger-Ausgabe; 1 : %.1f seit Simulationsbeginn";

	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.TimeLogger");
	// sicherstellen, dass beim ersten Schritt geloggt wird
	/** letzte Sim-Zeit */
	private long simTimeAtLastLog;
	/** letzte Real-Zeit */
	private long realTimeAtLastLog;
	/** Real-Zeit beim Start*/
	private long realTimeAtSimulationStart;

	{ // instance initializer
		initVariables();
	}

	/**
	 * Initialisierung
	 */
	private void initVariables() {
		realTimeAtSimulationStart = Long.MIN_VALUE;
		simTimeAtLastLog = - intervalInSimMs;
		realTimeAtLastLog = 0;
	}

	/**
	 * @see ctSim.view.View#onApplicationInited()
	 */
	public void onApplicationInited() {
		lg.info("TimeLogger l\u00E4uft; Simzeit und Realzeit werden " +
				"w\u00E4hrend der Simulation periodisch ausgegeben");
	}

	/**
	 * @see ctSim.view.View#onSimulationStep(long)
	 */
	public void onSimulationStep(long simTimeInMs) {
		if (realTimeAtSimulationStart == Long.MIN_VALUE)
			realTimeAtSimulationStart = System.currentTimeMillis();

    	if (simTimeInMs - simTimeAtLastLog < intervalInSimMs)
    		return;

		long now = System.currentTimeMillis();
		// Falls minimalMsg verwendet wird, wird das ueberfluessige Argument
		// ignoriert
		lg.info((simTimeAtLastLog == - intervalInSimMs)
			? minimalMsg : normalMsg,
			simTimeInMs, System.currentTimeMillis(),
			(now - realTimeAtLastLog) /
			(float)(simTimeInMs - simTimeAtLastLog),
			(now - realTimeAtSimulationStart) / (float)simTimeInMs);

		simTimeAtLastLog = simTimeInMs;
		realTimeAtLastLog = now;
    }

	/**
	 * @see ctSim.view.View#onSimulationFinished()
	 */
	public void onSimulationFinished() {
		initVariables();
	}

	/**
	 * @see ctSim.view.View#onBotAdded(ctSim.model.bots.Bot)
	 */
	public void onBotAdded(Bot bot) {
		// no-op
	}

	/**
	 * @see ctSim.view.View#onJudgeSet(ctSim.model.rules.Judge)
	 */
	public void onJudgeSet(Judge j) {
		// no-op
	}

	/**
	 * @see ctSim.view.View#onWorldOpened(ctSim.model.World)
	 */
	public void onWorldOpened(World newWorld) {
		// no-op
	}

	/**
	 * @see ctSim.view.View#onResetAllBots()
	 */
	public void onResetAllBots() {
		// NOP
	}
}
