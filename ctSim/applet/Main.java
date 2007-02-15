package ctSim.applet;

import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import ctSim.TcpConnection;
import ctSim.controller.BotReceiver;
import ctSim.controller.Config;
import ctSim.model.bots.Bot;
import ctSim.util.FmtLogger;
import ctSim.util.IconProvider;
import ctSim.view.gui.BotViewer;

//$$ doc
public class Main extends JApplet implements BotReceiver {
	private static final long serialVersionUID = - 2381362461560258968L;

	/**
	 * Nur f&uuml;r Entwicklung: Normalerweise verbindet sich das Applet mit dem
	 * Host, von dem es heruntergeladen wurde. Falls es keinen Host gibt, weil
	 * man es lokal im Browser aufgerufen hat (file:///home/dings/...), dann
	 * wird ersatzweise diese Adresse genommen, um testen zu k&ouml;nnen.
	 */
	private static final String fallbackAddress = "10.10.22.58";

	final Logger mainLogger = Logger.getAnonymousLogger();

	private final JLabel status = new JLabel();

	private Bot bot = null;

	@Override
	public void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// Ignorieren; dann halt den normalen Look and Feel
		}

		initLogging();
		initIcons();

		status.setAlignmentX(CENTER_ALIGNMENT);
		status.setAlignmentY(CENTER_ALIGNMENT);
		status.setFont(status.getFont().deriveFont(Font.PLAIN));

		JPanel p = new JPanel();
		p.add(status);
		getContentPane().add(p);
	}

	private void initLogging() {
		FmtLogger.setFactory(new FmtLogger.Factory() {
			@Override
			public Logger createLogger(
			@SuppressWarnings("unused") String name) {
				return mainLogger;
			}
		});

		mainLogger.setLevel(Level.INFO);
		Handler h = new Handler() {
			@Override public void close() { /* No-op */ }
			@Override public void flush() { /* No-op */ }

			@SuppressWarnings("synthetic-access")
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

	private void initIcons() {
		Config.setIconProvider(new IconProvider() {
			public Icon get(String key) {
				// Icon aus jar-Datei laden; Annahme: jar enthaelt Icon in
				// seinem Root-Verzeichnis
				return new ImageIcon(getClass().getClassLoader().getResource(
					key+".gif"));
			}
		});
	}

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

	public void onBotAppeared(final Bot b) {
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				String title = getParameter("windowTitle");
				if (title == null || title.trim().length() == 0)
					title = "c't-Bot";
				JFrame f = new JFrame(title);
				BotViewer bv = new BotViewer(b);
				f.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(
					@SuppressWarnings("unused") WindowEvent e) {
						bot.dispose();
						mainLogger.info("Bot-Fenster geschlossen");
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
	}

	@Override
	public void destroy() {
		SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				if (bot != null)
					bot.dispose();
				mainLogger.info("c't-Bot-Applet wird beendet");
			}
		});
	}
}
