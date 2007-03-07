package ctSim.model.rules;

import ctSim.controller.DefaultController;
import ctSim.model.World;
import ctSim.view.gui.Debug;

//$$ Judges weg, deren einzige Bedeutung: Spielende (Simulationsende) veranlassen. Siehe Wiki
//$$ Problem: Wenn Judge Bot-hinzufuegen verbietet, wird Bot nicht korrekt de-initialisiert
/**
 * Abstrakte Superklasse f&uuml;r alle Judges, die pr&uuml;fen,
 * ob die Spielregeln eingehalten werden.
 *
 * @author bbe (bbe@heise.de)
 */
public abstract class Judge {
	/**
	 * Diese konfus benannte Variable gibt an, ob
	 * {@link #isSimulationFinished()} in irgendeinem der bisherigen
	 * Simulatorschritte <code>true</code> zur&uuml;ckgegeben hat. Mit der
	 * vorliegenden Implementierung von DefaultJudge bleibt die Variable immer
	 * auf <code>true</code> stehen. Mit der vorliegenden Implementierung von
	 * LabyrinthJudge wird die Variable genau dann <code>false</code>, wenn
	 * ein Bot das Ziel erreicht hat.
	 */
	private boolean start = true;

	/** Verweis auf den zuegehoerigen controller */
	protected DefaultController controller;

	//$$ startTime Wofuer?
	/** Welt-Zeit zu Beginn des Wettkampfes [ms] */
	private long startTime = 0;

	/** aktuelle Welt-Zeit [ms] */
	private long time = 0;

	/**
	 * Erzeuge neuen Judge
	 * @param ctrl Der DefaultController
	 */
	public Judge(DefaultController ctrl) {
		super();
		this.controller = ctrl;
	}

	/** Gibt an, ob es erlaubt ist, Bots zum Spiel hinzuzufuegen.
	 */
	public boolean isAddingBotsAllowed() {
		return this.time == this.startTime;
	}

	/**
	 * @return true, wenn die Bedingungen fuer einen Start erfuellt sind
	 */
	public boolean isStartingSimulationAllowed() {
		if(this.controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte.");
			return false;
		}
		return this.start;
	}

	public void setWorld(World w) {
    	time = startTime = w.getSimTimeInMs();
    }

	/** Stellt fest, ob die momentane Simulation beendet werden soll.
	 *
	 * @return <code>true</code>, falls die Simulation beendet werden soll
	 * &ndash; typischerweise, weil ein Bot das Ziel erreicht hat.
	 * <code>False</code>, falls die Simulation fortgesetzt werden soll. */
	public final boolean isSimulationFinished(long t) {
		this.time = t;
		boolean rv = isSimulationFinished();
		if (rv)
			start = false;
		return rv;
	}

	/** Hier kommen die eigentlichen Schiedsrichteraufgaben rein. */
	protected abstract boolean isSimulationFinished();

	public void reinit() {
		this.start = true;
		this.time = 0;
		this.startTime = 0;
	}

	/** Liefert die Simulatorzeit [ms] seit Beginn des aktuellen Spiels. */
	public long getTime() {
		return this.time - this.startTime;
	}
}
