package ctSim.util;

/**
 * Runnable-Interface
 * @param <T> Typ
 */
public interface Runnable1<T> extends java.util.EventListener {
	/**
	 * @param argument
	 */
	public void run(T argument);
}
