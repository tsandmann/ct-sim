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

import java.awt.Color;
import java.io.IOException;
import java.net.ProtocolException;

import ctSim.controller.Config;
import ctSim.controller.Controller;
import ctSim.model.Command;
import ctSim.model.bots.BasicBot;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.MapComponent;
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;
import ctSim.util.BotID;

/**
 * Abstrakte Oberklasse fuer alle c't-Bots
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

	/** Hoehe des Bots [m] */
	protected static final double BOT_HEIGHT = 0.120d;

	/** Bodenfreiheit des Bots [m] */
	protected static final double BOT_GROUND_CLEARANCE = 0.015d;
	
	/**
	 * Vorverarbeitung der Kommandos 
	 * z.B. Weiterleiten von Kommandos für andere Bots
	 * Adressvergabe, etc.
	 * @param cmd das Kommando
	 * @return True, Wenn das Kommando abgearbeitet wurde, sonst False
	 * @throws IOException falls Output-Stream.flush() fehlschlaegt
	 * @throws ProtocolException falls kein Controller vorhanden zum Weiterleiten
	 */
	protected boolean preProcessCommands(Command cmd) throws IOException,
			ProtocolException {
		BotID id = cmd.getFrom();
		if (cmd.has(Command.Code.WELCOME)) { // Von einem Welcome nehmen wir
			// sicherheitshalber erstmal die ID an.
			lg.info("Nehme für Bot " + toString()
					+ " erstmal die ID des Welcome-Paketes:"
					+ id);
			try {
				setId(id);
			} catch (ProtocolException e) {
				lg.warn("ID " + id
						+ " konnte nicht gesetzt werden");
			}
			return false;
		}

		if (cmd.has(Command.Code.ID)) {
			// Will der Bot seine ID selbst setzen?
			if (cmd.getSubCode() == Command.SubCode.ID_SET) {
				BotID newId = new BotID(cmd.getDataL());
				lg.info("Bot " + toString() + " setzt seine ID selbst auf:"
						+ id);
				try {
					setId(newId);
				} catch (ProtocolException e) {
					lg.warn("ID " + newId
							+ " konnte nicht gesetzt werden");
				}
				return true;
			}

			// Will der Bot eine ID aus dem Pool?
			if (cmd.getSubCode() == Command.SubCode.ID_REQUEST) {
				lg.info("Bot (" + toString()
						+ ") fordert eine ID aus dem Pool an");

				//TODO:	unschoene Loesung
				while (getController() == null) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// NOP
					}
				}
				BotID newId = getController().generateBotId();

				Command answer = getConnection().getCmdOutStream().getCommand(
						Command.Code.ID);
				answer.setSubCmdCode(Command.SubCode.ID_OFFER);
				answer.setDataL(newId.intValue()); // Die neue kommt in das Datenfeld
				getConnection().getCmdOutStream().flush(); // Und raus damit

				lg.info("Schlage Bot die Adresse " + newId + " vor");
				return true;
			}
		}

		if (!cmd.getFrom().equals(getId())) {
			if (cmd.getFrom().equals(Command.getBroadcastId())) {
				lg.warn("Nachricht mit Broadcast-ID als Absender "
						+ " erhalten, ignoriere Absender");
				return false;
			}
			lg.warn("Nachricht von einem unerwarteten Absender ("
					+ cmd.getFrom() + ") erhalten. Erwartet: "
					+ getId());
			return true;
		}

		if (!cmd.getTo().equals(Command.getSimId())) {
			lg.info("Nachricht ist fuer " + cmd.getTo());
			// Diese Nachricht ist nicht fuer den Sim, sondern fuer einen
			// anderen Bot
			// Also weiterleiten
			Controller controller = this.getController();

			if (controller != null) {
				if (cmd.getFrom().equals(Command.getBroadcastId())) {
					// ungueltiger Absender => Paket verwerfen
					return true;
				}
				controller.deliverMessage(cmd);
			} else {
				throw new ProtocolException(
						"Nachricht empfangen, die an einen anderen Bot (Id="
								+ cmd.getTo()
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
			throw new ProtocolException("Bot "+ this.getId() +" hat ein Kommando "+command.toCompactString()+" empfangen, dass nicht für ihn ist");
		
		if (getConnection() == null)
			throw new ProtocolException("Bot "+ this.getId() +" hat gar keine Connection");
		
		// Wir werfen das Kommando direkt an den angehaengten Bot
		try{ 
			getConnection().write(command);
		} catch (IOException e) {
			lg.warn("Es gab Probleme beim Erreichen des Bots");
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
			new Actuators.DoorServo(),
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
			new Sensors.Door(),
			new Sensors.Trans(),
			new Sensors.Error(),
			//new Actuators.Abl(),
			new MapComponent(),
			new RemoteCallCompnt()
		);
		
		if (this instanceof RealCtBot ||  Config.getValue("LightSensors").equals("true")) {
			components.add(new Sensors.Light(true), new Sensors.Light(false));
		}
		if (this instanceof RealCtBot || Config.getValue("BPSSensors").equals("true")) {
			components.add(new Sensors.BPSReceiver(true), new Sensors.BPSReceiver(false));
		}

		// LEDs
		int numLeds = ledColors.length;
		for (int i = 0; i < numLeds; i++) {
			String ledName = "LED " + (i + 1)
					 + (i == 1 ? " (vorn rechts)" :
						i == 0 ? " (vorn links)" : "");
			components.add(
				new Actuators.Led(ledName, i, ledColors[i]));
		}
	}


}
