package ctSim.model.rules;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.view.Debug;
//import ctSim.view.JudgePanel;

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
	
	// private World world;
	
	private boolean start = true;
	
	/** Verweis auf den zuegehoerigen controller */
	private Controller controller;
	
	/** Soll der Thread noch laufen ?*/
//	private boolean run = true;

	/**
	 * Welt-Zeit zu Beginn des Wettkampfes [ms]
	 */
	private long startTime = 0;

	/**
	 * aktuelle Welt-Zeit [ms]
	 */
	private long time = 0;
	
	/**
	 * bisher verstrichene Zeit [ms]
	 */
//	private long runTime;

	/** 
	 * Wartezeit zwischen zwei Aufrufen des Judges [ms]	
	 */
//	private int delay = 100; 
	
	/** Die Anzeige des Judges */
//	private JudgePanel panel;
	
	/**
	 * Erzeuge neuen Judge
	 * @param ctrl Der Controller
	 * @param w Die Welt
	 */
	public Judge(Controller ctrl) {
		super();
		
		this.controller = ctrl;
		//this.world = w;
//		this.startTime = w.getSimulTime();
//		this.time = this.startTime;
//		panel = new JudgePanel(this);
//		setName(this.getClass().getName());
	}

	/**
	 * Nimmt die Startzeit
	 */
//	public void takeStartTime(){
//		World world =controller.getWorld();
//		startTime = world.getSimulTimeUnblocking();
//		runTime=0;
//	}
	
	/** 
	 * Wie oft soll der Judge das System pruefen?
	 * Diese Routine muss den Judge-Thread in einen Schlaf-Modus versetzen.
	 * Sie kann von Unterklassen ueberschrieben werden, oder sie setzen einfach 
	 * das Feld delay auf einen anderen Wert!  
	 * @return  
	 */
//	protected void delay() throws InterruptedException{
//		sleep(delay);
//	}
	
	public void setWorld(World w) {
		
		this.startTime = w.getSimulTime();
		this.time = this.startTime;
	}
	
	/** Gibt an, ob es erlaubt ist, Bots zum Spiel hinzuzufuegen.
	 * Der Controller fragt hier z.B. nach
	 */
	public boolean isAddingBotsAllowed(){
		
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
	
	/** hier kommen die eigentlichen Schiedsrichteraufgaben rein */
//	abstract protected void work();
	protected abstract boolean check();
	
	/**
	 * Erledigt die Arbeit
	 */
//	@Override
//	public void run() {
//		init();
//		takeStartTime();
//		while (run == true) {
//			try {
//				delay();
//			}catch (InterruptedException ex){
//				ErrorHandler.error("Judge "+this.getClass().getName()+" wurde unterbrochen");
//				die();
//			}
//
//			time=controller.getWorld().getSimulTimeUnblocking();
//			runTime = time - startTime;
//			panel.setRunTime(getRunTime());
//			
//			work();
//		}
//	}
	
	/** Hier kommt alles rein, was vor dem Start ausgefuehrt werden muss*/
//	protected abstract void init();
	
	public void reinit() {
		
		this.start = true;
		this.time = 0;
		this.startTime = 0;
	}


	/**
	 * Versetzt die Welt in einen Dornroeschenschlaf, oder erweckt sie
	 * @param sleep true, wenn die Welt einschlafen soll, false wenn sie aufwachen soll
	 */
//	public void suspendWorld(boolean sleep){
//		controller.getControlFrame().setHaveABreak(sleep);
//	}
	
	
	/**
	 * Beendet den Judge
	 */
//	public void die() {
//		run = false;
//		this.interrupt();
//	}


	/**
	 * Laufzeit seit dem setzen der StartTime
	 * @return die Zeit
	 */
//	public long getRunTime() {
//		return runTime;
//	}

	/**
	 * Liefert die Startzeit des Wettlampfes zurueck
	 * @return die Zeit
	 */
	public long getStartTime() {
		return this.startTime;
	}

	/**
	 * Liefert die aktuelle Zeit zurueck
	 * @return die Zeit
	 */
	public long getTime() {
		return this.time-this.startTime;
	}

	/** Der zugehoerige Controller 
	 *  @return der Controller 
	 */
//	public Controller getController() {
//		return controller;
//	}


	/** Lege die Geschwindigkeit des Judges fest 
	 * @param delay verzoegerung
	 */ 
//	public void setDelay(int delay) {
//		this.delay = delay;
//	}

	/**
	 * Liefert das zugehoerige Panel
	 * @return das Panel
	 */
//	public JudgePanel getPanel() {
//		return panel;
//	}

	/**
	 * Setze den Controller
	 * @param controller
	 */
//	public void setController(Controller controller) {
//		this.controller = controller;
//	}
}

