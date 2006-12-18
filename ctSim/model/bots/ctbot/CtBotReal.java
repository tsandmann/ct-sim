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

import java.io.IOException;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import ctSim.Connection;
import ctSim.model.Command;
import ctSim.util.FmtLogger;

/**
 * Die Oberklasse fuer Repraesentationen aller Bots, die ausserhalb der Grenzen
 * des Simulators existieren und mit diesem ueber eine Connection kommunizieren.
 */
public class CtBotReal extends CtBot {
	final FmtLogger lg;

	/** Die TCP-Verbindung */
	private Connection con;

	/** Sequenznummer der TCP-Pakete */
	int seq = 0;

	/**
	 * Erzeugt einen neuen Bot
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 * @param tc
	 *            Kommunikationsverbindung
	 */
	public CtBotReal(String name, Point3d pos, Vector3d head,
			Connection tc) {
		super(name, pos, head);
		lg = FmtLogger.getLogger("ctSim.model.bots.ctbot.CtBotReal." + name);
		con = tc;
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
				setSensIrL( ((double)command.getDataL())/1000);
				setSensIrR( ((double)command.getDataR())/1000);
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

			case Command.CMD_LOG:
				this.setLog(command.getDataBytesAsString());
				break;

			case Command.CMD_WELCOME:
				if (command.getSubcommand() != Command.SUB_WELCOME_REAL){
					lg.severe("Ich bin kein realer Bot! Sterbe vor Schreck ;-)");
					die();
				}
				break;


			case Command.CMD_SENS_MOUSE_PICTURE:
				// Empfangen eine Bildes
				System.out.println("Mausteilbild empfangen");
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
	 * Fordert ein MaussensorBild an
	 * @see CtBot#requestMousePicture()
	 */
	@Override
	public void requestMousePicture() {
		Command command = new Command(Command.CMD_SENS_MOUSE_PICTURE, 0, 0, seq++);
		try {
			System.out.println("Frage nach Bild");
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
	@Override
	protected void cleanup() {
		super.cleanup();
		try {
			if (con != null)
				con.disconnect();
			super.cleanup();
		} catch (Exception ex) {
			// Wenn jetzt noch was daneben geht ist uns das egal
		}
	}

	@Override
	protected void init() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void work() {
		// TODO Auto-generated method stub

	}
}
