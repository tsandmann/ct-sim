/*
 * c't-Sim - Robotersimulator für den c't-Bot
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
 * Realisiert eine Sensorkennlinie über eine Reihe von Stützstellen. Die
 * Messgrößen, die einen bestimmten Sensor-Output ausloesen, müssen in ganzen
 * Zahlen angegeben werden, deshalb sollten sie in einer Einheit angegeben
 * werden, die hinreichend fein ist (Distanzsensoren in cm, Abgrundsensoren
 * vielleicht in mm). Die Sensor-Output-Daten duerfen in Gleitkommazahlen angegeben
 * werden (allerdings liefern viele Sensoren natuerlich ebenfalls ganzzahlige Werte).
 * 
 * @author p-king
 * 
 */

public class Characteristic {

	/** Das Array mit ausgewählten Messgrößen (M) und Sensordaten (S), Format:
	 * M1, S1, M2, S2 ....
	 * wobei Mx ganzzahlige, positive Werte sind und M(x+1) > Mx sein muss
	 * (Lücken sind aber erlaubt)
	 * Sx sind die Sensordaten als Gleitkommazahlen (floats)
	 */
	private float[] characteristic;

	/** Eine Lookup-Table, ist eine ergänzte Form von characteristic;
	 * hier übernimmt der Array-Index die Funktion der Mx-Werte.
	 * Diese sind hier lückenlos, Zwischenwerte werden extrapoliert.
	 */
	private float[] lookup;
	
	/** Kopie der Lookup-Tables, der auf ganze Zahlen gerundete Sensorwerte 
	 * zurückgibt
	 */
	private int[] intLookup;

	/** Sensordatum für alle Messgrößen ausserhalb der Kennlinie */
	private float INF;	
	
	/**
	 * Main-Methode nur zum Testen
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Characteristic charac = new Characteristic(new File("characteristics\\gp2d12Right.txt"), 999f);
		System.out.println("Präzise Werte");
		for (float f = 0f; f < 100; f = f + 0.1f) {
			System.out.printf("\n%12f", f);
			for (int i = 0; i < charac.lookupPrecise(f) / 5; i++) {
				System.out.print("*");
			}
		}
	}
	
	/**
	 * @param filename Pfad zur charac-Datei
	 * @param inf Sensordatum für Messgrößen ausserhalb der Kennlinie
	 * @throws IOException
	 */
	public Characteristic(String filename, float inf) throws IOException {
		this(ClassLoader.getSystemResource(filename).openStream(), inf);
	}

	/**
	 * @param openStream Stream der chrac-Datei
	 * @param inf Sensordatum für Messgrößen ausserhalb der Kennlinie
	 */
	public Characteristic(InputStream openStream, float inf) {
		this.INF = inf;
	    BufferedReader in = new BufferedReader(new InputStreamReader(openStream));
	    String line;
	    String c = new String();
		try {
			while ((line = in.readLine()) != null) {
				c += line + "\r\n";
			}
			in.close();
		} catch (FileNotFoundException e1) {
			System.err.println("Kennlinien-Datei nicht gefunden");
		} catch (IOException e1) {
			System.err.println("I/O-Fehler");
		}
		
		Number[] charac = csv2array(c); 
		// Numbers in primitive floats verwandeln:
		characteristic = new float[charac.length]; 
		for (int i = 0; i < charac.length; i++) {
			characteristic[i] = charac[i].floatValue();
		}
		
		// Lookup-Table hat so viele Stellen wie die letzte Messgröße (in der
		// vorletzten Stelle der Kennlinie) angibt -- plus eine natuerlich für
		// den 0-Index: 
		lookup = new float[(int) (1 + Math.floor(characteristic[characteristic.length - 2]))];
		// Lookup-Table jetzt fuellen:
		int firstMeas = (int) Math.floor(characteristic[0]);
		// Alles vor der ersten Messgröße mit INF fuellen:
		for (int i = 0; i < firstMeas; i++) {
			lookup[i] = INF;
		}
		// Dann jeweils in Zweierschritten voran:
		for (int i = 0; i < characteristic.length; i += 2) {
			// Zwei aufeinanderfolgende Messgrößen heraussuchen:
			int firMea = (int) Math.floor(characteristic[i]);
			// Wert am ersten Index eintragen:
			lookup[firMea] = characteristic[i + 1];
			try { // Klappt nicht, wenn schon das Ende erreicht ist.			
				int secMea = (int) Math.floor(characteristic[i + 2]);
				// Wie viele Schritte lassen die Messgrößen aus?
				int diff = secMea - firMea;
				// Und wie verändert sich der zugeordnete Wert zwischen den
				// Messgrößen?
				float valDiff = characteristic[i + 3] - characteristic[i + 1];
				// Das ist pro Schritt gleich der Wertdifferenz durch
				// Messgrößendifferenz:
				float delta = valDiff / diff;
				// Zwischenwerte addieren, für jeden weiteren
				// einmal delta auf lookup[firMea] draufrechnen:
				for (int j = 1; j < diff; j++) {
					lookup[firMea + j] = lookup[firMea] + j * delta;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				// Tja, das war wohl zu weit 8-)
			}
		}

		// Es wird ein zweiter Lookup-Table erstellt, für Sensorwerte, die 
		// nur aus ganzen Zahlen bestehen:
		intLookup = new int[lookup.length]; 
		for (int i = 0; i < lookup.length; i++) {
			intLookup[i] = Math.round(lookup[i]);
		}
		//printLookup();		
	}
	
	/**
	 * Der Konstruktor errechnet aus der lückenhaften Stützwerttabelle den
	 * kompletten Lookup-Table mit Zwischenwerten für alle ganzzahligen
	 * Messgrößen im Bereich der Kennlinie
	 * 
	 * @param file
	 *            Eine Textdatei mit der Stützwerttabelle 
	 *            Format:
	 *            Messgröße (int>=0) \t resultierendes Sensordatum (float) \n)
	 *            Messgrößen aufsteigend, aber nicht zwingend lückenlos
	 * @param inf
	 *            Sensordatum für Messgrößen ausserhalb der Kennlinie	  
	 */
	public Characteristic(File file, float inf) {
		// Wert ausserhalb des Messbereichs:
		this.INF = inf;
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
		for (int i = 0; i < charac.length; i++) {
			characteristic[i] = charac[i].floatValue();
		}
		
		// Lookup-Table hat so viele Stellen wie die letzte Messgröße (in der
		// vorletzten Stelle der Kennlinie) angibt -- plus eine natuerlich für
		// den 0-Index:	
		this.lookup = new float[(int) (1 + Math.floor(characteristic[characteristic.length - 2]))];
		// Lookup-Table jetzt fuellen:
		int firstMeas = (int) Math.floor(characteristic[0]);
		// Alles vor der ersten Messgröße mit INF fuellen:
		for (int i = 0; i < firstMeas; i++) {
			lookup[i] = INF;
		}
		// Dann jeweils in Zweierschritten voran:
		for (int i = 0; i < characteristic.length; i += 2) {
			// Zwei aufeinanderfolgende Messgrößen heraussuchen:	
			int firMea = (int) Math.floor(characteristic[i]);
			// Wert am ersten Index eintragen:
			lookup[firMea] = characteristic[i + 1];
			try { // Klappt nicht, wenn schon das Ende erreicht ist.
				int secMea = (int) Math.floor(characteristic[i + 2]);
				// Wie viele Schritte lassen die Messgrößen aus?
				int diff = secMea - firMea;
				// Und wie verändert sich der zugeordnete Wert zwischen den
				// Messgrößen?
				float valDiff = characteristic[i + 3] - characteristic[i + 1];
				// Das ist pro Schritt gleich der Wertdifferenz durch
				// Messgrößendifferenz:
				float delta = valDiff / diff;
				// Zwischenwerte addieren, für jeden weiteren
				// einmal delta auf lookup[firMea] draufrechnen:
				for (int j = 1; j < diff; j++) {
					lookup[firMea + j] = lookup[firMea] + j * delta;
				}
			} catch (ArrayIndexOutOfBoundsException e) {
				// Tja, das war wohl zu weit 8-)
			}
		}

		// Es wird ein zweiter Lookup-Table erstellt, für Sensorwerte, die 
		// nur aus ganzen Zahlen bestehen:
		intLookup = new int[lookup.length]; 
		for (int i = 0; i < lookup.length; i++) {
			intLookup[i] = Math.round(lookup[i]);
		}
		// printLookup();
	}

	/**
	 * Gibt zur übergebenen Messgröße den passenden Sensorwert zurück.
	 * Präzise Funktion, die bei Messgrößen zwischen ganzen Zahlen weitere
	 * Zwischenwerte berechnet. Nur sinnvoll bei Sensoren, die nicht nur ganzzahlige
	 * Messwerte liefern
	 * 
	 * @param measure
	 *            Die Messgröße, aufgrund derer der Sensor seinen Wert erzeugt
	 *            (z.B. die Distanz bei Distanzsensoren)
	 * @return Das Sensordatum laut Kennlinie, ist eine ganze Zahl
	 */
	public double lookupPrecise(Number measure) {
		double measurement = measure.doubleValue();
		double data;
		// Liegt der Wert innerhalb der Tabelle?
		if (measurement >= 0 && measurement <= lookup.length - 1) {
			int index = (int) Math.floor(measurement);
			data = lookup[index];
			// Falls der Wert nicht am Rand der Tabelle liegt,
			// noch Zwischenwert extrapolieren --
			if (data != INF && index < lookup.length - 1) {
				data = data + (measurement - index) * (lookup[index + 1] - lookup[index]);
				// Es sollen ganze Zahlen zurückgegeben werden, wie 
				// sich das für einen digitalen Sensor gehört:
				data = Math.round(data);
			}
		} else {
			// Sonst INF zurückgeben:
			data = INF;
		}
		return data;
	}

	
	/**
	 * Schreibt die Lookup-Tables zum Debuggen auf die Konsole
	 */
	@SuppressWarnings("unused")
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
		for (int i = 0; i < num.size(); i++) {
			result[i] = num.elementAt(i);
		}
		return result;
	}

	/**
	 * Liest den Inhalt einer Datei und gibt ihn als String zurück.
	 * 
	 * @param file
	 *            Die Datei
	 * @return Der String mit dem Inhalt der Datei
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static String readFile(File file) throws IOException, FileNotFoundException {
		StringBuffer input = new StringBuffer();
		FileInputStream stream = new FileInputStream(file);
		int c = 0;
		while (c != -1) {
			c = stream.read();
			input.append((char) c);
		}
		stream.close();
		
		/*
		 * Merkwuerdigerweise wird bei dieser Methode, einen FileInputStream in
		 * einen String zu verwandeln, ans Ende ein '?' als Zeichen für EOF
		 * angehängt, das wir auf etwas unschoene Art und Weise abschneiden
		 * müssen:
		 */
		input = input.deleteCharAt((input.length()) - 1);
		return (input.toString());
	}
}
