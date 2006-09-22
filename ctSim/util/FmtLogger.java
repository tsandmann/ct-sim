package ctSim.util;

import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

//$$ doc ganze Klasse
// Kurzschreibweise (throwable) weicht ab von log()-Signatur
public class FmtLogger extends Logger {
	private Logger delegate;

	protected FmtLogger(String name, Logger delegate) {
	    super(name, null);
	    this.delegate = delegate;
    }

	//$$ auch Aufruf mit .class zulassen; flaechendeckend einfuehren
	public static FmtLogger getLogger(String name) {
		//$$ Ist das ok? Wir erzeugen abweichend von Logger.getLoggers Verhalten jedesmal ne neue Instanz
		return new FmtLogger(name, Logger.getLogger(name));
	}

	public void warn(String msg) {
		delegate.warning(msg);
	}

	//////////////////////////////////////////////////////////////////////////
	// Kurzschreibweise mit Throwable

	public void severe(Throwable t, String msg) {
		delegate.log(Level.SEVERE, msg, t);
	}

	public void warning(Throwable t, String msg) {
		delegate.log(Level.WARNING, msg, t);
	}

	public void warn(Throwable t, String msg) {
		warning(t, msg);
	}

	public void info(Throwable t, String msg) {
		delegate.log(Level.INFO, msg, t);
	}

	public void config(Throwable t, String msg) {
		delegate.log(Level.CONFIG, msg, t);
	}

	public void fine(Throwable t, String msg) {
		delegate.log(Level.FINE, msg, t);
	}

	public void finer(Throwable t, String msg) {
		delegate.log(Level.FINER, msg, t);
	}

	public void finest(Throwable t, String msg) {
		delegate.log(Level.FINEST, msg, t);
	}

	//////////////////////////////////////////////////////////////////////////
	// Kurzschreibweisen mit Format-Strings

	public void severe(String formatString, Object... parameters) {
		delegate.log(Level.SEVERE, String.format(formatString, parameters));
	}

	public void warning(String formatString, Object... parameters) {
		delegate.log(Level.WARNING, String.format(formatString, parameters));
	}

	public void warn(String formatString, Object... parameters) {
		warning(formatString, parameters);
	}

	public void info(String formatString, Object... parameters) {
		delegate.log(Level.INFO, String.format(formatString, parameters));
	}

	public void config(String formatString, Object... parameters) {
		delegate.log(Level.CONFIG, String.format(formatString, parameters));
	}

	public void fine(String formatString, Object... parameters) {
		delegate.log(Level.FINE, String.format(formatString, parameters));
	}

	public void finer(String formatString, Object... parameters) {
		delegate.log(Level.FINER, String.format(formatString, parameters));
	}

	public void finest(String formatString, Object... parameters) {
		delegate.log(Level.FINEST, String.format(formatString, parameters));
	}

	public void severe(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.SEVERE, String.format(formatString, parameters), t);
	}

	public void warning(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.WARNING, String.format(formatString, parameters), t);
	}

	public void warn(Throwable t, String formatString, Object... parameters) {
		warning(t, formatString, parameters);
	}

	public void info(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.INFO, String.format(formatString, parameters), t);
	}

	public void config(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.CONFIG, String.format(formatString, parameters), t);
	}

	public void fine(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.FINE, String.format(formatString, parameters), t);
	}

	public void finer(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.FINER, String.format(formatString, parameters), t);
	}

	public void finest(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.FINEST, String.format(formatString, parameters), t);
	}


	//////////////////////////////////////////////////////////////////////////
	// Alptraum: Delegate-Zeugs

	@Override
    public synchronized void addHandler(Handler handler) throws SecurityException {
	    delegate.addHandler(handler);
    }

	@Override
    public void config(String msg) {
	    delegate.config(msg);
    }

	@Override
    public void entering(String sourceClass, String sourceMethod, Object param1) {
	    delegate.entering(sourceClass, sourceMethod, param1);
    }

	@Override
    public void entering(String sourceClass, String sourceMethod, Object[] params) {
	    delegate.entering(sourceClass, sourceMethod, params);
    }

	@Override
    public void entering(String sourceClass, String sourceMethod) {
	    delegate.entering(sourceClass, sourceMethod);
    }

	@Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
	    delegate.exiting(sourceClass, sourceMethod, result);
    }

	@Override
    public void exiting(String sourceClass, String sourceMethod) {
	    delegate.exiting(sourceClass, sourceMethod);
    }

	@Override
    public void fine(String msg) {
	    delegate.fine(msg);
    }

	@Override
    public void finer(String msg) {
	    delegate.finer(msg);
    }

	@Override
    public void finest(String msg) {
	    delegate.finest(msg);
    }

	@Override
    public Filter getFilter() {
	    return delegate.getFilter();
    }

	@Override
    public synchronized Handler[] getHandlers() {
	    return delegate.getHandlers();
    }

	@Override
    public Level getLevel() {
	    return delegate.getLevel();
    }

	@Override
    public String getName() {
	    return delegate.getName();
    }

	@Override
    public Logger getParent() {
	    return delegate.getParent();
    }

	@Override
    public ResourceBundle getResourceBundle() {
	    return delegate.getResourceBundle();
    }

	@Override
    public String getResourceBundleName() {
	    return delegate.getResourceBundleName();
    }

	@Override
    public synchronized boolean getUseParentHandlers() {
	    return delegate.getUseParentHandlers();
    }

	@Override
    public void info(String msg) {
	    delegate.info(msg);
    }

	@Override
    public boolean isLoggable(Level level) {
	    return delegate.isLoggable(level);
    }

	@Override
    public void log(Level level, String msg, Object param1) {
	    delegate.log(level, msg, param1);
    }

	@Override
    public void log(Level level, String msg, Object[] params) {
	    delegate.log(level, msg, params);
    }

	@Override
    public void log(Level level, String msg, Throwable thrown) {
	    delegate.log(level, msg, thrown);
    }

	@Override
    public void log(Level level, String msg) {
	    delegate.log(level, msg);
    }

	@Override
    public void log(LogRecord record) {
	    delegate.log(record);
    }

	@Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
	    delegate.logp(level, sourceClass, sourceMethod, msg, param1);
    }

	@Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
	    delegate.logp(level, sourceClass, sourceMethod, msg, params);
    }

	@Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
	    delegate.logp(level, sourceClass, sourceMethod, msg, thrown);
    }

	@Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
	    delegate.logp(level, sourceClass, sourceMethod, msg);
    }

	@Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {
	    delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
    }

	@Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {
	    delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
    }

	@Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
	    delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
    }

	@Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
	    delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg);
    }

	@Override
    public synchronized void removeHandler(Handler handler) throws SecurityException {
	    delegate.removeHandler(handler);
    }

	@Override
    public void setFilter(Filter newFilter) throws SecurityException {
	    delegate.setFilter(newFilter);
    }

	@Override
    public void setLevel(Level newLevel) throws SecurityException {
	    delegate.setLevel(newLevel);
    }

	@Override
    public void setParent(Logger parent) {
	    delegate.setParent(parent);
    }

	@Override
    public synchronized void setUseParentHandlers(boolean useParentHandlers) {
	    delegate.setUseParentHandlers(useParentHandlers);
    }

	@Override
    public void severe(String msg) {
	    delegate.severe(msg);
    }

	@Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
	    delegate.throwing(sourceClass, sourceMethod, thrown);
    }

	@Override
    public void warning(String msg) {
	    delegate.warning(msg);
    }
}
