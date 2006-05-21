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

package ctSim.View;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

/**
 * Fenster fuer die Anzeige von Loggings.
 * 
 * @author Andreas Merkle (mail@blue-andi.de)
 *
 */
public class LogFrame extends javax.swing.JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	
	/** Button zum Loeschen des Ausgabefensters */
	private JButton jButtonClear = null;
	/** Panel fuer die Buttons */
	private JPanel jPanelButtons = null;
	/** Ausgabefenster */
	private JTextArea jTextAreaLog = null;
	/** Scrollbalken zum Ausgabefenster */
	private JScrollPane	jScrollPane = null;

	/**
	 * Erzeugt Log-Fenster.
	 */
	LogFrame() {
		setTitle("Log-Window");
		
		/* Buttons erzeugen */
		jButtonClear = new JButton("Clear");
		jButtonClear.setActionCommand("clear");
		jButtonClear.addActionListener(this);
		
		/* Panel fuer Buttons erzeugen und Buttons hinzuefuegen */
		jPanelButtons = new JPanel();
		jPanelButtons.setLayout(new BoxLayout(jPanelButtons, BoxLayout.X_AXIS));
		jPanelButtons.add(jButtonClear);
		
		/* Textbereich fuer die Logausgaben erzeugen */
		jTextAreaLog = new JTextArea(20, 60);
		jTextAreaLog.setEditable(false);
		jTextAreaLog.setLineWrap(false);
		jTextAreaLog.setFont(new Font("Courier New", Font.PLAIN, 12));
		/* Mit Scrollbalken */
		jScrollPane = new JScrollPane(	jTextAreaLog,
		              					ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
		              					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		/* Layout */
		getContentPane().setLayout(new BorderLayout(5,5));
		getContentPane().add(BorderLayout.NORTH, jPanelButtons);
		getContentPane().add(BorderLayout.CENTER, jScrollPane);
		
		/* Beim Schliessen des Fensters auch das Programm beenden */
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		
		/* Fenster an Position schieben */
		setLocation(200, 200);
		
		/* Gesamten Frame auf optimale Groessee packen */
		pack();
		
		super.repaint();
		this.repaint();
	}
	
	/**
	 * Diese Methode wertet Events von den Buttons aus.
	 */
	public void actionPerformed(ActionEvent evt) {
		
		if (evt.getActionCommand().equals("clear")) {
			synchronized (jTextAreaLog) {
				/* Logging loeschen */
				jTextAreaLog.setText("");
			}
		}
	}
	
	/**
	 * Diese Methode fuegt einen String in die Logausgabe.
	 * @param str String
	 */
	public void setLog(String str) {
		synchronized (jTextAreaLog) {
			if (0 < str.length()) {
				jTextAreaLog.append(str);

				/* Ans Ende scrollen */
				jTextAreaLog.setCaretPosition(jTextAreaLog.getText().length());
			}
		}		
	}
}
