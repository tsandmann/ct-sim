package ctSim.view.contestConductor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import ctSim.controller.Config;
import ctSim.controller.Main;

public class ContestDatabase {

	private Connection connection;

	static {
		Main.dependencies.registerImplementation(Config.class);
	}

	protected ContestDatabase() {

	}

	public ContestDatabase(Config c)
	throws ClassNotFoundException, SQLException {
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection(
			c.get("contest-database-url"),
			c.get("contest-database-user"),
			c.get("contest-database-password"));
	}

	public Connection getConnection() {
	    return connection;
    }

}
