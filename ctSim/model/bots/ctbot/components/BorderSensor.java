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
package ctSim.model.bots.ctbot.components;

import ctSim.model.Command.Code;
import ctSim.model.bots.components.NumberTwin;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.SimpleSensor;

/**
 * Klasse der Abgrundsensoren
 *
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
//$$ doc
public class BorderSensor extends NumberTwin implements SimpleSensor, CanWrite {
	@Override
	protected String getBaseDescription() {
		return "Abgrundsensor [0; 1023]";
	}

	@Override protected String getBaseName() { return "Border"; }
	public BorderSensor(boolean isLeft) { super(isLeft); }
	public Code getHotCmdCode() { return Code.SENS_BORDER; }
}
