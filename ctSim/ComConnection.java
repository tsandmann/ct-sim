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

import com.fazecast.jSerialComm.*;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.logging.Level;

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
 * Die <a href="http://fazecast.github.io/jSerialComm/">jSerialComm-Dokumentation</a>
 * beschreibt die API, die in dieser Klasse verwendet wird ({@com.fazecast.jSerialComm.*}).
 * </p>
 *
 * @author Maximilian Odendahl (maximilian.odendahl@rwth-aachen.de)
 * @author Hendrik Krauß &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 * @author Timo Sandmann
 */
public class ComConnection extends Connection {
	/** Input verfuegbar? */
	protected boolean inputAvailable = false;
	/** Mutex */
	protected final Object inputAvailMutex = new Object();
	/** Port */
	private SerialPort port;

	/**
	 * Baut eine COM-Verbindung auf (d.h. i.d.R. zum USB-2-Bot-Adapter).
	 * Verwendet die in der Konfigdatei angegebenen Werte.
	 *
	 * @throws CouldntOpenTheDamnThingException
	 */
	private ComConnection() throws CouldntOpenTheDamnThingException {
		String comPortName = Config.getValue("serialport");
		String baudrate = Config.getValue("serialportBaudrate");

		port = SerialPort.getCommPort(comPortName);
		port.closePort();
		if (! port.openPort()) {
			throw new CouldntOpenTheDamnThingException("Serial Port '" + comPortName + "' ungueltig");
		}
		
		try {
			int br = Integer.parseInt(baudrate);
			port.setComPortParameters(br, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
			port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 1);
			lg.info("Warte auf Verbindung vom c't-Bot an seriellem Port " + comPortName + " (" + br + " baud)");
		} catch (NumberFormatException e) {
			port.closePort();
			throw new CouldntOpenTheDamnThingException("Die Baud-Rate '" + baudrate + "' ist keine gültige Zahl", e);
		}

		setInputStream(port.getInputStream());
		setOutputStream(port.getOutputStream());

		registerEventListener();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				port.closePort();
				lg.fine("Serial Port is closed!");
			}
		});
	}

	/**
	 * Registriert einen Listener
	 */
	private void registerEventListener() {
		class OurEventListener implements SerialPortDataListener {
			@Override
			public int getListeningEvents() { 
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
			}
			
			@Override
			public void serialEvent(SerialPortEvent evt) {
				if (evt.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
					// Es gibt was zu lesen
					synchronized (inputAvailMutex) {
						inputAvailable = true;
						inputAvailMutex.notify();
					}
				}
			}
		}

		port.addDataListener(new OurEventListener());
	}

	/**
	 * Wartet, bis neue Daten eingetroffen sind
	 * @throws InterruptedException
	 */
	public void blockUntilDataAvailable() throws InterruptedException {
		synchronized (inputAvailMutex) {
			while (! inputAvailable) {
				// while ist Schutz vor spurious wakeups (siehe wait()-Doku)
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
	 * schließt.
	 */
	@Override
	public synchronized void close() {
		// No-op
	}

	/**
	 * Gibt den Namen des Ports unserer Connection zurück
	 * @return	Name 
	 */
	@Override 
	public String getName() {
		return port.getDescriptivePortName(); 
	}

	/**
	 * Gibt den Kurznamen unserer Connection zurück
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
	/** Der Empfänger der Bots */
	private static BotReceiver botReceiver = null; 
	
	/**
	 * Erzeugt einen Bot
	 * Überschreibt die entsprechende Methode von Connection, weil hier ein paar Sondersachen dazukommen
	 * @param c Kommando
	 * @return Bot
	 * @throws ProtocolException
	 */
	@Override
	protected Bot createBot(Command c) throws ProtocolException {
		CtBot bot;
		switch (c.getSubCode()) {
    		case WELCOME_REAL:
    			lg.fine("COM-Verbindung von realem Bot eingegangen");
    			bot = new RealCtBot(comConnSingleton, c.getFrom(), c.getDataL());
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
	 * Startet das Lauschen für neue Bots
	 * @param receiver	BotReceiver für neuen Bot
	 */
	public static void startListening(final BotReceiver receiver) {
		if (comConnSingleton != null)
			throw new IllegalStateException();
		try {
			comConnSingleton = new ComConnection();
			botReceiver = receiver;
			spawnThread(receiver);
		} catch (Exception e) {
			lg.warn("Konnte serielle Verbindung nicht aufbauen");
			
			if (Level.parse(Config.getValue("LogLevel")).intValue() <= Level.FINE.intValue()) {
				e.printStackTrace(System.out);
			}
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
	 * Exception-Klasse für ComConnection
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
		public CouldntOpenTheDamnThingException(String message, Throwable cause) {
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
	 * Thread, der die COM-Connection überwacht
	 */
	static class ComListenerThread extends SaferThread {
		/** Bot-Receiver */
		private final BotReceiver botReceiver;
		
		/**
		 * Erzeugt einen Thread, der auf COM-Connections lauscht
		 * @param receiver	BotReceiver für den neuen Bot
		 */
		public ComListenerThread(BotReceiver receiver) {
			super("ctSim-Listener-COM");
			this.botReceiver = receiver;
		}
		
		/**
		 * work-Methode des Threads
		 * @throws InterruptedException
		 */
		@Override
		public void work() throws InterruptedException {
			comConnSingleton.blockUntilDataAvailable();
			lg.fine("Serielle Verbindung eingegangen");
			
			comConnSingleton.doHandshake(botReceiver);
			
			////////////////
			
//			Bot b = new RealCtBot(comConnSingleton, (byte) 0);
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
