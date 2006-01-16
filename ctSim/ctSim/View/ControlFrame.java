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

package ctSim.View;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ctSim.Model.*;
import ctSim.Controller.Controller;

/**
 * Bildet den Rahmen, der die Kontrolltafeln fuer alle Bots enthaelt.
 * 
 * @author pek (pek@heise.de)
 * 
 */

public class ControlFrame extends javax.swing.JFrame {
	private static final long serialVersionUID = 1L;

	private JTabbedPane controlPanels;

	private World world;

	private boolean haveABreak;

	private JPanel buttonPanel;

	private JButton pauseButton;

	private JButton endButton;

	/**
	 * Erzeugt einen neuen ControlFrame
	 */
	public ControlFrame() {
		super();
		haveABreak = false;
		world = Controller.getWorld();
		initGUI();
	}

	/*
	 * Startet GUI
	 */
	private void initGUI() {

		Dimension buttDim = new Dimension(20, 70);

		try {
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			BoxLayout thisLayout = new BoxLayout(getContentPane(),
					javax.swing.BoxLayout.Y_AXIS);
			getContentPane().setLayout(thisLayout);
			this.setTitle("Kontrolltafel");
			{
				buttonPanel = new JPanel();
				BoxLayout panelLayout = new BoxLayout(buttonPanel,
						javax.swing.BoxLayout.X_AXIS);
				getContentPane().add(buttonPanel);
				buttonPanel.setLayout(panelLayout);
				{
					pauseButton = new JButton();
					pauseButton.setPreferredSize(buttDim);
					buttonPanel.add(pauseButton);
					if (!haveABreak) {
						pauseButton.setText("Pause");
					} else {
						pauseButton.setText("Resume");
					}
					pauseButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							haveABreak = !haveABreak;
							world.setHaveABreak(haveABreak);
							if (!haveABreak) {
								pauseButton.setText("Pause");
							} else {
								pauseButton.setText("Weiter");
							}

						}
					});

				}
				{
					endButton = new JButton();
					endButton.setPreferredSize(buttDim);
					buttonPanel.add(endButton);
					endButton.setText("Beenden");
				}
				endButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						Controller.endSim();
					}
				});

			}

			{
				controlPanels = new JTabbedPane();
				getContentPane().add(controlPanels);
			}
			setSize(300, 200);
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fuegt ein neues ControlPanel fuer einen Bot hinzu
	 * 
	 * @param bot
	 *            Referenz auf den Bot, der ein Panel erhalten soll
	 */
	public void addBot(Bot bot) {
		// Ein neues Panel fuer den Bot erzeugen
		controlPanels.addTab(bot.getBotName(), null, bot.getPanel(), null);
		// Dem Panel anzeigen, wo es dargestellt wird
		bot.getPanel().setFrame(this);
		pack();
	}

	/**
	 * @return Gibt das Feld zurueck, das alle ControlPanels enthaelt.
	 */
	public JTabbedPane getControlPanels() {
		return controlPanels;
	}
}
