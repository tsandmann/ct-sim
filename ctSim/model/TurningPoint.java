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

package ctSim.model;

import java.util.Vector;

import javax.vecmath.Vector2d;

/**
 * Diese Klasse beschreibt die Position eines Knicks im Pfad des Roboters,
 * wenn er an einem Hindernis abbiegt. Ausserdem enthält sie Methoden um die
 * kürzeste Verbindung in einem ungerichteten Graphen zu finden
 *
 * @author phophermi (mpraehofer@web.de)
 */
public class TurningPoint {
	/**
	 * <p>
	 * Der Abstand, wie weit der Mittelpunkt des Bots von den
	 * Wänden/Löchern/anderen Hindernissen hält, in Grid-Einheiten.
	 * </p>
	 * <p>
	 * 0.25 ist <strong>bei momentaner Block-Größe</strong> ein Bot-Radius.
	 * ("Block-Größe": siehe blockSizeInM in der Klasse Parcours.) 0.25 bedeutet also,
	 * dass dieser Wegfindungs-Algorithmus dem Bot erlaubt , bis zur Kollision an Wände
	 * heranzukommen.
	 * </p>
	 * <ol>
	 * <h3>Probleme:</h3>
	 * <li>Wenn dieser Wert zu groß ist (z.B. 0.25), und man den Weg bis zum Ziel bestimmt
	 * von der Position eines Bots aus, kann folgendes passieren:
	 * Bots, die mit ihrer Vorderseite ein bisschen über einem Loch stehen, sind nach Meinung
	 * dieses Algorithmus in ungültiger Position. Das heißt, dass
	 * {@link #getShortestPathTo(TurningPoint, int[][])} zurückliefert, es gäbe keinen gültigen
	 * Weg von der Botposition zum Ziel, obwohl laut {@link ThreeDBot} der Bot noch lange nicht
	 * in ein Loch gefallen ist.</li>
	 * <li> Es ist nicht empfehlenswert, diesen Wert auf 0 zu setzen.
	 * Angenommen, solche Stellen kommen auf der Karte (XML) vor: <br>#<br>#===<br>#<br>
	 * Ein Weg zum Ziel wird dann auch gefunden in der zweiten Zeile zwischen den Blöcken "#" und "=",
	 * wo der Bot natürlich nicht durchkommt. Dieser Wegfindungsalgorithmus ist für den Wert 0
	 * also praktisch nutzlos, da er auch Wege durch Wände u.dgl. sucht.</li>
	 * </ol>
	 *
	 * Mist: Dieser Algorithmus hat ganz eigene Vorstellungen davon, was "Bot ist in ein Loch gefallen"
	 * heißt (nämlich Bot-Zentrum ist näher als distFromCorner an einer Ecke). Es wäre besser,
	 * wenn das Model und diese Klasse einen Bot unter den gleichen Umständen als "im Loch" betrachten,
	 * und nicht unter subtil unterschiedlichen Umständen. Variable distFromCorner ist wirklich
	 * problematisch und sollte anders gelöst werden - siehe zugehörige Doku
	 */
	public static final double distFromCorner = 0.05;

	/**
	 * Eingezeichnete Linienbreite der kuerzesten Verbindung in grid-Einheiten
	 * (0.5 entspricht bot-Durchmesser)
	 */
	public static final float lineWidth = 0.5f;

	/**
	 * z-Koordinate der Linie: 0 entspricht Bodenhöhe, dann wird die Linie
	 * allerdings von Start- und Zielfeld überdeckt. Nicht getestet wurde ob
	 * der bot stolpert, wenn height &gt; 0
	 */
	public static final float height = 0.0f;

	/** effektiv unendlich */
	public static final double infinity = 1e30;

	/** Basis des Vektors */
	Vector2d pos;

	/**
	 * @param x	X-Koordinate
	 * @param y	Y-Koordinate
	 */
	TurningPoint(double x, double y) {
		pos = new Vector2d();
		pos.x = x;
		pos.y = y;
	}

	/**
	 * @param p	x/Y als Vektor
	 */
	TurningPoint(Vector2d p) {
		pos = new Vector2d(p);
	}

	/**
	 * Testet ob this mit p2 in der parcoursMapSimple auf direktem Wege vom Bot
	 * erreicht werden können
	 *
	 * @param p2				Zu testender Punkt
	 * @param parcoursMapSimple	ParcoursMap
	 * @return true wenn direkt erreichbar
	 */
	boolean isDirectlyConnectedTo(TurningPoint p2, int[][] parcoursMapSimple) {
		Vector2d direction = new Vector2d(p2.pos);
		direction.sub(this.pos);
		double rectL = (this.pos.x < p2.pos.x ? this.pos.x : p2.pos.x)
				- (1. + distFromCorner);
		double rectR = (this.pos.x > p2.pos.x ? this.pos.x : p2.pos.x)
				+ distFromCorner;
		double rectD = (this.pos.y < p2.pos.y ? this.pos.y : p2.pos.y)
				- (1. + distFromCorner);
		double rectU = (this.pos.y > p2.pos.y ? this.pos.y : p2.pos.y)
				+ distFromCorner;
		for (int x = 0; x < parcoursMapSimple.length; x++) {
			for (int y = 0; y < parcoursMapSimple[0].length; y++) {
				if (parcoursMapSimple[x][y] == 1 && x > rectL && x < rectR
						&& y > rectD && y < rectU) {
					Vector2d corner = new Vector2d(x - distFromCorner, y
							- distFromCorner);
					corner.sub(this.pos);
					double whichside = direction.x * corner.y - direction.y
							* corner.x;
					corner.set(x + (1. + distFromCorner), y - distFromCorner);
					corner.sub(this.pos);
					double sameside = direction.x * corner.y - direction.y
							* corner.x;
					if (whichside == 0)
						whichside = sameside;
					else if (sameside * whichside < 0)
						return false;
					corner.set(x + (1. + distFromCorner), y
							+ (1. + distFromCorner));
					corner.sub(this.pos);
					sameside = direction.x * corner.y - direction.y * corner.x;
					if (whichside == 0)
						whichside = sameside;
					else if (sameside * whichside < 0)
						return false;
					corner.set(x - distFromCorner, y + (1. + distFromCorner));
					corner.sub(this.pos);
					sameside = direction.x * corner.y - direction.y * corner.x;
					if (whichside == 0)
						whichside = sameside;
					else if (sameside * whichside < 0)
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Euklidischer Abstand von this zu p2
	 *
	 * @param p2
	 * @return Euklidischer Abstand von this zu p2
	 */
	double getDistanceTo(TurningPoint p2) {
		Vector2d h = new Vector2d(p2.pos);
		h.sub(this.pos);
		return h.length();
	}

	/**
	 * gibt die Länge des durch path gegebenen Streckenzugs bezüglich der
	 * Inzidenz- (und Abstands-)Matrix M zurück
	 *
	 * @param path
	 * @param M
	 * @return Länge von path bezüglich M
	 */
	static double getLengthOfPath(Vector<Integer> path, double[][] M) {
		double l = 0;
		if (path == null)
			return infinity;
		else {
			if (path.size() < 2)
				return 0;
			else {
				for (int i = 1; i < path.size(); i++)
					l += M[path.get(i)][path.get(i - 1)];
			}
		}
		return l;
	}

	/**
	 * Gibt die Länge des durch path gegebenen Streckenzugs zurück
	 *
	 * @param path	der Streckenzug
	 * @return länge von path bezüglich M
	 */
	static double getLengthOfPath(Vector<TurningPoint> path) {
		double l = 0;
		if (path == null)
			return infinity;
		else {
			if (path.size() < 2)
				return 0;
			else {
				for (int i = 1; i < path.size(); i++)
					l += path.get(i - 1).getDistanceTo(path.get(i));
			}
		}
		return l;
	}

	/**
	 * die kürzeste Verbindung von this zu p2 wird rekursiv bestimmt
	 *
	 * @param s			?
	 * @param f			?
	 * @param initPath	der bisher untersuchte Weg
	 * @param cutoff	ist der untersuchte Weg länger wird abgebrochen
	 * @param M			Inzidenzmatrix
	 * @param N			?
	 * @return die kürzeste Verbindung von this zu p2 unter Vermeidung von initialPath
	 */
	Vector<Integer> getShortestPath(int s, int f, Vector<Integer> initPath,
			double cutoff, double[][] M, int[][] N) {
		if (initPath.contains(s) || initPath.contains(f)) {
			System.out.println("error");
			return null;
		}
		if (M[s][f] < infinity) {
			if (M[s][f] < cutoff) {
				Vector<Integer> path = new Vector<>();
				path.add(s);
				path.add(f);
				return path;
			} else
				return null;
		} else {
			Vector<Integer> minPath = null;
			double minDist = cutoff;
			Vector<Integer> newInitPath = new Vector<>(initPath);
			newInitPath.add(s);
			for (int i = 0; i < N[s].length; i++) {
				if (!initPath.contains(N[s][i]) && M[s][N[s][i]] < cutoff) {
					boolean shortcutFlag = false;
					for (int j = 0; !shortcutFlag && j < N[N[s][i]].length; j++) {
						if (initPath.contains(N[N[s][i]][j])) {
							shortcutFlag = true;
						}
					}
					if (!shortcutFlag) {
						Vector<Integer> helpPath = this.getShortestPath(
								N[s][i], f, newInitPath, minDist
								- M[s][N[s][i]], M, N);
						double helpDist = getLengthOfPath(helpPath, M)
								+ M[s][N[s][i]];
						if (helpDist < minDist) {
							minDist = helpDist;
							minPath = helpPath;
						}
					}
				}
			}
			if (minPath != null)
				minPath.insertElementAt(s, 0);
			return minPath;
		}
	}

	/**
	 * Gibt einen Polygonzug der mit Breite und einer Spitze versehen Linie von
	 * this zu p2 zurück, zur Weiterverwendung in createLine()
	 *
	 * @param p2
	 * @return {x1,y1,z1,x2,y2,z2,...,x6,y6,z6}
	 */
	float[] returnLineTo(TurningPoint p2) {
		Vector2d offset = new Vector2d(0.5, 0.5);
		Vector2d length = new Vector2d(p2.pos);
		length.sub(this.pos);
		Vector2d halfWidth = new Vector2d(-length.y, length.x);
		halfWidth.normalize();
		halfWidth.scale(lineWidth / 2);
		Vector2d corner = new Vector2d(this.pos);
		// corner.add(new Vector2d(0.5,0.5));
		corner.sub(halfWidth);
		corner.sub(offset);
		float[] polygon = new float[18];
		polygon[0] = (float) corner.x;
		polygon[1] = (float) corner.y;
		polygon[2] = height;
		corner.add(length);
		polygon[3] = (float) corner.x;
		polygon[4] = (float) corner.y;
		polygon[5] = height;
		corner.add(halfWidth);
		corner.add(halfWidth);
		polygon[6] = (float) corner.x;
		polygon[7] = (float) corner.y;
		polygon[8] = height;
		corner.sub(length);
		polygon[9] = (float) corner.x;
		polygon[10] = (float) corner.y;
		polygon[11] = height;
		corner.sub(halfWidth);
		Vector2d head = new Vector2d(length);
		head.normalize();
		head.scale(-lineWidth / 2);
		corner.add(head);
		polygon[12] = (float) corner.x;
		polygon[13] = (float) corner.y;
		polygon[14] = height;
		corner.sub(halfWidth);
		corner.sub(head);
		polygon[15] = (float) corner.x;
		polygon[16] = (float) corner.y;
		polygon[17] = height;
		return polygon;
	}

	/**
	 * Findet alle möglichen Eckpunkte der kürzesten Verbindung
	 *
	 * @param parcoursMapSimple
	 * @return eine Liste möglicher Eckpunkte für die kürzeste Verbindung
	 */
	Vector<TurningPoint> findTurningPoints(int[][] parcoursMapSimple) {
		Vector<TurningPoint> turningPoints = new Vector<>();
		// turningPoints.add(new TurningPoint(start));
		// turningPoints.add(new TurningPoint(finish));
		//		int count = 0;
		for (int i = 1; i < parcoursMapSimple[0].length; i++) {
			for (int j = 1; j < parcoursMapSimple.length; j++) {
				if (parcoursMapSimple[j - 1][i - 1] == 1
						&& parcoursMapSimple[j - 1][i] == 0
						&& parcoursMapSimple[j][i - 1] == 0
						&& parcoursMapSimple[j][i] == 0) {
					turningPoints.add(new TurningPoint(j + distFromCorner, i
							+ distFromCorner));
					//					count++;
				}
				if (parcoursMapSimple[j - 1][i - 1] == 0
						&& parcoursMapSimple[j - 1][i] == 1
						&& parcoursMapSimple[j][i - 1] == 0
						&& parcoursMapSimple[j][i] == 0) {
					turningPoints.add(new TurningPoint(j + distFromCorner, i
							- distFromCorner));
					//					count++;
				}
				if (parcoursMapSimple[j - 1][i - 1] == 0
						&& parcoursMapSimple[j - 1][i] == 0
						&& parcoursMapSimple[j][i - 1] == 1
						&& parcoursMapSimple[j][i] == 0) {
					turningPoints.add(new TurningPoint(j - distFromCorner, i
							+ distFromCorner));
					//					count++;
				}
				if (parcoursMapSimple[j - 1][i - 1] == 0
						&& parcoursMapSimple[j - 1][i] == 0
						&& parcoursMapSimple[j][i - 1] == 0
						&& parcoursMapSimple[j][i] == 1) {
					turningPoints.add(new TurningPoint(j - distFromCorner, i
							- distFromCorner));
					//					count++;
				}
			}
		}
		return turningPoints;
	}

	/**
	 * Erzeugt die Incidenzmatrix der Punkteliste bezüglich parcoursMapSimple
	 *
	 * @param turningPoints
	 * @param parcoursMapSimple
	 * @return double[i][j] ist der Abstand von Punkt i zu j, falls direkt verbunden,
	 * 			ansonsten 10e30 (defacto unendlich)
	 */
	double[][] createIncidenceMatrix(Vector<TurningPoint> turningPoints,
			int[][] parcoursMapSimple) {
		int count = turningPoints.size();
		double[][] incidenceMatrix = new double[count][count];
		for (int i = 0; i < count - 1; i++) {
			for (int j = i; j < count; j++) {
				if (i == j)
					incidenceMatrix[i][i] = 0;
				else {
					if (turningPoints.get(i).isDirectlyConnectedTo(
							turningPoints.get(j), parcoursMapSimple)) {
						incidenceMatrix[i][j] = incidenceMatrix[j][i] = turningPoints
								.get(i).getDistanceTo(turningPoints.get(j));
						// System.out.print("*");
					} else {
						incidenceMatrix[i][j] = incidenceMatrix[j][i] = infinity;
						// System.out.print("0");
					}
				}
			}
			// System.out.println();
		}
		return incidenceMatrix;
	}

	/**
	 * Gibt eine Liste aller Punkte zusammen mit ihren direkten Nachbarn zurück
	 *
	 * @param incidenceMatrix
	 * @return int[i].length ist die Koordinationszahl des i-ten Punktes
	 */
	int[][] getDirectNeighbors(double[][] incidenceMatrix) {
		int count = incidenceMatrix.length;
		int[][] neighbors = new int[count][];
		for (int i = 0; i < count; i++) {
			// System.out.print(turningPoints.get(i).pos);System.out.print(":");
			int coordination = 0;
			for (int j = 0; j < count; j++) {
				if (i != j && incidenceMatrix[i][j] < infinity)
					coordination++;
			}
			neighbors[i] = new int[coordination];
			coordination = 0;
			for (int j = 0; j < count; j++) {
				if (i != j && incidenceMatrix[i][j] < infinity) {
					neighbors[i][coordination] = j;
					coordination++;
					// System.out.print(turningPoints.get(j).pos);
				}
			}
			// System.out.println(",");
		}
		return neighbors;
	}

	/**
	 * Gibt eine Folge von Punkten zurück, die den kürzesten Weg von
	 * <code>this</code> nach <code>finish</code> beschreiben, den der Bot
	 * im Labyrinth <code>parcoursMapSimple</code> durchfahren kann.
	 *
	 * @param finish
	 * 				Der Punkt, zu dem der kürzeste Weg bestimmt werden soll;
	 * 				typischerweise das Zielfeld des Labyrinths.
	 * @param parcoursMapSimple
	 * 				Vereinfachte Karte des Parcours: 0 = befahrbar, 1 = Hindernis
	 * @return Eckpunkte der kürzesten Verbindung von <code>this</code> zu finish.
	 * 			Die Liste kann leer sein.
	 */
	Vector<TurningPoint> getShortestPathTo(TurningPoint finish,
			int[][] parcoursMapSimple) {
		Vector<TurningPoint> turningPoints = this
				.findTurningPoints(parcoursMapSimple);
		turningPoints.insertElementAt(finish, 0);
		turningPoints.insertElementAt(this, 0);
		// Erstellen der Inzidenzmatrix des ungerichteten Graphen
		double[][] incidenceMatrix = this.createIncidenceMatrix(turningPoints,
				parcoursMapSimple);
		// Erstellen der Tabelle der jeweils direkt erreichbaren Nachbarn
		int[][] neighbors = this.getDirectNeighbors(incidenceMatrix);
		// finde die kürzeste Verbindung
		Vector<Integer> shortestPath = turningPoints.get(0).getShortestPath(0,
				1, new Vector<Integer>(), 1e30, incidenceMatrix, neighbors);
		Vector<TurningPoint> shortest = new Vector<>();
		if (shortestPath != null) {
			for (int i : shortestPath) {
				shortest.add(turningPoints.get(i));
			}
		}
		return shortest;
	}
}
