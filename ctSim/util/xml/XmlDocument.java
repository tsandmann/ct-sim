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
 * Klasse, die die Arbeit mit XPath vereinfachen und erträglicher machen soll.
 * </p>
 * <h3>XPath?</h3>
 * <p>
 * Eine Metapher für <a href="http://en.wikipedia.org/wiki/Xpath">XPath</a> sind reguläre Ausdrücke:
 * So, wie reguläre Ausdrücke Teilstrings in einem langen String fangen, fängt XPath Teile aus einem
 * XML-Dokument. XPath lässt sich schnell lernen durch die
 * <a href="http://www.w3.org/TR/xpath.html#path-abbrev">Anwendungsbeispiele in der Spezifikation</a>
 * ("context node" heißt in aller Regel Root-Knoten des XML-Dokuments.)
 * </p>
 * <h3>Verwendungsbeispiel:</h3>
 * <p>
 * <ul>
 * <li><code>XmlDocument d = new <a href="#XmlDocument(java.lang.String)">XmlDocument</a>("pfad/zu/ner/datei.xml");</code></li>
 * <li><code>Node r = d.<a href="#getNode(java.lang.String)">getNode</a>("//ruebe[12]"); // Einzelknoten: zwölfte Rübe</code></li>
 * <li><code>for (Node node : d.<a href="#getNodeList(java.lang.String)">getNodeList</a>("//gurke")) { ... } // Liste aller Gurken im XML</code></li>
 * </ul>
 * <code>getNodeList()</code> liefert dabei eine {@link org.w3c.dom.NodeList} gemäß der Java-Plattform-API,
 * die aber als Verbesserung auch {@link Iterable} implementiert. Daher ist sie in der gezeigten Weise in
 * for-each-Schleifen verwendbar, im Gegensatz zur normalen NodeList.
 * </p>
 * <h3>Spezifikation:</h3>
 * <p>
 * Diese Klasse greift zurück auf die XPath-API aus der Java-Plattform (javax.xml.xpath).
 * Es ist der Dokumentation nicht zu entnehmen, welche XPath-Version die Plattform-API implementiert,
 * aber ich glaube es ist 1.0 (das wird zumindest bei {@link XPath} angedeutet);
 * siehe <a href="http://www.w3.org/TR/xpath.html">XPath-1.0-Spezifikation</a>
 * </p>
 * <h3>Motivation:</h3>
 * <p>
 * Die Handhabung von XPath in der normalen Java-Plattform ist leider sehr "javaig" (d.h. umständlich
 * und bürokratisch). Eine Routineaufgabe wie "bitte mal <code>//tomate</code> auf gemuese.xml anwenden"
 * erfordert, wenn ich das richtig sehe, mindestens folgende Monstrosität:
 * <code>XPathFactory.newInstance().newXPath().evaluate(DocumentBuilderFactory.newDocumentBuilder().parse("gemuese.xml"), "//tomate");</code>
 * ... und ist dann noch nicht mal typsicher. (Bei der verwendeten Methode handelt es sich übrigens um
 * eine "convenience"-Methode, d.h. das ist noch der einfachere Fall. Was auch nicht hilft ist, dass
 * man sich den o.g. Aufruf mühsam zusammenstückeln muss, da die Dokumentation leider unübersichtlich
 * geraten ist und sich ständig verzettelt.)
 * </p>
 * <p>
 * Diese Klasse vereinfacht die o.g. Routineaufgabe zu:
 * <code>new XmlDocument("gemuese.xml").getNodeList("//tomate")</code>.
 * Sie bietet außerdem Typsicherheit beim Umgang mit XPath, im Gegensatz zur Java-Plattform; siehe javax.xml.xpath
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class XmlDocument {
	/**
	 * Parst die übergegebene XML-Datei und validiert sie gegen die DTD, die in der Datei angegeben ist.
	 *
	 * @param fileToParse	Die XML-Datei, die geparst und validiert werden soll,
	 * 			z.B. <code>new File("spielzeug/tiere/kuschelnilpferd.xml")</code>.
	 * @param baseDir		Das Verzeichnis, das fürs Auflösen von relativen URIs als Ausgangspunkt
	 * 			genommen wird. Das ist hauptsächlich relevant für das Finden von DTDs, besonders falls
	 * 			die DTD in einem anderen Verzeichnis liegt als die XML-Datei. - Beispiel: In unseren
	 * 			Parcoursdateien wird die DTD folgendermaßen angegeben:
	 * 			<code><!DOCTYPE collection SYSTEM "parcours.dtd"></code>.
	 * 			Die relative URI ist dabei ist <code>parcours.dtd</code> (im XML-Jargon "system identifier"
	 * 			genannt). Wenn diese Klasse also eine XML-Datei mit eben genannten Zeile parst und z.B.
	 * 			als baseDir "wurst" übergeben wurde, wird der Parser versuchen, die Datei wurst/parcours.dtd
	 * 			zu öffnen und als DTD zu verwenden.
	 * @return das Dokument
	 * @throws SAXException	falls die übergebene Datei nicht geparst werden kann, etwa weil das enthaltene
	 * 			XML nicht wohlgeformt ist oder nicht gegen die DTD validiert. Eine SAXException wird unter
	 * 			genau den Umständen geworfen, wenn {@link DocumentBuilder#parse(InputStream, String)}, eine
	 * 			solche wirft.
	 * @throws IOException	falls beim Lesen der übergebenen Datei eine IOException auftritt, etwa weil die
	 * 			Datei nicht lesbar ist.
	 * @throws ParserConfigurationException	in dem abwegigen Fall, dass die Java-Plattform keinen validierenden
	 * 			Parser auftreiben kann.
	 */
	public static QueryableDocument parse(File fileToParse, String baseDir)
			throws SAXException, IOException, ParserConfigurationException {
		return parse(new FileInputStream(fileToParse), baseDir);
	}

	/**
	 * Wie {@link #parse(java.io.File, java.lang.String)}, aber verwendet als Parameter "baseDir" das
	 * Verzeichnis, das das übergebene File enthält. Relevant also für den Fall, dass die XML- und
	 * DTD-Datei im selben Verzeichnis liegen.
	 *
	 * @param fileToParse	Die XML-Datei, die geparst und validiert werden soll,
	 * 			z.B. <code>new File("sachen/gemuese/gurken.xml")</code>.
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
	 * Wie {@link #parse(java.io.File)}:
	 *
	 * @param fileToParse	Name und ggf. Pfad der XML-Datei, die geparst und validiert werden soll,
	 * 			z.B. "sachen/gemuese/gurken.xml".
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
	 * Wie {@link #parse(java.io.File, java.lang.String)}, liest aber die Datei aus dem übergebenen
	 * InputStream.
	 *
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
		// Irrsinnige Bürokratie ...
		DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
		f.setValidating(true);	// soll gegen DTD validieren
		DocumentBuilder parser = f.newDocumentBuilder();
		/**
		 * Wichtig für die Validierung: Wenn kein ErrorHandler gesetzt und eine XML-Datei geparst wird,
		 * die zwar wohlgeformt, aber nicht gültig (valid) ist, dann gibt der Parser eine dumme Warnung
		 * auf stderr aus, man solle doch einen ErrorHandler setzen.
		 */
		parser.setErrorHandler(new ErrorHandler() {
			@Override
			public void error(SAXParseException exception)
					throws SAXException {
				throw exception;
			}

			@Override
			public void fatalError(SAXParseException exception)
					throws SAXException {
				throw exception;
			}

			@Override
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
	 * @param baseNode	Node
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

	/** Mixin */
	public static class QueryableMixin implements XPathQueryable {
		/** Dieser Mixin wird in diesen Knoten hinein gemixt; Bedenken: Dokumente sind auch Knoten */
		private Node cocktail;

		/** Hiwi zum Auswerten von XPath-Ausdrücken */
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
		@Override
		public IterableNodeList getNodeList(String xPathExpression)
				throws XPathExpressionException {
			return new IterableNodeList((NodeList)evaluator.evaluate(
					xPathExpression, cocktail, XPathConstants.NODESET));
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getNode(java.lang.String)
		 */
		@Override
		public QueryableNode getNode(String xPathExpression)
				throws XPathExpressionException {
			return createQueryableNode((Node)evaluator.evaluate(
					xPathExpression, cocktail, XPathConstants.NODE));
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getString(java.lang.String)
		 */
		@Override
		public String getString(String xPathExpression)
				throws XPathExpressionException {
			return (String)evaluator.evaluate(xPathExpression, cocktail,
					XPathConstants.STRING);
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getNumber(java.lang.String)
		 */
		@Override
		public Double getNumber(String xPathExpression)
				throws XPathExpressionException {
			/**
			 * Cast nach Double (nicht Number) - eine XPath-Number mappt in Java auf einen Double,
			 * siehe <a href="http://java.sun.com/j2se/1.5.0/docs/api/javax/xml/xpath/XPathConstants.html#NUMBER"></a>
			 */
			return (Double)evaluator.evaluate(xPathExpression, cocktail,
					XPathConstants.NUMBER);
		}

		/**
		 * @see ctSim.util.xml.XmlDocument.XPathQueryable#getBoolean(java.lang.String)
		 */
		@Override
		public Boolean getBoolean(String xPathExpression)
				throws XPathExpressionException {
			return (Boolean)evaluator.evaluate(xPathExpression, cocktail,
					XPathConstants.BOOLEAN);
		}

		/**
		 * Knoten vorhanden?
		 *
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
		@Override
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
		@Override
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
		@Override
		public Boolean getBooleanOrNull(String xPathExpression)
				throws XPathExpressionException {
			Boolean rv = getBoolean(xPathExpression);
			if (rv == false)
				return nullIfNotExists(xPathExpression, rv);
			else
				return rv;
		}
	}

	/** Selektiert mehrere Knoten */
	public interface XPathQueryable {
		/**
		 * <p>
		 * Selektiert mehrere Knoten. Wenn z.B. beim Konstruieren dieses Objekts eine Datei angegeben
		 * wurde, die 42 Stück <code><wurst>...</wurst></code>-Elemente enthält, wird
		 * <code>getNodeList("//wurst")</code> eine Liste dieser 42 Elemente zurückliefern.
		 * </p>
		 * <p>
		 * <strong>Beachten:</strong> Knoten müssen nicht Elemente sein; Attribute, CData-Abschnitte
		 * usw. sind auch Knoten. Siehe {@link Node}.
		 * </p>
		 *
		 * @param xPathExpression
		 * @return Die selektierten Knoten als {@link NodeList}, wie sie die Java-Plattform implementiert,
		 * 			die aber auch {@link Iterable} ist. <code>for (Node n : document.getNodeList(...))</code>
		 * 			ist also möglich. Gibt es im Dokument keine Knoten, die von dem XPath-Ausdruck selektiert
		 * 			werden, wird eine NodeList zurückgegeben, die keine Knoten enthält.
		 * @throws XPathExpressionException
		 */
		public IterableNodeList getNodeList(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * <p>
		 * Selektiert einen einzelnen Knoten. Falls der übergebene XPath-Ausdruck mehrere Knoten
		 * selektiert, wird er erste zurückgeliefert; falls er keine Knoten selektiert, wird {@code null}
		 * zurückliefert.
		 * </p>
		 * <p>
		 * <strong>Beachten:</strong> Knoten müssen nicht Elemente sein; Attribute, CData-Abschnitte usw.
		 * sind auch Knoten; siehe {@link Node}.
		 * </p>
		 *
		 * @param xPathExpression
		 * @return Knoten
		 * @throws XPathExpressionException
		 */
		public QueryableNode getNode(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * <p>
		 * Liefert den "string-value" der Knoten, die vom übergebenen XPath-Ausdruck selektiert wurden.
		 * Liefert einen leeren String (""), falls der übergebene XPath-Ausdruck keine Knoten selektiert
		 * (gemäß <a href="http://www.w3.org/TR/xpath#function-string">XPath-Konvertierungsregeln</a>).
		 * Der "string-value" ist:
		 * <ul>
		 * <li>für Elemente: sämtlicher Text, den sie enthalten. Beispiel: "wurstbrot", wenn die Elemente
		 * <code><fruehstueck><a>wurst</a><b>brot</b></fruehstueck></code> mit XPath selektiert wurden.</li>
		 * <li>für Attribute: der Wert des Attributs. Beispiel: "käse" falls das erste Attribut von
		 * <code><fruehstueck belag="käse"></code> mit XPath selektiert wurde.
		 * <a href="http://www.w3.org/TR/xml/#AVNormalize">Details zur Umwandlung eines Attributknotens in
		 * einen String</a></li>
		 * <li>für Textknoten: ihr Inhalt</li>
		 * <li>für alle anderen Knotenarten:
		 * siehe <a href="http://www.w3.org/TR/xpath.html#data-model">XPath-Spezifikation Abschnitt 5</a></li>
		 * </ul>
		 * Details zum Thema "string-value":
		 * siehe <a href="http://www.w3.org/TR/xpath.html#data-model">XPath-Spezifikation Abschnitt 5</a>.
		 * </p>
		 *
		 * @param xPathExpression
		 * @return String
		 * @throws XPathExpressionException
		 *
		 * @see #getStringOrNull(String)
		 */
		public String getString(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * Selektiert Knoten, konvertiert sie in einen String (siehe {@link #getString(String)}) und
		 * konvertiert den String in einen Double. Falls der übergebene XPath-Ausdruck keine Knoten
		 * selektiert, wird {@code NaN} zurückgeliefert (d.h. {@code getNumber(...).isNaN() == true}).
		 * <a href="http://www.w3.org/TR/xpath#function-number">Details zur Konvertierung in eine Zahl</a>
		 *
		 * @param xPathExpression
		 * @return Zahl
		 * @throws XPathExpressionException
		 *
		 * @see #getNumberOrNull(String)
		 */
		public Double getNumber(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * Selektiert Knoten, konvertiert sie in einen String (siehe {@link #getString(String)}) und
		 * konvertiert den String in einen Boolean. Falls der übergebene XPath-Ausdruck keine Knoten
		 * selektiert, wird {@code false} zurückgeliefert.
		 * <a href="http://www.w3.org/TR/xpath#function-boolean">Details zur Konvertierung in einen Boolean</a>
		 *
		 * @param xPathExpression
		 * @return Boolean
		 * @throws XPathExpressionException
		 *
		 * @see #getBooleanOrNull(String)
		 */
		public Boolean getBoolean(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * Wie {@link #getString(String) getString()}, aber falls der übergebene XPath-Ausdruck keine
		 * Knoten selektiert, wird {@code null} zurückgeliefert. So kann man auseinanderhalten, ob der
		 * Knoten nicht existiert ({@code null}) oder ob er existiert, aber keinen Text enthält ("").
		 *
		 * @param xPathExpression
		 * @return String / null
		 * @throws XPathExpressionException
		 */
		public String getStringOrNull(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * Wie {@link #getNumber(String) getNumber()}, aber falls der übergebene XPath-Ausdruck keine
		 * Knoten selektiert, wird {@code null} zurückgeliefert. So kann man auseinanderhalten, ob der
		 * Knoten nicht existiert ({@code null}) oder ob er existiert, aber etwas anderes als eine Zahl
		 * enthält.
		 *
		 * @param xPathExpression
		 * @return Zahl / null
		 * @throws XPathExpressionException
		 */
		public Number getNumberOrNull(String xPathExpression)
				throws XPathExpressionException;

		/**
		 * Wie {@link #getBoolean(String) getBoolean()}, aber falls der übergebene XPath-Ausdruck keine
		 * Knoten selektiert, wird {@code null} zurückgeliefert. So kann man auseinander halten, ob der
		 * Knoten nicht existiert ({@code null}) oder ob er wirklich da ist und {@code false} enthält.
		 *
		 * @param xPathExpression
		 * @return Boolean / null
		 * @throws XPathExpressionException
		 */
		public Boolean getBooleanOrNull(String xPathExpression)
				throws XPathExpressionException;
	}
}
