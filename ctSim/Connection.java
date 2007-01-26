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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.util.FmtLogger;

//$$ doc
/**
 * Repr&auml;sentiert eine Verbindung
 *
 * @author bbe (bbe@heise.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public abstract class Connection {
	final FmtLogger lg = FmtLogger.getLogger("ctSim.Connection");

	/**
	 * Hat keinen BufferedOutputStream, denn auf dem muss man (offenbar) immer
	 * flush() aufrufen. Wir wissen jedoch nicht, wann die Leute, die uns
	 * verwenden, flushen wollen &amp;ndash; daher m&uuml;ssen die das machen
	 * mit dem BufferedOutputStream.
	 */
	private DataOutputStream output = null;
	
	/**
	 * Aufbau:
	 *
	 * <pre>
	 * .------------------------------------------.
	 * | DataInputStream                          |
	 * |                                          |
	 * | .--------------------------------------. |
	 * | | BufferedInputStream                  | |
	 * | |                                      | |
	 * | | .----------------------------------. | |
	 * | | | InputStream, den wir von unserer | | |
	 * | | | Subklasse geholt haben           | | |
	 * | | | ( getInputStream() )             | | |
	 * | | `----------------------------------´ | |
	 * | `--------------------------------------´ |
	 * `------------------------------------------´
	 * </pre>
	 */
	private DataInputStream input = null;

	private CommandOutputStream cmdOutStream = null;
	
	/**
	 * Beendet die laufende Verbindung
	 *
	 * @throws IOException
	 */
	public synchronized void close() throws IOException {
		if (input != null)
			input.close();
		if (output != null)
			output.close();
	}

	//$$$ Umstellen, so dass man Con an Command gibt; Methode weg
	/**
	 * Uebertraegt Daten
	 *
	 * @throws IOException
	 */
	public void write(Command c) throws IOException {
		if (output == null)
			output = new DataOutputStream(getOutputStream());
		output.write(c.getCommandBytes());
		output.flush();
	}

	// Singleton mit lazy init fuer bessere Performance
	public synchronized CommandOutputStream getCmdOutStream() {
		if (cmdOutStream == null)
			cmdOutStream = new CommandOutputStream(output);
		return cmdOutStream;
	}

	public void read(byte[] b) throws IOException {
		if (input == null) {
			input = new DataInputStream(new BufferedInputStream(
				getInputStream()));
		}
		input.readFully(b);
	}

	protected abstract InputStream getInputStream() throws IOException;
	protected abstract OutputStream getOutputStream() throws IOException;

	public abstract String getName();
}
