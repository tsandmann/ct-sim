package ctSim.model.rules;

import ctSim.controller.DefaultController;
import ctSim.model.World;
import ctSim.view.gui.Debug;

/** TODO Doc ist fuerchterlich falsch
 * Abstrakte Superklasse fuer alle Judges, die pruefent, 
 * ob die Spielregeln eingehalten werden
 * 
 * Ein Judge ist ein eigener Thread, der permanent laeuft. Er ist nicht 
 * synchron mit der Welt,
 * damit er diese pausieren und wieder freigeben kann.
 * Wie oft der Judge dran kommt, bestimmt die Routine delay(), die man 
 * ueberschreiben kann. 
 * Sie verwendet das Feld delayMs das die Wartezeit in Millisekunden enthaelt.
 * 
 * Zu implementieren ist eigentlich nur die Routine work()
 * Der judge sichert automatisch bei seinem ersten Aufruf die Start-Welt-Zeit 
 * (startTime) und stellt in jedem Aufruf von work
 * Die bisher abgelaufene Zeit zur Verfuegung (runTime)
 * 
 * Wenn aus irgendwelchen Gründen die Welt nich pausiert gestartet werden 
 * soll, kann der Judge seinen Zeit-Zähler 
 * jederzeit durch einen Aufruf von takeStartTime() zurücksetzen.
 * @author bbe (bbe@heise.de)
 *
 */
public abstract class Judge {
	private boolean start = true;
	
	/** Verweis auf den zuegehoerigen controller */
	protected DefaultController controller;

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

	public void setWorld(World w) {
		this.startTime = w.getSimulTime();
		this.time = this.startTime;
	}
	
	/** Gibt an, ob es erlaubt ist, Bots zum Spiel hinzuzufuegen.
	 */
	public boolean isAddAllowed(){
		return this.time == this.startTime;
	}
	
	/**
	 * @return true, wenn die Bedingungen fuer einen Start erfuellt sind
	 */
	public boolean isStartAllowed() {
		if(this.controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte."); //$NON-NLS-1$
			return false;
		}
		return this.start;
	}
	
	/**
	 * @return true, falls Aenderungen vorgenommen werden koennen
	 */
	public boolean isModifyingAllowed() {
		return false;
	}
	
	public final void update(long t) {
		this.time = t;
		if(!check()) {
			this.controller.pause();
			this.start = false;
		}
	}
	
	/** Hier kommen die eigentlichen Schiedsrichteraufgaben rein. */
	protected abstract boolean check();
	
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
