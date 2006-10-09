package ctSim.view;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;

//$$ doc TimeLogger
public class TimeLogger implements View {
	private static final int intervalInSimMs = 5000; //TODO Koennte man in Config-Datei, auch zum Ein-/Ausschalten der ganzen Klasse
	private static final String minimalMsg =
		"Simzeit %d ms; Armbanduhrenzeit %tT.%<tL";
	private static final String normalMsg = minimalMsg +
			"; Verh\u00E4ltnis 1 : %.1f seit letzter " +
			"TimeLogger-Ausgabe; 1 : %.1f seit Simulationsbeginn";

	FmtLogger lg = FmtLogger.getLogger("ctSim.view.TimeLogger");
	// sicherstellen, dass beim ersten Schritt geloggt wird
	private long simTimeAtLastLog = - intervalInSimMs;
	private long realTimeAtLastLog = 0;
	//TODO Wann der Simulationsstart ist, wird ueber bloedsinnige Detektivarbeit herausgefunden. Besser: DefaultController macht auf den Views einen Aufruf "onSimulationBegins" oder so
	private long realTimeAtSimulationStart = Long.MIN_VALUE;

	public void onApplicationInited() {
		lg.fine("TimeLogger l\u00E4uft; Simzeit und Realzeit werden " +
				"w\u00E4hrend der Simulation periodisch ausgegeben");
	}

	public void onSimulationStep(long simTimeInMs) {
		if (realTimeAtSimulationStart == Long.MIN_VALUE)
			realTimeAtSimulationStart = System.currentTimeMillis();

    	if (simTimeInMs - simTimeAtLastLog < intervalInSimMs)
    		return;

		long now = System.currentTimeMillis();
		// Falls minimalMsg verwendet wird, wird das ueberfluessige Argument
		// ignoriert
		lg.fine((simTimeAtLastLog == - intervalInSimMs)
			? minimalMsg : normalMsg,
			simTimeInMs, System.currentTimeMillis(),
			(now - realTimeAtLastLog) /
			(float)(simTimeInMs - simTimeAtLastLog),
			(now - realTimeAtSimulationStart) / (float)simTimeInMs);

		simTimeAtLastLog = simTimeInMs;
		realTimeAtLastLog = now;
    }

	public void onSimulationFinished() {
		realTimeAtSimulationStart = Long.MIN_VALUE;
	}

	public void onBotAdded(@SuppressWarnings("unused") Bot bot) {
		// no-op
	}

	public void onBotRemoved(@SuppressWarnings("unused") Bot bot) {
		// no-op
	}

	public void onJudgeSet(@SuppressWarnings("unused") Judge j) {
		// no-op
	}

	public void onWorldOpened(@SuppressWarnings("unused") World newWorld) {
		// no-op
	}
}
