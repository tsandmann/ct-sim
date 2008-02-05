package ctSim.util;

import javax.swing.Icon;

/**
 * <p>
 * Ding, das Icons bereitstellt.
 * </p>
 * <p>
 *
 * <pre>
 * .--------.  verwendet einen   .--------------.
 * | Config | -----------------> | IconProvider |
 * '--------'                    '--------------'
 *                                 ^          ^
 *                                 |          |
 *                   implementiert |          | implementiert
 *                                 |          |
 *                  .-------------------. .---------------------.
 *                  | FileIconMap       | | Ding, das Icons     |
 *                  | Liest Icons aus   | | aus einer Jar-Datei |
 *                  | einem Verzeichnis | | anfordert           |
 *                  '-------------------' '---------------------'
 * </pre>
 *
 * Der c't-Sim verwendet die Klasse {@link FileIconMap}; das Applet l&auml;dt
 * seine Icons aus einer Jar-Datei, da es ja nicht aufs Dateisystem zugreifen
 * darf. Die Config und die Verwender der Config m&uuml;ssen davon aber nichts
 * wissen.
 * </p>
 */
public interface IconProvider {
	/**
	 * Liefert ein aus einer Datei geladenes Icon.
	 *
	 * @param key Der Dateiname (ohne Extension) des zur&uuml;ckzuliefernden
	 * Icons. F&uuml;r ein Icon "fruehstueck/Marmelade.gif" w&auml;re das
	 * "Marmelade". Volles Verwendungsbeispiel siehe {@link FileIconMap}.
	 * @return Icon
	 */
	public Icon get(String key);
}