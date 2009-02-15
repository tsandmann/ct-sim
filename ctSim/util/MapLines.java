/**
 * 
 */
package ctSim.util;

/**
 * Datentyp fuer Linien, die der MapViewer darstellen kann
 * @author Timo Sandmann (mail@timosandmann.de)
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
