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
import java.net.Socket;

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

	/* Interne Hilfsmethode fuer die Verbindung */
	private void connect() throws IOException {
		setInputStream(this.socket.getInputStream());
		setOutputStream(this.socket.getOutputStream());
	}

	/**
	 * Legt fuer den uebergebenen Socket Little Endian Streams an.
	 * @param sock Der Socket, ueber den geschrieben und gelesen wird.
	 * @throws IOException Wenn es beim Anlegen der beiden Streams zu Problemen kommt.
	 * */
	public void connect(Socket sock) throws IOException {

		this.socket = sock;
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
			this.socket = new Socket(ipa.getHostAddress(), port);
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
	 * @Override CtSim.Connection.disconnect()
	 */
	@Override
	public synchronized void disconnect() throws IOException, Exception {
		super.disconnect();
		try {
			this.socket.close(); // as well as the socket
		} catch (Exception Ex) {
			throw Ex;
		}
	}

	/**
	 * @return Gibt eine Referenz auf socket zurueck
	 */
	public Socket getSocket() {
		return this.socket;
	}
}
