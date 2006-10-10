package ctSim.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ctSim.util.XmlDocument;

//$$ doc Config
public class Config extends HashMap<String, String> {
    private static final long serialVersionUID = 6761099832031843055L;

	public static class SourceFile extends File {
        private static final long serialVersionUID = 3022935037996253818L;

    	public SourceFile(String pathAndName) {
    		super(pathAndName);
    	}
    }

	/** <p>Default-Werte der Konfiguration. Ist ein Array, um sie im Quelltext
	 * m&ouml;glichst bequem notieren zu k&ouml;nnen.</p>
	 *
	 * <p>Vorteile: Die Werte sind hier zentral statt quer durch den Quelltext
	 * verteilt; Code, der {@link #getValue(String)} aufruft, kann einfacher
	 * werden, da nicht dauernd der R&uuml;gabewert gegen <code>null</code>
	 * gepr&uuml;ft werden muss &ndash; wenn in diesem Array ein Wert steht,
	 * kann getValue(String) kein <code>null</code> mehr liefern.</p>*/
	private static final String[] configDefaults = {
		"botport", "10001",
		"judge", "ctSim.model.rules.DefaultJudge",
		"worlddir", ".", //$$ besser dokumentieren in ct-sim.xml und Co.
		"botdir", ".", //$$ besser dokumentieren in ct-sim.xml und Co.
		"useContestConductor", "false",
		"contestBotTargetDir", "tmp",
		"contestBotFileNamePrefix", "tmp-contest-bot",
		"contestBotFileNameSuffix", ".exe",
		"simTimePerStep", "10",
		"ctSimTimeout", "10000",
	};

	//LODO Klappt der Link "#get" im Javadoc? get ist geerbt
	/**
	 * L&auml;dt die <code>&lt;parameter></code>-Tags aus der
	 * Konfigurationsdatei des Sims. Die Werte der Tags sind dann mittels
	 * {@link #get(String)} verf&uuml;gbar.
	 * @param file Konfigurationsdatei dem von "config/config.dtd"
	 * vorgeschriebenen XML-Format.
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public Config(SourceFile file)
	throws SAXException, IOException, ParserConfigurationException {
		// configDefaults laden
		assert configDefaults.length % 2 == 0;
		for (int i = 0; i < configDefaults.length; i += 2)
			put(configDefaults[i], configDefaults[i + 1]);

		// Datei laden
		try {
			if (file.exists()) {
				System.out.println("Lade Konfiguration aus '"+file+"'");
                for(Node n : new XmlDocument(file).
                	getNodeList("/ct-sim/parameter")) {
                	put(n.getAttributes().getNamedItem("name").getNodeValue(),
                		n.getAttributes().getNamedItem("value").getNodeValue());
                }
			}
			else {
				throw new FileNotFoundException("Konfigurationsdatei '"+file+
						"' nicht gefunden");
			}
		} catch (XPathExpressionException e) {
			// "Kann nicht passieren"
			e.printStackTrace();
		} catch (DOMException e) {
			// Obskurer Fehler, wenn die Laenge der DOMStrings nicht reicht
			e.printStackTrace();
		}
	}
}
