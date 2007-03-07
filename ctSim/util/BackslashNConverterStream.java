package ctSim.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * Stream, der Sachen ausgibt und dabei Newlines (\n, 0xA) in der
 * plattformüblichen Weise schreibt. Soll heißen: Wenn die Applikation auf
 * Windows läuft, wird das \n in \r\n (0xD 0xA) konvertiert. Wichtig beim
 * Schreiben von Text-Dateien.
 * </p>
 * <p>
 * Der Stream wrappt einen anderen Stream (gibt nichts selber aus).
 * </p>
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class BackslashNConverterStream extends OutputStream {
	private final OutputStream underlyingStream;
	private final byte[] lineEnding =
		System.getProperty("line.separator").getBytes();

	/**
	 * Erzeugt eine Instanz, die Eingaben konvertiert und an den übergebenen
	 * Stream weiterreicht.
	 */
	public BackslashNConverterStream(OutputStream underlyingStream) {
		this.underlyingStream = underlyingStream;
	}

	@Override
	public void write(int b) throws IOException {
		if (b == '\n')
			underlyingStream.write(lineEnding);
		else
			underlyingStream.write(b);
	}
}
