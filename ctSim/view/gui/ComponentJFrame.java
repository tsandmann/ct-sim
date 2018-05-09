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

import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import ctSim.util.AuxFrameButton;
import ctSim.util.FmtLogger;

/**
 * Ein JFrame, das die letzte Position speichert und einen KeyHandler zum Schließen des Fensters
 * implementiert
 * 
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class ComponentJFrame extends JFrame implements WindowListener {
	/** UID */
	private static final long serialVersionUID = 1407018820276635315L;
	
	/** Logger für das Fenster */
	private final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.ComponentJFrame");
	
	/** Positionen aller Frames */
	protected static PositionMap positions = null;
	
	/** Inhalt des Frames */
	protected JComponent comp;

	/** Button, der diesen Frame erzeugt hat / öffnet und schließt */
	protected AuxFrameButton button;
	
	/**
	 * Entfernt alles nach dem ersten Leerzeichen aus einem Fenstertitel
	 * 
	 * @param title	Fenstertitel
	 * @return Titel bis zum ersten Leerzeichen
	 */
	private static String stripTitle(String title) {
		final int index = title.indexOf(" ");
		if (index >= 0) {
			return title.substring(0, index);
		} else {
			return title;
		}
	}
	
	/**
	 * @param title		Fenstertitel
	 * @param comp		Inhalt des Fensters
	 * @param button	Button, der zu diesem Fenster führt
	 */
	public ComponentJFrame(String title, final JComponent comp, final AuxFrameButton button) {
		super(title);
		this.comp = comp;
		this.button = button;
		addWindowListener(this);
		
		if (positions == null) {
			/* Daten der Fenster-Positionen laden */
			ObjectInputStream objIn = null;
			try {
				objIn = new ObjectInputStream(new BufferedInputStream(new FileInputStream("config/window.settings")));
			} catch (FileNotFoundException e1) {
				// egal
			} catch (IOException e1) {
				// egal
			} 
			if (objIn != null) {
				try {
					positions = (PositionMap) objIn.readObject();
				} catch (IOException e1) {
					// egal
				} catch (ClassNotFoundException e1) {
					// egal
				} 
				try {
					objIn.close();
				} catch (IOException e1) {
					// egal
				}
			} else {
				/* Laden nicht möglich, leere Map erzeugen */
				positions = new PositionMap();
			}
		}
		
		/* Position dieses Fensters laden */
		Point p = positions.getMap().get(stripTitle(title));
		if (p != null) {
			setLocation(p);
		} else {
			/* noch nicht vorhanden, default in der Map speichern */
			positions.getMap().put(stripTitle(title), getLocation());
		}

		/* Key-Handler */
		InputMap inputMap = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		// Ctrl / Cmd + w
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        inputMap.put(key, "close");
        comp.getActionMap().put("close", new AbstractAction() {
			private static final long serialVersionUID = -7639062435105576105L;
			public void actionPerformed(ActionEvent e) {
				try {
					button.doClick();	// Klick auf den Button schließt das Fenster und schaltet den Button um
				} catch (NullPointerException ex) {
					// egal
				}
			}
        });
	}

	/**
	 * Handler, der aufgerufen wird, wenn das Fenster geschlossen wird
	 * 
	 * @param e	Event
	 */
	public void windowClosed(WindowEvent e) {
		lg.fine(this.getTitle() + ": windowClosed()");
		positions.getMap().put(stripTitle(getTitle()), getLocation());
		
		ObjectOutputStream objOut = null;
		try {
			objOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("config/window.settings")));
		} catch (FileNotFoundException ex) {
			lg.warn("Fenster-Positionen konnten nicht gespeichert werden: " + ex.getMessage());
		} catch (IOException ex) {
			lg.warn("Fenster-Positionen konnten nicht gespeichert werden: " + ex.getMessage());
		} 
		if (objOut == null) {
			return;
		}
		try {
			objOut.writeObject(positions);
		} catch (IOException ex) {
			lg.warn("Fenster-Positionen konnten nicht gespeichert werden: " + ex.getMessage());
		}
		try {
			objOut.close();
		} catch (IOException ex) {
			lg.warn("Fenster-Positionen konnten nicht gespeichert werden:" + ex.getMessage());
		}
	}

	
	/* Zeug, das ein WindowListener verlangt, das wir aber hier nicht brauchen */
	
	/**
	 * @param e
	 */
	public void windowActivated(WindowEvent e) {
		// No-op
	}
	
	/**
	 * @param e
	 */
	public void windowClosing(WindowEvent e) {
		// No-op
	}

	/**
	 * @param e
	 */
	public void windowDeactivated(WindowEvent e) {
		// No-op
	}

	/**
	 * @param e
	 */
	public void windowDeiconified(WindowEvent e) {
		// No-op
	}

	/**
	 * @param e
	 */
	public void windowIconified(WindowEvent e) {
		// No-op
	}

	/**
	 * @param e
	 */
	public void windowOpened(WindowEvent e) {
		// No-op
	}
}