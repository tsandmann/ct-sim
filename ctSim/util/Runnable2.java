package ctSim.util;

/**
 * Runnable-Interface
 * @param <T1> Typ 1
 * @param <T2> Typ 2
 */
public interface Runnable2<T1, T2> extends java.util.EventListener {
	/**
	 * @param argument1
	 * @param argument2
	 */
	public void run(T1 argument1, T2 argument2);
}
