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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;

import ctSim.ConfigManager;
import ctSim.controller.Config;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuators.Program;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.FmtLogger;

/**
 * Stellt das Eingabefenster für ABL- oder Basic-Programme dar. Diese können geladen, gespeichert (Textdatei)
 * oder zum Bot gesendet und dort optional gestartet und auf Syntaxfehler überprüft werden.
 *
 * @author Timo Sandmann
 */
public class ProgramViewer extends JPanel implements ActionListener {
	/** UID	*/
	private static final long serialVersionUID = 2371285729455694008L;
	/** Logger für das Fenster */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.LogViewer");
	/** Programm-Komponente */
	private final Program programCompnt;
	/** Bot zu dem der Viewer gehört */
	private final Bot owner;
	/** Toolbar Zeile 1 */
	private final JPanel toolbar0;
	/** Toolbar Zeile 2 */
	private final JPanel toolbar1;
	/** Toolbar Zeile 3 */
	private final JPanel toolbar2;
	/** Button für ABL-Beispiel */
	private final JButton exmplABL;
	/** Textfeld für Dateiname */
	private final JTextField fileName;
	/** Textfeld (Editor) */
	private final JTextArea programText;
	/** Checkbox für Autostart */
	private final JCheckBox autoStart;
	/** Checkbos für Syntax-Check */
	private final JCheckBox syntaxCheckABL;
	/** Statusanzeige für ABL-Syntax-Check */
	private RemoteCallViewer.PlannedBhvModel.Done checkLabelABL;
	/** ausgewählter Typ. 0: Basic, 1: ABL */
	private int type = 0;
	/** Pfad für Laden- / Speichern-Dialog */
	private String path = ConfigManager.path2Os(Config.getValue("botdir")) + "/../bot-logic/basic";

	/** Stellt unsere Buttons dar, inklusive Icon und Tooltip */
	class Button extends JButton {
		/** UID */
		private static final long serialVersionUID = 6172865032677505851L;

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
				public void actionPerformed(ActionEvent e) {
					onClick.run();
				}
			});
		}
	}

	/** Filter für Textdateien */
	class TextFilter extends FileFilter {
		/**
		 * Die Dateinanmen-Erweiterung einer Datei auslesen
		 *
		 * @param f	File
		 * @return Extension
		 */
		public String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 &&  i < s.length() - 1) {
				ext = s.substring(i + 1).toLowerCase();
			}
			return ext;
		}

		/**
		 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(File f) {
			if (f.isDirectory()) {
				return true;
			}

			String extension = getExtension(f);
			if (extension != null) {
				if (extension.equals("txt")) {
					return true;
				} else {
					return false;
				}
			}

			return false;
		}

		/**
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Textdateien";
		}
	}

	/** Programm-Laden-Handler */
	private final Runnable onLoad = new Runnable() {
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			JFileChooser fc = new JFileChooser(path);
			fc.addChoosableFileFilter(new TextFilter());
			int userChoice = fc.showOpenDialog(ProgramViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION)
				// Benutzer hat abgebrochen
				return;
			try {
				File f = fc.getSelectedFile();
				path = f.getParent();
				String fname = fileName.getText().substring(0, fileName.getText().lastIndexOf('/') + 1);
				fileName.setText(fname + f.getName());
				BufferedReader in = new BufferedReader(new FileReader(f));
				String line;
				String data = new String();
				while ((line = in.readLine()) != null) {
					data += line + "\n";
				}
				in.close();
				programText.setText(data);
				lg.info("Programm aus Datei " + f.getAbsolutePath() + " geladen (" + f.length() + " Byte)");
			} catch (IOException e) {
				lg.warn(e, "E/A-Problem beim Laden der Daten; ignoriere");
			}
		}
	};

	/** Programm-Speichern-Handler */
	private final Runnable onSave = new Runnable() {
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			JFileChooser fc = new JFileChooser(path);
			fc.addChoosableFileFilter(new TextFilter());
			fc.setSelectedFile(new File(path + "/" + fileName.getText().substring(fileName.getText().
					lastIndexOf('/') + 1)));
			int userChoice = fc.showSaveDialog(ProgramViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION) {
				// Benutzer hat abgebrochen
				return;
			}
			try {
				File f = fc.getSelectedFile();
				path = f.getParent();
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f));
				out.write(programText.getText());
				out.flush();
				lg.info("Programm in Datei " + f.getAbsolutePath() + " geschrieben (" + f.length() + " Byte)");
				out.close();
			} catch (IOException e) {
				lg.warn(e, "E/A-Problem beim Schreiben der Daten; ignoriere");
			}
		}
	};

	/**
	 * Liefert das aktuelle ProgamViewer-Objekt
	 *
	 * @return this
	 */
	private ProgramViewer getViewer() {
		return this;
	}

	/** Programm-Senden-Handler */
	private final Runnable onSend = new Runnable() {
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			if (fileName.getText().length() == 0) {
				lg.warn("Dateiname fehlt, Abbruch");
				return;
			}
			if (fileName.getText().length() > 100) {
				lg.warn("Dateiname zu lang, Abbruch");
				return;
			}
			try {
				programCompnt.sendProgramData(fileName.getText(), programText.getText(), type, owner);
				lg.info(programText.getText().length() + 1 + " Bytes gesendet als Datei " + fileName.getText());
				if (syntaxCheckABL.isSelected()) {
					/* Syntaxcheck ab Zeile 1 starten */
					checkSyntax(1);
					lastCheckedLine = 1;
				} else {
					if (autoStart.isSelected()) {
						/* Programm sofort starten */
						programCompnt.startProgram(type);
					}
				}
			} catch (IOException e) {
				// wenn hier was schief ging, hat das keine tragischen Folgen, also nur eine Warnung ausgeben
				lg.warn("E/A Fehler beim Senden des Programms");
			}
		}
	};

	/** Programm-Abbrechen-Handler */
	private final Runnable onStop = new Runnable() {
		public void run() {
			programCompnt.stopProgram(type);
		}
	};

	/** Schreibt ein kleines ABL-Beispiel-Programm in den Editor. */
	private void setExampleProgram() {
		programText.setText("// Hey Bot, I'm an ABL-script!\nfor(4)\n\tbot_goto_dist(150,1)\n\tbot_turn(90)\nendf()\n");
	}

	/** Beispiel-Programm-Handler der ein einfaches ABL-Beispiel erzeugt */
	private final Runnable onExmplAbl = new Runnable() {
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			if (programText.getText().length() == 0) {
				setExampleProgram();
			} else {
				if (JOptionPane.showConfirmDialog(null, "Es sind bereits Programmdaten im Fenster vorhanden. " +
						"Sollen diese überschrieben werden?", "Hey!", JOptionPane.YES_NO_OPTION) == 0) {
					setExampleProgram();
				}
			}
		}
	};

	/**
	 * Erzeugt das Programm-Fenster, in dem sich Programme laden, speichern, eingeben und versenden lassen
	 *
	 * @param program	Program-Actuator, der vom Fenster verwendet werden soll.
	 * @param bot		Bot-Referenz
	 */
	public ProgramViewer(Program program, Bot bot) {
		programCompnt = program;
		owner = bot;
		setLayout(new BorderLayout());

		/* Editor bauen */
		programText = new JTextArea();
		programText.setColumns(40);
		programText.setRows(35);
		programText.setEditable(true);
		programText.setLineWrap(false);
		programText.setTabSize(2);

		/* Radio Buttons bauen */
		JRadioButton typeABL = new JRadioButton("ABL", false);
		typeABL.setActionCommand("ABL");
		typeABL.addActionListener(this);
		typeABL.setEnabled(bot.get_feature_abl_program());
		JRadioButton typeBasic = new JRadioButton("Basic", true);
		typeBasic.setActionCommand("Basic");
		typeBasic.addActionListener(this);
		typeBasic.setEnabled(bot.get_feature_basic_program());

		ButtonGroup group = new ButtonGroup();
	    group.add(typeABL);
	    group.add(typeBasic);

		/* Buttons bauen */
		JButton load = new Button("Laden  ", "Programm aus einer Textdatei laden. " +
				"Voreingestelltes Bot-Verzeichnis wie in Konfig-Datei angegeben.",
				Config.getIcon("Open16"), onLoad);
		JButton save = new Button("Speichern  ", "Programm in eine Textdatei speichern. " +
				"Voreingestelltes Bot-Verzeichnis wie in Konfig-Datei angegeben.",
				Config.getIcon("Save16"), onSave);
		exmplABL = new Button("ABL-Beispiel  ",	"Erzeugt ein kleines Beispiel-ABL-Programm",
				Config.getIcon("New16"), onExmplAbl);
		JButton stop = new Button("Stopp  ", "laufendes Programm abbrechen",
				Config.getIcon("Stop16"), onStop);
		JButton send = new Button("Senden  ", "Programm zum Bot senden",
				Config.getIcon("Play16"), onSend);

		/* Checkboxes bauen */
		autoStart = new JCheckBox("Start");
		autoStart.setToolTipText("Programm nach der Übertragung sofort starten");
		autoStart.setSelected(true);

		syntaxCheckABL = new JCheckBox("Check");
		syntaxCheckABL.setToolTipText("Syntax-Check des ABL-Programms nach der Übertragung");
//		syntaxCheck.setSelected((bot instanceof CtBotSimTcp));
		syntaxCheckABL.setSelected(false);

		RemoteCallViewer rcViwer = new RemoteCallViewer(null);
		RemoteCallViewer.PlannedBhvModel rcModel = rcViwer.new PlannedBhvModel();

		checkLabelABL = rcModel.new Done("-", "Kein Syntaxcheck bisher", Color.GRAY);

		/* Dateinamen Feld bauen */
		fileName = new JTextField();
		fileName.setEditable(true);
		fileName.setToolTipText("Dateiname (auf dem Bot-Dateisystem)");
		fileName.setPreferredSize(new Dimension(160, fileName.getPreferredSize().height));

		/* Toolbars bauen */
		JPanel toolbars = new JPanel(new BorderLayout());
		toolbar0 = new JPanel(new FlowLayout(FlowLayout.CENTER));
		toolbar1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
		toolbar2 = new JPanel(new FlowLayout(FlowLayout.CENTER));

		toolbar0.add(typeBasic);
		toolbar0.add(typeABL);
		toolbar1.add(load);
		toolbar1.add(save);
		toolbar1.add(exmplABL);
		toolbar1.add(stop);
		toolbar2.add(fileName);
		toolbar2.add(send);
		toolbar2.add(autoStart);
		toolbar2.add(syntaxCheckABL);
		toolbar2.add(checkLabelABL);

		toolbars.add(toolbar0, BorderLayout.NORTH);
		toolbars.add(toolbar1, BorderLayout.SOUTH);
		toolbars.add(toolbar2);

		/* Gesamtgröße setzen */
		JScrollPane s = new JScrollPane(programText);
		int edit_w = getInsets().left + s.getInsets().left + s.getPreferredSize().width + s.getInsets().right +
				getInsets().right + 20;	// scrollbar-width == 20
		int h = getInsets().top + s.getInsets().top + s.getPreferredSize().height + s.getInsets().bottom +
				getInsets().bottom +	toolbars.getPreferredSize().height;

		int toolbar_w = getInsets().left + toolbars.getInsets().left + toolbars.getPreferredSize().width +
				toolbars.getInsets().right + getInsets().right;

		setPreferredSize(new Dimension(Math.max(edit_w, toolbar_w), h));

		int min_w = getInsets().left + toolbars.getInsets().left + toolbar_w + toolbars.getInsets().right +
				getInsets().right;
		int min_h = getInsets().top + s.getInsets().top + s.getMinimumSize().height + s.getInsets().bottom +
				getInsets().bottom +	toolbars.getPreferredSize().height;

		setMinimumSize(new Dimension(min_w, min_h + 60));

		/* Ausliefern */
		add(toolbars, BorderLayout.NORTH);
		add(s, BorderLayout.CENTER);

		if (bot.get_feature_basic_program()) {
			typeBasic.doClick();	// Typ auf Basic setzen
		} else {
			typeABL.doClick();	// Typ auf ABL setzen
		}
	}

	/**
	 * Handler für Klick auf einen der Radio-Buttons
	 *
	 * @param e	Event
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals("Basic")) {
			type = 0;
			syntaxCheckABL.setEnabled(false);
			exmplABL.setEnabled(false);
			checkLabelABL.setText("-");
			checkLabelABL.setToolTipText("Nicht verfügbar für Basic-Programme");
			checkLabelABL.setBackground(Color.GRAY);
			fileName.setText("bas1.txt");
			path = path.replace("/abl", "/basic");
		} else if (e.getActionCommand().equals("ABL")) {
			type = 1;
			syntaxCheckABL.setEnabled(true);
			exmplABL.setEnabled(true);
			checkLabelABL.setText("-");
			checkLabelABL.setToolTipText("Kein Syntaxcheck bisher");
			checkLabelABL.setBackground(Color.GRAY);
			fileName.setText("prog1.txt");
			path = path.replace("/basic", "/abl");
		}
	}

	/** zuletzt geprüfte Programmzeile */
	private int lastCheckedLine = 0;

	/**
	 * Zeigt das Ergebnis eines ABL-Syntax-Checks in der Toolbar an
	 *
	 * @param result	das Ergebnnis, true für SUCCESS
	 */
	public void setSyntaxCheck(boolean result) {
		if (result == false || lastCheckedLine == programText.getLineCount()) {
			/* Fehler oder alle Zeilen überprüft */
			toolbar2.remove(checkLabelABL);
			RemoteCallViewer rcViwer = new RemoteCallViewer(null);
			RemoteCallViewer.PlannedBhvModel rcModel = rcViwer.new PlannedBhvModel();
			if (result) {
				/* OK setzen */
				checkLabelABL = rcModel.new Done(":-)", "Keine Syntaxfehler gefunden", Color.GREEN);
				lg.info("Syntaxcheck abgeschlossen - keine Fehler gefunden :-)");
			} else {
				/* Fehler setzen */
				checkLabelABL = rcModel.new Done(":-(", "Syntaxfehler gefunden in Zeile " + lastCheckedLine, Color.RED);
				lg.info("Syntaxcheck ergab Fehler in Zeile " + lastCheckedLine + ". Weitere Überprüfung abgebrochen.");
				/* Zeile mit Syntaxfehler markieren */
				programText.setSelectedTextColor(Color.RED);
				try {
					programText.select(programText.getLineStartOffset(lastCheckedLine - 1),
						programText.getLineEndOffset(lastCheckedLine - 1));
				} catch (BadLocationException e) {
					// kann nicht passieren, wenn wir nur vorhandene Zeilen checken
				}
			}
			/* Status anzeigen */
			toolbar2.add(checkLabelABL);
			validate();
			/* Programmstart, falls gewünscht und keine Fehler gefunden */
			if (result == true && autoStart.isSelected()) {
				/* ABL-Programm sofort starten */
				programCompnt.startProgram(type);
			}
		} else {
			/* nächste Zeile prüfen */
			lastCheckedLine++;
			checkSyntax(lastCheckedLine);
		}
	}

	/**
	 * Startet einen Syntaxcheck bis Zeile line per RemoteCall
	 *
	 * @param line	Zeile, bis zu der das Programm geprüft werden soll
	 */
	private void checkSyntax(int line) {
		if (line == 1) {
			/* check for() / endf() */
			int fors = countSubString(programText.getText(), "for(");
//			lg.info("fors=" + fors);
			int endfs = countSubString(programText.getText(), "endf()");
//			lg.info("endfs=" + endfs);
			if (fors != endfs) {
				lg.warn("ungleiche Anzahl von for(X) und endf()!");
				lastCheckedLine = 0;
				setSyntaxCheck(false);
				return;
			}

			/* check if() / else() / fi() */
			int ifs = countSubString(programText.getText(), "if(");
			int elses = countSubString(programText.getText(), "else()");
			int fis = countSubString(programText.getText(), "fi()");
			if (ifs != fis || elses > ifs) {
				lg.warn("ungleiche Anzahl von if(X) und fi() oder mehr else() als if(X)!");
				lastCheckedLine = 0;
				setSyntaxCheck(false);
				return;
			}
		}
		lg.fine("ABL-Syntaxcheck bis Zeile " + line);
		if (owner instanceof CtBotSimTcp) {
			/* simulierter Bot */
			CtBotSimTcp simBot = (CtBotSimTcp) owner;
			simBot.startRemoteCall("bot_abl_check", line, getViewer());
		} else if (owner instanceof RealCtBot) {
			/* echter Bot */
			RealCtBot realBot = (RealCtBot) owner;
			realBot.startRemoteCall("bot_abl_check", line, getViewer());
		}
	}

	/**
	 * Zählt die Vorkommen eines SubStrings in einem String
	 *
	 * @param s			Quell-String, der untersucht wird
	 * @param subString	SubString, dessen Vorkommen gezählt wird
	 * @return Anzahl der Vorkommen
	 */
	private int countSubString(String s, String subString) {
		int count = 0;
		for (int i = 0; i >= 0;) {
			int nextSubString = s.indexOf(subString, i + 1);
			if (nextSubString >= 0) {
				++count;
//				lg.info("nextSubString=" + nextSubString);
				String foundSubString = s.substring(i + 1, nextSubString);

				int lastComment = foundSubString.lastIndexOf("//");
//				lg.info("lastComment=" + lastComment);

				int lastNewLine = foundSubString.lastIndexOf('\n');
//				lg.info("lastNewLine=" + lastNewLine);

				if (lastComment > lastNewLine) {
					/* Kommentar */
					--count;	// nicht mitzählen
//					lg.info("kommentar");
				}
			}
			i = nextSubString;
		}
		return count;
	}
}
