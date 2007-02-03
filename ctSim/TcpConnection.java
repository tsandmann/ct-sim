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
import java.net.ConnectException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import ctSim.controller.BotReceiver;
import ctSim.controller.Config;
import ctSim.model.Command;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.SaferThread;

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

	public static void startListening(final BotReceiver receiver) {
        int p = 10001; //TODO Default sollte hier weg und nach Config umziehen

        try {
            p = Integer.parseInt(Config.getValue("botport"));
        } catch(NumberFormatException nfe) {
            lg.warning(nfe, "Problem beim Parsen der Konfiguration: " +
                    "Parameter 'botport' ist keine Ganzzahl");
        }

        lg.info("Warte auf Verbindung vom c't-Bot auf TCP-Port "+p);

		try {
			final ServerSocket srvSocket = new ServerSocket(p);
			new SaferThread("ctSim-Listener-"+p+"/tcp") {
				@SuppressWarnings("synthetic-access")
				@Override
				public void work() {
					try {
						Socket s = srvSocket.accept(); // blockiert
						lg.fine("Verbindung auf Port "
							+ srvSocket.getLocalPort() + "/tcp eingegangen");
						new TcpConnection(s).doHandshake(receiver);
					} catch (IOException e) {
						lg.warn(e, "Thread "+getName()+" hat ein E/A-Problem " +
								"beim Lauschen");
					}
				}
			}.start();
		} catch (IOException e) {
			lg.warning(e, "E/A-Problem beim Binden an TCP-Port "+p+"; " +
				"l\u00E4uft der c't-Sim schon?");
			return;
		}
	}

	public static void connectTo(final String hostname, final int port,
	final BotReceiver receiver) {
		final String address = hostname+":"+port; // Nur fuer Meldungen
    	lg.info("Verbinde mit "+address+" ...");
		new SaferThread("ctSim-Connect-"+address) {
			@SuppressWarnings("synthetic-access")
			@Override
			public void work() {
				try {
					new TcpConnection(hostname, port).doHandshake(receiver);
		    	} catch (UnknownHostException e) {
		    		lg.warn("Host '"+e.getMessage()+"' nicht gefunden");
		    	} catch (ConnectException e) {
		    		// ConnectExcp deckt so Sachen ab wie "connection refused" 
		    		// und "connection timed out"
		    		lg.warn("Konnte Verbindung mit "+address+
		    			" nicht herstellen ("+e.getLocalizedMessage()+")");
				} catch (IOException e) {
					lg.severe(e, "E/A-Problem beim Verbinden mit "+address);
				}
				// Arbeit ist getan, ob's funktioniert hat oder nicht
				die();
			}
		}.start();
	}

	// Blockiert, bis Handshake erfolgreich oder IOException
	//LODO Fuer connectTo() passt diese Methode, fuer startListening() passt sie nicht ganz: Wenn ein Bot nie einen Handshake zustande kriegt und ein zweiter Bot derweil verbinden will, kommt der zweite nicht zum Zug. Loesung: Timeout oder doHandshake auf neuem Thread laufen lassen
	private void doHandshake(BotReceiver receiver) {
		while (true) {
			try {
				lg.fine("Sende Willkommen");
				write(new Command(Command.Code.WELCOME));
				Command cmd = new Command(this);
				if (cmd.has(Command.Code.WELCOME)) {
					receiver.onBotAppeared(createBot(cmd));
                	return; // Erfolg
                } else {
                    lg.fine("Kommando, aber kein Willkommen von Verbindung " +
                    		"gelesen: Bot l\u00E4uft schon oder ist " +
                    		"veraltet, schicke Willkommen nochmals; " +
                    		"ignoriertes Kommando folgt" + cmd);
                    // Handshake nochmal versuchen
                    continue;
                }
			} catch (ProtocolException e) {
				lg.severe(e, "Ung\uu00FCltiges Kommando beim Handshake; " +
						"ignoriere");
				continue;
			} catch (IOException e) {
				lg.severe(e, "E/A-Problem beim Handshake; Abbruch");
				return;
			}
		}
	}

	private Bot createBot(Command c) throws ProtocolException {
		switch (c.getSubCode()) {
    		case WELCOME_SIM:
    			lg.fine("TCP-Verbindung von simuliertem Bot eingegangen");
    			return new CtBotSimTcp(this);

    		case WELCOME_REAL:
    			lg.fine("TCP-Verbindung von realem Bot eingegangen");
    			return new RealCtBot(this);

    		default:
    			throw new ProtocolException(c.toString());
    	}
	}
}
