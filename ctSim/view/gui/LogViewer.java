package ctSim.view.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import ctSim.controller.Config;
import ctSim.model.bots.components.Actuators.Log;
import ctSim.util.BackslashNConverterStream;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

//$$ doc
public class LogViewer extends JPanel {
	private static final long serialVersionUID = 2371285729455694008L;
	final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.LogViewer");

	private final Document logContent;

	class Button extends JButton {
		private static final long serialVersionUID = 6172889032677505851L;

		public Button(String label, String toolTipText, Icon icon,
		final Runnable onClick) {
			super(label);
			setToolTipText(toolTipText);
			setIcon(icon);
			addActionListener(new ActionListener() {
				public void actionPerformed(
				@SuppressWarnings("unused") ActionEvent e) {
					onClick.run();
				}
			});
		}
	}

	private final Runnable onSaveLog = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			JFileChooser fc = new JFileChooser();
			int userChoice = fc.showSaveDialog(LogViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION)
				// Benutzer hat abgebrochen
				return;
			try {
				File f = fc.getSelectedFile();
				OutputStreamWriter out = new OutputStreamWriter(
					new BackslashNConverterStream(new FileOutputStream(f)),
					"UTF-8");
				out.write(logContent.getText(0, logContent.getLength()));
				lg.info("Log-Ausgabe in Datei "+f.getAbsolutePath()+
					" geschrieben ("+f.length()+" Byte)");
			} catch (IOException e) {
				lg.warn(e, "E/A-Problem beim Schreiben der Log-Daten; " +
						"ignoriere");
			} catch (BadLocationException e) {
				// Kann nicht passieren
				throw new AssertionError(e);
			}
		}
	};

	private final Runnable onClearLog = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			try {
				logContent.remove(0, logContent.getLength());
			} catch (BadLocationException e) {
				// Kann nicht passieren
				throw new AssertionError(e);
			}
		}
	};

	public LogViewer(Log log) {
		setLayout(new BorderLayout());
		logContent = log.getExternalModel();
		final JTextArea t = new JTextArea(logContent);
		t.setEditable(false);
		Misc.setCaretPolicy(t, DefaultCaret.NEVER_UPDATE);

		JButton save = new Button("Speichern ...",
			"Inhalt des Logfensters in eine Textdatei speichern",
			Config.getIcon("Save16"), onSaveLog);
		JButton clear = new Button("Leeren",
			"Logausgaben l\u00F6schen",
			Config.getIcon("schliessen"), onClearLog);

		equalizeHeight(save, clear);

		// Rechtsbuendig (trailing)
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.TRAILING));

		toolbar.add(save);
		toolbar.add(clear);

		// Checkbox bauen
		final JCheckBox cb = new JCheckBox("Auto-Scrolling");
		cb.setToolTipText("Immer zur neuesten Logausgabe scrollen");
		cb.setSelected(true);
		toolbar.add(cb);

		// Auto-Scrolling
		log.getExternalModel().addDocumentListener(new DocumentListener() {
			private void scrollTo(DocumentEvent e) {
				if (cb.isSelected())
					t.setCaretPosition(e.getOffset() + e.getLength());
			}

			public void changedUpdate(DocumentEvent e) { scrollTo(e); }
			public void insertUpdate (DocumentEvent e) { scrollTo(e); }
			public void removeUpdate (DocumentEvent e) { scrollTo(e); }
		});

		add(toolbar, BorderLayout.NORTH);
		int w = toolbar.getPreferredSize().width;
		setPreferredSize(new Dimension(w, 2 * w));
		add(new JScrollPane(t), BorderLayout.CENTER);
	}

	private static void equalizeHeight(JComponent c1, JComponent c2) {
		int prefHeight = Math.max(
			c1.getPreferredSize().height,
			c2.getPreferredSize().height);
		setPrefHeight(c1, prefHeight);
		setPrefHeight(c2, prefHeight);
	}

	private static void setPrefHeight(JComponent c, int preferredHeight) {
		c.setPreferredSize(new Dimension(
			c.getPreferredSize().width, preferredHeight));
	}
}