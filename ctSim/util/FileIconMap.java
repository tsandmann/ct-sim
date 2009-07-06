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

package ctSim.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

import javax.swing.Icon;
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
 * <li><code>FileIconMap icons = new
 * FileIconMap(new File("pfad/zu/den/icons"));</code></li>
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
 * werden: <code>FileIconMap ganzeChose = new
 * FileIconMap(new File("IconUnterverzeichnis"));</code></li>
 * <li>Beim Erweitern der Applikation k&ouml;nnen Icons hinzugef&uuml;gt
 * werden, indem nur die Icon-Datei ins entsprechende Verzeichnis gelegt wird.
 * Code-&Auml;nderungen zum Laden sind nicht n&ouml;tig.</li>
 * </ul>
 * </p>
 *
 * @see ImageIcon
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class FileIconMap implements IconProvider {
    /** UID */
	private static final long serialVersionUID = 7916128473992195865L;

    /** Logger */
    final FmtLogger lg = FmtLogger.getLogger("ctSim.util.FileIconMap");

    /**
	 * Aus dem Verzeichnis, das dem Konstruktor &uuml;bergeben wurde, werden
	 * alle Icons geladen und hier zwischengespeichert
	 */
    private final HashMap<String, ImageIcon> map;

    /** Um sp&auml;ter in Fehlermeldungen den Pfad angeben zu k&ouml;nnen */
    private final File parentDir;

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
     * enth&auml;lt.
     * @throws NullPointerException Falls parentDir <code>null</code> ist.
     * @throws FileNotFoundException Falls parentDir nicht existiert.
     * @throws IllegalArgumentException Falls parentDir kein Verzeichnis
     * darstellt.
     */
    public FileIconMap(File parentDir)
    throws NullPointerException, FileNotFoundException,
    IllegalArgumentException {
        this.parentDir = parentDir;
        map = Misc.newMap();
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
            map.put(key, new ImageIcon(f.getPath()));
        }
    }

    /**
	 * &Auml;hnlich wie {@link IconProvider#get(String)}. Liefert {@code null},
	 * falls das gew&uuml;nschte Icon im Dateisystem nicht existiert (genauer:
	 * falls es nicht existiert hat zu dem Zeitpunkt, als der Konstruktor
	 * aufgerufen wurde).
	 */
    public Icon get(String key) {
        ImageIcon rv = map.get(key);
        if (rv == null) {
        	lg.warn("Icon-Datei '%s%s%s.*' nicht gefunden",
        		parentDir.getPath(), File.separator, key);
        }
        return rv;
    }
}
