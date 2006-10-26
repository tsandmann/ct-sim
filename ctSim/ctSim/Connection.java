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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Repraesentiert eine Verbindung
 * 
 * @author bbe (bbe@heise.de)
 * 
 */
public abstract class Connection {

	/** Ein Input-Stream */
	private DataInputStream dis = null;
	BufferedReader br = null;
	
	//private InputStream is = null;
	
	/** Ein Ausgabe-Stream */
	private DataOutputStream dos = null;

	/**
	 * 
	 */
	public Connection() {
		super();
	}

	/**
	 * Liest ein Byte
	 * 
	 * @return das Byte
	 * @throws IOException
	 */
//	public int readUnsignedByte() throws IOException {
//		int data;
//		synchronized (isr) {
//			data = this.dis.readUnsignedByte();
////			data = isr.read();
//		}
//		return data;
//	}

	/**
	 * Liest einen 16-Bit-Ganzzahlwert (short)
	 * 
	 * @return das Datum
	 * @throws IOException
	 */
//	public short readShort() throws IOException {
//		//byte [] w = new byte[2];
//		int [] w = new int[2];
//		synchronized (br) {
//	        //dis.readFully( w, 0, 2 );
////			w[0]=dis.readUnsignedByte();
////			w[1]=dis.readUnsignedByte();
//			
//			//isr.read( w, 0, 2 );
//	        return (short) ( ( w[ 1 ] & 0xff ) << 8 | ( w[ 0 ] & 0xff ) );
//		}
//	}

	/**
	 * Beendet die laufende Verbindung
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public synchronized void disconnect() throws IOException, Exception {
		try {
			//isr.close(); // close input and output stream
			br.close();
			this.dos.close();
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
			synchronized (this.dos) {
				this.dos.writeBytes(sendString);
				this.dos.flush(); // write dos
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
			synchronized (this.dos) {
				this.dos.write(sendByte);
				this.dos.flush(); // write dos
			}
		} catch (IOException iOEx) {
			throw iOEx;
		}
	}

	/**
	 * @return Gibt eine Referenz auf dis zurueck
	 */
//	public DataInputStream getDis() {
//		return dis;
//	}

	/**
	 * @param disIn
	 *            Referenz auf dis, die gesetzt werden soll
	 */
	public void setInputStream(InputStream is) {
		dis = new DataInputStream(is);
//		this.dis=is;
		//isr= new InputStreamReader(is);
		br = new BufferedReader(new InputStreamReader(is));
	}

	/**
	 * @return Gibt eine Referenz auf dos zurueck
	 */
//	public DataOutputStream getDos() {
//		return dos;
//	}

	/**
	 * @param dosIn
	 *            Referenz auf dos, die gesetzt werden soll
	 */
	public void setOutputStream(OutputStream os) {
		dos = new DataOutputStream(os);
	}

	public void read(byte[] b) throws IOException {
		dis.readFully( b, 0, b.length);
//		char [] c = new char[b.length];
//		br.read( c, 0, b.length);
//		for (int i=0; i<c.length; i++)
//			b[i]=(byte)c[i];
	}

	public void read(byte[] b, int len) throws IOException {
		dis.readFully( b, 0, len);
		
//		char [] c = new char[len];
//		br.read( c, 0, b.length);
//		for (int i=0; i<c.length; i++)
//			b[i]=(byte)c[i];

	}

}
