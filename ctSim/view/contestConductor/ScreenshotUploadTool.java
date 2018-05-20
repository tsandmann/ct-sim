/*
 * c't-Sim - Robotersimulator für den c't-Bot
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

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import ctSim.controller.Main;
import ctSim.util.Misc;

/**
 * Element, das ein Verzeichnis voller Screenshots in eine Datenbank hochlädt. Sollte weitgehend
 * selbst-dokumentierend sein...
 */
public class ScreenshotUploadTool {
    /** usage-Meldung */
    public static void usage() {
        System.out.println("Syntax:   java ScreenshotUploadTool dburl dbuser dbpassword");
        System.out.println("Beispiel: java ScreenshotUploadTool 10.10.22.111:3306/ctbot-contest root wurstbrot");
        System.exit(1);
    }

    /**
     * Bereitet eine URL vor
     * 
     * @param rawUrl	URL
     * @return String
     */
    private static String preprocessUrl(String rawUrl) {
   		if (Misc.startsWith(rawUrl, "jdbc:mysql://"))
   			return rawUrl;
        if (Misc.startsWith(rawUrl, "mysql://"))
        	return "jdbc:" + rawUrl;
        return "jdbc:mysql://" + rawUrl;
    }

    /**
     * main
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String... args) throws Exception {
        if (args.length != 3)
            usage();

        final String url = preprocessUrl(args[0]);
       	final String user = args[1];
        final String pw = args[2];

        System.out.printf("Verbinde mit Datenbank %s, User %s, Passwort ", url, user);
        for (int i = 0; i < pw.length(); i++)
        	System.out.print("*");
        System.out.println();

        Main.dependencies.reRegisterInstance(ContestDatabase.class, new ContestDatabase() {
        	@Override
        	public Connection getConnection() {
        		try {
        			Class.forName("com.mysql.jdbc.Driver");
        			return DriverManager.getConnection(url, user, pw);
        		} catch (Exception e) {
        			throw new RuntimeException(e);
        		}
        	}
        });

        DatabaseAdapter da = (DatabaseAdapter)Main.dependencies.
        		getComponentInstanceOfType(ConductorToDatabaseAdapter.class);

        File dir = new File(".");
        int len = dir.list().length;
        System.out.println("Eiere durch Verzeichnis " + dir.getAbsolutePath() +
        		" (" + len + " Sache" + (len == 1 ? "" : "n") +" drin)");
        for (File f : dir.listFiles()) {
        	String name = f.getName();
        	if (! name.endsWith(".png") || ! name.matches(".*\\d.*"))
        		continue;
        	int id = Integer.parseInt(name.replaceAll("[^\\d-]", ""));
        	System.out.println("Datei gefunden: " + name + "; Upload in die DB mit Level-ID " + id);
        	if (da.execSql("SELECT * FROM ctsim_level WHERE id = ?", id).next()) {
        		da.execSql("UPDATE ctsim_level SET screenshot = ? WHERE id = ?", new FileInputStream(f), id);
        	} else {
        		System.err.println("Warnung: Kein Level mit ID " + id + " in der DB");
        	}
        }
    }
}
