package ctSim;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.TooManyListenersException;

import ctSim.controller.BotReceiver;
import ctSim.controller.Config;
import ctSim.model.Command;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBot;
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
	/** Input verfuegbar? */
	protected boolean inputAvailable = false;
	/** Mutex */
	protected final Object inputAvailMutex = new Object();
	/** Port */
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
		String baudrate = Config.getValue("serialportBaudrate");
		try {
			int br = Integer.parseInt(baudrate);
			port.setSerialPortParams(br, SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			lg.info("Warte auf Verbindung vom c't-Bot an seriellem Port "+
				comPortName+" ("+br+" baud)");
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			port.enableReceiveTimeout(60000);
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

	/**
	 * Registriert einen Listener
	 */
	private void registerEventListener() {
		port.notifyOnDataAvailable(true);

		class OurEventListener implements SerialPortEventListener {
			public void serialEvent(SerialPortEvent evt) {
				if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
					// Es gibt was zu lesen
					synchronized (inputAvailMutex) {
						inputAvailable = true;
						inputAvailMutex.notify();
					}
				}
			}
		}

		try {
			port.addEventListener(new OurEventListener());
		} catch (TooManyListenersException e) {
			// "Kann nicht passieren"
			throw new AssertionError(e);
		}
	}

	/**
	 * Wartet, bis neue Daten eingetroffen sind
	 * @throws InterruptedException
	 */
	public void blockUntilDataAvailable() throws InterruptedException {
		synchronized (inputAvailMutex) {
			while (! inputAvailable) {
				// while ist Schutz vor spurious wakeups
				// (siehe wait()-Doku)
				inputAvailMutex.wait();
			}
		}
	}

	/**
	 * Liest Daten aus einem Inputstream
	 * @throws IOException
	 */
	@Override
	public void read(byte[] b) throws IOException {
		inputAvailable = false;
		super.read(b);
	}

	/**
	 * Tut nichts: ComConnection ist ein Singleton und soll nie geschlossen
	 * werden. Wir verhindern durch den Override auch, dass die Vaterklasse was
	 * schlie&szlig;t.
	 */
	@Override
	public synchronized void close() {
		// No-op
	}

	/**
	 * Gibt den Namen des Ports unserer Connection zurueck
	 * @return	Name 
	 */
	@Override 
	public String getName() {
		return port.getName(); 
	}

	/**
	 * Gibt den Kurznamen unserer Connection zurueck
	 * @return	"USB"
	 */
	@Override 
	public String getShortName() { 
		return "USB"; 
	}

	///////////////////////////////////////////////////////////////////////////
	// Auto-detect-Kram

	/**
	 * COM-Verbindung
	 */
	private static ComConnection comConnSingleton = null;
	/** Der empfänger der Bots */
	private static BotReceiver botReceiver = null; 
	
	/**
	 * Erzeugt einen Bot
	 * Überschreibt die entsprechende Methode von Connection, weil hier ein paar Sondersachen dazukommen
	 * @param c Kommando
	 * @return Bot
	 * @throws ProtocolException
	 */
	protected Bot createBot(Command c) throws ProtocolException {
		CtBot bot;
		switch (c.getSubCode()) {
    		case WELCOME_REAL:
    			lg.fine("COM-Verbindung von realem Bot eingegangen");
    			bot= new RealCtBot(comConnSingleton,c.getFrom());
    			bot.addDisposeListener(new Runnable() {
    				public void run() {
    					spawnThread(botReceiver);
    				}
    			});
    			break;
    		default:
    			throw new ProtocolException(c.toString());
    	}
		
		return bot;
	}	
	
	/**
	 * Startet das Lauschen fuer neue Bots
	 * @param receiver	BotReceiver fuer neuen Bot
	 */
	public static void startListening(final BotReceiver receiver) {
		if (comConnSingleton != null)
			throw new IllegalStateException();
		try {
			comConnSingleton = new ComConnection();
			botReceiver = receiver;
			spawnThread(receiver);
		} catch (Exception e) {
			lg.severe(e, "Konnte serielle Verbindung nicht aufbauen");
		}
	}

	/**
	 * Neuer Thread
	 * @param receiver Bot-Receiver
	 */
	private static void spawnThread(BotReceiver receiver) {
		new ComListenerThread(receiver).start();
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * Exception-Klasse fuer ComConnection
	 */
	public static class CouldntOpenTheDamnThingException extends Exception {
		/** UID */
		private static final long serialVersionUID = 4896454703538812700L;

		/**
		 * Erzeugt eine neue Exception
		 */
		public CouldntOpenTheDamnThingException() {
			super();
		}

		/**
		 * Erzeugt eine neue Exception
		 * @param message	Text der Exception
		 * @param cause		
		 */
		public CouldntOpenTheDamnThingException(String message,
		Throwable cause) {
			super(message, cause);
		}

		/**
		 * Erzeugt eine neue Exception
		 * @param message	Text der Exception
		 */
		public CouldntOpenTheDamnThingException(String message) {
			super(message);
		}

		/**
		 * Erzeugt eine neue Exception
		 * @param cause
		 */
		public CouldntOpenTheDamnThingException(Throwable cause) {
			super(cause);
		}
	}
	
	/**
	 * Thread, der die COM-Connection ueberwacht
	 */
	static class ComListenerThread extends SaferThread {
		/** Bot-Receiver */
		private final BotReceiver botReceiver;
		
		/**
		 * Erzeugt einen Thread, der auf COM-Connections lauscht
		 * @param receiver	BotReceiver fuer den neuen Bot
		 */
		public ComListenerThread(BotReceiver receiver) {
			super("ctSim-Listener-COM");
			this.botReceiver = receiver;
		}
		
		/**
		 * work-Methode des Threads
		 * @throws InterruptedException
		 */
		@SuppressWarnings("synthetic-access")
		@Override
		public void work() throws InterruptedException {
			comConnSingleton.blockUntilDataAvailable();
			lg.fine("Serielle Verbindung eingegangen");
			
			comConnSingleton.doHandshake(botReceiver);
			
			////////////////
			
//			Bot b = new RealCtBot(comConnSingleton,(byte)0);
//			b.addDisposeListener(new Runnable() {
//				public void run() {
//					spawnThread(botReceiver);
//				}
//			});
//			botReceiver.onBotAppeared(b);
			die();
		}
	}
}
