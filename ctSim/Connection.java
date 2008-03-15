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

/**
 * Repr&auml;sentiert eine Verbindung
 *
 * @author bbe (bbe@heise.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public abstract class Connection {	
	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger("ctSim.Connection");

	/**
	 * Aufbau:
	 *
	 * <pre>
	 * .-------------------------------------------.
	 * | DataInputStream                           |
	 * |                                           |
	 * | .---------------------------------------. |
	 * | | BufferedInputStream                   | |
	 * | |                                       | |
	 * | | .-----------------------------------. | |
	 * | | | InputStream, den unsere Subklasse | | |
	 * | | | beim Konstruieren gesetzt hat     | | |
	 * | | | ( setInputStream() )              | | |
	 * | | '-----------------------------------' | |
	 * | '---------------------------------------' |
	 * '-------------------------------------------'
	 * </pre>
	 */
	private DataInputStream input = null;

	/**
	 * Hat keinen BufferedOutputStream, denn auf dem muss man (offenbar) immer
	 * flush() aufrufen. Wir wissen jedoch nicht, wann die Leute, die uns
	 * verwenden, flushen wollen &amp;ndash; daher m&uuml;ssen die das machen
	 * mit dem BufferedOutputStream.
	 */
	private DataOutputStream output = null;

	/** OutputStream fuer Kommandos */
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

	/**
	 * Uebertraegt ein Kommando
	 * @param c	das Kommando
	 * @throws IOException
	 */
	public void write(Command c) throws IOException {
		output.write(c.getCommandBytes());
		output.flush();
	}

	/**
	 * Liefert den Cmd-Stream
	 * @return	der CommandOutputStream
	 */
	public synchronized CommandOutputStream getCmdOutStream() {
		assert cmdOutStream != null;
		return cmdOutStream;
	}

	/**
	 * Liest Daten aus dem InputStream
	 * @param b	Daten
	 * @throws IOException
	 */
	public void read(byte[] b) throws IOException {
		input.readFully(b);
	}

	/** 
	 * Muss waehrend Konstruktion aufgerufen werden
	 * @param is InputStream
	 */
	protected void setInputStream(InputStream is) {
		input = new DataInputStream(new BufferedInputStream(is));
	}

	/**
	 * Muss waehrend Konstruktion aufgerufen werden
	 * @param os OutputStream
	 */
	protected void setOutputStream(OutputStream os) {
		output = new DataOutputStream(os);
		cmdOutStream = new CommandOutputStream(output);
	}

	/**
	 * Gibt den Kurznamen der Connection zurueck
	 * @return	Name
	 */
	public abstract String getShortName();

	/**
	 * Gibt den Namen der Connection zurueck
	 * @return	Name
	 */
	public abstract String getName();
}
