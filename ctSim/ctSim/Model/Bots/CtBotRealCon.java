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

package ctSim.Model.Bots;

import java.io.IOException;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.Connection;
import ctSim.ErrorHandler;
import ctSim.Controller.Controller;
import ctSim.Model.Command;

/**
 * Die Oberklasse fuer Repraesentationen aller Bots, die ausserhalb der Grenzen
 * des Simulators existieren und mit diesem ueber eine Connection kommunizieren.
 */
public class CtBotRealCon extends CtBotReal implements TcpBot {

	/** Die TCP-Verbindung */
	private Connection con;

	/** Der "Anrufbeantworter" fuer eingehende Kommandos */
	private AnsweringMachine answeringMachine;

	/** Sequenznummer der TCP-Pakete */
	int seq = 0;

	/**
	 * Erzeugt einen neuen Bot
	 * 
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 * @param tc
	 *            Kommunikationsverbindung
	 */
	public CtBotRealCon(Controller controller, Point3f pos, Vector3f head,
			Connection tc) {
		super(controller, pos, head);
		con = tc;
		answeringMachine = new AnsweringMachine(this, con);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#work()
	 */
	public void work() {
		super.work();
		// TODO noch zu implementieren
		// ErrorHandler.error("BotRealTcp.work is missing");
	}

	/**
	 * Wertet ein empfangenes Kommando aus
	 * 
	 * @param command
	 *            Das Kommando
	 */
	public void evaluate_command(Command command) {

		if (command.getDirection() == Command.DIR_REQUEST) {

			switch (command.getCommand()) {
			case Command.CMD_SENS_IR:
				setSensIrL(command.getDataL());
				setSensIrR(command.getDataR());
				break;
			case Command.CMD_SENS_ENC:
				setSensEncL((short) command.getDataL());
				setSensEncR((short) command.getDataR());
				break;
			case Command.CMD_SENS_BORDER:
				setSensBorderL(command.getDataL());
				setSensBorderR(command.getDataR());
				break;
			case Command.CMD_SENS_DOOR:
				setSensDoor(command.getDataL());
				break;
			case Command.CMD_SENS_LDR:
				setSensLdrL(command.getDataL());
				setSensLdrR(command.getDataR());
				break;
			case Command.CMD_SENS_LINE:
				setSensLineL(command.getDataL());
				setSensLineR(command.getDataR());
				break;
			case Command.CMD_SENS_MOUSE:
				setSensMouseDX(command.getDataL());
				setSensMouseDY(command.getDataR());
				break;
			case Command.CMD_SENS_TRANS:
				setSensTrans(command.getDataL());
				break;
			case Command.CMD_SENS_RC5:
				setSensRc5(command.getDataL());
				break;
			case Command.CMD_SENS_ERROR:
				setSensError(command.getDataL());
				break;

			case Command.CMD_AKT_MOT:
				setActMotL((short) command.getDataL());
				setActMotR((short) command.getDataR());
				break;
			case Command.CMD_AKT_SERVO:
				setActServo(command.getDataL());
				break;
			case Command.CMD_AKT_DOOR:
				setActDoor(command.getDataL());
				break;
			case Command.CMD_AKT_LED:
				setActLed(command.getDataL());
				break;
			case Command.CMD_ACT_LCD:
				switch (command.getSubcommand()) {
				case Command.SUB_CMD_NORM:
					setLcdText(command.getDataL(), command.getDataR(), 
							command.getDataBytesAsString());
					break;
				case Command.SUB_LCD_CURSOR:
					setCursor(command.getDataL(), command.getDataR());
					break;
				case Command.SUB_LCD_CLEAR:
					lcdClear();
					break;
				case Command.SUB_LCD_DATA:
					setLcdText(command.getDataBytesAsString());
					break;
				}
				break;
			case Command.CMD_SENS_MOUSE_PICTURE:
				// Empfangen eine Bildes
				setMousePicture(command.getDataL(),command.getDataBytes());
				break;
			default:
				ErrorHandler.error("Unknown Command:" + command.toString());
				break;
			}
			// System.out.println("Command: " + (char)command.getCommand());

			try {
				// tcpCon.send(answer.getCommandBytes());
			} catch (Exception ex) {
				ErrorHandler.error("Sending answer failed");
			}

		} else {
			// TODO: Antworten werden noch nicht gegeben
		}
	}

	/**
	 * Startet den Bot und den Thread fuer eingehende Kommandos
	 * 
	 * @see java.lang.Thread#start()
	 */
	public synchronized void start() {
		super.start();
		answeringMachine.start();

	}

	/* Fordert ein MaussensorBild an
	 * @see ctSim.Model.Bots.CtBot#requestMousePicture()
	 */
	public void requestMousePicture() {
		Command command = new Command(Command.CMD_SENS_MOUSE_PICTURE, 0, 0, seq++);
		try {
			con.send(command.getCommandBytes());
		} catch (IOException e) {
			ErrorHandler.error("Probleme bei der Bitte um ein neues Maussesnorbil: "+e);
		}
	}
	
	/*
	 * Aufraeumen
	 * 
	 * @see ctSim.Model.Bot#cleanup()
	 */
	protected void cleanup() {
		super.cleanup();
		try {
			if (con != null)
				con.disconnect();
			if (answeringMachine != null)
				answeringMachine.die();
			super.cleanup();
		} catch (Exception ex) {
		}
	}
}
