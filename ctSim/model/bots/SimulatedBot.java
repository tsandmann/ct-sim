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

package ctSim.model.bots;

/** Interface für alle simulierten Bots */
public interface SimulatedBot extends Bot {
	
	/**
	 * Diese Routine kommt pro Simulationsschritt einmal dran. 
	 * Hier muss daher alles rein, was ein Bot tun soll
	 * 
	 * @throws InterruptedException
	 * @throws UnrecoverableScrewupException
	 */
	public void doSimStep() 
	throws InterruptedException, UnrecoverableScrewupException;

	/** Exception-Klasse für simulierte Bots */
	public static class UnrecoverableScrewupException extends Exception {
		/** UID */
		private static final long serialVersionUID = 9162073871640062415L;

		/** UnrecoverableScrewupException */
		public UnrecoverableScrewupException() {
			super();
		}

		/**
		 * UnrecoverableScrewupException
		 * 
		 * @param message	Text
		 */
		public UnrecoverableScrewupException(String message) {
			super(message);
		}

		/**
		 * UnrecoverableScrewupException
		 * 
		 * @param cause	Grund
		 */
		public UnrecoverableScrewupException(Throwable cause) {
			super(cause);
		}

		/**
		 * UnrecoverableScrewupException
		 * 
		 * @param message	Text
		 * @param cause		Grund
		 */
		public UnrecoverableScrewupException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
