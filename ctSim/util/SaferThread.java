package ctSim.util;

//$$ doc
public abstract class SaferThread extends Thread {
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
		// No-op
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
