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

package ctSim.model.bots.components;

import java.io.*;
import java.util.*;

/**
 * Realisiert eine Sensorkennlinie ueber eine Reihe von Stuetzstellen. Die
 * Messgroessen, die einen bestimmten Sensor-Output ausloesen, muessen in ganzen
 * Zahlen angegeben werden, deshalb sollten sie in einer Einheit angegeben
 * werden, die hinreichend fein ist (Distanzsensoren in cm, Abgrundsensoren
 * vielleicht in mm). Die Sensor-Output-Daten duerfen in Gleitkommazahlen angegeben
 * werden (allerdings liefern viele Sensoren natuerlich ebenfalls ganzzahlige Werte).
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
	
	// Kopie des Lookup-Tables, der auf ganze Zahlen gerundete Sensorwerte 
	// zurueckgibt
	private int[] intLookup;

	// Sensordatum fuer alle Messgroessen ausserhalb der Kennlinie
	private float INF;	
	
	/**
	 * Main-Methode nur zum Testen
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Characteristic charac = new Characteristic(new File("..\\ct-Sim_V2\\characteristics\\gp2d12Left.txt"), 999f);
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
	 * @param file
	 *            Eine Textdatei mit der Stuetzwerttabelle 
	 *            Format:
	 *            Messgroesse (int>=0) \t resultierendes Sensordatum (float) \n)
	 *            Messgroessen aufsteigend, aber nicht zwingend lueckenlos
	 * @param inf
	 *            Sensordatum fuer Messgroessen ausserhalb der Kennlinie	  
	 */
	public Characteristic(File file, float inf) {
		// Wert ausserhalb des Messbereichs:
		INF = inf;
		// String mit der Kennlinie
		String c = new String();
		try {
			c = readFile(file);
		} catch (FileNotFoundException e1) {
			System.err.println("Kennlinien-Datei nicht gefunden");
		} catch (IOException e1) {
			System.err.println("I/O-Fehler");
		}			 
		
		Number[] charac = csv2array(c); 
		// Numbers in primitive floats verwandeln:
		characteristic = new float[charac.length]; 
		for (int i=0; i<charac.length; i++){
			characteristic[i] = charac[i].floatValue();
		}
		
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

		// Es wird ein zweiter Lookup-Table erstellt, fuer Sensorwerte, die 
		// nur aus ganzen Zahlen bestehen:
		intLookup = new int[lookup.length]; 
		for (int i = 0; i<lookup.length; i++){
				intLookup[i] = Math.round(lookup[i]);
			}
		printLookup();
	}

	/**
	 * Gibt zur uebergebenen Messgroesse den passenden Sensorwert zurueck.
	 * Performante Funktion, die ganzzahlige Messgroessen erwartet und Sensorwerte 
	 * auf ganze Zahlen rundet. 
	 * 
	 * @param measurement
	 *            Die Messgroesse, aufgrund derer der Sensor seinen Wert erzeugt
	 *            (z.B. die Distanz bei Distanzsensoren)
	 * @return Das Sensordatum laut Kennlinie
	 */
	public int lookup(int measurement) {
		int data;
		// Liegt der Wert innerhalb der Tabelle?
		if (measurement >= 0 && measurement <= intLookup.length - 1) {
			data = intLookup[measurement];
		} else {
			// Sonst INF zurueckgeben:
			data = Math.round(INF);
		}
		return data;
	}

	/**
	 * Gibt zur uebergebenen Messgroesse den passenden Sensorwert zurueck.
	 * Rundet die Messgroesse auf ganze Zahlen.
	 * Performante Funktion, die Messgroessen und Sensorwerte auf ganze Zahlen rundet. 
	 * Sollte nur benutzt werden, wenn Messgroessen auch Gleitkommazahlen sein
	 * koennen.
	 * 
	 * @param measurement
	 *            Die Messgroesse, aufgrund derer der Sensor seinen Wert erzeugt
	 *            (z.B. die Distanz bei Distanzsensoren)
	 * @return Das Sensordatum laut Kennlinie
	 */
	public int lookup(float measurement) {
		int data;
		// Liegt der Wert innerhalb der Tabelle?
		if (measurement >= 0 && measurement <= intLookup.length - 1) {
			int index = Math.round(Math.round(Math.floor(measurement)));
			data = intLookup[index];
		} else {
			// Sonst INF zurueckgeben:
			data = Math.round(INF);
		}
		return data;
	}

	
	/**
	 * Gibt zur uebergebenen Messgroesse den passenden Sensorwert zurueck.
	 * Praezise Funktion, die bei Messgroessen zwischen ganzen Zahlen weitere
	 * Zwischenwerte berechnet. Nur sinnvoll bei Sensoren, die nicht nur ganzzahlige
	 * Messwerte liefern
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
	 * Schreibt die Lookup-Tables zum Debuggen auf die Konsole
	 */
	private void printLookup() {
		System.out.println("Lookup-Table");
		for (int i = 0; i < lookup.length; i++) {
			System.out.println("Zeile\t" + i + "\t" + lookup[i]+ "\t" + intLookup[i]);
		}
	}

	/**
	 * Zerlegt einen CSV-String und schreibt alle gefundenen Zahlenwerte in ein Array.
	 * @param input
	 *            Der zu zerlegende String, Einzelteile durch ; getrennt
	 * @return Das Array
	 */
	private static Number[] csv2array(String input) {

		StringTokenizer st = new StringTokenizer(input, ";");
		Vector<Number> num = new Vector<Number>();
		Number curr;
		while (st.hasMoreTokens()) {
			try {
				curr = new Double(st.nextToken());
				num.add(curr);
			} catch (NumberFormatException e) {
				// Alles auslassen, was keine Zahl ist 
			}
		}
		
		Number[] result = new Number[num.size()];
		for (int i = 0; i<num.size(); i++){
			result[i] = num.elementAt(i);
		}
		return result;
	}

	/**
	 * Liest den Inhalt einer Datei und gibt ihn als String zurueck.
	 * 
	 * @param file
	 *            Die Datei
	 * @return Der String mit dem Inhalt der Datei
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static String readFile(File file) throws IOException,
			FileNotFoundException {

		StringBuffer input = new StringBuffer();
		FileInputStream stream = new FileInputStream(file);
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