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

import java.awt.Color;
import java.io.IOException;
import java.net.ProtocolException;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.Command.SubCode;
import ctSim.model.CommandOutputStream;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.model.bots.components.BotComponent.SimpleActuator;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Klasse, die nur als Container für innere Klassen dient und selber keine Methoden oder Felder hat.
 * (Für die winzigen inneren Klassen lohnt sich keine eigene Datei.)
 *
 * @author Hendrik Krauß
 */
public class Actuators {
	/**
	 * Governor, der die Fahrgeschwindigkeit des Bot regelt (genauer: die Drehgeschwindigkeit eines
	 * Rads; der c't-Bot hat daher einen linken und einen rechten Governor).
	 */
	public static class Governor extends NumberTwin implements SimpleActuator, CanRead {
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
		@Override
		protected String getBaseName() { 
			return "Motor"; 
		}
		
		/**
		 * @param isLeft	links?
		 */
		public Governor(boolean isLeft) {
			super(isLeft); 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		@Override
		public Code getHotCmdCode() { 
			return Code.ACT_MOT; 
		}
	}

	/** Servo für Klappe */
	public static class DoorServo extends NumberTwin implements SimpleActuator, CanRead {
		
		/**
		 * Servo für Klappe
		 * 
		 * @param isLeft	Servo 1 ("links") oder 2 ("rechts")?
		 */
		public DoorServo(boolean isLeft) {
			super(isLeft);
			assert(isLeft);
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getBaseDescription() {
			return "Servomotor für Klappe";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getBaseName() { 
			return "DoorServo"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		@Override
		public Code getHotCmdCode() { 
			return Code.ACT_SERVO; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			return getBaseName();
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return getBaseDescription();
		}
	}
	
	/** Servo für Kamera */
	public static class CamServo extends NumberTwin implements SimpleActuator, CanRead {
		
		/**
		 * Servo für Kamera
		 * 
		 * @param isLeft	Servo 1 ("links") oder 2 ("rechts")?
		 */
		public CamServo(boolean isLeft) {
			super(isLeft);
			assert(! isLeft);
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getBaseDescription() {
			return "Servomotor für Kamera";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getBaseName() { 
			return "CamServo"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		@Override
		public Code getHotCmdCode() { 
			return Code.ACT_SERVO; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			return getBaseName();
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return getBaseDescription();
		}
	}

	/**
	 * Log eines Bot; hier werden die vom Bot geschickten Log-Ausgaben (Textzeilen) gesammelt.
	 * Diese Bot-Komponente existiert nicht in Hardware.
	 */
	public static class Log extends BotComponent<PlainDocument> implements CanRead {
		/** Internes Model */
		private final StringBuffer newStuff = new StringBuffer(); 
		/** Zeitpunkt des letzten View-Updates */
		private long lastUpdateTime = 0;
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
		@Override
		public void readFrom(Command c) {
			synchronized (newStuff) {
				final String newLine = Command.replaceCtrlChars(c.getPayloadAsString());
				newStuff.append(newLine);
				if (newStuff.charAt(newStuff.length() - 1) != '\n' && 
						(newLine.contains("DEBUG") || newLine.contains("INFO") || newLine.contains("WARNING")
						|| newLine.contains("ERROR") || newLine.contains("FATAL"))) {
					newStuff.append("\n");
				}
			}
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public void updateExternalModel() {
			final long now = System.currentTimeMillis();
			if (now - lastUpdateTime < 500) {
				return;
			}
			lastUpdateTime = now;
			try {
				synchronized (newStuff) {
					getExternalModel().insertString(getExternalModel().getLength(),
						newStuff.toString(), null);
					newStuff.delete(0, newStuff.length());
				}
			} catch (BadLocationException e) {
				// kann nur passieren wenn jemand etwas am Code vermurkst; weiter werfen zu Debugzwecken
				throw new AssertionError(e);
			}
		}
		
		/** Logfenster */
		public Log() { 
			super(new PlainDocument()); 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		@Override
		public Code getHotCmdCode() { 
			return Command.Code.LOG; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() { 
			return "Log"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() { 
			return "Log-Anzeige"; 
		}
	}
	
	/**
	 * Programm-Komponente eines Bots. Kann Basic- und ABL-Programme aus Textdateien laden,
	 * in Textdateien schreiben und zum simulierten oder echten Bot senden.
	 * 
	 * @author Timo Sandmann
	 */
	public static class Program extends BotComponent<PlainDocument> implements CanWriteAsynchronously {
		/** Logger für die Programm-Komponente */
		final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.components.Program");
		
		/** asynchroner Outputstream */
		private CommandOutputStream asyncOut;
		
		/** Größe eines übertragenen Blocks [Byte] */
		private final int SEND_SIZE = 64;

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public synchronized void updateExternalModel() {
			// No-op
		}

		/** Programm-Komponente eines Bots */
		public Program() { 
			super(new PlainDocument()); 
		}
		
		/**
		 * @return Command-Code für Skript-Programme
		 */
		public Code getHotCmdCode() {
			return Command.Code.PROGRAM; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			return "Programm"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() { 
			return "Programmfenster für Skriptsprachen"; 
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanWriteAsynchronously#setAsyncWriteStream(ctSim.model.CommandOutputStream)
		 */
		@Override
		public void setAsyncWriteStream(CommandOutputStream s) {
			asyncOut = s;
		}

		/**
		 * Sendet den Inhalt des Fensters als Programm zum Bot
		 * 
		 * @param filename	Dateiname für das Programm
		 * @param data		Das Programm
		 * @param type		Typ, 0: Basic, 1: ABL
		 * @param bot		Referenz auf die zugehörige Bot-Instanz
		 * @throws IOException
		 */
		public void sendProgramData(String filename, String data, int type, Bot bot) throws IOException {
			/** Wartezeit zwischen den Bloecken [ms] */
			final int WAIT_TIME = 75;
			
			lg.fine("sendProgramData(" + filename + ", " + data + ", " + type + ")");
			data += '\0';
			final int length = data.length();
			lg.fine(" length=" + length);
			prepareCmd(asyncOut, filename.getBytes(), type, length);
			asyncOut.flush();
			if (bot instanceof RealCtBot) {
				/* dem Bot Zeit geben, den Empfangspuffer zu verarbeiten und die Datei anzulegen */
				try {
					Thread.sleep(WAIT_TIME * 3);
				} catch (InterruptedException e) {
					// kein Plan
				}
			}
			
			lg.fine(" sende " + (length / SEND_SIZE) + " Bloecke von " + SEND_SIZE + " Byte");
			/* SEND_SIZE Byte Blöcke */
			byte[] bytes = new byte[SEND_SIZE];
			int i;
			for (i = 0; i < length / SEND_SIZE; ++i) {
				lg.fine(" i=" + i);
				System.arraycopy(data.getBytes(), i * SEND_SIZE, bytes, 0, SEND_SIZE);
				sendData(asyncOut, bytes, type, i);
				asyncOut.flush();
				if (bot instanceof RealCtBot) {
					/* dem Bot Zeit geben, den Empfangspuffer zu verarbeiten */
					try {
						Thread.sleep(WAIT_TIME);
					} catch (InterruptedException e) {
						// kein Plan
					}
				}
			}
			/* Rest */
			final int to_send = length % SEND_SIZE;
			lg.fine("to_send=" + to_send);
			if (to_send > 0) {
				lg.fine(" sende zusätzliche " + to_send + " Byte");
				bytes = new byte[to_send];
				System.arraycopy(data.getBytes(), length - to_send, bytes, 0, to_send - 1);
				sendData(asyncOut, bytes, type, i);
				asyncOut.flush();
				if (bot instanceof RealCtBot) {
					/* dem Bot Zeit geben, den Empfangspuffer zu verarbeiten */
					try {
						Thread.sleep(WAIT_TIME);
					} catch (InterruptedException e) {
						// kein Plan
					}
				}
			}
		}
		
		/**
		 * Bereitet den Transfer eines Programms zum Bot vor
		 * 
		 * @param s			OutputStream für die Daten
		 * @param filename	Dateiname für das Programm
		 * @param type		Typ, 0: Basic, 1: ABL
		 * @param length	Länge des Programms in Byte
		 */
		private void prepareCmd(CommandOutputStream s, byte[] filename, int type, int length) {
			Command c = s.getCommand(getHotCmdCode());
			c.setSubCmdCode(Command.SubCode.PROGRAM_PREPARE);
			c.setDataL(type);			
			c.setDataR(length);
			c.setPayload(filename);
		}
		
		/**
		 * Sendet die Programmdaten zum Bot (in SEND_SIZE Byte großen Teilen)
		 * 
		 * @param s		OutputStream für die Daten
		 * @param data	Programmdaten
		 * @param type	Typ, 0: Basic, 1: ABL
		 * @param step	Nr. des Datenstücks
		 */
		private void sendData(CommandOutputStream s, byte[] data, int type, int step) {
			Command c = s.getCommand(getHotCmdCode());
			c.setSubCmdCode(Command.SubCode.PROGRAM_DATA);
			c.setDataL(type);
			c.setDataR(step * SEND_SIZE);
			c.setPayload(data);
		}
		
		/**
		 * Sendet das Kommando, um ein Programm auf dem Bot zu starten
		 * 
		 * @param type	Typ des Programm; 0: Basic, 1: ABL
		 */
		public void startProgram(int type) {
			switch (type) {
			case 0:
				lg.info("Starte Basic-Programm auf dem Bot...");
				break;
				
			case 1:
				lg.info("Starte ABL-Programm auf dem Bot...");
				break;
				
			default:
				lg.warn("ungültiger Programm-Typ");
				return;
			}
			Command c = asyncOut.getCommand(getHotCmdCode());
			c.setSubCmdCode(Command.SubCode.PROGRAM_START);
			c.setDataL(type);
			try {
				asyncOut.flush();
			} catch (IOException e) {
				lg.warn("Konnte Programm nicht starten:");
				e.printStackTrace();
			}
		}
		
		/**
		 * Bricht ein auf dem Bot laufendes Programm ab
		 * 
		 * @param type	Typ, 0: Basic, 1: ABL
		 */
		public void stopProgram(int type) {			
			lg.fine("Breche Programm auf dem Bot ab");
			Command c = asyncOut.getCommand(getHotCmdCode());
			c.setSubCmdCode(Command.SubCode.PROGRAM_STOP);
			c.setDataL(type);
			try {
				asyncOut.flush();
			} catch (IOException e) {
				lg.warn("Konnte Programm nicht abbrechen:");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * <p>
	 * Das Liquid Crystal Display (LCD) oben auf dem c't-Bot.
	 * </p>
	 * <p>
	 * <h3>{@link Command c't-Bot-Protokoll}</h3>
	 * Das LCD lauscht auf Kommandos mit dem {@linkplain Code#ACT_LCD Command-Code ACT_LCD}.
	 * Diese Kommandos müssen als Sub-Command-Code einen der folgenden Werte haben:
	 * <ul>
	 * <li>{@linkplain SubCode#LCD_CLEAR} -- löscht das LCD, so dass überall nur Leerzeichen stehen.</li>
	 * <li>{@linkplain SubCode#LCD_CURSOR} -- Bewegt den Cursor in die Spalte, die in {@code dataL}
	 * angegeben ist, und die Zeile, die in {@code dataR} angegeben ist. Beide Angaben zero-based,
	 * d.h. 0,0 bezeichnet das linkeste Zeichen der obersten Zeile. Negative Werte werden wie 0
	 * behandelt. Werte rechts außerhalb des Display werden wie die rechteste Spalte behandelt;
	 * Werte unten außerhalb des Display wie die unterste Zeile.</li>
	 * <li>{@linkplain SubCode#LCD_DATA} -- Holt die Nutzlast des Kommandos und schreibt sie ins
	 * Display an die aktuelle Cursor-Position. Falls dort schon etwas steht, wird der alte Text
	 * überschrieben (nicht weitergeschoben). Falls der Text rechts aus dem Display hinauslaufen
	 * würde, wird er abgeschnitten (bricht nicht in die nächste Zeile um).</li>
	 * <li>{@linkplain SubCode#NORM} -- Kombination von LCD_CURSOR und LCD_DATA: Bewegt den Cursor
	 * wie ein LCD_CURSOR-Kommando und zeigt dann Text an wie ein LCD_DATA-Kommando.</li>
	 * </ul>
	 * </p>
	 */
	public static class LcDisplay extends BotComponent<PlainDocument> implements CanRead {
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
		 * Erstellt eine LCD-BotComponent mit der angegebenen Zahl Spalten (= Zeichen) und Zeilen
		 * 
		 * @param numCols	Spalten
		 * @param numRows	Zeilen
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
		@Override
		public Code getHotCmdCode() { return Command.Code.ACT_LCD; }

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
		@Override
		public synchronized void readFrom(Command c) throws ProtocolException {
			try {
	    	    switch (c.getSubCode()) {
		            case LCD_DATA:
		            	setCursor(c.getDataL(), c.getDataR());
	            		overwrite(Command.replaceCtrlChars(c.getPayloadAsString()));
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
				// egal
			} catch (BadLocationException e) {
				// "kann nicht passieren"
				throw new AssertionError(e);
			}
		}

		/**
		 * @param d	Document
		 * @return Text von d
		 * @throws BadLocationException
		 */
		protected synchronized String getAllText(Document d) throws BadLocationException {
			return d.getText(0, Math.max(d.getLength(), numCols * numRows));
		}

		/**
		 * Setzt das Display zurück, sodass es auf ganzer Breite und Höhe nur Leerzeichen anzeigt.
		 * 
		 * @param d	Document des Displays
		 * @throws BadLocationException	nur falls jemand etwas am Code ändert;
		 * 			sollte normalerweise nie vorkommen.
		 */
		protected synchronized void clearModel(Document d) throws BadLocationException {
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
		 * Text einfügen an der aktuellen Cursorposition
		 * Dort stehender Text wird überschrieben (nicht verschoben).
		 *
		 * @param text	Der Text, der ab der neuen Cursorposition einzutragen ist
		 * @throws BadLocationException	"Kann nicht passieren"
		 */
		protected synchronized void overwrite(String text) throws BadLocationException {
			// +1 für \n am Zeilenende
	    	int offset = cursorY * (numCols + 1) + cursorX;
	    	int numCharsAvail = numCols - cursorX;
	    	if (numCharsAvail < text.length())
	    		// abschneiden; wir haben nicht genug Platz in der Zeile
	    		text = text.substring(0, numCharsAvail);

			internalModel.remove(offset, text.length());
			internalModel.insertString(offset, text, null);
	    }

	    /**
		 * Bewegt den Cursor
		 *
		 * @param col	Neue Cursorposition (Spalte). Falls sie außerhalb der Ausdehnung dieses
		 * 				Displays liegt, wird sie auf den nächstliegenden zulässigen Wert gesetzt.
		 * @param row	Dito (Zeile)
		 */
		protected synchronized void setCursor(int col, int row) {
	        cursorX = Misc.clamp(col, numCols - 1);
	        cursorY = Misc.clamp(row, numRows - 1);
	    }

		/** 
		 * Wie viele Spalten breit ist das Display? (1 Zeichen pro Spalte)
		 * 
		 * @return Spaltenanzahl 
		 */
		public synchronized int getNumCols() { 
			return numCols; 
		}

		/** 
		 * Wie viele Zeilen hoch ist das Display?
		 * 
		 * @return Zeilenanzahl
		 */
		public synchronized int getNumRows() { 
			return numRows; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() { 
			return "LCD"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() { 
			return "LCD-Anzeige"; 
		}
	}

	/**
	 * <p>
	 * Repräsentation einer LED (Leuchtdiode) auf dem Bot. Nicht verwirren lassen wegen dem Model:
	 * Eine LED ist etwas, was an oder aus sein kann, d.h. sie ist sinnvollerweise eine Art
	 * {@link JCheckBox} (auch wenn unsere LEDs sich nicht als Kasten mit/ohne Häkchen malen, sondern
	 * als runde helle/dunkle Punkte). Diese Klasse hat daher das Model, das Checkboxen auch verwenden:
	 * JToggleButton.ToggleButtonModel. (Das wiederum kommt daher, dass Checkboxen in Java von Buttons
	 * abgeleitet sind.)
	 * </p>
	 * <p>
	 * <h3>{@link Command c't-Bot-Plotokoll}</h3>
	 * Jede LED lauscht auf {@link Command}s mit Command-Code {@link Code#ACT_LED ACT_LED}. Bei diesen
	 * Commands gibt das Feld {@code dataL} den Zustand aller LEDs an: ein Bit pro LED, 1 = an, 0 = aus.
	 * Jede Instanz dieser Klasse lauscht auf ein Bit und ignoriert die anderen; welches Bit wird beim
	 * Konstruieren angegeben.
	 * </p>
	 * <p>
	 * Datenformat des Felds dataL: Beispiel dataL = 0x42 = 0b01000010
	 *
	 * <pre>
	 *       .--------------- weiß
	 *       | .------------- türkis
	 *       | | .----------- grün
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
	public static class Led extends BotComponent<ButtonModel> implements CanRead {
		/** an oder aus? */
		private boolean internalModel = false;
		/** Name */
		private final String name;
		/** LED-Auswahl */
		private final int bitMask;
		/** Farbe, wenn LED an */
		private final Color colorWhenOn;

		/**
		 * Erstellt eine LED
		 *
		 * @param name				Name der LED, wie er dem Benutzer präsentiert wird
		 * @param bitIndexFromLsb	Welches Bit der LED-Statusangaben soll die LED beachten? 0 = LSB,
		 * 							Näheres siehe {@link Led oben}, Abschnitt "c't-Bot-Protokoll"
		 * @param colorWhenOn		Farbe der LED, wenn sie leuchtet. Eine dunklere Variante für dann,
		 * 							wenn sie nicht leuchtet, wird automatisch berechnet
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
		@Override
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
		@Override
		public Code getHotCmdCode() { 
			return Command.Code.ACT_LED; 
		}

		/**
		 * Liefert die Farbe, in der die LED darzustellen ist, wenn sie an ist.
		 * Die Farbe für dann, wenn sie aus ist, sollte hieraus berechnet werden (typischerweise durch
		 * Reduzieren der Sättigung und/oder Helligkeit).
		 * 
		 * @return Farbe
		 */
		public Color getColorWhenOn() { 
			return colorWhenOn; 
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() { 
			return name; 
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() { 
			return ""; 
		}
	}
}
