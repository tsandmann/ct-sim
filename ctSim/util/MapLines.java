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

/**
 * Datentyp für Linien, die der MapViewer darstellen kann
 *
 * @author Timo Sandmann
 */
public class MapLines {
	/** X-Koordinate Startpunkt */
	public int x1;
	/** Y-Koordinate Startpunkt */
	public int y1;
	/** X-Koordinate Endpunkt */
	public int x2;
	/** Y-Koordinate Endpunkt */
	public int y2;
	/** Farbe der Linie */
	public int color;

	/**
	 * Linie, wie sie vom MapViewer dargestellt werden kann
	 *
	 * @param x1	X-Koordinate Startpunkt
	 * @param y1	Y-Koordinate Startpunkt
	 * @param x2	X-Koordinate Endpunkt
	 * @param y2	Y-Koordinate Endpunkt
	 * @param color	Farbe
	 */
	public MapLines(int x1, int y1, int x2, int y2, int color) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		this.color = color;
	}
}
