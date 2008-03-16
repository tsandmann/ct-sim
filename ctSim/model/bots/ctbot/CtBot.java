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
import ctSim.model.bots.components.RemoteCallCompnt;
import ctSim.model.bots.components.Sensors;

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
	 * @param cmd
	 * @return True, Wenn das Kommando abgearbeitet wurde, sonst False
	 * @throws IOException 
	 */
	protected boolean preProcessCommands(Command cmd) throws IOException{
		// TODO: Ist das sinnvoll von einem Welcome die ID zu übernehmen????
		if (cmd.has(Command.Code.ID)){	// Von einem Welcome nehmen wir sicherheitshalber erstmal die ID an.
			lg.info("Nehme für Bot "+getDescription()+" erstmal die ID des Welcome-Paketes:"+cmd.getFrom());
			setId(cmd.getFrom());
		}
		
		if (cmd.has(Command.Code.ID)){
			// Will der Bot seine ID selbst setzen?
			if (cmd.getSubCode() == Command.SubCode.ID_SET){
				lg.info("Bot "+getDescription()+" setzt seine ID selbst auf:"+cmd.getDataL());
				setId((byte)cmd.getDataL());
				return true;
			}
			
			// Will der Bot eine ID aus dem Pool?
			if (cmd.getSubCode() == Command.SubCode.ID_REQUEST){
				lg.info("Bot ("+getDescription()+") fordert eine ID aus dem Pool an");

				
				byte newId= getController().generateBotId();
				
				Command answer = getConnection().getCmdOutStream().getCommand(Command.Code.ID);
				answer.setSubCmdCode(Command.SubCode.ID_OFFER);
				answer.setDataL(newId); // Die neue kommt in das Datenfeld
				getConnection().getCmdOutStream().flush(); // Und raus damit
				
				lg.info("Schlage Bot die Adresse "+newId+" vor");
				return true;
			}
		}

		if (cmd.getFrom() != getId()){
			lg.warn("Nachricht von einem unerwarteten Absender ("+cmd.getFrom()+") erhalten. Erwartet: "+getId());
			return true;
		}

		if (cmd.getTo() != Command.SIM_ID) {
			// Diese Nachricht ist nicht fuer den Sim, sondern fuer einen anderen Bot
			// Also weiterleiten
			Controller controller =	this.getController();
			
			if (controller != null) 
				controller.deliverMessage(cmd);
			else {
				throw new ProtocolException("Nachricht empfangen, die an einen anderen Bot (Id="
								+cmd.getTo()+
								") gehen sollte. Habe aber keinen Controller!");
			}
			//	lg.warn(cmd.toString());
			//	lg.warn("Nachricht empfangen, die an einen anderen Bot (Id="
			//		+cmd.getTo()+
			//		") gehen sollte. Weiterleitungen noch nicht implementiert!");
			//throw new ProtocolException("Nachricht empfangen, die an einen anderen Bot (Id="
			//		+cmd.getTo()+
			//		") gehen sollte. Weiterleitungen noch nicht implementiert!");
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
		if (command.getTo() != this.getId())
			throw new ProtocolException("Bot "+this.getId()+" hat ein Kommando "+command.toCompactString()+" empfangen, dass nicht für ihn ist");
		
		if (getConnection() == null)
			throw new ProtocolException("Bot "+this.getId()+" hat gar keine Connection");
		
		//Wir werfen das Kommando direkt an den angehängten Bot
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
			new Sensors.Light(true),
			new Sensors.Light(false),
			new Sensors.Mouse(true),
			new Sensors.Mouse(false),
			new Sensors.RemoteControl(Config.getValue("RC5-type")),
			new Sensors.Door(),
			new Sensors.Trans(),
			new Sensors.Error(),
			//new Actuators.Abl(),
			new RemoteCallCompnt()
		);

		// LEDs
		int numLeds = ledColors.length;
		for (int i = 0; i < numLeds; i++) {
			String ledName = "LED " + (i + 1)
					 + (i == 0 ? " (vorn rechts)" :
						i == 1 ? " (vorn links)" : "");
			components.add(
				new Actuators.Led(ledName, i, ledColors[i]));
		}
	}


}
