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

package ctSim.model.bots;

import java.io.IOException;

import ctSim.Connection;
import ctSim.ErrorHandler;
import ctSim.model.Command;
import ctSim.model.bots.TcpBot;

/**
 * Kuemmert sich um die Beantwortung eingehender Kommanods
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */
public class AnsweringMachine extends Thread {

	/** Soll der Thread noch laufen? */
	private boolean run = true;

	/** Verweis auf den zugehoerigen Bot*/
	private TcpBot bot = null;

	/** Die TCP-Verbindung */
	private Connection con;

	/**
	 * Erzeugt einen neuen Anrufbeantworter
	 * @param b Der zugehoerige Bot
	 * @param c Die Verbindung
	 */
	public AnsweringMachine(TcpBot b, Connection c) {
		super();
		this.bot = b;
		this.con = c;
	}

	/**
	 * Beendet den Thread<b>
	 * 
	 * @see Bot#work()
	 */
	public void die() {
		run = false;
		this.interrupt();
		bot.die(); // Alles muss sterben
	}

	/**
	 * Kuemmert sich um die Beantwortung eingehender Kommanods
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		super.run();
		Command command = new Command();
		int valid = 0;
		while (run) {
			try {
				valid = command.readCommand(con);
				//System.out.println("incoming command");
				if (valid == 0) {// Kommando ist in Ordnung
					bot.evaluate_command(command);
				} else
					System.out.println("Ungueltiges Kommando"); //$NON-NLS-1$
			} catch (IOException ex) {
				ErrorHandler.error("Verbindung unterbrochen -- Bot stirbt: " + ex); //$NON-NLS-1$
				die();
			}
		}
		die();
	}
}