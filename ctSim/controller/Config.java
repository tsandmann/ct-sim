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

package ctSim.controller;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import ctSim.util.FileIconMap;
import ctSim.util.FmtLogger;
import ctSim.util.IconProvider;
import ctSim.util.Misc;
import ctSim.util.xml.QueryableDocument;
import ctSim.util.xml.QueryableNode;
import ctSim.util.xml.XmlDocument;

/**
 * Managed die Konfiguration des ct-Sim
 * Theoretisch kann man mehr als einmal initialisieren (Konfig laden), aber das ist ungetestet.
 */
public class Config {
	/** für Dateien */
	public static class SourceFile extends File {
		/** UID */
		private static final long serialVersionUID = 3022935037996253818L;

		/**
		 * Neues Dateihandle
		 * 
		 * @param pathAndName	Pfad
		 */
		public SourceFile(String pathAndName) {
			super(pathAndName);
		}
	}

	/** Logger */
	static FmtLogger lg = FmtLogger.getLogger("ctSim.controller.Config");

	/** <p>Default-Werte der Konfiguration. Ist ein Array, um sie im Quelltext möglichst bequem notieren
	 * zu können.</p>
	 *
	 * <p>Vorteile: Die Werte sind hier zentral statt quer durch den Quelltext verteilt; Code, der
	 * {@link #getValue(String)} aufruft, kann einfacher werden, da nicht dauernd der Rügabewert gegen
	 * <code>null</code> geprüft werden muss - wenn in diesem Array ein Wert steht, kann getValue(String)
	 * kein <code>null</code> mehr liefern.</p>
	 */
	static final String[] parameterFallbacks = {
		"botport", "10001",
		"judge", "ctSim.model.rules.DefaultJudge",
		"worlddir", ".",	// TODO: besser dokumentieren in ct-sim.xml und Co.
		"botdir", ".",
		"useContestConductor", "false",
		"contestBotTargetDir", "tmp",
		"contestBotFileNamePrefix", "tmp-contest-bot",
		"contestBotFileNameSuffix", ".exe",
		"simTimePerStep", "10",
		"ctSimTimeout", "10000",
		"simBotErrorHandling", "kill",
	};

	/** Fallback-Farbe */
	static final Color botColorFallback = Color.GRAY;

	/** mögliche Parametertypen */
	static final ParameterType[] parameterTypes = {
		new ParameterType("ctSimTimeout", Integer.class),
		new ParameterType("parcours", File.class),
		new ParameterType("worlddir", File.class),
		new ParameterType("botport", Integer.class),
		new ParameterType("botbinary", File.class),
		new ParameterType("botdir", File.class),
	};

	/** <p>Enthält die Einzelparameter der Konfiguration (spiegelt also die <code><parameter></code>-Tags
	 * wider)</p>
	 *
	 * <p>Verwendung: Wird zunächst auf die Default-Werte gesetzt, die aus dem hartkodierten Array
	 * <code>configDefaults</code> kommen. Beim späteren Parsen der Konfigurationsdatei werden Defaults
	 * dann möglicherweise überschrieben.</p> */
	private static PlainParameters parameters;

	/** Bot-Appearances */
	private static BotAppearances appearances;

	/** Icons */
	@SuppressWarnings("unused")
	private static IconProvider icons;
	
	/**
	 * Lädt die <code><parameter></code>-Tags aus der Konfigurationsdatei des Sims. Die Werte der Tags
	 * sind dann mittels {@link #getValue(String)} verfügbar.
	 * 
	 * @param file	Konfigurationsdatei dem von "config/config.dtd" vorgeschriebenen XML-Format.
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void loadConfigFile(String file)
	throws SAXException, IOException, ParserConfigurationException {
		lg.info("Lade Konfigurationsdatei '"+file+"'");
		java.net.URL url = ClassLoader.getSystemResource(file);
		if (url != null) {
			QueryableDocument doc = XmlDocument.parse(url.openStream(), url.toString());
			parameters = new PlainParameters(doc);
			appearances = new BotAppearances(doc);
		} else {
			throw new FileNotFoundException();
		}
	}

	/**
	 * Lädt Icons
	 * 
	 * @param iconsBaseDirectory	Verzeichnis
	 * @throws NullPointerException
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 */
	public static void loadIcons(File iconsBaseDirectory)
	throws NullPointerException, FileNotFoundException,
	IllegalArgumentException {
		setIconProvider(new FileIconMap(iconsBaseDirectory));
	}

	/**
	 * @param iconProvider
	 */
	public static void setIconProvider(IconProvider iconProvider) {
		icons = iconProvider;
	}

	/**
	 * @param key
	 * @return Wert zum Key
	 */
	public static String getValue(String key) {
		if (parameters == null) {
			return "";
		}
		return parameters.get(key);
	}

	/**
	 * @param botType			Bot-Typ
	 * @param index				Bot-Nummer
	 * @param appearanceType	Appearance
	 * @return Farbe
	 */
	public static Color getBotColor(Class<?> botType, int index,
	String appearanceType) {
		return appearances.get(botType, appearanceType, index);
	}

	/**
	 * @param key
	 * @return Icon zum Key
	 */
	public static Icon getIcon(String key) {
		URL u = ClassLoader.getSystemResource("images/" + key+".gif");
		// NullPointerException vermeiden
		if (u == null)
			return new ImageIcon();	// leeres Icon
		else
			return new ImageIcon(u);		
	}

	/** HashMap für Plain-Parameter */
	static class PlainParameters extends HashMap<String, String> {
		/** UID */
		private static final long serialVersionUID = - 6931464910390788433L;

		/**
		 * Lädt die <code><parameter></code>-Tags aus der
		 * Konfigurationsdatei des Sims. Die Werte der Tags sind dann mittels
		 * get(String) verfügbar.
		 * @param doc Config-Dokument
		 */
		PlainParameters(QueryableDocument doc) {
			if (parameterFallbacks.length % 2 != 0)
				throw new AssertionError();
			// Defaults laden
			for (int i = 0; i < parameterFallbacks.length; i += 2)
				put(parameterFallbacks[i], parameterFallbacks[i + 1]);

			// XML laden
			try {
				for(QueryableNode n : doc.getNodeList("/ct-sim/parameter")) {
					String parmOs = n.getString("@os").toLowerCase();
					// Beispiele für os.name: "Windows XP", "Linux", "Mac OS X"
					// Siehe http://tolstoy.com/samizdat/sysprops.html
					String currentOs = System.getProperty("os.name")
						.toLowerCase();

					/* Attribut os nicht vorhanden (= alle Betriebsysteme), oder vorhanden und System
					 * passt startsWith() damit "Windows" in der ct-sim.xml das von System.getProperty()
					 * gelieferte "Windows XP" matcht
					 */
					if ("".equals(parmOs) || Misc.startsWith(currentOs, parmOs))
						put(n.getString("@name"), n.getString("@value"));
				}
			} catch (XPathExpressionException e) {
				// "Kann nicht passieren"
				throw new AssertionError(e);
			}
		}
	}

	/** Hash-Map für Bot-Appearances */
	static class BotAppearances
	extends HashMap<BotAppearances.AppearanceKey, List<Color>> {
		/** UID */
		private static final long serialVersionUID = 8690190797733423514L;

		/** Appearance-Key */
		class AppearanceKey {
			/** Bot-Typ */
			final Class<?> botType;
			/** Appearance-Typ */
			final String appearanceType;

			/**
			 * Neue Appearance
			 * 
			 * @param botType			Bot-Typ
			 * @param appearanceType	Appearance
			 */
			public AppearanceKey(final Class<?> botType,
				final String appearanceType) {
				this.botType = botType;
				this.appearanceType = appearanceType;
			}

			/**
			 * Wichtig, weil wir es als Schlüssel in der Map verwenden
			 * 
			 * @see java.lang.Object#equals(java.lang.Object)
			 */
			@Override
			public boolean equals(Object o) {
				if (! (o instanceof AppearanceKey))
					return false;
				AppearanceKey that = (AppearanceKey)o;
				if (this.botType == null && that.botType != null)
					return false;
				if (! this.botType.equals(that.botType))
					return false;
				if (this.appearanceType == null && that.appearanceType != null)
					return false;
				if (! this.appearanceType.equals(that.appearanceType))
					return false;
				return true;
			}

			/**
			 * Wichtig, weil wir es als Schlüssel in der Map verwenden Implementiert nach Josh Bloch:
			 * "Effective Java", (http://developer.java.sun.com/developer/Books/effectivejava/Chapter3.pdf)
			 * 
			 * @see java.lang.Object#hashCode()
			 */
			@Override
			public int hashCode() {
				int rv = 17;	// beliebiger Wert
				rv = 37 * rv + hash(botType);	// 37: ungerade Primzahl
				rv = 37 * rv + hash(appearanceType);
				return rv;
			}

			/**
			 * Hashwert des Objekts berechnen
			 * 
			 * @param o	Objekt
			 * @return Hashwert
			 */
			private int hash(Object o) {
				return o == null ? 42 : o.hashCode();
			}
		}

		/**
		 * @param d	Document
		 */
		BotAppearances(QueryableDocument d) {
			try {
				for (QueryableNode botTag : d.getNodeList("/ct-sim/bots/bot")) {
					// Neues Format versuchen
					String className = botTag.getString("@class");
					if ("".equals(className))
						// Altes Format
						className = botTag.getString("@name");
					try {
						// getClass() == null, falls className "default" ist
						loadAppearances(botTag, getClass(className));
					} catch (ClassNotFoundException e) {
						lg.warn("Konfigurationsdatei hat Klasse '%s' " +
							"angefordert, die nicht gefunden werden " +
							"konnte; ignoriere diesen <bot>-Tag in der " +
							"Datei", className);
						continue;
					}
				}
			} catch (XPathExpressionException e) {
				// Kann nur passieren, wenn einer etwas am Code ändert
				throw new AssertionError(e);
			}
		}

		/**
		 * Lädt Appearances
		 * 
		 * @param botTag	Tag
		 * @param botType	Typ
		 * @throws XPathExpressionException
		 */
		private void loadAppearances(QueryableNode botTag, Class<?> botType)
		throws XPathExpressionException {
			for (QueryableNode appNode : botTag.getNodeList("appearance")) {
				add(new AppearanceKey(
					botType,
					appNode.getString("@type")),
					Color.decode(appNode.getString("color")));
			}
		}

		/**
		 * @param classNameFromXml	Klasse als XML
		 * @return Klasse
		 * @throws ClassNotFoundException
		 */
		private static Class<?> getClass(String classNameFromXml)
		throws ClassNotFoundException {
			// 1. Versuch
			if ("default".equals(classNameFromXml))
				return null;	// Default-Wert wird mit null ausgedrückt

			// 2. Versuch
			try { return getClassTolerateNumber(classNameFromXml); }
			catch (ClassNotFoundException e) { /* weitermachen */ }

			// 3. + 4. Versuch: Angegebenes Package weg wenn vorhanden, unsere
			// eigenen Schätzungen versuchen
			String c = Misc.lastOf(classNameFromXml.split("\\."));
			try { return getClassTolerateNumber("ctSim.model.bots."+c); }
			catch (ClassNotFoundException e) { /* weitermachen */ }

			// Wenn das auch nicht geht, Exception rauslassen
			return getClassTolerateNumber("ctSim.model.bots.ctbot."+c);
		}

		// Das alte Format erlaubte z.B. "CtBotSimTcp_3";
		// probieren wir es ohne Unterstrich und Nummer
		
		/**
		 * @param classNameFromXml	Klasse als XML
		 * @return Klasse
		 * @throws ClassNotFoundException
		 */
		private static Class<?> getClassTolerateNumber(String classNameFromXml)
		throws ClassNotFoundException {
			// 1. Versuch: Klassenname exakt wie angegeben
			try { return Class.forName(classNameFromXml); }
			catch (ClassNotFoundException e) { /* weitermachen */ }

			// 2. Versuch: Ohne Anhängsel
			return Class.forName(classNameFromXml.split("_")[0]);	// $$ XML anpassen
		}

		/**
		 * Fügt eine Appearance hinzu
		 * 
		 * @param key	Schlüssel
		 * @param value	Farbe
		 */
		void add(AppearanceKey key, Color value) {
			if (! containsKey(key))
				put(key, new ArrayList<Color>());
			get(key).add(value);
		}

		/**
		 * Modulo, d.h. wenn als Farben Rot, Grün und Hellbordeauxdemousin da sind, kommt raus:
		 * (botType und appearanceType bleiben gleich)
		 * <ul>
		 * <li>Index 0: Rot</li>
		 * <li>Index 1: Grün </li>
		 * <li>Index 2: Hellbordeauxdemousin</li>
		 * <li>Index 3: Rot</li>
		 * <li>Index 4: Grün</li>
		 * <li>usw. </li>
		 * </ul>
		 * @param botType			Bot-Typ
		 * @param appearanceType	Appearance-Typ
		 * @param index				Nummer
		 * @return Farbe
		 */
		Color get(Class<?> botType, String appearanceType, int index) {
			List<Color> cList = get(new AppearanceKey(botType, appearanceType));
			if (cList == null) {
				lg.warn("Konfigdatei: Keine Appearance vom Typ '%s' für " +
					"Bots vom Typ '%s' gefunden; verwende Default",
					appearanceType, botType.getSimpleName());
				cList = get(new AppearanceKey(null, appearanceType));
			}

			if (cList == null) {
				lg.warn("Konfigdatei: Keine Default-Appearance gefunden; " +
					"verwende Fallback");
				return botColorFallback;
			}

			if (index >= cList.size()) {
				lg.warn("Konfigdatei: %d Bots vom Typ '%s' vorhanden, aber " +
					"nur %d Appearances; muss Appearances mehrfach verwenden",
					index + 1, botType.getSimpleName(), cList.size());
			}
			return cList.get(index % cList.size());
		}
	}

	/** Parameter-Typen */
	static class ParameterType {
		/** Name */
		final String name;
		/** Typ */
		final Class<?> type;

		/**
		 * Parameter-Typ
		 * 
		 * @param name	Name
		 * @param type	Typ
		 */
		public ParameterType(final String name, final Class<?> type) {
			this.name = name;
			this.type = type;
		}
	}
}
