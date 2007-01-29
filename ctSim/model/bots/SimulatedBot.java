package ctSim.model.bots;

//$$ doc
public interface SimulatedBot extends Bot {
	
	public void doSimStep() 
	throws InterruptedException, UnrecoverableScrewupException;

	public static class UnrecoverableScrewupException extends Exception {
		private static final long serialVersionUID = 9162073871640062415L;

		public UnrecoverableScrewupException() {
			super();
		}

		public UnrecoverableScrewupException(String message) {
			super(message);
		}

		public UnrecoverableScrewupException(Throwable cause) {
			super(cause);
		}

		public UnrecoverableScrewupException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
