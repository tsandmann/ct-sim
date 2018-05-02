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

package ctSim.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * <p>
 * Formatter, der ausführliche Log-Meldungen produziert. Baut auf das java.util.logging-Framework auf.
 * </p>
 * <p>
 * <strong>Beispielausgabe:</strong>
 *
 * <pre>[18 Oct 06 13:32:51.441] INFO: Elefanten sind i.d.R. grau [ctSim.controller.info() Thread (10)]</pre>
 *
 * Format ist also
 * "[Zeit-der-Logausgabe] Loglevel: Meldung [Quellpaket.Quellmethode() Thread Threadname-falls-vorhanden
 * (Thread-ID)]".
 * Falls der Logmeldung eine Exception mitgegeben wird, sieht die Ausgabe so aus:
 *
 * <pre>[10 Oct 06 13:53:36.623] SEVERE: TCP-Verbindungen haben sich ganz schlimm verwurstelt
 * [ctSim.model.bots.ctbot.CtBotSimTcp.severe() Thread AWT-Shutdown (18)]
 * java.io.IOException
 * 		at java.io.DataInputStream.readFully(DataInputStream.java:178)
 * 		at ctSim.Connection.read(Connection.java:180)
 * 		at ctSim.model.Command.readCommand(Command.java:322)
 * 		at ctSim.model.bots.ctbot.CtBotSimTcp.receiveCommands(CtBotSimTcp.java:973)
 * 		at ctSim.model.bots.ctbot.CtBotSimTcp.work(CtBotSimTcp.java:779)
 * 		at ctSim.model.AliveObstacle.run(AliveObstacle.java:393)
 * 		at java.lang.Thread.run(Thread.java:595)
 * </pre>
 * </p>
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public class CtSimFormatter extends Formatter {
	/** Format */
	private final SimpleDateFormat timestampFormatter =
			new SimpleDateFormat("d MMM y H:mm:ss.SSS");

	/**
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	@Override
	public String format(LogRecord r) {
		String throwable = "";
		if (r.getThrown() != null) {
			StringWriter s = new StringWriter();
			r.getThrown().printStackTrace(new PrintWriter(s));
			throwable = s.toString();
		}
		// "*2" ist quick and dirty. Kommt daher, dass Thread.activeCount() nur Schätzungen über die
		// Größe zurückgibt; für Details siehe Doku der Methode
		Thread[] threads = new Thread[Thread.activeCount() * 2];
		Thread.enumerate(threads);
		String threadName = "";
		for (Thread t : threads) {
			if (t != null && t.getId() == r.getThreadID()) {
				threadName = t.getName() + " ";
				break;
			}
		}

		return "[" + timestampFormatter.format(r.getMillis()) + "] " +
		r.getLevel().getName() + ": " + r.getMessage() +
		" [" + r.getLoggerName() + "."
		+ r.getSourceMethodName() + "() " +
		"Thread " + threadName + "(" + r.getThreadID() + ")" +
		"]\n" + throwable;
	}
}