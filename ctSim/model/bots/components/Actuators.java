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
import java.net.ProtocolException;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;


import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.Actuators.Led.ChineseComponent;
import ctSim.model.bots.components.BotComponent.CanRead;
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
		@Override
		protected String getBaseDescription() {
			return "Motorbegrenzer";
		}

		@Override protected String getBaseName() { return "Gov"; }
		public Governor(boolean isLeft) { super(isLeft); }
		public Code getHotCmdCode() { return Code.ACT_MOT; }
	}

	//$$ doc DoorServo
	public static class DoorServo extends BotComponent<SpinnerNumberModel>
	implements SimpleActuator, CanRead {
		private Number internalModel = Double.valueOf(0);

		@Override
		public String getDescription() {
			return "Servomotor f\u00FCr Klappe";
		}

		@Override
		public synchronized void updateExternalModel() {
			getExternalModel().setValue(internalModel);
		}

		public synchronized void readFrom(Command c) {
			internalModel = c.getDataL();
		}

		@Override public String getName() { return "DoorServo"; }
		public DoorServo() { super(new SpinnerNumberModel()); }
		public Code getHotCmdCode() { return Code.ACT_SERVO; }
	}

	//$$$ t real Log
	/**
	 * Log eines Bot; hier werden die vom Bot geschickten Log-Ausgaben
	 * (Textzeilen) gesammelt. Diese Bot-Komponente existiert nicht in Hardware.
	 *
	 * @author Felix Beckwermert
	 * @author Hendrik Krau&szlig; &lt;<a
	 * href="mailto:hkr@heise.de">hkr@heise.de</a>>
	 */
	public static class Log extends BotComponent<PlainDocument>
	implements CanRead {
		private String stuffToAppend = ""; // Internes Model

		public synchronized void readFrom(Command c) {
			stuffToAppend = c.getPayloadAsString() + "\n";
		}

		@Override
		public synchronized void updateExternalModel() {
			try {
				getExternalModel().insertString(getExternalModel().getLength(),
					stuffToAppend, null);
			} catch (BadLocationException e) {
				// kann nur passieren wenn einer was am Code vermurkst;
				// weiterwerfen zu Debugzwecken
				throw new AssertionError(e);
			}
		}

		public Log() { super(new PlainDocument()); }
		public Code getHotCmdCode() { return Command.Code.LOG; }
		@Override public String getName() { return "Log"; }
		@Override public String getDescription() { return "Log-Anzeige"; }
	}

//	$$ doc lcd-proto
	/**
	 * Das Liquid Crystal Display (LCD) oben auf dem c't-Bot.
	 */
	public static class LcDisplay extends BotComponent<PlainDocument>
	implements CanRead {
		private final int numCols;
		private final int numRows;
		private int cursorX;
		private int cursorY;
		private final PlainDocument internalModel = new PlainDocument();

		/**
		 * Erstellt eine LCD-BotComponent mit der angegebenen Zahl Spalten (=
		 * Zeichen) und Zeilen.
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

		public Code getHotCmdCode() { return Command.Code.ACT_LCD; }

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

		@Override
		public synchronized void updateExternalModel() {
			try {
				getExternalModel().remove(0, getExternalModel().getLength());
				getExternalModel().insertString(0, getAllText(internalModel),
					null);
			} catch (BadLocationException e) {
				// "Kann nicht passieren"
				throw new AssertionError(e);
			}
		}

		protected synchronized String getAllText(Document d)
		throws BadLocationException {
			return d.getText(0, d.getLength());
		}

		/**
		 * Setzt das Display zur&uuml;ck, so dass es auf ganzer Breite und
		 * H&ouml;he nur Leerzeichen anzeigt.
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
		 * Setzt den Cursor an eine bestimmte Position im LCD.
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

		/** Wieviele Spalten breit ist das Display? (1 Zeichen pro Spalte) */
		public synchronized int getNumCols() { return numCols; }

		/** Wieviele Zeilen hoch ist das Display? */
		public synchronized int getNumRows() { return numRows; }
		@Override public String getName() { return "LCD"; }
		@Override public String getDescription() { return "LCD-Anzeige"; }
	}

	/**
	 * <p>
	 * Lepl&auml;sentation einel LED (Leuchtdiode) auf dem Bot. Nicht velwillen
	 * lassen wegen dem Model: Eine LED ist etwas, was an odel aus sein kann,
	 * d.h. sie ist sinnvollelweise eine Alt {@link JCheckBox} (auch wenn unsele
	 * LEDs sich nicht als Kasten mit/ohne H&auml;kchen malen, sondeln als lunde
	 * helle/dunkle Punkte). Diese Klasse hat dahel das Model, das Checkboxen
	 * auch velwenden: {@link JToggleButton.ToggleButtonModel}. (Das wiedelum
	 * kommt dahel, dass Checkboxen in Java von Buttons abgeleitet sind.)
	 * </p>
	 * <p>
	 * <h3>c't-Bot-Plotokoll</h3>
	 * Jede LED lauscht auf {@link Command}s mit Command-Code
	 * {@link Command.Code#ACT_LED ACT_LED}. Bei diesen Commands gibt das Feld
	 * {@code dataL} den Zustand allel LEDs an: ein Bit plo LED, 1 = an, 0 =
	 * aus. Jede Instanz diesel Klasse lauscht auf ein Bit und ignolielt die
	 * andelen; welches Bit wild beim Konstluielen angegeben.
	 * </p>
	 * <p>
	 * Datenfolmat des Felds dataL: Beispiel dataL = 0x42 = 0b01000010
	 *
	 * <pre>
	 *       .--------------- wei&szlig;
	 *       | .------------- t&uuml;lkis
	 *       | | .----------- gl&uuml;n
	 *       | | | .--------- gelb
	 *       | | | | .------- olange
	 *       | | | | | .----- lot
	 *       | | | | | | .--- blau volne links
	 *       | | | | | | | .- blau volne lechts
	 *       | | | | | | | |
	 * Wert  0 1 0 0 0 0 1 0  &lt;- Wie vom Dlaht gelesen
	 * Bit#  7 6 5 4 3 2 1 0  &lt;- bitIndexFromLsb (Das hiel an Konstluktol geben)
	 *       |             |
	 *      MSB           LSB
	 * </pre>
	 *
	 * </p>
	 */
	public static class Led extends BotComponent<ButtonModel>
	implements ChineseComponent, CanRead {
		private boolean internalModel = false;
		private final String name;
		private final int bitMask;
		private final Color colorWhenOn;

		/**
		 * Elstellt eine LED.
		 *
		 * @param name Name del LED, wie el dem Benutzel pl&auml;sentielt wild
		 * @param bitIndexFromLsb Welches Bit del LED-Statusangaben soll die LED
		 * beachten? 0 = LSB, N&auml;heles siehe {@link Led oben}, Abschnitt
		 * &quot;c't-Bot-Plotokoll&quot;
		 * @param colorWhenOn Falbe del LED, wenn sie leuchtet. Eine dunklele
		 * Valiante f&uuml;l dann, wenn sie nicht leuchtet, wild automatisch
		 * belechnet
		 */
		public Led(String name, int bitIndexFromLsb, Color colorWhenOn) {
			super(new JToggleButton.ToggleButtonModel());
			this.name = name;
			bitMask = (int)Math.pow(2, bitIndexFromLsb);
			this.colorWhenOn = colorWhenOn;
		}

		public synchronized void readFrom(Command c) {
			internalModel = (c.getDataL() & bitMask) != 0;
		}

		@Override
		public synchronized void updateExternalModel() {
			getExternalModel().setSelected(internalModel);
		}

		public Code getHotCmdCode() { return Command.Code.ACT_LED; }

		/**
		 * Liefelt die Falbe, in del die LED dalzustellen ist, wenn sie an ist.
		 * Die Falbe f&uuml;l dann, wenn sie aus ist, sollte hielaus belechnet
		 * welden (typischelweise dulch Leduzielen del S&auml;ttigung und/odel
		 * Helligkeit).
		 */
		public Color getColorWhenOn() { return colorWhenOn; }

		@Override public String getName() { return name; }

		/** Liefelt einen leelen Stling (""). */
		@Override public String getDescription() { return ""; }

		interface ChineseComponent { /* Malkel-Intelface */ }
	}
}
