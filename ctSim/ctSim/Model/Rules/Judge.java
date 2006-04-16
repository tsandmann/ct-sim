/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
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
package ctSim.Model.Rules;

import ctSim.ErrorHandler;
import ctSim.Controller.Controller;
import ctSim.Model.World;
import ctSim.View.JudgePanel;

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
public abstract class Judge extends Thread {
	/** Verweis auf den zuegehoerigen controller */
	private Controller controller;
	
	/** Soll der Thread noch laufen ?*/
	private boolean run = true;

	/**
	 * Welt-Zeit zu Beginn des Wettkampfes [ms]
	 */
	private long startTime;

	/**
	 * aktuelle Welt-Zeit [ms]
	 */
	private long time;
	
	/**
	 * bisher verstrichene Zeit [ms]
	 */
	private long runTime;

	/** 
	 * Wartezeit zwischen zwei Aufrufen des Judges [ms]	
	 */
	private int delay = 100; 
	
	/** Die Anzeige des Judges */
	private JudgePanel panel;
	
	/**
	 * Erzeuge neuen Judge
	 * @param controller Verweis auf den zugehoerigen Controller
	 */
	public Judge() {
		super();
		panel = new JudgePanel(this);
		setName(this.getClass().getName());
	}	
	

	/**
	 * Nimmt die Startzeit
	 */
	public void takeStartTime(){
		World world =controller.getWorld();
		startTime = world.getSimulTimeUnblocking();
		runTime=0;
	}
	
	/** 
	 * Wie oft soll der Judge das System prüfen?
	 * Diese Routine muss den Judge-Thread in einen Schlaf-Modus versetzen.
	 * Sie kann von Unterklassen überschrieben werden, oder sie setzen einfach 
	 * das Feld delay auf einen anderen Wert!  
	 */
	protected void delay() throws InterruptedException{
		sleep(delay);
	}
	
	/** hier kommen die eigentlichen Schiedsrichteraufgaben rein */
	abstract protected void work();
	
	/**
	 * Erledigt die Arbeit
	 */
	public void run() {
		init();
		takeStartTime();
		while (run == true) {
			try {
				delay();
			}catch (InterruptedException ex){
				ErrorHandler.error("Judge "+this.getClass().getName()+" wurde unterbrochen");
				die();
			}

			time=controller.getWorld().getSimulTimeUnblocking();
			runTime = time - startTime;
			
			work();
		}
	}
	
	/** Hier kommt alles rein, was vor dem Start ausgefuehrt werden muss*/
	abstract protected void init();


	/**
	 * Versetzt die Welt in einen Dornroeschenschlaf, oder erweckt sie
	 * @param sleep true, wenn die Welt einschlafen soll, false wenn sie aufwachen soll
	 */
	public void suspendWorld(boolean sleep){
		controller.getControlFrame().setHaveABreak(sleep);
	}
	
	
	/**
	 * Beendet den Judge
	 */
	public void die() {
		run = false;
		this.interrupt();
	}


	/**
	 * Laufzeit seit dem setzen der StartTime
	 * @return
	 */
	public long getRunTime() {
		return runTime;
	}

	/**
	 * Liefert die Startzeit des Wettlampfes zurueck
	 * @return
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * Liefert die aktuelle Zeit zurueck
	 * @return
	 */
	public long getTime() {
		return time;
	}

	/** Der zugehoerige Controller */
	public Controller getController() {
		return controller;
	}


	/** Lege die Geschwindigkeit des Judges fest */ 
	public void setDelay(int delay) {
		this.delay = delay;
	}


	public JudgePanel getPanel() {
		return panel;
	}


	public void setController(Controller controller) {
		this.controller = controller;
	}
}
