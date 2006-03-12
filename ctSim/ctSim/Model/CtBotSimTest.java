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

package ctSim.Model;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.Controller.Controller;

/**
 * Klasse fuer Testbots, die ausschlie�lich zum Test des Simulators dienen; Bots
 * diesen Typs brauchen keine TCP-Verbindung.
 * 
 * @author pek (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * 
 */
public class CtBotSimTest extends CtBotSim {

	private short ll;
	private short rr;

	
	/**
	 * Erzeugt einen neuen Testbot
	 * 
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 */
	public CtBotSimTest(Controller controller, Point3f pos, Vector3f head) {
		super(controller, pos, head);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#init()
	 */
	protected void init() {
	}

	/**
	 * Diese Methode enthaelt eine einfache Beispielroutine fuer eine
	 * Robotersteuerung. Der Bot faehrt moeglichst schnell, solange der 
	 * Weg frei ist, wird langsamer, wenn er in die Naehe einer Wand kommt 
	 * und dreht sich langsam von der Wand weg. Kommt die Wand zu nah oder
	 * wird ein Abgrund vor dem Bot gemeldet, dreht er
	 * "auf dem Teller", bis der Weg wieder frei ist. <br/>
	 * Die Steuerung vermeidet in den meisten Faellen die Kollision mit der 
	 * Wand, fuehrt aber gelegentlich dazu, dass der Bot in einer Ecke haengen bleibt. <br/>
	 * Dieser Code laesst sich NICHT auf einen echten c't-Bot
	 * uebertragen, da diese kein Java unterstuetzt. <br/>
	 * Diese einfache Implementierung des c't-Bot reagiert NICHT auf Kommandos der Fernbedienung
	 * 
	 * @see ctSim.Model.Bot#work()
	 */
	public void work() {
		
		ll = rr = 100;

		int irL = this.getSensIrL();
		int irR = this.getSensIrR();

		// Ansteuerung fuer die Motoren in Abhaengigkeit vom Input
		// der IR-Abstandssensoren, welche die Entfernung in mm
		// zum naechsten Hindernis in Blickrichtung zurueckgeben

		// Solange die Wand weit weg ist, wird Stoff gegeben:
		if (irL >= 500) {
			ll = 255;
		}
		if (irR >= 500) {
			rr = 255;
		}

		// Vorsicht, die Wand kommt naeher:
		// Jetzt den Motor auf der Seite, die weiter entfernt ist,
		// langsamer laufen lassen als den auf der anderen Seite
		// - dann bewegt sich der Bot selbst
		// bei Wandkollisionen noch etwas und kommt eventuell
		// wieder frei:
		if (irL < 500 && irL >= 200) {
			if (irL <= irR)
				ll = 70;
			else
				ll = 50;
		}
		if (irR < 500 && irR >= 200) {
			if (irL > irR)
				rr = 70;
			else
				rr = 50;
		}

		// Ist ein Absturz zu bef�rchten?
		int borderL = getSensBorderL();
		int borderR = getSensBorderR();
		if (borderL > borderR) {
			ll = 100;
			rr = -100;
		} else if (borderL < borderR) {
			ll = -100;
			rr = 100;
		}
		
		// Kollision oder Abgrund droht: Auf dem Teller rausdrehen,
		// und zwar immer nach links!
		if (irL < 200 || irR < 200 || borderL > 1000 || borderR > 1000) {
			ll = -100;
			rr = 100;
		}

		this.setAktMotL(ll);
		this.setAktMotR(rr);

		super.work();
	}
}
