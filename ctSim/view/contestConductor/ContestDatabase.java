package ctSim.view.contestConductor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ctSim.controller.Config;
import ctSim.controller.Main;

//$$ doc ContestDatabase -- ist eigentlich ne Factory
//$$ Ganze Klasse ueberarbeiten
public class ContestDatabase {

	static {
		Main.dependencies.registerImplementation(Config.class);
	}

	protected ContestDatabase() {

	}

	public ContestDatabase(Config c)
	throws ClassNotFoundException {
		Class.forName("com.mysql.jdbc.Driver");
	}

	public Connection getConnection()  {
		try {
			Config c = Main.dependencies.get(Config.class);
		    return DriverManager.getConnection(
				c.get("contest-database-url"),
				c.get("contest-database-user"),
				c.get("contest-database-password"));
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
    }

}
