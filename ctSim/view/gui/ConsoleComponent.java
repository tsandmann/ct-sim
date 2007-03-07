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
package ctSim.view.gui;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Eine Textkonsole in der c't-Sim-GUI
 * 
 * @author Felix Beckwermert
 */
//$$ Umbenennen (irgendwasViewer)
//$$$ Sollte immer zur letzten Ausgabe scrollen
public class ConsoleComponent extends JScrollPane implements DebugWindow {
    private static final long serialVersionUID = 7743090247891316572L;
	private JTextArea textArea;
	
	public ConsoleComponent() {
		super(new JTextArea(3, 40), 
				VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_AS_NEEDED);
		textArea = (JTextArea)getViewport().getView();
		textArea.setEditable(false);
		textArea.setBorder(BorderFactory.createEtchedBorder());
		setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
	}
	
	public synchronized void print(String msg) {
		textArea.append(msg);
		textArea.setCaretPosition(textArea.getText().length());
	}

	public synchronized void println(String msg) {
		print(msg + "\n");
	}
	
	public Handler createLoggingHandler() {
		return new LoggingHandler();
	}
	
	private class LoggingHandler extends Handler {
		@SuppressWarnings("synthetic-access")
        public LoggingHandler() {
			setFormatter(new Formatter() {
				@Override
                public String format(LogRecord r) {
					String lvl = r.getLevel().getLocalizedName();
	                return String.format("[%1$tk:%1$tM:%tS] %s%s: %s\n", 
	                		r.getMillis(), 
	                		lvl.toUpperCase().charAt(0), 
	                		lvl.toLowerCase().substring(1), 
	                		r.getMessage());
                }});
		}

		@Override
		public synchronized void publish(LogRecord record) {
			if (record.getLevel().intValue() >= getLevel().intValue())
				print(getFormatter().format(record));
		}

		@Override
		public void close() throws SecurityException {
			// No-op
		}

		@Override
		public void flush() {
			// No-op
		}
	}
}
