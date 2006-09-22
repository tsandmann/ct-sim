package ctSim.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import javax.swing.ImageIcon;

/**
 * <p>
 * Hilfsklasse, die im Dateisystem ein Verzeichnis abklappert und alle dortigen
 * Dateien als Icons l&auml;dt (genauer: als Instanzen der Klasse
 * {@link ImageIcon}).
 * </p>
 * <p>
 * <strong>Verwendungsbeispiel:</strong>
 * </p>
 * <ol>
 * <li><code>IconHashMap icons = new
 * IconHashMap(new File("pfad/zu/den/icons"));</code></li>
 * <li><code>meinWurstButton.setIcon(icons.get("Wurst"))</code>, um dem
 * Button das Icon zuzuweisen, was unter dem Dateinamen
 * "pfad/zu/den/icons/Wurst.xyz" zu finden war. "xyz" steht dabei f&uuml;r eine
 * beliebige Erweiterung; sie wird von dieser Klasse ignoriert.</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Sinn:</strong> Es muss nicht mehr jedes Icon pers&ouml;nlich
 * angefordert werden, was zu Vereinfachungen f&uuml;hrt:
 * <ul>
 * <li>beliebige Mengen Icons k&ouml;nnen mit einer einzelnen Zeile geladen
 * werden: <code>IconHashMap ganzeChose = new
 * IconHashMap(new File("IconUnterverzeichnis"));</code></li>
 * <li>Beim Erweitern der Applikation k&ouml;nnen Icons hinzugef&uuml;gt
 * werden, indem nur die Icon-Datei ins entsprechende Verzeichnis gelegt wird.
 * Code-&Auml;nderungen zum Laden o.&auml;. sind nicht n&ouml;tig.</li>
 * </ul>
 * </p>
 *
 * @see ImageIcon
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class IconHashMap extends HashMap<String, ImageIcon> {
    private static final long serialVersionUID = 7916128473992195865L;

    /** Um sp&auml;ter in Fehlermeldungen den Pfad angeben zu
     * k&ouml;nnen */
    private File parentDir;

    /**
	 * <p>
	 * Erzeugt eine Instanz, die das angegebene Verzeichnis abklappert und alle
	 * dortigen Dateien als Icons l&auml;dt. Der Abklappervorgang erfolgt nicht
	 * rekursiv, d.h. Unterverzeichnisse werden von der vorliegenden
	 * Implementierung nicht ber&uuml;cksichtigt.
	 * </p>
	 * <p>
	 * Das Verhalten der Klasse bei Namenskollisionen, d.h. falls zwei Dateien
	 * mit gleichen Namen und verschiedenen Erweiterungen gefunden werden
	 * (kaese.png, kaese.gif), ist undefiniert.
	 * </p>
	 *
	 * @param parentDir Das Verzeichnis, das die zu ladenden Bilddateien
	 *            enth&auml;lt.
	 * @throws NullPointerException Falls parentDir <code>null</code> ist.
	 * @throws FileNotFoundException Falls parentDir nicht existiert.
	 * @throws IllegalArgumentException Falls parentDir kein Verzeichnis
	 *             darstellt.
	 */
    public IconHashMap(File parentDir)
    throws NullPointerException, FileNotFoundException,
    IllegalArgumentException {
    	this.parentDir = parentDir;
    	if (! parentDir.exists()) {
    		throw new FileNotFoundException("Icon-Verzeichnis '"+
    			parentDir.getAbsolutePath()+"' nicht gefunden");
    	}
    	if (! parentDir.isDirectory()) {
    		throw new IllegalArgumentException(
    				"Icon-Pfad '"+parentDir+"' ist kein Verzeichnis");
    	}
    	for (File f : parentDir.listFiles()) {
    		// Dateiname ohne Pfad und ohne Extension
    		String key = f.getName().replaceAll("\\..*$", "");
    		put(key, new ImageIcon(f.getPath()));
    	}
    }

    /**
	 * Liefert ein aus einer Datei geladenes Icon.
	 *
	 * @param key Der Dateiname (ohne Extension) des zurückzuliefernden Icons.
	 *            Für ein Icon "fruehstueck/Marmelade.gif" wäre das "Marmelade".
	 *            Volles Verwendungsbeispiel siehe {@link IconHashMap}.
	 * @throws RuntimeException Falls das gewünschte Icon im Dateisystem nicht
	 *             existiert (genauer: falls es zum Zeitpunkt des
	 *             Konstruktor-Aufrufs nicht existiert hat). Die Exception hat
	 *             als <em>cause</em> eine FileNotFoundException (die nicht
	 *             direkt geworfen werden kann, da die geerbte Methodensignatur
	 *             keine <em>checked exceptions</em> zulässt).
	 */
	@Override
    public ImageIcon get(Object key) throws RuntimeException {
		ImageIcon rv = super.get(key);
		if (rv == null) {
			// In RuntimeException eingewickelt, um mit der geerbten
			// Methodensignatur (keine throws-Dekl) kompatibel zu sein
			throw new RuntimeException(
					new FileNotFoundException(String.format(
					"Icon-Datei '%s%s%s' nicht gefunden.",
					parentDir.getPath(), File.separator, key)));
		}
		return rv;
    }
}