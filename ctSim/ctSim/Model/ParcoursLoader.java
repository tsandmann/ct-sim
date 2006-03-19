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

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Material;
import javax.media.j3d.PointLight;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.image.TextureLoader;

import ctSim.ErrorHandler;

/**
 * Diese Klasse hilft einen Parcours aus einer ASCII-Datei zu laden
 * 
 * @author bbe (bbe@heise.de)
 */
public class ParcoursLoader {
	/** Z-Koorndinate der Lampen */
	public static final float LIGHTZ = 0.5f;
	
	public static final float LINEWIDTH = 0.1f;
	
	public static final float[] LINE_HORIZ = 	{ -0.5f, 0f - LINEWIDTH/2,0f, 
													   0.5f, 0f - LINEWIDTH/2,0f, 
													   0.5f, 0f + LINEWIDTH/2,0f,
													  -0.5f, 0f + LINEWIDTH/2,0f,
													  -0.5f, 0f - LINEWIDTH/2,0f}; 
	public static final float[] LINE_VERT =  	{ 0f - LINEWIDTH/2,-0.5f,0f,  // Start unten links 
													  0f + LINEWIDTH/2,-0.5f,0f,  // kurze Linie nach rechts
													  0f + LINEWIDTH/2, 0.5f,0f,  // Lange Linie hoch  
													  0f - LINEWIDTH/2, 0.5f,0f,  // kurze Linie nach links   
													  0f - LINEWIDTH/2,-0.5f,0f}; // lange Linie runter
	
	public static final float[] LINE_CORNER_SE = { 0f - LINEWIDTH/2,-0.5f               ,0f,  // Start unten links
													  0f + LINEWIDTH/2,-0.5f               ,0f,  // kurze Linie nach rechts   
													  0f + LINEWIDTH/2, 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie hoch 
													  0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach rechts
													  0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // kurze Linie hoch
													  0f -LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,  // Lange Linie nach links
													  0f - LINEWIDTH/2,-0.5f               ,0f};  // Lange Linie nach unten 
	public static final float[] LINE_CORNER_SW = { 0f - LINEWIDTH/2,-0.5f               ,0f,  // Start unten links
													  0f + LINEWIDTH/2,-0.5f               ,0f,  // kurze Linie nach rechts   
													  0f + LINEWIDTH/2, 0.0f + LINEWIDTH/2 ,0f,  // Lange Linie hoch 
													 -0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // Lange Linie nach links
													 -0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // kurze Linie runter
													  0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach links
													  0f - LINEWIDTH/2,-0.5f               ,0f};  // Lange Linie nach unten
	public static final float[] LINE_CORNER_NW ={ -0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // Start Links oben
													 -0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // kurze Linie runter
													  0f +LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach rechts
													  0f +LINEWIDTH/2 , 0.5f               ,0f,  // Lange Linie nach oben
													  0f -LINEWIDTH/2 , 0.5f               ,0f,  // kurze Linie nach links
													  0f -LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,  // lange Linie nach unten
													 -0.5f			  , 0.0f + LINEWIDTH/2 ,0f};  // Lange Linie nach links	
	
	public static final float[] LINE_CORNER_NE = { 0f +LINEWIDTH/2 , 0.5f               ,0f,  // Start oben rechts
												      0f -LINEWIDTH/2 , 0.5f               ,0f,  // kurze Linie nach links
													  0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,  // lange Linie nach unten
													  0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach rechts 
													  0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // kurze Linie nach oben 
													  0f +LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,  // lange Linie nach links
													  0f +LINEWIDTH/2 , 0.5f			   ,0f,  // lange Linie nach oben
	};
	/** Aussehen von Hindernissen */
	private Appearance wallAppear;

	/** Aussehen von weißem Fußboden */
	private Appearance whiteFloorAppear;

	/** Aussehen von normalem Fußboden */
	private Appearance normalFloorAppear;	

	/** Aussehen des Startfeldes Bot1*/
	private Appearance start1FloorAppear;	

	/** Aussehen des Startfeldes Bot2*/
	private Appearance start2FloorAppear;	
	
	/** Aussehen des Ziels*/
	private Appearance finishFloorAppear;		

	/** Aussehen der Linien*/
	private Appearance lineAppear;		
	
	
	String[] test = { 
			"============#", 
			"#     X   #O#", 
			"#   X   * # #", 
			"# *   === # #",
			"# =====     #", 
			"#     *  ===#", 
			"#=== ==     #", 
			"#     X X   #",
			"==Z=======12#" };

	
/*	String[] test = { 
			"    .        ", 
			"    *        ", 
			"    X        " };
*/	
/*

	String[] test = { 
			"XXXXXXXXXXXXX", 
			"X   X       X", 
			"X X   XXXX  X",
			"X XXXXX     X", 
			"X     X  XXXX", 
			"XXXX XX     X", 
			"X  *  X X   X",
			"XX XXXXXXX  X" };
*/
/*	String[] test = { 
			"==           ", 
			"===          ", 
			"===========  ", 			
			"============ ", 
			"XXXXXXXXXXXXX" };
*/	
	
	private int[][] parcoursMap = null;

	private Parcours parcours;

	/** Erscheinungsbild von Lichtquellen */
	private Appearance lightSourceAppear; 
	
	/**
	 * Neuen ParcoursLoader instantiieren
	 */
	public ParcoursLoader() {
		super();
		parcours = new Parcours();
		
		// Weißer-Boden-Material
		Material mat = new Material();
		mat.setAmbientColor(new Color3f(1f, 1f, 1f));
		mat.setDiffuseColor(new Color3f(1f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		// Aussehen des Bodens
		whiteFloorAppear = new Appearance();
		whiteFloorAppear.setMaterial(mat);

		// Normaler-Boden-Material
		mat = new Material();
		mat.setAmbientColor(new Color3f(Color.GRAY));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		// Aussehen des Bodens
		normalFloorAppear= new Appearance();
		normalFloorAppear.setMaterial(mat);
		
		// Start1-Boden-Material
		mat = new Material();
		mat.setAmbientColor(new Color3f(Color.RED));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		// Aussehen des Bodens
		start1FloorAppear= new Appearance();
		start1FloorAppear.setMaterial(mat);
		
		// Start1-Boden-Material
		mat = new Material();
		mat.setAmbientColor(new Color3f(Color.BLUE));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		// Aussehen des Bodens
		start2FloorAppear= new Appearance();
		start2FloorAppear.setMaterial(mat);

		// Ziel-Boden-Material
		mat = new Material();
		mat.setAmbientColor(new Color3f(Color.YELLOW));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		// Aussehen des Bodens
		finishFloorAppear= new Appearance();
		finishFloorAppear.setMaterial(mat);
		
		
		// Lichtquellen-Material
		mat = new Material();
		mat.setEmissiveColor(new Color3f(1f, 1f, 0.7f));
		mat.setAmbientColor(new Color3f(1f, 1f, 0f));
		mat.setDiffuseColor(new Color3f(1f, 1f, 0f));
		mat.setSpecularColor(new Color3f(0.7f, 0.7f, 0.7f));

		// Aussehen der Lichtquellen
		lightSourceAppear = new Appearance();
		lightSourceAppear.setMaterial(mat);
		
		// Wand-Material
		mat = new Material();
		mat.setAmbientColor(new Color3f(Color.LIGHT_GRAY));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		
		// Aussehen der Hindernisse -- dunkelgrau:
		wallAppear = new Appearance();
		wallAppear.setMaterial(mat);

		// ...und mit einer Textur ueberzogen:
		TexCoordGeneration tcg = new TexCoordGeneration(
				TexCoordGeneration.OBJECT_LINEAR,
				TexCoordGeneration.TEXTURE_COORDINATE_3, new Vector4f(1.0f,
						1.0f, 0.0f, 0.0f),
				new Vector4f(0.0f, 1.0f, 1.0f, 0.0f), new Vector4f(1.0f, 0.0f,
						1.0f, 0.0f));
		wallAppear.setTexCoordGeneration(tcg);

		TextureLoader loader = new TextureLoader(ClassLoader
				.getSystemResource(World.OBST_TEXTURE), null);
		Texture2D texture = (Texture2D) loader.getTexture();
		texture.setBoundaryModeS(Texture.WRAP);
		texture.setBoundaryModeT(Texture.WRAP);
		wallAppear.setTexture(texture);

		// Linien-Material
		mat = new Material();
		mat.setAmbientColor(new Color3f(Color.RED));
		mat.setDiffuseColor(new Color3f(0.8f, 1f, 1f));
		mat.setSpecularColor(new Color3f(1f, 1f, 1f));
		// Aussehen des Bodens
		lineAppear= new Appearance();
		lineAppear.setMaterial(mat);
		
	}

	/**
	 * Erzeugt ein Wandsegment 
	 * Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x  Position in X-Richtung
	 * @param y  Position in X-Richtung
	 * @param lengthX Laenge der Wand in X-Richtung
	 * @param lengthX Laenge der Wand in Y-Richtung
	 */
	private void createWall(int x, int y, int lengthX, int lengthY) {
		Box box = new Box(Parcours.GRID / 2 * lengthX, Parcours.GRID / 2* lengthY, 0.2f, wallAppear);
		parcours.addObstacle(box,(float)x+lengthX/2.0f,(float)y+lengthY/2.0f);
	}

	
	/**
	 * Erzeugt ein Stück Fußboden 
	 * Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x  Position in X-Richtung
	 * @param y  Position in X-Richtung
	 * @param lengthX Länge der Fläche in X-Richtung
	 * @param lengthY Länge der Fläche in Y-Richtung
	 * @param app Aussehen des Bodens
	 */
	@SuppressWarnings("unused")
	private void createFloor(int x, int y, int lengthX, int lengthY, Appearance app) {
		Box box = new Box(Parcours.GRID / 2 * lengthX, Parcours.GRID / 2* lengthY, World.PLAYGROUND_THICKNESS, app);
		parcours.addFloor(box,x+lengthX/2.0f,(float)y+lengthY/2.0f, -World.PLAYGROUND_THICKNESS+0.001f);
	}	

	/**
	 * Erzeugt ein Stück Fußboden 
	 * Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x  Position in X-Richtung
	 * @param y  Position in X-Richtung
	 * @param app Aussehen des Bodens
	 */
	private void createFloor(int x, int y, Appearance app) {
		Box box = new Box(Parcours.GRID *0.5f , Parcours.GRID  *0.5f, World.PLAYGROUND_THICKNESS, app);
		parcours.addFloor(box,x+0.5f,y+0.5f, -World.PLAYGROUND_THICKNESS+0.001f);
	}	

	/**
	 * Erzeugt eine Linie auf dem Boden 
	 * Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x  Position in X-Richtung
	 * @param y  Position in X-Richtung
	 * @param type Art der Linie
	 */
	private void createLine(int x, int y, float[] points) {
		// zwei Polygone (Deckel und Boden) mit N Ecken
		float[] p = new float[points.length];
		int stripCounts[] = { points.length/3 };
		// Zaehler
		int n = 0;

		for (n=0; n< points.length; n++)
			p[n]=points[n]*Parcours.GRID;
		
	    createFloor(x, y,normalFloorAppear);

		// Polygone in darstellbare Form umwandeln
		GeometryInfo gi = new GeometryInfo(GeometryInfo.POLYGON_ARRAY);
		gi.setCoordinates(p);
		gi.setStripCounts(stripCounts);

		NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		gi.recomputeIndices();

		Stripifier st = new Stripifier();
		st.stripify(gi);
		gi.recomputeIndices();

		// Hinzufuegen der Ober- und Unterseite des Linien-Shape3D
		Shape3D ls = new Shape3D();
		ls.addGeometry(gi.getGeometryArray());
		
		ls.setAppearance(lineAppear);

		parcours.addFloor(ls,x+0.5f,y+0.5f, 0.002f);
	}	
		
	
	/**
	 * Erzeugt eine Sauele mit Lichtquelle obendrauf
	 * @param x X-Koordinate
	 * @param y Y-Koordinate
	 * @return eine Transformgroup, die die Sauele enthaelt
	 */
	private void createPillar(int x, int y){
		Cylinder pillar = new Cylinder(0.05f, 0.5f, wallAppear);
		pillar.setPickable(true);

		Transform3D translate = new Transform3D();
		translate.rotX(0.5 * Math.PI);
		TransformGroup tg = new TransformGroup(translate);
		tg.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg.addChild(pillar);
		
		parcours.addObstacle(tg,x+0.5f,y+0.5f);
		createLight(new BoundingSphere(new Point3d(0d,
				0d, 0d), 10d),new Color3f(1.0f, 1.0f, 0.9f),x,y);
	}
	
	/**
	 * Fuegt ein Licht ein
	 * @param pointLightBounds
	 * @param pointLightColor
	 * @param x
	 * @param y
	 */
	private void createLight(BoundingSphere pointLightBounds,Color3f pointLightColor, int x, int y) {
		// Lichter bestehen aus dem echten Licht
		PointLight pointLight = new PointLight();
		pointLight.setColor(pointLightColor);
		pointLight.setPosition((x+0.5f)*Parcours.GRID, (y+0.5f)*Parcours.GRID, LIGHTZ);
		pointLight.setInfluencingBounds(pointLightBounds);
		pointLight.setAttenuation(1f, 3f, 0f);
		pointLight.setEnable(true);
		parcours.addLight(pointLight);

		//Und einer gelben Kugel, um es zu visulaisieren
		Sphere lightSphere = new Sphere(0.07f);
		lightSphere.setAppearance(lightSourceAppear);
		parcours.addLight(lightSphere,x+0.5f,y+0.5f,0.5f);
	}

	
	
	/**
	 * Fuegt in die uebergebene TransformGroup das geladene ein
	 * 
	 * @param root
	 *            Die Gruppe zu der alles hinzugefuegt wird
	 */
	public void insertSceneGraph(TransformGroup obstRoot, BranchGroup lightRoot, BranchGroup terrainRoot) {
    	int l;
    	int d;

    	if (parcoursMap != null){
			for (int y = 0; y < parcours.getDimY(); y++){
				for (int x = 0; x < parcours.getDimX(); x++) 
					switch (parcoursMap[x][y]) {
						case 'X':
							createWall(x, y,1 ,1);
							break;
					    case '=':
					    	l=0;
					    	d=x;
					    	// ermittle die Laenge der zusammenhaengenden Wand
					    	while ((d< parcours.getDimX()) && (parcoursMap[d][y] == '=')){
					    		parcoursMap[d][y] = 'O';  // Feld ist schon bearbeitet
					    		l++; // Laenge hochzaeheln
					    		d++;
					    	}
							createWall(x, y, l, 1);
							break;
					    case '#':
					    	l=0;
					    	d=y;
					    	// ermittle die Laenge der zusammenhaengenden Wand
					    	while ((d< parcours.getDimY()) && (parcoursMap[x][d] == '#')){
					    		parcoursMap[x][d] = 'O';  // Feld ist schon bearbeitet
					    		l++; // Laenge hochzaeheln
					    		d++;
					    	}
							createWall(x, y, 1, l);
							break;
					    case '*':
							createPillar(x, y);
							createFloor(x, y,normalFloorAppear);
							break;
					    case '.':
							createFloor(x, y,whiteFloorAppear);
							break;
					    case ' ':
					    	createFloor(x, y,normalFloorAppear);
							break;							
					    case '1':
							parcours.setStartPosition(1,x,y);
							createFloor(x, y,start1FloorAppear);
							break;
					    case '2':
							parcours.setStartPosition(2,x,y);
							createFloor(x, y,start2FloorAppear);
							break;
					    case 'Z':
							parcours.setFinishPosition(x,y);
							createFloor(x, y,finishFloorAppear);
							break;
					    case '-':
							createLine(x,y,LINE_HORIZ);
							break;
					    case '|':
							createLine(x,y,LINE_VERT);
							break;
					    case '/':
							createLine(x,y,LINE_CORNER_NE);
							break;
					    case '\\':
							createLine(x,y,LINE_CORNER_NW);
							break;
					    case '<':
							createLine(x,y,LINE_CORNER_SE);
							break;
					    case '>':
							createLine(x,y,LINE_CORNER_SW);
							break;

						
					}
				    	
				    	
			}
    	}
    	
    	Transform3D translate = new Transform3D();
    	translate.setTranslation(new Vector3f(-parcours.getDimX()/2*Parcours.GRID,-parcours.getDimY()/2*Parcours.GRID,0f));
    	TransformGroup tg = new TransformGroup();
    	tg.setTransform(translate);
    	tg.addChild(parcours.getObstBG());
    	obstRoot.addChild(tg);

    	tg = new TransformGroup();
    	tg.setTransform(translate);
    	tg.addChild(parcours.getLightBG());
    	lightRoot.addChild(tg);
    	
    	tg = new TransformGroup();
    	tg.setTransform(translate);
    	tg.addChild(parcours.getTerrainBG());
    	terrainRoot.addChild(tg);

	}
	
	/**
	 * Laedt den Default-Testparcours
	 */
	public void load() {
	}
	
	/**
	 * Lädt einen Parcours aus einer Datei in parcours
	 * @param filename
	 */
	public void load_file(String filename){
		File inputFile = new File(filename);

		// TODO Map nur so groß machen, wie wir sie auch brauchen
		parcoursMap = new int[100][100];

		
		int c;
		int x=0;
		int y=0;
		int xmax=0;
		
		try {
			FileReader in = new FileReader(inputFile);
			//Lesen, bis nix mehr da ist
		    while ((c = in.read()) != -1){
		    	switch (c){
		    		case '\n':
			    		y++;
			    		if (x>xmax)
			    			xmax=x;
			    		x=0;
			    		break;
		    		case '\r':
		    			break;
		    		default:
			    		parcoursMap[x][y]=c;
		    			x++;
		    	}
		    }
		    in.close();
		    
		    parcours.setDimY(y+1);	
    		parcours.setDimX(xmax);

		} catch (IOException ex){
			ErrorHandler.error("Probleme beim Lesen der Datei: "+filename+" "+ex);
		}
	}

	/**
	 * Lädt den Test-Parcours in parcours
	 */
	public void load_test(){
		char[] line = new char[test[0].length()];

		parcours.setDimX(test[0].length());
		parcours.setDimY(test.length);
		parcoursMap = new int[parcours.getDimX()][parcours.getDimY()];
		
		for (int y=0; y< parcours.getDimY(); y++){
			test[y].getChars(0, test[y].length(), line, 0);
			for (int x=0; x< parcours.getDimX(); x++)
				parcoursMap[x][y]=	line[x];
		}
		
	}

	
	
}
