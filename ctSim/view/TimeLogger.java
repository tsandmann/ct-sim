/*
 * c't-Sim - Robotersimulator für den c't-Bot
 *
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 */

package ctSim.view;

import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;
import ctSim.util.FmtLogger;

/** Zeit-Logger */
public class TimeLogger implements View {
	/** Intervall */
	private static final int intervalInSimMs = 10000;
	/** verkürzte Ausgabe */
	private static final String minimalMsg = "Simzeit %d ms; Armbanduhrenzeit %tT.%<tL";
	/** komplette Ausgabe */
	private static final String normalMsg = minimalMsg + "; " +
			"Verhältnis 1 : %.3f seit letzter TimeLogger-Ausgabe; " +
			"Verhältnis 1 : %.3f seit Simulationsbeginn";

	/** Logger */
	// sicherstellen, dass beim ersten Schritt geloggt wird:
	FmtLogger lg = FmtLogger.getLogger("ctSim.view.TimeLogger");
	/** letzte Sim-Zeit */
	private long simTimeAtLastLog;
	/** letzte Real-Zeit */
	private long realTimeAtLastLog;
	/** Real-Zeit beim Start*/
	private long realTimeAtSimulationStart;

	{	// instance initializer
		initVariables();
	}

	/** Initialisierung */
	private void initVariables() {
		realTimeAtSimulationStart = Long.MIN_VALUE;
		simTimeAtLastLog = - intervalInSimMs;
		realTimeAtLastLog = 0;
	}

	/**
	 * @see ctSim.view.View#onApplicationInited()
	 */
	public void onApplicationInited() {
		lg.info("TimeLogger läuft; Simzeit und Realzeit werden während der Simulation periodisch ausgegeben");
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
		// falls minimalMsg verwendet wird, wird das überflüssige Argument ignoriert
		lg.info((simTimeAtLastLog == - intervalInSimMs)
			? minimalMsg : normalMsg,
			simTimeInMs, System.currentTimeMillis(),
			(now - realTimeAtLastLog) /
			(double)(simTimeInMs - simTimeAtLastLog),
			(now - realTimeAtSimulationStart) / (double)simTimeInMs);

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
		// No-op
	}

	/**
	 * @see ctSim.view.View#onJudgeSet(ctSim.model.rules.Judge)
	 */
	public void onJudgeSet(Judge j) {
		// No-op
	}

	/**
	 * @see ctSim.view.View#onWorldOpened(ctSim.model.World)
	 */
	public void onWorldOpened(World newWorld) {
		// No-op
	}

	/**
	 * @see ctSim.view.View#onResetAllBots()
	 */
	public void onResetAllBots() {
		// No-op
	}
}
