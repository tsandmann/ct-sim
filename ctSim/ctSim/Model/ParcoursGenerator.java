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

	// TODO: performantere Waende einsetzen!!!
	
	public static final char FLOOR = ' ';

	public static final char WHITE = '.';

	public static final char WALL = 'X';

	public static final char HOLE = '0';

	public static final char LAMP = '*';

	public static final char GOAL = 'G';

	public static final char START1 = '1';

	public static final char START2 = '2';

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
	private int roughness;
	
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
			parcGen.generateParc();

		}
	}

	/**
	 * Diese Methode generiert das eigentliche Labyrinth. Parameter werden
	 * zufällig gesetzt.
	 */
	public void generateParc() {
		int w, h, r, t, p;
		w = rand.nextInt(10) + 6; // Breite zwischen 12 und 30 Felder
		h = rand.nextInt(19) + 12; // Hoehe zwischen 12 und 30 Felder
		r = 5; // TODO: zufällig variieren
		t = 3; // TODO: zufällig variieren
		p = 10; // TODO: zufällig variieren
		generateParc(w, h, r, t, p);
	}

	public void generateParc(int wi, int he, int ro, int tw, int pe) {
		width = wi;
		height = he;
		roughness = ro;
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
		
	}
	
	/**
	 * Versieht das Spielfeld mit einer Einfriedung:
	 */
	private void buildEnclosure() {

		// Nord- und Suedwand:
		for (int c = 1; c < width; c++) {
			halfmap[0][c] = WALL;
			halfmap[height - 1][c] = WALL;
		}

		// Westwand:
		for (int r = 1; r < height - 1; r++) {
			halfmap[r][0] = WALL;
		}

		// Die Lampen in die Ecken:
		halfmap[0][0] = LAMP;
		halfmap[height - 1][0] = LAMP;

		// Zielfeld ganz rechts oben:		
		halfmap[0][width-1]=GOAL;
		
	}

	/**
	 * Fuegt Hindernisse an die Waende an, so dass der Weg an der Wand entlang
	 * auf jeden Fall laenger ist als der kuerzeste Weg.
	 */
	private void roughenWalls() {
		// Die Anzahl der Hindernisse an der Wand berechnet sich durch 
		// Lange / roughness

		// Zuerst Westwand moeblieren:
		int obstNum = height/roughness;
		for (int i = 0; i < obstNum; i++) {
			addWallObstacle('W');
		}
		
		// Dann Nord- und Suedwand moeblieren:
		obstNum = width/roughness;
		for (int i = 0; i < obstNum; i++) {
			addWallObstacle('N');			
			addWallObstacle('S');
		}

	}

	/**
	 * Fuegt ein neues Hindernis direkt an der Wand hinzu
	 * 
	 * @param wall
	 *            Die Wand, die ein Hindernis erhalten soll (N, S, W)
	 */
	private void addWallObstacle(char wall) {

		int col, row;

		switch (wall) {
		case 'N':
			// erste Zeile,
			row = 0;
			// Spalte ist Zufall, haelt aber Sicherheitsabstand
			// von der Ecke und vom Zielfeld:
			col = rand.nextInt(width-5) + 3;
			generateTwirl(row, col, 2, twirling);
			break;
		case 'S':
			// zweite Zeile,
			row = height - 1;
			// Spalte ist Zufall, haelt aber Sicherheitsabstand
			// von der Ecke: 
			col = rand.nextInt(width-4) + 3;
			generateTwirl(row, col, 0, twirling);
			break;
		case 'W':
			// Reihe ist Zufall, haelt aber Sicherheitsabstand von der Ecke:
			row = rand.nextInt(height - 6) + 3;
			// erste Spalte  
			col = 0;
			generateTwirl(row, col, 1, twirling);
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
	 * @param twirlsLeft Wie viele weitere Segmente noch folgen duerfen
	 */
		
	private void generateTwirl(int row, int col, int twirlsLeft) {
		//Zufaellige Richtung: 0 = nach Norden 1 = nach Osten 2 = nach Sueden 3 =
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
	 * @param twirlsLeft Wie viele weitere Segmente noch folgen duerfen
	 * 				
	 */
	private void generateTwirl(int row, int col, int dir, int twirlsLeft) {
		twirlsLeft--;
		if (twirlsLeft<0) return;
		// Der Schnoerkel wird segmentweise gebaut. Segmente haben eine
		// zufaellig bestimmte Wunschlaenge, die mindestens 3 betraegt 
		// und auch von den Parcours-Dimensionen abhaengt:
		 
		int desL = rand.nextInt(Math.min(halfmap[0].length, halfmap.length)/4)+3;
		int[] newC = new int[2];
		
		// Falls das nächste Feld in der gewünschten Richtung Boden ist...
		if (testNextFields(row, col, dir, 1)){
			//... Schnoerkel so lange verlaengern, bis nicht mehr
			// genuegend Raum da ist oder bis die gewuenschte Laenge
			// erreicht ist:
			while (testNextFields(row, col, dir, 3) && desL > 0) {
				newC = getNextCoordinate(row, col, dir);
				halfmap[newC[0]][newC[1]] = WALL;
				row = newC[0];
				col = newC[1];
				desL--;
			}
			// Ansonsten ist dieser Schnoerkelabschnitt zu Ende 
			// und der naechste beginnt:

			generateTwirl(row, col, twirlsLeft);
		}
		else {
			// Ansonsten wird versucht, je einen Schnoerkel
			// rechtwinklig dazu anzubringen:
			if(testNextFields(row, col, (dir + 1) % 4, 3) && testNextFields(row, col, (dir - 1) % 4, 3)){
			generateTwirl(row, col, (dir + 1) % 4, twirlsLeft);
			generateTwirl(row, col, (dir - 1) % 4, twirlsLeft);
			}
			// Irgendwann muss jeder Schnoerkel sein Ende haben 8-)
			else return;				
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
	 * @param twirlsLeft Wie viele weitere Segmente noch folgen duerfen
	 */
	
	// TODO: Maximale Rekursionstiefe festlegen?
	

	/**
	 * Testet, ob von einem Feld aus nach Norden, Sueden und Westen gesehen
	 * in einer bestimmten Tiefe nur Bodenfelder vorkommen. 
	 * Es wird auch in die Breite geprueft: bei Tiefe 1 ist der gepruefte
	 * Streifen 1 Feld breit, bei 2 3 Felder und bei 3 oder mehr 5 Felder.
	 * @param row Zeile des Startfelds
	 * @param col Spalte des Startfelds
	 * @param depth Suchtiefe in Feldern
	 * @return true, wenn alle getesteten Felder Bodenfelder sind.
	 */		
	private boolean testNextFields(int row, int col, int depth) {
		return (testNextFields(row, col, 0, depth)&&testNextFields(row, col, 2, depth)&&testNextFields(row, col, 3, depth));
	}
	
	/**
	 * Testet, ob von einem Feld aus in eine bestimmte Richtung gesehen
	 * in einer bestimmten Tiefe nur Bodenfelder vorkommen. 
	 * Es wird auch in die Breite geprueft: bei Tiefe 1 ist der gepruefte
	 * Streifen 1 Feld breit, bei 2 3 Felder und bei 3 oder mehr 5 Felder.
	 * @param row Zeile des Startfelds
	 * @param col Spalte des Startfelds
	 * @param dir Richtung (0=N, 1=O, 2=S, 3 =W) 	
	 * @param depth Suchtiefe in Feldern
	 * @return true, wenn alle getesteten Felder Bodenfelder sind.
	 */		
	private boolean testNextFields(int row, int col, int dir, int depth) {
		// Offset sind die ebenfalls zu pruefenden Reihen parallel
		// zu den eigentlichen Feldern
		int offset = Math.max(0, Math.min(depth-1, 2));

		boolean result = true;
		try {
			switch (dir) {
			case 0:
				for (int i=(-offset); i<(offset+1); i++){
					for (int j=1; j<=depth; j++){
						result = result && (halfmap[row - j][col+i]==FLOOR);
					}
				}
				break;
			case 1:
				for (int i=(-offset); i<(offset+1); i++){
					for (int j=1; j<=depth; j++){
						result = result && (halfmap[row+i][col + j]==FLOOR);
					}
				}
				break;
			case 2:
				for (int i=(-offset); i<(offset+1); i++){
					for (int j=1; j<=depth; j++){
						result = result && (halfmap[row + j][col+i]==FLOOR);
					}
				}
				break;
			case 3:
				for (int i=(-offset); i<(offset+1); i++){
					for (int j=1; j<=depth; j++){
						result = result && (halfmap[row+i][col-j]==FLOOR);
					}
				}
				break;
			default:
				result = false;
			}
		} catch (RuntimeException e) {
			System.out.println("Wahrscheinlich ArrayOutOfBounds");
			// e.printStackTrace();
			
			// TODO: Wirft zu oft mit Exceptions und verhindert Hindernisse ueber die Mittelachse! 
			
			result = false;
		}
		return result;
	}

	/**
	 * Gibt die Koordinaten eines Felds zurueck, dass in angegebener
	 * Richtung vom uebergebenen Feld liegt.
	 * @param row Die Zeile des Ausgangsfelds
	 * @param col Die Spalte des Ausgangsfelds
	 * @param dir Richtung (0=N, 1=O, 2=S, 3 =W)
	 * @return Ein Tupel mit den neuen Koordinaten [Zeile|Spalte]
	 */
	private int[] getNextCoordinate(int row, int col, int dir){
		int[] result = new int[2];
			switch (dir) {
			case 0:
				result[0] = row - 1;
				result[1] = col;
				break;
			case 1:
				result[0] = row;
				result[1] = col+1;
				break;
			case 2:
				result[0] = row + 1;
				result[1] = col;
				break;
			case 3:
				result[0] = row;
				result[1] = col-1;
				break;
			default:
				result[0] = -1;
				result[0] = -1;
			}
		return result;		
	}

	private void generateFreeObstacles(){
		// Anzahl haengt von Dimension des Parcours und roughness ab:
		int obstNum = 2*Math.max(width, height)/roughness;
		int row, col;
		for(int i=0; i<obstNum; i++){
			row = rand.nextInt(height);
			col = rand.nextInt(width);
			// Ist das gefundene Feld leer und von freiem Raum umgeben?
			if ((halfmap[row][col]==FLOOR) && testNextFields(row, col, 2)){
				generateTwirl(row, col, twirling+3);
			}
		}
	}
	
	private void generateStart(){
		int offset = rand.nextInt(width-3)+2;
		int row = height-2;
		map [row][offset] = START1;
		map [row][offset-1] = WHITE;
		map [row][2*width-1-offset] = START2;
		map [row][2*width-offset] = WHITE;		
	}
	
	private void mirror(){

		map = new char[height][width*2];
		
		for (int r = 0; r < height; r++) {
			for (int c = 0; c < width; c++) {
				map[r][c] = halfmap[r][c];
				map[r][width*2-1-c] = halfmap[r][c];
			}
		}
	}
	
	private void perforate(){
		for (int r = 1; r < height-1; r++) {
			for (int c = 1; c < width-1; c++) {
				if ((halfmap[r][c]==WALL)&&(rand.nextInt(perforation)==0)){
					halfmap[r][c]=HOLE;
				}
			}
		}
	}
	
	/**
	 * Gibt einen Parcours auf der Konsole aus.
	 * 
	 * @param step
	 *            Bezeichnung des Generierungsschrittes für die Fehlersuche
	 * @param parc Der Parcours
	 * 		           
	 */
	public void printParc(String step, char[][]parc) {
		System.out.println("\n" + step + "\n");
		System.out.println(parc2String(parc));
	}

	/**
	 * Formatiert einen Parcours für die Ausgabe auf der Konsole.
	 * 
	 * @param parc Der Parcours
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
}
