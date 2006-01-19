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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;

/**
 * Repraesentiert eine TCP-Verbindung
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */

public class TcpConnection {
	/** Der Socket */
	private Socket socket = null;

	/** Ein Input-Stream */
	private LEDataInputStream dis = null;

	/** Ein Ausgabe-Stream */
	private LEDataOutputStream dos = null;

	// private BufferedReader br = null;

	/**
	 * Neue Verbindung
	 */

	public TcpConnection() {
		super();
	}

	/**
	 * Liest ein Byte
	 * 
	 * @return das Byte
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
	 */
	public short readShort() throws IOException {
		short data = 0;
		synchronized (dis) {
			data = dis.readShort();
		}

		return data;
	}

	/**
	 * Lauscht auf einem Port und initialisiert die Verbindung
	 * 
	 * @param port
	 *            Der Port, auf dem gelauscht werden soll
	 * @return 0 wenn alles ok, sonst -1
	 */
	public int listen(int port) {
		try {
			ServerSocket server = new ServerSocket(port);
			socket = server.accept();

			connect();

		} catch (IOException iOEx) {
			ErrorHandler.error("Error listening on port: " + port + " "
					+ iOEx.getMessage());
			return -1;
		}
		return 0;
	}

	/* Interne Hilfsmethode für die Verbindung */
	private void connect() throws IOException {
		// Erzeugt gepufferten Datenstrom vom XPort zu PC:
		// br = new BufferedReader(new
		// InputStreamReader(socket.getInputStream()));
		dis = new LEDataInputStream(socket.getInputStream());

		// Erzeugt gepufferten Datenstrom vom PC zum XPort:
		dos = new LEDataOutputStream(socket.getOutputStream());

		// this.start();
	}

	/**
	 * Stellt eine TCP/IP-Verbindung her
	 * 
	 * @param ipa
	 *            TCP/IP-Adresse
	 * @param port
	 *            Portnummer
	 * @throws IOException
	 * @throws Exception
	 */
	public void connect(InetAddress ipa, int port) throws IOException,
			Exception {
		try {
			// really establish a TCP connection (at last...)
			socket = new Socket(ipa.getHostAddress(), port);
			connect(); // Init rest of stuff

		} catch (IOException IOEx) {
			throw IOEx;
		} catch (Exception Ex) {
			throw Ex;
		}
	}

	/**
	 * Wandelt den uebergebenen String und die Portnummer in eine TCP/IP-Adresse
	 * und stellt dann die Verbindung her
	 * 
	 * @param ipa_str
	 *            Adresse als String
	 * @param port
	 *            Portnummer
	 * @throws IOException
	 * @throws Exception
	 */
	public void connect(String ipa_str, int port) throws IOException, Exception {
		InetAddress ipa = null;
		try {
			ipa = InetAddress.getByName(ipa_str);

			connect(ipa, port);

		} catch (IOException IOEx) {
			throw IOEx;
		} catch (Exception Ex) {
			throw Ex;
		}
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
			socket.close(); // as well as the socket
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
}
