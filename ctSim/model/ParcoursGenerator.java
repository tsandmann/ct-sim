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

package ctSim.model;

import java.util.Random;

/**
 * Parcours-Generator fuer den c't-Sim-Wettbewerb
 * 
 * @author pek (pek@heise.de)
 * 
 */

public class ParcoursGenerator {

	/**
	 * Zeichen fuer normalen Boden
	 */
	private static final char FLOOR = ' ';

	/**
	 * Zeichen fuer hellen Boden
	 */
	private static final char WHITE = '.';

	/**
	 * Zeichen fuer einzelnes Wandstueck
	 */
	private static final char WALL = 'X';

	/**
	 * Zeichen fuer horizontales Wandstueck
	 */
	private static final char WALLH = '=';

	/**
	 * Zeichen fuer vertikales Wandstueck
	 */
	private static final char WALLV = '#';

	/**
	 * Zeichen fuer Loch
	 */
	private static final char HOLE = 'L';

	/**
	 * Zeichen fuer Lampe
	 */
	private static final char LAMP = '*';

	/**
	 * Zeichen fuer Zielfeld
	 */
	private static final char GOAL = 'Z';

	/**
	 * Zeichen fuer linkes Startfeld
	 */
	private static final char START1 = '1';

	/**
	 * Zeichen fuer rechtes Startfeld
	 */
	private static final char START2 = '2';

	/**
	 * XML-String -- Anfang der Parcours-Datei
	 */
	private static final String xmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE world SYSTEM \"parcours.dtd\">\n"
			+ "<world>\n"
			+ "	<description>Dieses ist ein automatisch generierter Beispielparcours fuer den c't-Sim-Wettbewerb.</description>\n"
			+ "	<parcours>\n";

	/*
	 * XML-String -- Ende der Parcours-Datei
	 * TODO: Boden bleibt leer, irgendwas geht mit dem Leerzeichen schief... 
	 */
	private static final String xmlTail = "	</parcours>\n"
			+ "	<optics>\n"
			+ "		<appearance type=\"X\">\n"
			+ "			<description>quadratische Wand</description>\n"
			+ "			<texture>textures/rock_wall.jpg</texture>\n"
			+ "			<color>#999999</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"#\">\n"
			+ "			<description>senkrechte Wand</description>\n"
			+ "			<clone>X</clone>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"=\">\n"
			+ "			<description>wagrechte Wand</description>\n"
			+ "			<clone>X</clone>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\".\">\n"
			+ "			<description>Fussboden im Eingangsbereich</description>\n"
			+ "			<color type=\"ambient\">#FFFFFF</color>\n"
			+ "			<color type=\"diffuse\">#FFFFFF</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\" \">\n"
			+ "			<description>Fussboden im Labyrinth</description>\n"
			+ "			<color type=\"ambient\">#606060</color>\n"
			+ "			<color type=\"diffuse\">#606060</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"1\">\n"
			+ "			<description>Fussboden des Startfeldes 1</description>\n"
			+ "			<color type=\"ambient\">#993030</color>\n"
			+ "			<color type=\"diffuse\">#993030</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"2\">\n"
			+ "			<description>Fussboden des Startfeldes 2</description>\n"
			+ "			<color type=\"ambient\">#000099</color>\n"
			+ "			<color type=\"diffuse\">#000099</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"0\">\n"
			+ "			<description>Fussboden des Default-Startfeldes</description>\n"
			+ "			<clone>.</clone>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"Z\">\n"
			+ "			<description>Fussboden des Zielfeldes 0</description>\n"
			+ "			<color type=\"ambient\">#66FF00</color>\n"
			+ "			<color type=\"diffuse\">#66FF00</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"*\"><description>Lichtkugel</description>\n"
			+ "			<color type=\"emmissive\">#FFFF90</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"-\"><description>Linie</description>\n"
			+ "			<color type=\"ambient\">#000000</color>\n"
			+ "			<color type=\"diffuse\">#000000</color>\n"
			+ "			<color type=\"specular\">#000000</color>\n"
			+ "			<color type=\"emmissive\">#000000</color>\n"
			+ "		</appearance>\n"
			+ "		<appearance type=\"|\"><description>Linie</description>\n"
			+ "			<clone>-</clone>\n" + "		</appearance>\n"
			+ "		<appearance type=\"/\">\n"
			+ "			<description>Linie</description>\n" + "			<clone>-</clone>\n"
			+ "		</appearance>\n" + "		<appearance type=\"\\\">\n"
			+ "			<description>Linie</description>\n" + "			<clone>-</clone>\n"
			+ "		</appearance>\n" + "		<appearance type=\"+\">\n"
			+ "			<description>Linie</description>\n" + "			<clone>-</clone>\n"
			+ "		</appearance>\n" + "		<appearance type=\"~\">\n"
			+ "			<description>Linie</description>\n" + "			<clone>-</clone>\n"
			+ "		</appearance>\n" + "	</optics>\n" + "</world>\n";

	/*
	 * Die Karte ist ein char-Array, erste Dimension die Zeilennummer, die
	 * zweite die Spaltennummer:
	 */
	private char[][] map;

	/*
	 * Die Karte wird allerdings zurerst nur zur Haelfte generiert, ebenfalls
	 * als char-Array, erste Dimension die Zeilennummer, die zweite die
	 * Spaltennummer:
	 */
	private char[][] halfmap;

	/*
	 * Die Groesse des Labyrinths in Feldern; die Breite ist die Breite der
	 * halben Karte!
	 */
	private int width;

	private int height;

	/*
	 * Zufallszahlengenerator
	 */
	private Random rand;

	/*
	 * Parameter fuer die Erzeugung: Verschnoerkelungsfaktor; Anzahl der
	 * angestrebten Segmente pro Hindernis
	 */
	private int twirling;

	/*
	 * Hindernisdichte an der Wand; klein = rauh
	 */
	private int wallRoughness;

	/*
	 * Verhaeltnis der Hindernismenge an der Wand zu solchen in der Mitte
	 */
	private int innerRoughness; // TODO: einbauen!

	/*
	 * Lochanteil; jedes n-te Wandstück wird durch Loch ersetzt
	 */
	private int perforation;

	/**
	 * Der Konstruktor
	 */
	public ParcoursGenerator() {
		rand = new Random();
	}

	/**
	 * Diese main-Methode dient nur dem Debugging. Sie generiert 20 Parcours mit
	 * zufaelligen Parametern und gibt sie auf der Konsole aus.
	 * 
	 * @param args
	 *            Keine Argumente
	 */
	public static void main(String[] args) {
		for (int i = 0; i < 20; i++) {
			ParcoursGenerator parcGen = new ParcoursGenerator();
			System.out.println(parcGen.generateParc());
		}
	}

	/**
	 * Diese Methode generiert das eigentliche Labyrinth. Parameter werden
	 * innerhalb fester Grenzen zufaellig gesetzt: Breite zwischen 12 und 30
	 * Feldern Hoehe zwischen 12 und 30 Feldern wallRoughness = 5 innerRoughness =
	 * 5 twirling = 3 perforation = 10
	 * 
	 * @return Der Parcours als XML-String
	 */
	public String generateParc() {
		// Generiere zufaellige Werte fuer die Parameter:
		int w, h, wr, ir, t, p;
		w = rand.nextInt(10) + 6; // Breite zwischen 12 und 30 Felder
		h = rand.nextInt(19) + 12; // Hoehe zwischen 12 und 30 Felder
		wr = 5; // TODO: zufällig variieren
		ir = 5; // TODO: zufällig variieren
		t = 3; // TODO: zufällig variieren
		p = 10; // TODO: zufällig variieren
		// Rufe Methode mit den Parametern auf:
		return generateParc(w, h, ir, wr, t, p);
	}

	/**
	 * Diese Methode generiert das eigentliche Labyrinth.
	 * 
	 * @param wi
	 *            Halbe Breite des Parcours in Feldern
	 * @param he
	 *            Hoehe des Parcours in Feldern
	 * @param wr
	 *            Hindernisdichte an der Wand; klein = rauh
	 * @param ir
	 *            Verhaeltnis der Hindernismenge an der Wand zu solchen in der
	 *            Mitte
	 * @param tw
	 *            Verschnoerkelungsfaktor; Anzahl der angestrebten Segmente pro
	 *            Hindernis
	 * @param pe
	 *            Lochanteil; jedes n-te Wandstück wird durch Loch ersetzt
	 * @return Der Parcours als XML-String
	 */
	public String generateParc(int wi, int he, int wr, int ir, int tw, int pe) {
		width = wi;
		height = he;
		wallRoughness = wr;
		innerRoughness = ir;
		twirling = tw;
		perforation = pe;

		/*
		 * Zunaechst wird nur die halbe Karte gebaut.
		 * 
		 * Erste Dimension ist die Zeilennummer und zweite die Spaltennummer,
		 * daher erst Hoehe und dann Breite:
		 */

		halfmap = new char[height][width];

		// Mit Leerzeichen fuellen:
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				halfmap[r][c] = FLOOR;
			}
		}

		// Dann die Einfassung bauen:
		buildEnclosure();
		// Dann Hindernisse an den Waenden anfuegen:
		roughenWalls();
		// Dann freie Hindernisse bauen:
		generateFreeObstacles();
		// Einzelne Hindernisse durch Loecher ersetzen:
		perforate();
		// Fehlende Haelfte rekonstruieren:
		mirror();
		// Startfelder einbauen:
		generateStart();
		// Einmal ausgeben:
		printParc("", map); // TODO: Ausgabe entfernen!!
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
		/*
		 * Die Anzahl der Hindernisse an der Wand berechnet sich durch Laenge /
		 * wallRoughness, allerdings im Osten durch Laenge /
		 * (wallRoughness/innerRoughness), weil im Inneren des Labyrinths
		 * weniger Hindernisse als am Rand vorkommen sollen.
		 * 
		 * Zuerst Ostwand moeblieren (= Hindernisse in die Mitte einfuegen):
		 */
		int obstNum = height / (wallRoughness / innerRoughness); 
		// TODO: Hier passiert irgendwo Mist!
		for (int i = 0; i < obstNum; i++) {
			addWallObstacle('E');
		}

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
	 * Fuegt ein neues Hindernis an einen zufaelligen Ort direkt an der
	 * betreffenden Wand hinzu, sofern dort Platz genug vorhanden ist.
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
	 * "Verschnoerkelt" ein Hindernis in zufaellige Richtung. Die Methode ruft
	 * sich rekursiv selbst auf.
	 * 
	 * @param row
	 *            Zeile der Startkoordinate
	 * @param col
	 *            Spalte der Startkoordinate
	 * 
	 * @param twirlsLeft
	 *            Wie viele weitere Segmente (rekursive Methodenaufrufe) noch
	 *            folgen (duerfen)
	 */

	private void generateTwirl(int row, int col, int twirlsLeft) {
		// Zufaellige Richtung: 0 = nach Norden 1 = nach Osten
		// 2 = nach Sueden 3= nach Westen
		int dir = rand.nextInt(4);
		// Dann Methode mit diesen Parametern aufrufen:
		generateTwirl(row, col, dir, twirlsLeft);
	}

	/**
	 * "Verschnoerkelt" ein Hindernis in zufaellige Richtung. Die Methode ruft
	 * sich rekursiv selbst auf.
	 * 
	 * 
	 * @param row
	 *            Zeile der Startkoordinate
	 * @param col
	 *            Spalte der Startkoordinate
	 * @param dir
	 *            Richtung (0=N, 1=O, 2=S, 3=W)
	 * @param twirlsLeft
	 *            Wie viele weitere Segmente (rekursive Methodenaufrufe) noch
	 *            folgen (duerfen)
	 * 
	 */
	private void generateTwirl(int row, int col, int dir, int twirlsLeft) {
		twirlsLeft--;
		if (twirlsLeft < 0)
			return;
		/*
		 * Der Schnoerkel wird segmentweise gebaut. Segmente haben eine
		 * zufaellig bestimmte Wunschlaenge, die mindestens 3 betraegt und auch
		 * von den Parcours-Dimensionen abhaengt:
		 */
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
			// damit ist definitiv nicht genuegend Platz!
			result = false;

			// TODO: Das verhindert Hindernisse ueber die Mittelachse, evt.
			// nochmal
			// Fallunterscheidung einfuegen und Hindernisse nach Osten bis zum
			// Rand
			// fortsetzen.
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

	/**
	 * Fuegt dem Parcours Hindernisse ohne Wandkontakt hinzu. Hindernisse bauen
	 * garantiert keine Wege zu und halten Abstand von allen vorhandenen
	 * Hindernissen.
	 */
	private void generateFreeObstacles() {
		// TODO: von innerRoughness abhaengig machen!!
		// Anzahl haengt von Dimension des Parcours und roughness ab:
		int obstNum = 2 * Math.max(width, height) / wallRoughness;
		int row, col;
		for (int i = 0; i < obstNum; i++) {
			row = rand.nextInt(height);
			col = rand.nextInt(width);
			// Ist das gefundene Feld leer und von freiem Raum umgeben?
			if ((halfmap[row][col] == FLOOR) && testNextFields(row, col, 2)) {
				// Dann einfach mit extra langen Schnoerkeln anfangen,
				generateTwirl(row, col, twirling + 3);
			}
		}
	}

	/**
	 * Fuegt die Startfelder hinzu.
	 */
	private void generateStart() {
		int offset = rand.nextInt(width - 3) + 2;
		int row = height - 2;
		map[row][offset] = START1;
		map[row][offset - 1] = WHITE;
		map[row][2 * width - 1 - offset] = START2;
		map[row][2 * width - offset] = WHITE;
		// Sicherheitsabstand einhalten:
		map[row - 1][offset] = FLOOR;
		map[row - 1][offset - 1] = FLOOR;
		map[row - 1][2 * width - 1 - offset] = FLOOR;
		map[row - 1][2 * width - offset] = FLOOR;
	}

	/**
	 * Spiegelt den halben Parcours aus halfmap in map
	 */
	private void mirror() {

		map = new char[height][width * 2];

		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				map[r][c] = halfmap[r][c];
				map[r][width * 2 - 1 - c] = halfmap[r][c];
			}
		}
	}

	/**
	 * Ersetzt je nach Faktor perforation jedes n-te Wandfeld durch ein Loch
	 */
	private void perforate() {
		for (int r = 1; r < height - 1; r++) {
			for (int c = 1; c < width - 1; c++) {
				// Es gibt so viele Sorten von Waenden... 8-)
				if ((rand.nextInt(perforation) == 0)
						&& ((halfmap[r][c] == WALL) || (halfmap[r][c] == WALLH) || (halfmap[r][c] == WALLV))) {
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
	 * @return Der Ausgabestring
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

	/**
	 * Schreibt einen Parcours in einen XML-String
	 * 
	 * @param parc
	 *            Der Parcours
	 * @return Der XML-String
	 */
	private String parc2XML(char[][] parc) {
		StringBuffer result = new StringBuffer();
		result.append(xmlHead);
		for (int r = 0; r < parc.length; r++) {
			result.append("		<line>");
			for (int c = 0; c < parc[r].length; c++) {
				result.append(parc[r][c]);
			}
			result.append("</line>\n");
		}
		result.append(xmlTail);
		return result.toString();
	}
}
