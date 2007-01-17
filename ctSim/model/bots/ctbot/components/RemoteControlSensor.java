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

import javax.swing.SpinnerNumberModel;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.view.gui.RemoteControlViewer;

/**
 * <p>
 * Software-Emulation der Fernbedienung. Spezielles Verhalten beim Schreiben:
 * Nur, wenn sein Wert ungleich 0 ist, sendet der Sensor. Anschlie&szlig;end
 * setzt er seinen Wert auf 0 zur&uuml;ck, so dass pro Tastendruck in der
 * Fernbedienungs-Gui ein Command auf den Draht gegeben wird.
 * </p>
 * <p>
 * <strong>c't-Bot-Protokoll:</strong> Der Fernbedienungssensor sendet eine
 * Ganzzahl, die f&uuml;r einen Knopf steht (Zuordnung siehe
 * {@link RemoteControlViewer}). Die Zahl steht im Feld {@code dataL}, die
 * &uuml;brigen Felder werden nicht ver&auml;ndert.
 * </p>
 *
 * @author Peter K&ouml;nig
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class RemoteControlSensor extends BotComponent<SpinnerNumberModel>
implements CanWrite {
	public void writeTo(Command c) {
		int v = getModel().getNumber().intValue();
		if (v == 0)
			return;
		c.setDataL(v);
		getModel().setValue(0); //$$$ Wer muss das sonst noch? MausPicture
	}

	public void set(final Integer rc5Code) {
		getModel().setValue(rc5Code);
	}

	@Override public String getName() { return "RC5"; }
	@Override public String getDescription() { return "Fernbedienung"; }
	public RemoteControlSensor() { super(new SpinnerNumberModel()); }
	public Code getHotCmdCode() { return Code.SENS_RC5; }
}