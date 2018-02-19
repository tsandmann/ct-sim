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
package ctSim.model.bots.ctbot;

import static ctSim.model.bots.components.BotComponent.ConnectionFlags.READS;
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES;
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES_ASYNCLY;

import java.io.IOException;
import java.net.ProtocolException;
import ctSim.Connection;
import ctSim.controller.Config;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.MapComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;
import ctSim.model.bots.components.RemoteCallCompnt.BehaviorExitStatus;
import ctSim.util.BotID;
import ctSim.util.Runnable1;

/**
 * Klasse aller simulierten c't-Bots, die über TCP mit dem Simulator
 * kommunizieren
 */
public class CtBotSimTcp extends CtBot implements SimulatedBot {
	/**
	 * @param connection Verbindung
	 * @param newId Id für die Kommunikation 
	 * @param features Features des Bots gepackt in einen Integer
	 * @throws ProtocolException 
	 */
	public CtBotSimTcp(final Connection connection, BotID newId, int features) throws ProtocolException {
		super("Sim-Bot");
		
		if (connection == null) { 
			throw new ProtocolException("Connection ist null");
		}
		
		setConnection(connection);
		lg.info("ID ist erstmal " + newId);
		setId(newId);

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
			createCompnt(Actuators.Governor.class   , READS),
			createCompnt(Actuators.LcDisplay.class  , READS),
			createCompnt(Actuators.Log.class        , READS),
			createCompnt(Actuators.DoorServo.class  , READS),
			createCompnt(Actuators.CamServo.class   , READS),
			createCompnt(Actuators.Led.class        , READS),
			createCompnt(Sensors.Clock.class        , READS, WRITES),
			createCompnt(Sensors.Encoder.class      , WRITES),
			createCompnt(Sensors.Distance.class     , WRITES),
			createCompnt(Sensors.Line.class         , WRITES),
			createCompnt(Sensors.Border.class       , WRITES),
			createCompnt(Sensors.Light.class        , WRITES),
			createCompnt(Sensors.Mouse.class        , WRITES),
			createCompnt(Sensors.RemoteControl.class, WRITES),
			createCompnt(Sensors.Door.class			, WRITES),
			createCompnt(Sensors.CamPos.class		, WRITES),
			createCompnt(Sensors.Trans.class		, WRITES),
			createCompnt(Sensors.Error.class		, WRITES),
			createCompnt(Sensors.BPSReceiver.class	, WRITES),
			createCompnt(Sensors.Shutdown.class		, READS, WRITES_ASYNCLY),
			createCompnt(WelcomeReceiver.class		, READS),
			createCompnt(Actuators.Program.class	, WRITES_ASYNCLY),
			createCompnt(MapComponent.class			, READS, WRITES),	
			createCompnt(RemoteCallCompnt.class		, READS, WRITES)
		);
		
		for (BotComponent<?> c : components) {
			c.offerAsyncWriteStream(connection.getCmdOutStream());

			/* RemoteCall-Componente suchen und DoneListener registrieren (AblViewer) */
			if (c instanceof RemoteCallCompnt) {
				RemoteCallCompnt rc = (RemoteCallCompnt) c;			
				rc.addDoneListener(new Runnable1<BehaviorExitStatus>() {
					public void run(BehaviorExitStatus status) {
						if (ablResult != null) {
							ablResult.setSyntaxCheck(status == BehaviorExitStatus.SUCCESS);
						}
					}
				});	
			}
			
			if (c instanceof WelcomeReceiver) {
				welcomeReceiver = (WelcomeReceiver) c;
				welcomeReceiver.setFeatures(features);
			}
		}
		
		sendRcStartCode();
	}

	/**
	 * @see ctSim.model.bots.BasicBot#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Simulierter, in C geschriebener c't-Bot, verbunden über " + getConnection().getName();
	}

	/**
	 * Sendet einen Fernbedienungscode an den Bot
	 * @param code	zu sendender RC5-Code als String
	 */
	public void sendRC5Code(String code) {
		try {
			for (BotComponent<?> c : components) {
				if (c instanceof Sensors.RemoteControl) {
					((Sensors.RemoteControl)c).send(code);
					lg.fine("RC5Code für Taste \"" + code + "\" gesendet");
					break;
				}
			}
		} catch (IOException e) {
			// Kann nicht passieren, da die RC nur IOExcp wirft, wenn sie
			// asynchron betrieben wird, was CtBotSimTcp nicht macht
			throw new AssertionError(e);
		}		
	}
	
	/**
	 * Sendet den Fernbedienungs-(RC5-)Code, der in der Konfigdatei angegeben
	 * ist. Methode tut nichts, falls nichts, 0 oder ein nicht von
	 * {@link Integer#decode(String)} verwertbarer Code angegeben ist.
	 */
	public void sendRcStartCode() {
		String rcStartCode = Config.getValue("rcStartCode");
		sendRC5Code(rcStartCode);
	}

	/**
	 * @see ctSim.model.bots.SimulatedBot#doSimStep()
	 */
	public void doSimStep()
	throws InterruptedException, UnrecoverableScrewupException {
		transmitSensors();
		processUntilDoneCmd();
//		updateView(); // macht die ThreeDBot-Instanz bereits
	}

	/** Leite Sensordaten an den Bot weiter
	 * @throws UnrecoverableScrewupException */
	private synchronized void transmitSensors()
	throws UnrecoverableScrewupException {
		try {
			CommandOutputStream s = getConnection().getCmdOutStream();
			for (BotComponent<?> c : components) {
				c.askForWrite(s);
			}
			s.flush();
		} catch (IOException e) {
			throw new UnrecoverableScrewupException(e);
		}
	}
	
	/**
	 * Alle Kommandos verarbeiten
	 * @throws UnrecoverableScrewupException
	 */
	private void processUntilDoneCmd() throws UnrecoverableScrewupException {
		try {
			while (true) {
				try {
					Command cmd = new Command(getConnection());
					
					if (preProcessCommands(cmd)) {
						// Ist das Kommando schon abgearbeitet?
						continue;
					}
					
					components.processCommand(cmd);

					if (cmd.has(Command.Code.DONE)) {
						break;
					}
				} catch (ProtocolException e) {
					lg.warn(e, "Ungültiges Kommando; ignoriere");
				}
			}
		} catch (IOException e) {
			throw new UnrecoverableScrewupException(e);
		}
	}

	/**
	 * @return Simulator-Klasse
	 */
	public Class<? extends Runnable> getSimulatorType() {
		return MasterSimulator.class;
	}
}
