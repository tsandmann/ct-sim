package ctSim.util;

/**
 * Threading-Klasse
 */
public abstract class SaferThread extends Thread {
	/** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.util.SaferThread");
	
	/** Abbruchbedingung */
	private volatile boolean deathPending = false;

	/**
	 * @return true/false
	 */
	public boolean isDeathPending() {
		return deathPending;
	}

	/**
	 * die
	 */
	public void die() {
		deathPending = true;
		interrupt();
	}

	/**
	 * work-Methode
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
				// No-op, wir machen mit dispose() weiter
			}
		}
		dispose();
	}
	
	/**
	 * Thread beenden
	 */
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

	/**
	 * Neuer Thread
	 */
	public SaferThread() {
		super();
	}

	/**
	 * Neuer Thread "name"
	 * @param name
	 */
	public SaferThread(String name) {
		super(name);
	}

	/**
	 * Neuer Thread "name" in Gruppe "group"
	 * @param group
	 * @param name
	 */
	public SaferThread(ThreadGroup group, String name) {
		super(group, name);
	}
}
