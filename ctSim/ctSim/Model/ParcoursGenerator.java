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

import java.util.Random;

/**
 * Parcours-Generator fuer den c't-Sim-Wettbewerb
 * 
 * @author pek (pek@heise.de)
 * 
 */

public class ParcoursGenerator {

	public static final char FLOOR = ' ';

	public static final char WHITE = '.';

	public static final char WALL = 'X';

	public static final char WALLH = '='; // horizontale Wand

	public static final char WALLV = '#'; // vertikale Wand

	public static final char HOLE = 'L';

	public static final char LAMP = '*';

	public static final char GOAL = 'Z';

	public static final char START1 = '1';

	public static final char START2 = '2';

	public static final String xmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE world SYSTEM \"parcours.dtd\"><world><description>Dieses ist ein automatisch generierter Beispielparcours fuer den c't-Sim-Wettbewerb.</description><parcours>";

	public static final String xmlTail = "</parcours><optics><appearance type=\"X\"><description>quadratische Wand</description><texture>textures/rock_wall.jpg</texture><color>#999999</color></appearance><appearance type=\"#\"><description>senkrechte Wand</description><clone>X</clone></appearance><appearance type=\"=\"><description>wagrechte Wand</description><clone>X</clone></appearance><appearance type=\".\"><description>Fussboden im Eingangsbereich</description><color type=\"ambient\">#FFFFFF</color><color type=\"diffuse\">#FFFFFF</color></appearance><appearance type=\" \"><description>Fussboden im Labyrinth</description><color type=\"ambient\">#606060</color><color type=\"diffuse\">#606060</color></appearance><appearance type=\"1\"><description>Fussboden des Startfeldes 1</description><color type=\"ambient\">#993030</color><color type=\"diffuse\">#993030</color></appearance><appearance type=\"2\"><description>Fussboden des Startfeldes 2</description><color type=\"ambient\">#000099</color><color type=\"diffuse\">#000099</color></appearance><appearance type=\"0\"><description>Fussboden des Default-Startfeldes</description><clone>.</clone></appearance><appearance type=\"Z\"><description>Fussboden des Zielfeldes 0</description><color type=\"ambient\">#66FF00</color><color type=\"diffuse\">#66FF00</color></appearance><appearance type=\"*\"><description>Lichtkugel</description><color type=\"emmissive\">#FFFF90</color></appearance><appearance type=\"-\"><description>Linie</description><color type=\"ambient\">#000000</color><color type=\"diffuse\">#000000</color><color type=\"specular\">#000000</color><color type=\"emmissive\">#000000</color></appearance><appearance type=\"|\"><description>Linie</description><clone>-</clone></appearance><appearance type=\"/\"><description>Linie</description><clone>-</clone></appearance><appearance type=\" \\\"><description>Linie</description><clone>-</clone></appearance><appearance type=\"+\"><description>Linie</description><clone>-</clone></appearance><appearance type=\"~\"><description>Linie</description><clone>-</clone></appearance></optics></world>";

	/**
	 * Die Karte ist ein char-Array, erste Dimension die Zeilennummer, die
	 * zweite die Spaltennummer:
	 */

	private char[][] map;

	// Generiert wird die Karte allerdings nur zur Haelfte:

	private char[][] halfmap;

	/**
	 * Die Groesse des Labyrinths in Feldern, wegen Symmetrie wird bei der
	 * Breite nur die Hälfte angegeben
	 */

	private int width;

	private int height;

	private Random rand;

	// Parameter fuer die Erzeugung:

	// Verschnoerkelungsfaktor; Anzahl der angestrebten
	// Segmente pro Hindernis
	private int twirling;

	// Hindernisdichte an der Wand; klein = rauh
	private int wallRoughness;

	// Verhaeltnis der Hindernismenge an der Wand zu solchen in der Mitte
	private int innerRoughness; // TODO: einbauen!

	// Lochanteil; jedes n-te Wandstück wird durch Loch ersetzt
	private int perforation;

	// Konstruktor ist sehr simpel:
	public ParcoursGenerator() {
		rand = new Random();
	}

	/**
	 * Main-Methode nur fuers Debugging
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		for (int i = 0; i < 20; i++) {
			ParcoursGenerator parcGen = new ParcoursGenerator();
			System.out.println(parcGen.generateParc());
		}
	}

	/**
	 * Diese Methode generiert das eigentliche Labyrinth. Parameter werden
	 * zufällig gesetzt.
	 */
	public String generateParc() {
		int w, h, r, t, p;
		w = rand.nextInt(10) + 6; // Breite zwischen 12 und 30 Felder
		h = rand.nextInt(19) + 12; // Hoehe zwischen 12 und 30 Felder
		r = 5; // TODO: zufällig variieren
		t = 3; // TODO: zufällig variieren
		p = 10; // TODO: zufällig variieren
		return generateParc(w, h, r, t, p);
	}

	public String generateParc(int wi, int he, int ro, int tw, int pe) {
		width = wi;
		height = he;
		wallRoughness = ro;
		twirling = tw;
		perforation = pe;

		// Zunaechst wird nur die halbe Karte gebaut.

		// Erste Dimension ist die Zeilennummer und
		// zweite die Spaltennummer, daher erst Hoehe
		// und dann Breite:

		halfmap = new char[height][width];

		// Mit Space fuellen:
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				halfmap[r][c] = FLOOR;
			}
		}

		// Dann die Einfassung bauen:
		buildEnclosure();
		// printParc("Einfriedung:", halfmap);
		// Dann Hindernisse am linken Rand anfuegen:
		roughenWalls();
		// printParc("Aufrauen:", halfmap);
		generateFreeObstacles();
		// printParc("Freie Hindernisse:" halfmap);
		// Einzelne Hindernisse durch Loecher ersetzen:
		perforate();
		// Fehlende Haelfte rekonstruieren:
		mirror();
		// printParc("Gespiegelt:", map);
		// Startfelder einbauen:
		generateStart();
		printParc("Fertig!!", map);

		// Parcours in XML packen:
		return parc2XML(map);

	}

	/**
	 * Versieht das Spielfeld mit einer Einfriedung:
	 */
	private void buildEnclosure() {

		// Nord- und Suedwand:
		for (int c = 1; c < width; c++) {
			halfmap[0][c] = WALLH;
			halfmap[height - 1][c] = WALLH;
		}

		// Westwand:
		for (int r = 1; r < height - 1; r++) {
			halfmap[r][0] = WALLV;
		}

		// Die Lampen in die Ecken:
		halfmap[0][0] = LAMP;
		halfmap[height - 1][0] = LAMP;

		// Zielfeld ganz rechts oben:
		halfmap[0][width - 1] = GOAL;

	}

	/**
	 * Fuegt Hindernisse an die Waende an, so dass der Weg an der Wand entlang
	 * auf jeden Fall laenger ist als der kuerzeste Weg. Die Methode sorgt auch
	 * fuer Hindernisse, die ueber die Mittellinie hinausgehen.
	 */
	private void roughenWalls() {
		// Die Anzahl der Hindernisse an der Wand berechnet sich durch
		// Laenge / roughness

		// Zuerst Ostwand moeblieren
		// (= Hindernisse in die Mitte einfuegen)

		int obstNum = height / wallRoughness;
		// TODO: Menge von Parametern abhaenging machen!!
		// for (int i = 0; i < obstNum; i++) {
		addWallObstacle('E');
		// }

		// Dann Westwand moeblieren:
		obstNum = height / wallRoughness;
		for (int i = 0; i < obstNum; i++) {
			addWallObstacle('W');
		}

		// Dann Nord- und Suedwand moeblieren:
		obstNum = width / wallRoughness;
		for (int i = 0; i < obstNum; i++) {
			addWallObstacle('N');
			addWallObstacle('S');
		}

	}

	/**
	 * Fuegt ein neues Hindernis direkt an der Wand hinzu
	 * 
	 * @param wall
	 *            Die Wand, die ein Hindernis erhalten soll (N, S, W, E)
	 */
	private void addWallObstacle(char wall) {

		int col, row;

		switch (wall) {
		case 'N':
			// erste Zeile,
			row = 0;
			// Spalte ist Zufall, haelt aber Sicherheitsabstand
			// von der Ecke und vom Zielfeld:
			col = rand.nextInt(width - 5) + 3;
			generateTwirl(row, col, 2, twirling);
			break;
		case 'S':
			// zweite Zeile,
			row = height - 1;
			// Spalte ist Zufall, haelt aber Sicherheitsabstand
			// von der Ecke:
			col = rand.nextInt(width - 4) + 3;
			generateTwirl(row, col, 0, twirling);
			break;
		case 'W':
			// Reihe ist Zufall, haelt aber Sicherheitsabstand von der Ecke:
			row = rand.nextInt(height - 6) + 3;
			// erste Spalte
			col = 0;
			generateTwirl(row, col, 1, twirling);
			break;
		case 'E':
			// Reihe ist Zufall, haelt aber Sicherheitsabstand von der Ecke:
			row = rand.nextInt(height - 6) + 3;
			// letzte Spalte
			col = width - 1;
			halfmap[row][col] = WALLH;
			generateTwirl(row, col, 3, twirling);
			break;

		default:
			return;
		}
	}

	/**
	 * "Verschnoerkelt" ein Hindernis in zufaellige Richtung.
	 * 
	 * @param row
	 *            Zeile der Startkoordinate
	 * @param col
	 *            Spalte der Startkoordinate
	 * 
	 * @param twirlsLeft
	 *            Wie viele weitere Segmente noch folgen duerfen
	 */

	private void generateTwirl(int row, int col, int twirlsLeft) {
		// Zufaellige Richtung: 0 = nach Norden 1 = nach Osten 2 = nach Sueden 3
		// =
		// nach Westen
		int dir = rand.nextInt(4);
		generateTwirl(row, col, dir, twirlsLeft);
	}

	/**
	 * "Verschnoerkelt" ein Hindernis in vorgegebene Richtung.
	 * 
	 * @param row
	 *            Zeile der Startkoordinate
	 * @param col
	 *            Spalte der Startkoordinate
	 * @param dir
	 *            Richtung (0=N, 1=O, 2=S, 3=W)
	 * @param twirlsLeft
	 *            Wie viele weitere Segmente noch folgen duerfen
	 * 
	 */
	private void generateTwirl(int row, int col, int dir, int twirlsLeft) {
		twirlsLeft--;
		if (twirlsLeft < 0)
			return;
		// Der Schnoerkel wird segmentweise gebaut. Segmente haben eine
		// zufaellig bestimmte Wunschlaenge, die mindestens 3 betraegt
		// und auch von den Parcours-Dimensionen abhaengt:

		int desL = rand
				.nextInt(Math.min(halfmap[0].length, halfmap.length) / 4) + 3;
		int[] newC = new int[2];

		// Falls das nächste Feld in der gewünschten Richtung Boden ist...
		if (testNextFields(row, col, dir, 1)) {
			// ... Schnoerkel so lange verlaengern, bis nicht mehr
			// genuegend Raum da ist oder bis die gewuenschte Laenge
			// erreicht ist:
			while (testNextFields(row, col, dir, 3) && desL > 0) {
				newC = getNextCoordinate(row, col, dir);
				if (dir % 2 == 0) {
					halfmap[newC[0]][newC[1]] = WALLV;
				} else {
					halfmap[newC[0]][newC[1]] = WALLH;
				}
				row = newC[0];
				col = newC[1];
				desL--;
			}
			// Ansonsten ist dieser Schnoerkelabschnitt zu Ende
			// und der naechste beginnt:

			generateTwirl(row, col, twirlsLeft);
		} else {
			// Ansonsten wird versucht, je einen Schnoerkel
			// rechtwinklig dazu anzubringen:
			if (testNextFields(row, col, (dir + 1) % 4, 3)
					&& testNextFields(row, col, (dir - 1) % 4, 3)) {
				generateTwirl(row, col, (dir + 1) % 4, twirlsLeft);
				generateTwirl(row, col, (dir - 1) % 4, twirlsLeft);
			}
			// Irgendwann muss jeder Schnoerkel sein Ende haben 8-)
			else
				return;
		}
	}

	/**
	 * Testet, ob von einem Feld aus nach Norden, Sueden und Westen gesehen in
	 * einer bestimmten Tiefe nur Bodenfelder vorkommen. Es wird auch in die
	 * Breite geprueft: bei Tiefe 1 ist der gepruefte Streifen 1 Feld breit, bei
	 * 2 3 Felder und bei 3 oder mehr 5 Felder.
	 * 
	 * @param row
	 *            Zeile des Startfelds
	 * @param col
	 *            Spalte des Startfelds
	 * @param depth
	 *            Suchtiefe in Feldern
	 * @return true, wenn alle getesteten Felder Bodenfelder sind.
	 */
	private boolean testNextFields(int row, int col, int depth) {
		return (testNextFields(row, col, 0, depth)
				&& testNextFields(row, col, 2, depth) && testNextFields(row,
				col, 3, depth));
	}

	/**
	 * Testet, ob von einem Feld aus in eine bestimmte Richtung gesehen in einer
	 * bestimmten Tiefe nur Bodenfelder vorkommen. Es wird auch in die Breite
	 * geprueft: bei Tiefe 1 ist der gepruefte Streifen 1 Feld breit, bei 2 3
	 * Felder und bei 3 oder mehr 5 Felder.
	 * 
	 * @param row
	 *            Zeile des Startfelds
	 * @param col
	 *            Spalte des Startfelds
	 * @param dir
	 *            Richtung (0=N, 1=O, 2=S, 3 =W)
	 * @param depth
	 *            Suchtiefe in Feldern
	 * @return true, wenn alle getesteten Felder Bodenfelder sind.
	 */
	private boolean testNextFields(int row, int col, int dir, int depth) {
		// Offset sind die ebenfalls zu pruefenden Reihen parallel
		// zu den eigentlichen Feldern
		int offset = Math.max(0, Math.min(depth - 1, 2));

		boolean result = true;
		try {
			switch (dir) {
			case 0:
				for (int i = (-offset); i < (offset + 1); i++) {
					for (int j = 1; j <= depth; j++) {
						result = result && (halfmap[row - j][col + i] == FLOOR);
					}
				}
				break;
			case 1:
				for (int i = (-offset); i < (offset + 1); i++) {
					for (int j = 1; j <= depth; j++) {
						result = result && (halfmap[row + i][col + j] == FLOOR);
					}
				}
				break;
			case 2:
				for (int i = (-offset); i < (offset + 1); i++) {
					for (int j = 1; j <= depth; j++) {
						result = result && (halfmap[row + j][col + i] == FLOOR);
					}
				}
				break;
			case 3:
				for (int i = (-offset); i < (offset + 1); i++) {
					for (int j = 1; j <= depth; j++) {
						result = result && (halfmap[row + i][col - j] == FLOOR);
					}
				}
				break;
			default:
				result = false;
			}
		} catch (ArrayIndexOutOfBoundsException e) {

			// Pruefung freier Felder hat Grenzen des Parcours ueberschritten,
			// daher ist nicht genuegend Platz!
			result = false;

			// TODO: Verhindert Hindernisse ueber die Mittelachse?

		}
		return result;
	}

	/**
	 * Gibt die Koordinaten eines Felds zurueck, dass in angegebener Richtung
	 * vom uebergebenen Feld liegt.
	 * 
	 * @param row
	 *            Die Zeile des Ausgangsfelds
	 * @param col
	 *            Die Spalte des Ausgangsfelds
	 * @param dir
	 *            Richtung (0=N, 1=O, 2=S, 3 =W)
	 * @return Ein Tupel mit den neuen Koordinaten [Zeile|Spalte]
	 */
	private int[] getNextCoordinate(int row, int col, int dir) {
		int[] result = new int[2];
		switch (dir) {
		case 0:
			result[0] = row - 1;
			result[1] = col;
			break;
		case 1:
			result[0] = row;
			result[1] = col + 1;
			break;
		case 2:
			result[0] = row + 1;
			result[1] = col;
			break;
		case 3:
			result[0] = row;
			result[1] = col - 1;
			break;
		default:
			result[0] = -1;
			result[0] = -1;
		}
		return result;
	}

	private void generateFreeObstacles() {
		// Anzahl haengt von Dimension des Parcours und roughness ab:
		int obstNum = 2 * Math.max(width, height) / wallRoughness;
		int row, col;
		for (int i = 0; i < obstNum; i++) {
			row = rand.nextInt(height);
			col = rand.nextInt(width);
			// Ist das gefundene Feld leer und von freiem Raum umgeben?
			if ((halfmap[row][col] == FLOOR) && testNextFields(row, col, 2)) {
				generateTwirl(row, col, twirling + 3);
			}
		}
	}

	private void generateStart() {
		int offset = rand.nextInt(width - 3) + 2;
		int row = height - 2;
		map[row][offset] = START1;
		map[row][offset - 1] = WHITE;
		map[row][2 * width - 1 - offset] = START2;
		map[row][2 * width - offset] = WHITE;
	}

	private void mirror() {

		map = new char[height][width * 2];

		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				map[r][c] = halfmap[r][c];
				map[r][width * 2 - 1 - c] = halfmap[r][c];
			}
		}
	}

	private void perforate() {
		for (int r = 1; r < height - 1; r++) {
			for (int c = 1; c < width - 1; c++) {
				if (((halfmap[r][c] == WALL) || (halfmap[r][c] == WALLH) || (halfmap[r][c] == WALLV))
						&& (rand.nextInt(perforation) == 0)) {
					halfmap[r][c] = HOLE;
				}
			}
		}
	}

	/**
	 * Gibt einen Parcours auf der Konsole aus.
	 * 
	 * @param step
	 *            Bezeichnung des Generierungsschrittes für die Fehlersuche
	 * @param parc
	 *            Der Parcours
	 * 
	 */
	private void printParc(String step, char[][] parc) {
		System.out.println("\n" + step + "\n");
		System.out.println(parc2String(parc));
	}

	/**
	 * Formatiert einen Parcours für die Ausgabe auf der Konsole.
	 * 
	 * @param parc
	 *            Der Parcours
	 * 
	 * @return Der String
	 */
	private String parc2String(char[][] parc) {
		StringBuffer result = new StringBuffer();
		for (int r = 0; r < parc.length; r++) {
			for (int c = 0; c < parc[r].length; c++) {
				result.append(parc[r][c]);
			}
			result.append("\n");
		}
		return result.toString();
	}

	private String parc2XML(char[][] parc) {
		StringBuffer result = new StringBuffer();
		result.append(xmlHead);
		result.append("<line>");
		for (int r = 0; r < parc.length; r++) {
			result.append("<line>");
			for (int c = 0; c < parc[r].length; c++) {
				result.append(parc[r][c]);
			}
			result.append("</line>");
		}
		result.append(xmlTail);
		return result.toString();
	}
}
