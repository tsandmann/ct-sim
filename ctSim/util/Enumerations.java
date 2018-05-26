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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Utility-Klasse im Stil von {@link Arrays}, die Hilfen für die Arbeit mit {@link Enumeration}-Objekten
 * enthält.
 *
 * @author Hendrik Krauß
 */
public class Enumerations {
	/**
	 * <p>
	 * Ermöglicht, for-each-Schleifen mit Enumeration-Objekten zu füttern: Liefert ein {@link Iterable},
	 * das die Elemente der übergebenen Enumeration enthält. Nützlich wenn eine veraltete Methode eine
	 * Enumeration zurückliefert, man aber gern eine for-each-Konstruktion verwenden würde:<br>
	 * <code>
	 * Enumeration<String> e = antikesObjekt.getEnumeration();<br>
	 * for (String s : Enumerations.asIterable(e)) {<br>
	 * 		...<br>
	 * }
	 * </code>
	 * </p>
	 * <p>
	 * <strong>Achtung:</strong> Die Implementierung geht davon aus, dass nach dem Methodenaufruf <strong>
	 * nur noch </strong> das Iterable verwendet wird, nicht mehr die übergebene Enumeration. Falls nach
	 * dem Aufruf dieser Methode sowohl das Iterable als auch die Enumeration benutzt werden, insbesondere
	 * die Methode {@code next()} bzw. {@code nextElement()} verwendet wird, kommt es zu Chaos und Problemen.
	 * </p>
	 * <p>
	 * Die optionale Methode {@link Iterator#remove()} schlägt in der vorliegenden Implementierung mit einer
	 * {@link UnsupportedOperationException} fehl.
	 * </p>
	 *
	 * @param source	Enumeration, die dem zurückgelieferten Iterable zugrunde liegt.
	 * @param <T>		Element-Typ der übergebenen Enumeration und damit auch des zurückgelieferten Iterable.
	 * @return T
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
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
                    }
	            };
            }
		};
	}
}
