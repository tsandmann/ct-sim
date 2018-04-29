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

import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Logger-Klasse */
public class FmtLogger extends Logger {
	/** Logger-Factory */
	public abstract static class Factory {
		/**
		 * Neuer Logger
		 * 
		 * @param name	Name
		 * @return		Logger
		 */
		public abstract Logger createLogger(String name);
	}

	/** Logger-Factory */
	private static Factory loggerFactory = new Factory() {
		@Override
		public Logger createLogger(String name) {
			return Logger.getLogger(name);
		}
	};

	/**
	 * Setzt Factory
	 * 
	 * @param f	Factory
	 */
	public static void setFactory(Factory f) {
		loggerFactory = f;
	}

	/** Delegate-Logger */
	private Logger delegate;

	/**
	 * Logger
	 * 
	 * @param name		Name
	 * @param delegate	Delegate
	 */
	protected FmtLogger(String name, Logger delegate) {
	    super(name, null);
	    this.delegate = delegate;
    }

	/**
	 * @param name	Name des Loggers
	 * @return Logger
	 */
	public static synchronized FmtLogger getLogger(String name) {
		return new FmtLogger(name, loggerFactory.createLogger(name));
	}

	/**
	 * Warnung
	 * 
	 * @param msg	Text
	 */
	public void warn(String msg) {
		delegate.warning(msg);
	}

	//////////////////////////////////////////////////////////////////////////
	// Kurzschreibweise mit Throwable

	/**
	 * Severe
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void severe(Throwable t, String msg) {
		delegate.log(Level.SEVERE, msg, t);
	}

	/**
	 * Warnung
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void warning(Throwable t, String msg) {
		delegate.log(Level.WARNING, msg, t);
	}

	/**
	 * Warnung
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void warn(Throwable t, String msg) {
		warning(t, msg);
	}

	/**
	 * Info
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void info(Throwable t, String msg) {
		delegate.log(Level.INFO, msg, t);
	}

	/**
	 * Config
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void config(Throwable t, String msg) {
		delegate.log(Level.CONFIG, msg, t);
	}

	/**
	 * Fine
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void fine(Throwable t, String msg) {
		delegate.log(Level.FINE, msg, t);
	}

	/**
	 * Finer
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void finer(Throwable t, String msg) {
		delegate.log(Level.FINER, msg, t);
	}

	/**
	 * Finest
	 * 
	 * @param t		Throwable
	 * @param msg	Text
	 */
	public void finest(Throwable t, String msg) {
		delegate.log(Level.FINEST, msg, t);
	}

	//////////////////////////////////////////////////////////////////////////
	// Kurzschreibweisen mit Format-Strings

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void severe(String formatString, Object... parameters) {
		delegate.log(Level.SEVERE, String.format(formatString, parameters));
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void warning(String formatString, Object... parameters) {
		delegate.log(Level.WARNING, String.format(formatString, parameters));
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void warn(String formatString, Object... parameters) {
		warning(formatString, parameters);
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void info(String formatString, Object... parameters) {
		delegate.log(Level.INFO, String.format(formatString, parameters));
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void config(String formatString, Object... parameters) {
		delegate.log(Level.CONFIG, String.format(formatString, parameters));
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void fine(String formatString, Object... parameters) {
		delegate.log(Level.FINE, String.format(formatString, parameters));
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void finer(String formatString, Object... parameters) {
		delegate.log(Level.FINER, String.format(formatString, parameters));
	}

	/**
	 * @param formatString
	 * @param parameters
	 */
	public void finest(String formatString, Object... parameters) {
		delegate.log(Level.FINEST, String.format(formatString, parameters));
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void severe(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.SEVERE, String.format(formatString, parameters), t);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void warning(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.WARNING, String.format(formatString, parameters), t);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void warn(Throwable t, String formatString, Object... parameters) {
		warning(t, formatString, parameters);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void info(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.INFO, String.format(formatString, parameters), t);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void config(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.CONFIG, String.format(formatString, parameters), t);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void fine(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.FINE, String.format(formatString, parameters), t);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void finer(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.FINER, String.format(formatString, parameters), t);
	}

	/**
	 * @param t
	 * @param formatString
	 * @param parameters
	 */
	public void finest(Throwable t, String formatString, Object... parameters) {
		delegate.log(Level.FINEST, String.format(formatString, parameters), t);
	}


	//////////////////////////////////////////////////////////////////////////
	// Albtraum: Delegate-Elemente

	/**
	 * @see java.util.logging.Logger#addHandler(java.util.logging.Handler)
	 */
	@Override
	public synchronized void addHandler(Handler handler) throws SecurityException {
	    delegate.addHandler(handler);
    }

	/**
	 * @see java.util.logging.Logger#config(java.lang.String)
	 */
	@Override
	public void config(String msg) {
	    delegate.config(msg);
    }

	/**
	 * @see java.util.logging.Logger#entering(java.lang.String, java.lang.String, java.lang.Object)
	 */
	@Override
	public void entering(String sourceClass, String sourceMethod, Object param1) {
	    delegate.entering(sourceClass, sourceMethod, param1);
    }

	/**
	 * @see java.util.logging.Logger#entering(java.lang.String, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void entering(String sourceClass, String sourceMethod, Object[] params) {
	    delegate.entering(sourceClass, sourceMethod, params);
    }

	/**
	 * @see java.util.logging.Logger#entering(java.lang.String, java.lang.String)
	 */
	@Override
	public void entering(String sourceClass, String sourceMethod) {
	    delegate.entering(sourceClass, sourceMethod);
    }

	/**
	 * @see java.util.logging.Logger#exiting(java.lang.String, java.lang.String, java.lang.Object)
	 */
	@Override
	public void exiting(String sourceClass, String sourceMethod, Object result) {
	    delegate.exiting(sourceClass, sourceMethod, result);
    }

	/**
	 * @see java.util.logging.Logger#exiting(java.lang.String, java.lang.String)
	 */
	@Override
	public void exiting(String sourceClass, String sourceMethod) {
	    delegate.exiting(sourceClass, sourceMethod);
    }

	/**
	 * @see java.util.logging.Logger#fine(java.lang.String)
	 */
	@Override
	public void fine(String msg) {
	    delegate.fine(msg);
    }

	/**
	 * @see java.util.logging.Logger#finer(java.lang.String)
	 */
	@Override
	public void finer(String msg) {
	    delegate.finer(msg);
    }

	/**
	 * @see java.util.logging.Logger#finest(java.lang.String)
	 */
	@Override
	public void finest(String msg) {
	    delegate.finest(msg);
    }

	/**
	 * @see java.util.logging.Logger#getFilter()
	 */
	@Override
	public Filter getFilter() {
	    return delegate.getFilter();
    }

	/**
	 * @see java.util.logging.Logger#getHandlers()
	 */
	@Override
	public synchronized Handler[] getHandlers() {
	    return delegate.getHandlers();
    }

	/**
	 * @see java.util.logging.Logger#getLevel()
	 */
	@Override
	public Level getLevel() {
	    return delegate.getLevel();
    }

	/**
	 * @see java.util.logging.Logger#getName()
	 */
	@Override
	public String getName() {
	    return delegate.getName();
    }

	/**
	 * @see java.util.logging.Logger#getParent()
	 */
	@Override
	public Logger getParent() {
	    return delegate.getParent();
    }

	/**
	 * @see java.util.logging.Logger#getResourceBundle()
	 */
	@Override
	public ResourceBundle getResourceBundle() {
	    return delegate.getResourceBundle();
    }

	/**
	 * @see java.util.logging.Logger#getResourceBundleName()
	 */
	@Override
	public String getResourceBundleName() {
	    return delegate.getResourceBundleName();
    }

	/**
	 * @see java.util.logging.Logger#getUseParentHandlers()
	 */
	@Override
	public synchronized boolean getUseParentHandlers() {
	    return delegate.getUseParentHandlers();
    }

	/**
	 * @see java.util.logging.Logger#info(java.lang.String)
	 */
	@Override
	public void info(String msg) {
	    delegate.info(msg);
    }

	/**
	 * @see java.util.logging.Logger#isLoggable(java.util.logging.Level)
	 */
	@Override
	public boolean isLoggable(Level level) {
	    return delegate.isLoggable(level);
    }

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level, java.lang.String, java.lang.Object)
	 */
	@Override
	public void log(Level level, String msg, Object param1) {
	    delegate.log(level, msg, param1);
    }

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(Level level, String msg, Object[] params) {
	    delegate.log(level, msg, params);
    }

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void log(Level level, String msg, Throwable thrown) {
	    delegate.log(level, msg, thrown);
    }

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.Level, java.lang.String)
	 */
	@Override
	public void log(Level level, String msg) {
	    delegate.log(level, msg);
    }

	/**
	 * @see java.util.logging.Logger#log(java.util.logging.LogRecord)
	 */
	@Override
	public void log(LogRecord record) {
	    delegate.log(record);
    }

	/**
	 * @see java.util.logging.Logger#logp(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
	 */
	@Override
	public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
	    delegate.logp(level, sourceClass, sourceMethod, msg, param1);
    }

	/**
	 * @see java.util.logging.Logger#logp(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
	    delegate.logp(level, sourceClass, sourceMethod, msg, params);
    }

	/**
	 * @see java.util.logging.Logger#logp(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
	    delegate.logp(level, sourceClass, sourceMethod, msg, thrown);
    }

	/**
	 * @see java.util.logging.Logger#logp(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
	    delegate.logp(level, sourceClass, sourceMethod, msg);
    }

//	/**
//	 * @see java.util.logging.Logger#logrb(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
//	 */
//	@Override
//	public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {
//		delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
//	}
//
//	/**
//	 * @see java.util.logging.Logger#logrb(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
//	 */
//	@Override
//	public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {
//		delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
//	}
//
//	/**
//	 * @see java.util.logging.Logger#logrb(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable)
//	 */
//	@Override
//	public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
//		delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
//	}
//
//	/**
//	 * @see java.util.logging.Logger#logrb(java.util.logging.Level, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
//	 */
//	@Override
//	public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
//		delegate.logrb(level, sourceClass, sourceMethod, bundleName, msg);
//	}

	/**
	 * @see java.util.logging.Logger#removeHandler(java.util.logging.Handler)
	 */
	@Override
	public synchronized void removeHandler(Handler handler) throws SecurityException {
	    delegate.removeHandler(handler);
    }

	/**
	 * @see java.util.logging.Logger#setFilter(java.util.logging.Filter)
	 */
	@Override
	public void setFilter(Filter newFilter) throws SecurityException {
	    delegate.setFilter(newFilter);
    }

	/**
	 * @see java.util.logging.Logger#setLevel(java.util.logging.Level)
	 */
	@Override
	public void setLevel(Level newLevel) throws SecurityException {
	    delegate.setLevel(newLevel);
    }

	/**
	 * @see java.util.logging.Logger#setParent(java.util.logging.Logger)
	 */
	@Override
	public void setParent(Logger parent) {
	    delegate.setParent(parent);
    }

	/**
	 * @see java.util.logging.Logger#setUseParentHandlers(boolean)
	 */
	@Override
	public synchronized void setUseParentHandlers(boolean useParentHandlers) {
	    delegate.setUseParentHandlers(useParentHandlers);
    }

	/**
	 * @see java.util.logging.Logger#severe(java.lang.String)
	 */
	@Override
	public void severe(String msg) {
	    delegate.severe(msg);
    }

	/**
	 * @see java.util.logging.Logger#throwing(java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
	    delegate.throwing(sourceClass, sourceMethod, thrown);
    }

	/**
	 * @see java.util.logging.Logger#warning(java.lang.String)
	 */
	@Override
	public void warning(String msg) {
	    delegate.warning(msg);
    }
}