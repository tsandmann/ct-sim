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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point2i;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;

import ctSim.model.bots.Bot;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Repräsentiert einen Parcours für die Bots Wird zum Laden eines solchen
 * benötigt. Des Weiteren stehen hier auch Informationen über Start- und
 * Zielpunkte der Bots.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * 
 */
public class Parcours {
	/** Logger */
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.Parcours");

	/**
	 * <p>
	 * Breite einer Gittereinheit (eines Blocks) in Meter. Da die Blöcke
	 * immer quadratisch sind, ist die Breite auch die Höhe.
	 * 
	 * Idee: ct-Bot ist 12cm breit, wir setzen das Gitter auf 24cm, damit der
	 * Bot auf dem engstmöglichen Durchgang (1 Block breit) möglichst
	 * viel Spielraum hat nach links und rechts (oben und unten), aber dass
	 * gerade keine zwei Bots mehr aneinander vorbei können.
	 * </p>
	 */
	private final float blockSizeInM = 0.24f;

	/**
	 * Anzahl der Startpositionen für Bots. Dir Position 0 ist die Default
	 * Position, ab 1 für die Wettkampfbots.
	 */
	public static final int BOTS = 3;	// ParcoursLoader kann max 2 Startplätze erzeugen, darum hardcoded auf 3

	/** Enthält alle Hindernisse */
	private BranchGroup ObstBG;
	/** Enthält alle Lichter */
	private BranchGroup lightBG;
	/** Enthält alle Landmarken */
	private BranchGroup bpsBG;
	/** Enthält den Boden */
	private BranchGroup terrainBG;

	/** Ausdehnung des Parcours in X-Richtung [Gitter] */
	private int dimX = 0;
	/** Ausdehnung des Parcours in Y-Richtung [Gitter] */
	private int dimY = 0;

	/**
	 * Startposition der Bots [Gitter] Erste Dimension: Bots (0= default, ab 1
	 * Wettkampfbots), zweite Dimension X, Y
	 */
	private int[][] startPositions = new int[BOTS][2];
	/** Info, welcher Bot wo gestartet ist oder starten wird */
	private Bot[] startPositionsUsed = new Bot[BOTS];

	/**
	 * Startposition der Bots [Gitter] Erste Dimension: Bots (0= default, ab 1
	 * Wettkampfbots), zweite Dimension X, Y
	 */
	private int[][] startHeadings = new int[BOTS][2];

	/** Zielpositionen */
	private List<Vector2d> finishPositions = Misc.newList();

	/** Liste mit allen Abgründen */
	private Vector<Vector2d> holes = new Vector<Vector2d>();

	/**
	 * Referenz auf eine Rohversion der Karte des Parcours (wie aus dem XML
	 * gelesen). Format siehe {@link ParcoursLoader}.
	 */
	private int[][] parcoursMap;
	
	/** Parcoursloder */
	final private ParcoursLoader parcoursLoader;

	/**
	 * Erzeugt einen neuen, leeren Parcours
	 * 
	 * @param parcoursLoader ParcoursLoader, der den Parcours erzeugt hat
	 */
	public Parcours(ParcoursLoader parcoursLoader) {
		super();
		this.parcoursLoader = parcoursLoader;
		// Die Branchgroup für die Hindernisse
		ObstBG = new BranchGroup();
		ObstBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		ObstBG.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		ObstBG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		ObstBG.setCapability(BranchGroup.ALLOW_DETACH);
		ObstBG.setPickable(true);

		// Die Branchgroup für die Lichtquellen
		lightBG = new BranchGroup();
		lightBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		lightBG.setPickable(true);
		
		// Die Branchgroup für die BPS-Landmarken
		bpsBG = new BranchGroup();
		bpsBG.setPickable(true);

		// Die Branchgroup für den Boden
		terrainBG = new BranchGroup();
		terrainBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		terrainBG.setPickable(true);

		// Standard Startposition
		startPositions[0][0] = 0;
		startPositions[0][1] = 0;
	}

	/**
	 * Setzt ein Startfeld auf belegt
	 * 
	 * @param bot	Zeiger auf Bot, der das Startfeld belegt
	 */
	public void setStartFieldUsed(Bot bot) {
		for (int i = 1; i < BOTS; i++) {
			if (startPositionsUsed[i] == null) {
				startPositionsUsed[i] = bot;
				break;
			}
		}
	}

	/**
	 * Gibt ein Startfeld wieder frei
	 * 
	 * @param bot	Zeiger auf Bot, der auf dem Startfeld steht oder gestartet ist
	 */
	public void setStartFieldUnused(Bot bot) {
		for (int i = 1; i < BOTS; i++) {
			if (startPositionsUsed[i] == bot) {
				startPositionsUsed[i] = null;
				break;
			}
		}
	}

	/**
	 * @return Liefert die Parcoursbreite in Gittereinheiten zurück
	 */
	public int getWidthInBlocks() {
		return dimX;
	}

	/**
	 * Setzt die Parcoursbreite in Gittereinheiten
	 * 
	 * @param dimX1	Breite in Gittereinheiten
	 */
	public void setDimX(int dimX1) {
		dimX = dimX1;
	}

	/**
	 * @return Liefert die Parcourshöhe in Gittereinheiten zurück
	 */
	public int getHeightInBlocks() {
		return dimY;
	}

	/**
	 * @return Liefert die Breite (X-Größe) des Parcours in Meter zurück
	 */
	public float getWidthInM() {
		return dimX * blockSizeInM;
	}

	/**
	 * @return Liefert die Höhe (Y-Größe) des Parcours in Meter zurück
	 */
	public float getHeightInM() {
		return dimY * blockSizeInM;
	}

	/**
	 * Setzt die Parcourshöhe in Gittereinheiten
	 * 
	 * @param dimY1	Höhe in Gittereinheiten
	 */
	public void setDimY(int dimY1) {
		dimY = dimY1;
	}

	/**
	 * Fügt ein Hindernis ein
	 * 
	 * @param obstacle	Das Hindernis
	 * @param x			X-Achse im Parcours-Gitter
	 * @param y			Y-Achse im Parcours-Gitter
	 */
	public void addObstacle(Node obstacle, float x, float y) {
		addNode(obstacle, x, y, ObstBG);
	}

	/**
	 * Fügt ein Hindernis ein
	 * 
	 * @param obstacle	Das Hindernis
	 * @param x			X-Achse im Parcours-Gitter
	 * @param y			Y-Achse im Parcours-Gitter
	 * @param z			Z-Achse absolut
	 */
	public void addObstacle(Node obstacle, float x, float y, float z) {
		addNode(obstacle, x * blockSizeInM, y * blockSizeInM, z, ObstBG);
	}
	
	/**
	 * Fügt ein bewegliches Hindernis ein
	 * 
	 * @param obstacle	Das Hindernis
	 * @param x			X-Koordinate
	 * @param y			Y-Koordinate
	 */
	public void addMoveableObstacle(Node obstacle, float x, float y) {
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		addNode(obstacle, x, y, 0.0f, bg);
		ObstBG.addChild(bg);
	}

	/**
	 * Fügt ein Stück Boden ein
	 * 
	 * @param floor	der Boden
	 * @param x		X-Achse im Parcours-Gitter
	 * @param y		Y-Achse im Parcours-Gitter
	 * @param z		Z-Achse absolut
	 */
	public void addFloor(Node floor, float x, float y, float z) {
		addNode(floor, x * blockSizeInM, y * blockSizeInM, z, terrainBG);
	}

	/**
	 * Fügt eine Node ein
	 * 
	 * @param node	Die Node
	 * @param x		X-Koordinate
	 * @param y		Y-Koordinate
	 * @param z		Z-Achse absolut; positiv ist vom Boden Richtung Bot +
	 * 				Betrachter, negativ vom Boden weg vom Betrachter
	 * @param bg	BranchGroup, in die das Objekt rein soll
	 */
	public void addNode(Node node, float x, float y, float z, BranchGroup bg) {
		Transform3D translate = new Transform3D();

		translate.set(new Vector3d(x, y, z));

		TransformGroup tg = new TransformGroup(translate);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.addChild(node);
		
		TransformGroup tgObject = new TransformGroup();
		tgObject.addChild(tg);
		tgObject.setCapability(Node.ENABLE_PICK_REPORTING);
		tgObject.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		tgObject.setPickable(true);
		bg.addChild(tgObject);
	}

	/**
	 * Fügt eine Node ein
	 * 
	 * @param node	Die Node
	 * @param x		X-Achse im Parcours-Gitter
	 * @param y		Y-Achse im Parcours-Gitter
	 * @param bg	Gruppe in die das Objekt rein soll
	 */
	public void addNode(Node node, float x, float y, BranchGroup bg) {
		addNode(node, x * blockSizeInM, y * blockSizeInM, 0.0f, bg);
	}

	/**
	 * Fügt eine Darstellung der Lichtquelle ein
	 * 
	 * @param light	Die Lichtquelle
	 * @param x		X-Achse im Parcours-Gitter
	 * @param y		Y-Achse im Parcours-Gitter
	 * @param z		Z-Achse im Parcours-Gitter
	 */
	public void addLight(Node light, float x, float y, float z) {
		addNode(light, x * blockSizeInM, y * blockSizeInM, z, lightBG);
	}

	/**
	 * Fügt eine Lichtquelle ein
	 * 
	 * @param light	Die Lichtquelle
	 */
	public void addLight(Node light) {
		lightBG.addChild(light);
	}
	
	/**
	 * Fügt eine Darstellung der Landmarke ein
	 * 
	 * @param bps	Die Landmarke
	 * @param x		X-Achse im Parcours-Gitter
	 * @param y		Y-Achse im Parcours-Gitter
	 * @param z		Z-Achse im Parcours-Gitter
	 */
	public void addBPSLight(Node bps, float x, float y, float z) {
		addNode(bps, x * blockSizeInM, y * blockSizeInM, z, bpsBG);
	}
	
	/**
	 * Fügt eine Landmarke für BPS ein
	 * 
	 * @param bps	Landmarke
	 */
	public void addBPSLight(Node bps) {
		bpsBG.addChild(bps);
	}

	/**
	 * Erzeugt ein bewegliches Objekt
	 * 
	 * @param x	X-Koordinate
	 * @param y	Y-Koordinate
	 */
	public void createMovableObject(float x, float y) {
		parcoursLoader.createMovableObject(x, y);
	}
	
	/**
	 * @return Liefert die Licht-Branchgroup zurück
	 */
	public BranchGroup getLightBG() {
		return lightBG;
	}

	/**
	 * @return Liefert die Branchgroup mit den Landmarken für BPS zurück 
	 */
	public BranchGroup getBpsBG() {
		return bpsBG;
	}
	
	/**
	 * @return Liefert die Hindernis-Branchgroup zurück
	 */
	public BranchGroup getObstBG() {
		return ObstBG;
	}

	/**
	 * @return Liefert die Boden-Branchgroup zurück
	 */
	public BranchGroup getTerrainBG() {
		return terrainBG;
	}

	/**
	 * Legt die Startposition eines Bots fest
	 * 
	 * @param bot	Nummer des Bots (fängt bei 0 an zu zählen)
	 * @param x		X-Koordinate
	 * @param y		Y-Koordinate
	 */
	public void setStartPosition(int bot, int x, int y) {
		if (bot <= BOTS - 1) {
			startPositions[bot][0] = x;
			startPositions[bot][1] = y;
		}
	}

	/**
	 * Legt die Startrichtung eines Bots fest
	 * 
	 * @param bot	Nummer des Bots (fängt bei 0 an zu zählen)
	 * @param dir	Richtung in Grad. 0 entspricht (x=1, y=0) dann im Uhrzeigersinn
	 */
	public void setStartHeading(int bot, int dir) {
		if (bot <= BOTS - 1) {
			switch (dir) {
			case 0:
				startHeadings[bot][0] = 1;
				startHeadings[bot][1] = 0;
				break;
			case 90:
				startHeadings[bot][0] = 0;
				startHeadings[bot][1] = -1;
				break;
			case 180:
				startHeadings[bot][0] = -1;
				startHeadings[bot][1] = 0;
				break;
			case 270:
				startHeadings[bot][0] = 0;
				startHeadings[bot][1] = 1;
				break;

			default:
				break;
			}
		}
	}

	/**
	 * Liefert die Startposition für einen neuen Bots
	 * Wenn keine festgelegt wurde, dann die Default-Position (0).
	 * 
	 * @param bot	Bot-Nummer
	 * @return Die Startposition
	 */
	public Point3d getStartPosition(int bot) {
		Point3d pos = null;
		if (bot < BOTS) {
			int i;
			for (i = 1; i < BOTS; i++) {
				if (startPositionsUsed[i] == null) {
					break;
				}
			}
			if (i == BOTS) {
				i = 0;
			}
			pos = new Point3d(startPositions[i][0] * blockSizeInM + blockSizeInM / 2, startPositions[i][1] * blockSizeInM + blockSizeInM / 2, 0.0f);
		} else {
			pos = new Point3d(startPositions[0][0] * blockSizeInM + blockSizeInM / 2, startPositions[0][1] * blockSizeInM + blockSizeInM / 2, 0.0f);
		}
		return pos;
	}

	/**
	 * Liefert die Nummer des Startfeldes zu einem Bot
	 * 
	 * @param bot	Referenz auf Bot
	 * @return Nummer des Startfeldes, oder 0, falls Bot unbekannt
	 */
	public int getStartPositionNumber(Bot bot) {
		for (int i = 1; i < startPositionsUsed.length; i++) {
			if (startPositionsUsed[i] == bot) {
				return i;
			}
		}
		return 0;
	}

	/**
	 * Liefert die Startposition eines vorhandenen Bots
	 * 
	 * @param bot	Bot-Nummer
	 * @return Die Startposition
	 */
	public Point3d getUsedStartPosition(int bot) {
		Point3d pos = null;
		if (bot < BOTS) {
			pos = new Point3d(startPositions[bot][0] * blockSizeInM + blockSizeInM / 2, startPositions[bot][1] * blockSizeInM + blockSizeInM / 2, 0.0f);
		} else {
			pos = new Point3d(startPositions[0][0] * blockSizeInM + blockSizeInM / 2, startPositions[0][1] * blockSizeInM + blockSizeInM / 2, 0.0f);
		}

		return pos;
	}

	/**
	 * Liefert die Startrichtung eines Bots
	 * Wenn keine festgelegt wurde, dann die Default-Position (0).
	 * 
	 * @param bot
	 * @return Die Startrichtung
	 */
	public Vector3d getStartHeading(int bot) {
		Vector3d pos = null;
		if (bot < BOTS) {
			pos = new Vector3d(startHeadings[bot][0], startHeadings[bot][1], 0.0f);
		} else {
			// sonst liefere die Default-Richtung
			pos = new Vector3d(startHeadings[0][0], startHeadings[0][1], 0.0f);
		}

		if (pos.length() == 0) {
			pos.x = 1.0f;
			lg.warn("getStartHeading wurde nach einer noch nicht gesetzten "
				+ "Heading gefragt (Bot " + bot + "). Setze Default");
		}

		pos.normalize();

		return pos;
	}

	/**
	 * Fügt eine neue Zielposition hinzu
	 * 
	 * @param x
	 * @param y
	 */
	public void addFinishPosition(int x, int y) {
		finishPositions.add(new Vector2d(x, y));
	}

	/**
	 * @return Liefert die Breite/Höhe einer Gittereinheit (eines Blocks)
	 *         in Meter zurück. (Da die Blöcke quadratisch sind, sind
	 *         Breite und Höhe gleich.)
	 */
	public float getBlockSizeInM() {
		return blockSizeInM;
	}

	/**
	 * @return Liefert die Breite/Höhe einer Gittereinheit (eines Blocks)
	 *         in Millimeter zurück. (Da die Blöcke quadratisch sind, sind
	 *         Breite und Höhe gleich.)
	 */
	public int getBlockSizeInMM() {
		return (int) (blockSizeInM * 1000.0f);
	}

	/**
	 * Prüft, ob ein Punkt innerhalb des Zielfeldes liegt
	 * 
	 * @param pos
	 * @return true, falls ja
	 */
	public boolean finishReached(Vector3d pos) {
		for (Vector2d p : finishPositions) {
			double minX = p.x * blockSizeInM;
			double maxX = p.x * blockSizeInM + blockSizeInM;
			double minY = p.y * blockSizeInM;
			double maxY = p.y * blockSizeInM + blockSizeInM;

			if ((pos.x > minX) && (pos.x < maxX) && (pos.y > minY) && (pos.y < maxY)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fügt ein Loch hinzu
	 * 
	 * @param x
	 * @param y
	 */
	public void addHole(int x, int y) {
		holes.add(new Vector2d(x, y));
	}

	/**
	 * Prüft, ob ein Punkt über einem Loch liegt
	 * 
	 * @param pos
	 * @return true, wenn der Bot über dem Loch steht
	 */
	public boolean checkHole(Point3d pos) {
		if ((pos.x < 0) || (pos.y < 0) || (pos.x > dimX * blockSizeInM) || (pos.y > dimY * blockSizeInM)) {
			return true;
		}

		Iterator it = holes.iterator();
		while (it.hasNext()) {
			Vector2f min = new Vector2f((Vector2d) it.next());
			min.scale(blockSizeInM);

			Vector2f max = new Vector2f(min);
			max.add(new Vector2f(blockSizeInM, blockSizeInM));

			if ((pos.x > min.x) && (pos.x < max.x) && (pos.y > min.y) && (pos.y < max.y)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Setzt eine Parcours-Map
	 * 
	 * @param parcoursMap	die neue Map
	 */
	void setParcoursMap(int[][] parcoursMap) {
		this.parcoursMap = parcoursMap;
	}

	/**
	 * Liefert eine Referenz auf die Rohversion der Karte dieses Parcours (wie
	 * aus dem XML gelesen). Das Format ist so, wie der {@link ParcoursLoader}
	 * die parcoursMap erstellt hat.
	 * 
	 * @return die ParcoursMap
	 */
	public int[][] getParcoursMap() {
		return parcoursMap;
	}

	/**
	 * @return Liefert einen stark vereinfachten Parcours zurück. Das Array
	 *         enthält nur 0 (freies Feld) und 1 (blockiertes Feld).
	 */
	int[][] getFlatParcours() {
		int[][] parcoursMapSimple = new int[getWidthInBlocks()][getHeightInBlocks()];
		for (int y = 0; y < parcoursMapSimple[0].length; y++) {
			for (int x = 0; x < parcoursMapSimple.length; x++) {
				switch (parcoursMap[x][y]) {
				case '.':
				case ' ':
				case '1':
				case '2':
				case '0':
				case 'Z':
				case '-':
				case '|':
				case '/':
				case '\\':
				case '+':
				case '[':
				case ']':
				case '{':
				case '}':
				case 'T':
				case '~':
				case '!':
				case '%':
					parcoursMapSimple[x][y] = 0;
					break;
				default:
					parcoursMapSimple[x][y] = 1;
					break;
				}
			}
		}
		return parcoursMapSimple;
	}

	/**
	 * @return Liefert einen stark vereinfachten Parcours zurück. Das Array
	 *         enthält nur 0 (freies Feld), 1 (blockiertes Feld) und 2 (Loch).
	 */
	public int[][] getFlatParcoursWithHoles() {
		int[][] parcoursMapSimple = new int[getWidthInBlocks()][getHeightInBlocks()];
		for (int y = 0; y < parcoursMapSimple[0].length; y++) {
			for (int x = 0; x < parcoursMapSimple.length; x++) {
				switch (parcoursMap[x][y]) {
				case '.':
				case ' ':
				case '1':
				case '2':
				case '0':
				case 'Z':
				case '-':
				case '|':
				case '/':
				case '\\':
				case '+':
				case '[':
				case ']':
				case '{':
				case '}':
				case 'T':
				case '~':
				case '!':
				case '%':
					/* frei */
					parcoursMapSimple[x][y] = 0;
					break;
				case 'L':
					/* Loch */
					parcoursMapSimple[x][y] = 2;
					break;
				default:
					/* Hindernis */
					parcoursMapSimple[x][y] = 1;
					break;
				}
			}
		}
		return parcoursMapSimple;
	}	
	
	/**
	 * Liefert die kürzeste Distanz von einem gegebenen Punkt (in
	 * Gitterkoordinaten) zum Ziel.
	 * 
	 * @param from	Startpunkt in Weltkoordinaten
	 * @return Distanz (ohne Drehungen) in Metern
	 */
	public double getShortestDistanceToFinish(Vector3d from) {
		// TODO: z wird ignoriert -- wird nicht mehr klappen, wenn der Bot
		// (hypothetisch) mal Rampen hochfährt und sich auf verschiedenen
		// Ebenen bewegt.
		return getShortestDistanceToFinish(new Vector2d(from.x, from.y));
	}

	/**
	 * Liefert die kürzeste Distanz von einem gegebenen Punkt (in
	 * Gitterkoordinaten) zum Ziel
	 * 
	 * @param from	Startpunkt in Weltkoordinaten
	 * @return Distanz in Metern
	 */
	public double getShortestDistanceToFinish(Vector2d from) {
		Vector<TurningPoint> shortestPath = getShortestPath(from);

		if (shortestPath == null || shortestPath.size() < 2) {
			return -1;
		}

		double distance = TurningPoint.getLengthOfPath(shortestPath);

		return distance * blockSizeInM;
	}

	/**
	 * Liefert den kürzesten Pfad von einem bestimmten Punkt aus zum Ziel
	 * 
	 * @param from	Startpunkt in weltkoordinaten
	 * @return Liste der Turningpoints (Gitterkoordinaten!!!)
	 */
	public Vector<TurningPoint> getShortestPath(Vector3d from) {
		return getShortestPath(new Vector2d(from.x, from.y));
	}

	/**
	 * Liefert den kürzesten Pfad von einem bestimmten Punkt aus zum Ziel
	 * 
	 * @param from	Startpunkt in weltkoordinaten
	 * @return Liste der Turningpoints (Gitterkoordinaten!!!)
	 */
	public Vector<TurningPoint> getShortestPath(Vector2d from) {
		Vector2d f = new Vector2d(from);
		f.scale(1 / blockSizeInM);

		TurningPoint start = new TurningPoint(f);

		if (finishPositions.size() == 0) {
			return null;
		}

		Iterator<Vector2d> it = finishPositions.iterator();

		double dist = java.lang.Double.MAX_VALUE;
		Vector<TurningPoint> shortestPath = null;

		while (it.hasNext()) {
			Vector2d fin = new Vector2d(it.next());
			fin.add(new Vector2d(0.5, 0.5));
			TurningPoint finish = new TurningPoint(fin);

			Vector<TurningPoint> tmpShortestPath = start.getShortestPathTo(finish, getFlatParcours());

			double tmpDist = TurningPoint.getLengthOfPath(tmpShortestPath);
			if (tmpDist < dist) {
				shortestPath = tmpShortestPath;
				dist = tmpDist;
			}
		}

		// finde die kürzeste Verbindung
		return shortestPath;
	}
	
	/** 
	 * Rechnet eine Block-Koordinate in eine Welt-Koordinate um
	 * 
	 * @param koord	Block-Koordinate
	 * @return Welt-Koordinate [mm]
	 */
	public int blockToWorld(int koord) {
		return koord * (int) (blockSizeInM * 1000.0f);
	}
	
	/**
	 * Liefert die Startposition zu einer Bot-Nr.
	 * 
	 * @param bot	Nummer des Bots [0, 2]
	 * @return {X, Y}
	 */
	public int[] getStartPositions(int bot) {
		if (bot < startPositions.length) {
			return startPositions[bot];
		} else {
			return null;
		}
	}
	
	/**
	 * Rechnet eine Welt-Koordinate in eine globale Position um
	 * 
	 * @param worldPos	Welt-Position (wie von Java3D verwendet)
	 * @return globale Position (wie zur Lokalisierung verwendet) / Blocks
	 */
	public Point2i transformWorldposToGlobalpos(Point3d worldPos) {
		int parcoursBlockSizeInMM = getBlockSizeInMM();
		final int x = (int) (worldPos.y * (1000.0 / parcoursBlockSizeInMM));
		final int y = (int) (((getWidthInM() - worldPos.x) * 1000.0) / parcoursBlockSizeInMM);
		return new Point2i(x, y);
	}
}
