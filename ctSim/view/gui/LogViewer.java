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

/** LOG-Fenster */
public class LogViewer extends JPanel {
	/** UID */
	private static final long serialVersionUID = 2371285729455694008L;
	/** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.LogViewer");

	/** Log-Text */
	private final Document logContent;

	/** Buttons */
	class Button extends JButton {
		/** UID */
		private static final long serialVersionUID = 6172889032677505851L;

		/**
		 * Button-Klasse
		 *
		 * @param label			Name
		 * @param toolTipText	Tooltip
		 * @param icon			Icon
		 * @param onClick		onClick-Handler
		 */
		public Button(String label, String toolTipText, Icon icon,
				final Runnable onClick) {
			super(label);
			setToolTipText(toolTipText);
			setIcon(icon);
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onClick.run();
				}
			});
		}
	}

	/** Save-Handler */
	private final Runnable onSaveLog = new Runnable() {
		@Override
		public void run() {
			JFileChooser fc = new JFileChooser();
			int userChoice = fc.showSaveDialog(LogViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION) {
				// Benutzer hat abgebrochen
				return;
			}
			try {
				File f = fc.getSelectedFile();
				OutputStreamWriter out = new OutputStreamWriter(new BackslashNConverterStream(new FileOutputStream(f)), "UTF-8");
				out.write(logContent.getText(0, logContent.getLength()));
				out.flush();
				lg.info("Log-Ausgabe in Datei " + f.getAbsolutePath() + " geschrieben (" + f.length() + " Byte)");
				out.close();
			} catch (IOException e) {
				lg.warn(e, "E/A-Problem beim Schreiben der Log-Daten; " + "ignoriere");
			} catch (BadLocationException e) {
				// "Kann nicht passieren"
				throw new AssertionError(e);
			}
		}
	};

	/** Clear-Handler */
	private final Runnable onClearLog = new Runnable() {
		@Override
		public void run() {
			try {
				logContent.remove(0, logContent.getLength());
			} catch (BadLocationException e) {
				// "Kann nicht passieren"
				throw new AssertionError(e);
			}
		}
	};

	/**
	 * Log-Viewer
	 *
	 * @param log	Log-Kompomente
	 */
	public LogViewer(final Log log) {
		setLayout(new BorderLayout());
		logContent = log.getExternalModel();
		final JTextArea t = new JTextArea(logContent);
		t.setColumns(40);
		t.setRows(40);
		t.setEditable(false);
		Misc.setCaretPolicy(t, DefaultCaret.NEVER_UPDATE);

		JButton save = new Button("Speichern ...",
				"Inhalt des Logfensters in eine Textdatei speichern",
				Config.getIcon("Save16"), onSaveLog);
		JButton clear = new Button("Leeren",
				"Logausgaben löschen",
				Config.getIcon("schließen"), onClearLog);

		equalizeHeight(save, clear);

		// Rechtsbündig (trailing)
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
			private void scrollToEnd() {
				if (cb.isSelected()) {
					t.setCaretPosition(log.getExternalModel().getLength());
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// No-op
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				scrollToEnd();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				scrollToEnd();
			}
		});

		// Gesamtgröße setzen
		JScrollPane s = new JScrollPane(t);
		int w = getInsets().left + s.getInsets().left + s.getPreferredSize().width +
				s.getInsets().right + getInsets().right + 20;
		int h = getInsets().top + s.getInsets().top + s.getPreferredSize().height +
				s.getInsets().bottom + getInsets().bottom +	toolbar.getPreferredSize().height;
		setPreferredSize(new Dimension(w, h));

		// ausliefern
		add(toolbar, BorderLayout.NORTH);
		add(s, BorderLayout.CENTER);
	}

	/**
	 * Gleicht die Höhe von zwei Komponenten an
	 *
	 * @param c1
	 * @param c2
	 */
	private static void equalizeHeight(JComponent c1, JComponent c2) {
		int prefHeight = Math.max(
				c1.getPreferredSize().height,
				c2.getPreferredSize().height);
		setPrefHeight(c1, prefHeight);
		setPrefHeight(c2, prefHeight);
	}

	/**
	 * Setzt eine Komponente auf eine gewünschte Höhe
	 *
	 * @param c
	 * @param preferredHeight
	 */
	private static void setPrefHeight(JComponent c, int preferredHeight) {
		c.setPreferredSize(new Dimension(
				c.getPreferredSize().width, preferredHeight));
	}
}
