package ctSim.model.bots;

/**
 * Interface fuer alle simulierten Bots
 */
public interface SimulatedBot extends Bot {
	
	/**
	 * Diese Routine kommt pro Simulationsschritt einmal dran. 
	 * Hier muss daher alles rein, was ein Bot tun soll
	 * @throws InterruptedException
	 * @throws UnrecoverableScrewupException
	 */
	public void doSimStep() 
	throws InterruptedException, UnrecoverableScrewupException;

	/**
	 * Exception-Klasse fuer simulierte Bots
	 */
	public static class UnrecoverableScrewupException extends Exception {
		/** UID */
		private static final long serialVersionUID = 9162073871640062415L;

		/**
		 * UnrecoverableScrewupException
		 */
		public UnrecoverableScrewupException() {
			super();
		}

		/**
		 * UnrecoverableScrewupException
		 * @param message Text
		 */
		public UnrecoverableScrewupException(String message) {
			super(message);
		}

		/**
		 * UnrecoverableScrewupException
		 * @param cause
		 */
		public UnrecoverableScrewupException(Throwable cause) {
			super(cause);
		}

		/**
		 * UnrecoverableScrewupException
		 * @param message Text
		 * @param cause
		 */
		public UnrecoverableScrewupException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
