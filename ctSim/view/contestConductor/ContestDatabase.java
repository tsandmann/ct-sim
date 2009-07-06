/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
 * 
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version. 
 * This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 * 
 */

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
