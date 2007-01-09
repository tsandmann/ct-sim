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

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import ctSim.model.Command;
import ctSim.model.bots.components.BotComponent;

//$$$ t
/**
 * Log eines Bot; hier werden die vom Bot geschickten Log-Ausgaben (Textzeilen)
 * gesammelt. Diese Bot-Komponente existiert nicht in Hardware.
 *
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class Log extends BotComponent<PlainDocument> {
	public Log() { super(new PlainDocument()); }

	@Override
	public void readFrom(Command c) {
		if (! c.has(Command.Code.LOG))
			return;
		try {
			getModel().insertString(getModel().getLength(),
				c.getPayloadAsString() + "\n", null);
		} catch (BadLocationException e) {
			// kann nur passieren wenn einer was am Code vermurkst;
			// weiterwerfen zu Debugzwecken
			throw new AssertionError(e);
		}
	}

	@Override public String getName() { return "Log"; }
	@Override public String getDescription() { return "Log-Anzeige"; }
}
