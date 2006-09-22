package ctSim.util;

import java.util.Enumeration;
import java.util.Iterator;

//$$ doc Enumerations
public class Enumerations {
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
