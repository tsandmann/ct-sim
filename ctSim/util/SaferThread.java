package ctSim.util;

//$$ doc
public abstract class SaferThread extends Thread {
	final FmtLogger lg = FmtLogger.getLogger("ctSim.util.SaferThread");
	
	private volatile boolean deathPending = false;

	public boolean isDeathPending() {
		return deathPending;
	}

	public void die() {
		deathPending = true;
		interrupt();
	}

	public abstract void work() throws InterruptedException;

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
	
	public void dispose() {
		lg.fine("Thread "+getName()+" stirbt");
	}
	
	@Override
	public synchronized void start() {
		lg.fine("Thread "+getName()+" startet");
		super.start();
	}

	public SaferThread() {
		super();
	}

	public SaferThread(String name) {
		super(name);
	}

	public SaferThread(ThreadGroup group, String name) {
		super(group, name);
	}
}
