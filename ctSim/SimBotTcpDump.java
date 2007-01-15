package ctSim;
import java.net.ServerSocket;

import ctSim.model.Command;

/**
 * <p>
 * Entwicklertool: Macht die Command-Objekte sichtbar, die durch die
 * TCP-Verbindung zwischen c't-Sim und Steuercode laufen. F&uuml;r Debugging der
 * IO.
 * </p>
 * <p>
 * Verwendung: "botport" in config/ct-sim.xml umstellen auf "10002"; Sim und
 * dieses Tool starten; Bot-Code starten. Das Hinschreiben der ganzen Commands
 * verz&ouml;gert alles &ndash; falls der Sim Meldungen wirft "Bot viel zu
 * langsam", dann den entsprechenden Timeout-Wert in der Konfigdatei hochsetzen.
 * </p>
 */ 
public class SimBotTcpDump {
	public static void main(String... args) throws Exception {
		// Blockiert
		TcpConnection bot = new TcpConnection(new ServerSocket(10001).accept());
		// Blockiert auch
		TcpConnection sim = new TcpConnection("127.0.0.1", 10002);
		// Go
		new Forwarder("Bot->Sim     ", bot, sim);
		new Forwarder("     Sim->Bot", sim, bot);
	}

	static class Forwarder extends Thread {
		TcpConnection from;
		TcpConnection to;

		public Forwarder(String name, TcpConnection from, TcpConnection to) {
			super(name);
			this.from = from;
			this.to = to;
			start();
		}

		@Override
		public void run() {
			try {
				while (true) {
					Command c = new Command(from);
					System.out.println(getName() + ": " + c.toCompactString());
					to.write(c);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("thread " + getName().trim() + " dying; " +
					"terminating process");
			System.exit(0);
		}
	}
}
