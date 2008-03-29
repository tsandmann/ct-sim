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
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.components.MousePictureComponent;
import ctSim.model.bots.components.Actuators.DoorServo;
import ctSim.model.bots.components.Actuators.Governor;
import ctSim.model.bots.components.Actuators.LcDisplay;
import ctSim.model.bots.components.Actuators.Led;
import ctSim.model.bots.components.Actuators.Log;
import ctSim.model.bots.components.Sensors.Border;
import ctSim.model.bots.components.Sensors.Distance;
import ctSim.model.bots.components.Sensors.Door;
import ctSim.model.bots.components.Sensors.Encoder;
import ctSim.model.bots.components.Sensors.Light;
import ctSim.model.bots.components.Sensors.Line;
import ctSim.model.bots.components.Sensors.Mouse;
import ctSim.model.bots.components.Sensors.RemoteControl;
import ctSim.model.bots.components.Sensors.Trans;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.BotID;
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
 * </ol>
 * </p>
 * <p>
 * Diese Klasse behandelt das Dekodieren eines Commands aus einem Haufen Bytes
 * (siehe {@linkplain #Command(Connection) Konstruktor}) und das Enkodieren
 * eines Commands (siehe {@link #getCommandBytes()}). Verwendet und
 * interpretiert werden die Commands in den Bot-Komponenten. Die betreffenden
 * Komponenten stehen in der {@linkplain Code Liste der Command-Codes}.
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
 *   2 Bit 0     0               Richtungsangabe, 0 = Anfrage, 1 = Antwort. Ist
 *                               historisch und steht immer auf 0.
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
 *   9          0               Absender-Id des Paketes
 *  10          0               Empaenger-Id des Paketes
 *  11           '&lt;' (Ascii 60)  CRC-Code, markiert Command-Ende, ist immer '&lt;'
 *                               (Name &quot;CRC&quot; irref&uuml;hrend)
 *  12 und folgende              Nutzlast falls vorhanden. Wird z.B. verwendet,
 *                               wenn der Bot den Inhalt des LCD &uuml;bertr&auml;gt oder
 *                               die Bilddaten, was der Maussensor sieht
 * </pre>
 *
 * </p>
 * <p>
 * F&uuml;r einen <strong>realen Bot</strong> besteht das Protokoll aus
 * folgenden Regeln:
 * <ul>
 * <li>Der Bot-Steuercode sendet laufend Commands mit Sensor-Messwerten und
 * anderen Statusinformationen, die der Sim auswertet und dem Benutzer anzeigt.
 * Welche einzelnen Commands behandelt werden, und wie sie im Detail
 * interpretiert werden, ist Sache der Bot-Komponenten wie in der
 * {@linkplain Code Command-Code-Liste} beschrieben. </li>
 * <li>Beim Start des Sim &uuml;bertr&auml;gt er ein Command mit dem
 * Command-Code {@link Code#WELCOME WELCOME}, das einen Handshake anfordert.
 * Der Bot antwortet mit einem Command, das ihn als realen Bot ausweist
 * (Command-Code WELCOME, Sub-Command-Code
 * {@link SubCode#WELCOME_REAL WELCOME_REAL}). Falls der Bot schon l&auml;uft,
 * wenn der Sim die Verbindung aufbaut, sendet der Sim trotzdem ein
 * {@code WELCOME}; bei USB-Verbindungen (COM-Verbindungen) schickt der Sim
 * kein {@code WELCOME}, da von vornherein klar ist, dass es ein Real-Bot sein
 * muss. Aus Sicht des Bot kann ein Handshake also jederzeit kommen oder
 * &uuml;berhaupt nie.</li>
 * <li>Jederzeit w&auml;hrend der Verbindung kann der Sim ein Command senden,
 * das das Bild anfordert, das der Maussensor auf der Botunterseite sieht
 * (Command-Code {@link Code#SENS_MOUSE_PICTURE SENS_MOUSE_PICTURE}). Der Bot
 * beantwortet das mit einer Serie von Commands mit dem Aufbau, der in
 * {@link MousePictureComponent} beschrieben ist</li>
 * <li>Jederzeit w&auml;hrend der Verbindung kann der Sim ein Command senden,
 * das einen Befehl der RC5-Fernbedienung repr&auml;sentiert (Command-Code
 * {@link Code#SENS_RC5 SENS_RC5})</li>
 * </ul>
 * </p>
 * <p>
 * F&uuml;r einen <strong>simulierten Bot</strong> ist das Protokoll:
 * <ul>
 * <li>Der Sim lauscht auf dem TCP-Port, der in der Konfigdatei angegeben ist
 * (Parameter "botport").</li>
 * <li>Bot-Steuercode verbindet sich mit dem TCP-Port. Der Sim sendet ein
 * Command mit dem {@link Code#WELCOME WELCOME}, das einen Handshake anfordert.
 * Der Bot antwortet mit einem Command, das ihn als simulierten Bot ausweist
 * (Command-Code WELCOME, Sub-Command-Code
 * {@link SubCode#WELCOME_SIM WELCOME_SIM}).</li>
 * <li>Der Sim sendet einen Block von Commands, die Sensorwerte beschreiben. Er
 * ist abgeschlossen mit einem Command, was den Command-Code
 * {@link Code#DONE DONE} hat. Beispiel:
 *
 * <pre>
 * Richtung  Command-Code         dataL dataR
 *
 * Sim->Bot: SENS_MOUSE           L   0 R   0 Payload=''
 * Sim->Bot: SENS_DOOR            L   0 R   0 Payload=''
 * Sim->Bot: SENS_BORDER          L 690 R 690 Payload=''
 * Sim->Bot: SENS_LDR             L 965 R 965 Payload=''
 * Sim->Bot: SENS_IR              L 100 R  80 Payload=''
 * Sim->Bot: SENS_ERROR           L   0 R   0 Payload=''
 * Sim->Bot: SENS_RC5             L   0 R   0 Payload=''
 * Sim->Bot: SENS_TRANS           L   0 R   0 Payload=''
 * Sim->Bot: SENS_LINE            L 690 R 690 Payload=''
 * Sim->Bot: SENS_ENC             L   0 R   0 Payload=''
 * Sim->Bot: DONE                 L  30 R   0 Payload=''</pre>
 *
 * Wenn ein Bot andere Sensoren h&auml;tte als der normale c't-Bot, s&auml;he
 * die Liste anders aus. Manche Sensoren senden nur, wenn sie etwas zu senden
 * haben (Beispiel f&uuml;r dieses Verhalten: SENS_MOUSE_PICTURE, also die
 * {@link MousePictureComponent}). Das DONE-Command enth&auml;lt die aktuelle
 * Simulatorzeit in Millisekunden, falls der Bot zeitabh&auml;ngige Sachen
 * rechnen will. </li>
 * <li>Der Bot berechnet auf Basis der simulierten Sensorwerte seine
 * n&auml;chsten Aktionen. Dann sendet er einen Block von Commands mit
 * Aktuatorwerten. Er ist abgeschlossen mit einem Command, was den Command-Code
 * {@link Code#DONE DONE} hat. Beispiel:
 *
 * <pre>
 * Richtung  CmdCode/SubCmdCode   dataL dataR
 *
 * Bot->Sim: ACT_LED              L 128 R 128 Payload=''
 * Bot->Sim: ACT_MOT              L -24 R  24 Payload=''
 * Bot->Sim: ACT_LCD/LCD_CURSOR   L   0 R   0 Payload=''
 * Bot->Sim: ACT_LCD/LCD_DATA     L   0 R   0 Payload='P=3C5 3C5 D=999 999 '
 * Bot->Sim: ACT_LCD/LCD_CURSOR   L   0 R   1 Payload=''
 * Bot->Sim: ACT_LCD/LCD_DATA     L   0 R   0 Payload='B=2B2 2B2 L=2B2 2B2 '
 * Bot->Sim: ACT_LCD/LCD_CURSOR   L   0 R   2 Payload=''
 * Bot->Sim: ACT_LCD/LCD_DATA     L   0 R   0 Payload='R= 0  0 F=0 K=0 T=0 '
 * Bot->Sim: ACT_LCD/LCD_CURSOR   L   0 R   3 Payload=''
 * Bot->Sim: ACT_LCD/LCD_DATA     L   0 R   0 Payload='I=1005 M=00000 00000'
 * Bot->Sim: DONE                 L  30 R   0 Payload=''</pre>
 *
 * Das DONE-Command enth&auml;lt zur Best&auml;tigung die Simulatorzeit, die vom
 * c't-Sim zuletzt empfangen wurde. </li>
 * </li>
 * Falls der Sim vom Benutzer pausiert wird, kann beliebig viel Armbanduhrenzeit
 * zwischen einem Block und dem n&auml;chsten liegen. Die Simulatorzeit
 * (DONE-Command) wird davon jedoch nicht beeinflusst. Bot-Steuercode sollte
 * sich also auf die Simulatorzeit verlassen, nicht auf Armbanduhrenzeit.
 * <li>
 * </ul>
 * </p>
 * <p>
 * Die Endianness auf dem Draht bei uns ist <a
 * href="http://de.wikipedia.org/wiki/Little_endian">Little-Endian</a>. Java
 * verwendet intern Big-Endian. Die Konvertierung erfolgt zu Fu&szlig; in dieser
 * Klasse.
 * </p>
 *
 * @author Benjamin Benz (bbe@heise.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class Command {
	/** Logger */
	final static FmtLogger lg = FmtLogger.getLogger("ctSim.model.Command");

	/** L&auml;nge eines Kommandos in Byte */
	public static final int COMMAND_SIZE = 12; // Fuer die alte Version ohne Sender-Ids =11 neu 12

	/** Markiert den Beginn eines Kommandos */
	public static final int STARTCODE = '>';

	/** Markiert das Ende des Kommandos */
	public static final int CRCCODE = '<';

	/** Direction Anfrage. Commands haben immer das hier gesetzt. */
	public static final int DIR_REQUEST = 0;

	/** Direction Antwort. Nicht verwendet. */
	public static final int DIR_ANSWER = 1;

	/** Id des Sims */
	private static final BotID SIM_ID = new BotID(0xFE);
	
	/** Broadcast Adresse */
	private static final BotID BROADCAST_ID = new BotID(0xFF);

	/**
	 * Basisklasse fuer Codes
	 */
	public static interface BotCodes {
		/**
		 * SubCodes werden erzeugt mit dieser Methode (und nur mit dieser).
		 * SubCodes sind mit dem Code-Enum verkoppelt, da vom Code abh&auml;ngt,
		 * ob z.B. ein &quot;R&quot; auf dem Draht f&uuml;r den SubCode
		 * WELCOME_REAL oder f&uuml;r RIGHT steht.
		 * @param b Int
		 * @return SubCude
		 * @throws ProtocolException 
		 */
		public SubCode getSubCode(int b) throws ProtocolException;
		
		/**
		 * @return Liefert das Byte, wie dieser SubCode auf dem Draht (im TCP oder USB)
		 * dargestellt werden soll. Das erste Bit des Byte ist immer 0; daher
		 * wird ein 7 Bit langer unsigned Int zur&uuml;ckgegeben.
		 */
		public byte toUint7();
	}
	
	/**
	 * Klasse fuer Kommandos mit beliebigen (Sub-)Codes.
	 * Wird fuer Bot-2-Bot-Kommunikation benutzt, der Sim
	 * kann somit auch Kommandos weiterleiten, die er selbst
	 * gar nicht kennt. 
	 * Erstellt werden koennen aber weiterhin nur Kommandos,
	 * die fuer Bot-2-Sim gueltig sind (siehe Klasse Command)
	 * @author Timo Sandmmann (mail@timosandmann.de)
	 */
	public static class Bot2BotCode implements BotCodes {
		/** /** Code auf der Leitung */
		private byte onTheWire;
		
		/**
		 * @param code Code des Kommandos als byte 
		 * (wird nicht weiter geprueft)
		 */
		public Bot2BotCode(byte code) {
			onTheWire = code;
		}
		
		/**
		 * SubCodes werden erzeugt mit dieser Methode (und nur mit dieser).
		 * Erzeugt immer SubCode.NORM
		 * @param b Dummy
		 * @return SubCode
		 * @throws ProtocolException 
		 */
		public SubCode getSubCode(int b) throws ProtocolException {
			return SubCode.NORM;
		}
		
		/**
		 * @return Liefert das Byte, wie dieser SubCode auf dem Draht (im TCP oder USB)
		 * dargestellt werden soll. Das erste Bit des Byte ist immer 0; daher
		 * wird ein 7 Bit langer unsigned Int zur&uuml;ckgegeben.
		 */
		public byte toUint7() { 
			return onTheWire; 
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Enum Command-Code
	
	/**
	 * Ein Command-Code kann einen der Werte in diesem Enum haben. Der
	 * Command-Code gibt den Typ des Commands an.
	 */
	public static enum Code implements BotCodes {
		/** Zum Hallo-Sagen (Handshake); siehe {@link Command}. */
		WELCOME('W', SubCode.WELCOME_REAL, SubCode.WELCOME_SIM),

		/** Der Bot fordert eine Adresse an */ 
		ID('A', SubCode.ID_SET, SubCode.ID_REQUEST, SubCode.ID_OFFER),
		
		/**
		 * Abschluss eines Blocks; Nur f&uuml;r simulierte Bots; siehe
		 * {@link Command}
		 */
		DONE('X'),

		/**
		 * Steuerung des Servomotors, der die Klappe vorm Transportfach
		 * &ouml;ffnet/schlie&szlig;t; siehe {@link DoorServo}.
		 */
		ACT_SERVO('S'),

		/**
		 * &Uuml;berwachung Klappe: Sagt wenn Klappe vorm Transportfach zu;
		 * siehe {@link Door}.
		 */
		SENS_DOOR('D'),

		/** LEDs (Leuchtdioden); siehe {@link Led}. */
		ACT_LED('l'),

		/** Motorgeschwindigkeit; siehe {@link Governor}. */
		ACT_MOT('M'),

		/** LCD-Anzeige; siehe {@link LcDisplay}. */
		ACT_LCD('c', SubCode.NORM, SubCode.LCD_CLEAR, SubCode.LCD_CURSOR,
			SubCode.LCD_DATA),

		/** Abgrundsensoren; siehe {@link Border}. */
		SENS_BORDER('B'),

		/** Radencoder; siehe {@link Encoder}. */
		SENS_ENC('E'),

		/**
		 * Motor- oder Batteriefehler, siehe
		 * {@link ctSim.model.bots.components.Sensors.Error}.
		 */
		SENS_ERROR('e'),

		/** Abstandssensoren, siehe {@link Distance}. */
		SENS_IR('I'),

		/** Helligkeitssensoren, siehe {@link Light}. */
		SENS_LDR('H'),

		/** Liniensensoren, siehe {@link Line}. */
		SENS_LINE('L'),

		/** Maussensor, siehe {@link Mouse}. */
		SENS_MOUSE('m'),

		/** Fernbedienung, siehe {@link RemoteControl}. */
		SENS_RC5('R'),

		/** &Uuml;berwachung Transportfach, siehe {@link Trans}. */
		SENS_TRANS('T'),

		/**
		 * &Uuml;bertragung eines Bildes vom Maussensor, siehe
		 * {@link MousePictureComponent}.
		 */
		SENS_MOUSE_PICTURE('P'),

		/** Logausgaben, siehe {@link Log}. */
		LOG('O'),

		/**
		 * F&uuml;r Remote-Calls, d.h. wenn der Sim ein Bot-Behavior aufruft.
		 * Keiner wei&szlig;, wof&uuml;r NORM steht, aber der Bot schickt das
		 * &ouml;fters mal. Um die Warnungen des Sim ("Ung&uuml;ltiges Kommando,
		 * SubCode NORM passt nicht zu Code REMOTE_CALL") zu unterdr&uuml;cken,
		 * f&uuml;ge ich das hier dazu.
		 */
		REMOTE_CALL('r', SubCode.NORM, SubCode.REMOTE_CALL_LIST,
			SubCode.REMOTE_CALL_ENTRY, SubCode.REMOTE_CALL_ORDER,
			SubCode.REMOTE_CALL_DONE, SubCode.REMOTE_CALL_ABORT,
			SubCode.REMOTE_CALL_ABL);


		/** Code auf der Leitung */
		private final byte onTheWire;
		/** gueltige SubCodes */
		private final SubCode[] validSubCodes;

		/**
		 * Konschtruktor; nicht aufrufbar; stattdessen {@link #fromByte(int)}
		 * verwenden. Setzt die zugelassenen SubCodes f&uuml;r diese
		 * Code-Instanz auf NORM und nichts sonst.
		 * @param c Code
		 */
		private Code(char c) {
			this(c, SubCode.NORM);
		}

		/**
		 * Erzeugt eine Enum-Instanz mit dem Array an zul&auml;ssigen SubCodes.
		 * Die Code-Instanz kennt ihre m&ouml;glichen SubCodes, da etwa ein
		 * SubCode &quot;L&quot; nach Code ACT_LCD was anderes hei&szlig;t als
		 * nach Code REMOTE_CALL. Das muss unterschieden werden k&ouml;nnen.
		 * @param c Subcode
		 * @param validSubCodes Subcode-Instanz
		 */
		private Code(char c, SubCode... validSubCodes) {
			if (c > 127)
				throw new AssertionError();
			onTheWire = (byte)c;
			this.validSubCodes = validSubCodes;
		}

		/**
		 * @return Liefert das Byte, wie dieser SubCode auf dem Draht (im TCP oder USB)
		 * dargestellt werden soll. Das erste Bit des Byte ist immer 0; daher
		 * wird ein 7 Bit langer unsigned Int zur&uuml;ckgegeben.
		 */
		public byte toUint7() { return onTheWire; }

		/**
		 * Erzeugt eine SubCode-Instanz. Akzeptiert ints aus Toleranz.
		 * @param b Byte
		 * @return SubCode
		 *
		 * @throws ProtocolException falls der Ascii-Wert von {@code b} keiner
		 * der Werte dieses Enums ist
		 */
		public static Code fromByte(int b) throws ProtocolException {
			for (Code c : Code.values()) {
				if (c.toUint7() == b)
					return c;
			}
			throw new ProtocolException("Command-Code "+formatChar(b)+
				" unbekannt");
		}

		/**
		 * SubCodes werden erzeugt mit dieser Methode (und nur mit dieser).
		 * SubCodes sind mit dem Code-Enum verkoppelt, da vom Code abh&auml;ngt,
		 * ob z.B. ein &quot;R&quot; auf dem Draht f&uuml;r den SubCode
		 * WELCOME_REAL oder f&uuml;r RIGHT steht.
		 * @param b Int
		 * @return SubCude
		 * @throws ProtocolException 
		 */
		public SubCode getSubCode(int b) throws ProtocolException {
			for (SubCode c : validSubCodes) {
				if (c.toUint7() == b)
					return c;
			}
			throw new ProtocolException("Sub-Command-Code "+formatChar(b)+
				" nicht vorgesehen f\u00FCr Command-Code "+
				formatChar(toUint7()));
		}

		/**
		 * @param sc Subcode
		 * @throws ProtocolException
		 */
		public void assertSubCodeValid(SubCode sc) throws ProtocolException {
			// Wenn SubCode ungueltig, explodiert der folgende Aufruf mit einer
			// ProtoExcp
			getSubCode(sc.toUint7());
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// Enum Sub-Command-Code

	/** Ein Sub-Command-Code kann einen der Werte in diesem Enum haben. */
	public static enum SubCode {
		/**
		 * Das Standard-Subkommando. Dieses ist gesetzt, wenn kein anderes
		 * gesetzt ist.
		 */
		NORM('N'),

		/** "Nur links". Wird nirgends verwendet, keine Ahnung was das ist. */
		LEFT('L'),

		/** "Nur rechts". Wird nirgends verwendet, keine Ahnung was das ist. */
		RIGHT('R'),

		/** F&uuml;r das LCD; {@linkplain LcDisplay siehe dort}. */
		LCD_CLEAR('c'),

		/** F&uuml;r das LCD; {@linkplain LcDisplay siehe dort}. */
		LCD_DATA('D'),

		/** F&uuml;r das LCD; {@linkplain LcDisplay siehe dort}. */
		LCD_CURSOR('C'),

		/**
		 * Subkommando f&uuml;r Handshake mit "Real-Bot", siehe
		 * {@linkplain Command}.
		 *
		 * @see RealCtBot
		 */
		WELCOME_REAL('R'),

		/**
		 * Subkommando f&uuml;r Handshake mit "Sim-Bot", siehe
		 * {@linkplain Command}.
		 *
		 * @see SimulatedBot
		 * @see CtBotSimTcp
		 * @see CtBotSimTest
		 */
		WELCOME_SIM('S'),

		/**
		 * Fordert den Bot auf, alle verf&uuml;gbaren Kommandos (Behaviors)
		 * aufzulisten
		 */
		REMOTE_CALL_LIST('L'),

		/**
		 * Ein Eintrag in der Auflistung der verf&uuml;gbaren Kommandos
		 * (Behaviors)
		 */
		REMOTE_CALL_ENTRY('E'),

		/** Sim gibt damit beim Bot einen Remote-Call in Auftrag */
		REMOTE_CALL_ORDER('O'),

		/**
		 * Signalisiert dem Sim, dass ein Remote-Call abgeschlossen wurde.
		 * Ergebnis des Remote-Call steht in dataL (1 = geklappt, 0 = in die
		 * Hose gegangen)
		 */
		REMOTE_CALL_DONE('D'),

		/**
		 * Sendet der Sim an den Bot, wenn der Remote-Call abgebrochen werden
		 * soll, der angefangen, aber noch nicht beendet wurde.
		 */
		REMOTE_CALL_ABORT('A'),
		
		/**
		 * Schickt ein ABL-Programm an den Bot
		 */
		REMOTE_CALL_ABL('I'),

		/**
		 * Setzt die ID
		 */
		ID_SET('S'),
		
		/**
		 * Offeriert eine ID
		 */
		ID_OFFER('O'),
		
		/**
		 * Fordert die ID an
		 */
		ID_REQUEST('R');
		
		
		
		/** SubCode auf der Leitung */
		private final byte onTheWire;

		/**
		 * Konschtruktor; nicht aufrufbar; stattdessen {@link Command.Code#getSubCode(int)}
		 * verwenden.
		 * @param c Subcode
		 */
		private SubCode(char c) {
			if (c > 127)
				throw new AssertionError();
			onTheWire = (byte)c;
		}

		/**
		 * @return Liefert das Byte, wie dieser SubCode auf dem Draht (im TCP oder USB)
		 * dargestellt werden soll. Das erste Bit des Byte ist immer 0; daher
		 * wird ein 7 Bit langer unsigned Int zur&uuml;ckgegeben.
		 */
		protected byte toUint7() { return onTheWire; }
	}

	///////////////////////////////////////////////////////////////////////////
	// Instanzvariablen

	/** Command-Code */
	private final BotCodes commandCode;

	/** Sub-Command-Code */
	private SubCode subCommandCode;

	/** Praktisch nicht verwendet. Kann DIR_REQUEST oder DIR_ANSWER werden. */
	private final int direction;

	/** Daten, die dem Kommando folgen */
	private byte[] payload;

	/** Daten zum Kommando links */
	private int dataL = 0;

	/** Daten zum Kommando rechts */
	private int dataR = 0;

	/** Paketsequenznummer */
	private byte seq = 0;

	/** Absender des Paketes */
	private BotID from = new BotID(SIM_ID);

	/** Empfaenger des Paketes */
	private BotID to = new BotID();

	/** Markiert das Ende des Kommandos */
	private final byte crc;

	/**
	 * Client-Code kann damit markieren, ob das Kommando verarbeitet ist oder
	 * nicht. Gibt einen Getter und Setter daf&uuml;r, sonst hat diese Klasse
	 * damit nichts am Hut.
	 */
	private boolean hasBeenProcessed = false;

	/** 
	 * Erzeugt ein Kommando 
	 * @param code des Kommandos 
	 */
	public Command(Code code) {
		this.commandCode = code;
		this.subCommandCode = SubCode.NORM;
		this.direction = DIR_REQUEST;
		this.crc = CRCCODE;
		this.payload = new byte[0];
	}

	/**
	 * Wie {@link #Command(Connection, boolean)} mit
	 * {@code suppressSyncWarnings == false} (dem Normalwert).
	 * @param con Connection fuer das Kommando
	 * @throws IOException 
	 * @throws ProtocolException 
	 */
	public Command(Connection con) throws IOException, ProtocolException {
		this(con, false);
	}

	/**
	 * <p>
	 * Liest und dekodiert ein Kommando von einer Connection. Sie wird dabei
	 * weitergesetzt um die Zahl Bytes, die das Kommando lang war.
	 * </p>
	 * <p>
	 * Falls das erste gelesene Byte nicht der Startcode ist, wird eine Warnung
	 * ausgegeben ("Synchronisierung verloren") und das Zeichen ignoriert. Im
	 * Normalbetrieb kommt sie nicht vor, da auf einen CRC-Code ohne weiteren
	 * Zwischenkram der Startcode des n&auml;chsten Kommandos folgen muss.
	 * </p>
	 * @param con Connection fuer das Kommando
	 *
	 * @param suppressSyncWarnings Unterdr&uuml;ckt die &quot;Synchronisierung
	 * verloren&quot;-Warnung (s.o.), d.h. gibt sie auf Log-Level {@code FINE}
	 * aus statt auf {@code WARNING}. Das ist sinnvoll f&uuml;r den Handshake
	 * mit einem realen Bot: Wenn der Bot schon Kommandos sendet, wenn der Sim
	 * sich mit ihm verbindet, dann ist es leicht m&ouml;glich, dass der Sim
	 * mitten in einem Kommando dazukommt. In diesem Fall verwirrt die Meldung
	 * mehr, als sie hilft.
	 * @throws IOException Bei E/A-Fehler w&auml;hrend dem Lesen
	 * @throws ProtocolException Falls ein ung&uuml;ltiger CRC empfangen wird
	 * oder die Direction nicht {@link #DIR_REQUEST} ist.
	 */
	public Command(Connection con, boolean suppressSyncWarnings)
	throws IOException, ProtocolException {
		byte[] b;

		// Startcode
		b = new byte[1];
		con.read(b);
		byte startCode = b[0];
		if (startCode != STARTCODE) {
			String msg = "Unerwartetes Zeichen als Startcode; " +
					"Synchronisierung verloren; synchronisiere neu";
			if (suppressSyncWarnings)
				lg.fine(msg);
			else
				lg.warn(msg);

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

		byte i =0;
		byte code = b[i++];
		// Nur 7 least significant bits
		int subcode = b[i] & 127;
		// 7 least significant Bits weg, nur 8. angucken
		direction = b[i++] >> 7 & 1;

		// Sinnvollitaet pruefen
		if (direction != DIR_REQUEST) {
			throw new ProtocolException(String.format("Ung\u00FCltiges " +
				"Kommando (verkehrtes Richtungsbit, h\u00E4tte %d sein " +
				"sollen); Kommando folgt%s", direction, this));
		}

		int payloadSize = Misc.toUnsignedInt8(b[i++]);
		// Shorts (je 2 Byte): Hier Konvertierung Little-Endian -> Big-Endian
		dataL = (short) ( ( b[ i+1 ] & 0xff ) << 8 | ( b[ i ] & 0xff ) );
		i+=2;
		dataR = (short) ( ( b[ i+1 ] & 0xff ) << 8 | ( b[ i ] & 0xff ) );
		i+=2;
		
		seq=b[i++];	// neue Version mit Adressen und kurzer seq
		// alte version seq   = (short) ( ( b[ i+1 ] & 0xff ) << 8 | ( b[ i ] & 0xff ) );	i++;
				
		
		from.set(b[i++]);	// neue Version mit Adressen
		to.set(b[i++]); // neue Version mit Adressen		
		
		if (to.equals(Command.getSimId())) {
			commandCode = Code.fromByte(code);
		} else {
			commandCode = new Bot2BotCode(code);
		}
		subCommandCode = commandCode.getSubCode(subcode);
		
		// und noch die Pruefsumme
		crc = b[i];

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

	/**
	 * {@code true}, falls dieses Kommando den Command-Code
	 * {@code someCommandCode} hat. Andernfalls {@code false}.
	 * @param someCommandCode 
	 * @return true/false
	 */
	public boolean has(Code someCommandCode) {
		return commandCode == someCommandCode;
	}

	/**
	 * {@code true}, falls dieses Kommando den Sub-Command-Code
	 * {@code subCommandCode} hat. Andernfalls {@code false}.
	 * @param subCode 
	 * @return true/false
	 */
	public boolean has(SubCode subCode) {
		return getSubCode() == subCode;
	}

	/**
	 * Enkodiert das Kommando, so dass es &uuml;ber TCP/USB an einen c't-Bot
	 * geschickt werden kann.
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
	//	data[i++] = (byte)(seq >> 8); // alte Version mit 16-Bit seq
		
		// neue Version mit Sender-Ids
		data[i++] = from.byteValue();
		data[i++] = to.byteValue();
		
		data[i++] = CRCCODE;

		System.arraycopy(payload, 0, data, i, payload.length);

		return data;
	}

	/**
	 * Hilfsmethode: Liefert Char und Ascii-Code zu einem {@code char} oder nur
	 * den Ascii-Code, falls es non-printable ist.
	 * @param aChar Der char
	 * @return Char- und ASCII-Code
	 */
	static String formatChar(int aChar) {
		if (aChar > 32 && aChar <= 255)
			return String.format("'%c' (%d)", aChar, aChar);
		else
			return String.format("(%d)", aChar);
	}

	/**
	 * Liefert eine vollst&auml;ndige Stringrepr&auml;sentation des Command
	 * (mehrere Zeilen lang).
	 */
	@Override
	public String toString() {
		String payloadStr;
		if (payload == null)
			payloadStr = "";
		else {
			payloadStr = "'"+replaceCtrlChars(getPayloadAsString())+
				"' ("+payload.length+" Byte)";
		}
		// Vorsicht beim Ausgeben; wenn man lustig %c macht und der Wert wegen
		// Synch- oder Lesefehlern mal < 0 wird, explodiert alles. Daher
		// formatChar() eingefuehrt
		return
			"\n\tCommand-Code:\t"+commandCode+
			"\n\tSubcommand-Code:\t"+subCommandCode+
			"\n\tDirection:\t"+direction+
			"\n\tData:\tL "+dataL+" / R "+dataR+
			"\n\tSeq:\t"+seq+
			"\n\tFrom:\t"+from+
			"\n\tTo:\t"+to+
			"\n\tPayload:\t"+payloadStr+
			"\n\tCRC:\t"+formatChar(crc);
	}

	/** 
	 * Liefert eine kompakte Stringrepr&auml;sentation des Command (1 Zeile) 
	 * @return String 
	 */
	public String toCompactString() {
		return
			String.format("%-20s",
				commandCode + (has(SubCode.NORM) ? "" : "/"+subCommandCode))+
			String.format(" L %4d R %4d", dataL, dataR)+
			String.format(" Payload='%s'",
				replaceCtrlChars(escapeNewlines(getPayloadAsString())));
	}

	/** 
	 * Gibt die angeh&auml;ngten Nutzdaten zur&uuml;ck 
	 * @return Payload als byte-Array 
	 */
	public byte[] getPayload() { return payload; }

	/**
	 * Gibt die Nutzdaten als String zur&uuml;ck, unter Verwendung des
	 * Standard-Charset &ndash; siehe {@link String#String(byte[])}.
	 * @return Payload als String
	 */
	public String getPayloadAsString() {
		return payload == null ? "(null)" : new String(payload);
	}

	/**
	 * Ersetzt jedes Steuerzeichen (Ascii 0 bis inkl. Ascii 31) durch einen
	 * Punkt (.), so dass man den String gefahrlos ausgeben kann.
	 * @param s Input-String
	 * @return Output-String
	 */
	public static String replaceCtrlChars(String s) {
		// dezimal 0 bis 31 = oktal 0 bis 37
		return s.replaceAll("[\000-\037]", ".");
	}

	/**
	 * Wenn man den Payload in einer Zeile anzeigen will: Ersetzt Newlines, so
	 * dass der Benutzer keinen Zeilenwechsel sieht, sondern die zwei Zeichen
	 * "\n".
	 * @param s Input-String
	 * @return Output-String
	 */
	public static String escapeNewlines(String s) {
		return s.replaceAll("\n", "\\\\n");
	}

	/** 
	 * @return Gibt das Datenfeld links ({@code dataL}) zur&uuml;ck 
	 */
	public int getDataL() { return dataL; }

	/** 
	 * @return Gibt das Datenfeld rechts ({@code dataR}) zur&uuml;ck 
	 */
	public int getDataR() { return dataR; }

	/** 
	 * @return Gibt die Richtung zurueck 
	 */
	public int getDirection() { return direction; }

	/** 
	 * @return Liefert die Kommando-Sequenznummer 
	 */
	public byte getSeq() { return seq; }

	/** 
	 * @return Getter zu {@link #setHasBeenProcessed(boolean)} 
	 */
	public boolean hasBeenProcessed() { return hasBeenProcessed; }

	/** 
	 * @return Gibt das Subkommando des Kommandos zur&uuml;ck 
	 */
	public SubCode getSubCode() { return subCommandCode; }

	/** 
	 * Setzt das Feld dataL 
	 * @param dataL 
	 */
	public void setDataL(int dataL) { this.dataL = dataL; }

	/** 
	 * Setzt das Feld dataR 
	 * @param dataR 
	 */
	public void setDataR(int dataR) { this.dataR = dataR; } 
	
	/** 
	 * Setzt die Kommandosequenznummer
	 * @param seq 
	 */
	public void setSeq(byte seq) { this.seq = seq; }

	/**
	 * Setzt den Sub-Command-Code. Nur sinnvoll f&uuml;r Commands, die der Sim
	 * senden wird.
	 * @param sc SubCode
	 */
	public void setSubCmdCode(SubCode sc) {
		try {
			((Code) commandCode).assertSubCodeValid(sc);
		} catch (ProtocolException e) {
			// Das ist ein Fehler des Programmierers, keine Laufzeitsache
			throw new AssertionError(e);
		}
		subCommandCode = sc;
	}

	/**
	 * Setzt die Nutzlast, die an das Command angeh&auml;ngt ist. Nur sinnvoll
	 * f&uuml;r Commands, die der Sim senden wird.
	 * @param payload Payload als byte-Array
	 */
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	/**
	 * Flag, ob dieses Kommando fertig verarbeitet ist. N&uuml;tzlich, wenn ein
	 * Kommando in der Gegend herumgereicht wird und am Schluss festgestellt
	 * werden soll, ob jemand das jetzt interpretiert hat oder nicht.
	 * @param hasBeenProcessed 
	 */
	public void setHasBeenProcessed(boolean hasBeenProcessed) {
		this.hasBeenProcessed = hasBeenProcessed;
	}

	/**
	 * Liest den Absender des Paketes aus
	 * @return Absender-Id
	 */
	public BotID getFrom() {
		return new BotID(from);
	}

	/**
	 * Setzt den Absender
	 * @param from Absender-ID
	 */
	public void setFrom(BotID from) {
		this.from = from;
	}

	/**
	 * Liest die Empfaenger-ID des Paketes aus
	 * @return Empfaenger-ID
	 */
	public BotID getTo() {
		return new BotID(to);
	}

	/** 
	 * Setzt die Empfaenger-ID
	 * @param to Empfaenger-ID
	 */
	public void setTo(BotID to) {
		this.to = to;
	}
	
	/**
	 * @return Bot-ID des Sims
	 */
	public static BotID getSimId() {
		return new BotID(SIM_ID);
	}
	
	/**
	 * @return Bot-ID fuer Broadcast
	 */
	public static BotID getBroadcastId() {
		return new BotID(BROADCAST_ID);
	}
}
