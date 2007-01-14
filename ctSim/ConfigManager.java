package ctSim;

import ctSim.util.FmtLogger;

//$$ Uffraeumen, evtl. umbenennen
public class ConfigManager {
	static FmtLogger lg = FmtLogger.getLogger("ctSim.ConfigManager");

	/**
	 * Wandelt einen Pfad mit Bot-Binary von Windows zu Linux
	 * @param in der Originalstring
	 * @return
	 */
	private static String botPathWin2Lin(String in){
		if (in == null)
			return null;
		String tmp = in.replace('\\','/');
		tmp= tmp.replace("exe","elf");
		return tmp.replace("Debug-W32","Debug-Linux");
	}

	/**
	 * Wandelt einen Pfad mit Bot-Binary von Linux zu Windows
	 * @param in der Originalstring
	 * @return
	 */
	private static String botPathLin2Win(String in){
		if (in == null)
			return null;
		String tmp = in.replace('/','\\');
		tmp= tmp.replace("elf","exe");
		return tmp.replace("Debug-Linux","Debug-W32");
	}

	/**
	 * Passt einen Pfad an das aktuelle OS an
	 * @param in der Originalstring
	 * @return
	 */
	public static String path2Os(String in){
		if (System.getProperty("os.name").indexOf("Linux") >=0)
			return botPathWin2Lin(in);
		else if (System.getProperty("os.name").indexOf("OS X") >=0)
			return botPathWin2Lin(in);
		else
			return botPathLin2Win(in);
	}
}
