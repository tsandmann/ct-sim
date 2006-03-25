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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

/** 
 * Die Haupt-View-Klasse des c't-Sim, welche die ControlPanels und die
 * WorldView-Klasse in einem grossen JSplitPane zusammen fuehrt.
 * 
 * @author Markus Lang (lang@repulse.de)
 * @version 2006-03-06
 */
public class CtSimFrame extends JFrame implements WindowStateListener {
	private static final long serialVersionUID = 1L;

	private JSplitPane splitPane;
	private ControlFrame controlFrame;
	
	/**
	 * Konstruktor: Erzeugt ein neues CtSimFrame-Hauptfenster
	 * 
	 * @param controlFrame die ControlFrame-Instanz (leftComponent)
	 * @param worldView  die WorldView-Instanz (rightComponent)
	 */
	public CtSimFrame(ControlFrame controlFrame, WorldView worldView) {
		// Super-Konstructor aufrufen
		super();
		// GUI-Elemente laden
		this.controlFrame = controlFrame;
		this.initGUI(worldView);
		// EventListener registrieren
		this.addWindowStateListener(this);
		// Fenstergroesse festlegen
		Dimension dimension = new Dimension(1024, 700);
		this.setMinimumSize(dimension);
		this.setPreferredSize(dimension);
		this.setSize(dimension);
		// Fenster positionieren
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setBounds(
				(screenSize.width / 2) - (dimension.width / 2),
				(screenSize.height / 2) - (dimension.height / 2),
				dimension.width, dimension.height);
		this.setVisible(true);
		this.pack();
	}
	
	/**
	 * Initialisierung der GUI-Elemente
	 *  
	 * @param worldView  die WorldView-Instanz (rightComponent)
	 */
	private void initGUI(WorldView worldView) {
		// Layout definieren
		this.setTitle("c't-Sim");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		BoxLayout thisLayout = new BoxLayout(getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
		this.getContentPane().setLayout(thisLayout);
		// min- & max-Groesse des controlFrame setzen
		Dimension preferedSize = this.controlFrame.getPreferredSize();
		this.controlFrame.setMinimumSize(preferedSize);
		this.controlFrame.setMaximumSize(preferedSize);
		// SplitPane erstellen
		this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.controlFrame, worldView); 
		this.splitPane.setContinuousLayout(false); 
		this.splitPane.setOneTouchExpandable(true);
		
		this.getContentPane().add(this.splitPane, BorderLayout.CENTER); 
	}
	
	/**
	 * Beim Aufruf dieser Methode wird die Slider-Position neu bestimmt
	 */
	public void updateSliderPosition() {
		// min- & max-Groesse des ControlFrame setzen
		Dimension preferredSize = this.controlFrame.getPreferredSize();
		this.controlFrame.setMinimumSize(preferredSize);
		this.controlFrame.setMaximumSize(preferredSize);
		this.controlFrame.revalidate();
		// splitPane Divider positionieren
		this.splitPane.setDividerLocation(
				this.controlFrame.getPreferredSize().width + this.splitPane.getInsets().left);
	}
	
	/**
	 * Implementierte Methode des WindowStateListener-Interfaces.
	 * Wird dazu genutzt, beim Minimieren/Maximieren des Fensters die 
	 * Divider-Position der SplitPlane zu aktualisieren.
	 * 
	 * @see java.awt.event.WindowStateListener#windowStateChanged(java.awt.event.WindowEvent)
	 */
	public void windowStateChanged(WindowEvent arg0) {
		this.updateSliderPosition();
	}
}
