package ctSim;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.ServerSocket;

import ctSim.model.Command;

/**
 * <p>
 * Entwicklertool: Macht die Command-Objekte sichtbar, die durch die
 * TCP-Verbindung zwischen c't-Sim und Steuercode laufen. F&uuml;r Debugging der
 * IO. Tool l&auml;uft solange, bis man es ausdr&uuml;cklich beendet.
 * </p>
 * <p>
 * Verwendung: "botport" in config/ct-sim.xml umstellen auf "10002"; Sim und
 * dieses Tool starten; Bot-Code starten. Das Hinschreiben der ganzen Commands
 * verz&ouml;gert alles &ndash; falls der Sim Meldungen wirft "Bot viel zu
 * langsam", dann den entsprechenden Timeout-Wert in der Konfigdatei hochsetzen.
 * </p>
 */
public class SimBotTcpDump {
	/**
	 * main
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
			try { bot.close(); } catch (IOException e) {/**/}
			try { sim.close(); } catch (IOException e) {/**/}
		}
	}

	/**
	 * Forwarder-Thread
	 */
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
