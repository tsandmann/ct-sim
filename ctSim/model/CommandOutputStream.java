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

package ctSim.model;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import ctSim.util.BotID;
import ctSim.util.Misc;

/** Output-Stream für Kommandos */
public class CommandOutputStream {
	/** Puffer */
	private final Map<Command.Code, Command> buffer = Misc.newMap();
	/** der Strean */
	private final BufferedOutputStream underlyingStream;
	/** Sequenznummer */
	private byte seq = 0;

	/** An wen gehen die Pakete? */
	private BotID to = new BotID();
	
	/**
	 * @param underlyingStream
	 */
	public CommandOutputStream(OutputStream underlyingStream) {
		if (underlyingStream == null)
			throw new NullPointerException();
		this.underlyingStream = new BufferedOutputStream(underlyingStream);
	}

	/**
	 * @param c	Command-Code
	 * @return Command
	 */
	public synchronized Command getCommand(Command.Code c) {
		if (! buffer.containsKey(c))
			buffer.put(c, new Command(c));
		return buffer.get(c);
	}

	/**
	 * Schreibt ein Kommando, toleriert null schweigend
	 * 
	 * @param c	Kommando
	 * @throws IOException 
	 */
	private synchronized void write(Command c) throws IOException {
		if (c == null)
			return;
		c.setSeq(seq++);
		c.setTo(to);
		underlyingStream.write(c.getCommandBytes());
	}

	/**
	 * Reihenfolge wichtig, sonst kommt der Bot-Steuercode durcheinander 
	 * 1. Alles außer SENS_ERROR und DONE
	 * 2. SENS_ERROR
	 * 3. DONE
	 * 
	 * @throws IOException
	 */
	public synchronized void flush() throws IOException {
		Command error = buffer.remove(Command.Code.SENS_ERROR);	// ist evtl. null
		Command done = buffer.remove(Command.Code.DONE);	// ist evtl. null

		for (Command c : buffer.values())
			write(c);
		write(error); 
		write(done);

		underlyingStream.flush();
		buffer.clear();
	}

	/** 
	 * Setzt den Empfänger aller Kommandos
	 * 
	 * @param to
	 */
	public void setTo(BotID to) {
		this.to = to;
	}

	/** 
	 * Liefert den Empfänger aller Kommandos
	 * 
	 * @return Empfänger-Id
	 */
	public BotID getTo() {
		return new BotID(to);
	}
}
