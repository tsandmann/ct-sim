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

import ctSim.Connection;
import ctSim.ErrorHandler;

/**
 * Realisiert Kommandos zwischen Bot und Simulator
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */
public class Command {
	/** Motorgeschwindigkeit */
	public static final int CMD_DONE = 'X';
	
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

	/** Uebertragung eines Bildes vom Maussensor */
	public static final int CMD_SENS_MOUSE_PICTURE = 'P';
	
	/** Kommando fuer Logausgaben */
	public static final int CMD_LOG = 'O';
	
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

	/** Subcommandos fuer LCD */
	/** Subkommando Clear Screen */
	public static final int SUB_LCD_CLEAR = 'c';

	/** Subkommando Text ohne Cursor */
	public static final int SUB_LCD_DATA = 'D';

	/** Subkommando Cursorkoordinaten */
	public static final int SUB_LCD_CURSOR = 'C';

	/** Kommando zum Hallo-Sagen  */
	public static final int CMD_WELCOME = 'W';

	/** Subkommando Cursorkoordinaten */
	public static final int SUB_WELCOME_REAL = 'R';

	/** Subkommando Cursorkoordinaten */
	public static final int SUB_WELCOME_SIM = 'S';

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
	 * @param command1 Kommando
	 * @param dataL1 Datum links
	 * @param dataR1 Datum rechts
	 * @param seq1
	 *            Die Sequenznummer des Packetes
	 */
	public Command(int command1, int dataL1, int dataR1, int seq1) {
		super();
		this.command = command1;
		this.dataL = dataL1;
		this.dataR = dataR1;
		this.subcommand = SUB_CMD_NORM;
		this.direction = DIR_REQUEST;
		this.startCode = STARTCODE;
		this.crc = CRCCODE;
		this.payload = 0;
		this.seq = seq1;
		this.dataBytes = new byte[this.payload];
	}

	/**
	 * Standard-Konstruktor
	 */
	public Command() {
		super();
		this.subcommand = SUB_CMD_NORM;
		this.direction = DIR_REQUEST;
		this.startCode = STARTCODE;
		this.crc = CRCCODE;
		this.payload = 0;
		this.dataBytes = new byte[this.payload];
	}

	/**
	 * @return Gibt Kommando zurueck
	 */
	public int getCommand() {
		return this.command;
	}

	/**
	 * Liefert das Kommando als Array von Bytes zurueck
	 * 
	 * @return byte[], in dem jeweils ein byte steht
	 */
	public byte[] getCommandBytes() {
		byte data[] = new byte[COMMAND_SIZE + this.payload];

		int i = 0;
		data[i++] = STARTCODE;
		data[i++] = (byte) (this.command & 255);
		data[i] = (byte) (this.subcommand);
		data[i++] += (byte) (this.direction << 7);

		data[i++] = (byte) (this.payload);
		data[i++] = (byte) (this.dataL & 255);
		data[i++] = (byte) (this.dataL >> 8);
		data[i++] = (byte) (this.dataR & 255);
		data[i++] = (byte) (this.dataR >> 8);
		data[i++] = (byte) (this.seq & 255);
		data[i++] = (byte) (this.seq >> 8);
		data[i++] = CRCCODE;

		return data;
	}

	/**
	 * @return Gib die angehaengten Nutzdaten weiter
	 */
	public byte[] getDataBytes() {
		return this.dataBytes;
	}

	/**
	 * @return Gibt die Nutzdaten als String zurueck
	 */
	public String getDataBytesAsString() {
		return new String(this.dataBytes);
	}

	/**
	 * @return Gibt die Daten zum Kommando links zurueck
	 */
	public int getDataL() {
		return this.dataL;
	}

	/**
	 * @return Gibt die Daten zum Kommando rechts zurueck
	 */
	public int getDataR() {
		return this.dataR;
	}

	/**
	 * @return Gibt die Richtung zurueck
	 */
	public int getDirection() {
		return this.direction;
	}

	/**
	 * @return Liefert die Paketsequenznummer
	 */
	public int getSeq() {
		return this.seq;
	}

	/**
	 * @return Gibt das Subkommando zurueck
	 */
	public int getSubcommand() {
		return this.subcommand;
	}

	
	public static final int CMD_LENGTH=11;
	private byte[] b = new byte[CMD_LENGTH];

	/**
	 * Liest ein Kommando von einer TcpConnection
	 * 
	 * @param con
	 *            Die Verbindung, von der gelesen werden soll
	 * @return das Ergebnis von validate()
	 * @throws IOException
	 * @see Command#validate()
	 */
	public int readCommand(Connection con) throws IOException {
//		int tmp;
//
		
		
		con.read(b,1);
		this.startCode = b[0];
		// lese Bytes bis Startcode gefunden wird
		while (this.startCode != STARTCODE){
			con.read(b,1);
			this.startCode = b[0];
			ErrorHandler.error("Data-Stream out of Sync - skipping byte search for new Startcode");
		}
//
//		this.command = con.readUnsignedByte();
//
//		tmp = con.readUnsignedByte();
//
//		this.subcommand = tmp & 127;
//		this.direction = tmp >> 7 & 1;
//
//		this.payload = con.readUnsignedByte();
//		this.dataL = con.readShort();
//		this.dataR = con.readShort();
//		this.seq = con.readShort();
//		this.crc = con.readUnsignedByte();
//
//		this.dataBytes = new byte[this.payload];
//
//		for (int i = 0; i < this.payload; i++) {
//			this.dataBytes[i] = (byte) con.readUnsignedByte();
//		}

		
		con.read(b,CMD_LENGTH-1);
		
//		startCode = b[0];

		command = b[0];

		subcommand = b[1] & 127;
		direction = b[1] >> 7 & 1;

		payload = b[2];
		dataL =(short) ( ( b[ 4 ] & 0xff ) << 8 | ( b[ 3 ] & 0xff ) );
		dataR =(short) ( ( b[ 6 ] & 0xff ) << 8 | ( b[ 5 ] & 0xff ) );
		seq =(short) ( ( b[ 8 ] & 0xff ) << 8 | ( b[ 7 ] & 0xff ) );
		crc = b[9];
		
		if (payload > 0 ){
			dataBytes = new byte[this.payload];
			con.read(dataBytes,payload);
		}
		
		return validate();
	}

	/**
	 * Liefert eine Stringrepraesentation des Objektes zurueck
	 * 
	 * @return Der String
	 */
	@Override
	public String toString() {
		String dataStr = getDataBytesAsString();

		return "Startcode:\t" + this.startCode + "\n" 
				+ "Command:\t" + (char) this.command + " " + (char) this.subcommand + "\t" + "Direction:\t" + this.direction + "\n" 
				+ "Data:\t\t" + this.dataL + " " + this.dataR + "\n"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$
				+ "Seq:\t\t" + this.seq + "\n" 
				+ "Payload:\t" + this.payload + " Byte\t '" + dataStr + "'\n"  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				+ "CRC:\t\t" + this.crc; //$NON-NLS-1$
	}

	/**
	 * Prueft das Kommando
	 * 
	 * @return 0, wenn intakt, sonst -1
	 */
	private int validate() {
		if ((this.startCode == STARTCODE) && (this.crc == CRCCODE)){
//			System.out.println("Valid Command: \n"+this.toString());
			return 0;
		}
			
		ErrorHandler.error("Invalid Command: \n"+this.toString());
		return -1;
	}

	/**
	 * @param command1
	 *            Kommando, das gesetzt werden soll
	 */
	public void setCommand(int command1) {
		this.command = command1;
	}

	/**
	 * @param dataL1
	 *            Datum fuer links, das gesetzt werden soll
	 */
	public void setDataL(int dataL1) {
		this.dataL = dataL1;
	}

	/**
	 * @param dataR1
	 *            Datum fuer rechts, das gesetzt werden soll
	 */
	public void setDataR(int dataR1) {
		this.dataR = dataR1;
	}

	/**
	 * @param direction1
	 *            Richtung, die gesetzt werden soll
	 */
	public void setDirection(int direction1) {
		this.direction = direction1;
	}

	/**
	 * @param seq1
	 *            Paketsequenznummer, die gesetzt werden soll
	 */
	public void setSeq(int seq1) {
		this.seq = seq1;
	}

	/**
	 * @param subcommand1
	 *            Subkommando, das gesetzt werden soll
	 */
	public void setSubcommand(int subcommand1) {
		this.subcommand = subcommand1;
	}

}
