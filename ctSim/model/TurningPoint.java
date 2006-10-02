package ctSim.model;

import java.util.Vector;
import javax.vecmath.Vector2d;

/**
 * Diese Klasse beschreibt die Position eines Knicks im Pfad des Roboters, wenn
 * er an einem Hindernis abbiegt. Ausserdem enthaelt sie Methoden um die
 * Kuerzeste Verbindung in einem ungerichteten Graphen zu finden
 * 
 * @author phophermi (mpraehofer@web.de)
 * 
 */
public class TurningPoint {
	/**
	 * Der vertikale bzw. horizontale Abstand den der Mittelpunkt des Bots von
	 * den Waenden haelt, in grid-Einheiten. 0.25 bedeutet kein
	 * Sicherheitsabstand
	 */
	public static final double distFromCorner = 0.25;

	/**
	 * Eingezeichnete Linienbreite der kuerzesten Verbindung in grid-Einheiten
	 * (0.5 entspricht bot-Durchmesser)
	 */
	public static final float lineWidth = 0.5f;

	/**
	 * z-Koordinate der Linie: 0 entspricht Bodenhoehe, dann wird die Linie
	 * allerdings von Start- und Zielfeld ueberdeckt. Nicht getestet wurde ob
	 * der bot stolpert, wenn height>0
	 */
	public static final float height = 0.0f;

	/** effektiv unendlich */
	public static final double infinity = 1e30;

	/** Basis des Vektors */
	Vector2d pos;

	/**
	 * Konstruktor
	 * 
	 * @param x
	 * @param y
	 */
	TurningPoint(double x, double y) {
		pos = new Vector2d();
		pos.x = x;
		pos.y = y;
	}

	/**
	 * Konstruktor
	 * 
	 * @param x
	 * @param y
	 */
	TurningPoint(Vector2d p) {
		pos = new Vector2d(p);
	}
	
	
	/**
	 * Testet ob this mit p2 in der parcoursMapSimple auf direktem Wege vom Bot
	 * erreicht werden koennen
	 * 
	 * @param p2
	 *            Zu testender Punkt
	 * @param parcoursMapSimple
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
	 * gibt die Laenge des durch path gegebenen Streckenzugs bezueglich der
	 * Inzidenz- (und Abstands-)Matrix M zurueck
	 * 
	 * @param path
	 * @param M
	 * @return laenge von path bezueglich M
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
	 * gibt die Laenge des durch path gegebenen Streckenzugs zurueck
	 * 
	 * @param path
	 * @param M
	 * @return laenge von path bezueglich M
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
	 * die kuerzeste Verbindung von this zu p2 wird rekursiv bestimmt
	 * 
	 * @param p2
	 *            Zielpunkt
	 * @param initialPath
	 *            der bisher untersuchte Weg
	 * @param upperbound
	 *            ist der untersuchte Weg laenger wird abgebrochen
	 * @param parcoursMapSimple
	 *            Parcours-Array, 0 befahrbar, 1 Hindernis
	 * 
	 * @return die kuerzeste Verbindung von this zu p2 unter Vermeidung von
	 *         initialPath
	 */
	Vector<Integer> getShortestPath(int s, int f, Vector<Integer> initPath,
			double cutoff, double[][] M, int[][] N) {
		if (initPath.contains(s) || initPath.contains(f)) {
			System.out.println("error");
			return null;
		}
		if (M[s][f] < infinity) {
			if (M[s][f] < cutoff) {
				Vector<Integer> path = new Vector<Integer>();
				path.add(s);
				path.add(f);
				return path;
			} else
				return null;
		} else {
			Vector<Integer> minPath = null;
			double minDist = cutoff;
			Vector<Integer> newInitPath = new Vector<Integer>(initPath);
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
	 * gibt einen Polygonzug der mit Breite und einer Spitze versehen Linie von
	 * this zu p2 zurueck, zur Weiterverwendung in createLine()
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
	 * Findet alle moeglichen Eckpunkte der kuerzesten Verbindung
	 * 
	 * @param parcoursMapSimple
	 * @return eine Liste moeglicher Eckpunkte fuer die kuerzesten Verbindung
	 */
	Vector<TurningPoint> findTurningPoints(int[][] parcoursMapSimple) {
		Vector<TurningPoint> turningPoints = new Vector<TurningPoint>();
		// turningPoints.add(new TurningPoint(start));
		// turningPoints.add(new TurningPoint(finish));
		int count = 0;
		for (int i = 1; i < parcoursMapSimple[0].length; i++) {
			for (int j = 1; j < parcoursMapSimple.length; j++) {
				if (parcoursMapSimple[j - 1][i - 1] == 1
						&& parcoursMapSimple[j - 1][i] == 0
						&& parcoursMapSimple[j][i - 1] == 0
						&& parcoursMapSimple[j][i] == 0) {
					turningPoints.add(new TurningPoint(j + distFromCorner, i
							+ distFromCorner));
					count++;
				}
				if (parcoursMapSimple[j - 1][i - 1] == 0
						&& parcoursMapSimple[j - 1][i] == 1
						&& parcoursMapSimple[j][i - 1] == 0
						&& parcoursMapSimple[j][i] == 0) {
					turningPoints.add(new TurningPoint(j + distFromCorner, i
							- distFromCorner));
					count++;
				}
				if (parcoursMapSimple[j - 1][i - 1] == 0
						&& parcoursMapSimple[j - 1][i] == 0
						&& parcoursMapSimple[j][i - 1] == 1
						&& parcoursMapSimple[j][i] == 0) {
					turningPoints.add(new TurningPoint(j - distFromCorner, i
							+ distFromCorner));
					count++;
				}
				if (parcoursMapSimple[j - 1][i - 1] == 0
						&& parcoursMapSimple[j - 1][i] == 0
						&& parcoursMapSimple[j][i - 1] == 0
						&& parcoursMapSimple[j][i] == 1) {
					turningPoints.add(new TurningPoint(j - distFromCorner, i
							- distFromCorner));
					count++;
				}
			}
		}
		return turningPoints;
	}

	/**
	 * Kreiert die Incidenzmatrix der Punkteliste bezueglich parcoursMapSimple
	 * 
	 * @param turningPoints
	 * @param parcoursMapSimple
	 * @return double[i][j] ist der Abstand von Punkt i zu j, falls direkt
	 *         verbunden, ansonsten 10e30(unendlich)
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
	 * Gibt eine Liste aller Punkte zusammen mit ihren direkten Nachbarn zurueck
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
	 * gibt eine Folge von Punkten zurueck, die unter Beruecksichtigung von
	 * parcoursMapSimple den kuerzersten Weg von start nach finish beschreiben,
	 * den der bot durchfahren kann
	 * 
	 * @param start
	 * @param finish
	 * @param parcoursMapSimple
	 * @return Eckpunkte der Kuerzesten Verbindung von start zu finish
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
		// finde die kuerzeste Verbindung
		Vector<Integer> shortestPath = turningPoints.get(0).getShortestPath(0,
				1, new Vector<Integer>(), 1e30, incidenceMatrix, neighbors);
		Vector<TurningPoint> shortest = new Vector<TurningPoint>();
		if (shortestPath != null) {
			for (int i : shortestPath) {
				shortest.add(turningPoints.get(i));
			}
		}
		return shortest;
	}
}