package ctSim.util.xml;

import java.util.Iterator;

import org.w3c.dom.NodeList;

/**
 * <p>
 * Regul&auml;re {@link NodeList} wie aus der Java-Plattform bekannt, die aber
 * auch {@link Iterable} ist. Das bedeutet, im Gegensatz zu Instanzen von
 * <code>NodeList</code> k&ouml;nnen Instanzen dieser Klasse auch
 * folgenderma&szlig;en verwendet werden:
 * <ul>
 * <li><code>IterableNodeList list = ...</code></li>
 * <li><code>for (Node node : list) { ... }</code></li>
 * </ul>
 * In der Regel wird man diese Klasse nicht direkt verwenden, sondern von
 * Methodenaufrufen zur&uuml;ckgeliefert bekommen.
 * </p>
 * <p>
 * Implementierung: Diese Klasse ist ein ottonormaler Wrapper.
 * </p>
 * 
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class IterableNodeList implements NodeList, Iterable<QueryableNode> {
	/** Node-Liste */
	private NodeList wrappee;

	/**
	 * @param wrappee Inhalt
	 */
	public IterableNodeList(NodeList wrappee) {
		this.wrappee = wrappee;
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<QueryableNode> iterator() {
		return new Iterator<QueryableNode>() {
			private int lastIdx = -1;

			@SuppressWarnings("synthetic-access")
			public boolean hasNext() {
				return lastIdx + 1 < wrappee.getLength();
			}

			public QueryableNode next() {
				lastIdx++;
				return IterableNodeList.this.item(lastIdx);
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * @see org.w3c.dom.NodeList#getLength()
	 */
	public int getLength() {
		return wrappee.getLength();
	}

	/**
	 * @see org.w3c.dom.NodeList#item(int)
	 */
	public QueryableNode item(int index) {
		return XmlDocument.createQueryableNode(wrappee.item(index));
	}
}
