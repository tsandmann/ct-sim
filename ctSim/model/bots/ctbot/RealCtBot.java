package ctSim.model.bots.ctbot;

import static ctSim.model.bots.components.BotComponent.ConnectionFlags.READS;
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES_ASYNCLY;

import java.io.IOException;
import java.net.ProtocolException;

import ctSim.Connection;
import ctSim.model.Command;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.MousePictureComponent;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;
import ctSim.util.SaferThread;

public class RealCtBot extends CtBot {
	protected class CmdProcessor extends SaferThread {
		private final Connection connection;

		public CmdProcessor(final Connection connection) {
			super("ctSim-"+RealCtBot.this.toString());
			this.connection = connection;
		}

		@SuppressWarnings("synthetic-access") // Bei den zwei Logging-Aufrufen ist uns das wurst
		@Override
		public void work() throws InterruptedException {
			Command cmd = null;
			try {
				cmd = new Command(connection);
				components.processCommand(cmd);
				updateView(); //$$$ Jedesmal? Performance?
			} catch (ProtocolException e) {
				lg.warn(e, "Ung\u00FCltiges Kommando; ignoriere%s", cmd);
			} catch (IOException e) {
				lg.severe(e, "E/A-Problem; Verbindung zu Bot verloren");
				die();
			}
		}

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

	private final String connectionName;

	public RealCtBot(Connection connection) {
		super(connection.getShortName()+"-Bot");

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
			_(WelcomeReceiver.class      , READS)
		);

		for (BotComponent<?> c : components)
			c.offerAsyncWriteStream(connection.getCmdOutStream());

		final CmdProcessor cp = new CmdProcessor(connection);
		addDisposeListener(new Runnable() {
			public void run() {
				cp.die();
			}
		});
		cp.start();
	}

	@Override
	public String getDescription() {
		return "Realer c't-Bot, verbunden \u00FCber "+connectionName;
	}
}
