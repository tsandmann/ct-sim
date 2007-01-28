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

import java.awt.Color;
import java.io.IOException;
import java.net.ProtocolException;

import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.Connection;
import ctSim.controller.Config;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.World;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.MousePictureComponent;
import ctSim.model.bots.components.NumberTwin;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;
import ctSim.util.FmtLogger;

/**
 * Klasse aller simulierten c't-Bots, die ueber TCP mit dem Simulator
 * kommunizieren
 */
public class CtBotSimTcp extends CtBotSim {
	private static final Color[] ledColors = {
		new Color(  0,  84, 255), // blau
		new Color(  0,  84, 255), // blau
		Color.RED,
		new Color(255, 200,   0), // orange
		Color.YELLOW,
		Color.GREEN,
		new Color(  0, 255, 210), // tuerkis
		Color.WHITE,
	};

	final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.model.bots.ctbot.CtBotSimTcp");

	/** Die TCP-Verbindung */
	private final Connection connection;

	private World world; //$$$ final 

	private final MasterSimulator masterSimulator;

	///////////////////////////////////////////////////////////////////////////

	/**
	 * @param w Die Welt
	 * @param pos Position
	 * @param head Blickrichtung
	 * @param con Verbindung
	 */
	public CtBotSimTcp(World w, Point3d pos, Vector3d head, Connection con) {
		super(w, "Sim-Bot", pos, head);
		this.connection = con;
		this.world = w;

		components.add(
			new MousePictureComponent(),
			new Actuators.Governor(true),
			new Actuators.Governor(false),
			new Actuators.LcDisplay(20, 4),
			new Actuators.Log(),
			new Actuators.DoorServo(),
			new Sensors.Clock(),
			new Sensors.Encoder(true),
			new Sensors.Encoder(false),
			new Sensors.Distance(true),
			new Sensors.Distance(false),
			new Sensors.Line(true),
			new Sensors.Line(false),
			new Sensors.Border(true),
			new Sensors.Border(false),
			new Sensors.Light(true),
			new Sensors.Light(false),
			new Sensors.Mouse(true),
			new Sensors.Mouse(false),
			new Sensors.RemoteControl(),
			new Sensors.Door(),
			new Sensors.Trans(),
			new Sensors.Error(),
			new WelcomeReceiver(Command.SubCode.WELCOME_SIM)
		);

		// LEDs
		int numLeds = ledColors.length;
		for (int i = 0; i < numLeds; i++) {
			String s = "LED " + (i + 1)
					 + (i == 0 ? " (vorn rechts)" :
						i == 1 ? " (vorn links)" : "");
			components.add(new Actuators.Led(s, numLeds - i - 1, ledColors[i]));
		}

		// Component-Flag-Tabelle
		components.applyFlagTable(
			_(MousePictureComponent.class),
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
			_(WelcomeReceiver.class      , READS)
		);

		// Simulation
		masterSimulator = new MasterSimulator(world, this);
		for (BotComponent<?> c : components) {
			c.acceptCompntVisitor(masterSimulator);
			if (c instanceof NumberTwin)
				((NumberTwin)c).acceptNumTwinVisitor(masterSimulator);
		}

		sendRcStartCode();
	}

	@Override
	public String getDescription() {
		return "Simulierter, in C geschriebener c't-Bot";
	}

	/**
	 * Sendet den Fernbedienungs-(RC5-)Code, der in der Konfigdatei angegeben
	 * ist. Methode tut nichts, falls nichts, 0 oder ein nicht von
	 * {@link Integer#decode(String)} verwertbarer Code angegeben ist.
	 */
	private void sendRcStartCode() {
		String rawStr = Config.getValue("rcStartCode");
		try {
			int rcStartCode = Integer.decode(rawStr);
			for (BotComponent<?> c : components) {
				if (c instanceof Sensors.RemoteControl) {
					lg.fine("Sende RC5-Code %d (%#x) an %s",
						rcStartCode, rcStartCode, getName());
					((Sensors.RemoteControl)c).send(rcStartCode);
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

	/** Leite Sensordaten an den Bot weiter */
	private synchronized void transmitSensors() {
		try {
			CommandOutputStream s = connection.getCmdOutStream();
			for (BotComponent<?> c : components)
				c.askForWrite(s);
			s.flush();
		} catch (IOException e) {
			lg.severe(e, "E/A-Problem beim Senden der Sensordaten; sterbe");
			die();
		}
	}

	/**
	 * Wertet ein empfangenes Kommando aus
	 *
	 * @param command Das Kommando
	 */
	public void evaluateCommand(Command command) throws ProtocolException {
		if (command.getDirection() != Command.DIR_REQUEST) {
			throw new ProtocolException("Kommando ist Unfug: Hat als " +
					"Richtung nicht 'Anfrage'; ignoriere");
		}

		for (BotComponent<?> c : components)
			c.offerRead(command);
		if (! command.hasBeenProcessed())
			throw new ProtocolException("Unbekanntes Kommando");
	}

	@Override
	protected void work() {
		transmitSensors();
		readAndProcessCommands();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@SuppressWarnings("synthetic-access")
				public void run() {
					for (BotComponent<?> c : components)
						c.askToUpdateExternalModel();
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Hier erfolgt die Aktualisierung der gesamten Simulation
	 *
	 * @see ctSim.model.AliveObstacle#updateSimulation(long)
	 * @param simulTime
	 */
	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime);
		masterSimulator.run();
	}

	private void readAndProcessCommands() {
		try {
			while (true) {
				try {
					Command cmd = new Command(connection);
					evaluateCommand(cmd);
					if (cmd.has(Command.Code.DONE))
						break;
				} catch (ProtocolException e) {
					lg.warn(e, "Ung\u00FCltiges Kommando; ignoriere");
				}
			}
		} catch (IOException e) {
			lg.severe(e, "E/A vermurkst: Verbindung unterbrochen; Bot " +
					"steckengeblieben");
			set(ObstState.HALTED);
		}
	}

	/** Erweitert die() um das Schliessen der TCP-Verbindung */
	@Override
	public void die() {
		super.die();
		try {
			connection.close();
		} catch (IOException e) {
			// uninteressant
		}
	}

	//$$ Unterschied cleanup() und die()? Zusammenfassen (Death-Listener)
	@Override
	protected void cleanup() {
		super.cleanup();
		world = null;
	}
}