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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;

import ctSim.controller.BotReceiver;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBot;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.FmtLogger;

/**
 * Repräsentiert eine Verbindung
 *
 * @author Benjamin Benz (bbe@heise.de)
 * @author Hendrik Krauß (hkr@heise.de)
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
	 * verwenden, flushen wollen - daher müssen die das machen
	 * mit dem BufferedOutputStream.
	 */
	private DataOutputStream output = null;

	/** OutputStream für Kommandos */
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
	 * Überträgt ein Kommando
	 *
	 * @param c	das Kommando
	 * @throws IOException
	 */
	public void write(Command c) throws IOException {
		output.write(c.getCommandBytes());
		output.flush();
	}

	/**
	 * Liefert den Cmd-Stream
	 *
	 * @return der CommandOutputStream
	 */
	public synchronized CommandOutputStream getCmdOutStream() {
		assert cmdOutStream != null;
		return cmdOutStream;
	}

	/**
	 * Liest Daten aus dem InputStream
	 *
	 * @param b	Daten
	 * @throws IOException
	 */
	public void read(byte[] b) throws IOException {
		input.readFully(b);
	}

	/**
	 * Muss während der Konstruktion aufgerufen werden...
	 *
	 * @param is	InputStream
	 */
	protected void setInputStream(InputStream is) {
		input = new DataInputStream(new BufferedInputStream(is));
	}

	/**
	 * Muss während der Konstruktion aufgerufen werden...
	 *
	 * @param os	OutputStream
	 */
	protected void setOutputStream(OutputStream os) {
		output = new DataOutputStream(os);
		cmdOutStream = new CommandOutputStream(output);
	}

	/**
	 * Gibt den Kurznamen der Connection zurück
	 *
	 * @return Name
	 */
	public abstract String getShortName();

	/**
	 * Gibt den Namen der Connection zurück
	 *
	 * @return Name
	 */
	public abstract String getName();

	/**
	 * Blockiert, bis Handshake erfolgreich oder IOException
	 * Abbruch nach 100 Versuchen
	 *
	 * @param receiver	Bot-Receiver
	 */
	protected void doHandshake(BotReceiver receiver) {
		for (int i = 0; i < 1000; i++) {
			lg.fine("Sende Willkommen");
			try {
				write(new Command(Command.Code.WELCOME));
			} catch (IOException e) {
				lg.severe(e, "E/A-Problem beim Handshake; Abbruch");
				return;
			}
			/* Warten auf Antwort */
			for (int j = 0; j < 100; j++) {
				try {
					Command cmd = new Command(this, true);
					if (cmd.has(Command.Code.WELCOME)) {
						receiver.onBotAppeared(createBot(cmd));
						return; // Erfolg
					} else {
						lg.fine("Kommando, aber kein Willkommen von Verbindung gelesen: Bot läuft schon oder ist "
								+ "veraltet, schicke Willkommen nochmals; ignoriertes Kommando folgt" + cmd);
						continue; // Handshake nochmal versuchen
					}
				} catch (ProtocolException e) {
					lg.severe(e, "Ung\uu00FCltiges Kommando beim Handshake; ignoriere");
					continue;
				} catch (IOException e) {
					lg.severe(e, "E/A-Problem beim Handshake; Abbruch");
					return;
				}
			}
		}
	}

	/**
	 * Erzeugt einen Bot
	 *
	 * @param c	Kommando
	 * @return Bot
	 * @throws ProtocolException
	 */
	protected Bot createBot(Command c) throws ProtocolException {
		CtBot bot;
		switch (c.getSubCode()) {
		case WELCOME_SIM:
			lg.fine("TCP-Verbindung von simuliertem Bot eingegangen");
			bot = new CtBotSimTcp(this, c.getFrom(), c.getDataL());
			break;
		case WELCOME_REAL:
			lg.fine("TCP-Verbindung von realem Bot eingegangen");
			bot = new RealCtBot(this, c.getFrom(), c.getDataL());
			break;
		default:
			throw new ProtocolException(c.toString());
		}

		return bot;
	}
}