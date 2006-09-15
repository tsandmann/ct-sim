package ctSim.util;

import java.io.File;
import java.util.Iterator;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

//$$ doc ganze Klasse
public class XPathApiBearableMaker {
	private InputSource document;
	private XPath evaluator;
	
	public XPathApiBearableMaker(File fileToParse) {
		this(new InputSource(fileToParse.toURI().toString()));
	}

	public XPathApiBearableMaker(InputSource document) {
		evaluator = XPathFactory.newInstance().newXPath();
        this.document = document;
    }
	
	public IterableNodeList getNodeList(String xPathExpression) 
	throws XPathExpressionException {
		return new IterableNodeList((NodeList)
				evaluator.evaluate(xPathExpression, document, 
				XPathConstants.NODESET));
	}

	public Node getNode(String xPathExpression) 
	throws XPathExpressionException {
		return (Node)evaluator.evaluate(xPathExpression, document, 
				XPathConstants.NODE);
	}
	
	
	public static class IterableNodeList 
	implements NodeList, Iterable<Node> {
		private NodeList carrier;
		
		public IterableNodeList(NodeList nl) {
			carrier = nl;
		}
		
		public Iterator<Node> iterator() {
	        return new Iterator<Node>() {
	        	private int lastIdx = -1;
	        	
				@SuppressWarnings("synthetic-access")
                public boolean hasNext() {
	                return lastIdx + 1 < carrier.getLength();
                }

				@SuppressWarnings("synthetic-access")
                public Node next() {
					lastIdx++;
	                return carrier.item(lastIdx);
                }

				public void remove() {
	                throw new UnsupportedOperationException();
                }};
        }

		public int getLength() {
	        return carrier.getLength();
        }

		public Node item(int index) {
	        return carrier.item(index);
        }
	}
}
