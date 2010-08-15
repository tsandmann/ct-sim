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
package ctSim.model.bots.components;


import java.awt.Color;
import java.io.IOException;
import java.net.ProtocolException;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.Command.SubCode;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.model.bots.components.BotComponent.SimpleActuator;
import ctSim.util.Misc;

/**
 * Klasse, die nur als Container f&uuml;r innere Klassen dient und selber keine
 * Methoden oder Felder hat. (F&uuml;r die winzigen inneren Klassen lohnt sich
 * keine eigene Datei.)
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class Actuators {
	/**
	 * Governor, der die Fahrgeschwindigkeit des Bot regelt (genauer: die
	 * Drehgeschwindigkeit eines Rads; der c't-Bot hat daher einen linken und
	 * einen rechten Governor).
	 */
	public static class Governor extends NumberTwin
	implements SimpleActuator, CanRead {
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override
		protected String getBaseDescription() {
			return "Motorbegrenzer";
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return "Gov"; }
		
		/**
		 * @param isLeft	links?
		 */
		public Governor(boolean isLeft) { super(isLeft); }
		
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.ACT_MOT; }
	}

	/**
	 * Servor fuer Klappe
	 */
	public static class DoorServo extends BotComponent<SpinnerNumberModel>
	implements SimpleActuator, CanRead {
		/** Zahlenwert */
		private Number internalModel = Double.valueOf(0);

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Servomotor f\u00FCr Klappe";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public synchronized void updateExternalModel() {
			getExternalModel().setValue(internalModel);
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
		public synchronized void readFrom(Command c) {
			internalModel = c.getDataL();
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "DoorServo"; }
		
		/**
		 * Servor fuer Klappe
		 */
		public DoorServo() { super(new SpinnerNumberModel()); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.ACT_SERVO; }
	}

	/**
	 * Log eines Bot; hier werden die vom Bot geschickten Log-Ausgaben
	 * (Textzeilen) gesammelt. Diese Bot-Komponente existiert nicht in Hardware.
	 */
	public static class Log extends BotComponent<PlainDocument>
	implements CanRead {
		/** Internes Model */
		private final StringBuffer newStuff = new StringBuffer(); 

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
		public synchronized void readFrom(Command c) {
			newStuff.append(c.getPayloadAsString());
			newStuff.append("\n");
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public synchronized void updateExternalModel() {
			try {
				getExternalModel().insertString(getExternalModel().getLength(),
					newStuff.toString(), null);
				newStuff.delete(0, newStuff.length());
			} catch (BadLocationException e) {
				// kann nur passieren wenn einer was am Code vermurkst;
				// weiterwerfen zu Debugzwecken
				throw new AssertionError(e);
			}
		}

		/**
		 * Logfenster
		 */
		public Log() { super(new PlainDocument()); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Command.Code.LOG; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "Log"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override public String getDescription() { return "Log-Anzeige"; }
	}
	
	/**
	 * ABL-Fenster eines Bots. Kann ABL-Programme aus Textdateien laden, in Textdateien
	 * schreiben und zum simulierten oder echten Bot senden.
	 * @author Timo Sandmann (mail@timosandmann.de)
	 */
	public static class Abl extends BotComponent<PlainDocument>
	implements CanWriteAsynchronously {
		/** asynchroner Outputstream */
		private CommandOutputStream asyncOut;

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public synchronized void updateExternalModel() {
			// NOP
		}

		/**
		 * ABL-Fenster eines Bots
		 */
		public Abl() { 
			super(new PlainDocument()); 
		}
		
		/**
		 * @return Command-Code fuer ABL == REMOTE_CALL (Unterscheidung per Subcommand)
		 */
		public Code getHotCmdCode() {
			return Command.Code.REMOTE_CALL; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() {
			return "ABL"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override public String getDescription() { 
			return "ABL-Control"; 
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanWriteAsynchronously#setAsyncWriteStream(ctSim.model.CommandOutputStream)
		 */
		public void setAsyncWriteStream(CommandOutputStream s) {
			asyncOut = s;
		}

		/**
		 * Sendet den Inhalt des Fensters als ABL-Programm zum Bot.
		 * @param data	Das Programm
		 * @throws IOException
		 */
		public void sendAblData(String data) throws IOException {
			prepareAblCmd(asyncOut, data.getBytes());
			asyncOut.flush();
		}
		
		/**
		 * Bereitet den Transfer eines ABL-Programms zum Bot vor.
		 * @param s			OutputStream fuer die Daten
		 * @param payload	Das Programm als Byte-Array
		 */
		private void prepareAblCmd(CommandOutputStream s, byte[] payload) {
			Command c = s.getCommand(getHotCmdCode());
			c.setSubCmdCode(Command.SubCode.REMOTE_CALL_ABL);
			c.setDataL(payload.length & 0xFFFFFFFF);
			c.setDataR(payload.length >> 16);
			c.setPayload(payload);
		}
	}
	
	
	/**
	 * <p>
	 * Das Liquid Crystal Display (LCD) oben auf dem c't-Bot.
	 * </p>
	 * <p>
	 * <h3>{@link Command c't-Bot-Protokoll}</h3>
	 * Das LCD lauscht auf Kommandos mit dem
	 * {@linkplain Code#ACT_LCD Command-Code ACT_LCD}. Diese Kommandos
	 * m&uuml;ssen als Sub-Command-Code einen der folgenden Werte haben:
	 * <ul>
	 * <li>{@linkplain SubCode#LCD_CLEAR} &ndash; l&ouml;scht das LCD, so dass
	 * &uuml;berall nur Leerzeichen stehen. </li>
	 * <li>{@linkplain SubCode#LCD_CURSOR} &ndash; Bewegt den Cursor in die
	 * Spalte, die in {@code dataL} angegeben ist, und die Zeile, die in
	 * {@code dataR} angegeben ist. Beide Angaben zero-based, d.h. 0,0
	 * bezeichnet das linkeste Zeichen der obersten Zeile. Negative Werte werden
	 * wie 0 behandelt. Werte rechts au&szlig;erhalb des Display werden wie die
	 * rechteste Spalte behandelt; Werte unten au&szlig;erhalb des Display wie
	 * die unterste Zeile. </li>
	 * <li>{@linkplain SubCode#LCD_DATA} &ndash; Holt die Nutzlast des
	 * Kommandos und schreibt sie ins Display an die aktuelle Cursor-Position.
	 * Falls dort schon etwas steht, wird der alte Text &uuml;berschrieben
	 * (nicht weitergeschoben). Falls der Text rechts aus dem Display
	 * hinauslaufen w&uuml;rde, wird er abgeschnitten (bricht nicht in die
	 * n&auml;chste Zeile um). </li>
	 * <li>{@linkplain SubCode#NORM} &ndash; Kombination von LCD_CURSOR und
	 * LCD_DATA: Bewegt den Cursor wie ein LCD_CURSOR-Kommando und zeigt dann
	 * Text an wie ein LCD_DATA-Kommando. </li>
	 * </ul>
	 * </p>
	 */
	public static class LcDisplay extends BotComponent<PlainDocument>
	implements CanRead {
		/** Anzahl der Spalten */
		private final int numCols;
		/** Anzahl der Zeilen */
		private final int numRows;
		/** Cursor-Position X */
		private int cursorX;
		/** Cursor-Position X */
		private int cursorY;
		/** Daten */
		private final PlainDocument internalModel = new PlainDocument();

		/**
		 * Erstellt eine LCD-BotComponent mit der angegebenen Zahl Spalten (=
		 * Zeichen) und Zeilen.
		 * @param numCols Spalten
		 * @param numRows Zeilen
		 */
		public LcDisplay(int numCols, int numRows) {
			super(new PlainDocument());
			if (numCols < 1 || numRows < 1)
				throw new IllegalArgumentException();
			this.numCols = numCols;
			this.numRows = numRows;
			try {
				clearModel(internalModel);
				clearModel(getExternalModel());
			} catch (BadLocationException e) {
				// "kann nicht passieren"
				throw new AssertionError(e);
			}
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Command.Code.ACT_LCD; }

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
		public synchronized void readFrom(Command c) throws ProtocolException {
			try {
	    	    switch (c.getSubCode()) {
		            case NORM:
	            		setCursor(c.getDataL(), c.getDataR());
	        	        overwrite(c.getPayloadAsString());
	    	            break;

		            case LCD_DATA:
	            		overwrite(c.getPayloadAsString());
	        	    	break;

	    	        case LCD_CURSOR:
		                setCursor(c.getDataL(), c.getDataR());
	                	break;

	            	case LCD_CLEAR:
	    				clearModel(internalModel);
	    	            break;

		            default:
	            		throw new ProtocolException();
	        	}
			} catch (BadLocationException e) {
				// "kann nicht passieren"
				throw new AssertionError(e);
			}
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public synchronized void updateExternalModel() {
			try {
				getExternalModel().remove(0, getExternalModel().getLength());
				getExternalModel().insertString(0, getAllText(internalModel),
					null);
			} catch (NullPointerException e) {
				// NOP
			} catch (BadLocationException e) {
				// "Kann nicht passieren"
				throw new AssertionError(e);
			}
		}

		/**
		 * @param d Document
		 * @return Text von d
		 * @throws BadLocationException
		 */
		protected synchronized String getAllText(Document d)
		throws BadLocationException {
			return d.getText(0, d.getLength());
		}

		/**
		 * Setzt das Display zur&uuml;ck, so dass es auf ganzer Breite und
		 * H&ouml;he nur Leerzeichen anzeigt.
		 * @param d Document des Displays
		 *
		 * @throws BadLocationException nur falls jemand was am Code
		 * &auml;ndert; sollte normalerweise nie vorkommen.
		 */
		protected synchronized void clearModel(Document d)
		throws BadLocationException {
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < numCols; i++)
				b.append(' ');
			String spaces = b.toString();

			d.remove(0, d.getLength());
			// letzte Zeile ohne \n
			d.insertString(0, spaces, null);
			// 1. bis vorletzte Zeile mit \n
			for (int i = 0; i < numRows - 1; i++)
				d.insertString(0, spaces + "\n", null);
		}

		/**
		 * Text einf&uuml;gen an der aktuellen Cursorposition. Dort stehender
		 * Text wird &uuml;berschrieben (nicht verschoben).
		 *
		 * @param text Der Text, der ab der neuen Cursorposition einzutragen ist
		 * @throws BadLocationException "Kann nicht passieren"&#8482;
		 */
		protected synchronized void overwrite(String text)
		throws BadLocationException {
			// +1 fuer \n am Zeilenende
	    	int offset = cursorY * (numCols + 1) + cursorX;
	    	int numCharsAvail = numCols - cursorX;
	    	if (numCharsAvail < text.length())
	    		// abschneiden; wir haben nicht genug Platz in der Zeile
	    		text = text.substring(0, numCharsAvail);

			internalModel.remove(offset, text.length());
			internalModel.insertString(offset, text, null);
	    }

	    /**
		 * Bewegt den Cursor.
		 *
		 * @param col Neue Cursorposition (Spalte). Falls sie au&szlig;erhalb
		 * der Ausdehnung dieses Displays liegt, wird sie auf den
		 * n&auml;chstliegenden zul&auml;ssigen Wert gesetzt.
		 * @param row Dito (Zeile).
		 */
		protected synchronized void setCursor(int col, int row) {
	        cursorX = Misc.clamp(col, numCols - 1);
	        cursorY = Misc.clamp(row, numRows - 1);
	    }

		/** 
		 * Wieviele Spalten breit ist das Display? (1 Zeichen pro Spalte) 
		 * @return Spaltenanzahl 
		 */
		public synchronized int getNumCols() { return numCols; }

		/** 
		 * Wieviele Zeilen hoch ist das Display? 
		 * @return Zeilenanzahl
		 */
		public synchronized int getNumRows() { return numRows; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "LCD"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override public String getDescription() { return "LCD-Anzeige"; }
	}

	/**
	 * <p>
	 * Repr&auml;sentation einer LED (Leuchtdiode) auf dem Bot. Nicht verwirren
	 * lassen wegen dem Model: Eine LED ist etwas, was an oder aus sein kann,
	 * d.h. sie ist sinnvollerweise eine Art {@link JCheckBox} (auch wenn unsere
	 * LEDs sich nicht als Kasten mit/ohne H&auml;kchen malen, sondern als runde
	 * helle/dunkle Punkte). Diese Klasse hat daher das Model, das Checkboxen
	 * auch verwenden:JToggleButton.ToggleButtonModel. (Das wiederum
	 * kommt daher, dass Checkboxen in Java von Buttons abgeleitet sind.)
	 * </p>
	 * <p>
	 * <h3>{@link Command c't-Bot-Plotokoll}</h3>
	 * Jede LED lauscht auf {@link Command}s mit Command-Code
	 * {@link Code#ACT_LED ACT_LED}. Bei diesen Commands gibt das Feld
	 * {@code dataL} den Zustand aller LEDs an: ein Bit pro LED, 1 = an, 0 =
	 * aus. Jede Instanz dieser Klasse lauscht auf ein Bit und ignoriert die
	 * anderen; welches Bit wird beim Konstruieren angegeben.
	 * </p>
	 * <p>
	 * Datenformat des Felds dataL: Beispiel dataL = 0x42 = 0b01000010
	 *
	 * <pre>
	 *       .--------------- wei&szlig;
	 *       | .------------- t&uuml;rkis
	 *       | | .----------- gr&uuml;n
	 *       | | | .--------- gelb
	 *       | | | | .------- orange
	 *       | | | | | .----- rot
	 *       | | | | | | .--- blau vorne links
	 *       | | | | | | | .- blau vorne rechts
	 *       | | | | | | | |
	 * Wert  0 1 0 0 0 0 1 0  &lt;- Wie vom Draht gelesen
	 * Bit#  7 6 5 4 3 2 1 0  &lt;- bitIndexFromLsb (Das hier an Konstruktor geben)
	 *       |             |
	 *      MSB           LSB
	 * </pre>
	 *
	 * </p>
	 */
	public static class Led extends BotComponent<ButtonModel>
	implements CanRead {
		/** an oder aus? */
		private boolean internalModel = false;
		/** Name */
		private final String name;
		/** LED-Auswahl */
		private final int bitMask;
		/** Farbe, wenn LED an */
		private final Color colorWhenOn;

		/**
		 * Erstellt eine LED.
		 *
		 * @param name Name der LED, wie er dem Benutzer pr&auml;sentiert wird
		 * @param bitIndexFromLsb Welches Bit der LED-Statusangaben soll die LED
		 * beachten? 0 = LSB, N&auml;heres siehe {@link Led oben}, Abschnitt
		 * &quot;c't-Bot-Protokoll&quot;
		 * @param colorWhenOn Farbe der LED, wenn sie leuchtet. Eine dunklere
		 * Valiante f&uuml;r dann, wenn sie nicht leuchtet, wird automatisch
		 * berechnet
		 */
		public Led(String name, int bitIndexFromLsb, Color colorWhenOn) {
			super(new JToggleButton.ToggleButtonModel());
			this.name = name;
			
			/* LED 0 und 1 vertauschen (vorne links/rechts) */
			if (bitIndexFromLsb == 0)
				bitIndexFromLsb = 1;
			else if (bitIndexFromLsb == 1)
				bitIndexFromLsb = 0;
			
			bitMask = (int)Math.pow(2, bitIndexFromLsb);
			this.colorWhenOn = colorWhenOn;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
		public synchronized void readFrom(Command c) {
			internalModel = (c.getDataL() & bitMask) != 0;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public synchronized void updateExternalModel() {
			getExternalModel().setSelected(internalModel);
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Command.Code.ACT_LED; }

		/**
		 * Liefert die Farbe, in der die LED darzustellen ist, wenn sie an ist.
		 * Die Farbe f&uuml;r dann, wenn sie aus ist, sollte hieraus berechnet
		 * werden (typischerweise durch Reduzieren der S&auml;ttigung und/oder
		 * Helligkeit).
		 * @return Farbe
		 */
		public Color getColorWhenOn() { return colorWhenOn; }

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return name; }

		/** 
		 * Liefert einen leeren String (""). 
		 */
		@Override public String getDescription() { return ""; }

	}
}
