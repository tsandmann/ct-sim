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
import java.awt.HeadlessException;
import java.awt.Toolkit;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JSplitPane;

/** 
 * Ein Fenster fuer den ctSimClient
 * @author Benjamin Benz (bbe@heise.de)
 */
public class clientFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JSplitPane splitPane;

	public clientFrame(WorldView worldView) throws HeadlessException {
		super();
		this.initGUI(worldView);
		// Fenstergroesse festlegen
		Dimension dimension = new Dimension(1024, 700);
		setMinimumSize(dimension);
		setPreferredSize(dimension);
		setSize(dimension);
		// Fenster positionieren
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		/**
		 * @throws HeadlessException
		 */
		setBounds(
				(screenSize.width / 2) - (dimension.width / 2),
				(screenSize.height / 2) - (dimension.height / 2),
				dimension.width, dimension.height);
		setVisible(true);
		pack();
	}

	/**
	 * Initialisierung der GUI-Elemente
	 * @param worldView  die WorldView-Instanz (rightComponent)
	 */
	private void initGUI(WorldView worldView) {
		// Layout definieren
		setTitle("c't-Sim");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		BoxLayout thisLayout = new BoxLayout(getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
		getContentPane().setLayout(thisLayout);
		// min- & max-Groesse des controlFrame setzen
		// SplitPane erstellen
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,null ,worldView); 
		splitPane.setContinuousLayout(false); 
		splitPane.setOneTouchExpandable(true);
		
		getContentPane().add(this.splitPane, BorderLayout.CENTER); 
	}

}
