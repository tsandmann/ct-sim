package ctSim.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//$$ doc ganze Klasse
// http://java.sun.com/j2se/1.5.0/docs/guide/xml/jaxp/JAXP-Compatibility_150.html
public class XmlDocument {
	private Document document;
	private XPath evaluator;

	public XmlDocument(File fileToParse)
	throws SAXException, IOException, ParserConfigurationException {
		this(fileToParse, fileToParse.getParent() + "/");
	}

	public XmlDocument(File fileToParse, String baseDir)
	throws SAXException, IOException, ParserConfigurationException {
		this(new FileInputStream(fileToParse), baseDir);
	}

	public XmlDocument(InputStream documentStream, String baseDir)
	throws SAXException, IOException, ParserConfigurationException {
		// Irrsinnige Buerokratie ...
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setValidating(true); // Soll gegen DTD validieren
        document = f.newDocumentBuilder().parse(documentStream, baseDir);
        evaluator = XPathFactory.newInstance().newXPath();
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

	//TODO braucht Test
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
