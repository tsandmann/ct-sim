package ctSim.Model;

import java.io.IOException;

import ctSim.TcpConnection;

/**
 * Kommandos zwischen Bot und Simulator
 * @author Benjamin Benz (bbe@heise.de)
 */
public class Command {
	/**  Steuerung Klappe */
	public static final int CMD_AKT_DOOR=	'd';			
	
	/** LEDs steuern */
	public static final int CMD_AKT_LED	=	'l';
	
	/** Motorgeschwindigkeit */
	public static final int CMD_AKT_MOT=	'M';				
	
	/** Steuerung Servo */
	public static final int CMD_AKT_SERVO=	'S';
	
	
	/** Abgrundsensoren */
	public static final int CMD_SENS_BORDER= 'B';				
	/** Überwachung Klappe */
	public static final int CMD_SENS_DOOR=	'D';  		
	/** Radencoder */
	public static final int CMD_SENS_ENC =	'E';		
	/** Motor- oder Batteriefehler */
	public static final int CMD_SENS_ERROR=	'e';		
	/** Abstandssensoren */
	public static final int CMD_SENS_IR	= 'I';		
	/** Helligkeitssensoren */
	public static final int CMD_SENS_LDR =	'H';		
	/** Liniensensoren */
	public static final int CMD_SENS_LINE =	'L';		
	/** Maussensor */
	public static final int CMD_SENS_MOUSE=	'm';		
	/** IR-Fernbedienung */
	public static final int CMD_SENS_RC5 =	'R';		
	/** Überwachung Transportfach */
	public static final int CMD_SENS_TRANS=	'T';		
	/** Länge eines Kommandos in Byte */
	public static final int COMMAND_SIZE = 11;		
	
	/** Markiert den Beginn eines Commands */
	public static final int STARTCODE = '>';			
	/** Markiert das Ende des Commands */
	public static final int CRCCODE = '<';		
	/** Richtung Antwort */

	public static final int DIR_ANSWER = 1;		
	/** Richtung Anfrage */
	public static final int DIR_REQUEST = 0;		
	
	/** Nicht genug Daten */
	public static final int ERROR_TOO_SHORT = -2;		
		
	/** Das Standardkommando */
	public static final int SUB_CMD_NORM = 'N';		
	/** Nur Links */
	public static final int SUB_CMD_LEFT = 'L';				
	/** Nur rechts */
	public static final int SUB_CMD_RIGHT = 'R';		
	
	/** Markiert den Beginn eines Commands */
	private int startCode=STARTCODE;			
	/** Kommando */
	private int command=0;	
	/**  Subkommando */
	private int subcommand=0;	
	/** 0 ist anfrage, 1 ist antwort */
	private int direction=0;		
	/** Bytes, die dem Kommando noch folgen */
	private int payload=0;		

	/** Daten zum Kommando Links */
	private  int dataL=0;	
	/** Daten zum Kommando Rechts */
	private  int dataR=0;
	
	/** Paketsequenznummer */
	private  int seq=0;		
	/** Markiert das Ende des Commands */
	private int crc=CRCCODE;		
	
	/**
	 * Erzeugt ein Kommando zum abschicken (direction=DIR_REQUEST)
	 * @param command
	 * @param dataL
	 * @param dataR
	 * @param seq Die Sequenznummer des Packetes
	 */
	public Command(int command, int dataL, int dataR,int seq) {
		super();
		// TODO Auto-generated constructor stub
		this.command = command;
		this.dataL = dataL;
		this.dataR = dataR;
		this.subcommand = SUB_CMD_NORM;
		direction=DIR_REQUEST;
		startCode=STARTCODE;
		crc=CRCCODE;
		payload=0;
		this.seq=seq;
	}

	/**
	 * Standard Konstruktor
	 */
	public Command(){
		super();
	}

	/**
	 * @return Returns the command.
	 */
	public int getCommand() {
		return command;
	}
	
	
	/**
	 * Liefert das Kommando als Array von Bytes zurück
	 * @return byte[] in dem jeweils ein byte steht
	 */
	public byte[] getCommandBytes(){
		byte data[] = new byte[COMMAND_SIZE];

		int i=0;
		data[i++]=STARTCODE;
		data[i++]= (byte)(command &255);
		data[i]= (byte)(subcommand);
		data[i++]+= (byte)(direction<<7) ;
		
		
		data[i++]=(byte)(payload);
		data[i++]=(byte)(dataL &255);
		data[i++]=(byte)(dataL >> 8);
		data[i++]=(byte)(dataR &255);
		data[i++]=(byte)(dataR >> 8);
		data[i++]=(byte)(seq &255);
		data[i++]=(byte)(seq >> 8);
		data[i++]=CRCCODE;

		return data;
	}
	
	/**
	 * @return Returns the dataL.
	 */
	public int getDataL() {
		return dataL;
	}
	
	/**
	 * @return Returns the data_r.
	 */
	public int getDataR() {
		return dataR;
	}

	/**
	 * @return Returns the direction.
	 */
	public int getDirection() {
		return direction;
	}

	/**
	 * @return Returns the seq.
	 */
	public int getSeq() {
		return seq;
	}
	
	/**
	 * @return Returns the subcommand.
	 */
	public int getSubcommand() {
		return subcommand;
	}

	/** 
	 * Liest ein Kommando von einem DataInputStream
	 * @param dis Der Stream zum lesen
	 * @return das ergebnis von validate()
	 * @throws IOException
	 * @see Command#validate()
	 * @see DataInputStream
	 */
	public int readCommand(TcpConnection tcpCon) throws IOException {
		int tmp;
		
		startCode=0;
		// read Bytes until Startcode found!
		while (startCode != STARTCODE){
			startCode= tcpCon.readUnsignedByte();	
		}
		
		command = tcpCon.readUnsignedByte();
		
		tmp=tcpCon.readUnsignedByte();
		
		subcommand=tmp&127;
		direction=tmp>>7 &1;
		
		payload= tcpCon.readUnsignedByte();
		dataL= tcpCon.readShort();
		dataR= tcpCon.readShort();
		seq= tcpCon.readShort();
		crc=tcpCon.readUnsignedByte();
		
		return validate();
	}

	/**
	 * @param command The command to set.
	 */
	public void setCommand(int command) {
		this.command = command;
	}

	/**
	 * @param dataL The dataL to set.
	 */
	public void setDataL(int dataL) {
		this.dataL = dataL;
	}

	/**
	 * @param dataR The data_r to set.
	 */
	public void setDataR(int dataR) {
		this.dataR = dataR;
	}

	/**
	 * @param direction The direction to set.
	 */
	public void setDirection(int direction) {
		this.direction = direction;
	}

	/**
	 * @param seq The seq to set.
	 */
	public void setSeq(int seq) {
		this.seq = seq;
	}

	/**
	 * @param subcommand The subcommand to set.
	 */
	public void setSubcommand(int subcommand) {
		this.subcommand = subcommand;
	}

	/**
	 * Liefert eine Stringrepräsentation des Objektes zurück
	 * @return Den String
	 */
	public String toString(){
		return "Startcode:\t" + startCode +"\n"+
			     "Command:\t" + command+"\n"+
				 "Subcommand:\t" + subcommand+"\n"+
				 "Direction:\t" + direction+"\n"+
				 "Payload:\t" +payload+"\n"+
				 "Data:\t\t" +dataL +" "+dataR+"\n"+
				 "Seq:\t\t"+seq+"\n"+
				 "CRC:\t\t"+crc;
	}

	/**
	 * Prüft das Kommando
	 * @return 0, wenn intakt, sonst -1
	 */
	private int validate(){
		if ((startCode==STARTCODE) && (crc==CRCCODE))
			return 0;
		else 
			return -1;
	}

}
