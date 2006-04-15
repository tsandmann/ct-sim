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

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;

/**
 * Repraesentiert einen Parcouts fuer die Bots
 * Wird zum Laden eines solchen benoetigt. 
 * Des Weiteren stehen hier auch Informationen ueber
 * Start- und Zielpunkte der Bots
 * @author bbe (bbe@heise.de)
 *
 */
public class Parcours {
	/** Raster des Parcours-Gitters */
	private float grid = (float) (CtBot.BOT_RADIUS * 2 * 2);

	/** Anzahl der Startpositionen für Bots. Dir Position 0 ist die Default Position, ab 1 für die Wettkampfbots.*/
	public static int BOTS = 3; 
	
	/** enthaelt alle Hindernisse */
	private BranchGroup obstBG;
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

	/** Startposition der Bots [Gitter] Erste Dimension: Bots (0= default, ab 1 Wettkampfbots), zweite Dimension X, Y*/
	private int[][] startHeadings = new int[BOTS][2];
	
	
	/** Zielposition */
	private int[] finishPosition = new int[2];

	/**
	 * Erzeugt einen neuen, leeren Parcours 
	 */
	public Parcours() {
		super();
		// Die Branchgroup fuer die Hindernisse
		obstBG = new BranchGroup();
		obstBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		obstBG.setPickable(true);
		
		// Die Branchgroup fuer die Lichtquellen
		lightBG = new BranchGroup();
		lightBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		lightBG.setPickable(true);

		// Die Branchgroup fuer die Lichtquellen
		terrainBG = new BranchGroup();
		terrainBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		terrainBG.setPickable(true);

		
		// Standard startposition
		startPositions[0][0] =0;
		startPositions[0][1] =0;
	}

	/**
	 * Liefert die Parcursbreite zurück in Gittereinheiten
	 * @return
	 */
	public int getDimX() {
		return dimX;
	}

	/**
	 * Setzt die Parcursbreite in Gittereinheiten
	 * @param dimX Breite in Gittereinheiten
	 */
	public void setDimX(int dimX) {
		this.dimX = dimX;
	}

	/**
	 * Liefert die Parcurshoehe zurück in Gittereinheiten
	 * @return
	 */
	public int getDimY() {
		return dimY;
	}

	/**
	 * Liefert die Breite (X) des Parcours in mm zurueck
	 * @return
	 */
	public float getWidth(){
		return dimX* grid;
	}

	/**
	 * Liefert die Hoehe (Y) des Parcours in mm zurueck
	 * @return
	 */
	public float getHeight(){
		return dimY* grid;
	}
	/**
	 * Setzt die Parcurshoehe in Gittereinheiten
	 * @param dimX Hoehe in Gittereinheiten
	 */
	public void setDimY(int dimY) {
		this.dimY = dimY;
	}

	/**
	 * Fuegt ein Hinderniss ein
	 * @param obstacle Das Hinderniss
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 */
	public void addObstacle(Node obstacle, float x, float y) {
		addNode(obstacle,x,y,obstBG);
	}

	/**
	 * Fuegt ein Hinderniss ein
	 * @param obstacle Das Hinderniss
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse Absolut
	 */
	public void addObstacle(Node obstacle, float x, float y, float z) {
		addNode(obstacle,x,y,z,obstBG);
	}

	/**
	 * Fuegt ein Stueck Boden ein
	 * @param floor der Boden
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse Absolut
	 */
	public void addFloor(Node floor, float x, float y, float z) {
		addNode(floor,x,y,z,terrainBG);
	}
	
	/**
	 * Fuegt eine Node ein
	 * @param node Die Node
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param z Z-Achse absolut
	 * @param branchGroup Gruppe in die das Objekt rein soll
	 */
	public void addNode(Node node, float x, float y,float z,BranchGroup bg) {
		Transform3D translate = new Transform3D();

		node.setPickable(true);
//		node.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		
		translate.set(new Vector3f(x * grid, y* grid, z));

		TransformGroup tg = new TransformGroup(translate);
		tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg.setPickable(true);
		tg.addChild(node);
		bg.addChild(tg);
	}

	/**
	 * Fuegt eine Node ein
	 * @param node Die Node
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 * @param branchGroup Gruppe in die das Objekt rein soll
	 */
	public void addNode(Node node, float x, float y,BranchGroup bg) {
		addNode(node,x,y,0.0f,bg);
	}
	
	/**
	 * Fuegt eine Dartsellung der Lichtquelle ein
	 * @param light Die Lichtquelle
	 * @param x X-Achse im Parcours-Gitter
	 * @param y Y-Achse im Parcours-Gitter
	 */
	public void addLight(Node light, float x, float y, float z) {
		addNode(light,x,y,z,lightBG);
	}

	/**
	 * Fuegt eine Lichtquelle ein
	 * @param light Die Lichtquelle
	 */
	public void addLight(Node light) {
		lightBG.addChild(light);
	}
	
	
	/**
	 * Liefert die Licht-Branchgroup zurück
	 * @return
	 */
	public BranchGroup getLightBG() {
		return lightBG;
	}

	/**
	 * Liefert die Hinderniss-Branchgroup zurück
	 * @return
	 */
	public BranchGroup getObstBG() {
		return obstBG;
	}
	
	/**
	 * Liefert die Boden-Branchgroup zurück
	 * @return
	 */
	public BranchGroup getTerrainBG() {
		return terrainBG;
	}
	
	/**
	 * Legt die Startposition eines Bots fest
	 * @param bot Nummer des Bots (fängt bei 0 an zu zählen)
	 * @param x
	 * @param y
	 */
	public void setStartPosition(int bot, int x, int y){
		if (bot <= BOTS-1){
			startPositions[bot][0]=x;
			startPositions[bot][1]=y;
		}
	}

	/**
	 * Legt die Startrichtung eines Bots fest
	 * @param bot Nummer des Bots (fängt bei 0 an zu zählen)
	 * @param dir Richtung in Grad. 0 entspricht (x=1, y=0) dann im Uhrzeigersinn
	 */
	public void setStartHeading(int bot, int dir){
		if (bot <= BOTS-1){
			switch (dir) {
			case 0:
				startHeadings[bot][0]=1;
				startHeadings[bot][1]=0;
				break;
			case 90:
				startHeadings[bot][0]=0;
				startHeadings[bot][1]=-1;
				break;
			case 180:
				startHeadings[bot][0]=-1;
				startHeadings[bot][1]=0;
				break;
			case 270:
				startHeadings[bot][0]=0;
				startHeadings[bot][1]=1;
				break;

			default:
				break;
			}
		}
	}
	
	
	
	/**
	 * Liefert die Startposition eines Bots
	 * Wenn keine festgelegt wurde, dann die Default-Position  (0)
	 * @param bot
	 * @return
	 */
	public Vector3f getStartPosition(int bot){
		Vector3f pos = null;
		if (bot < BOTS)
			pos= new Vector3f(startPositions[bot][0]*grid + grid/2,startPositions[bot][1]*grid + grid/2,0.0f);
		else
			pos= new Vector3f(startPositions[0][0]*grid + grid/2,startPositions[0][1]*grid + grid/2,0.0f);
		
		return pos;
	}

	/**
	 * Liefert die Startrichtung eines Bots
	 * Wenn keine festgelegt wurde, dann die Default-Position  (0)
	 * @param bot
	 * @return
	 */
	public Vector3f getStartHeading(int bot){
		Vector3f pos = null;
		if (bot < BOTS)
			pos= new Vector3f(startHeadings[bot][0],startHeadings[bot][1],0.0f);
		else  // sonst leifer die Default-Richtung
			pos= new Vector3f(startHeadings[0][0],startHeadings[0][1],0.0f);
			
		if (pos.length()==0){
			pos.x=1.0f;
			ErrorHandler.error("getStartHeading wurde nach einer noch nicht gesetzten Heading gefragt. Setze Default");
		}
		
		pos.normalize();
		
		return pos;
	}
	
	
	/**
	 * Legt die Zielposition fest
	 * @param x
	 * @param y
	 */
	public void setFinishPosition(int x, int y){
		finishPosition[0]=x;
		finishPosition[1]=y;
	}

	/** 
	 * Liefert die Gitterbreite in mm zurueck
	 * @return
	 */
	public float getGrid() {
		return grid;
	}

	/**
	 * Prueft, ob ein Punkt innerhalb des Zielfeldes liegt
	 * @param pos
	 * @return
	 */
	public boolean finishReached(Vector3f pos){
		float minX = finishPosition[0]*grid ;
		float maxX = finishPosition[0]*grid + grid;
		float minY = finishPosition[1]*grid ;
		float maxY = finishPosition[1]*grid + grid;
		
		if ((pos.x > minX) && (pos.x < maxX) && (pos.y > minY) && (pos.y < maxY))
			return true;
		else
			return false;
	}
	
}
