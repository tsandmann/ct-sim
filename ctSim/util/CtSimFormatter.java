package ctSim.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

//$$ doc CtSimFormatter
public class CtSimFormatter extends Formatter {
	private SimpleDateFormat df = new SimpleDateFormat(
		"d MMM y H:mm:ss.SSS");

	@Override
	public String format(LogRecord r) {
		String throwable = "";
		if (r.getThrown() != null) {
			StringWriter s = new StringWriter();
			r.getThrown().printStackTrace(new PrintWriter(s));
			throwable = s.toString();
		}
		// TODO "* 2" ist quick and dirty
		Thread[] threads = new Thread[Thread.activeCount() * 2];
		Thread.enumerate(threads);
		String threadName = "";
		for (Thread t : threads) {
			if (t != null && t.getId() == r.getThreadID()) {
				threadName = t.getName() + " ";
				break;
			}
		}

		return "[" + df.format(r.getMillis()) + "] " +
			r.getLevel() + ": " + r.getMessage() +
			" [" + r.getLoggerName() + "."
		    + r.getSourceMethodName() + "() " +
		    "Thread " + threadName + "(" + r.getThreadID() + ")" +
		    "]\n" + throwable;
    }
}