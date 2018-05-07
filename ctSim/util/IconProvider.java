/*
 * c't-Sim - Robotersimulator für den c't-Bot
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
 * Der c't-Sim verwendet die Klasse {@link FileIconMap}; das Applet lädt seine Icons aus einer Jar-Datei,
 * da es ja nicht aufs Dateisystem zugreifen darf. Die Config und die Verwender der Config müssen davon
 * aber nichts wissen.
 * </p>
 */
public interface IconProvider {
	/**
	 * Liefert ein aus einer Datei geladenes Icon.
	 *
	 * @param key	Der Dateiname (ohne Extension) des zurückzuliefernden Icons. Für ein Icon
	 * 				"fruehstück/Marmelade.gif" wäre das "Marmelade". Volles Verwendungsbeispiel siehe
	 * 				unter {@link FileIconMap}.
	 * @return Icon
	 */
	public Icon get(String key);
}