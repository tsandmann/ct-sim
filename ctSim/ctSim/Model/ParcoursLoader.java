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
import java.io.PrintStream;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
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
import javax.vecmath.Vector4f;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

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

	/** Der eigentliche Parcours */
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
		Box box = new Box(parcours.getGrid() / 2 * lengthX, parcours.getGrid() / 2* lengthY, 0.2f, wallAppear);
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
		Box box = new Box(parcours.getGrid() / 2 * lengthX, parcours.getGrid() / 2* lengthY, World.PLAYGROUND_THICKNESS, app);
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
		Box box = new Box(parcours.getGrid() *0.5f , parcours.getGrid()  *0.5f, World.PLAYGROUND_THICKNESS, app);
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
			p[n]=points[n]*parcours.getGrid();
		
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
		pointLight.setPosition((x+0.5f)*parcours.getGrid(), (y+0.5f)*parcours.getGrid(), LIGHTZ);
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
	 * Prueft die angrenzenden Felder (ohne diagonalen), ob mindestens eines davon den uebergebenen Wert hat
	 * @param x X-Koordinate des mittelfeldes 
	 * @param y Y-Koordinate des mittelfeldes
	 * @param c Der zu suchende Feldtyp
	 * 
	 * @return 0 wenn kein Feld den Wert hat
	 */
	private int checkNeighbours(int x, int y, char c){
		if ((x>0) && (parcoursMap[x-1][y] == c))
				return 1;
		if ((x<parcours.getDimX()-1) && (parcoursMap[x+1][y] == c))
			return 1;		
		if ((y>0) && (parcoursMap[x][y-1] == c))
			return 1;
		if ((y<parcours.getDimY()-1) && (parcoursMap[x][y+1] == c))
			return 1;		
		
		
		return 0;
	}
		
		
	
	/**
	 * Liest die parcourMap ein und baut daraus einen Parcour zusammen
	 */
//	public void insertSceneGraph(TransformGroup obstRoot_, BranchGroup lightRoot_, BranchGroup terrainRoot_) {
	public void parse(){
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
							// Sind wir im Startbereich
							if (checkNeighbours(x,y,'.') != 0)
								createFloor(x, y,whiteFloorAppear);
							else
								createFloor(x, y,normalFloorAppear);
							break;
					    case '.':
							createFloor(x, y,whiteFloorAppear);
							break;
					    case ' ':
					    		createFloor(x, y,normalFloorAppear);
							break;
							
					    case '0':
							parcours.setStartPosition(0,x,y);
							createFloor(x, y,whiteFloorAppear);
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
					    case '+':
							createLine(x,y,LINE_CORNER_SE);
							break;
					    case '~':
							createLine(x,y,LINE_CORNER_SW);
							break;

						
					}
				    	
				    	
			}
       }
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

    			parse();		// Parcours Zusammenbauen
    			
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
		
		parse();		// Parcours Zusammenbauen
		
	}

	/**
	 * Liefert das soeben aufgebaute Parcours-Objekt zurueck
	 * @return
	 */
	public Parcours getParcours() {
		return parcours;
	}

	public void load_xml_file(String filename){
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			parser.parse(filename);
			// umwandeln in ein Document
			Document doc = parser.getDocument();
			
			// Und Anfangen mit dem abarbeiten
			
			//als erster suchen wir uns den Parcours-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while ((n != null)&& (!n.getNodeName().equals("parcours"))){
				print(n,System.out);
				n=n.getNextSibling();
			}
			// jetzt haben wir ihn
			// TODO hier koennte man die Attribute des Parcours verwenden

			int y=0;	// Anzahl der Zeilen im File
			int x=0;	// Anzahl der Spalten im File
			
			
			//	Eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			NodeList children=n.getChildNodes();	
	
			//	Anzahl der Zeilen und spalten bestimmen
	        for (int i=0; i<children.getLength(); i++){
	        		Node child =children.item(i);
	        		if (child.getNodeName().equals("line")){	        			
	        			y++;
	        			if (x < child.getChildNodes().item(0).getNodeValue().length())
	        				x = child.getChildNodes().item(0).getNodeValue().length();
	        		}
	        }
			
	        // Parcors vorbereiten
			parcours.setDimX(x);
			parcours.setDimY(y);

			// Und eine Map anlegen
			parcoursMap = new int[x][y];

			x=0; y=0;
			//	ParcoursMap aufbauen
	        for (int i=0; i<children.getLength(); i++){
	        		Node child =children.item(i);
	        		if (child.getNodeName().equals("line")){	        			
	        			char c[] = child.getChildNodes().item(0).getNodeValue().toCharArray();
	        			for (x=0; x<c.length; x++)
	        				parcoursMap[x][y]= c[x];
	        			y++;
	        		}
	        }
		
	        // ********** Appearances aus dem Document lesen 
	        
	        
	        
	        
	        
	        
			// Soweit fertig.
			parse();		// Parcours Zusammenbauen
			
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+ex);
		}
	}
	
	  static void print(Node node, PrintStream out) {
		    int type = node.getNodeType();
		    switch (type) {
		      case Node.ELEMENT_NODE:
		        out.print("<" + node.getNodeName());
		        NamedNodeMap attrs = node.getAttributes();
		        int len = attrs.getLength();
		        for (int i=0; i<len; i++) {
		            Attr attr = (Attr)attrs.item(i);
		            out.print(" " + attr.getNodeName() + "=\"" +
		                      attr.getNodeValue() + "\"");
		        }
		        out.print('>');
		        NodeList children = node.getChildNodes();
		        len = children.getLength();
		        for (int i=0; i<len; i++)
		          print(children.item(i), out);
		        out.print("</" + node.getNodeName() + ">");
		        break;
		      case Node.ENTITY_REFERENCE_NODE:
		        out.print("&" + node.getNodeName() + ";");
		        break;
		      case Node.CDATA_SECTION_NODE:
		        out.print("<![CDATA[" + node.getNodeValue() + "]]>");
		        break;
		      case Node.TEXT_NODE:
		        out.print(node.getNodeValue());
		        break;
		      case Node.PROCESSING_INSTRUCTION_NODE:
		        out.print("<?" + node.getNodeName());
		        String data = node.getNodeValue();
		        if (data!=null && data.length()>0)
		           out.print(" " + data);
		        out.println("?>");
		        break;
		    }
		  }
}
