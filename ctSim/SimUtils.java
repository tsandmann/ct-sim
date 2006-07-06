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

package ctSim;

import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import java.lang.Math;

/**
 * Diese Klasse enthaelt reine Hilfsfunktionen wie Umrechnungsmethoden zwischen
 * verschiedenen Mass-Einheiten usw.
 * 
 * @author pek (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */
public class SimUtils {

	/**
	 * Errechnet aus einem javax.vecmath.Vector3d eine ganzzahlig gerundete
	 * Gradangabe und gibt sie als String zur�ck. Diese Methode wird zur Anzeige
	 * der Bot-Blickrichtung im ControlPanel benutzt.
	 * 
	 * @param vec
	 *            der Eingabevektor
	 * @return die Gradzahl als String
	 */
	public static String vec3dToString(Vector3d vec) {
		return new Integer(Math.round(Math.round(vec3dToDouble(vec))))
				.toString();
	}

	/**
	 * Errechnet aus einem javax.vecmath.Vector3d eine Gradangabe. Diese Methode
	 * wird zur Anzeige der Bot-Blickrichtung im ControlPanel benutzt.
	 * 
	 * @param vec
	 *            der Eingabevektor
	 * @return die (gerundete) Gradzahl als int-Wert
	 */
	public static double vec3dToDouble(Vector3d vec) {
		Vector3d north = new Vector3d(0d, 1d, 0d);
		double rad = vec.angle(north);
		double deg = Math.toDegrees(rad);
		// Da Vector3f.angle() nur Werte zwischen 0 und PI liefert,
		// muessen hier zwei Faelle unterschieden werden:
		if (vec.x >= 0)
			return deg;
		else
			return -deg;
	}

	/**
	 * Erzeugt aus einer Grad-Angabe einen normalisierten
	 * javax.vecmath.Vector3d. Diese Methode wird zum Setzen der
	 * Bot-Blickrichtung ueber das ControlPanel benutzt.
	 * 
	 * @param deg
	 *            der Winkel in Grad
	 * @return der neue Blickvektor
	 */
	public static Vector3d intToVec3d(int deg) {
		double rad = Math.toRadians(deg);
		// Sinus und Cosinus sind hier vertauscht, weil
		// 0� in Richtung der positiven Y-Achse zeigen soll!
		double x = Math.sin(rad);
		double y = Math.cos(rad);
		Vector3d newHead = new Vector3d(new Point3d(x, y, 0d));
		return newHead;
	}

	/**
	 * Errechnet den Winkel zwischen Nordrichtung des Universums (Richtung der 
	 * positiven Y-Achse) und der angegeben Blickrichtung.
	 * 
	 * @param heading 
	 * 			  Gib die Blickrichtung an, zu welcher der Winkel berechnet werden soll. 				 
	 * @return Gibt den Winkel in Bogenmass (radians, Rad) zur�ck
	 */
//	public static double getRotation(Vector3f heading) {
//		double angle = heading.angle(new Vector3f(0f, 1f, 0f));
//		// Da Vector3f.angle() nur Werte zwischen 0 und PI liefert,
//		// muessen hier zwei Faelle unterschieden werden:
//		if (heading.x >= 0)
//			return -angle;
//		else
//			return angle;
//	}
	// TODO: Allgemeiner!? Ref-Vector auslagern...
	public static double getRotation(Vector3d heading) {
		
		double angle = heading.angle(new Vector3d(0d, 1d, 0d));
		
		if(heading.y < 0)
			angle = -angle;
		
		return angle;
	}
	
	// TODO: Allgemeiner!?
	public static Transform3D getTransform(Point3d pos, Vector3d head) {
		
		Transform3D transform = new Transform3D();
		
		transform.setTranslation(new Vector3d(pos));
		
		double angle = getRotation(head);
		
		transform.setRotation(new AxisAngle4d(0d, 0d, 1d, angle));
		
		return transform;
	}
}