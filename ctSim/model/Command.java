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

package ctSim.model;

import java.io.IOException;
import java.net.ProtocolException;

import ctSim.Connection;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.MousePictureComponent;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.ctbot.CtBot;
import ctSim.model.bots.ctbot.CtBotSim;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * <p>
 * Hauptteil des Protokolls zwischen c't-Sim und Bot-Steuercode (sog.
 * <em>c't-Bot-Protokoll</em>). Eingesetzt wird diese Klasse vorwiegend in
 * zwei F&auml;llen:
 * <ol>
 * <li>Ein realer (in Hardware existierender) Bot, der per USB oder TCP
 * verbunden ist, sendet laufend Messwerte der Sensoren und andere
 * Statusinformationen. 11 Byte auf dem Draht stellen die Messwerte eines
 * Sensorpaars dar, z.B. linker und rechter Distanzsensor; die Aufgabe dieser
 * Klasse ist das Lesen und Repr&auml;sentieren dieser 11 Byte innerhalb des
 * Sim.</li>
 * <li>Ein simulierter Bot (Bot-Steuercode, auf einem PC l&auml;uft) bekommt
 * &uuml;ber seine TCP-Verbindung vom Sim Sensorwerte gef&uuml;ttert. Der Sim
 * speichert die Werte in einem Command-Objekt, das er anschlie&szlig;end ins
 * TCP schreibt.</li>
 * </ul>
 * </li>
 * </ol>
 * </p>
 * <p>
 * <strong>Beispiel</strong> eines Command:
 * 
 * <pre>
 * Byte#         Wert            Bedeutung
 * im TCP
 *   0           '&gt;' (Ascii 62)  Startcode, markiert Beginn des Command, ist
 *                               immer '&gt;'
 *   1           'H' (Ascii 72)  Command-Code, hier der f&uuml;r die
 *                               Helligkeitssensoren
 *   2 Bit 0     0               Richtungsangabe, 0 = Anfrage, 1 = Antwort $$$ direction
 *   2 Bits 1-7  'N' (Ascii 78)  Sub-Command-Code: Nur von einigen Command-Codes
 *                               verwendet, ist normalerweise N (&quot;normal&quot;)
 *   3           0               L&auml;nge der Nutzlast in Byte, hier: keine
 *                               Nutzlast
 *   4           42              Datenfeld &quot;dataL&quot; LSB: niederwertiges Byte des
 *                               Messwerts des linken Helligkeitssensors
 *   5           1               dataL MSB: h&ouml;herwertiges Byte des Messwerts
 *   6           37              dataR LSB: niederwertiges Byte rechter
 *                               Helligkeitssensor
 *   7           0               dataR MSB: zugeh&ouml;riges h&ouml;herwertiges Byte
 *   8           61              Sequenznummer LSB, bei aufeinanderfolgenden
 *                               Commands erh&ouml;ht sich die Sequenznummer immer
 *                               um eins
 *   9           17              Sequenznummer MSB
 *  10           '&lt;' (Ascii 60)  CRC-Code, markiert Command-Ende, ist immer '&lt;'
 *                               (Name &quot;CRC&quot; irref&uuml;hrend)
 *  11 und folgende              Nutzlast falls vorhanden. Wird z.B. verwendet,
 *                               wenn der Bot den Inhalt des LCD &uuml;bertr&auml;gt oder
 *                               die Bilddaten, was der Maussensor sieht
 * </pre>
 * 
 * </p>
 * <p>
 * Im <strong>Fall 1</strong> (realer Bot) besteht das Protokoll aus folgenden
 * Regeln:
 * <ul>
 * <li>Der Steuercode sendet laufend Commands mit Sensor-Messwerten und anderen
 * Statusinformationen, die der Sim auswertet und dem Benutzer anzeigt. Welche
 * einzelnen Commands behandelt werden, und wie sie im Detail interpretiert
 * werden, ist in den Bot-Komponenten beschrieben, die das Interpretieren
 * vornehmen. Siehe die Abschnitte <strong>c't-Bot-Protokoll</strong> in den
 * Klassen {@link Actuators.Led}, {@link Sensors.RemoteControl}
 * $$ doc-c't-Bot-Protokoll </li>
 * <li>Beim Start des Sim &uuml;bertr&auml;gt er ein Command mit dem
 * Command-Code {@link Code#WELCOME WELCOME}, das einen Handshake anfordert.
 * (Zu diesem Zeitpunkt kann der Bot schon laufen, d.h. f&uuml;r den Bot kann
 * ein Handshake jederzeit angefordert werden.) Der Bot antwortet mit einem
 * Command, das ihn als realen Bot ausweist (Command-Code WELCOME,
 * Sub-Command-Code {@link SubCode#WELCOME_REAL WELCOME_REAL})</li>
 * <li>Jederzeit w&auml;hrend der Verbindung kann der Sim ein Command senden,
 * das das Bild anfordert, das der Maussensor auf der Botunterseite sieht
 * (Command-Code {@link Code#SENS_MOUSE_PICTURE SENS_MOUSE_PICTURE}). Der Bot
 * beantwortet das mit einer Serie von Commands mit dem Aufbau, der in
 * {@link MousePictureComponent} beschrieben ist. </li>
 * </ul>
 * </p>
 * <p>
 * Im <strong>Fall 2</strong> (simulierter Bot) ist das Protokoll:
 * <ul>
 * <li>$$ doc</li>
 * <li></li>
 * </ul>
 * </p>
 * <p>
 * "Network order&quot;, also die Endianness auf dem Draht, ist <a
 * href="http://de.wikipedia.org/wiki/Big_endian">Big-Endian</a>. Java
 * verwendet intern Little-Endian &ndash; Konvertierung erfolgt zu Fu&szlig; in
 * dieser Klasse.
 * </p>
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
//$$ mehr doc
public class Command {
	final static FmtLogger lg = FmtLogger.getLogger("ctSim.model.Command");

	/** Verschiedene Konstanten **********************************************/

	/** L&auml;nge eines Kommandos in Byte */
	public static final int COMMAND_SIZE = 11;

	/** Markiert den Beginn eines Kommandos */
	public static final int STARTCODE = '>';

	/** Markiert das Ende des Kommandos */
	public static final int CRCCODE = '<';

	/** Richtung Anfrage */
	public static final int DIR_REQUEST = 0;

	/** Richtung Antwort */
	public static final int DIR_ANSWER = 1;

	public static enum Code {
		/** Kommando zum Hallo-Sagen (Handshake)  */
		WELCOME('W'),

		/**
		 * Abschluss eines Packs (nur beim Senden an Sim-Bot, Real-Bot schickt
		 * uns das nicht)
		 */
		DONE('X'),

		//$$$ Verwirrend: akt-servo, akt-door, sens-door. Was ist der Unterschied?
		/**
		 * Steuerung Servo, der die Klappe vorm Transportfach
		 * &ouml;ffnet/schlie&szlig;t
		 */
		ACT_SERVO('S'),

		/**
		 * &Uuml;berwachung Klappe: Sagt wenn Klappe vorm Transportfach zu.
		 */
		SENS_DOOR('D'),

		/**
		 * Nicht verwendet, weder im ctSim noch im C-Code (wird dort als
		 * "Steuerung Klappe" beschrieben, aber die Bedeutung hat ja schon
		 * ACT_SERVO)
		 */
		@Deprecated
		ACT_DOOR('d'),

		/** LEDs */
		ACT_LED('l'),

		/** Motorgeschwindigkeit */
		ACT_MOT('M'),

		/** LCD-Anzeige */
		ACT_LCD('c'),

		/** Abgrundsensoren */
		SENS_BORDER('B'),

		/** Radencoder */
		SENS_ENC('E'),

		/** Motor- oder Batteriefehler */
		SENS_ERROR('e'),

		/** Abstandssensoren */
		SENS_IR('I'),

		/** Helligkeitssensoren */
		SENS_LDR('H'),

		/** Liniensensoren */
		SENS_LINE('L'),

		/** Maussensor */
		SENS_MOUSE('m'),

		/** IR-Fernbedienung */
		SENS_RC5('R'),

		/** &Uuml;berwachung Transportfach */
		SENS_TRANS('T'),

		/** &Uuml;bertragung eines Bildes vom Maussensor */
		SENS_MOUSE_PICTURE('P'),

		/** Logausgaben */
		LOG('O');

		private final byte onTheWire;

		Code(char c) {
			if (c > 127)
				throw new AssertionError();
			onTheWire = (byte)c;
		}

		protected byte toUint7() { return onTheWire; }

		//$$ Code-Duplikation enum Code <-> enum SubCode
		/** Akzeptiert ints aus Toleranz. */
		public static Code fromByte(int b) throws ProtocolException {
			for (Code c : Code.values()) {
				if (c.toUint7() == b)
					return c;
			}
			throw new ProtocolException("Command-Code "+formatChar(b)+
				" unbekannt");
		}
	}

	public static enum SubCode {
		/** Das Standard-Subkommando */
		NORM('N'),

		/** "Nur links". Wird nirgends verwendet, keine Ahnung was das ist. */
		LEFT('L'),

		/** "Nur rechts". Wird nirgends verwendet, keine Ahnung was das ist. */
		RIGHT('R'),

		/** Subkommando Clear Screen */
		LCD_CLEAR('c'),

		/** Subkommando Text ohne Cursor */
		LCD_DATA('D'),

		/** Subkommando Cursorkoordinaten */
		LCD_CURSOR('C'),

		/**
		 * Subkommando f&uuml;r Handshake mit "real bot"
		 *
		 * @see CtBot
		 * @see CtBotReal
		 */
		WELCOME_REAL('R'),

		/**
		 * Subkommando f&uuml;r Handshake mit "simbot"
		 *
		 * @see CtBot
		 * @see CtBotSim
		 */
		WELCOME_SIM('S');

		private final byte onTheWire;

		SubCode(char c) {
			if (c > 127)
				throw new AssertionError();
			onTheWire = (byte)c;
		}

		protected byte toUint7() { return onTheWire; }

		/** Akzeptiert ints aus Toleranz. */
		public static SubCode fromByte(int b) throws ProtocolException {
			for (SubCode c : SubCode.values()) {
				if (c.toUint7() == b)
					return c;
			}
			throw new ProtocolException("Sub-Command-Code "+formatChar(b)+
				" unbekannt");
		}
	}

	/** Instanzvariablen *****************************************************/

	/** Kommandocode */
	private final Code commandCode;

	/** Subkommandocode */
	private final SubCode subCommandCode;

	/** 0 bedeutet Anfrage, 1 bedeutet Antwort. */
	private final int direction;

	/** Daten, die dem Kommando folgen */
	private final byte[] payload;

	/** Daten zum Kommando links */
	private int dataL = 0;

	/** Daten zum Kommando rechts */
	private int dataR = 0;

	/** Paketsequenznummer */
	private int seq = 0;

	/** Markiert das Ende des Kommandos */
	private final int crc;

	/**
	 * Erzeugt ein Kommando zum Abschicken (direction=DIR_REQUEST)
	 */
	public Command(Code code) {
		this.commandCode = code;
		this.subCommandCode = SubCode.NORM;
		this.direction = DIR_REQUEST;
		this.crc = CRCCODE;
		this.payload = new byte[0];
	}

	/**
	 * Liest ein Kommando von einer Connection. Sie wird dabei weitergesetzt um
	 * die Zahl Bytes, die das Kommando lang war.
	 *
	 * @throws IOException Bei E/A-Fehler w&auml;hrend dem Lesen
	 * @throws ProtocolException Falls ein ung&uuml;ltiger CRC empfangen wird oder die Direction ungleich $$ ist
	 */
	public Command(Connection con) throws IOException, ProtocolException {
		byte[] b;

		// Startcode
		b = new byte[1];
		con.read(b);
		byte startCode = b[0];
		if (startCode != STARTCODE) {
			lg.warn("Unerwartetes Zeichen als Startcode; Synchronisierung " +
				"verloren; synchronisiere neu");
			while (startCode != STARTCODE) {
				b = new byte[1];
				con.read(b);
				startCode = b[0];
			}
			lg.fine("Synchronisierung erfolgreich; beginne, Kommandos zu " +
				"interpretieren");
		}

		// Rest des Kommandos
		b = new byte[COMMAND_SIZE - 1]; // -1: Startcode haben wir schon
		con.read(b);

		commandCode = Code.fromByte(b[0]);
		// Nur 7 least significant bits
		subCommandCode = SubCode.fromByte(b[1] & 127);
		// 7 least significant Bits weg, nur 8. angucken
		direction = b[1] >> 7 & 1;

		// Sinnvollitaet pruefen
		if (direction != DIR_REQUEST) {
			throw new ProtocolException(String.format("Ung\u00FCltiges " +
				"Kommando (verkehrtes Richtungsbit, h\u00E4tte %d sein " +
				"sollen); Kommando folgt%s", direction, this));
		}

		int payloadSize = Misc.toUnsignedInt8(b[2]);
		// Shorts (je 2 Byte): Hier Konvertierung Big-Endian -> Little-Endian
		//LODO Bin nicht ueberzeugt, dass das richtig ist. Vorzeichen korrekt? Laut Ben ist das nach Trial and Error entstanden ... "funktioniert, aber keiner weiss warum"
		dataL = (short) ( ( b[ 4 ] & 0xff ) << 8 | ( b[ 3 ] & 0xff ) );
		dataR = (short) ( ( b[ 6 ] & 0xff ) << 8 | ( b[ 5 ] & 0xff ) );
		seq   = (short) ( ( b[ 8 ] & 0xff ) << 8 | ( b[ 7 ] & 0xff ) );
		crc = b[9];

		// Sinnvollitaet pruefen
		if (crc != CRCCODE) {
			throw new ProtocolException(String.format("Ung\u00FCltiges " +
				"Kommando (verkehrter CRC, h\u00E4tte %s sein " +
				"sollen); Kommando folgt%s", formatChar(CRCCODE), this));
		}

		// Nutzlast (Payload)
		payload = new byte[payloadSize];
		con.read(payload);
	}

	public boolean has(Code aCommandCode) {
		return getCommandCode() == aCommandCode;
	}

	public boolean has(SubCode subCommand) {
		return getSubCode() == subCommand;
	}

	/**
	 * Liefert das Kommando als Array von Bytes zurueck
	 *
	 * @return byte[], in dem jeweils ein byte steht
	 */
	public byte[] getCommandBytes() {
		byte data[] = new byte[COMMAND_SIZE + payload.length];

		int i = 0;
		data[i++] = STARTCODE;
		data[i++] = commandCode.toUint7();
		data[i] =   subCommandCode.toUint7();
		data[i++]+= (byte)(direction << 7);

		data[i++] = (byte)(payload.length);
		// Konvertierung Big-Endian -> Little-Endian
		data[i++] = (byte)(dataL & 255);
		data[i++] = (byte)(dataL >> 8);
		data[i++] = (byte)(dataR & 255);
		data[i++] = (byte)(dataR >> 8);
		data[i++] = (byte)(seq & 255);
		data[i++] = (byte)(seq >> 8);
		data[i++] = CRCCODE;

		System.arraycopy(payload, 0, data, i, payload.length);

		return data;
	}

	static String formatChar(int aChar) {
		if (aChar > 32 && aChar <= 255)
			return String.format("'%c' (%d)", aChar, aChar);
		else
			return String.format("(%d)", aChar);
	}

	/** Liefert eine vollst&auml;ndige Stringrepr&auml;sentation des Command */
	@Override
	public String toString() {
		// Vorsicht beim Ausgeben; wenn man lustig %c macht und der Wert wegen
		// Synch- oder Lesefehlern mal < 0 wird, explodiert alles. Daher
		// formatChar() eingefuehrt
		return
			String.format("\n\tCommand-Code:\t%s\n\tSubcommand-Code:\t%s\n",
				commandCode, subCommandCode) +
			String.format("\tDirection:\t%d\n\tData:\tL %d / R %d\n" +
				"\tPayload:\t%d Byte\n", direction, dataL, dataR,
				payload.length) +
			String.format("\tSeq:\t%d\n\tPayload:\t\"%s\" (%d)\n" +
				"\tCRC:\t%s",
				seq, getPayloadAsString(), payload.length, formatChar(crc));
	}

	/** Liefert eine kompakte Stringrepr&auml;sentation des Command (1 Zeile) */
	public String toCompactString() {
		return
			String.format("%-20s",
				commandCode + (has(SubCode.NORM) ? "" : "/"+subCommandCode))+
			String.format(" L %4d R %4d", dataL, dataR)+
			String.format(" Payload='%s'", getPayloadAsString().
				replaceAll("\n", "\\\\n"));
	}

	//$$ Kann bald weg
	/** @return Gibt Kommando zurueck */
	public Code getCommandCode() { return commandCode; }

	/** @return Gib die angehaengten Nutzdaten weiter */
	public byte[] getPayload() { return payload; }

	/**
	 * @return Gibt die Nutzdaten als String zur&uuml;ck, unter Verwendung des
	 * Standard-Charset &ndash; siehe {@link String#String(byte[])}.
	 */
	public String getPayloadAsString() { return new String(payload); }

	/** @return Gibt die Daten zum Kommando links zurueck */
	public int getDataL() { return dataL; }

	/** @return Gibt die Daten zum Kommando rechts zurueck */
	public int getDataR() { return dataR; }

	/** @return Gibt die Richtung zurueck */
	public int getDirection() { return direction; }

	/** @return Liefert die Command-Sequenznummer */
	public int getSeq() { return seq; }

	//$$ Kann bald weg
	/** @return Gibt das Subkommando zurueck */
	public SubCode getSubCode() { return subCommandCode; }

	/** @param dataL Datum f&uuml;r links, das gesetzt werden soll */
	public void setDataL(int dataL) { this.dataL = dataL; }

	/** @param dataR Datum f&uuml;r rechts, das gesetzt werden soll */
	public void setDataR(int dataR) { this.dataR = dataR; }

	/** @param seq Paketsequenznummer, die gesetzt werden soll */
	public void setSeq(int seq) { this.seq = seq; }
}
