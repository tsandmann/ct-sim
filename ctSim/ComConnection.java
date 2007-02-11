package ctSim;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.util.TooManyListenersException;

import ctSim.controller.BotReceiver;
import ctSim.controller.Config;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.SaferThread;

/**
 * <p>
 * Serielle Verbindung / USB-Verbindung zu realen Bots (als Hardware vorhandenen
 * Bots). Der Treiber, den wir mit unserem USB-2-Bot-Adapter verwenden, emuliert
 * einen seriellen Anschluss. Dieser kann dann von dieser Klasse angesprochen
 * werden.
 * </p>
 * <p>
 * Die <a
 * href="http://java.sun.com/products/javacomm/reference/api">javacomm-Dokumentation</a>
 * beschreibt das API, das in dieser Klasse verwendet wird ({@code gnu.io.*}).
 * Tricks wie die Doku in Eclipse einbinden (Project / Build Path / rxtx.jar /
 * Javadoc location) gehen aber m.E. nicht.
 * </p>
 * <p style='font-size: smaller;'>
 * Hintergrund: Eclipse sucht nach gnu.io.irgendwas und findet aber nichts, da
 * die Doku unter javax.comm steht. Das r&uuml;hrt daher, dass die o.g.
 * Dokumentation zu Suns "<a href="http://java.sun.com/products/javacomm/">Java
 * Commmunications API</a>" geh&ouml;rt ({@code javax.comm.*}), c't-Sim aber
 * stattdessen den Gnu-Nachbau namens RXTX verwendet. RXTX ist bzgl. der
 * Klassennamen, Methodensignaturen usw. kompatibel mit javacomm, ist aber in
 * bester Open-Source-Tradition 100%ig undokumentiert.
 * </p>
 * <p>
 * Die RXTX-Bibliothek erzeugt einen Thread, der auf Eingaben lauscht und einen
 * unklaren Namen hat ("Thread-3" o.&auml;.).
 * </p>
 *
 * @author Maximilian Odendahl (maximilian.odendahl@rwth-aachen.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ComConnection extends Connection {
	protected boolean inputAvailable = false;
	protected final Object inputAvailMutex = new Object();
	private final SerialPort port;

	/**
	 * Baut eine COM-Verbindung auf (d.h. i.d.R. zum USB-2-Bot-Adapter).
	 * Verwendet die in der Konfigdatei angegebenen Werte.
	 *
	 * @throws CouldntOpenTheDamnThingException
	 * @throws IOException
	 */
	private ComConnection()
	throws CouldntOpenTheDamnThingException, IOException {
		String comPortName = Config.getValue("serialport");

		CommPortIdentifier portId;
		try {
			portId = CommPortIdentifier.getPortIdentifier(comPortName);
		} catch (NoSuchPortException e) {
			throw new CouldntOpenTheDamnThingException(
				"COM-Port-Name '"+comPortName+"' stinkt", e);
		}
		try {
			port = (SerialPort)portId.open("CtSim", 2000);
		} catch (PortInUseException e) {
			throw new CouldntOpenTheDamnThingException("COM-Port "+comPortName+
				" wird schon verwendet; l\u00E4uft noch eine alte " +
				"Sim-Instanz? Eventuell hilft es, den schwarzen Eumel des " +
				"USB-2-Bot-Adapter vom USB-Kabel abzuziehen und wieder " +
				"dranzustecken", e);
		}
		//TODO Parameter configurierbar machen
		String baudrate = Config.getValue("serialportBaudrate");
		try {
			int br = Integer.parseInt(baudrate);
			port.setSerialPortParams(br, SerialPort.DATABITS_8, 
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			lg.info("Warte auf Verbindung vom c't-Bot an seriellem Port "+
				comPortName+" ("+br+" baud)");
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			port.enableReceiveTimeout(60000); //$$$ t receive timeout isReceiveTEna
		} catch (NumberFormatException e) {
			throw new CouldntOpenTheDamnThingException("Die Baud-Rate '"+
				baudrate+"' ist keine g\u00FCltige Zahl", e);
		} catch (UnsupportedCommOperationException e) {
			port.close();
			throw new CouldntOpenTheDamnThingException("Konnte seriellen " +
					"Port \u00F6ffnen, aber nicht konfigurieren");
		}

		setInputStream(port.getInputStream());
		setOutputStream(port.getOutputStream());

		registerEventListener();
	}

	private void registerEventListener() {
		port.notifyOnDataAvailable(true);

		class OurEventListener implements SerialPortEventListener {
			public void serialEvent(SerialPortEvent evt) {
				if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
					synchronized (inputAvailMutex) {
						inputAvailable = true;
						inputAvailMutex.notify();
					}
				}
				/*//$$$ BI
					case SerialPortEvent.BI: // BI = Break Interrupt
						// Jemand hat was an den USB-Adapter angeschlossen
						if (acceptListener == null)
							return;
						acceptListener.someoneConnected(ComConnection.this);
						break;
				 */
			}
		}

		try {
			port.addEventListener(new OurEventListener());
		} catch (TooManyListenersException e) {
			// "Kann nicht passieren"
			throw new AssertionError(e);
		}
	}

	public void blockUntilDataAvailable() throws InterruptedException {
		synchronized (inputAvailMutex) {
			while (! inputAvailable) {
				// while ist Schutz vor spurious wakeups
				// (siehe wait()-Doku)
				inputAvailMutex.wait();
			}
		}
	}
	
	@Override
	public void read(byte[] b) throws IOException {
		inputAvailable = false;
		super.read(b);
	}

	/** Schlie&szlig;t den seriellen Port und schlie&szlig;t die Vaterklasse. */
	@Override
	public synchronized void close() {
		//$$$ comconn close
		/*
		port.close();
		super.close();
		*/
	}

	@Override public String getName() { return port.getName(); }

	@Override public String getShortName() { return "USB"; }

	///////////////////////////////////////////////////////////////////////////
	// Auto-detect-Kram

	private static ComConnection comConnSingleton = null;

	static class ComListenerThread extends SaferThread {
		private final BotReceiver botReceiver;

		public ComListenerThread(BotReceiver receiver) {
			super("ctSim-Listener-COM");
			this.botReceiver = receiver;
		}

		@SuppressWarnings("synthetic-access")
		@Override
		public void work() throws InterruptedException {
			comConnSingleton.blockUntilDataAvailable();
			lg.fine("Serielle Verbindung eingegangen");
			Bot b = new RealCtBot(comConnSingleton);
			b.addDisposeListener(new Runnable() {
				public void run() {
					spawnThread(botReceiver);
				}
			});
			botReceiver.onBotAppeared(b);
			die();
		}
	}

	public static void startListening(final BotReceiver receiver) {
		if (comConnSingleton != null)
			throw new IllegalStateException();
		try {
			comConnSingleton = new ComConnection();
			spawnThread(receiver);
		} catch (Exception e) {
			lg.severe(e, "Konnte serielle Verbindung nicht aufbauen");
		}
	}

	private static void spawnThread(BotReceiver receiver) {
		new ComListenerThread(receiver).start();
	}

	///////////////////////////////////////////////////////////////////////////

	public static class CouldntOpenTheDamnThingException extends Exception {
		private static final long serialVersionUID = 4896454703538812700L;

		public CouldntOpenTheDamnThingException() {
			super();
		}

		public CouldntOpenTheDamnThingException(String message,
		Throwable cause) {
			super(message, cause);
		}

		public CouldntOpenTheDamnThingException(String message) {
			super(message);
		}

		public CouldntOpenTheDamnThingException(Throwable cause) {
			super(cause);
		}
	}
}
