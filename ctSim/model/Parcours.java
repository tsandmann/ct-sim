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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3d;

import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Repraesentiert einen Parcours fuer die Bots
 * Wird zum Laden eines solchen benoetigt.
 * Des Weiteren stehen hier auch Informationen ueber
 * Start- und Zielpunkte der Bots
 * @author bbe (bbe@heise.de)
 *
 */
public class Parcours {
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.Parcours");
/*	private class Hole{
		int x;
		int y;
		*//**
		 * @param x
		 * @param y
		 *//*
		public Hole(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}
	}*/

	/**
	 * <p>
	 * Breite einer Gittereinheit (eines Blocks) in Meter. Da die Bl&ouml;cke
	 * immer quadratisch sind, ist die Breite auch die H&ouml;he.
	 * </p>
	 * <p>
	 * Idee: ct-Bot ist 12cm breit, wir setzen das Gitter auf 24cm, damit der
	 * Bot auf dem engstm&ouml;glichen Durchgang (1 Block breit) m&ouml;glichst
	 * viel Spielraum hat nach links und rechts (oben und unten), aber dass
	 * gerade keine zwei Bots mehr aneinander vorbeik&ouml;nnen.
	 * </p>
	 */
	private float blockSizeInM = 0.24f;

	/** Anzahl der Startpositionen fuer Bots. Dir Position 0 ist die Default Position, ab 1 f√ºr die Wettkampfbots.*/
	public static int BOTS = 3;

	/** enthaelt alle Hindernisse */
	private BranchGroup ObstBG;
	/** Enthaelt alle Lichter */
	private BranchGroup lightBG;
	/** Enthaelt den Boden */
	private BranchGroup terrainBG;



	/** Ausdehnung des Parcours in X-Richtung [Gitter]*/
	private int dimX =0;
	/** Ausdehnung des Parcours in Y-Richtung [Gitter]*/
	private int dimY =0;

	/** Startposition der Bots [Gitter] Erste Dimension: Bots (0= default, ab 1 Wettkampfbots), zweite Dimension X, Y*/
	private int[][] startPositions = new int[BOTS][2];

	/**
	 * <p>
	 * Startpositionen der Bots in Grad; 0&#176; = Norden; gemessen im
	 * Uhrzeigersinn = im &quot;mathematisch negativen&quot; Drehsinn
	 * </p>
	 * <p>
	 * Array-Index: Startpositionen; es werden nur 1 und 2 verwendet in der
	 * gegenw‰rtigen Implementierung des ParcoursLoader
	 * </p>
	 */
	private double[] startHeadings = new double[BOTS];


	/** Zielpositionen */
	//private int[] finishPosition = new int[2];
	private List<Vector2d> finishPositions = new ArrayList<Vector2d>();

	/** Liste mit allen Abgruenden */
	private Vector<Vector2d> holes = new Vector<Vector2d>();

	/** Referenz auf eine Rohversion der Karte des Parcours (wie aus dem XML
	 * gelesen). Format siehe {@link ParcoursLoader}. */
	private int[][] parcoursMap;

	/**
	 * Erzeugt einen neuen, leeren Parcours
	 */
	public Parcours() {
		super();
		// Die Branchgroup fuer die Hindernisse
		this.ObstBG = new BranchGroup();
		this.ObstBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		this.ObstBG.setPickable(true);

		// Die Branchgroup fuer die Lichtquellen
		this.lightBG = new BranchGroup();
		this.lightBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		this.lightBG.setPickable(true);

		// Die Branchgroup fuer die Lichtquellen
		this.terrainBG = new BranchGroup();
		this.terrainBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		this.terrainBG.setPickable(true);


		// Standard startposition
		this.startPositions[0][0] =0;
		this.startPositions[0][1] =0;
	}

	/**
	 * @return Liefert die Parcoursbreite in Gittereinheiten zur&uuml;ck
	 */
	public int getWidthInBlocks() {
		return this.dimX;
	}

	/**
	 * Setzt die Parcursbreite in Gittereinheiten
	 * @param dimX1 Breite in Gittereinheiten
	 */
	public void setDimX(int dimX1) {
		this.dimX = dimX1;
	}

	/**
	 *
	 * @return Liefert die Parcoursh&ouml;he in Gittereinheiten zur&uuml;ck
	 */
	public int getHeightInBlocks() {
		return this.dimY;
	}

	/**
	 * @return Liefert die Breite (X-Gr&ouml;&szlig;e) des Parcours in Meter
	 * zur&uuml;ck
	 */
	public float getWidthInM(){
		return this.dimX* this.blockSizeInM;
	}

	/**
	 * @return Liefert die H&ouml;he (Y-Gr&ouml;&szlig;e) des Parcours in Meter
	 * zur&uuml;ck
	 */
	public float getHeightInM(){
		return this.dimY* this.blockSizeInM;
	}

	/**
	 * Setzt die Parcurshoehe in Gittereinheiten
	 * @param dimY1 Hoehe in Gittereinheiten
	 */
	public void setDimY(int dimY1) {
		this.dimY = dimY1;
	}

	/**
	 * Fuegt ein Hinderniss ein
	 * @param obstacle Das Hinderniss
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 */
	public void addObstacle(Node obstacle, float x, float y) {
		addNode(obstacle,x,y,this.ObstBG);
	}

	/**
	 * Fuegt ein Hinderniss ein
	 * @param obstacle Das Hinderniss
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse Absolut
	 */
	public void addObstacle(Node obstacle, float x, float y, float z) {
		addNode(obstacle,x,y,z,this.ObstBG);
	}

	/**
	 * Fuegt ein Stueck Boden ein
	 * @param floor der Boden
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse Absolut
	 */
	public void addFloor(Node floor, float x, float y, float z) {
		addNode(floor,x,y,z,this.terrainBG);
	}

	/**
	 * Fuegt eine Node ein
	 *
	 * @param node Die Node
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse absolut; positiv ist vom Boden Richtung Bot +
	 * Betrachter, negativ vom Boden weg vom Betrachter
	 * @param bg Gruppe in die das Objekt rein soll
	 */
	public void addNode(Node node, float x, float y,float z,BranchGroup bg) {
		Transform3D translate = new Transform3D();

		node.setPickable(true);
//		node.setCapability(TransformGroup.ENABLE_PICK_REPORTING);

		translate.set(new Vector3d(x * this.blockSizeInM, y* this.blockSizeInM, z));

		TransformGroup tg = new TransformGroup(translate);
		tg.setCapability(Node.ENABLE_PICK_REPORTING);
		tg.setPickable(true);
		tg.addChild(node);
		bg.addChild(tg);
	}

	/**
	 * Fuegt eine Node ein
	 * @param node Die Node
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param bg Gruppe in die das Objekt rein soll
	 */
	public void addNode(Node node, float x, float y,BranchGroup bg) {
		addNode(node,x,y,0.0f,bg);
	}

	/**
	 * Fuegt eine Darstellung der Lichtquelle ein
	 * @param light Die Lichtquelle
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse im Parcours-Gitter
	 */
	public void addLight(Node light, float x, float y, float z) {
		addNode(light,x,y,z,this.lightBG);
	}

	/**
	 * Fuegt eine Lichtquelle ein
	 * @param light Die Lichtquelle
	 */
	public void addLight(Node light) {
		this.lightBG.addChild(light);
	}


	/**
	 *
	 * @return Liefert die Licht-Branchgroup zurueck
	 */
	public BranchGroup getLightBG() {
		return this.lightBG;
	}

	/**
	 *
	 * @return Liefert die Hindernis-Branchgroup zurueck
	 */
	public BranchGroup getObstBG() {
		return this.ObstBG;
	}

	/**
	 *
	 * @return  Liefert die Boden-Branchgroup zurueck
	 */
	public BranchGroup getTerrainBG() {
		return this.terrainBG;
	}

	/**
	 * Legt die Startposition eines Bots fest
	 * @param bot Nummer des Bots (faengt bei 0 an zu zaehlen)
	 * @param x
	 * @param y
	 */
	public void setStartPosition(int bot, int x, int y){
		if (bot <= BOTS-1){
			this.startPositions[bot][0]=x;
			this.startPositions[bot][1]=y;
		}
	}

	/**
	 * Legt die Startrichtung eines Bots fest
	 *
	 * @param bot Nummer des Bots (f&auml;ngt bei 0 an zu z&auml;hlen)
	 * @param dir Richtung in Grad. 0 entspricht "nach Osten", d.h. x=1, y=0,
	 * dann im Uhrzeigersinn weiter. <strong>Achtung</strong>, weil der
	 * ParcoursLoader die genannten Gradangaben verwendet, der Rest des
	 * Programms aber &quot;0&#176; = Norden&quot; annimmt, wird in dieser
	 * Methode konvertiert (&minus; 90&#176; gerechnet)
	 */
	public void setStartHeading(int bot, int dir){
		if (bot < 0 || bot >= startHeadings.length)
			throw new IllegalArgumentException();
		if (dir != 0 && dir != 90 && dir != 180 && dir != 270)
			throw new IllegalArgumentException();

		startHeadings[bot] = dir - 90;
	}

	/**
	 * Liefert die Startposition eines Bots
	 * Wenn keine festgelegt wurde, dann die Default-Position  (0)
	 * @param bot
	 * @return Die Startposition
	 */
	public Vector3d getStartPosition(int bot){
		Vector3d pos = null;
		if (bot < startHeadings.length)
			pos= new Vector3d(this.startPositions[bot][0]*this.blockSizeInM + this.blockSizeInM/2,this.startPositions[bot][1]*this.blockSizeInM + this.blockSizeInM/2,0.0f);
		else
			pos= new Vector3d(this.startPositions[0][0]*this.blockSizeInM + this.blockSizeInM/2,this.startPositions[0][1]*this.blockSizeInM + this.blockSizeInM/2,0.0f);

		return pos;
	}

	/**
	 * Liefert die Startrichtung eines Bots. Diese ergibt sich wie folgt:
	 * <ul>
	 * <li>Die Richtung, die im Parcours definiert ist; z.B. 90∞, falls die
	 * Methode mit {@code bot == 2} aufgerufen wird und ein Parcours geladen
	 * worden ist, der die Felder "2." enth‰lt</li>
	 * <li>Falls der Parcours keine solche Startrichtung definiert, wird
	 * stillschweigend 0&#176; zur&uuml;ckgeliefert, d.h. Blick nach Norden</li>
	 * </ul>
	 *
	 * @param bot Nummer des Startfelds: 0 f&uuml;r 0∞ = Defaultrichtung; 1 oder
	 * 2 f&uuml;r die Nummer des Startfelds; andere Werte nicht zul&auml;ssig
	 * aufgrund der gegenw&auml;rtigen Implementierung des ParcoursLoader
	 * @return Die Richtung, in die ein auf dem Startfeld Nummer {@code bot}
	 * stehender Bot laut Parcours-XML gucken soll. In Grad; 0∞ = Norden;
	 * gemessen im Uhrzeigersinn; Intervall ]-180; 180]
	 */
	public double getStartHeadingInDeg(int bot) {
		if (bot > 0 || bot < startHeadings.length)
			return Misc.normalizeAngle(startHeadings[bot]);
		else
			return 0; // default
	}


	/**
	 * Legt die Zielposition fest
	 * @param x
	 * @param y
	 */
//	public void setFinishPosition(int x, int y){
//		this.finishPosition[0]=x;
//		this.finishPosition[1]=y;
//	}

	/**
	 * Fuegt eine neue Zielposition hinzu
	 * @param x
	 * @param y
	 */
	public void addFinishPosition(int x, int y) {
		this.finishPositions.add(new Vector2d(x, y));
	}

	/**
	 * @return Liefert die Breite/H&ouml;he einer Gittereinheit (eines Blocks)
	 * in Meter zur&uuml;ck. (Da die Bl&ouml;cke quadratisch sind, sind Breite
	 * und H&ouml;he gleich.)
	 */
	public float getBlockSizeInM() {
		return blockSizeInM;
	}

	/**
	 * Prueft, ob ein Punkt innerhalb des Zielfeldes liegt
	 * @param pos
	 * @return true, falls ja
	 */
	public boolean finishReached(Vector3d pos){
//		float minX = this.finishPosition[0]*this.grid ;
//		float maxX = this.finishPosition[0]*this.grid + this.grid;
//		float minY = this.finishPosition[1]*this.grid ;
//		float maxY = this.finishPosition[1]*this.grid + this.grid;
//
//		if ((pos.x > minX) && (pos.x < maxX) && (pos.y > minY) && (pos.y < maxY))
//			return true;
//		return false;

		for(Vector2d p : this.finishPositions) {

			double minX = p.x*this.blockSizeInM ;
			double maxX = p.x*this.blockSizeInM + this.blockSizeInM;
			double minY = p.y*this.blockSizeInM ;
			double maxY = p.y*this.blockSizeInM + this.blockSizeInM;

			if((pos.x > minX) && (pos.x < maxX) && (pos.y > minY) && (pos.y < maxY))
				return true;
		}
		return false;
	}

	/**
	 * Fuegt ein Loch hinzu
	 * @param x
	 * @param y
	 */
	public void addHole(int x, int y) {
		holes.add(new Vector2d(x,y));
	}

	/**
	 * Prueft, ob ein Punkt ueber einem Loch liegt
	 * @param pos
	 * @return true, wenn der Bot ueber dem loch steht
	 */
	public boolean checkHole(Point3d pos){
		if ((pos.x < 0) || (pos.y <0) || (pos.x > dimX* blockSizeInM) || (pos.y > dimY* blockSizeInM))
			return true;

		Iterator it = holes.iterator();
		while (it.hasNext()){
			Vector2f min = new Vector2f((Vector2d)it.next());
			min.scale(blockSizeInM);

			Vector2f max = new Vector2f(min);
			max.add(new Vector2f(blockSizeInM,blockSizeInM));

			if ((pos.x > min.x) && (pos.x < max.x) && (pos.y > min.y) && (pos.y < max.y))
				return true;
		}

		return false;
	}

	void setParcoursMap(int[][] parcoursMap) {
	    this.parcoursMap = parcoursMap;
    }

	/**
	 * Liefert eine Referenz auf die Rohversion der Karte dieses Parcours (wie
	 * aus dem XML gelesen). Das Format ist so, wie der {@link ParcoursLoader}
	 * die parcoursMap erstellt hat.
	 */
	public int[][] getParcoursMap() {
    	return parcoursMap;
    }

	/**
	 * Liefert einen stark verienfachten Parcours zur√ºck.
	 * das Array enthaelt nur 0 (freies Feld) und 1 (blockiertes Feld)
	 *
	 */
	int[][] getFlatParcours() {
		int[][] parcoursMapSimple = new int[this.getWidthInBlocks()][this.getHeightInBlocks()];
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
				case '~':
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
	 * Liefert die kuerzeste Distanz von einem gegebenen Punkt (in Gitterkoordinaten) zum Ziel
	 * @param from Startpunkt in Weltkoordinaten
	 * @return Distanz (ohne Drehungen) in Metern
	 */
	public double getShortestDistanceToFinish(Vector3d from){
		return getShortestDistanceToFinish(new Vector2d(from.x,from.y)); //LODO z wird ignoriert -- wird nicht mehr klappen, wenn der Bot (hypothetisch) mal Rampen hochfaehrt und sich auf verschiedenen Ebenen bewegt
	}


	/**
	 * Liefert die kuerzeste Distanz von einem gegebenen Punkt (in Gitterkoordinaten) zum Ziel
	 * @param from Startpunkt in Weltkoordinaten
	 * @return Distanz in Metern
	 */
	public double getShortestDistanceToFinish(Vector2d from){
		Vector<TurningPoint> shortestPath=getShortestPath(from);

    	if(shortestPath==null || shortestPath.size()<2)
    		return -1;

    	double distance=TurningPoint.getLengthOfPath(shortestPath);

		return distance*this.blockSizeInM;
	}

	/**
	 * Liefert den kuerzesten Pfad von einem bestimmten Punkt aus zum Ziel
	 * @param from Startpunkt in weltkoordinaten
	 * @return Liste der Turningpoints (Gitterkoordinaten!!!)
	 */
	public Vector<TurningPoint> getShortestPath(Vector3d from){
		return getShortestPath(new Vector2d(from.x,from.y));
	}

	/**
	 * Liefert den kuerzesten Pfad von einem bestimmten Punkt aus zum Ziel
	 * @param from Startpunkt in weltkoordinaten
	 * @return Liste der Turningpoints (Gitterkoordinaten!!!)
	 */
	public Vector<TurningPoint> getShortestPath(Vector2d from){
		Vector2d f = new Vector2d(from);
		f.scale(1/this.blockSizeInM);

		TurningPoint start = new TurningPoint(f);

		if (finishPositions.size()== 0)
			return null;

		Iterator<Vector2d> it = finishPositions.iterator();

		double dist = java.lang.Double.MAX_VALUE;
		Vector<TurningPoint> shortestPath = null;

		while (it.hasNext()){
			Vector2d fin = new Vector2d(it.next());
			fin.add(new Vector2d(0.5,0.5));
			TurningPoint finish = new TurningPoint(fin);

			Vector<TurningPoint> tmpShortestPath = start.getShortestPathTo(finish, getFlatParcours());

			double tmpDist = TurningPoint.getLengthOfPath(tmpShortestPath);
			if (tmpDist < dist){
				shortestPath = tmpShortestPath;
				dist=tmpDist;
			}
		}

    	// finde die kuerzeste Verbindung
    	return shortestPath;
	}
}
