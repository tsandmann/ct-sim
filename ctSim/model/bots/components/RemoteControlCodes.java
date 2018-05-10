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

package ctSim.model.bots.components;

import java.util.Map;

import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Erzeugt die Zuordnung von Tasten auf Fernbedienungscodes je nach Typ der Fernbedienung (einstellbar
 * wie beim Bot-Code über die Datei rc5-codes.h).
 *
 * @author Timo Sandmann
 */
public class RemoteControlCodes {
	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.components.RemoteControlCodes");

	/** Zuordnung Tasten <-> RC5-Codes */
	private final Map<String, Integer> keys = Misc.newMap();

	/**
	 * @param type	Typ der Fernbedienung (wie in rc5-codes.h beim Bot-Code)
	 * @throws Exception	falls der Typ unbekannt
	 */
	public RemoteControlCodes(final String type) throws Exception {
		RC5Codes codeMapping = null;
		/* Standard-Fernbedienung */
		if (type.equals("RC_HAVE_HQ_RC_UNIVERS29_334") || type.equals("")) {
			codeMapping = new HQ_RC_UNIVERS29_334();
		}

		/* RC_HAVE_HAUPPAUGE_WINTV */
		if (type.equals("RC_HAVE_HAUPPAUGE_WINTV")) {
			codeMapping = new HAUPPAUGE_WINTV();
		}

		/* RC_HAVE_Technisat_TTS35AI */
		if (type.equals("RC_HAVE_Technisat_TTS35AI")) {
			codeMapping = new Technisat_TTS35AI();
		}

		if (codeMapping != null) {
			/* Tastenzuordnungen eintragen */
			codeMapping.createCodes();
			lg.config("Fernbedienung vom Typ " + codeMapping.getClass().getSimpleName() + " erzeugt");
		} else {
			/* Typ nicht gefunden */
			throw new Exception("Unbekannter Fernbedienungstyp \"" + type + "\"");
		}
	}

	/**
	 * Gibt die Tastenbelegung der erzeugten Fernbedienung zurück
	 *
	 * @return HashMap mit Tastenzuordnungen
	 */
	public Map<String, Integer> getKeyMap() {
		return keys;
	}

	/**
	 * Fügt eine Taste hinzu und legt ihren RC5-Code in der HashMap ab
	 *
	 * @param label	Name der Taste
	 * @param code	RC5-Code der Taste
	 */
	private void addKey(final String label, Integer code) {
		keys.put(label, code);
	}

	/**
	 * Interface für alle RC5-Fernbedienungen
<<<<<<< bugfixes/format_spelling2
	 *
	 * @author Timo Sandmann (mail@timosandmann.de)
=======
	 * 
	 * @author Timo Sandmann
>>>>>>> 120d6a1 comment in MainWinMenuBar.java restored, author information unified
	 */
	interface RC5Codes {
		/** Erzeugt die Fernbedienungstasten */
		void createCodes();
	}

	/**
	 * Definiert den Fernbedienungstyp der Standardfernbedienung HQ_RC_UNIVERS29_334 und ihre
	 * Tastenzuordnungen.
	 * Möchte man weitere Fernbedienungen ergänzen, erzeugt man weitere Klassen, die RC5Codes
	 * implementieren und genauso aufgebaut sind wie diese. Außerdem trägt man sie ebenfalls in
	 * RemoteControlCodes() ein.
<<<<<<< bugfixes/format_spelling2
	 *
	 * @author Timo Sandmann (mail@timosandmann.de)
=======
	 * 
	 * @author Timo Sandmann
>>>>>>> 120d6a1 comment in MainWinMenuBar.java restored, author information unified
	 */
	class HQ_RC_UNIVERS29_334 implements RC5Codes {
		/**
		 * @see ctSim.model.bots.components.RemoteControlCodes.RC5Codes#createCodes()
		 */
		@Override
		public void createCodes() {
			addKey("\u03A6", 0x118C);	// 03A6: "Power-Symbol" (wie Kombination von O und |)
			addKey("1" , 0x1181);
			addKey("2" , 0x1182);
			addKey("3" , 0x1183);
			addKey("4" , 0x1184);
			addKey("5" , 0x1185);
			addKey("6" , 0x1186);
			addKey("7" , 0x1187);
			addKey("8" , 0x1188);
			addKey("9" , 0x1189);
			addKey("10", 0x1180);
			addKey("11", 0x118A);
			addKey("12", 0x11A3);

			addKey("GR \u2013", 0x01BA);
			addKey("RE +", 0x01BD);
			addKey("YE \u2013", 0x01B1);
			addKey("BL +", 0x01B0);

			addKey("I/II", 0x11AB);
			addKey("TV/VCR", 0x11B8);
			addKey("||", 0x11A9);		// Stop
			addKey("<<", 0x11B2);		// Backward
			addKey(">",  0x11B5);		// Play
			addKey(">>", 0x11B4);		// Forward
			addKey("\u25A1", 0x11B6);	// 25A1: Quadrat für "Stop"
			addKey("\u25CF", 0x11AB);	// 25CF: Dicker Punkt für "Record"

			addKey("CH*P/P", 0x11BF);
			addKey("Vol+", 0x1190);
			addKey("Mute", 0x01BF);
			addKey("Ch+",  0x11A0);
			addKey("Vol\u2013", 0x1191);
			addKey("Ch\u2013",  0x11A1);
		}
	}

	/**
	 * Definiert den Fernbedienungstyp "HAUPPAUGE_WINTV" und ihre Tastenzuordnungen
<<<<<<< bugfixes/format_spelling2
	 *
	 * @author Timo Sandmann (mail@timosandmann.de)
=======
	 * 
	 * @author Timo Sandmann
>>>>>>> 120d6a1 comment in MainWinMenuBar.java restored, author information unified
	 */
	class HAUPPAUGE_WINTV implements RC5Codes {
		/**
		 * @see ctSim.model.bots.components.RemoteControlCodes.RC5Codes#createCodes()
		 */
		@Override
		public void createCodes() {
			addKey("\u03A6", 0x1026);	// 03A6: "Power-Symbol" (wie Kombination von O und |)
			addKey("1" , 0x1001);
			addKey("2" , 0x1002);
			addKey("3" , 0x1003);
			addKey("4" , 0x1004);
			addKey("5" , 0x1005);
			addKey("6" , 0x1006);
			addKey("7" , 0x1007);
			addKey("8" , 0x1008);
			addKey("9" , 0x1009);
			addKey("10", 0x1000);

			addKey("I/II", 0x1022);
			addKey("TV/VCR", 0x102e);
			addKey("||", 0x1020);		// Stop
			addKey("<<", 0x1011);		// Backward
			addKey(">",  0xffff);		// Play
			addKey(">>", 0x1010);		// Forward
			addKey("\u25A1", 0x1021);	// 25A1: Quadrat für "Stop"
		}
	}

	/** Definiert den Fernbedienungstyp Technisat TTS35AI und ihre Tastenzuordnungen */
	class Technisat_TTS35AI implements RC5Codes {
		/**
		 * @see ctSim.model.bots.components.RemoteControlCodes.RC5Codes#createCodes()
		 */
		@Override
		public void createCodes() {
			addKey("\u03A6", 0x128C);	// 03A6: "Power-Symbol" (wie Kombination von O und |)
			addKey("1" , 0x1281);
			addKey("2" , 0x1282);
			addKey("3" , 0x1283);
			addKey("4" , 0x1284);
			addKey("5" , 0x1285);
			addKey("6" , 0x1286);
			addKey("7" , 0x1287);
			addKey("8" , 0x1288);
			addKey("9" , 0x1289);
			addKey("10", 0x1280);
			addKey("11", 0x128A);
			addKey("12", 0x12A3);

			addKey("GR \u2013", 0x02AC);
			addKey("RE +", 0x02AB);
			addKey("YE \u2013", 0x02AD);
			addKey("BL +", 0x02AE);

			addKey("I/II", 0x12A2);
			addKey("TV/VCR", 0x028F);
			addKey("||", 0x12A0);		// Pause
			addKey("<<", 0x1291);		// Backward
			addKey(">",  0x0297);		// Play
			addKey(">>", 0x1290);		// Forward
			addKey("\u25A1", 0x12A1);	// 25A1: Quadrat für "Stop"
			addKey("\u25CF", 0x12AF);	// 25CF: Dicker Punkt für "Record"

			addKey("CH*P/P", 0x0293);
			addKey("Vol+", 0x128F);
			addKey("Mute", 0x128D);
			addKey("Ch+",  0x12A9);
			addKey("Vol\u2013", 0x12BC);
			addKey("Ch\u2013",  0x12B8);
		}
	}
}
