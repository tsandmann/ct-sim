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
package ctSim.model.bots.ctbot;

import ctSim.model.bots.SimulatedBot;

/**
 * Klasse aller simulierten c't-Bots, die nur innerhalb des Simulators existieren
 *
 */
public class CtBotSimTest extends CtBot implements SimulatedBot {
	public CtBotSimTest() {
		super("Test-Bot");
	}

	@Override
	public String getDescription() {
		return "Simulierter, in Java geschriebener c't-Bot";
	}

	public void doSimStep() {
/*//$$$ Simulator fuer test-bot
		// TODO Pruefen ob: super.work();

		@SuppressWarnings({"unused"}) double ll = 100d, rr = 100d;

		double irl = irL.get().doubleValue();
		double irr = irR.get().doubleValue();

		// Ansteuerung fuer die Motoren in Abhaengigkeit vom Input
		// der IR-Abstandssensoren, welche die Entfernung in mm
		// zum naechsten Hindernis in Blickrichtung zurueckgeben

		// Solange die Wand weit weg ist, wird Stoff gegeben:
		if (irl >= 500) {
			ll = 255;
		}
		if (irr >= 500) {
			rr = 255;
		}

		// Vorsicht, die Wand kommt naeher:
		// Jetzt den Motor auf der Seite, die weiter entfernt ist,
		// langsamer laufen lassen als den auf der anderen Seite
		// - dann bewegt sich der Bot selbst
		// bei Wandkollisionen noch etwas und kommt eventuell
		// wieder frei:
		if (irl < 500 && irl >= 200) {
			if (irl <= irr)
				ll = 80;
			else
				ll = 50;
		}
		if (irr < 500 && irr >= 200) {
			if (irl > irr)
				rr = 80;
			else
				rr = 50;
		}

		// Ist ein Absturz zu befuerchten?
		short borderl = borderL.get().shortValue();
		short borderr = borderR.get().shortValue();
		if (borderl > borderr) {
			ll = 100;
			rr = -100;
		} else if (borderl < borderr) {
			ll = -100;
			rr = 100;
		}

		// Kollision oder Abgrund droht: Auf dem Teller rausdrehen,
		// und zwar immer nach links!
		if (irl < 200 || irr < 200 || borderl > 1000 || borderr > 1000) {
			ll = -100;
			rr = 100;
		}

		// TODO:
//		this.setActMotL(ll);
//		this.setActMotR(rr);
 */
	}
	
	public void updateView() throws InterruptedException {
		components.updateView();
	}
}