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

package ctSim.util.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ctSim.util.Decoratoror;

/**
 * <p>
 * Klasse, die die Arbeit mit XPath vereinfachen und verertr&auml;glichen
 * soll.
 * </p>
 * <h3>XPath?</h3>
 * <p>
 * Eine Metapher f&uuml;r <a href="http://en.wikipedia.org/wiki/Xpath">XPath</a>
 * sind regul&auml;re Ausdr&uuml;cke: So, wie regul&auml;re Ausdr&uuml;cke
 * Teilstrings in einem langen String fangen, f&auml;ngt XPath Teile aus einem
 * XML-Dokument. XPath l&auml;sst sich rubbeldiekatz lernen durch die <a
 * href="http://www.w3.org/TR/xpath.html#path-abbrev">Anwendungsbeispiele in
 * der Spezifikation</a> (&quot;context node&quot; hei&szlig;t in aller Regel
 * Root-Knoten des XML-Dokuments.)
 * </p>
 * <h3>Verwendungsbeispiel</h3>
 * <p>
 * <ul>
 * <li><code>XmlDocument d = new <a href="#XmlDocument(java.lang.String)">XmlDocument</a>("pfad/zu/ner/datei.xml");</code></li>
 * <li><code>Node r = d.<a href="#getNode(java.lang.String)">getNode</a>("//ruebe[12]"); // Einzelknoten: zw&ouml;lfte R&uuml;be</code></li>
 * <li><code>for (Node node : d.<a href="#getNodeList(java.lang.String)">getNodeList</a>("//gurke")) { ... } // Liste aller Gurken im XML</code></li>
 * </ul>
 * <code>getNodeList()</code> liefert dabei eine
 * {@link org.w3c.dom.NodeList} gem&auml;&szlig; der Java-Plattform-API, die
 * aber als Verbesserung auch {@link Iterable} implementiert. Daher ist sie in
 * der gezeigten Weise in for-each-Schleifen verwendbar, im Gegensatz zur
 * normalen NodeList.
 * </p>
 * <h3>Spezifikation</h3>
 * <p>
 * Diese Klasse greift zur&uuml;ck auf die XPath-API aus der Java-Plattform ({@link javax.xml.xpath}).
 * Es ist der Dokumentation nicht zu entnehmen, welche XPath-Version die
 * Plattform-API implementiert, aber ich glaube es ist 1.0 (das wird zumindest
 * bei {@link XPath} angedeutet). &rarr; <a
 * href="http://www.w3.org/TR/xpath.html">XPath-1.0-Spezifikation</a>
 * </p>
 * <h3>Motivation</h3>
 * <p>
 * Die Handhabung von XPath in der normalen Java-Plattform ist leider sehr
 * javaig (d.h. umst&auml;ndlich und b&uuml;rokratisch). Eine Routineaufgabe
 * wie "bitte mal <code>//tomate</code> auf gemuese.xml anwenden" erfordert,
 * wenn ich das richtig sehe, mindestens folgende Monstrosit&auml;t:
 * <code>XPathFactory.newInstance().newXPath().evaluate(DocumentBuilderFactory.newDocumentBuilder().parse("gemuese.xml"), "//tomate");</code>
 * &ndash; und ist dann noch nicht mal typsicher. (Bei der verwendeten Methode
 * handelt es sich &uuml;brigens um eine "convenience&quot;-Methode, d.h. das
 * ist noch der einfachere Fall. Was auch nicht hilft ist, dass man sich den
 * o.g. Aufruf m&uuml;hsam zusammenst&uuml;ckeln muss, da die Dokumentation
 * leider un&uuml;bersichtlich geraten ist und sich st&auml;ndig verzettelt.)
 * </p>
 * <p>
 * Diese Klasse vereinfacht die o.g. Routineaufgabe zu:
 * <code>new XmlDocument("gemuese.xml").getNodeList("//tomate")</code>.
 * Sie bietet au&szlig;erdem Typsicherheit beim Umgang mit XPath, im Gegensatz
 * zur Java-Plattform.
 * </p>
 *
 * @see javax.xml.xpath
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class XmlDocument {
	/**
	 * Parst die &uuml;bergegebene XML-Datei und validiert sie gegen die DTD,
	 * die in der Datei angegeben ist.
	 *
	 * @param fileToParse Die XML-Datei, die geparst und validiert werden soll,
	 * z.B. <code>new File("spielzeug/tiere/kuschelnilpferd.xml")</code>.
	 * @param baseDir Das Verzeichnis, das f&uuml;rs Aufl&ouml;sen von relativen
	 * URIs als Ausgangspunkt genommen wird. Das ist haupts&auml;chlich relevant
	 * f&uuml;rs Finden von DTDs, besonders falls die DTD in einem anderen
	 * Verzeichnis liegt als die XML-Datei. &ndash; Beispiel: In unseren
	 * Parcoursdateien wird die DTD folgenderma&szlig;en angegeben:
	 * <code>&lt;!DOCTYPE collection SYSTEM "parcours.dtd"></code>. Die
	 * relative URI ist dabei ist <code>parcours.dtd</code> (im XML-Jargon
	 * "system identifier" genannt). Wenn diese Klasse also eine XML-Datei mit
	 * eben genannten Zeile parst und z.B. als baseDir &quot;wurst&quot;
	 * &uuml;bergeben wurde, wird der Parser versuchen, die Datei
	 * wurst/parcours.dtd zu &ouml;ffnen und als DTD zu verwenden.
	 * @return das Dokument
	 * @throws SAXException Falls die &uuml;bergebene Datei nicht geparst werden
	 * kann, etwa weil das enthaltene XML nicht wohlgeformt ist oder nicht gegen
	 * die DTD validiert. Eine SAXException wird unter genau den Umst&auml;nden
	 * geworfen, wenn {@link DocumentBuilder#parse(InputStream, String)}, eine
	 * wirft.
	 * @throws IOException Falls beim Lesen der &uuml;bergebenen Datei eine
	 * IOException auftritt, etwa weil die Datei nicht lesbar ist.
	 * @throws ParserConfigurationException In dem abwegigen Fall, dass die
	 * Java-Plattform keinen validierenden Parser auftreiben kann.
	 */
	public static QueryableDocument parse(File fileToParse, String baseDir)
	throws SAXException, IOException, ParserConfigurationException {
		return parse(new FileInputStream(fileToParse), baseDir);
	}

	/**
	 * Wie {@link #parse(java.io.File, java.lang.String)}, aber verwendet
	 * als Parameter &quot;baseDir&quot; das Verzeichnis, das das
	 * &uuml;bergebene File enth&auml;lt. Relevant also f&uuml;r den Fall, dass
	 * die XML- und DTD-Datei im selben Verzeichnis liegen.
	 *
	 * @param fileToParse Die XML-Datei, die geparst und validiert werden soll,
	 * z.B. <code>new File("sachen/gemuese/gurken.xml")</code>.
	 * @return Das Dokument
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 */
	public static QueryableDocument parse(File fileToParse)
	throws SAXException, IOException, ParserConfigurationException {
		return parse(fileToParse, fileToParse.getParent() + "/");
	}

	/**
	 * Wie {@link #parse(java.io.File)}.
	 *
	 * @param fileToParse Name und ggf. Pfad der XML-Datei, die geparst und
	 * validiert werden soll, z.B. "sachen/gemuese/gurken.xml".
	 * @return Das Dokument
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 */
	public static QueryableDocument parse(String fileToParse)
		throws SAXException, IOException, ParserConfigurationException {
		return parse(new File(fileToParse));
	}

	/**
	 * Wie {@link #parse(java.io.File, java.lang.String)}, aber liest die
	 * Datei aus dem &uuml;bergebenen InputStream.
	 * @param documentStream 
	 * @param baseDir 
	 * @return Das Dokument
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 */
	public static QueryableDocument parse(
		InputStream documentStream, String baseDir)
		throws SAXException, IOException, ParserConfigurationException {
		// Irrsinnige Buerokratie ...
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setValidating(true); // Soll gegen DTD validieren
		DocumentBuilder parser = f.newDocumentBuilder();
		/*
		 * Wichtig fuer Validierung: Wenn kein ErrorHandler gesetzt und eine
		 * XML-Datei geparst wird, die zwar wohlgeformt, aber nicht gueltig
		 * (valid) ist, dann gibt der Parser eine dumme Warnung auf stderr aus,
		 * man solle doch einen ErrorHandler setzen
		 */
		parser.setErrorHandler(new ErrorHandler() {
			public void error(SAXParseException exception)
			throws SAXException {
				throw exception;
			}

			public void fatalError(SAXParseException exception)
			throws SAXException {
				throw exception;
			}

			public void warning(SAXParseException exception)
			throws SAXException {
				throw exception;
			}});
		Document document = parser.parse(documentStream, baseDir);
		try {
			// document kann nicht null sein
			return Decoratoror.createDecorated(QueryableDocument.class,
				new QueryableMixin(document), document);
		} catch (NoSuchMethodException e) {
			// "Kann nicht passieren"
			throw new AssertionError(e);
		}
	}

	/**
	 * @param baseNode Node
	 * @return QueryableNode
	 */
	static QueryableNode createQueryableNode(Node baseNode) {
		try {
			if (baseNode == null)
				return null;
			return Decoratoror.createDecorated(QueryableNode.class,
				new QueryableMixin(baseNode), baseNode);
		} catch (NoSuchMethodException e) {
			// "Kann nicht passieren"
			throw new AssertionError(e);
		}
	}

	/**
	 * Mixin
	 */
	public static class QueryableMixin implements XPathQueryable {
		/**
		 * Dieser Mixin wird in diesen Knoten hineingemixt; Drandenken:
		 * Dokumente sind auch Knoten
		 */
		private Node cocktail;

		/** Hiwi zum Auswerten von XPath-Ausdr&uuml;cken */
		private XPath evaluator = XPathFactory.newInstance().newXPath();

		/**
		 * @param forWhichNode	Knoten
		 */
		public QueryableMixin(Node forWhichNode) {
			this.cocktail = forWhichNode;
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getNodeList(java.lang.String)
		 */
		public IterableNodeList getNodeList(String xPathExpression)
		throws XPathExpressionException {
			return new IterableNodeList((NodeList)evaluator.evaluate(
				xPathExpression, cocktail, XPathConstants.NODESET));
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getNode(java.lang.String)
		 */
		public QueryableNode getNode(String xPathExpression)
		throws XPathExpressionException {
			return createQueryableNode((Node)evaluator.evaluate(
				xPathExpression, cocktail, XPathConstants.NODE));
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getString(java.lang.String)
		 */
		public String getString(String xPathExpression)
		throws XPathExpressionException {
			return (String)evaluator.evaluate(xPathExpression, cocktail,
				XPathConstants.STRING);
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getNumber(java.lang.String)
		 */
		public Double getNumber(String xPathExpression)
		throws XPathExpressionException {
			/*
			 * Cast nach Double (nicht Number) -- eine XPath-Number mappt in
			 * Java auf einen Double, siehe
			 * http://java.sun.com/j2se/1.5.0/docs/api/javax/xml/xpath/XPathConstants.html#NUMBER
			 */
			return (Double)evaluator.evaluate(xPathExpression, cocktail,
				XPathConstants.NUMBER);
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getBoolean(java.lang.String)
		 */
		public Boolean getBoolean(String xPathExpression)
		throws XPathExpressionException {
			return (Boolean)evaluator.evaluate(xPathExpression, cocktail,
				XPathConstants.BOOLEAN);
		}

		/**
		 * Knoten vorhanden?
		 * @param <T>
		 * @param xPathExpression
		 * @param value
		 * @return Knoten oder null
		 * @throws XPathExpressionException
		 */
		private <T> T nullIfNotExists(String xPathExpression, T value)
		throws XPathExpressionException {
			// War der Knoten nicht da oder war er leer?
			return getNode(xPathExpression) == null ? null : value;
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getStringOrNull(java.lang.String)
		 */
		public String getStringOrNull(String xPathExpression)
		throws XPathExpressionException {
			String rv = getString(xPathExpression);
			if ("".equals(rv))
				return nullIfNotExists(xPathExpression, rv);
			else
				return rv;
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getNumberOrNull(java.lang.String)
		 */
		public Number getNumberOrNull(String xPathExpression)
		throws XPathExpressionException {
			Double rv = getNumber(xPathExpression);
			if (rv.isNaN())
				return nullIfNotExists(xPathExpression, rv);
			else
				return rv;
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getBooleanOrNull(java.lang.String)
		 */
		public Boolean getBooleanOrNull(String xPathExpression)
		throws XPathExpressionException {
			Boolean rv = getBoolean(xPathExpression);
			if (rv == false)
				return nullIfNotExists(xPathExpression, rv);
			else
				return rv;
		}
	}

	/**
	 * Selektiert mehrere Knoten
	 */
	public interface XPathQueryable {
		/**
		 * <p>
		 * Selektiert mehrere Knoten. Wenn z.B. beim Konstruieren dieses Objekts
		 * eine Datei angegeben wurde, die 42 St&uuml;ck
		 * <code>&lt;wurst&gt;...&lt;/wurst&gt</code>-Elemente enth&auml;lt,
		 * wird <code>getNodeList("//wurst")</code> eine Liste dieser 42
		 * Elemente zur&uuml;ckliefern.
		 * </p>
		 * <p>
		 * <strong>Drandenken:</strong> Knoten m&uuml;ssen nicht Elemente sein
		 * &ndash; Attribute, CData-Abschnitte usw. sind auch Knoten. Siehe
		 * {@link Node}.
		 * </p>
		 * @param xPathExpression 
		 *
		 * @return Die selektierten Knoten als {@link NodeList}, wie sie die
		 * Java-Plattform implementiert, die aber auch {@link Iterable} ist.
		 * <code>for (Node n : document.getNodeList(...))</code> ist also
		 * m&ouml;glich. Gibt es im Dokument keine Knoten, die von dem
		 * XPath-Ausdruck selektiert werden, wird eine NodeList
		 * zur&uuml;ckgegeben, die keine Knoten enth&auml;lt.
		 * @throws XPathExpressionException 
		 */
		public IterableNodeList getNodeList(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * <p>
		 * Selektiert einen einzelnen Knoten. Falls der &uuml;bergebene
		 * XPath-Ausdruck mehrere Knoten selektiert, wird er erste
		 * zur&uuml;ckgeliefert; falls er keine Knoten selektiert, wird
		 * {@code null} zur&uuml;ckliefert.
		 * </p>
		 * <p>
		 * <strong>Drandenken:</strong> Knoten m&uuml;ssen nicht Elemente sein
		 * &ndash; Attribute, CData-Abschnitte usw. sind auch Knoten. Siehe
		 * {@link Node}.
		 * </p>
		 * @param xPathExpression 
		 * @return Knoten
		 * @throws XPathExpressionException 
		 */
		public QueryableNode getNode(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * <p>
		 * Liefert den &quot;string-value&quot; der Knoten, die vom
		 * &uuml;bergebenen XPath-Ausdruck selektiert wurden. Liefert einen
		 * leeren String (""), falls der &uuml;bergebene XPath-Ausdruck keine
		 * Knoten selektiert (gem&auml;&szlig; <a
		 * href="http://www.w3.org/TR/xpath#function-string">XPath-Konvertierungsregeln</a>).
		 * Der &quot;string-value&quot; ist &ndash;
		 * <ul>
		 * <li>f&uuml;r Elemente: s&auml;mtlicher Text, den sie enthalten.
		 * Beispiel: &quot;wurstbrot&quot;, wenn die Elemente
		 * <code>&lt;fruehstueck&gt;&lt;a&gt;wurst&lt;/a&gt;&lt;b&gt;brot&lt;/b&gt;&lt;/fruehstueck&gt;</code>
		 * mit XPath selektiert wurden.</li>
		 * <li>f&uuml;r Attribute: der Wert des Attributs. Beispiel:
		 * &quot;kaese&quot; falls das erste Attribut von
		 * <code>&lt;fruehstueck belag=&quot;kaese&quot;&gt;</code> mit XPath
		 * selektiert wurde. <a
		 * href="http://www.w3.org/TR/xml/#AVNormalize">Details zur Umwandlung
		 * eines Attributknotens in einen String</a></li>
		 * <li>f&uuml;r Textknoten: ihr Inhalt</li>
		 * <li>f&uuml;r alle anderen Knotenarten: Siehe <a
		 * href="http://www.w3.org/TR/xpath.html#data-model">XPath-Spezifikation
		 * Abschnitt 5</a></li>
		 * </ul>
		 * Details zum Thema "string-value" siehe <a
		 * href="http://www.w3.org/TR/xpath.html#data-model">XPath-Spezifikation
		 * Abschnitt 5</a>.
		 * </p>
		 * @param xPathExpression 
		 * @return String
		 * @throws XPathExpressionException 
		 *
		 * @see #getStringOrNull(String)
		 */
		public String getString(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * Selektiert Knoten, konvertiert sie in einen String (siehe
		 * {@link #getString(String)}) und konvertiert den String in einen
		 * Double. Falls der &uuml;bergebene XPath-Ausdruck keine Knoten
		 * selektiert, wird {@code NaN} zur&uuml;ckgeliefert (d.h.
		 * {@code getNumber(...).isNaN() == true}). <a
		 * href="http://www.w3.org/TR/xpath#function-number">Details zur
		 * Konvertierung in eine Zahl</a>
		 * @param xPathExpression 
		 * @return Zahl
		 * @throws XPathExpressionException 
		 *
		 * @see #getNumberOrNull(String)
		 */
		public Double getNumber(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * Selektiert Knoten, konvertiert sie in einen String (siehe
		 * {@link #getString(String)}) und konvertiert den String in einen
		 * Boolean. Falls der &uuml;bergebene XPath-Ausdruck keine Knoten
		 * selektiert, wird {@code false} zur&uuml;ckgeliefert. <a
		 * href="http://www.w3.org/TR/xpath#function-boolean">Details zur
		 * Konvertierung in einen Boolean</a>
		 * @param xPathExpression 
		 * @return Boolean
		 * @throws XPathExpressionException 
		 *
		 * @see #getBooleanOrNull(String)
		 */
		public Boolean getBoolean(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * Wie {@link #getString(String) getString()}, aber falls der
		 * &uuml;bergebene XPath-Ausdruck keine Knoten selektiert, wird
		 * {@code null} zur&uuml;ckgeliefert. So kann man auseinanderhalten, ob
		 * der Knoten nicht existiert ({@code null}) oder ob er existiert,
		 * aber keinen Text enth&auml;lt ("").
		 * @param xPathExpression 
		 * @return String / null
		 * @throws XPathExpressionException 
		 */
		public String getStringOrNull(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * Wie {@link #getNumber(String) getNumber()}, aber falls der
		 * &uuml;bergebene XPath-Ausdruck keine Knoten selektiert, wird
		 * {@code null} zur&uuml;ckgeliefert. So kann man auseinanderhalten, ob
		 * der Knoten nicht existiert ({@code null}) oder ob er existiert,
		 * aber etwas anderes als eine Zahl enth&auml;lt.
		 * @param xPathExpression 
		 * @return Zahl / null
		 * @throws XPathExpressionException 
		 */
		public Number getNumberOrNull(String xPathExpression)
		throws XPathExpressionException;

		/**
		 * Wie {@link #getBoolean(String) getBoolean()}, aber falls der
		 * &uuml;bergebene XPath-Ausdruck keine Knoten selektiert, wird
		 * {@code null} zur&uuml;ckgeliefert. So kann man auseinanderhalten, ob
		 * der Knoten nicht existiert ({@code null}) oder ob er wirklich da
		 * ist und {@code false} enth&auml;lt.
		 * @param xPathExpression 
		 * @return Boolean / null
		 * @throws XPathExpressionException 
		 */
		public Boolean getBooleanOrNull(String xPathExpression)
		throws XPathExpressionException;
	}
}
