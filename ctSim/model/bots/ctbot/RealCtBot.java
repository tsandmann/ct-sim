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
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES_ASYNCLY;

import java.io.IOException;
import java.net.ProtocolException;
import ctSim.Connection;
import ctSim.model.Command;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.MapComponent;
import ctSim.model.bots.components.MousePictureComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;
import ctSim.model.bots.components.RemoteCallCompnt.BehaviorExitStatus;
import ctSim.util.BotID;
import ctSim.util.Runnable1;
import ctSim.util.SaferThread;

/**
 * Reale Bots
 */
public class RealCtBot extends CtBot {
	/**
	 * Kommando-Auswerter
	 */
	protected class CmdProcessor extends SaferThread {
		/** Verbindung zum Sim */
		private final Connection connection;

		/**
		 * @param connection Connection zum Bot
		 */
		public CmdProcessor(final Connection connection) {
			super("ctSim-"+RealCtBot.this.toString());
			this.connection = connection;
		}

		/**
		 * @see ctSim.util.SaferThread#work()
		 */
		@SuppressWarnings("synthetic-access") // Bei den zwei Logging-Aufrufen ist uns das wurst
		@Override
		public void work() throws InterruptedException {
			Command cmd = null;
			try {
				cmd = new Command(connection);
				if (cmd.has(Command.Code.DONE)) {
					updateView();
					return;
				}
				
				if (cmd.has(Command.Code.SHUTDOWN)) {
					die();
					RealCtBot.this.dispose();
					return;
				}
				
				if (!preProcessCommands(cmd)){
					components.processCommand(cmd);
				}
			} catch (ProtocolException e) {
				lg.warn(e, "Ung\u00FCltiges Kommando; ignoriere");
			} catch (IOException e) {
				lg.severe("E/A-Problem; Verbindung zu Bot verloren");
				die();
				RealCtBot.this.dispose();
			}
		}

		/**
		 * @see ctSim.util.SaferThread#dispose()
		 */
		@Override
		public void dispose() {
			try {
				connection.close();
			} catch (IOException e) {
				// Strategie auf Best-effort-Basis
				// (soll heissen ist uns wurscht)
			}
		}
	}
	
	/** Name der Verbindung */
	private final String connectionName;

	/**
	 * @param connection Connection zum Bot
	 * @param newId Id fuer die Kommunikation 
	 * @throws ProtocolException 
	 */
	public RealCtBot(Connection connection, BotID newId) throws ProtocolException {
		super(connection.getShortName()+"-Bot");
		
		// connection speichern
		setConnection(connection);
		
		setId(newId);
		
		connectionName = connection.getName();

		components.add(
			new MousePictureComponent(),
			new WelcomeReceiver(Command.SubCode.WELCOME_REAL)
		);

		components.applyFlagTable(
			_(MousePictureComponent.class, READS, WRITES_ASYNCLY),
			_(Actuators.Governor.class   , READS),
			_(Actuators.LcDisplay.class  , READS),
			_(Actuators.Log.class        , READS),
			_(Actuators.DoorServo.class  , READS),
			_(Actuators.Led.class        , READS),
			_(Sensors.Encoder.class      , READS),
			_(Sensors.Distance.class     , READS),
			_(Sensors.Line.class         , READS),
			_(Sensors.Border.class       , READS),
			_(Sensors.Light.class        , READS),
			_(Sensors.Mouse.class        , READS),
			_(Sensors.RemoteControl.class, WRITES_ASYNCLY),
			_(Sensors.Door.class         , READS),
			_(Sensors.Trans.class        , READS),
			_(Sensors.Error.class        , READS),
			_(Sensors.BPSReceiver.class  , READS),
			_(Sensors.Shutdown.class     , READS, WRITES_ASYNCLY),
			_(WelcomeReceiver.class      , READS),
			_(Actuators.Program.class		 , WRITES_ASYNCLY),
			_(MapComponent.class		 , READS, WRITES_ASYNCLY),
			_(RemoteCallCompnt.class     , READS, WRITES_ASYNCLY)
		);

		for (BotComponent<?> c : components) {
			c.offerAsyncWriteStream(connection.getCmdOutStream());
			
			/* RemoteCall-Componente suchen und DoneListener registrieren (ProgramViewer) */
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

		// Und einen CommandProcessor herstellen
		final CmdProcessor cp = new CmdProcessor(connection);
		addDisposeListener(new Runnable() {
			public void run() {
				cp.die();
			}
		});
		cp.start();
	}

	/**
	 * @see ctSim.model.bots.BasicBot#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Realer c't-Bot, verbunden \u00FCber "+connectionName;
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
			e.printStackTrace();
		}		
	}	
	
//	/**
//	 * Startet das Verhalten "name" per RemoteCall
//	 * @param name	Das zu startende Verhalten
//	 * @param param	Int-Parameter fuer das Verhalten (16 Bit)
//	 * @param ref	Referenz auf den Program-Viewer, falls das Ergebnis dort angezeigt werden soll
//	 */
//	public void startRemoteCall(String name, int param, ProgramViewer ref) {
//		for (BotComponent<?> c : components) {
//			if (c instanceof RemoteCallCompnt) {
//				try {
//					ablResult = ref;
//					RemoteCallCompnt rc = (RemoteCallCompnt)c;
//					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//					bytes.write(name.getBytes());
//					bytes.write(0);
//					RemoteCallCompnt.Behavior beh = rc.new Behavior(bytes.toByteArray());
//					RemoteCallCompnt.Parameter par = new RemoteCallCompnt.IntParam("uint16 x");
//					par.setValue(param);
//					beh.getParameters().add(par);					
//					beh.call();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//	}	
}
