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

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

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
	
	/** 
	 * @see ctSim.view.gui.DebugWindow#print(java.lang.String)
	 */
	public synchronized void print(String msg) {
		textArea.append(msg);
		textArea.setCaretPosition(textArea.getText().length());
	}

	/** 
	 * @see ctSim.view.gui.DebugWindow#println(java.lang.String)
	 */
	public synchronized void println(String msg) {
		print(msg + "\n");
	}
	
	public class LoggingHandler extends StreamHandler {
		@SuppressWarnings("synthetic-access")
        public LoggingHandler() {
			super(new ConsoleComponentOutputStream(), new Formatter() {
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

		// Workaround fuer das sehr merkwuerdige Verhalten von
		// LoggingHandler. Er scheint normalerweise irgendwie zu 
		// puffern (Meldungen kommen stark verspaetet durch)
		@Override
		public synchronized void publish(LogRecord record) {
			    super.publish(record);
			    flush();
		}
	}
	
	private class ConsoleComponentOutputStream extends OutputStream {
		@Override
		public void write(int b) {
			// 24 MSB ignorieren, siehe Doku OutputStream.write(int)
			b &= 0x000000ff;
			ConsoleComponent.this.print(new String(new byte[] { (byte)b }));
		}
	}
}
