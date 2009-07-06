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
//import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES_ASYNCLY;

import java.io.ByteArrayOutputStream;
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
import ctSim.view.gui.AblViewer;

/**
 * Klasse aller simulierten c't-Bots, die ueber TCP mit dem Simulator
 * kommunizieren
 */
public class CtBotSimTcp extends CtBot implements SimulatedBot {
	///** Die TCP-Verbindung */
    //private final Connection connection;
    /** Referenz eines AblViewer, der bei bei Bedarf ein RemoteCall-Ergebnis anzeigen kann */
	private AblViewer ablResult;

	/**
	 * @param connection Verbindung
	 * @param newId Id für die Kommunikation 
	 * @throws ProtocolException 
	 */
	public CtBotSimTcp(final Connection connection, BotID newId) throws ProtocolException {
		super("Sim-Bot");
		
		if (connection == null) 
			throw new ProtocolException("Connection ist null");
		
		setConnection(connection);
		lg.info("Id ist erstmal "+newId);
		setId(newId);
		this.ablResult = null;

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
			//_(Actuators.Abl.class		 , WRITES_ASYNCLY),
			_(MapComponent.class		 , READS, WRITES),	
			_(RemoteCallCompnt.class     , READS, WRITES)
		);
		
		for (BotComponent<?> c : components) {
			c.offerAsyncWriteStream(connection.getCmdOutStream());

			/* RemoteCall-Componente suchen und DoneListener registrieren (AblViewer) */
			if (c instanceof RemoteCallCompnt) {
				RemoteCallCompnt rc = (RemoteCallCompnt)c;			
				rc.addDoneListener(new Runnable1<BehaviorExitStatus>() {
					public void run(BehaviorExitStatus status) {
						if (ablResult != null) {
							ablResult.setSyntaxCheck(status == BehaviorExitStatus.SUCCESS);
						}
					}
				});	
			}
		}
		
		sendRcStartCode();
	}

	/**
	 * @see ctSim.model.bots.BasicBot#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Simulierter, in C geschriebener c't-Bot, verbunden \u00FCber "+
			getConnection().getName();
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
					lg.fine("RC5Code fuer Taste \"" + code + "\" gesendet");
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
	 * Sendet den RC5-Code, um ein ABL-Programm zu starten
	 */
	public void startABL() {
		lg.info("Starte ABL-Programm auf dem Bot...");
		sendRC5Code(">");	
	}
	
	/**
	 * Startet das Verhalten "name" per RemoteCall
	 * @param name	Das zu startende Verhalten
	 * @param param	Int-Parameter fuer das Verhalten (16 Bit)
	 * @param ref	Referenz auf den ABL-Viewer, falls das Ergebnis dort angezeigt werden soll
	 */
	public void startRemoteCall(String name, int param, AblViewer ref) {
		for (BotComponent<?> c : components) {
			if (c instanceof RemoteCallCompnt) {
				try {
					ablResult = ref;
					RemoteCallCompnt rc = (RemoteCallCompnt)c;
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					bytes.write(name.getBytes());
					bytes.write(0);
					RemoteCallCompnt.Behavior beh = rc.new Behavior(bytes.toByteArray());
					RemoteCallCompnt.Parameter par = new RemoteCallCompnt.IntParam("uint16 x");
					par.setValue(param);
					beh.getParameters().add(par);
					beh.call();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * @see ctSim.model.bots.SimulatedBot#doSimStep()
	 */
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
					
					if (preProcessCommands(cmd))	// Ist das Kommando schon abgearbeitet?
						continue;
					
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

	/**
	 * @return Simulator-Klasse
	 */
	public Class<? extends Runnable> getSimulatorType() {
		return MasterSimulator.class;
	}
}
