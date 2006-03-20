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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ctSim.Model.*;
import ctSim.Controller.Controller;

/**
 * Bildet den Rahmen, der die Kontrolltafeln fuer alle Bots enthaelt.
 * 
 * @author Peter Koenig (pek@heise.de)
 * @author Werner Pirkl (morpheus.the.real@gmx.de)
 */

/* Markus Lang 2006-03-17:
 * ControlFrame erweitert nun die JPanel Klasse, da es kein
 * eingenst�ndiges Fenster mehr ist. Somit ist der Name
 * ControlFrame eigentlich missverst�ndlich.
 */
public class ControlFrame extends JPanel {
	private static final long serialVersionUID = 1L;

	private JTabbedPane controlPanels;

	private World world;

	/** Verweis auf den zugehoerigen Controller */
	private Controller controller;

	private boolean haveABreak;

	private boolean slowMotion;

	private JPanel buttonPanel;

	private JButton pauseButton;

	/**
	 * Slow Motion Box
	 */
	private JCheckBox slowMotionBox;

	private JSlider slowMotionSlider;

	private JPanel slowMotionSliderPanel;

	private JPanel slowMotionBoxPanel;

	private JPanel slowMotionPanel;

	private JTextField slowMotionText;

	private JButton endButton;
	
	/* Markus Lang 2006-03-17:
	 * Instanzen f�r das JSplitPane Hauptfenster
	 */
	private CtSimFrame ctSimFrame;

	/**
	 * Erzeugt einen neuen ControlFrame
	 */
	public ControlFrame(Controller controller) {
		super();
		this.controller = controller;
		haveABreak = false;
		world = controller.getWorld();

		initGUI();
	}

	/*
	 * Startet GUI
	 */
	private void initGUI() {

		Dimension buttDim = new Dimension(20, 70);

		try {
			BoxLayout thisLayout = new BoxLayout(this, javax.swing.BoxLayout.Y_AXIS);
			this.setLayout(thisLayout);
			{
				buttonPanel = new JPanel();
				BoxLayout panelLayout = new BoxLayout(buttonPanel,
						javax.swing.BoxLayout.X_AXIS);
				this.add(buttonPanel);
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
					// erstmal initialisieren und richtiges Layout
					this.slowMotionPanel = new JPanel();
					this.slowMotionBoxPanel = new JPanel();
					this.slowMotionSliderPanel = new JPanel();

					this.slowMotionPanel.setLayout(new BoxLayout(
							slowMotionPanel, BoxLayout.Y_AXIS));
					this.slowMotionSliderPanel.setLayout(new BoxLayout(
							slowMotionSliderPanel, BoxLayout.X_AXIS));

					slowMotionText = new JTextField();
					Dimension dim = new Dimension(30, 25);
					slowMotionText.setPreferredSize(dim);
					slowMotionText.setEditable(false);
					slowMotionText.setText("10");
					slowMotionText.setSize(30, 25);

					slowMotionBox = new JCheckBox("setzen", false);
					slowMotionSlider = new JSlider(JSlider.HORIZONTAL, 1, 21,
							10);
					slowMotionSlider.setMajorTickSpacing(5);
					slowMotionSlider.setMinorTickSpacing(1);
					slowMotionSlider.setPaintTicks(true);
					slowMotionSlider.setPaintLabels(true);

					// schliesslich zum Panel dazufuegen
					slowMotionBoxPanel.add(new javax.swing.JLabel(
							"Slow Motion (Faktor x):"));
					slowMotionBoxPanel.add(slowMotionText);
					slowMotionBoxPanel.add(slowMotionBox);
					slowMotionSliderPanel.add(slowMotionSlider);
					slowMotionPanel.add(slowMotionBoxPanel);
					slowMotionPanel.add(slowMotionSliderPanel);
					this.add(slowMotionPanel);
				}

				// und dann die Listener dazuhaengen
				slowMotionBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						slowMotion = slowMotionBox.isSelected();
						world.setSlowMotion(slowMotion);

						if (slowMotion) {
							slowMotionBox.setSelected(true);
							// unter 1 ist nicht gut
							int tempValue = slowMotionSlider.getValue();
							if (tempValue < 1)
								tempValue = 1;
							world.setSlowMotionTime(tempValue);
						} else {
							slowMotionBox.setSelected(false);
							// unter 1 ist nicht gut
							int tempValue = slowMotionSlider.getValue();
							if (tempValue < 1)
								tempValue = 1;
							world.setSlowMotionTime(tempValue);
						}
					}
				});

				slowMotionSlider.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent arg0) {
						// unter 1 ist nicht gut
						int tempValue = slowMotionSlider.getValue();
						if (tempValue < 1)
							tempValue = 1;
						world.setSlowMotionTime(tempValue);
						slowMotionText.setText(Integer.toString(tempValue));
					}

				});

				{
					endButton = new JButton();
					endButton.setPreferredSize(buttDim);
					buttonPanel.add(endButton);
					endButton.setText("Beenden");
				}
				endButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						controller.endSim();
					}
				});

			}

			{
				controlPanels = new JTabbedPane();
				this.add(controlPanels);
			}
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
		/* Markus Lang 2006-03-06:
		 * JSplitPlane �ber die Gr��en�nderung informieren
		 * (Ersatz f�r den alten pack() Aufruf der JFrame Klasse)
		 */
		this.ctSimFrame.updateSliderPosition();
	}

	/**
	 * @return Gibt das Feld zurueck, das alle ControlPanels enthaelt.
	 */
	public JTabbedPane getControlPanels() {
		return controlPanels;
	}
	
	/** Markus Lang 2006-03-17:
	 * Diese Methode setzt die interne CtSimFrame Instanz
	 * 
	 * @param ctSimFrame die neue CtSimFrame Instanz
	 * @author Markus Lang
	 */
	public void setCtSimFrameInstance(CtSimFrame ctSimFrame) {
		this.ctSimFrame = ctSimFrame;
 	}
}
