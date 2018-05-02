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

import java.net.ProtocolException;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.Command.SubCode;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.SimpleActuator;
import ctSim.util.BotID;

/** Handshake für Bot-Sim Connection */
public class WelcomeReceiver extends BotComponent<BotID> implements SimpleActuator, CanRead {
	/** Subcode */
	private final SubCode expectedForWelcome;

	/** Log-Ausgabe */
	private boolean feature_log = false;

	/** Fernbedienung */
	private boolean feature_rc5 = false;

	/** ABL Programm-Empfang */
	private boolean feature_abl_program = false;

	/** Basic Programm-Empfang */
	private boolean feature_basic_program = false;

	/** Kartographie */
	private boolean feature_map = false;

	/** RemoteCalls */
	private boolean feature_remotecall = false;

	/**
	 * Handshake für Connection
	 *
	 * @param expectedForWelcome	Subcode für neue Connection
	 */
	public WelcomeReceiver(SubCode expectedForWelcome) {
		super(null);
		this.expectedForWelcome = expectedForWelcome;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Willkommenskommando-Auswerte-Ding";
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
	 */
	@Override
	public Code getHotCmdCode() {
		return Command.Code.WELCOME;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
	 */
	@Override
	public void readFrom(Command c) throws ProtocolException {
		if (! c.has(expectedForWelcome)) {
			throw new ProtocolException("Willkommenskommando empfangen, das nicht den erwarteten Subcode " +
					expectedForWelcome + " hatte");
		}
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public void updateExternalModel() {
		// No-op
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getName()
	 */
	@Override
	public String getName() {
		return "Bot-ID";
	}

	/**
	 * Setzt die Features des Bots. Codierung gemäß ct-Bot/command.c
	 *
	 * @param features	Alle Features in einen integer gepackt
	 */
	public void setFeatures(int features) {
		if ((features & 1) == 1) {
			feature_log = true;
		}

		if ((features & 2) == 2) {
			feature_rc5 = true;
		}

		if ((features & 4) == 4) {
			feature_abl_program = true;
		}

		if ((features & 8) == 8) {
			feature_basic_program = true;
		}

		if ((features & 16) == 16) {
			feature_map = true;
		}

		if ((features & 32) == 32) {
			feature_remotecall = true;
		}
	}

	/**
	 * @return Hat der Bot eine Logausgabe aktiviert?
	 */
	public boolean get_feature_log() {
		return feature_log;
	}

	/**
	 * @return Hat der Bot eine Fernbedienung aktiviert?
	 */
	public boolean get_feature_rc5() {
		return feature_rc5;
	}

	/**
	 * @return Kann der Bot ABL-Programme empfangen?
	 */
	public boolean get_feature_abl_program() {
		return feature_abl_program;
	}

	/**
	 * @return Kann der Bot Basic-Programme empfangen?
	 */
	public boolean get_feature_basic_program() {
		return feature_basic_program;
	}

	/**
	 * @return Hat der Bot die Kartographie aktiviert?
	 */
	public boolean get_feature_map() {
		return feature_map;
	}

	/**
	 * @return Kann der Bot RemoteCalls empfangen?
	 */
	public boolean get_feature_remotecall() {
		return feature_remotecall;
	}
}
