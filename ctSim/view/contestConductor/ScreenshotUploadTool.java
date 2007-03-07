package ctSim.view.contestConductor;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import ctSim.controller.Main;
import ctSim.util.Misc;

/**
 * Dings, das ein Verzeichnis voller Screenshots in eine Datenbank
 * hochl&auml;dt. Sollte weitgehend selbstdokumentierend sein (hua hua).
 */
public class ScreenshotUploadTool {
    public static void usage() {
        System.out.println(
        	"Syntax:   java ScreenshotUploadTool dburl dbuser dbpassword");
        System.out.println(
        	"Beispiel: java ScreenshotUploadTool " +
        	"10.10.22.111:3306/ctbot-contest root wurstbrot");
        System.exit(1);
    }

    private static String preprocessUrl(String rawUrl) {
   		if (Misc.startsWith(rawUrl, "jdbc:mysql://"))
   			return rawUrl;
        if (Misc.startsWith(rawUrl, "mysql://"))
        	return "jdbc:" + rawUrl;
        return "jdbc:mysql://" + rawUrl;
    }

    public static void main(String... args) throws Exception {
        if (args.length != 3)
            usage();

        final String url = preprocessUrl(args[0]);
       	final String user = args[1];
        final String pw = args[2];

        System.out.printf("Verbinde mit Datenbank %s, User %s, Passwort ",
        	url, user);
        for (int i = 0; i < pw.length(); i++)
        	System.out.print("*");
        System.out.println();

        Main.dependencies.reRegisterInstance(ContestDatabase.class,
            new ContestDatabase() {
            @Override
            public Connection getConnection() {
            	try {
	                Class.forName("com.mysql.jdbc.Driver");
	                return DriverManager.getConnection(
	                    url, user, pw);
            	} catch (Exception e) {
            		throw new RuntimeException(e);
            	}
            }
        });

        DatabaseAdapter da = (DatabaseAdapter)Main.dependencies.
        	getComponentInstanceOfType(ConductorToDatabaseAdapter.class);

        File dir = new File(".");
        int len = dir.list().length;
        System.out.println("Eiere durch Verzeichnis " +
        	dir.getAbsolutePath() + " (" + len + " Sache" +
        	(len == 1 ? "" : "n") +" drin)");
        for (File f : dir.listFiles()) {
        	String name = f.getName();
        	if (! name.endsWith(".png") || ! name.matches(".*\\d.*"))
        		continue;
        	int id = Integer.parseInt(name.replaceAll("[^\\d-]", ""));
        	System.out.println("Datei gefunden: " + name + "; Upload in die " +
        			"DB mit Level-ID " + id);
        	if (da.execSql("SELECT * FROM ctsim_level WHERE id = ?", id).
        		next()) {
        		da.execSql("UPDATE ctsim_level SET screenshot = ? WHERE id = ?",
        			new FileInputStream(f), id);
        	} else {
        		System.err.println("Warnung: Kein Level mit ID " + id + 
        			" in der DB");
        	}
        }
    }
}
