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
import ctSim.model.Command;
import ctSim.util.FmtLogger;

/**
 * Kuemmert sich um die Beantwortung eingehender Kommanods
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */
public class AnsweringMachine extends Thread {
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.AnsweringMachine");

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
		this.run = false;
		this.interrupt();
		this.bot.die(); // Alles muss sterben
	}

	/**
	 * Kuemmert sich um die Beantwortung eingehender Kommanods
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		long start, duration;
		super.run();
		
		
		int valid = 0;
		while (this.run) {
			try {
				Command command = new Command();

				start= System.nanoTime();
				valid = command.readCommand(this.con);
				duration= (System.nanoTime()-start)/1000;
				System.out.println("habe auf Kommando "+(char)command.getCommand()+" "+duration+" usec gewartet");

				//System.out.println("incoming command");
				if (valid == 0) {// Kommando ist in Ordnung
					bot.storeCommand(command);
//					this.bot.evaluate_command(command);
				} else
					lg.warn("Ung\u00FCltiges Kommando");
			} catch (IOException e) {
				lg.severe(e, "Verbindung unterbrochen -- Bot stirbt");
				die();
			}
		}
		die();
	}
}