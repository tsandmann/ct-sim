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
package ctSim.model.bots.ctbot;

import static ctSim.model.bots.components.BotComponent.ConnectionFlags.READS;
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES;

import java.io.IOException;
import java.net.ProtocolException;

import ctSim.Connection;
import ctSim.controller.Config;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;

/**
 * Klasse aller simulierten c't-Bots, die ueber TCP mit dem Simulator
 * kommunizieren
 */
public class CtBotSimTcp extends CtBot implements SimulatedBot {
	/** Die TCP-Verbindung */
    private final Connection connection;

	/**
	 * @param connection Verbindung
	 */
	public CtBotSimTcp(final Connection connection) {
		super("Sim-Bot");
		this.connection = connection;

		addDisposeListener(new Runnable() {
			public void run() {
				try {
					connection.close();
				} catch (IOException e) {
					// uninteressant
				}
			}
		});

		components.add(
			new Sensors.Clock(),
			new WelcomeReceiver(Command.SubCode.WELCOME_SIM)
		);

		// Wer liest, wer schreibt
		components.applyFlagTable(
			_(Actuators.Governor.class   , READS),
			_(Actuators.LcDisplay.class  , READS),
			_(Actuators.Log.class        , READS),
			_(Actuators.DoorServo.class  , READS),
			_(Actuators.Led.class        , READS),
			_(Sensors.Clock.class        , READS, WRITES),
			_(Sensors.Encoder.class      , WRITES),
			_(Sensors.Distance.class     , WRITES),
			_(Sensors.Line.class         , WRITES),
			_(Sensors.Border.class       , WRITES),
			_(Sensors.Light.class        , WRITES),
			_(Sensors.Mouse.class        , WRITES),
			_(Sensors.RemoteControl.class, WRITES),
			_(Sensors.Door.class         , WRITES),
			_(Sensors.Trans.class        , WRITES),
			_(Sensors.Error.class        , WRITES),
			_(WelcomeReceiver.class      , READS),
			_(RemoteCallCompnt.class     , READS, WRITES)
		);

		sendRcStartCode();
	}

	@Override
	public String getDescription() {
		return "Simulierter, in C geschriebener c't-Bot, verbunden \u00FCber "+
			connection.getName();
	}

	/**
	 * Sendet den Fernbedienungs-(RC5-)Code, der in der Konfigdatei angegeben
	 * ist. Methode tut nichts, falls nichts, 0 oder ein nicht von
	 * {@link Integer#decode(String)} verwertbarer Code angegeben ist.
	 */
	@SuppressWarnings("cast")
	public void sendRcStartCode() {
		String rawStr = Config.getValue("rcStartCode");
		try {
			int rcStartCode = Integer.decode(rawStr);
			for (BotComponent<?> c : components) {
				if ((Object)c instanceof Sensors.RemoteControl) {
					((Sensors.RemoteControl)((Object)c)).send(rcStartCode);
					lg.fine("StartCode "+rcStartCode+" gesendet");
					break;
				}
			}
		} catch (NumberFormatException e) {
			lg.warn(e, "Konnte rcStartCode '%s' aus der Konfigdatei nicht " +
					"verwerten; ignoriere", rawStr);
		} catch (IOException e) {
			// Kann nicht passieren, da die RC nur IOExcp wirft, wenn sie
			// asynchron betrieben wird, was CtBotSimTcp nicht macht
			throw new AssertionError(e);
		}
	}

	public void doSimStep()
	throws InterruptedException, UnrecoverableScrewupException {
		transmitSensors();
		processUntilDoneCmd();
//		updateView();	// macht die ThreeDBot-Instanz bereits
	}

	/** Leite Sensordaten an den Bot weiter
	 * @throws UnrecoverableScrewupException */
	private synchronized void transmitSensors()
	throws UnrecoverableScrewupException {
		try {
			CommandOutputStream s = connection.getCmdOutStream();
			for (BotComponent<?> c : components)
				c.askForWrite(s);
			s.flush();
		} catch (IOException e) {
			throw new UnrecoverableScrewupException(e);
		}
	}

	private void processUntilDoneCmd() throws UnrecoverableScrewupException {
		try {
			while (true) {
				try {
					Command cmd = new Command(connection);
					components.processCommand(cmd);
					if (cmd.has(Command.Code.DONE))
						break;
				} catch (ProtocolException e) {
					lg.warn(e, "Ung\u00FCltiges Kommando; ignoriere");
				}
			}
		} catch (IOException e) {
			throw new UnrecoverableScrewupException(e);
		}
	}

	public Class<? extends Runnable> getSimulatorType() {
		return MasterSimulator.class;
	}
}
