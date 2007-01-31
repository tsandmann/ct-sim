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
import java.net.Socket;

/**
 * Repraesentiert eine TCP-Verbindung
 *
 * @author Benjamin Benz (bbe@heise.de)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 */

public class TcpConnection extends Connection {
	private Socket socket = null;

	public TcpConnection(Socket sock) throws IOException {
		this.socket = sock;
		setInputStream(socket.getInputStream());
		setOutputStream(socket.getOutputStream());
	}

	/**
	 * Wandelt den uebergebenen String und die Portnummer in eine TCP/IP-Adresse
	 * und stellt dann die Verbindung her
	 *
	 * @param hostname Adresse als String
	 * @param port Portnummer
	 * @throws IOException
	 */
	public TcpConnection(String hostname, int port) throws IOException {
		this(new Socket(hostname, port));
	}

	/**
	 * Beendet die laufende Verbindung
	 * @throws IOException
	 */
	@Override
	public synchronized void close() throws IOException {
		socket.close();
		super.close();
	}

	@Override
	public String getName() {
		return
			"TCP "+socket.getLocalAddress()+":"+socket.getLocalPort()+
			  "->"+socket.getInetAddress() +":"+socket.getPort();
	}

	@Override public String getShortName() { return "TCP"; }
}
