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

//$$ doc Misc
public class Misc {
	public static double log2(double x) {
		return Math.log(x) / Math.log(2);
	}

	/**
	 * Bringt einen Winkel in das Intervall ]&minus;180°; 180°].
	 * 
	 * @param angleInDeg Winkel in Grad
	 */
	public static double normalizeAngleDeg(double angleInDeg) {
		while (angleInDeg > 180)
			angleInDeg -= 360;
		while (angleInDeg <= -180)
			angleInDeg += 360;
		return angleInDeg;
	}

	/**
	 * Bringt einen Winkel in das Intervall ]&minus;&pi;; &pi;].
	 * 
	 * @param angleInRad Winkel im Bogenma&szlig;
	 */
	public static double normalizeAngleRad(double angleInRad) {
		while (angleInRad > PI)
			angleInRad -= 2 * PI;
		while (angleInRad <= -PI)
			angleInRad += 2 * PI;
		return angleInRad;
	}

	/**
	 * Liefert den &uuml;bergebenen Wert <code>value</code> zurück, falls 0
	 * &lt; <code>value</code> &lt; <code>maxAllowed</code> gilt. Falls
	 * nicht, liefert den Wert aus dem Intervall [0; <code>maxAllowed</code>],
	 * der <code>value</code> am n&auml;chsten liegt.
	 */
	public static int clamp(int value, int maxAllowed) {
		int rv = value;
		rv = Math.max(rv, 0);
		rv = Math.min(rv, maxAllowed);
		return rv;
	}

	/** Wie {@link #clamp(int, int)} */
	public static double clamp(double value, double maxAllowed) {
		double rv = value;
		rv = Math.max(rv, 0);
		rv = Math.min(rv, maxAllowed);
		return rv;
	}

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

	// throws NullPtr wenn separator == null
	@SuppressWarnings("unchecked")
    public static <T> T[] intersperse(T separator, T... stuff) {
		T[] rv = (T[])Array.newInstance(separator.getClass(),
			stuff.length * 2 - 1);
		for (int i = 0; i < stuff.length; i++) {
			rv[i * 2] = stuff[i];
			if (i + 1 < stuff.length)
				rv[i * 2 + 1] = separator;
		}
		return rv;
	}

	public static String join(String... strings) {
		StringBuilder b = new StringBuilder();
		for (String s : strings)
			b.append(s);
		return b.toString();
	}

	public static void copyStreamToStream(InputStream source,
		OutputStream dest) throws IOException {
		byte[] buf = new byte[4096];
		int len;
		while ((len = source.read(buf)) > 0)
            dest.write(buf, 0, len);
		dest.close();
	}

	private static HashMap<RenderingHints.Key, Object> antiAliasOn = newMap();

	static {
		antiAliasOn.put(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON);
	}

	public static void enableAntiAliasing(Graphics2D g) {
		g.addRenderingHints(antiAliasOn);
	}

	/**
	 * Konvertiert ein Byte (Wertebereich [&minus;128; +127]) in ein
	 * <em>unsigned byte</em> (Wertebereich [0; 255]).
	 */
	public static int toUnsignedInt8(byte value) {
		// Zweierkomplement (1. Bit gesetzt = negatives Vorzeichen)
		return (value & 0x7F) + (value < 0 ? 128 : 0);
	}

	/**
	 * Liefert das letzte Element des &uuml;bergebenen Arrays.
	 *
	 * @throws ArrayIndexOutOfBoundsException falls das &uuml;bergebene
	 * Array leer ist (L&auml;nge 0 hat)
	 * @throws NullPointerException falls das &uuml;bergebene Array
	 * {@code null} ist.
	 */
	public static <T> T lastOf(T[] array) {
		return array[array.length - 1];
	}

	/**
	 * <p>
	 * Erzeugt eine {@link ArrayList} mit den korrekten Typparametern.
	 * Verwendung:<br />
	 * {@code List&lt;EchtLangerAnstrengenderTyp&gt; listInstance = newList();}<br />
	 * Das ist sch&ouml;ner und leichter &auml;nderbar als:<br />
	 * {@code List&lt;EchtLangerAnstrengenderTyp&gt; listInstance = new ArrayList&lt;EchtLangerAnstrengenderTyp&gt;();}
	 * </p>
	 * <p>
	 * Nach der hervorragenden Entdeckung von Josh Bloch; siehe <a
	 * href="http://developers.sun.com/learning/javaoneonline/2006//coreplatform/TS-1512.html">Vortrag
	 * bei der JavaOne</a>.
	 */
	public static <T> ArrayList<T> newList() {
		return new ArrayList<T>();
	}

	/** Wie {@link #newList()}, aber für eine {@link HashMap}. */
	public static <K, V> HashMap<K, V> newMap() {
		return new HashMap<K, V>();
	}

	//$$ doc
	public static void setCaretPolicy(JTextArea t, int updatePolicy) {
		DefaultCaret c = new DefaultCaret();
		c.setUpdatePolicy(updatePolicy);
		t.setCaret(c);
	}
}
