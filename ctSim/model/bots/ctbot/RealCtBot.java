package ctSim.model.bots.ctbot;

import static ctSim.model.bots.components.BotComponent.ConnectionFlags.READS;
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES_ASYNCLY;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ProtocolException;

import ctSim.Connection;
import ctSim.model.Command;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.MousePictureComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;
import ctSim.model.bots.components.RemoteCallCompnt.BehaviorExitStatus;
import ctSim.util.Runnable1;
import ctSim.util.SaferThread;
import ctSim.view.gui.AblViewer;

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
				components.processCommand(cmd);
				updateView(); //$$$ Jedesmal? Performance?
			} catch (ProtocolException e) {
				lg.warn(e, "Ung\u00FCltiges Kommando; ignoriere%s", cmd);
			} catch (IOException e) {
				lg.severe(e, "E/A-Problem; Verbindung zu Bot verloren");
				die(); //$$ Das klappt nicht. Wenn die Verbindung abreißt, läuft alles weiter. Untersuchen
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
	/** Referenz eines AblViewer, der bei bei Bedarf ein RemoteCall-Ergebnis anzeigen kann */
	private AblViewer ablResult;

	/**
	 * @param connection Connection zum Bot
	 */
	public RealCtBot(Connection connection) {
		super(connection.getShortName()+"-Bot");

		connectionName = connection.getName();
		this.ablResult = null;

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
			_(WelcomeReceiver.class      , READS),
			//_(Actuators.Abl.class		 , WRITES_ASYNCLY),
			_(RemoteCallCompnt.class     , READS, WRITES_ASYNCLY)
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
				if ((Object)c instanceof Sensors.RemoteControl) {
					((Sensors.RemoteControl)((Object)c)).send(code);
					lg.fine("RC5Code fuer Taste \"" + code + "\" gesendet");
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
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
}
