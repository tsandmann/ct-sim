package ctSim;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
	protected final SerialPort port;
	protected final InputStream in;
	protected final OutputStream out;

	/**
	 * Baut eine COM-Verbindung auf (d.h. i.d.R. zum USB-2-Bot-Adapter).
	 * Verwendet die in der Konfigdatei angegebenen Werte.
	 *
	 * @throws NoSuchPortException
	 * @throws PortInUseException
	 * @throws UnsupportedCommOperationException
	 * @throws NumberFormatException
	 * @throws IOException
	 */
//	$$$ PortInUseExcp bei Verwendern
	public ComConnection() throws NoSuchPortException, PortInUseException,
		NumberFormatException, UnsupportedCommOperationException, IOException {

		CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(
			ConfigManager.getValue("serialport"));
		port = (SerialPort)portId.open("CtSim", 2000);

		//TODO Parameter configurierbar machen
		port.setSerialPortParams(
			Integer.parseInt(ConfigManager.getValue("serialportBaudrate")),
			SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
			SerialPort.PARITY_NONE);
		port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		port.enableReceiveTimeout(60000);

		in = port.getInputStream();
		out = port.getOutputStream();
	}

	/** Schlie&szlig;t den seriellen Port und schlie&szlig;t die Vaterklasse. */
	@Override
	public synchronized void close() throws IOException {
		port.close();
		super.close();
	}

	@Override public String getName() { return port.getName(); }
	@Override public InputStream getInputStream() { return in; }
	@Override public OutputStream getOutputStream() { return out; }
}
