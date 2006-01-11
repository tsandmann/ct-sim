package ctSim;

/**
 * Klasse um Fehler zentral auszugeben
 * @author Benjamin Benz (bbe@heise.de)
 */
public class ErrorHandler {
	/**
	 * Meldet einen Fehler
	 * @param code Der Fehler
	 */
	public static void error(String code){
		System.err.println(code);
	}
}
