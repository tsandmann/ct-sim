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

package ctSim.Model.Bots;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.Controller.Controller;

/**
 * Klasse fuer Testbots, die ausschliesslich zum Test des Simulators dienen; Bots
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
	 * @param controller
	 * @param pos initiale Position
	 * @param head initiale Blickrichtung
	 */
	public CtBotSimTest(Controller controller, Point3f pos, Vector3f head) {
		super(controller, pos, head);
	}

	/**
	 * Initialisiere den Testbot
	 * 
	 * @see Bot#init()
	 */
	@Override
	protected void init() {
		byte w= 18;
		byte h= 18;
		
		byte[] data = new byte[w*h];
		
		
		for (byte x= 0; x<w; x++)
			for (byte y= 0; y<h; y++)
				data[y + (x*h)] = (new Integer( y * (64/h) +64 )).byteValue();
		
		setMousePicture(1,data);
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
	 * @see Bot#work()
	 */
	@Override
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

		// Ist ein Absturz zu befuerchten?
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

		this.setActMotL(ll);
		this.setActMotR(rr);

		super.work();
	}

	/**
	 * Hilfsroutine, die einen String abschneidet oder mit Nullen (0) auffuellt
	 * bis die uebergebene konstante Laenge erreicht ist.
	 * @param numberStr
	 *              Der String, der gekuerzt/verlaengert werden soll.
	 * @param length
	 *              Die Ziellaenge des Ergebnisstrings
	 * @return Der String
	 */
	private static String formatNumberStr(String numberStr, int length) {
		if (numberStr == null)
			return null;

		if (numberStr.length() < length)
			return "0000000000".substring(0, length - numberStr.length())
					+ numberStr;
		else if (numberStr.length() > length)
			return numberStr.substring(numberStr.length() - length, numberStr
					.length());
		else
			return numberStr;
	}
	
	
	/**
	 * Aktualisiert den Text, der auf dem LCDisplay angezeigt wird anhand der aktuellen
	 * Sensordaten.
	 */
	protected void updateLcdText() {
		// entspricht: display_printf("P=%03X %03X D=%03d %03d ",sensLDRL,sensLDRR,sensDistL,sensDistR);
		this.setLcdText(0, 0, "P="
				+ formatNumberStr(Integer.toHexString(this.getSensLdrL())
						.toUpperCase(), 3)
				+ " "
				+ formatNumberStr(Integer.toHexString(this.getSensLdrR())
						.toUpperCase(), 3) + " D="
				+ formatNumberStr(Integer.toString(this.getSensIrL()), 3) + " "
				+ formatNumberStr(Integer.toString(this.getSensIrR()), 3));

		// entspricht: display_printf("B=%03X %03X L=%03X %03X ",sensBorderL,sensBorderR,sensLineL,sensLineR);
		this.setLcdText(0, 1, "B="
				+ formatNumberStr(Integer.toHexString(this.getSensBorderL())
						.toUpperCase(), 3)
				+ " "
				+ formatNumberStr(Integer.toHexString(this.getSensBorderR())
						.toUpperCase(), 3)
				+ " L="
				+ formatNumberStr(Integer.toHexString(this.getSensLineL())
						.toUpperCase(), 3)
				+ " "
				+ formatNumberStr(Integer.toHexString(this.getSensLineR())
						.toUpperCase(), 3));

		// entspricht: display_printf("R=%2d %2d F=%d K=%d T=%d ",sensEncL % 10,sensEncR %10,sensError,sensDoor,sensTrans);
		String sensEncL = String.valueOf(this.getSensEncL() % 10);
		if (this.getSensEncL() >= 0)
			sensEncL = " " + sensEncL;
		String sensEncR = String.valueOf(this.getSensEncR() % 10);
		if (this.getSensEncR() >= 0)
			sensEncR = " " + sensEncR;

		this.setLcdText(0, 2, "R=" + sensEncL + " " + sensEncR + " F="
				+ Integer.toString(this.getSensError()) + " K="
				+ Integer.toString(this.getSensDoor()) + " T="
				+ Integer.toString(this.getSensTrans()));

		// entspricht: display_printf("I=%04X M=%05d %05d",RC5_Code,sensMouseX,sensMouseY);
		this.setLcdText(0, 3, "I="
				+ formatNumberStr(Integer.toHexString(this.getSensRc5())
						.toUpperCase(), 4) + " M="
				+ formatNumberStr(Integer.toString(this.getSensMouseDX()), 5)
				+ " "
				+ formatNumberStr(Integer.toString(this.getSensMouseDY()), 5));
	}

	/**
	 * Aktualisiert den Status des Bot. In dieser Routine sitzt ein Grossteil
	 * der Intelligenz des Simulators.<br/> Dabei wird alles relativ zu deltaT
	 * berechnet.
	 * 
	 */
	@Override
	protected void updateStats() {
		super.updateStats();
	
		// LCD aktualisieren
		this.updateLcdText();
	}


	/** Dummy-Funktion ohne Wirkung */
	@Override
	public void requestMousePicture() {
		System.out.println("Frage nach Maussbild");
	}
	
	
}
