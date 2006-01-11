package ctSim;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;
import java.lang.Math;

/**
 * Diese Klasse enthaelt reine Hilfsfunktionen wie Umrechnungsmethoden zwischen
 * verschiedenen Mass-Einheiten etc.
 * 
 * @author pek (pek@ctmagazin.de)
 */
public class SimUtils {

	/**
	 * Errechnet aus einem javax.vecmath.Vector3f eine ganzzahlig gerundete
	 * Gradangabe. Diese Methode wird zur Anzeige der Bot-Blickrichtung im
	 * ControlPanel benutzt.
	 * 
	 * @param vec
	 *            der Eingabevektor
	 * @return die Gradzahl als String
	 */
	public static String vec3fToString(Vector3f vec) {
		return new Integer(vec3fToInt(vec)).toString();
	}

	/**
	 * Errechnet aus einem javax.vecmath.Vector3f eine ganzzahlig gerundete
	 * Gradangabe. Diese Methode wird zum Setzen der Anzeige der
	 * Bot-Blickrichtung im ControlPanel benutzt.
	 * 
	 * @param vec
	 *            der Eingabevektor
	 * @return die (gerundete) Gradzahl als int-Wert
	 */
	public static int vec3fToInt(Vector3f vec) {
		Vector3f north = new Vector3f(0f, 1f, 0f);
		double rad = vec.angle(north);
		int deg = Math.round(Math.round(Math.toDegrees(rad)));
		return deg;
	}

	/**
	 * Erzeugt aus einer Grad-Angabe einen normalisierten
	 * javax.vecmath.Vector3f. Diese Methode wird zum Setzen der Anzeige der
	 * Bot-Blickrichtung im ControlPanel benutzt.
	 * 
	 * @param deg
	 *            der Winkel in Grad
	 * @return der neue Blickvektor
	 */
	public static Vector3f intToVec3f(int deg) {
		double rad = Math.toRadians(deg);
		// Sinus und Cosinus sind hier vertauscht, weil
		// 0° in Richtung der positiven Y-Achse zeigen soll!
		double x = Math.sin(rad);
		double y = Math.cos(rad);
		Vector3f newHead = new Vector3f(new Point3d(x, y, 0d));
		return newHead;
	}
}
