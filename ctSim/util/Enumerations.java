package ctSim.util;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Utility-Klasse im Stil von {@link Arrays}, die Hilfen f&uuml;r die Arbeit
 * mit {@link Enumeration}-Objekten enth&auml;lt.
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class Enumerations {
	/**
	 * <p>
	 * Erm&ouml;glicht, for-each-Schleifen mit Enumeration-Objekten zu
	 * f&uuml;ttern: Liefert ein {@link Iterable}, das die Elemente der
	 * &uuml;bergebenen Enumeration enth&auml;lt. N&uuml;tzlich wenn eine
	 * veraltete Methode eine Enumeration zur&uuml;ckliefert, man aber gern eine
	 * for-each-Konstruktion verwenden w&uuml;rde: <br/> <code>
	 * Enumeration&lt;String> e = antikesObjekt.getEnumeration();<br />
	 * for (String s : Enumerations.asIterable(e)) {<br />
	 * &nbsp;&nbsp;&nbsp;&nbsp;...<br />
	 * }
	 * </code>
	 * </p>
	 * <p>
	 * <strong>Achtung:</strong> Die Implementierung geht davon aus, dass nach
	 * dem Methodenaufruf &lt;strong&gt;nur noch&lt;/strong&gt; das Iterable
	 * verwendet wird, nicht mehr die &uuml;bergebene Enumeration. Falls nach
	 * dem Aufruf dieser Methode sowohl das Iterable als auch die Enumeration
	 * benutzt werden, insbesondere die Methode {@code next()} bzw.
	 * {@code nextElement()} verwendet wird, kommt es zu Chaos, Aufregung,
	 * Depression, Heulen und Z&auml;hneklappern.
	 * </p>
	 * <p>
	 * Die optionale Methode {@link Iterator#remove()} schl&auml;gt in der
	 * vorliegenden Implementierung mit einer
	 * {@link UnsupportedOperationException} fehl.
	 * </p>
	 *
	 * @param source Enumeration, die dem zur&uuml;ckgelieferten Iterable
	 * zugrunde liegt.
	 * @param <T> Elementtyp der &uuml;bergebenen Enumeration und damit
	 * auch des zur&uuml;ckgelieferten Iterable.
	 */
	public static <T> Iterable<T> asIterable(final Enumeration<T> source) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
	            return new Iterator<T>() {
					public boolean hasNext() {
	                    return source.hasMoreElements();
                    }

					public T next() {
	                    return source.nextElement();
                    }

					public void remove() {
						throw new UnsupportedOperationException();
                    }
	            };
            }
		};
	}
}
