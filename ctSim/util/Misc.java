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

import static java.lang.Math.PI;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

/** Tools */
public class Misc {
	/**
	 * @param x
	 * @return log2(x)
	 */
	public static double log2(double x) {
		return Math.log(x) / Math.log(2);
	}

	/**
	 * Bringt einen Winkel in das Intervall ]-180°; 180°].
	 *
	 * @param angleInDeg	Winkel in Grad
	 * @return Winkel
	 */
	public static double normalizeAngleDeg(double angleInDeg) {
		while (angleInDeg > 180)
			angleInDeg -= 360;
		while (angleInDeg <= -180)
			angleInDeg += 360;
		return angleInDeg;
	}

	/**
	 * Bringt einen Winkel in das Intervall ]-π; π].
	 *
	 * @param angleInRad	Winkel im Bogenmaß
	 * @return Winkel
	 */
	public static double normalizeAngleRad(double angleInRad) {
		while (angleInRad > PI)
			angleInRad -= 2 * PI;
		while (angleInRad <= -PI)
			angleInRad += 2 * PI;
		return angleInRad;
	}

	/**
	 * Liefert den übergebenen Wert <code>value</code> zurück, falls
	 * 0 &lt; <code>value</code> &lt; <code>maxAllowed</code>
	 * gilt. Falls nicht, liefert den Wert aus dem Intervall [0; <code>maxAllowed</code>],
	 * der <code>value</code> am Nächsten liegt.
	 *
	 * @param value
	 * @param maxAllowed
	 * @return Wert
	 */
	public static int clamp(int value, int maxAllowed) {
		int rv = value;
		rv = Math.max(rv, 0);
		rv = Math.min(rv, maxAllowed);
		return rv;
	}

	/**
	 * Wie {@link #clamp(int, int)}
	 *
	 * @param value
	 * @param maxAllowed
	 * @return Wert
	 */
	public static double clamp(double value, double maxAllowed) {
		double rv = value;
		rv = Math.max(rv, 0);
		rv = Math.min(rv, maxAllowed);
		return rv;
	}

	/**
	 * @param fullString
	 * @param whitelistOfBeginnings
	 * @return true/falls
	 */
	public static boolean startsWith(
			String fullString, String... whitelistOfBeginnings) {
		if (whitelistOfBeginnings == null)
			return false;
		for (String shorter : whitelistOfBeginnings) {
			if (shorter == null)
				continue;
			if (fullString.length() < shorter.length())
				continue;
			if (fullString.substring(0, shorter.length()).equals(shorter))
				return true;
		}
		return false;
	}

	// @throws NullPtr	wenn separator == null
	/**
	 * @param <T>
	 * @param separator
	 * @param stuff
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] intersperse(T separator, T... stuff) {
		T[] rv = (T[])Array.newInstance(separator.getClass(), stuff.length * 2 - 1);
		for (int i = 0; i < stuff.length; i++) {
			rv[i * 2] = stuff[i];
			if (i + 1 < stuff.length)
				rv[i * 2 + 1] = separator;
		}
		return rv;
	}

	/**
	 * @param strings
	 * @return neuer String
	 */
	public static String join(String... strings) {
		StringBuilder b = new StringBuilder();
		for (String s : strings)
			b.append(s);
		return b.toString();
	}

	/**
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	public static void copyStreamToStream(InputStream source, OutputStream dest) throws IOException {
		byte[] buf = new byte[4096];
		int len;
		while ((len = source.read(buf)) > 0)
			dest.write(buf, 0, len);
		dest.close();
	}

	/**
	 * Raw Type HashMap antiAliasOn
	 */
	private static HashMap<RenderingHints.Key, Object> antiAliasOn = newMap();

	static {
		antiAliasOn.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}

	/**
	 * @param g	Graphics2D
	 */
	public static void enableAntiAliasing(Graphics2D g) {
		g.addRenderingHints(antiAliasOn);
	}

	/**
	 * Konvertiert ein Byte (Wertebereich [-128; +127]) in ein <em>unsigned byte</em> (Wertebereich [0; 255]).
	 *
	 * @param value
	 * @return uByte
	 */
	public static int toUnsignedInt8(byte value) {
		// Zweierkomplement (1. Bit gesetzt = negatives Vorzeichen)
		return (value & 0x7F) + (value < 0 ? 128 : 0);
	}

	/**
	 * Liefert das letzte Element des übergebenen Arrays
	 *
	 * @param array
	 * @param <T>
	 * @return letztes Element
	 * @throws ArrayIndexOutOfBoundsException	falls das übergebene Array leer ist (Länge 0 hat)
	 * @throws NullPointerException				falls das übergebene Array {@code null} ist
	 */
	public static <T> T lastOf(T[] array) {
		return array[array.length - 1];
	}

	/**
	 * <p>
	 * Erzeugt eine {@link ArrayList} mit den korrekten Typparametern. Verwendung:<br>
	 * {@code List<EchtLangerAnstrengenderTyp> listInstance = newList();}<br>
	 * Das ist schöner und leichter änderbar als:<br>
	 * {@code List<EchtLangerAnstrengenderTyp> listInstance = new ArrayList<EchtLangerAnstrengenderTyp>();}
	 * </p>
	 * <p>
	 * Implementierung nach: Bloch, Joshua (2018)³: "Effective Java", Boston, Addison-Wesley
	 *
	 * @param <T>
	 * @return ArrayList
	 */
	public static <T> ArrayList<T> newList() {
		return new ArrayList<T>();
	}

	/**
	 * Wie {@link #newList()}, aber für eine {@link HashMap}
	 *
	 * @param <K>
	 * @param <V>
	 * @return HashMap
	 */
	public static <K, V> HashMap<K, V> newMap() {
		return new HashMap<K, V>();
	}

	/**
	 * @param t
	 * @param updatePolicy
	 */
	public static void setCaretPolicy(JTextArea t, int updatePolicy) {
		DefaultCaret c = new DefaultCaret();
		c.setUpdatePolicy(updatePolicy);
		t.setCaret(c);
	}
}
