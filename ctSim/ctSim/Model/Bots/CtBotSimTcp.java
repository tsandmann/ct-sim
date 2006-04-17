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

import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.Controller.Controller;
import ctSim.Model.Command;

/**
 * Simulierter Bot, der Daten per TCP/IP- empfaengt und ausliefert
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */
public class CtBotSimTcp extends CtBotSim implements TcpBot {

	/** Die TCP-Verbindung */
	private TcpConnection tcpCon;

	/** Der "Anrufbeantworter" fuer eingehende Kommandos */
	private AnsweringMachine answeringMachine;

	/**
	 * Erzeugt einen neuen BotSimTcp mit einer schon bestehenden Verbindung
	 * 
	 * @param tc
	 *            Die Verbindung fuer den Bot
	 */
	public CtBotSimTcp(Controller controller, Point3f pos, Vector3f head,
			TcpConnection tc) {
		super(controller, pos, head);
		tcpCon = (TcpConnection) tc;
		answeringMachine = new AnsweringMachine(this, tcpCon);
	}

	/** Sequenznummer der TCP-Pakete */
	int seq = 0;

	/**
	 * Leite Sensordaten an den Bot weiter
	 */
	private synchronized void transmitSensors() {
		try {
			Command command = new Command(Command.CMD_SENS_IR, getSensIrL(),
					getSensIrR(), seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_ENC);
			command.setDataL(getSensEncL());
			command.setDataR(getSensEncR());
			setSensEncL((short) 0);
			setSensEncR((short) 0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_BORDER);
			command.setDataL(getSensBorderL());
			command.setDataR(getSensBorderR());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_DOOR);
			command.setDataL(getSensDoor());
			command.setDataR(0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_LDR);
			command.setDataL(getSensLdrL());
			command.setDataR(getSensLdrR());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_LINE);
			command.setDataL(getSensLineL());
			command.setDataR(getSensLineR());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_MOUSE);
			command.setDataL(getSensMouseDX());
			command.setDataR(getSensMouseDY());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_TRANS);
			command.setDataL(getSensTrans());
			command.setDataR(0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			if (getSensRc5() != 0) {
				command.setCommand(Command.CMD_SENS_RC5);
				command.setDataL(getSensRc5());
				command.setDataR(42);
				command.setSeq(seq++);
				tcpCon.send(command.getCommandBytes());
				setSensRc5(0);
			}

			command.setCommand(Command.CMD_SENS_ERROR);
			command.setDataL(getSensError());
			command.setDataR(0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

		} catch (IOException IoEx) {
			ErrorHandler.error("Error during sending Sensor data, dieing: "
					+ IoEx);
			die();
		}
	}

	/**
	 * Wertet ein empfangenes Kommando aus
	 * 
	 * @param command
	 *            Das Kommando
	 */
	public void evaluate_command(Command command) {
		Command answer = new Command();

		if (command.getDirection() == Command.DIR_REQUEST) {
			// Antwort vorbereiten
			answer.setDirection(Command.DIR_ANSWER);
			answer.setCommand(command.getCommand());
			answer.setSubcommand(command.getSubcommand());
			answer.setSeq(command.getSeq());

			switch (command.getCommand()) {
			case Command.CMD_SENS_IR:
				answer.setDataL(this.getSensIrL());
				answer.setDataR(this.getSensIrR());
				break;
			case Command.CMD_SENS_ENC:
				answer.setDataL(this.getSensEncL());
				this.setSensEncL((short) 0); // nach Uebertragung aufraeumen
				answer.setDataR(this.getSensEncR());
				this.setSensEncR((short) 0); // nach Uebertragung aufraeumen
				break;
			case Command.CMD_SENS_BORDER:
				answer.setDataL(this.getSensBorderL());
				answer.setDataR(this.getSensBorderR());
				break;
			case Command.CMD_SENS_DOOR:
				answer.setDataL(this.getSensDoor());
				answer.setDataR(0);
				break;
			case Command.CMD_SENS_LDR:
				answer.setDataL(this.getSensLdrL());
				answer.setDataR(this.getSensLdrR());
				break;
			case Command.CMD_SENS_LINE:
				answer.setDataL(this.getSensLineL());
				answer.setDataR(this.getSensLineR());
				break;
			case Command.CMD_SENS_MOUSE:
				answer.setDataL(this.getSensMouseDX());
				answer.setDataR(this.getSensMouseDY());
				break;
			case Command.CMD_SENS_TRANS:
				answer.setDataL(this.getSensTrans());
				answer.setDataR(0);
				break;
			case Command.CMD_SENS_RC5:
				answer.setDataL(this.getSensRc5());
				this.setSensRc5(0); // nicht zweimal lesen
				answer.setDataR(0);
				break;
			case Command.CMD_SENS_ERROR:
				answer.setDataL(this.getSensError());
				answer.setDataR(0);
				break;

			case Command.CMD_AKT_MOT:
				this.setActMotL((short) command.getDataL());
				this.setActMotR((short) command.getDataR());
				// System.out.println("MotorL:" + command.getDataL() + " MotorR:
				// "
				// + command.getDataR());
				break;
			case Command.CMD_AKT_SERVO:
				this.setActServo(command.getDataL());
				break;
			case Command.CMD_AKT_DOOR:
				this.setActDoor(command.getDataL());
				break;
			case Command.CMD_AKT_LED:
				this.setActLed(command.getDataL());
				break;
			case Command.CMD_ACT_LCD:
				switch (command.getSubcommand()) {
				case Command.SUB_CMD_NORM:
					this.setLcdText(command.getDataL(), command.getDataR(),
							command.getDataBytesAsString());
					break;
				case Command.SUB_LCD_CURSOR:
					this.setCursor(command.getDataL(), command.getDataR());
					break;
				case Command.SUB_LCD_CLEAR:
					this.lcdClear();
					break;
				case Command.SUB_LCD_DATA:
					this.setLcdText(command.getDataBytesAsString());
					break;
				}
				break;
			case Command.CMD_LOG:
				this.setLog(command.getDataBytesAsString());
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see ctSim.Model.Bot#cleanup()
	 */
	protected void cleanup() {
		super.cleanup();
		try {
			if (tcpCon != null)
				tcpCon.disconnect();
			if (answeringMachine != null)
				answeringMachine.die();
			super.cleanup();
		} catch (Exception ex) {
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

	/**
	 * Uebertraegt nach dem Aufruf von super.work alle Sensordaten an den Bot
	 * 
	 * @see ctSim.Model.CtBotSim#work()
	 */
	protected void work() {
		super.work();
		transmitSensors();
	}
}
