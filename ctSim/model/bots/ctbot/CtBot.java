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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ProtocolException;
import ctSim.controller.Config;
import ctSim.controller.Controller;
import ctSim.model.Command;
import ctSim.model.bots.BasicBot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.MapComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.WelcomeReceiver;
import ctSim.util.BotID;
import ctSim.view.gui.ProgramViewer;

/**
 * Abstrakte Oberklasse für alle c't-Bots
 */
public abstract class CtBot extends BasicBot {
	
	/** LED-Farben */
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

	/** Abstand vom Zentrum zur Aussenkante des Bots [m] */
	protected static final double BOT_RADIUS = 0.060d;

	/** Höhe des Bots [m] */
	protected static final double BOT_HEIGHT = 0.120d;

	/** Bodenfreiheit des Bots [m] */
	protected static final double BOT_GROUND_CLEARANCE = 0.015d;
	
    /** Referenz eines ProgramViewers, der bei bei Bedarf ein RemoteCall-Ergebnis anzeigen kann */
	protected ProgramViewer ablResult;
	
	/** Referenz zum Welcome-Receiver */
	protected WelcomeReceiver welcomeReceiver = null;
	
	/**
	 * Vorverarbeitung der Kommandos 
	 * z.B. Weiterleiten von Kommandos für andere Bots
	 * Adressvergabe, etc.
	 * @param cmd das Kommando
	 * @return True, Wenn das Kommando abgearbeitet wurde, sonst False
	 * @throws IOException falls Output-Stream.flush() fehlschlägt
	 * @throws ProtocolException falls kein Controller vorhanden zum Weiterleiten
	 */
	protected boolean preProcessCommands(Command cmd) throws IOException, ProtocolException {
		BotID id = cmd.getFrom();
		if (cmd.has(Command.Code.WELCOME)) {
			// Von einem Welcome nehmen wir sicherheitshalber erstmal die ID an
			lg.info("Nehme für Bot " + toString() + " erstmal die ID des Welcome-Paketes:"	+ id);
			try {
				setId(id);
			} catch (ProtocolException e) {
				lg.warn("ID " + id + " konnte nicht gesetzt werden");
			}
			return false;
		}

		if (cmd.has(Command.Code.ID)) {
			// Will der Bot seine ID selbst setzen?
			if (cmd.getSubCode() == Command.SubCode.ID_SET) {
				BotID newId = new BotID(cmd.getDataL());
				lg.info("Bot " + toString() + " setzt seine ID selbst auf:"	+ id);
				try {
					setId(newId);
				} catch (ProtocolException e) {
					lg.warn("ID " + newId + " konnte nicht gesetzt werden");
				}
				return true;
			}

			// Will der Bot eine ID aus dem Pool?
			if (cmd.getSubCode() == Command.SubCode.ID_REQUEST) {
				lg.info("Bot (" + toString() + ") fordert eine ID aus dem Pool an");

				while (getController() == null) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// NOP
					}
				}
				BotID newId = getController().generateBotId();

				Command answer = getConnection().getCmdOutStream().getCommand(Command.Code.ID);
				answer.setSubCmdCode(Command.SubCode.ID_OFFER);
				answer.setDataL(newId.intValue()); // Die neue kommt in das Datenfeld
				getConnection().getCmdOutStream().flush(); // Und raus damit

				lg.info("Schlage Bot die Adresse " + newId + " vor");
				return true;
			}
		}

		if (!cmd.getFrom().equals(getId())) {
			if (cmd.getFrom().equals(Command.getBroadcastId())) {
				lg.warn("Nachricht mit Broadcast-ID als Absender erhalten, ignoriere Absender");
				return false;
			}
			lg.warn("Nachricht von einem unerwarteten Absender (" + cmd.getFrom() + ") erhalten. Erwartet: " + getId());
			return true;
		}

		if (!cmd.getTo().equals(Command.getSimId())) {
			lg.info("Nachricht ist für " + cmd.getTo());
			// Diese Nachricht ist nicht für den Sim, sondern für einen anderen Bot
			// Also weiterleiten
			Controller controller = getController();

			if (controller != null) {
				if (cmd.getFrom().equals(Command.getBroadcastId())) {
					// ungueltiger Absender => Paket verwerfen
					return true;
				}
				controller.deliverMessage(cmd);
			} else {
				throw new ProtocolException("Nachricht empfangen, die an einen anderen Bot (Id=" + cmd.getTo()
					+ ") gehen sollte. Habe aber keinen Controller!");
			}
			return true;
		}
		return false;
	}

	
	/**
	 * Verarbeitet ein Kommando und leitet es an den angehängten Bot weiter
	 * @param command das Kommando
	 * @throws ProtocolException Wenn was nicht klappt
	 */
	public void receiveCommand(Command command) throws ProtocolException {
		if (!command.getTo().equals(this.getId()) && !command.getTo().equals(Command.getBroadcastId()))
			throw new ProtocolException("Bot " + getId() + " hat ein Kommando "+command.toCompactString() + " empfangen, dass nicht für ihn ist");
		
		if (getConnection() == null) {
			throw new ProtocolException("Bot " + getId() + " hat gar keine Connection");
		}
		
		// Wir werfen das Kommando direkt an den angehängten Bot
		try { 
			getConnection().write(command);
		} catch (IOException e) {
			lg.warn("Es gab Probleme beim Erreichen des Bots");
		}
	}
	
	/**
	 * Startet das Verhalten "name" per RemoteCall
	 * @param name	Das zu startende Verhalten
	 * @param param	Int-Parameter für das Verhalten (16 Bit)
	 * @param ref	Referenz auf den ABL-Viewer, falls das Ergebnis dort angezeigt werden soll
	 */
	public void startRemoteCall(String name, int param, ProgramViewer ref) {
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
	 * Neuer Bot "name"
	 * @param name
	 */
	public CtBot(String name) {
		super(name);

		components.add(
			new Actuators.Governor(true),
			new Actuators.Governor(false),
			new Actuators.LcDisplay(20, 4),
			new Actuators.Log(),
			new Actuators.DoorServo(true),
			new Actuators.CamServo(false),
			new Sensors.Encoder(true),
			new Sensors.Encoder(false),
			new Sensors.Distance(true),
			new Sensors.Distance(false),
			new Sensors.Line(true),
			new Sensors.Line(false),
			new Sensors.Border(true),
			new Sensors.Border(false),
			new Sensors.Mouse(true),
			new Sensors.Mouse(false),
			new Sensors.RemoteControl(Config.getValue("RC5-type")),
			new Sensors.Door(true),
			new Sensors.CamPos(true),
			new Sensors.Trans(true),
			new Sensors.Error(),
			new Sensors.Shutdown(),
			new Actuators.Program(),
			new MapComponent(),
			new RemoteCallCompnt()
		);
		
		if (this instanceof RealCtBot || Config.getValue("LightSensors").equals("true")) {
			components.add(new Sensors.Light(true), new Sensors.Light(false));
		}
		if (this instanceof RealCtBot || Config.getValue("BPSSensor").equals("true")) {
			components.add(new Sensors.BPSReceiver(true));
		}

		// LEDs
		int numLeds = ledColors.length;
		for (int i = 0; i < numLeds; i++) {
			String ledName = "LED " + (i + 1) + (i == 1 ? " (vorn rechts)" : i == 0 ? " (vorn links)" : "");
			components.add(new Actuators.Led(ledName, i, ledColors[i]));
		}
		
		addDisposeListener(new Runnable() {
			public void run() {
				sendShutdown();
			}
		});
		
		ablResult = null;
	}

	/**
	 * Sendet einen Shutdown-Befehl zum Bot
	 */
	protected void sendShutdown() {
		try {
			for (BotComponent<?> c : components) {
				if (c instanceof Sensors.Shutdown) {
					((Sensors.Shutdown) c).sendShutdown();
					lg.fine("Shutdown-Befehl gesendet");
					break;
				}
			}
		} catch (IOException e) {
			// NOP
		}
	}
	
	/**
	 * @see ctSim.model.bots.Bot#get_feature_log()
	 */
	public boolean get_feature_log() {
		try {
			return welcomeReceiver.get_feature_log();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @see ctSim.model.bots.Bot#get_feature_rc5()
	 */
	public boolean get_feature_rc5() {
		try {
			return welcomeReceiver.get_feature_rc5();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @see ctSim.model.bots.Bot#get_feature_abl_program()
	 */
	public boolean get_feature_abl_program() {
		try {
			return welcomeReceiver.get_feature_abl_program();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_basic_program()
	 */
	public boolean get_feature_basic_program() {
		try {
			return welcomeReceiver.get_feature_basic_program();
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * @see ctSim.model.bots.Bot#get_feature_map()
	 */
	public boolean get_feature_map() {
		try {
			return welcomeReceiver.get_feature_map();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_remotecall()
	 */
	public boolean get_feature_remotecall() {
		try {
			return welcomeReceiver.get_feature_remotecall();
		} catch (Exception e) {
			return false;
		}
	}
}
