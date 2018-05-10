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

package ctSim.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * Stream, der Sachen ausgibt und dabei Newlines (\n, 0xA) in der plattformüblichen Weise schreibt.
 * Soll heißen: Wenn die Applikation auf Windows läuft, wird das \n in \r\n (0xD 0xA) konvertiert.
 * Wichtig beim Schreiben von Text-Dateien.
 * </p>
 * <p>
 * Der Stream wrappt einen anderen Stream (gibt nichts selber aus).
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class BackslashNConverterStream extends OutputStream {
	/** Output-Stream */
	private final OutputStream underlyingStream;
	/** Zeilenende */
	private final byte[] lineEnding =
			System.getProperty("line.separator").getBytes();

	/**
	 * Erzeugt eine Instanz, die Eingaben konvertiert und an den übergebenen Stream weiterreicht.
	 *
	 * @param underlyingStream
	 */
	public BackslashNConverterStream(OutputStream underlyingStream) {
		this.underlyingStream = underlyingStream;
	}

	/**
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		if (b == '\n')
			underlyingStream.write(lineEnding);
		else
			underlyingStream.write(b);
	}
}
