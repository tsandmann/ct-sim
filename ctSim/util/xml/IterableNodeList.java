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

package ctSim.util.xml;

import java.util.Iterator;

import org.w3c.dom.NodeList;

/**
 * <p>
 * Reguläre {@link NodeList} wie aus der Java-Plattform bekannt, die aber auch {@link Iterable} ist.
 * Das bedeutet, im Gegensatz zu Instanzen von <code>NodeList</code> können Instanzen dieser Klasse auch
 * folgendermaßen verwendet werden:
 * <ul>
 * <li><code>IterableNodeList list = ...</code></li>
 * <li><code>for (Node node : list) { ... }</code></li>
 * </ul>
 * In der Regel wird man diese Klasse nicht direkt verwenden, sondern von Methodenaufrufen zurückgeliefert
 * bekommen.
 * </p>
 * <p>
 * Implementierung: Diese Klasse ist ein ottonormaler Wrapper.
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class IterableNodeList implements NodeList, Iterable<QueryableNode> {
	/** Node-Liste */
	private NodeList wrappee;

	/**
	 * @param wrappee	Inhalt
	 */
	public IterableNodeList(NodeList wrappee) {
		this.wrappee = wrappee;
	}

	/**
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<QueryableNode> iterator() {
		return new Iterator<>() {
			private int lastIdx = -1;

			@Override
			public boolean hasNext() {
				return lastIdx + 1 < wrappee.getLength();
			}

			@Override
			public QueryableNode next() {
				lastIdx++;
				return IterableNodeList.this.item(lastIdx);
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * @see org.w3c.dom.NodeList#getLength()
	 */
	@Override
	public int getLength() {
		return wrappee.getLength();
	}

	/**
	 * @see org.w3c.dom.NodeList#item(int)
	 */
	@Override
	public QueryableNode item(int index) {
		return XmlDocument.createQueryableNode(wrappee.item(index));
	}
}