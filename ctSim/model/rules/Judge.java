package ctSim.model.rules;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.view.Debug;

/**
 * Abstrakte Superklasse fuer alle Judges, die pruefent, 
 * ob die Spielregeln eingehalten werden
 * 
 * Ein Judge ist ein eigener Thread, der permanent laeuft. Er ist nicht synchron mit der Welt,
 * Damit er diese pausieren und wieder freigeben kann.
 * Wie oft der Judge dran kommt, bestimmt die Routine delay() die man ueberschreiben kann. 
 * Sie verwendet das Feld delayMs das die Wartezeit in Millisekunden enthaelt.
 * 
 * Zu implementieren ist eigentlich nur die Routine work()
 * Der judge sichert automatisch bei seinem ersten Aufruf die Start-Welt-Zeit (startTime) und stellt in jedem Aufruf von work
 * Die bisher abgelaufene Zeit zur Verfuegung (runTime)
 * 
 * Wenn aus irgendwelchen Gründen die Welt nich pausiert gestartet werden soll, kann der Judge seinen Zeit-Zähler 
 * jederzeit durch einen Aufruf von takeStartTime() zurücksetzen.
 * @author bbe (bbe@heise.de)
 *
 */
public abstract class Judge {
	private boolean start = true;
	
	/** Verweis auf den zuegehoerigen controller */
	private Controller controller;

	/**
	 * Welt-Zeit zu Beginn des Wettkampfes [ms]
	 */
	private long startTime = 0;

	/**
	 * aktuelle Welt-Zeit [ms]
	 */
	private long time = 0;
	
	/**
	 * Erzeuge neuen Judge
	 * @param ctrl Der Controller
	 */
	public Judge(Controller ctrl) {
		super();
		
		this.controller = ctrl;
	}

	public void setWorld(World w) {
		
		this.startTime = w.getSimulTime();
		this.time = this.startTime;
	}
	
	/** Gibt an, ob es erlaubt ist, Bots zum Spiel hinzuzufuegen.
	 * Der Controller fragt hier z.B. nach
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
	
	/**
	 * @param t
	 */
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
		return this.time-this.startTime;
	}

	/** <p>Vom Controller aufzurufen, um mitzuteilen, dass ein neuer Bot 
	 * angekommen ist.</p>
	 * 
	 * <p>Tut nichts in der vorliegenden Implementierung. Judges, die die 
	 * Information ben&ouml;tigen, &uuml;berschreiben die Methode.</p>
	 * 
	 * <p>Die Methode ist erforderlich, da in der gegenw&auml;rtigen 
	 * (diskutablen) Architektur der ContestJudge veranlasst, dass ein 
	 * externer bin&auml;rer Bot geladen wird, und dann warten muss, bis 
	 * der Bot sich initialisiert und mit dem Controller verbunden hat. 
	 * Diese Methode dient dazu, dem ContestJudge mitzuteilen, wann der Bot
	 * bereitsteht und der Judge aufh&ouml;ren kann, auf ihn zu warten.</p> */
	public void newBotArrived(@SuppressWarnings("unused") Bot bot) {
		// no-op im Normalfall; die Judges, die die Methode brauchen, 
		// ueberschreiben sie
	}
}
