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
package ctSim.model.bots.components.actuators;

import java.net.ProtocolException;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.util.Misc;

/**
 * Das Liquid Crystal Display (LCD) oben auf dem c't-Bot.
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class LcDisplay extends BotComponent<PlainDocument> implements CanRead {
	private final int numCols;
	private final int numRows;
	private int cursorX;
	private int cursorY;

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
		clearModel();
	}

	public Code getHotCmdCode() { return Command.Code.ACT_LCD; }

	public void readFrom(Command c) throws ProtocolException {
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
	            	clearModel();
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
	 * Setzt das Display zur&uuml;ck, so dass es auf ganzer Breite und H&ouml;he
	 * nur Leerzeichen anzeigt.
	 */
	protected void clearModel() {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < numCols; i++)
			b.append(' ');
		try {
			getModel().remove(0, getModel().getLength());
			// letzte Zeile ohne \n
			getModel().insertString(0, b.toString(), null);
			// 1. bis vorletzte Zeile mit \n
			for (int i = 0; i < numRows - 1; i++)
				getModel().insertString(0, b.toString() + "\n", null);
		} catch (BadLocationException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Text einf&uuml;gen an der aktuellen Cursorposition. Dort stehender Text
	 * wird &uuml;berschrieben (nicht verschoben).
	 *
	 * @param text Der Text, der ab der neuen Cursorposition einzutragen ist
	 * @throws BadLocationException "Kann nicht passieren"&#8482;
	 */
	protected void overwrite(String text)
    throws BadLocationException {
		// +1 fuer \n am Zeilenende
    	int offset = cursorY * (numCols + 1) + cursorX;
    	int numCharsAvail = numCols - cursorX;
    	if (numCharsAvail < text.length())
    		// abschneiden; wir haben nicht genug Platz in der Zeile
    		text = text.substring(0, numCharsAvail);

    	getModel().remove(offset, text.length());
    	getModel().insertString(offset, text, null);
    }

    /**
	 * Setzt den Cursor an eine bestimmte Position im LCD.
	 *
	 * @param col Neue Cursorposition. Falls sie au&szlig;erhalb der Ausdehnung
	 * dieses Displays liegt, wird sie auf den n&auml;chstliegenden
	 * zul&auml;ssigen Wert gesetzt.
	 * @param row Dito.
	 */
	protected void setCursor(int col, int row) {
        cursorX = Misc.clamp(col, numCols - 1);
        cursorY = Misc.clamp(row, numRows - 1);
    }

	/** Wieviele Spalten breit ist das Display? (1 Zeichen pro Spalte) */
	public int getNumCols() { return numCols; }

	/** Wieviele Zeilen hoch ist das Display? */
	public int getNumRows() { return numRows; }
	@Override public String getName() { return "LCD"; }
	@Override public String getDescription() { return "LCD-Anzeige"; }
}
