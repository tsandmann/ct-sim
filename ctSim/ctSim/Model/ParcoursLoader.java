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
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.ImageComponent;
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
	
	/** Breite einer Linie */
	public static final float LINEWIDTH = 0.1f;	
	
	/** Linie horizontal */
	public static final float[] LINE_HORIZ = 	{ -0.5f, 0f - LINEWIDTH/2,0f, 
													   0.5f, 0f - LINEWIDTH/2,0f, 
													   0.5f, 0f + LINEWIDTH/2,0f,
													  -0.5f, 0f + LINEWIDTH/2,0f,
													  -0.5f, 0f - LINEWIDTH/2,0f}; 
	/** Linie vertikal */
	public static final float[] LINE_VERT =  	{ 0f - LINEWIDTH/2,-0.5f,0f,  // Start unten links 
													  0f + LINEWIDTH/2,-0.5f,0f,  // kurze Linie nach rechts
													  0f + LINEWIDTH/2, 0.5f,0f,  // Lange Linie hoch  
													  0f - LINEWIDTH/2, 0.5f,0f,  // kurze Linie nach links   
													  0f - LINEWIDTH/2,-0.5f,0f}; // lange Linie runter
	/** Linie mit Ecke SE */
	public static final float[] LINE_CORNER_SE = { 0f - LINEWIDTH/2,-0.5f               ,0f,  // Start unten links
													  0f + LINEWIDTH/2,-0.5f               ,0f,  // kurze Linie nach rechts   
													  0f + LINEWIDTH/2, 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie hoch 
													  0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach rechts
													  0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // kurze Linie hoch
													  0f -LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,  // Lange Linie nach links
													  0f - LINEWIDTH/2,-0.5f               ,0f};  // Lange Linie nach unten 
	/** Linie mit Ecke SW */
	public static final float[] LINE_CORNER_SW = { 0f - LINEWIDTH/2,-0.5f               ,0f,  // Start unten links
													  0f + LINEWIDTH/2,-0.5f               ,0f,  // kurze Linie nach rechts   
													  0f + LINEWIDTH/2, 0.0f + LINEWIDTH/2 ,0f,  // Lange Linie hoch 
													 -0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // Lange Linie nach links
													 -0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // kurze Linie runter
													  0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach links
													  0f - LINEWIDTH/2,-0.5f               ,0f};  // Lange Linie nach unten
	/** Linie mit Ecke NW */
	public static final float[] LINE_CORNER_NW ={ -0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // Start Links oben
													 -0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // kurze Linie runter
													  0f +LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach rechts
													  0f +LINEWIDTH/2 , 0.5f               ,0f,  // Lange Linie nach oben
													  0f -LINEWIDTH/2 , 0.5f               ,0f,  // kurze Linie nach links
													  0f -LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,  // lange Linie nach unten
													 -0.5f			  , 0.0f + LINEWIDTH/2 ,0f};  // Lange Linie nach links	
	/** Linie mit Ecke NE */
	public static final float[] LINE_CORNER_NE = { 0f +LINEWIDTH/2 , 0.5f               ,0f,  // Start oben rechts
												      0f -LINEWIDTH/2 , 0.5f               ,0f,  // kurze Linie nach links
													  0f -LINEWIDTH/2 , 0.0f - LINEWIDTH/2 ,0f,  // lange Linie nach unten
													  0.5f			  , 0.0f - LINEWIDTH/2 ,0f,  // Lange Linie nach rechts 
													  0.5f			  , 0.0f + LINEWIDTH/2 ,0f,  // kurze Linie nach oben 
													  0f +LINEWIDTH/2 , 0.0f + LINEWIDTH/2 ,0f,  // lange Linie nach links
													  0f +LINEWIDTH/2 , 0.5f			   ,0f,  // lange Linie nach oben
	};
	
	/** Verwaltet alle Aussehen */
	HashMap appearances = new HashMap();
	
	private int[][] parcoursMap = null;

	/** Der eigentliche Parcours */
	private Parcours parcours;
	
	/**
	 * Neuen ParcoursLoader instantiieren
	 */
	public ParcoursLoader() {
		super();
		parcours = new Parcours();
	}

	/**
	 * Erzeugt ein Wandsegment 
	 * Alle Postionen sind keine Weltkoordinaten,
	 * sondern ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * 
	 * @param x  Position in X-Richtung
	 * @param y  Position in X-Richtung
	 * @param lengthX Laenge der Wand in X-Richtung
	 * @param lengthY Laenge der Wand in Y-Richtung
	 * @param appearance
	 */
	private void createWall(int x, int y, int lengthX, int lengthY, Appearance appearance) {
		Box box = new Box(parcours.getGrid() / 2 * lengthX, parcours.getGrid() / 2* lengthY, 0.2f, appearance);
		parcours.addObstacle(box,x+lengthX/2.0f,y+lengthY/2.0f);
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
		parcours.addFloor(box,x+lengthX/2.0f,y+lengthY/2.0f, -World.PLAYGROUND_THICKNESS+0.001f);
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
	 * @param points Liste mit punkten entlang der
	 * @param appearance
	 */
	private void createLine(int x, int y, float[] points, Appearance appearance) {
		// zwei Polygone (Deckel und Boden) mit N Ecken
		float[] p = new float[points.length];
		int stripCounts[] = { points.length/3 };
		// Zaehler
		int n = 0;

		for (n=0; n< points.length; n++)
			p[n]=points[n]*parcours.getGrid();
		
	    createFloor(x, y,getAppearance(' '));

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
		
		ls.setAppearance(appearance);

		parcours.addFloor(ls,x+0.5f,y+0.5f, 0.002f);
	}	
		
	
	/**
	 * Erzeugt eine Sauele mit Lichtquelle obendrauf
	 * @param x X-Koordinate
	 * @param y Y-Koordinate
	 * @param lightAppearance Erscheinungsbild des Lichtes
	 * @param wallAppearance Erscheinungsbild der Säule
	 */
	private void createPillar(int x, int y,Appearance wallAppearance, Appearance lightAppearance){
		Cylinder pillar = new Cylinder(0.05f, 0.5f, wallAppearance);
		pillar.setPickable(true);

		Transform3D translate = new Transform3D();
		translate.rotX(0.5 * Math.PI);
		TransformGroup tg = new TransformGroup(translate);
		tg.setCapability(javax.media.j3d.Node.ENABLE_PICK_REPORTING);
		tg.addChild(pillar);
		
		parcours.addObstacle(tg,x+0.5f,y+0.5f);
		createLight(new BoundingSphere(new Point3d(0d,
				0d, 0d), 10d),new Color3f(1.0f, 1.0f, 0.9f),x,y, lightAppearance);
	}
	
	/**
	 * Fuegt ein Licht ein
	 * @param pointLightBounds
	 * @param pointLightColor
	 * @param x
	 * @param y
	 * @param appearance
	 */
	private void createLight(BoundingSphere pointLightBounds,Color3f pointLightColor, int x, int y,Appearance appearance) {
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
		lightSphere.setAppearance(appearance);
		parcours.addLight(lightSphere,x+0.5f,y+0.5f,0.5f);
	}

	/**
	 * Prueft die angrenzenden Felder (ohne diagonalen), ob mindestens eines davon den uebergebenen Wert hat
	 * @param x X-Koordinate des mittelfeldes 
	 * @param y Y-Koordinate des mittelfeldes
	 * @param c Der zu suchende Feldtyp
	 * 
	 * @return -1 wenn kein Feld den Wert hat. Wenn er einen Nachbarn findet dann die Richtung in Grad.
	 * 0 = (x=1, y=0) ab da im Uhrzeigersinn
	 */
	private int checkNeighbours(int x, int y, char c){
		if ((y>0) && (parcoursMap[x][y-1] == c))
			return 90;
		if ((y<parcours.getDimY()-1) && (parcoursMap[x][y+1] == c))
			return 270;		
		if ((x>0) && (parcoursMap[x-1][y] == c))
				return 180;
		if ((x<parcours.getDimX()-1) && (parcoursMap[x+1][y] == c))
			return 0;		
		
		
		return -1;
	}
		
		
	
	/**
	 * Liest die parcourMap ein und baut daraus einen Parcour zusammen
	 */
	public void parse(){
	int l;
    	int d;

    	
    	if (parcoursMap != null){
			for (int y = 0; y < parcours.getDimY(); y++){
				for (int x = 0; x < parcours.getDimX(); x++) 
					switch (parcoursMap[x][y]) {
						case 'X':
							createWall(x, y,1 ,1,getAppearance(parcoursMap[x][y]));
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
							createWall(x, y, l, 1,getAppearance('='));
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
							createWall(x, y, 1, l,getAppearance('#'));
							break;
					    case '*':
							createPillar(x, y,getAppearance('X'),getAppearance('*'));
							// Sind wir im Startbereich
							if (checkNeighbours(x,y,'.') != -1)
								createFloor(x, y,getAppearance('.'));
							else
								createFloor(x, y,getAppearance(' '));
							break;
					    case '.':
					    		//	TODO Boden optimieren, kacheln zusammenfassen
					    		createFloor(x, y,getAppearance(parcoursMap[x][y]));
							break;
					    case ' ':
					    		//	TODO Boden optimieren, kacheln zusammenfassen
					    		createFloor(x, y,getAppearance(parcoursMap[x][y]));
							break;
							
					    case '0':
							parcours.setStartPosition(0,x,y);
							createFloor(x, y,getAppearance(parcoursMap[x][y]));
							break;
					    case '1':
							parcours.setStartPosition(1,x,y);
							parcours.setStartHeading(1,checkNeighbours(x,y,'.'));
							createFloor(x, y,getAppearance(parcoursMap[x][y]));
							break;
					    case '2':
							parcours.setStartPosition(2,x,y);
							parcours.setStartHeading(2,checkNeighbours(x,y,'.'));
							createFloor(x, y,getAppearance(parcoursMap[x][y]));
							break;
					    case 'Z':
							parcours.setFinishPosition(x,y);
							createFloor(x, y,getAppearance(parcoursMap[x][y]));
							break;
							
					    case '-':
							createLine(x,y,LINE_HORIZ,getAppearance(parcoursMap[x][y]));
							break;
					    case '|':
							createLine(x,y,LINE_VERT,getAppearance(parcoursMap[x][y]));
							break;
					    case '/':
							createLine(x,y,LINE_CORNER_SE,getAppearance(parcoursMap[x][y]));
							break;
					    case '\\':
							createLine(x,y,LINE_CORNER_SW,getAppearance(parcoursMap[x][y]));
							break;
					    case '+':
							createLine(x,y,LINE_CORNER_NE,getAppearance(parcoursMap[x][y]));
							break;
					    case '~':
							createLine(x,y,LINE_CORNER_NW,getAppearance(parcoursMap[x][y]));
							break;

						
					}
				    	
				    	
			}
       }
	}    	
	
	/**
	 * Liefert das soeben aufgebaute Parcours-Objekt zurueck
	 * @return  Parcours-Objekt
	 */
	public Parcours getParcours() {
		return parcours;
	}

	/**
	 * Laedt einen Oarcours aus einer XML-Datei
	 * @param filename
	 * @throws Exception
	 */
	public void load_xml_file(String filename) throws Exception{
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
			while ((n != null)&& (!n.getNodeName().equals("parcours")))
				n=n.getNextSibling();
			// jetzt haben wir ihn

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
	        for (int i=children.getLength()-1; i>=0; i--){
	        		Node child =children.item(i);
	        		if (child.getNodeName().equals("line")){	        			
	        			char c[] = child.getChildNodes().item(0).getNodeValue().toCharArray();
	        			for (x=0; x<c.length; x++)
	        				parcoursMap[x][y]= c[x];
	        			y++;
	        		}
	        }
		
	        // ********** Appearances aus dem Document lesen 
	        
			//suchen wir uns den Otptics-Block
			n = doc.getDocumentElement().getFirstChild();
			while ((n != null)&& (!n.getNodeName().equals("optics")))
				n=n.getNextSibling();
			// jetzt haben wir ihn

			//	Eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			children=n.getChildNodes();	
			
			//	HashMap mit den Apearances aufbauen
	        for (int i=0; i<children.getLength()-1; i++){
	        		Node appearance =children.item(i);
	        		if (appearance.getNodeName().equals("appearance")){
	        			// Zuerst den Type extrahieren
	        			char item = appearance.getAttributes().getNamedItem("type").getNodeValue().toCharArray()[0];
	        			
	        			String texture = null;
	        			String clone = null;
	        			
	        			HashMap<String,String> colors = new HashMap<String,String>();
	        			
	        			NodeList features = appearance.getChildNodes();
	        			for (int j=0; j< features.getLength(); j++){
	        				if (features.item(j).getNodeName().equals("texture"))
	        					texture= features.item(j).getChildNodes().item(0).getNodeValue();
	        				if (features.item(j).getNodeName().equals("color"))
	        					colors.put(features.item(j).getAttributes().getNamedItem("type").getNodeValue(),features.item(j).getChildNodes().item(0).getNodeValue());
	        				if (features.item(j).getNodeName().equals("clone"))
	        					clone= features.item(j).getChildNodes().item(0).getNodeValue();
	        			}
	        				   
	        			
	        			addAppearance(item, colors, texture, clone);
	        		}
	        }
	        
			// Soweit fertig.
			parse();		// Parcours Zusammenbauen
			
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+filename+" : "+ex);
			throw ex;
		}
	}
	
	/**
	 * Liefert eine Appearance aus der Liste zurueck
	 * @param key Der Schluessel mit dem sie abgelegt wurde
	 * @return Die Appearance
	 */
	private Appearance getAppearance(int key) {
		Appearance app= (Appearance)appearances.get((char)key);
		if (app == null)
			ErrorHandler.error("Appearance fuer '"+(char)key+"' nicht gefunden!");
		return app;
	}
	
	/**
	 * Erzeugt eine Appearnace und fuegt die der Liste hinzu
	 * @param item Der Key, iunter dem diese Apperance abgelegt wird
	 * @param colors HashMap mit je Farbtyp und ASCII-Represenation der Farbe
	 * @param textureFile Der Name des Texture-Files
	 * @param clone Referenz auf einen schon bestehenden Eintrag, der geclonet werden soll
	 */
	@SuppressWarnings("unchecked")
	private void addAppearance(char item, HashMap colors, String textureFile, String clone){

		if (clone != null){
			appearances.put(item, appearances.get(clone.toCharArray()[0]));
			return;
		}
		
		Appearance appearance = new Appearance();
		
		if (colors != null){
			Material mat = new Material();

			Iterator it = colors.keySet().iterator();
			while (it.hasNext()) {
				String colorType = (String)it.next();
				String colorName = (String)colors.get(colorType);

				if (colorType.equals("ambient"))
					mat.setAmbientColor(new Color3f(Color.decode(colorName)));
				if (colorType.equals("diffuse"))
					mat.setDiffuseColor(new Color3f(Color.decode(colorName)));
				if (colorType.equals("specular"))
					mat.setSpecularColor(new Color3f(Color.decode(colorName)));
				if (colorType.equals("emmissive"))
					mat.setEmissiveColor(new Color3f(Color.decode(colorName)));
			}
			appearance.setMaterial(mat);

			
		}
				
		if (textureFile != null){
			TexCoordGeneration tcg = new TexCoordGeneration(
					TexCoordGeneration.OBJECT_LINEAR,
					TexCoordGeneration.TEXTURE_COORDINATE_3, 
					new Vector4f(1.0f, 1.0f, 0.0f, 0.0f),
					new Vector4f(0.0f, 1.0f, 1.0f, 0.0f), 
					new Vector4f(1.0f, 0.0f, 1.0f, 0.0f));
			appearance.setTexCoordGeneration(tcg);

			try {
				TextureLoader loader = new TextureLoader(ClassLoader.getSystemResource(textureFile), null);
				Texture2D texture = (Texture2D) loader.getTexture();
				texture.setBoundaryModeS(Texture.WRAP);
				texture.setBoundaryModeT(Texture.WRAP);

				// Mache die textur lesbar
				texture.setCapability(Texture.ALLOW_IMAGE_READ);
				ImageComponent[] imgs = texture.getImages();
				for (int i=0; i< imgs.length; i++)
					imgs[i].setCapability(ImageComponent.ALLOW_IMAGE_READ);
				
				appearance.setTexture(texture);
				appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
				
			} catch (Exception ex) {
				ErrorHandler.error("Probleme beim Laden der Textur: "+textureFile+" :"+ex);
			}
			
		}
		
		appearances.put(item,appearance);
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
