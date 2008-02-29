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

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;

import ctSim.controller.Config;
import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuators.Abl;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.RealCtBot;
import ctSim.util.FmtLogger;

/* under construction! */

/**
 * Stellt das Eingabefenster fuer ABL-Programme dar. Diese koennen geladen, gespeichert
 * (Textdatei) oder zum Bot gesendet und dort optional gestartet und auf Syntaxfehler
 * ueberprueft werden.
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class AblViewer extends JPanel {
	/** UID	*/
	private static final long serialVersionUID = 2371285729455694008L;
	
	/** Logger fuer das ABL-Fenster */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.LogViewer");

	/** ABL-Komponente */
	private final Abl ablCompnt;
	/** Bot zu dem der Viewer gehoert */
	private final Bot owner;
	/** Textfeld (Editor) */
	private JTextArea programText;
	/** Toolbar Zeile 1 */
	private JPanel toolbar1;
	/** Toolar Zeile 2 */
	private JPanel toolbar2;
	/** Checkbox fuer Autostart */
	private final JCheckBox autoStart;
	/** Checkbos fuer Syntax-Check */
	private final JCheckBox syntaxCheck;
	/** Statusanzeige fuer Syntax-Check */
	private RemoteCallViewer.PlannedBhvModel.Done checkLabel;

	/**
	 * Stellt unsere Buttons dar mit Icon und Tooltip
	 */
	class Button extends JButton {
		/** UID */
		private static final long serialVersionUID = 6172865032677505851L;

		/**
		 * Button-Klasse
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
				public void actionPerformed(
				@SuppressWarnings("unused") ActionEvent e) {
					onClick.run();
				}
			});
		}
	}
	
	/**
	 * @author ts
	 *
	 */
	class TextFilter extends FileFilter {
	    /**
	     * Get the extension of a file.
	     * @param f 
	     * @return Extension
	     */  
	    public String getExtension(File f) {
	        String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');

	        if (i > 0 &&  i < s.length() - 1) {
	            ext = s.substring(i+1).toLowerCase();
	        }
	        return ext;
	    }
		
	    /**
	     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	     */
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
	    public String getDescription() {
	        return "Textdateien";
	    }
	}

	/**
	 * Programm-Laden-Handler
	 */
	private final Runnable onLoadAbl = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter(new TextFilter());
			int userChoice = fc.showOpenDialog(AblViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION)
				// Benutzer hat abgebrochen
				return;
			try {
				File f = fc.getSelectedFile();
			    BufferedReader in = new BufferedReader(new FileReader(f));
			    String line;
			    String data = new String();
				while ((line = in.readLine()) != null) {
					data += line + "\n";
				}
				in.close();
				programText.setText(data);
				lg.info("ABL-Programm aus Datei "+f.getAbsolutePath()+
					" geladen ("+f.length()+" Byte)");
			} catch (IOException e) {
				lg.warn(e, "E/A-Problem beim Laden der Daten; " +
						"ignoriere");
			}
		}
	};	
	
	/**
	 * Programm-Speichern-Handler
	 */
	private final Runnable onSaveAbl = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter(new TextFilter());
			int userChoice = fc.showSaveDialog(AblViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION)
				// Benutzer hat abgebrochen
				return;
			try {
				File f = fc.getSelectedFile();
				OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f));
				out.write(programText.getText());
				out.flush();
				lg.info("ABL-Programm in Datei "+f.getAbsolutePath()+
					" geschrieben ("+f.length()+" Byte)");
			} catch (IOException e) {
				lg.warn(e, "E/A-Problem beim Schreiben der Daten; " +
						"ignoriere");
			}
		}
	};

	/**
	 * Liefert das aktuelle AblViewer-Objekt
	 * @return	this
	 */
	private AblViewer getViewer() {
		return this;
	}
	
	/**
	 * Programm-Senden-Handler
	 */
	private final Runnable onSendAbl = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			try {
				ablCompnt.sendAblData(programText.getText());
				lg.info(programText.getText().length() + " Bytes gesendet");
				if (syntaxCheck.isSelected()) {
					/* Syntaxcheck ab Zeile 1 starten */
					checkSyntax(1);
					lastCheckedLine = 1;
				} else {
					if (autoStart.isSelected()) {
						/* ABL-Programm sofort starten */
						if (owner instanceof CtBotSimTcp) {
							/* simulierter Bot */
							CtBotSimTcp simBot = (CtBotSimTcp) owner;
							simBot.startABL();
						} else if (owner instanceof RealCtBot) {
							/* echter Bot */
							RealCtBot realBot = (RealCtBot) owner;
							realBot.startABL();
						}
					}
				}
			} catch (IOException e) {
				// wenn hier was schief ging, hat das keine tragischen Folgen, also nur Warnung
				lg.warn("E/A Fehler beim Senden des ABL-Programms");
			}
		}
	};

	/**
	 * Schreibt ein kleines Beispiel-Programm ins ABL-Fenster.
	 */
	private void setExampleProgram() {
		programText.setText("// Hey Bot, I'm an ABL-script!\nfor(4)\n\tbot_goto_dist(150,1)\n\tbot_turn(90)\nendf()\n");
	}
	
	/**
	 * Beispiel-Programm-Handler. 
	 * Erzeugt ein einfaches ABL-Beispiel.
	 */
	private final Runnable onExmplAbl = new Runnable() {
		@SuppressWarnings("synthetic-access")
		public void run() {
			programText.setSelectedTextColor(Color.BLACK);
			if (programText.getText().length() == 0) {
				setExampleProgram();
			} else {
				if (JOptionPane.showConfirmDialog(null, "Es sind bereits Programmdaten im Fenster vorhanden. Sollen diese ueberschrieben werden?", 
					"Hey!", JOptionPane.YES_NO_OPTION) == 0) {
					setExampleProgram();
				}
			}
		}
	};
	
	/**
	 * Erzeugt das ABL-Fenster, in dem sich ABL-Programme laden, speichern, eingeben und versenden lassen.
	 * @param abl	ABL-Actuator, der vom Fenster verwendet werden soll.
	 * @param bot	Bot-Referenz
	 */
	public AblViewer(Abl abl, Bot bot) {
		ablCompnt = abl;
		owner = bot;
		setLayout(new BorderLayout());
		
		/* Editor bauen */
		programText = new JTextArea();
		programText.setColumns(30);
		programText.setRows(40);
		programText.setEditable(true);
		programText.setLineWrap(false);
		programText.setTabSize(2);

		/* Buttons bauen */
		JButton load = new Button("Laden  ",
			"ABL-Programm aus einer Textdatei laden",
			Config.getIcon("Open16"), onLoadAbl);
		JButton save = new Button("Speichern  ",
			"ABL-Programm in eine Textdatei speichern",
			Config.getIcon("Save16"), onSaveAbl);
		JButton exmpl = new Button("Beispiel  ",
				"Erzeugt ein kleines Beispiel-ABL-Programm",
				Config.getIcon("New16"), onExmplAbl);		
		JButton send = new Button("Senden  ",
			"Programm zum Bot senden", 
			Config.getIcon("Play16"), onSendAbl);

		/* Checkboxes bauen */
		autoStart = new JCheckBox("Start");
		autoStart.setToolTipText("ABL-Programm nach der \u00DCbertragung sofort starten");
		autoStart.setSelected(true);
		
		syntaxCheck = new JCheckBox("Check");
		syntaxCheck.setToolTipText("Syntax-Check des ABL-Programms nach der \u00DCbertragung");
		//syntaxCheck.setSelected((bot instanceof CtBotSimTcp));
		syntaxCheck.setSelected(false);
		
		RemoteCallViewer rcViwer = new RemoteCallViewer(null);
		RemoteCallViewer.PlannedBhvModel rcModel = rcViwer.new PlannedBhvModel();
		
		checkLabel = rcModel.new Done("-", "Kein Syntaxcheck bisher", Color.GRAY);
		
		/* Toolbars bauen */
		JPanel toolbars = new JPanel(new BorderLayout());
		toolbar1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
		toolbar2 = new JPanel(new FlowLayout(FlowLayout.CENTER));

		toolbar1.add(load);
		toolbar1.add(save);
		toolbar1.add(exmpl);
		toolbar2.add(send);
		toolbar2.add(autoStart);
		toolbar2.add(syntaxCheck);
		toolbar2.add(checkLabel);

		toolbars.add(toolbar1, BorderLayout.NORTH);
		toolbars.add(toolbar2, BorderLayout.SOUTH);
		
		/* Gesamtgroesse setzen */
		JScrollPane s = new JScrollPane(programText);
		int w = getInsets().left + s.getInsets().left +
			s.getPreferredSize().width +
			s.getInsets().right + getInsets().right + 20;
		int h = getInsets().top + s.getInsets().top +
			s.getPreferredSize().height + 
			s.getInsets().bottom + getInsets().bottom +
			toolbars.getPreferredSize().height;
		setPreferredSize(new Dimension(w, h));

		/* Ausliefern */
		add(toolbars, BorderLayout.NORTH);
		add(s, BorderLayout.CENTER);
	}
	
	/** zuletzt gepruefte Programmzeile */
	private int lastCheckedLine = 0;
	
	/**
	 * Zeigt das Ergebnis eines ABL-Syntax-Checks in der Toolbar an
	 * @param result	Das Ergebnnis, true fuer SUCCESS
	 */
	public void setSyntaxCheck(boolean result) {
		if (result == false || lastCheckedLine == programText.getLineCount()) {
			/* Fehler oder alle Zeilen ueberprueft */
			toolbar2.remove(checkLabel);
			RemoteCallViewer rcViwer = new RemoteCallViewer(null);
			RemoteCallViewer.PlannedBhvModel rcModel = rcViwer.new PlannedBhvModel();
			if (result) {
				/* OK setzen */
				checkLabel = rcModel.new Done(":-)", "Keine Syntaxfehler gefunden", Color.GREEN);
				lg.info("Syntaxcheck abgeschlossen - keine Fehler gefunden :-)");
			} else {
				/* Fehler setzen */
				checkLabel = rcModel.new Done(":-(", "Syntaxfehler gefunden in Zeile " + lastCheckedLine, Color.RED);
				lg.info("Syntaxcheck ergab Fehler in Zeile " + lastCheckedLine + ". Weitere Ueberpruefung abgebrochen.");
				/* Zeile mit Syntaxfehler markieren */
				programText.setSelectedTextColor(Color.RED);
				try {
					programText.select(programText.getLineStartOffset(lastCheckedLine-1), programText.getLineEndOffset(lastCheckedLine-1));
				} catch (BadLocationException e) {
					// kann nicht passieren, wenn wir nur vorhandene Zeilen checken
				}
			}
			/* Status anzeigen */
			toolbar2.add(checkLabel);
			validate();
			/* Programmstart, falls gewuenscht und keine Fehler gefunden */
			if (result == true && autoStart.isSelected()) {
				/* ABL-Programm sofort starten */
				if (owner instanceof CtBotSimTcp) {
					/* simulierter Bot */
					CtBotSimTcp simBot = (CtBotSimTcp) owner;
					simBot.startABL();
				} else if (owner instanceof RealCtBot) {
					/* echter Bot */
					RealCtBot realBot = (RealCtBot) owner;
					realBot.startABL();
				}
			}
		} else {
			/* naechste Zeile pruefen */
			lastCheckedLine++;
			checkSyntax(lastCheckedLine);
		}
	}
	
	/**
	 * Startet einen Syntaxcheck bis Zeile line per RemoteCall.
	 * @param line	Zeile, bis zu der das Programm geprueft werden soll
	 */
	private void checkSyntax(int line) {
		if (line == 1) {
			/*check for() / endf() */
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
	 * Zaehlt die Vorkommen eines SubStrings in einem String
	 * @param s Quell-String, der untersucht wird
	 * @param subString SubString, dessen Vorkommen gezaehlt wird
	 * @return Anzahl der Vorkommen
	 */
	private int countSubString(String s, String subString) {
		int count = 0;
		for (int i=0; i>=0;) {
			int nextSubString = s.indexOf(subString, i + 1);
			if (nextSubString >= 0) {
				count++;
//				lg.info("nextSubString=" + nextSubString);
				String foundSubString = s.substring(i + 1, nextSubString);
				
				int lastComment = foundSubString.lastIndexOf("//");
//				lg.info("lastComment=" + lastComment);
				
				int lastNewLine = foundSubString.lastIndexOf('\n');
//				lg.info("lastNewLine=" + lastNewLine);
				
				if (lastComment > lastNewLine) {
					/* Kommentar */
					count--;	// nicht mitzaehlen
//					lg.info("kommentar");
				}
			}
			i = nextSubString;
		}
		return count;
	}
}
