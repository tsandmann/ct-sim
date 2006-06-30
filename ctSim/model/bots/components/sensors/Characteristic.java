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

package ctSim.model.bots.components.sensors;

import java.io.*;
import java.util.*;

/**
 * Realisiert eine Sensorkennlinie ueber eine Reihe von Stuetzstellen. Die
 * Messgroessen, die einen bestimmten Sensor-Output ausloesen, muessen in ganzen
 * Zahlen angegeben werden, deshalb sollten sie in einer Einheit angegeben
 * werden, die hinreichend fein ist (Distanzsensoren in cm, Abgrundsensoren
 * vielleicht in mm). Die Sensor-Output-Daten duerfen Gleitkommazahlen sein.
 * 
 * @author p-king
 * 
 */

public class Characteristic {

	// Das Array mit ausgewaehlten Messgroessen (M) und Sensordaten (S), Format:
	// M1, S1, M2, S2 ....
	// wobei Mx ganzzahlige, positive Werte sind und M(x+1) > Mx sein muss
	// (Luecken sind aber erlaubt)
	// Sx sind die Sensordaten als Gleitkommazahlen (floats)
	private float[] characteristic;

	// Ein Lookup-Table, ist eine ergaenzte Form von characteristic;
	// hier uebernimmt der Array-Index die Funktion der Mx-Werte.
	// Diese sind hier lueckenlos, Zwischenwerte werden extrapoliert.
	private float[] lookup;

	// Sensordatum fuer alle Messgroessen ausserhalb der Kennlinie
	private float INF;

	/**
	 * Main-Methode nur zum Testen
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Characteristic charac = new Characteristic("kennlinie.txt", 999f);
		System.out.println("Performante Werte");
		for (float f = -2f; f < 100; f = f + 0.25f) {
			System.out.println(f + "\t" + charac.lookup(f));
		}
		System.out.println("Praezise Werte");
		for (float f = -2f; f < 100; f = f + 0.25f) {
			System.out.println(f + "\t" + charac.lookupPrecise(f));
		}

	}

	/**
	 * Der Konstruktor errechnet aus der lueckenhaften Stuetzwerttabelle den
	 * kompletten Lookup-Table mit Zwischenwerten fuer alle ganzzahligen
	 * Messgroessen im Bereich der Kennlinie
	 * 
	 * @param filename
	 *            Name der Textdatei mit der Stuetzwerttabelle 
	 *            Format:
	 *            Messgroesse (int>=0) \t resultierendes Sensordatum (float) \n)
	 *            Messgroessen aufsteigend, aber nicht zwingend lueckenlos
	 * @param inf
	 *            Sensordatum fuer Messgroessen ausserhalb der Kennlinie
	 */
	public Characteristic(String filename, float inf) {
		INF = inf;

		// TODO: Kennlinie noch aus einer Textdatei einlesen!
		// BufferedReader in;
		// try {
		// in = new BufferedReader(new FileReader(path));
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// Vector vec = new Vector();
		//			 
		// Vorlaeufig hart gecodete Kennlinie fuer Testzwecke
		characteristic = new float[] { 8, 538, 9, 543, 10, 525, 11, 493, 12,
				470, 14, 412, 16, 364, 18, 323, 20, 298, 22, 276, 26, 240, 30,
				206, 34, 189, 38, 175, 44, 160, 50, 145, 56, 135, 64, 128, 72,
				119, 80, 110 };
		// Lookup-Table hat so viele Stellen wie die letzte Messgroesse (in der
		// vorletzten Stelle der Kennlinie) angibt -- plus eine natuerlich fuer
		// den 0-Index:
		lookup = new float[1 + Math.round(Math.round(Math
				.floor(characteristic[characteristic.length - 2])))];
		// Lookup-Table jetzt fuellen:
		int firstMeas = Math.round(Math.round(Math.floor(characteristic[0])));
		// Alles vor der ersten Messgroesse mit INF fuellen:
		for (int i = 0; i < firstMeas; i++) {
			lookup[i] = INF;
		}
		// Dann jeweils in Zweierschritten voran:
		for (int i = 0; i < characteristic.length; i += 2) {
			// Zwei aufeinanderfolgende Messgroessen heraussuchen:
			int firMea = Math.round(Math.round(Math.floor(characteristic[i])));
			// Wert am ersten Index eintragen:
			lookup[firMea] = characteristic[i + 1];
			try { // Klappt nicht, wenn schon das Ende erreicht ist.
				int secMea = Math.round(Math.round(Math
						.floor(characteristic[i + 2])));
				// Wie viele Schritte lassen die Messgroessen aus?
				int diff = secMea - firMea;
				// Und wie veraendert sich der zugeordnete Wert zwischen den
				// Messgroessen?
				float valDiff = characteristic[i + 3] - characteristic[i + 1];
				// Das ist pro Schritt gleich der Wertdifferenz durch
				// Messgroessendifferenz:
				float delta = valDiff / diff;
				// Zwischenwerte addieren, fuer jeden weiteren
				// einmal delta auf lookup[firMea] draufrechnen:
				for (int j = 1; j < diff; j++) {
					lookup[firMea + j] = lookup[firMea] + j * delta;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				// Tja, das war wohl zu weit 8-)
			}
		}
		printLookup();
	}

	/**
	 * Gibt zur uebergebenen Messgroesse den passenden Sensorwert zurueck.
	 * Performante Funktion, die Messgroessen auf ganze Zahlen rundet.
	 * 
	 * @param measurement
	 *            Die Messgroesse, aufgrund derer der Sensor seinen Wert erzeugt
	 *            (z.B. die Distanz bei Distanzsensoren)
	 * @return Das Sensordatum laut Kennlinie
	 */
	public float lookup(float measurement) {
		float data;
		// Liegt der Wert innerhalb der Tabelle?
		if (measurement >= 0 && measurement <= lookup.length - 1) {
			int index = Math.round(Math.round(Math.floor(measurement)));
			data = lookup[index];
		} else {
			// Sonst INF zurueckgeben:
			data = INF;
		}
		return data;
	}

	/**
	 * Gibt zur uebergebenen Messgroesse den passenden Sensorwert zurueck.
	 * Praezise Funktion, die bei Messgroessen zwischen ganzen Zahlen weitere
	 * Zwischenwerte berechnet.
	 * 
	 * @param measurement
	 *            Die Messgroesse, aufgrund derer der Sensor seinen Wert erzeugt
	 *            (z.B. die Distanz bei Distanzsensoren)
	 * @return Das Sensordatum laut Kennlinie
	 */
	public float lookupPrecise(float measurement) {
		float data;
		// Liegt der Wert innerhalb der Tabelle?
		if (measurement >= 0 && measurement <= lookup.length - 1) {
			int index = Math.round(Math.round(Math.floor(measurement)));
			data = lookup[index];
			// Falls der Wert nicht am Rand der Tabelle liegt,
			// noch Zwischenwert extrapolieren --
			if (data != INF && index < lookup.length - 1) {
				data = data + (measurement - index)
						* (lookup[index + 1] - lookup[index]);
			}
		} else {
			// Sonst INF zurueckgeben:
			data = INF;
		}
		return data;
	}

	/**
	 * Schreibt den Lookup-Table zum Debuggen auf die Konsole
	 */
	private void printLookup() {
		System.out.println("Lookup-Table");
		for (int i = 0; i < lookup.length; i++) {
			System.out.println("Zeile\t" + i + "\t" + lookup[i]);
		}
	}

	/**
	 * Diese Funktion zerlegt einen uebergebenen String und schreibt ihn
	 * zeilenweise in ein String-Array, das zurueckgegeben wird. Dazu wird ein
	 * String-Tokenizer verwendet, der als Trennzeichen den 'newline'-character
	 * \n uebergeben bekommt.
	 * 
	 * @param input
	 *            Der zu zerlegende String
	 * @return Das String-Array mit den Zeilen
	 */
	private static String[] breakIntoLines(String input) {

		StringTokenizer st = new StringTokenizer(input, "\n");

		/*
		 * Das String-Array lines bekommt eine Laenge zugewiesen, die genau der
		 * Zahl der Zeilen des Strings (= Anzahl der durch \n getrennten Tokens
		 * in diesem String) entspricht.
		 */

		String[] lines = new String[st.countTokens()];
		int i = 0;

		/* Der String input wird dann zeilenweise in lines geschrieben. */

		while (st.hasMoreTokens()) {
			lines[i] = st.nextToken();
			i++;
		}
		return lines;
	}

	/**
	 * Liest den Inhalt einer Datei und gibt ihn als String zurueck.
	 * 
	 * @param fileName
	 *            Der Dateiname
	 * @return Der String mit dem Inhalt der Datei
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static String readFile(String fileName) throws IOException,
			FileNotFoundException {

		StringBuffer input = new StringBuffer();
		FileInputStream stream = new FileInputStream(fileName);
		int c = 0;
		while (c != -1) {
			c = stream.read();
			input.append((char) c);
		}
		/*
		 * Merkwuerdigerweise wird bei dieser Methode, einen FileInputStream in
		 * einen String zu verwandeln, ans Ende ein '?' als Zeichen fuer EOF
		 * angehaengt, das wir auf etwas unschoene Art und Weise abschneiden
		 * muessen:
		 */
		input = input.deleteCharAt((input.length()) - 1);
		return (input.toString());
	}
}
