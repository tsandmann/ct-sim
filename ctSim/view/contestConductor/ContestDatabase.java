package ctSim.view.contestConductor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ctSim.controller.Config;
import ctSim.view.contestConductor.ConductorTestUtil.TestDatabase;

/**
 * Repr&auml;sentiert die Datenbank, die alles &uuml;ber den Wettbewerb
 * (&quot;contest&quot;) wei&szlig;. Idee: Die Klassen, die die Datenbank
 * verwenden, holen sie von hier. Daher k&ouml;nnen Unit-Tests einfach diese
 * Klasse ableiten und getConnection() &uuml;berschreiben, um dem
 * Contest-Conductor eine Test-Datenbank unterzuschieben. Beispiel siehe
 * {@link TestDatabase}.
 */
public class ContestDatabase {
	/**
	 * @return Connection
	 */
	public Connection getConnection()  {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// "Kann nicht passieren"
			throw new AssertionError();
		}

		try {
		    return DriverManager.getConnection(
				Config.getValue("contest-database-url"),
				Config.getValue("contest-database-user"),
				Config.getValue("contest-database-password"));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
    }
}
