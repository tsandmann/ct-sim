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

import javax.swing.SpinnerNumberModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.SimpleActuator;

/**
 * Klasse, die nur als Container f&uuml;r innere Klassen dient und selber keine
 * Methoden oder Felder hat. (F&uuml;r die winzigen inneren Klassen lohnt sich
 * keine eigene Datei.)
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public abstract class Actuator {
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
		@Override
		public String getDescription() {
			return "Servomotor f\u00FCr Klappe";
		}

		@Override public String getName() { return "DoorServo"; }
		public DoorServo() { super(new SpinnerNumberModel()); }
		public void readFrom(Command c) { getModel().setValue(c.getDataL()); }
		public Code getHotCmdCode() { return Code.ACT_DOOR; }
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
		public Log() { super(new PlainDocument()); }

		public void readFrom(Command c) {
			try {
				getModel().insertString(getModel().getLength(),
					c.getPayloadAsString() + "\n", null);
			} catch (BadLocationException e) {
				// kann nur passieren wenn einer was am Code vermurkst;
				// weiterwerfen zu Debugzwecken
				throw new AssertionError(e);
			}
		}

		public Code getHotCmdCode() { return Command.Code.LOG; }
		@Override public String getName() { return "Log"; }
		@Override public String getDescription() { return "Log-Anzeige"; }
	}
}
