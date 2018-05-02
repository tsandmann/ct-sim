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

/** Threading-Klasse */
public abstract class SaferThread extends Thread {
	/** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.util.SaferThread");

	/** Abbruch-Bedingung */
	private volatile boolean deathPending = false;

	/**
	 * @return true/false
	 */
	public boolean isDeathPending() {
		return deathPending;
	}

	/** die */
	public void die() {
		deathPending = true;
		interrupt();
	}

	/**
	 * work-Methode
	 *
	 * @throws InterruptedException
	 */
	public abstract void work() throws InterruptedException;

	/**
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		while (! deathPending) {
			try {
				work();
			} catch (InterruptedException e) {
				// wir machen mit dispose() weiter
			}
		}
		dispose();
	}

	/** Thread beenden */
	public void dispose() {
		lg.fine("Thread "+getName()+" stirbt");
	}

	/**
	 * @see java.lang.Thread#start()
	 */
	@Override
	public synchronized void start() {
		lg.fine("Thread "+getName()+" startet");
		super.start();
	}

	/** Neuer Thread */
	public SaferThread() {
		super();
	}

	/**
	 * Neuer Thread "name"
	 *
	 * @param name
	 */
	public SaferThread(String name) {
		super(name);
	}

	/**
	 * Neuer Thread "name" in Gruppe "group"
	 *
	 * @param group
	 * @param name
	 */
	public SaferThread(ThreadGroup group, String name) {
		super(group, name);
	}
}