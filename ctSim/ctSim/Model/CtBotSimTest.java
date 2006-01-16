/*
 * c't-Sim - Robotersimulator f�r den c't-Bot
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

/**
 * Klasse fuer Testbots, die ausschlie�lich zum Test des Simulators dienen; Bots
 * diesen Typs brauchen keine TCP-Verbindung.
 * 
 * @author pek (pek@heise.de)
 * 
 */
public class CtBotSimTest extends CtBotSim {

	private short ll = 10;

	private short rr = 11;

	/**
	 * Erzeugt einen neuen Testbot
	 * 
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 */
	public CtBotSimTest(Point3f pos, Vector3f head) {
		super(pos, head);
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
	 * Robotersteuerung. Dieser Code laesst sich NICHT auf einen echten c't-Bot
	 * uebertragen, da diese kein Java unterstuetzt.
	 * 
	 * @see ctSim.Model.Bot#work()
	 */
	public void work() {

		ll = rr = 0;

		int irL = this.getSensIrL();
		int irR = this.getSensIrR();

		// Ansteuerung fuer die Motoren in Abhaengigkeit vom Input
		// der IR-Abstandssensoren, welche die Entfernung in mm
		// zum naechsten Hindernis in Blickrichtung zurueckgeben

		// Solange die Wand weit weg ist, wird Stoff gegeben:
		if (irL >= 500) {
			ll = 20;
		}
		if (irR >= 500) {
			rr = 20;
		}

		// Vorsicht, die Wand kommt naeher:
		// Jetzt den Motor auf der Seite, die weiter entfernt ist,
		// langsamer laufen lassen als den auf der anderen Seite
		// - dann bewegt sich der Bot selbst
		// bei Wandkollisionen noch etwas und kommt eventuell
		// wieder frei:
		if (irL < 500 && irL >= 200) {
			if (irL <= irR)
				ll = 7;
			else
				ll = 5;
		}
		if (irR < 500 && irR >= 200) {
			if (irL >= irR)
				rr = 7;
			else
				rr = 5;
		}

		// Kollision droht: Auf dem Teller rausdrehen!
		if (irL < 200 || irR < 200) {
			// Drehung von der Wand weg:
			if (irL <= irR) {
				ll = 10;
				rr = -10;
			} else {
				ll = -10;
				rr = 10;
			}
		}

		this.setAktMotL(ll);
		this.setAktMotR(rr);

		super.work();
	}
}
