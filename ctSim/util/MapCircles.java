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
 * Datentyp für Kreise, die der MapViewer darstellen kann
 * 
 * @author Timo Sandmann
 */
public class MapCircles {
	/** X-Koordinate Mittelpunkt */
	public int x;
	/** Y-Koordinate Mittelpunkt */
	public int y;
	/** Radius */
	public int radius;
	/** Farbe der Linie */
	public int color;
	
	/**
	 * Kreis, wie er vom MapViewer dargestellt werden kann
	 * 
	 * @param x			X-Koordinate Mittelpunkt
	 * @param y			Y-Koordinate Mittelpunkt
	 * @param radius	Radius
	 * @param color		Farbe
	 */
	public MapCircles(int x, int y, int radius, int color) {
		this.x = x;
		this.y = y;
		this.radius = radius;
		this.color = color;
	}
}