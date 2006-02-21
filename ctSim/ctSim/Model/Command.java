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

package ctSim.Model;

import java.io.IOException;

import ctSim.TcpConnection;

/**
 * Realisiert Kommandos zwischen Bot und Simulator
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */
public class Command {
	/** Steuerung Klappe */
	public static final int CMD_AKT_DOOR = 'd';

	/** LEDs steuern */
	public static final int CMD_AKT_LED = 'l';

	/** Motorgeschwindigkeit */
	public static final int CMD_AKT_MOT = 'M';

	/** Steuerung Servo */
	public static final int CMD_AKT_SERVO = 'S';
	
	/** LCD Anzeige */
	public static final int CMD_ACT_LCD = 'c';

	/** Abgrundsensoren */
	public static final int CMD_SENS_BORDER = 'B';

	/** Ueberwachung Klappe */
	public static final int CMD_SENS_DOOR = 'D';

	/** Radencoder */
	public static final int CMD_SENS_ENC = 'E';

	/** Motor- oder Batteriefehler */
	public static final int CMD_SENS_ERROR = 'e';

	/** Abstandssensoren */
	public static final int CMD_SENS_IR = 'I';

	/** Helligkeitssensoren */
	public static final int CMD_SENS_LDR = 'H';

	/** Liniensensoren */
	public static final int CMD_SENS_LINE = 'L';

	/** Maussensor */
	public static final int CMD_SENS_MOUSE = 'm';

	/** IR-Fernbedienung */
	public static final int CMD_SENS_RC5 = 'R';

	/** Ueberwachung Transportfach */
	public static final int CMD_SENS_TRANS = 'T';

	/** Laenge eines Kommandos in Byte */
	public static final int COMMAND_SIZE = 11;

	/** Markiert den Beginn eines Kommandos */
	public static final int STARTCODE = '>';

	/** Markiert das Ende des Kommandos */
	public static final int CRCCODE = '<';

	/** Richtung Antwort */
	public static final int DIR_ANSWER = 1;

	/** Richtung Anfrage */
	public static final int DIR_REQUEST = 0;

	/** Nicht genug Daten */
	public static final int ERROR_TOO_SHORT = -2;

	/** Das Standardkommando */
	public static final int SUB_CMD_NORM = 'N';

	/** Nur links */
	public static final int SUB_CMD_LEFT = 'L';

	/** Nur rechts */
	public static final int SUB_CMD_RIGHT = 'R';

	/** Subcommandos f�r LCD */
	/** Subkommando Clear Screen */
	public static final int SUB_LCD_CLEAR = 'c';     
	
	/** Subkommando Text ohne Cursor */
	public static final int SUB_LCD_DATA = 'D';
	
	/** Subkommando Cursorkoordinaten */
	public static final int SUB_LCD_CURSOR = 'C';
	
	/** Markiert den Beginn eines Kommandos */
	private int startCode = STARTCODE;

	/** Kommando */
	private int command = 0;

	/** Subkommando */
	private int subcommand = 0;

	/** 0 bedeutet Anfrage, 1 bedeutet Antwort */
	private int direction = 0;

	/** Bytes, die dem Kommando noch folgen */
	private int payload = 0;
	
	/** Daten, die dem Kommando folgen */
	private byte[] dataBytes;

	/** Daten zum Kommando links */
	private int dataL = 0;

	/** Daten zum Kommando rechts */
	private int dataR = 0;

	/** Paketsequenznummer */
	private int seq = 0;

	/** Markiert das Ende des Kommandos */
	private int crc = CRCCODE;

	/**
	 * Erzeugt ein Kommando zum Abschicken (direction=DIR_REQUEST)
	 * 
	 * @param command
	 * @param dataL
	 * @param dataR
	 * @param seq
	 *            Die Sequenznummer des Packetes
	 */
	public Command(int command, int dataL, int dataR, int seq) {
		super();
		this.command = command;
		this.dataL = dataL;
		this.dataR = dataR;
		this.subcommand = SUB_CMD_NORM;
		direction = DIR_REQUEST;
		startCode = STARTCODE;
		crc = CRCCODE;
		payload = 0;
		this.seq = seq;
		dataBytes = new byte[payload];
	}

	/**
	 * Standard-Konstruktor
	 */
	public Command() {
		super();
	}

	/**
	 * @return Gibt Kommando zurueck
	 */
	public int getCommand() {
		return command;
	}

	/**
	 * Liefert das Kommando als Array von Bytes zurueck
	 * 
	 * @return byte[] , in dem jeweils ein byte steht
	 */
	public byte[] getCommandBytes() {
		byte data[] = new byte[COMMAND_SIZE+payload];

		int i = 0;
		data[i++] = STARTCODE;
		data[i++] = (byte) (command & 255);
		data[i] = (byte) (subcommand);
		data[i++] += (byte) (direction << 7);

		data[i++] = (byte) (payload);
		data[i++] = (byte) (dataL & 255);
		data[i++] = (byte) (dataL >> 8);
		data[i++] = (byte) (dataR & 255);
		data[i++] = (byte) (dataR >> 8);
		data[i++] = (byte) (seq & 255);
		data[i++] = (byte) (seq >> 8);
		data[i++] = CRCCODE;
		
		return data;
	}

	/**
	 * @return Gib die angeh�ngten Nutzdaten weiter
	 */
	public byte[] getDataBytes() {
		return dataBytes;
	}
	
	/**
	 * @return gib die Nutzdaten als String zur�ck
	 */
	public String getDataBytesAsString() {
		return new String(dataBytes);
	}
	
	/**
	 * @return Gibt die Daten zum Kommando links zurueck
	 */
	public int getDataL() {
		return dataL;
	}

	/**
	 * @return Gibt die Daten zum Kommando rechts zurueck
	 */
	public int getDataR() {
		return dataR;
	}

	/**
	 * @return Gibt die Richtung zurueck
	 */
	public int getDirection() {
		return direction;
	}

	/**
	 * @return Liefert die Paketsequenznummer
	 */
	public int getSeq() {
		return seq;
	}

	/**
	 * @return Gibt das Subkommando zurueck
	 */
	public int getSubcommand() {
		return subcommand;
	}

	/**
	 * Liest ein Kommando von einer TcpConnection
	 * 
	 * @param tcpCon
	 *            Die Verbindung, von der gelesen werden soll
	 * @return das Ergebnis von validate()
	 * @throws IOException
	 * @see Command#validate()
	 */
	public int readCommand(TcpConnection tcpCon) throws IOException {
		int tmp;
		
		startCode = 0;
		// lese Bytes bis Startcode gefunden wird
		while (startCode != STARTCODE) {
			startCode = tcpCon.readUnsignedByte();
		}

		command = tcpCon.readUnsignedByte();

		tmp = tcpCon.readUnsignedByte();

		subcommand = tmp & 127;
		direction = tmp >> 7 & 1;

		payload = tcpCon.readUnsignedByte();
		dataL = tcpCon.readShort();
		dataR = tcpCon.readShort();
		seq = tcpCon.readShort();
		crc = tcpCon.readUnsignedByte();

		dataBytes = new byte[payload];
		
		for (int i=0; i<payload; i++) {
			dataBytes[i] = (byte) tcpCon.readUnsignedByte();
		}
		System.out.println(toString());
		
		return validate();
	}

	/**
	 * Liefert eine Stringrepraesentation des Objektes zurueck
	 * 
	 * @return Der String
	 */
	public String toString() {
		String dataStr = getDataBytesAsString();
		
		return "Startcode:\t" + startCode + "\n" + "Command:\t" + (char)command 
				+ "\n" + "Subcommand:\t" + (char)subcommand + "\n" + "Direction:\t"
				+ direction + "\n" + "Payload:\t" + payload + "\n"
				+ "Data:\t\t" + dataL + " " + dataR + "\n" + "Seq:\t\t" + seq
				+ "\n" + "Data:\t\t'" + dataStr + "'\n" + "CRC:\t\t" + crc;
	}

	/**
	 * Prueft das Kommando
	 * 
	 * @return 0, wenn intakt, sonst -1
	 */
	private int validate() {
		if ((startCode == STARTCODE) && (crc == CRCCODE))
			return 0;
		else
			return -1;
	}

	/**
	 * @param command
	 *            Kommando, das gesetzt werden soll
	 */
	public void setCommand(int command) {
		this.command = command;
	}

	/**
	 * @param dataL
	 *            Datum fuer links, das gesetzt werden soll
	 */
	public void setDataL(int dataL) {
		this.dataL = dataL;
	}

	/**
	 * @param dataR
	 *            Datum fuer rechts, das gesetzt werden soll
	 */
	public void setDataR(int dataR) {
		this.dataR = dataR;
	}

	/**
	 * @param direction
	 *            Richtung, die gesetzt werden soll
	 */
	public void setDirection(int direction) {
		this.direction = direction;
	}

	/**
	 * @param seq
	 *            Paketsequenznummer, die gesetzt werden soll
	 */
	public void setSeq(int seq) {
		this.seq = seq;
	}

	/**
	 * @param subcommand
	 *            Subkommando, das gesetzt werden soll
	 */
	public void setSubcommand(int subcommand) {
		this.subcommand = subcommand;
	}

}
