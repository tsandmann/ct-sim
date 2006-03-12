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
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 */

public class TcpConnection extends Connection {
	/** Der Socket */
	private Socket socket = null;


	// private BufferedReader br = null;

	/**
	 * Neue Verbindung
	 */

	public TcpConnection() {
		super();
	}

	/**
	 * Lauscht auf einem Port und initialisiert die Verbindung
	 * 
	 * @deprecated Bitte diese Methode nicht mehr verwenden, da Sie ihren Port nicht wieder freigibt.
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

	/* Interne Hilfsmethode fuer die Verbindung */
	private void connect() throws IOException {
		setDis(new LEDataInputStream(socket.getInputStream()));
		setDos(new LEDataOutputStream(socket.getOutputStream()));
	}

	/**
	 * Legt fuer den uebergebenen Socket Little Endian Streams an.
	 * @param socket Der Socket, ueber den geschrieben und gelesen wird.
	 * @throws IOException Wenn es beim Anlegen der beiden Streams zu Problemen kommt.
	 * */
	public void connect (Socket socket) throws IOException {
		this.socket = socket;
		connect();
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
		super.disconnect();
		try {
			socket.close(); // as well as the socket
		} catch (Exception Ex) {
			throw Ex;
		}
	}

	/**
	 * @return Gibt eine Referenz auf socket zurueck
	 * @return Gibt den Wert von socket zurueck
	 */
	public Socket getSocket() {
		return socket;
	}
}
