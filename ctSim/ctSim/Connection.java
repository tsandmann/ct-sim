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

package ctSim;

import java.io.IOException;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;

/**
 * Repraesentiert eine Verbindung
 * @author bbe (bbe@heise.de)
 *
 */
public abstract class Connection {

	/** Ein Input-Stream */
	private LEDataInputStream dis = null;

	/** Ein Ausgabe-Stream */
	private LEDataOutputStream dos = null;

	/**
	 * 
	 */
	public Connection() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Liest ein Byte
	 * 
	 * @return das Byte
	 * @throws IOException
	 */
	public int readUnsignedByte() throws IOException {
		int data;
		synchronized (dis) {
			data = dis.readUnsignedByte();
		}
		return data;
	}

	/**
	 * Liest einen 16-Bit-Ganzzahlwert (short)
	 * 
	 * @return das Datum
	 * @throws IOException
	 */
	public short readShort() throws IOException {
		short data = 0;
		synchronized (dis) {
			data = dis.readShort();
		}

		return data;
	}

	/**
	 * Beendet die laufende Verbindung
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public synchronized void disconnect() throws IOException, Exception {
		try {
			dis.close(); // close input and output stream
			// br.close();
			dos.close();
		} catch (IOException IOEx) {
			throw IOEx;
		} catch (Exception Ex) {
			throw Ex;
		}
	}

	/**
	 * Schickt einen String
	 * 
	 * @param sendString
	 *            der String, der gesendet werden soll
	 * @throws IOException
	 *             falls etwas mit DataOutputStream.writeBytes oder flush schief
	 *             gegangen ist
	 */
	public void send(String sendString) throws IOException {

		try {
			synchronized (dos) {
				dos.writeBytes(sendString);
				dos.flush(); // write dos
			}

		} catch (IOException iOEx) {
			throw iOEx;
		}
	}

	/**
	 * Uebertraegt Daten
	 * 
	 * @param sendByte
	 *            Nutzdaten (einzelne Bytes in einem int-Array)
	 * @throws IOException
	 */
	public void send(byte sendByte[]) throws IOException {
		try {
			synchronized (dos) {
				dos.write(sendByte);
				dos.flush(); // write dos
			}
		} catch (IOException iOEx) {
			throw iOEx;
		}
	}

	/**
	 * @return Gibt eine Referenz auf dis zurueck
	 */
	public LEDataInputStream getDis() {
		return dis;
	}

	/**
	 * @param dis Referenz auf dis, die gesetzt werden soll
	 */
	public void setDis(LEDataInputStream dis) {
		this.dis = dis;
	}

	/**
	 * @return Gibt eine Referenz auf dos zurueck
	 */
	public LEDataOutputStream getDos() {
		return dos;
	}

	/**
	 * @param dos Referenz auf dos, die gesetzt werden soll
	 */
	public void setDos(LEDataOutputStream dos) {
		this.dos = dos;
	}

}
