package ctSim;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Material;
import javax.media.j3d.TexCoordGeneration;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.vecmath.Color3f;
import javax.vecmath.Vector4f;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.j3d.utils.image.TextureLoader;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import ctSim.controller.Config;
import ctSim.util.FmtLogger;

//TODO Typen verifizieren
public class ConfigManager {
	static FmtLogger lg = FmtLogger.getLogger("ctSim.ConfigManager");

	/** <p>Enth&auml;lt die Einzelparameter der Konfiguration (spiegelt also
	 * die <code>&lt;parameter></code>-Tags wider)</p>
	 *
	 * <p>Verwendung: Wird zun&auml;chst auf die Default-Werte gesetzt, die aus
	 * dem hartkodierten Array <code>configDefaults</code> kommen. Beim
	 * sp&auml;teren Parsen der Konfigurationsdatei werden Defaults
	 * dann m&ouml;glicherweise &uuml;berschrieben.</p> */
	private static Config config;

	/** Hilfskonstruktion. Sobald die Implementierung von getBotConfig()
	 * &uuml;berarbeitet ist, wird das unn&ouml;tig. */
	private static String filename;

	public static String getValue(String key) {
		return config.get(key);
	}

	/**
	 * L&auml;dt die <code>&lt;parameter></code>-Tags aus der
	 * Konfigurationsdatei des Sims. Die Werte der Tags sind dann mittels
	 * {@link #getValue(String)} verf&uuml;gbar.
	 * @param file Konfigurationsdatei dem von "config/config.dtd"
	 * vorgeschriebenen XML-Format.
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void loadConfigFile(Config.SourceFile file)
	throws SAXException, IOException, ParserConfigurationException {
		config = new Config(file); //$$ Sollte weg: von Pico holen
		filename = file.getPath();
	}

	/*
	 * Liefert die Config zu einem Bot zurueck
	 * @param filename
	 * @param botId
	 * @return
	 */
	public static HashMap getBotAppearances(String botId) {
		HashMap rv;
		// TODO Sinnvolle Zuordnung von Bot-Name zu Config
		rv = getBotConfig(botId);
		if (rv == null) {
			lg.warn("Keine BotConfig f\u00FCr '%s' in der Config-Datei " +
					"gefunden; lade Default", botId);
			rv = getBotConfig("default");
			if (rv == null) {
				lg.warn("Keine Default-BotConfig in der Config-Datei " +
						"gefunden; starte ohne");
			}
		}
		return rv;
	}

	//TODO Zu kompliziert. Wette: Diese Methode kann auf 15 Zeilen vereinfacht werden, wenn sie mit XPath neuimplementiert wird
	private static HashMap getBotConfig(String botId) {

		boolean found = false;

		HashMap botConfig = new HashMap();

		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			String configFile= filename;
			System.out.println("Lade Bot-Aussehen aus: "+configFile); //$NON-NLS-1$

			parser.parse(configFile);
			// umwandeln in ein Document
			Document doc = parser.getDocument();

			// Und Anfangen mit dem abarbeiten

			//als erster suchen wir uns den Parcours-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while ((n != null)&& (!n.getNodeName().equals("bots"))) { //$NON-NLS-1$
				n=n.getNextSibling();
			}

			//	Eine Liste aller Kinder des Parcours-Eitnrags organsisieren
			NodeList bots=n.getChildNodes();

			for(int b=0; b<bots.getLength()-1;b++){
				Node botSection = bots.item(b);
				// Ist das ueberhaupt ein Bot-Eintrag?
				if (botSection.getNodeName().equals("bot")) //$NON-NLS-1$
					// Und ist es auch der gesuchte
					if (botSection.getAttributes().getNamedItem("name").getNodeValue().equals(botId)){ //$NON-NLS-1$
						found=true;
						NodeList children = botSection.getChildNodes();

						//	HashMap mit den Apearances aufbauen
				        for (int i=0; i<children.getLength()-1; i++){
				        		Node appearance =children.item(i);
				        		if (appearance.getNodeName().equals("appearance")){ //$NON-NLS-1$
				        			// Zuerst den Type extrahieren
				        			String item = appearance.getAttributes().getNamedItem("type").getNodeValue(); //$NON-NLS-1$

				        			String texture = null;
				        			String clone = null;

				        			HashMap<String,String> colors = new HashMap<String,String>();

				        			NodeList features = appearance.getChildNodes();
				        			for (int j=0; j< features.getLength(); j++){
				        				if (features.item(j).getNodeName().equals("texture")) //$NON-NLS-1$
				        					texture= features.item(j).getChildNodes().item(0).getNodeValue();
//				        				 // TODO wir nutzen nur noch eine farbe, daher kann die auflistung von ambient und Co entfallen
				        				if (features.item(j).getNodeName().equals("color")) //$NON-NLS-1$
				        					colors.put(features.item(j).getAttributes().getNamedItem("type").getNodeValue(),features.item(j).getChildNodes().item(0).getNodeValue()); //$NON-NLS-1$
				        				if (features.item(j).getNodeName().equals("clone")) //$NON-NLS-1$
				        					clone= features.item(j).getChildNodes().item(0).getNodeValue();
				        			}

				        			addAppearance(botConfig, item, colors, texture, clone);
				        		}
				        }
				}
			}
		} catch (Exception ex) {
			System.err.println("Probleme beim Parsen der XML-Datei: ");
			ex.printStackTrace();
		}

		if (found == true)
			return botConfig;
		return null;
	}

	/*
	 * Erzeugt eine Appearnace und fuegt die der Liste hinzu
	 * @param appearances Die Hashmap in der das Pappearance eingetragen wird
	 * @param item Der Key, iunter dem diese Apperance abgelegt wird
	 * @param colors HashMap mit je Farbtyp und ASCII-Represenation der Farbe
	 * @param textureFile Der Name des Texture-Files
	 * @param clone Referenz auf einen schon bestehenden Eintrag, der geclonet werden soll
	 */
	public static void addAppearance(HashMap<String,Appearance> appearances, String item, HashMap colors, String textureFile, String clone){

		if (clone != null){
			appearances.put(item, appearances.get(clone));
			return;
		}

		Appearance appearance = new Appearance();

		if (colors != null){
			Material mat = new Material();

			Iterator it = colors.keySet().iterator();
			while (it.hasNext()) {
				String colorType = (String)it.next();
				String colorName = (String)colors.get(colorType);

				// TODO wir nutzen nur noch eine farbe, daher kann die auflistung von ambient und Co entfallen
				if (colorType.equals("ambient")) //$NON-NLS-1$
					appearance.setColoringAttributes(new ColoringAttributes(new Color3f(Color.decode(colorName)), ColoringAttributes.FASTEST));
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
				appearance.setTexture(texture);
			} catch (Exception e) {
				lg.warn(e, "Texturdatei '%s' nicht gefunden", textureFile);
			}

		}

		appearances.put(item,appearance);
	}

	/**
	 * Wandelt einen Pfad mit Bot-Binary von Windows zu Linux
	 * @param in der Originalstring
	 * @return
	 */
	private static String botPathWin2Lin(String in){
		if (in == null)
			return null;
		String tmp = in.replace('\\','/');
		tmp= tmp.replace("exe","elf");
		return tmp.replace("Debug-W32","Debug-Linux");
	}

	/**
	 * Wandelt einen Pfad mit Bot-Binary von Linux zu Windows
	 * @param in der Originalstring
	 * @return
	 */
	private static String botPathLin2Win(String in){
		if (in == null)
			return null;
		String tmp = in.replace('/','\\');
		tmp= tmp.replace("elf","exe");
		return tmp.replace("Debug-Linux","Debug-W32");
	}

	/**
	 * Passt einen Pfad an das aktuelle OS an
	 * @param in der Originalstring
	 * @return
	 */
	public static String path2Os(String in){
		if (System.getProperty("os.name").indexOf("Linux") >=0)
			return botPathWin2Lin(in);
		else if (System.getProperty("os.name").indexOf("OS X") >=0)
			return botPathWin2Lin(in);
		else
			return botPathLin2Win(in);
	}
}
