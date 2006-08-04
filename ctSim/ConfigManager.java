package ctSim;

import java.io.File;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class ConfigManager {
	
	/**
	 * Enthaelt die gesamten Einzelparameter der Konfiguration 
	 */
	private static HashMap<String,String> config = new HashMap<String,String>();
	
	public static String getConfigValue(String key) {
		
		if(config == null)
			return null;
		return config.get(key);
	}
	
	/**
	 * L�dt die Konfiguration des Sims ein
	 * @throws Exception
	 */
	public static boolean loadConfigFile(File file) {
		
		try {
			if(file.exists()) {
				parseConfig(file);
				return true;
			}
		} catch(Exception e) {
			// TODO: sch�ner machen
		}
		return false;
	}
	
	/**
	 * Liest die Konfiguration des Sims ein
	 * @throws Exception
	 */
	private static void parseConfig(File file) throws Exception{
		// Ein DOMParser liest ein XML-File ein
		DOMParser parser = new DOMParser();
		try {
			// einlesen
			System.out.println("Lade Konfiguration aus: "+file); //$NON-NLS-1$
			
			parser.parse(file.getAbsolutePath());
			// umwandeln in ein Document
			Document doc = parser.getDocument();
			
			// Und Anfangen mit dem abarbeiten
			
			//als erster suchen wir uns den Parameter-Block
			Node n = doc.getDocumentElement().getFirstChild();
			while (n != null){
				if (n.getNodeName().equals("parameter")){ //$NON-NLS-1$
					String name = n.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
					String value = n.getAttributes().getNamedItem("value").getNodeValue(); //$NON-NLS-1$
					config.put(name,value);
				}
				n=n.getNextSibling();
			}
		} catch (Exception ex) {
			ErrorHandler.error("Probleme beim Parsen der XML-Datei: "+ex); //$NON-NLS-1$
			throw ex;
		}
	}
	
	/**
	 * Wandelt einen Pfad mit Bot-Binary von Windows zu Linux 
	 * @param in der Originalstring
	 * @return 
	 */
	private static String botPathWin2Lin(String in){
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
