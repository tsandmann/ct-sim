/*
 * c't-Sim - Robotersimulator für den c't-Bot
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
import java.net.ProtocolException;
import java.net.ServerSocket;

import ctSim.model.Command;

/**
 * <p>
 * Entwicklertool: Macht die Command-Objekte sichtbar, die durch die
 * TCP-Verbindung zwischen c't-Sim und Steuercode laufen. Für Debugging der
 * IO. Tool läuft solange, bis man es ausdrücklich beendet.
 * </p>
 * <p>
 * Verwendung: "botport" in config/ct-sim.xml umstellen auf "10002"; Sim und
 * dieses Tool starten; Bot-Code starten. Das Hinschreiben der ganzen Commands
 * verzögert alles - falls der Sim Meldungen wirft "Bot viel zu
 * langsam", dann den entsprechenden Timeout-Wert in der Konfigdatei hochsetzen.
 * </p>
 */
public class SimBotTcpDump {
	/**
	 * main
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		ServerSocket srvSock = new ServerSocket(10001);
		while (true) {
			// Blockiert
			TcpConnection bot = new TcpConnection(srvSock.accept());
			// Blockiert auch
			TcpConnection sim = new TcpConnection("127.0.0.1", 10002);
			// Go
			Forwarder bs = new Forwarder("Bot->Sim     ", bot, sim);
			Forwarder sb = new Forwarder("     Sim->Bot", sim, bot);
			bs.peer = sb;
			sb.peer = bs;
			bs.join();
			sb.join();
			try { bot.close(); } catch (IOException e) {
				srvSock.close();
			}
			try { sim.close(); } catch (IOException e) {
				srvSock.close();	
			}
		}
	}

	/** Forwarder-Thread */
	static class Forwarder extends Thread {
		/** Quelle */
		TcpConnection from;
		/** Ziel */
		TcpConnection to;
		/** Abbruchbedingung */
		volatile boolean deathRequested = false;
		/** Peer */
		Forwarder peer;

		/**
		 * Forwarder
		 * 
		 * @param name
		 * @param from
		 * @param to
		 */
		public Forwarder(String name, TcpConnection from, TcpConnection to) {
			super(name);
			this.from = from;
			this.to = to;
			start();
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			System.err.println("Thread " + getName().trim() + " starting");
			try {
				while (! deathRequested) {
					try {
						Command c = new Command(from);
						System.out.println(getName() + ": " + c.toCompactString());
						to.write(c);
					} catch (ProtocolException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.err.println("\nThread " + getName().trim() + " dying; " +
					"killing peer");

			peer.deathRequested = true;
			peer.interrupt();
			// Rabiater, aber interrupt() zeigt vielleicht keine Wirkung
			try { peer.from.close(); } catch (IOException e) {/**/}
			try { peer.to.close(); } catch (IOException e) {/**/}
		}
	}
}
