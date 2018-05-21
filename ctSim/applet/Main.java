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

package ctSim.applet;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import ctSim.Init;
import ctSim.TcpConnection;
import ctSim.controller.BotReceiver;
import ctSim.controller.Config;
import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.model.bots.Bot;
import ctSim.util.FmtLogger;
import ctSim.util.IconProvider;
import ctSim.view.gui.BotViewer;

/** Main-Klasse für das c't-Bot-Applet */
public class Main extends JApplet implements BotReceiver {
	/** UID */
	private static final long serialVersionUID = - 2381362461560258968L;

	/**
	 * Nur für Entwicklung: Normalerweise verbindet sich das Applet mit dem Host, von dem es
	 * heruntergeladen wurde. Falls es keinen Host gibt, weil man es lokal im Browser aufgerufen hat
	 * (file:///home/dings/...), dann wird ersatzweise diese Adresse genommen, um testen zu können.
	 */
	private static final String fallbackAddress = "192.168.1.22";

	/** Logger */
	final Logger mainLogger = Logger.getAnonymousLogger();

	/** Statusanzeige */
	private final JLabel status = new JLabel();

	/** Bot-Referenz */
	private Bot bot = null;

	/** Controller-Referenz */
	private Controller controller = null;


	/** Initialiserung des Applets */
	@Override
	public void init() {
		initLogging();
		Init.setLookAndFeel();
		initIcons();

		status.setAlignmentX(CENTER_ALIGNMENT);
		status.setAlignmentY(CENTER_ALIGNMENT);
		status.setFont(status.getFont().deriveFont(Font.PLAIN));

		JPanel p = new JPanel();
		p.add(status);
		getContentPane().add(p);

		// Controller wird für Bot-ID-Vergabe benötigt!
		controller = new DefaultController();
	}

	/** Logging initialisieren */
	private void initLogging() {
		FmtLogger.setFactory(new FmtLogger.Factory() {
			@Override
			public Logger createLogger(String name) {
				return mainLogger;
			}
		});

		Level level;
		try {
			level = Level.parse(getParameter("logLevel"));
		} catch (Exception e) {
			level = Level.INFO;
		}

		mainLogger.setLevel(level);
		Handler h = new Handler() {
			@Override
			public void close() { /* No-op */ }
			@Override
			public void flush() { /* No-op */ }
			@Override
			public void publish(LogRecord record) {
				Level lvl = record.getLevel();
				status.setIcon(UIManager.getLookAndFeel().getDefaults().getIcon(
					lvl == Level.WARNING || lvl == Level.SEVERE
					? "OptionPane.warningIcon"
					: "OptionPane.informationIcon"));
				status.setText(record.getMessage());
				status.setToolTipText(record.getMessage());
			}
		};
		mainLogger.addHandler(h);
	}

	/** Icons initialisieren */
	private void initIcons() {
		Config.setIconProvider(new IconProvider() {
			public Icon get(String key) {
				// Icon aus jar-Datei laden; Annahme: jar enthält Icon in seinem Root-Verzeichnis
				URL u = getClass().getClassLoader().getResource(key + ".gif");	// //$$ images-unterverz
				// NullPointerException vermeiden
				if (u == null)
					return new ImageIcon();	// leeres Icon
				else
					return new ImageIcon(u);
			}
		});
	}

	/** Startet die TCP-Connection */
	@Override
	public void start() {
		int port;
		try {
			port = Integer.parseInt(getParameter("portToConnectTo"));
		} catch (NumberFormatException e) {
			port = 10002;
		}
		String host = getCodeBase().getHost();
		TcpConnection.connectTo(host.equals("") ? fallbackAddress : host,
			port, this);
	}

	/**
	 * Fügt einen neuen (bereits erstellten) Bot in das Fenster ein
	 * 
	 * @param b	Referenz auf den neuen Bot
	 */
	public void onBotAppeared(final Bot b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String title = getParameter("windowTitle");
				if (title == null || title.trim().length() == 0)
					title = "c't-Bot";
				final JFrame f = new JFrame(title);
				BotViewer bv = new BotViewer(b);
				f.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						bot.dispose();
						mainLogger.info("Bot-Fenster geschlossen");
						f.dispose();
					}
				});
				f.add(bv);
				f.setSize(
					bv.getInsets().left +
					bv.getPreferredSize().width +
					bv.getInsets().right +
					bv.getVerticalScrollBar().getPreferredSize().width,

					bv.getInsets().top +
					bv.getPreferredSize().height +
					bv.getInsets().bottom +
					bv.getHorizontalScrollBar().getPreferredSize().height);
				f.setLocationRelativeTo(null);
				f.setVisible(true);
			}
		});
		bot = b;
		/** Der Bot braucht einen Controller! */
		try {
			bot.setController(controller);
		} catch (ProtocolException e) {
			// "kann nicht passieren"
			destroy();
		}
	}

	/** Beendet das c't-Bot-Applet */
	@Override
	public void destroy() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (bot != null)
					bot.dispose();
				mainLogger.info("c't-Bot-Applet wird beendet");
			}
		} );
	}
}
