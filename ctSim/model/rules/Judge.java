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

package ctSim.model.rules;

import ctSim.controller.DefaultController;
import ctSim.model.World;
import ctSim.view.gui.Debug;

//$$ Problem: Wenn Judge Bot-hinzufügen verbietet, wird Bot nicht korrekt de-initialisiert
/**
 * Abstrakte Superklasse für alle Judges, die prüfen, ob die Spielregeln eingehalten werden.
 *
 * @author Benjamin Benz
 */
public abstract class Judge {
	/**
	 * Diese konfus benannte Variable gibt an, ob {@link #isSimulationFinished()} in irgendeinem der bisherigen
	 * Simulatorschritte <code>true</code> zurückgegeben hat. Mit der vorliegenden Implementierung von
	 * DefaultJudge bleibt die Variable immer auf <code>true</code> stehen. Mit der vorliegenden Implementierung
	 * von LabyrinthJudge wird die Variable genau dann <code>false</code>, wenn ein Bot das Ziel erreicht hat.
	 */
	private boolean start = true;

	/** Verweis auf den zugehörigen controller */
	protected DefaultController controller;

	/** Welt-Zeit zu Beginn des Wettkampfes [ms] */
	private long startTime = 0;

	/** aktuelle Welt-Zeit [ms] */
	private long time = 0;

	/**
	 * Erzeuge neuen Judge
	 * 
	 * @param ctrl	der DefaultController
	 */
	public Judge(DefaultController ctrl) {
		super();
		this.controller = ctrl;
	}

	/** 
	 * Gibt an, ob es erlaubt ist, Bots zum Spiel hinzuzufügen
	 * 
	 * @return true/false
	 */
	public boolean isAddingBotsAllowed() {
		return this.time == this.startTime;
	}

	/**
	 * @return true, wenn die Bedingungen für einen Start erfüllt sind
	 */
	public boolean isStartingSimulationAllowed() {
		if(this.controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte.");
			return false;
		}
		return this.start;
	}

	/**
	 * Setzt eine Welt
	 * 
	 * @param w	Welt
	 */
	public void setWorld(World w) {
    	time = startTime = w.getSimTimeInMs();
    }

	/** Stellt fest, ob die momentane Simulation beendet werden soll
	 * 
	 * @param t	Zeit
	 * @return <code>true</code>, falls die Simulation beendet werden soll; typischerweise, weil ein Bot das
	 * 			Ziel erreicht hat. <code>false</code>, falls die Simulation fortgesetzt werden soll.
	 */
	public final boolean isSimulationFinished(long t) {
		this.time = t;
		boolean rv = isSimulationFinished();
		if (rv)
			start = false;
		return rv;
	}

	/** 
	 * Hier kommen die eigentlichen Schiedsrichteraufgaben rein.
	 *  
	 * @return true / false
	 */
	protected abstract boolean isSimulationFinished();

	/** Neu-Initialisierung */
	public void reinit() {
		this.start = true;
		this.time = 0;
		this.startTime = 0;
	}

	/** 
	 * @return Liefert die Simulatorzeit [ms] seit Beginn des aktuellen Spiels. 
	 */
	public long getTime() {
		return this.time - this.startTime;
	}
}
