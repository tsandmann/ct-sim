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

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.*;

import javax.vecmath.Vector3f;

import ctSim.Model.CtBot;
import ctSim.SimUtils;
import ctSim.View.LogFrame;

/**
 * Ergaenzt das generische, abstrakte ControlPanel um die richtigen Felder und
 * Regler fuer c't-Bots
 * 
 * @author pek (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Heinrich Peters (heinrich-peters@gmx.net)
 * 
 */
public class CtControlPanel extends ControlPanel {

	private static final long serialVersionUID = 1L;

	private static final int RC5_CODE_0 = 0x3940; // /< Taste 0

	private static final int RC5_CODE_1 = 0x3941; // /< Taste 1

	private static final int RC5_CODE_2 = 0x3942; // /< Taste 2

	private static final int RC5_CODE_3 = 0x3943; // /< Taste 3

	private static final int RC5_CODE_4 = 0x3944; // /< Taste 4

	private static final int RC5_CODE_5 = 0x3945; // /< Taste 5

	private static final int RC5_CODE_6 = 0x3946; // /< Taste 6

	private static final int RC5_CODE_7 = 0x3947; // /< Taste 7

	private static final int RC5_CODE_8 = 0x3948; // /< Taste 8

	private static final int RC5_CODE_9 = 0x3949; // /< Taste 9

	private static final int RC5_CODE_UP = 0x2950; // /< Taste Hoch

	private static final int RC5_CODE_DOWN = 0x2951; // /< Taste Runter

	private static final int RC5_CODE_LEFT = 0x2955; // /< Taste Links

	private static final int RC5_CODE_RIGHT = 0x2956; // /< Taste Rechts

	private static final int RC5_CODE_PWR = 0x394C; // /< Taste An/Aus

	private static final int RC5_CODE_RED = 0x100B; // /< Taste An/Aus

	private static final int RC5_CODE_GREEN = 0x102E; // /< Taste An/Aus

	private static final int RC5_CODE_YELLOW = 0x1038; // /< Taste An/Aus

	private static final int RC5_CODE_BLUE = 0x1029; // /< Taste An/Aus
	
	private JTextField yPosField;

	private JTextField xPosField;

	private JTextField headField;
	
	private JLabel yPosLabel;

	private JLabel xPosLabel;

	private JPanel singlesPanel;

	private JPanel headSliderPanel;

	private JPanel xPosSliderPanel;

	private JPanel yPosSliderPanel;

	private JLabel headLabel;

	private JPanel headPanel;

	private JPanel yPosPanel;

	private JPanel xPosPanel;

	private JSlider headSlider;

	private JSlider xPosSlider;

	private JSlider yPosSlider;

	private JPanel multiplesPanel;

	private JLabel motLabelR;

	private JLabel irLabelL;

	private JTextField irValueR;

	private JSlider irSliderR;

	private JSlider irSliderL;

	private JLabel irLabelR;

	private JCheckBox rightIRSliderCheck;

	private JCheckBox leftIRSliderCheck;

	private JCheckBox headSliderCheck;

	private JCheckBox xPosSliderCheck;

	private JCheckBox yPosSliderCheck;

	private JPanel rightIRSliderPanel;

	private JPanel dummy6;

	private JPanel dummy5;

	private JButton jButtonDown;

	private JPanel dummy4;

	private JButton jButtonRight;

	private JButton jButtonOnOff;

	private JButton jButtonLeft;

	private JButton jButtonUp;

	private JPanel dummy3;

	private JPanel dummy2;

	private JButton jButton10;

	private JButton jButton9;

	private JButton jButton8;

	private JButton jButton7;

	private JButton jButton2;

	private JPanel dummy8;

	private JPanel keyPanel;

	private JButton jButton6;

	private JButton jButton5;

	private JButton jButton4;

	private JButton jButton3;

	private JButton jButton1;

	private JPanel rc5Panel;

	private JPanel irPanelR;

	private JPanel motPanelR;

	private JPanel irPanelL;

	private JPanel motPanelL;

	private JLabel rechtsLabel;

	private JLabel linksLabel;

	private JPanel rightPanel;

	private JPanel leftPanel;

	private JPanel leftIRSliderPanel;

	private JTextField irValueL;

	private JTextField motorValueL;

	private JTextField motorValueR;

	private JLabel motorLabelL;


	private boolean irL=false;
	private boolean irR=false;
	private boolean xpos= false;
	private boolean ypos=false;
	private boolean head= false;

	private JPanel sensorPanel;

	// LCDisplay
	private JPanel botPanel;

	private JPanel lcdPanel;

	private JLabel lcdLabel;

	private JLabel lcdLine1;

	private JLabel lcdLine2;

	private JLabel lcdLine3;

	private JLabel lcdLine4;

	// LEDs
	private JPanel ledPanel;

	private JLabel ledLabel;

	JPanel led1, led2, led3, led4, led5, led6, led7, led8;

	private static final Color colLed1 = new Color(137, 176, 255);

	private static final Color colLed1Akt = new Color(0, 84, 255); // blaue LED

	private static final Color colLed2 = new Color(137, 176, 255);

	private static final Color colLed2Akt = new Color(0, 84, 255); // blaue LED

	private static final Color colLed3 = new Color(255, 137, 137);

	private static final Color colLed3Akt = new Color(255, 0, 0); // rote LED

	private static final Color colLed4 = new Color(255, 230, 139);

	private static final Color colLed4Akt = new Color(255, 200, 0); // orange
																	// LED

	private static final Color colLed5 = new Color(255, 255, 159);

	private static final Color colLed5Akt = new Color(255, 255, 0); // gelbe LED

	private static final Color colLed6 = new Color(170, 255, 170);

	private static final Color colLed6Akt = new Color(0, 255, 0); // gruene
																	// LED

	private static final Color colLed7 = new Color(200, 255, 245);

	private static final Color colLed7Akt = new Color(0, 255, 210); // tuekise
																	// LED

	private static final Color colLed8 = new Color(245, 245, 245);

	private static final Color colLed8Akt = new Color(255, 255, 255);// weisse
																		// LED

	// Farb-Tasten
	private JPanel colKeyPanel;

	private JButton jButtonRed, jButtonGreen, jButtonYellow, jButtonBlue;

//	 Anzeigen von Loggings
	private JPanel jPanelLog;
	private JButton jButtonLog;
	private LogFrame logFrame;
	
	// Anzeige der Maussensordaten
	private JPanel msPanel;

	private JLabel msLabel;

	private JTextField msDeltaX;

	private JTextField msDeltaY;

	private JLabel msLabelX;

	private JLabel msLabelY;

	// Anzeige der Liniensensoren
	private JPanel linePanel;

	private JLabel lineLabel;

	private JTextField lineR;

	private JTextField lineL;

	private JLabel lineLabelR;

	private JLabel lineLabelL;

	// Anzeige der Abgrundsensoren
	private JPanel borderPanel;

	private JLabel borderLabel;

	private JTextField borderR;

	private JTextField borderL;

	private JLabel borderLabelR;

	private JLabel borderLabelL;

	// Anzeige der Lichtsensoren
	private JPanel ldrPanel;

	private JLabel ldrLabel;

	private JTextField ldrR;

	private JTextField ldrL;

	private JLabel ldrLabelR;

	private JLabel ldrLabelL;

	// Aufteilung des Tabs in linke und rechte Seite
	private JPanel mainPanelRight;

	private JPanel mainPanelLeft;

	/**
	 * Der Konstruktor
	 * 
	 * @param bot
	 *            Eine Referenz auf den Bot, zu dem das Panel gehoert
	 */
	public CtControlPanel(CtBot bot) {
		super(bot);

		//initGUI();
	}

	/*
	 * (non-Javadoc) Startet GUI
	 * 
	 * @see ctSim.View.ControlPanel#initGUI()
	 */
	protected void initGUI() {
		Dimension labelDim, fieldDim, smallGap;
		
		labelDim = new Dimension(70, 25);
		fieldDim = new Dimension(50, 25);
		smallGap = new Dimension(5, 5);

		yPosField= new JTextField();
		xPosField= new JTextField();
		headField= new JTextField();

		
		xPosField.setPreferredSize(fieldDim);
		yPosField.setPreferredSize(fieldDim);
		headField.setPreferredSize(fieldDim);

		
		try {
			// Das Tab wird in zwei Spalten augeteilt
			BoxLayout thisLayout = new BoxLayout(this,
					javax.swing.BoxLayout.X_AXIS);
			this.setLayout(thisLayout);

			// linke Spalte initialisieren
			mainPanelLeft = new JPanel();
			mainPanelLeft.setLayout(new BoxLayout(mainPanelLeft,
					javax.swing.BoxLayout.Y_AXIS));
			this.add(mainPanelLeft);

			this.add(Box.createRigidArea(smallGap));

			// rechte Spalte initialisieren
			mainPanelRight = new JPanel();
			mainPanelRight.setLayout(new BoxLayout(mainPanelRight,
					javax.swing.BoxLayout.Y_AXIS));
			this.add(mainPanelRight);

			// linke Spalte fuellen
			{
				singlesPanel = new JPanel();
				mainPanelLeft.add(singlesPanel);
				BoxLayout singlesPanelLayout = new BoxLayout(singlesPanel,
						javax.swing.BoxLayout.Y_AXIS);
				singlesPanel.setLayout(singlesPanelLayout);
				{
					headPanel = new JPanel();
					singlesPanel.add(headPanel);
					{
						headLabel = new JLabel();
						headPanel.add(headLabel);
						headLabel.setText("Richtung");
					}
					{
						headPanel.add(getHeadField());
						getHeadField().setText("0");
						getHeadField().setEditable(false);
					}
				}

				if (    ((CtBot)getBot()).CAP_HEAD   ) {
					{
						headSliderPanel = new JPanel();
						singlesPanel.add(headSliderPanel);
						BoxLayout headSliderPanelLayout = new BoxLayout(
								headSliderPanel, javax.swing.BoxLayout.Y_AXIS);
						headSliderPanel.setLayout(headSliderPanelLayout);
						{
							headSlider = new JSlider(JSlider.HORIZONTAL, -180,
									180, 0);
							headSliderPanel.add(headSlider);
							headSlider.setMajorTickSpacing(45);
							headSlider.setMinorTickSpacing(15);
							headSlider.setPaintTicks(true);
							headSlider.setPaintLabels(true);
						}
						{
							headSliderCheck = new JCheckBox();
							headPanel.add(headSliderCheck);
							headSliderCheck.setText("setzen" + "");
							headSliderCheck.addItemListener(new ItemListener() {
								public void itemStateChanged(ItemEvent e) {
									head = (e.getStateChange() == ItemEvent.SELECTED);
								}
							});
						}
					}
				}

				{
					xPosPanel = new JPanel();
					singlesPanel.add(xPosPanel);
					{
						xPosLabel = new JLabel();
						xPosPanel.add(xPosLabel);
						xPosLabel.setText("X Position");
					}
					{

						xPosPanel.add(getXPosField());
						getXPosField().setEditable(false);
						getXPosField().setText("0");
					}

				}
				if (((CtBot)getBot()).CAP_POS) {
					{
						xPosSliderPanel = new JPanel();
						singlesPanel.add(xPosSliderPanel);
						BoxLayout xPosSliderPanelLayout = new BoxLayout(
								xPosSliderPanel, javax.swing.BoxLayout.Y_AXIS);
						xPosSliderPanel.setLayout(xPosSliderPanelLayout);
						{
							xPosSlider = new JSlider(
									JSlider.HORIZONTAL,
//									Math.round(-100 * bot.getWorld().getPlaygroundDimX() / 2),
//									Math.round(100 * bot.getWorld().getPlaygroundDimX() / 2),
									Math.round(0),
									Math.round(100 * getBot().getWorld().getPlaygroundDimX()),
									0);
							
							xPosSliderPanel.add(xPosSlider);
							xPosSlider
									.setMajorTickSpacing((int) getBot().getWorld().getPlaygroundDimX() * 10);
							xPosSlider
									.setMinorTickSpacing((int) getBot().getWorld().getPlaygroundDimX() * 5);
							xPosSlider.setPaintTicks(true);
							xPosSlider.setPaintLabels(true);
						}
						{
							xPosSliderCheck = new JCheckBox();
							xPosPanel.add(xPosSliderCheck);
							xPosSliderCheck.setText("setzen" + "");
							xPosSliderCheck.addItemListener(new ItemListener() {
								public void itemStateChanged(ItemEvent e) {
									xpos = (e.getStateChange() == ItemEvent.SELECTED);
								}
							});
						}
					}
				}

				{
					yPosPanel = new JPanel();
					singlesPanel.add(yPosPanel);
					{
						yPosLabel = new JLabel();
						yPosPanel.add(yPosLabel);
						yPosLabel.setText("Y Position");
					}
					{
						yPosPanel.add(getYPosField());
						getYPosField().setText("0");
						getYPosField().setEditable(false);
					}
				}

				if (((CtBot)getBot()).CAP_POS) {
					{
						yPosSliderPanel = new JPanel();
						singlesPanel.add(yPosSliderPanel);
						BoxLayout yPosSliderPanelLayout = new BoxLayout(
								yPosSliderPanel, javax.swing.BoxLayout.Y_AXIS);
						yPosSliderPanel.setLayout(yPosSliderPanelLayout);
						{
							yPosSlider = new JSlider(
									JSlider.HORIZONTAL,
									Math.round(0),
									Math.round(100 * getBot().getWorld().getPlaygroundDimY()),
//									Math.round(-100 * bot.getWorld().getPlaygroundDimY()/ 2),
//									Math.round(100 * bot.getWorld().getPlaygroundDimY() / 2),
									0);
							yPosSliderPanel.add(yPosSlider);
							yPosSlider
									.setMajorTickSpacing((int) getBot().getWorld().getPlaygroundDimY() * 10);
							yPosSlider
									.setMinorTickSpacing((int) getBot().getWorld().getPlaygroundDimY() * 5);
							yPosSlider.setPaintTicks(true);
							yPosSlider.setPaintLabels(true);
						}
						{
							yPosSliderCheck = new JCheckBox();
							yPosPanel.add(yPosSliderCheck);
							yPosSliderCheck.setText("setzen" + "");
							yPosSliderCheck.addItemListener(new ItemListener() {
								public void itemStateChanged(ItemEvent e) {
									ypos = (e.getStateChange() == ItemEvent.SELECTED);
								}
							});
						}
					}
				}

				// Motor PWM + IR-Sensoren
				{
					multiplesPanel = new JPanel();
					mainPanelLeft.add(multiplesPanel);
					BoxLayout multiplesPanelLayout = new BoxLayout(
							multiplesPanel, javax.swing.BoxLayout.X_AXIS);
					multiplesPanel.setLayout(multiplesPanelLayout);
				}

				leftPanel = new JPanel();
				BoxLayout leftPanelLayout = new BoxLayout(leftPanel,
						javax.swing.BoxLayout.Y_AXIS);
				multiplesPanel.add(leftPanel);
				leftPanel.setLayout(leftPanelLayout);
				{
					linksLabel = new JLabel();
					leftPanel.add(linksLabel);
					linksLabel.setText("LINKS");
				}
				{
					motPanelL = new JPanel();
					leftPanel.add(motPanelL);
					BoxLayout motPanelLLayout = new BoxLayout(motPanelL,
							javax.swing.BoxLayout.X_AXIS);
					motPanelL.setLayout(motPanelLLayout);
					{
						motorLabelL = new JLabel();
						motPanelL.add(motorLabelL);
						motorLabelL.setText("Motor PWM");
						motorLabelL.setPreferredSize(labelDim);
					}
					{
						motorValueL = new JTextField();
						motPanelL.add(motorValueL);
						motorValueL.setText("0");
						motorValueL.setEditable(false);
						motorValueL.setPreferredSize(fieldDim);
					}
				}
				{
					irPanelL = new JPanel();
					leftPanel.add(irPanelL);
					BoxLayout irPanelLLayout = new BoxLayout(irPanelL,
							javax.swing.BoxLayout.X_AXIS);
					irPanelL.setLayout(irPanelLLayout);
					{
						irLabelL = new JLabel();
						irPanelL.add(irLabelL);
						irLabelL.setText("IR-Sensor");
						irLabelL.setPreferredSize(labelDim);
					}
					{
						irValueL = new JTextField();
						irPanelL.add(irValueL);
						irValueL.setText("0");
						irValueL.setEditable(false);
						irValueL.setPreferredSize(fieldDim);
					}
				}

				if (((CtBot)getBot()).CAP_SENS_IR) {
					{
						leftIRSliderPanel = new JPanel();
						leftPanel.add(leftIRSliderPanel);
						BoxLayout leftIRSliderPanelLayout = new BoxLayout(
								leftIRSliderPanel, javax.swing.BoxLayout.X_AXIS);
						leftIRSliderPanel.setLayout(leftIRSliderPanelLayout);
						{
							irSliderL = new JSlider(JSlider.VERTICAL, 0, 1000,
									0);
							leftIRSliderPanel.add(irSliderL);
							irSliderL.setMajorTickSpacing(200);
							irSliderL.setMinorTickSpacing(100);
							irSliderL.setPaintTicks(true);
							irSliderL.setPaintLabels(true);
						}
						{
							leftIRSliderCheck = new JCheckBox();
							leftIRSliderPanel.add(leftIRSliderCheck);
							leftIRSliderCheck.setText("setzen" + "");
							leftIRSliderCheck
									.addItemListener(new ItemListener() {
										public void itemStateChanged(ItemEvent e) {
											irL = (e.getStateChange() == ItemEvent.SELECTED);
										}
									});
						}
					}
				}

				{
					rightPanel = new JPanel();
					BoxLayout rightPanelLayout = new BoxLayout(rightPanel,
							javax.swing.BoxLayout.Y_AXIS);
					multiplesPanel.add(rightPanel);
					rightPanel.setLayout(rightPanelLayout);
					{
						rechtsLabel = new JLabel();
						rightPanel.add(rechtsLabel);
						rechtsLabel.setText("RECHTS");
					}
					{
						motPanelR = new JPanel();
						rightPanel.add(motPanelR);
						BoxLayout motPanelRLayout = new BoxLayout(motPanelR,
								javax.swing.BoxLayout.X_AXIS);
						motPanelR.setLayout(motPanelRLayout);
						{
							motLabelR = new JLabel();
							motPanelR.add(motLabelR);
							motLabelR.setText("Motor PWM");
							motLabelR.setPreferredSize(labelDim);
						}
						{
							motorValueR = new JTextField();
							motPanelR.add(motorValueR);
							motorValueR.setText("0");
							motorValueR.setEditable(false);
							motorValueR.setPreferredSize(fieldDim);
						}
					}
					{
						irPanelR = new JPanel();
						rightPanel.add(irPanelR);
						BoxLayout irPanelRLayout = new BoxLayout(irPanelR,
								javax.swing.BoxLayout.X_AXIS);
						irPanelR.setLayout(irPanelRLayout);
						{
							irLabelR = new JLabel();
							irPanelR.add(irLabelR);
							irLabelR.setText("IR-Sensor");
							irLabelR.setPreferredSize(labelDim);
						}
						{
							irValueR = new JTextField();
							irPanelR.add(irValueR);
							irValueR.setText("0");
							irValueR.setEditable(false);
							irValueR.setPreferredSize(fieldDim);
						}
					}

					if (((CtBot)getBot()).CAP_SENS_IR) {

						{
							rightIRSliderPanel = new JPanel();
							BoxLayout rightIRSliderPanelLayout = new BoxLayout(
									rightIRSliderPanel,
									javax.swing.BoxLayout.X_AXIS);
							rightIRSliderPanel
									.setLayout(rightIRSliderPanelLayout);
							rightPanel.add(rightIRSliderPanel);
							{
								irSliderR = new JSlider(JSlider.VERTICAL, 0,
										1000, 0);
								rightIRSliderPanel.add(irSliderR);
								irSliderR.setMajorTickSpacing(200);
								irSliderR.setMinorTickSpacing(100);
								irSliderR.setPaintTicks(true);
								irSliderR.setPaintLabels(true);
							}
							{
								rightIRSliderCheck = new JCheckBox();
								rightIRSliderPanel.add(rightIRSliderCheck);
								rightIRSliderCheck.setText("setzen");
								rightIRSliderCheck
										.addItemListener(new ItemListener() {
											public void itemStateChanged(
													ItemEvent evt) {
												irR = (evt.getStateChange() == ItemEvent.SELECTED);
											}
										});

							}
						}
					}
				}

				// Tastenfeld einfuegen
				keyPanel = new JPanel();
				mainPanelLeft.add(keyPanel);
				{
					keyPanel
							.setLayout(new BoxLayout(keyPanel, BoxLayout.Y_AXIS));

					// Zahlen- und Pfeiltasten
					rc5Panel = new JPanel();
					keyPanel.add(rc5Panel);
					GridLayout rc5PanelLayout = new GridLayout(1, 1);
					rc5PanelLayout.setHgap(5);
					rc5PanelLayout.setVgap(5);
					rc5PanelLayout.setColumns(3);
					rc5PanelLayout.setRows(7);
					rc5Panel.setLayout(rc5PanelLayout);
					rc5Panel.setPreferredSize(new java.awt.Dimension(200, 200));
					{
						jButton1 = new JButton();
						rc5Panel.add(jButton1);
						jButton1.setText("1");
						jButton1.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_1);
							}
						});
					}
					{
						jButton2 = new JButton();
						rc5Panel.add(jButton2);
						jButton2.setText("2");
						jButton2.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_2);
							}
						});
					}
					{
						jButton3 = new JButton();
						rc5Panel.add(jButton3);
						jButton3.setText("3");
						jButton3.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_3);
							}
						});
					}
					{
						jButton4 = new JButton();
						rc5Panel.add(jButton4);
						jButton4.setText("4");
						jButton4.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_4);
							}
						});
					}
					{
						jButton5 = new JButton();
						rc5Panel.add(jButton5);
						jButton5.setText("5");
						jButton5.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_5);
							}
						});
					}
					{
						jButton6 = new JButton();
						rc5Panel.add(jButton6);
						jButton6.setText("6");
						jButton6.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_6);
							}
						});
					}
					{
						jButton7 = new JButton();
						rc5Panel.add(jButton7);
						jButton7.setText("7");
						jButton7.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_7);
							}
						});
					}
					{
						jButton8 = new JButton();
						rc5Panel.add(jButton8);
						jButton8.setText("8");
						jButton8.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_8);
							}
						});
					}
					{
						jButton9 = new JButton();
						rc5Panel.add(jButton9);
						jButton9.setText("9");
						jButton9.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_9);
							}
						});
					}
					{
						jButtonOnOff = new JButton();
						rc5Panel.add(jButtonOnOff);
						jButtonOnOff.setText("ON / OFF");
						jButtonOnOff.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_PWR);
							}
						});
					}
					{
						jButton10 = new JButton();
						rc5Panel.add(jButton10);
						jButton10.setText("0");
						jButton10.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_0);
							}
						});
					}
					{
						dummy2 = new JPanel();
						rc5Panel.add(dummy2);
					}
					{
						dummy3 = new JPanel();
						rc5Panel.add(dummy3);
					}
					{
						jButtonUp = new JButton();
						rc5Panel.add(jButtonUp);
						jButtonUp.setText("UP");
						jButtonUp.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_UP);
							}
						});
					}
					{
						dummy4 = new JPanel();
						rc5Panel.add(dummy4);
					}
					{
						jButtonLeft = new JButton();
						rc5Panel.add(jButtonLeft);
						jButtonLeft.setText("LEFT");
						jButtonLeft.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_LEFT);
							}
						});
					}
					{
						dummy8 = new JPanel();
						rc5Panel.add(dummy8);
					}
					{
						jButtonRight = new JButton();
						rc5Panel.add(jButtonRight);
						jButtonRight.setText("RIGHT");
						jButtonRight.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_RIGHT);
							}
						});
					}
					{
						dummy5 = new JPanel();
						rc5Panel.add(dummy5);
					}
					{
						jButtonDown = new JButton();
						rc5Panel.add(jButtonDown);
						jButtonDown.setText("DOWN");
						jButtonDown.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent evt) {
								((CtBot)getBot()).setSensRc5(RC5_CODE_DOWN);
							}
						});
					}
					{
						dummy6 = new JPanel();
						rc5Panel.add(dummy6);
					}

					// Farb-Tastenfeld einfuegen
					colKeyPanel = new JPanel();
					keyPanel.add(colKeyPanel);
					colKeyPanel.setLayout(new GridLayout(1, 4, 5, 5));
					{
						{
							jButtonRed = new JButton();
							colKeyPanel.add(jButtonRed);
							jButtonRed.setBackground(new Color(255, 0, 0));
							jButtonRed.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent evt) {
									((CtBot)getBot()).setSensRc5(RC5_CODE_RED);
								}
							});
						}
						{
							jButtonGreen = new JButton();
							colKeyPanel.add(jButtonGreen);
							jButtonGreen.setBackground(new Color(0, 255, 0));
							jButtonGreen
									.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent evt) {
											((CtBot)getBot()).setSensRc5(RC5_CODE_GREEN);
										}
									});
						}
						{
							jButtonYellow = new JButton();
							colKeyPanel.add(jButtonYellow);
							jButtonYellow.setBackground(new Color(255, 255, 0));
							jButtonYellow
									.addActionListener(new ActionListener() {
										public void actionPerformed(
												ActionEvent evt) {
											((CtBot)getBot()).setSensRc5(RC5_CODE_YELLOW);
										}
									});
						}
						{
							jButtonBlue = new JButton();
							colKeyPanel.add(jButtonBlue);
							jButtonBlue.setBackground(new Color(0, 0, 255));
							jButtonBlue.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent evt) {
									((CtBot)getBot()).setSensRc5(RC5_CODE_BLUE);
								}
							});
						}
					} // Ende des Farb-Tastenfeldes
				} // Ende des Tastenfeldes
			} // Ende der linken Spalte

			// rechte Spalte fuellen
			{
				sensorPanel = new JPanel();
				mainPanelRight.add(sensorPanel);

				labelDim = new Dimension(50, 25);
				Dimension labelDimMax = new Dimension(100, 25);
				fieldDim = new Dimension(50, 25);
				Dimension fieldDimMax = new Dimension(100, 25);

				{
					sensorPanel.setLayout(new BoxLayout(sensorPanel,
							BoxLayout.Y_AXIS));

					// Maussensoranzeige
					msLabel = new JLabel();
					msLabel.setText("Maussensor");
					msLabel.setPreferredSize(labelDim);
					sensorPanel.add(msLabel);

					msPanel = new JPanel();
					sensorPanel.add(msPanel);
					BoxLayout msPanelLayout = new BoxLayout(msPanel,
							javax.swing.BoxLayout.X_AXIS);
					msPanel.setLayout(msPanelLayout);
					{
						msLabelX = new JLabel();
						msPanel.add(msLabelX);
						msLabelX.setText("Delta-X");
						msLabelX.setPreferredSize(labelDim);
						msLabelX.setMaximumSize(labelDimMax);

						msDeltaX = new JTextField();
						msPanel.add(msDeltaX);
						msDeltaX.setText("0");
						msDeltaX.setEditable(false);
						msDeltaX.setPreferredSize(fieldDim);
						msDeltaX.setMaximumSize(fieldDimMax);

						msPanel.add(Box.createRigidArea(smallGap));

						msLabelY = new JLabel();
						msPanel.add(msLabelY);
						msLabelY.setText("Delta-Y");
						msLabelY.setPreferredSize(labelDim);
						msLabelY.setMaximumSize(labelDimMax);

						msDeltaY = new JTextField();
						msPanel.add(msDeltaY);
						msDeltaY.setText("0");
						msDeltaY.setEditable(false);
						msDeltaY.setPreferredSize(fieldDim);
						msDeltaY.setMaximumSize(fieldDimMax);
					}

					sensorPanel.add(Box.createRigidArea(smallGap));

					// Liniensensoren
					lineLabel = new JLabel("Liniensensoren");
					lineLabel.setPreferredSize(labelDim);
					sensorPanel.add(lineLabel);
					linePanel = new JPanel();
					sensorPanel.add(linePanel);
					linePanel.setLayout(new BoxLayout(linePanel,
							BoxLayout.X_AXIS));
					{
						lineLabelL = new JLabel();
						linePanel.add(lineLabelL);
						lineLabelL.setText("links");
						lineLabelL.setPreferredSize(labelDim);
						lineLabelL.setMaximumSize(labelDimMax);

						lineL = new JTextField();
						linePanel.add(lineL);
						lineL.setText("0");
						lineL.setEditable(false);
						lineL.setPreferredSize(fieldDim);
						lineL.setMaximumSize(fieldDimMax);

						linePanel.add(Box.createRigidArea(smallGap));

						lineLabelR = new JLabel();
						linePanel.add(lineLabelR);
						lineLabelR.setText("rechts");
						lineLabelR.setPreferredSize(labelDim);
						lineLabelR.setMaximumSize(labelDimMax);

						lineR = new JTextField();
						linePanel.add(lineR);
						lineR.setText("0");
						lineR.setEditable(false);
						lineR.setPreferredSize(fieldDim);
						lineR.setMaximumSize(fieldDimMax);
					}

					sensorPanel.add(Box.createRigidArea(smallGap));

					// Abgrundsensoren
					borderLabel = new JLabel("Abgrundsensoren");
					borderLabel.setPreferredSize(labelDim);
					sensorPanel.add(borderLabel);
					borderPanel = new JPanel();
					sensorPanel.add(borderPanel);
					borderPanel.setLayout(new BoxLayout(borderPanel,
							BoxLayout.X_AXIS));
					{
						borderLabelL = new JLabel();
						borderPanel.add(borderLabelL);
						borderLabelL.setText("links");
						borderLabelL.setPreferredSize(labelDim);
						borderLabelL.setMaximumSize(labelDimMax);

						borderL = new JTextField();
						borderPanel.add(borderL);
						borderL.setText("0");
						borderL.setEditable(false);
						borderL.setPreferredSize(fieldDim);
						borderL.setMaximumSize(fieldDimMax);

						borderPanel.add(Box.createRigidArea(smallGap));

						borderLabelR = new JLabel();
						borderPanel.add(borderLabelR);
						borderLabelR.setText("rechts");
						borderLabelR.setPreferredSize(labelDim);
						borderLabelR.setMaximumSize(labelDimMax);

						borderR = new JTextField();
						borderPanel.add(borderR);
						borderR.setText("0");
						borderR.setEditable(false);
						borderR.setPreferredSize(fieldDim);
						borderR.setMaximumSize(fieldDimMax);
					}

					sensorPanel.add(Box.createRigidArea(smallGap));

					// Lichtsensoren
					ldrLabel = new JLabel("Lichtsensoren");
					ldrLabel.setPreferredSize(labelDim);
					sensorPanel.add(ldrLabel);
					ldrPanel = new JPanel();
					sensorPanel.add(ldrPanel);
					ldrPanel
							.setLayout(new BoxLayout(ldrPanel, BoxLayout.X_AXIS));
					{
						ldrLabelL = new JLabel();
						ldrPanel.add(ldrLabelL);
						ldrLabelL.setText("links");
						ldrLabelL.setPreferredSize(labelDim);
						ldrLabelL.setMaximumSize(labelDimMax);

						ldrL = new JTextField();
						ldrPanel.add(ldrL);
						ldrL.setText("0");
						ldrL.setEditable(false);
						ldrL.setPreferredSize(fieldDim);
						ldrL.setMaximumSize(fieldDimMax);

						ldrPanel.add(Box.createRigidArea(smallGap));

						ldrLabelR = new JLabel();
						ldrPanel.add(ldrLabelR);
						ldrLabelR.setText("rechts");
						ldrLabelR.setPreferredSize(labelDim);
						ldrLabelR.setMaximumSize(labelDimMax);

						ldrR = new JTextField();
						ldrPanel.add(ldrR);
						ldrR.setText("0");
						ldrR.setEditable(false);
						ldrR.setPreferredSize(fieldDim);
						ldrR.setMaximumSize(fieldDimMax);
					}

				} // Ende Sensor Panel

				mainPanelRight.add(Box.createRigidArea(smallGap));

				/*
				 * Debug-Ausgaben am Roboter: damit die hier angezeigt werden:
				 * Kommentarzeichen in ct-bot.h vor "#define
				 * DISPLAY_REMOTE_AVAILABLE" entfernen!
				 */
				botPanel = new JPanel();
				mainPanelRight.add(botPanel);
				{
					botPanel
							.setLayout(new BoxLayout(botPanel, BoxLayout.Y_AXIS));

					// LCD - Pannel
					lcdLabel = new JLabel("Display");
					botPanel.add(lcdLabel);

					lcdPanel = new JPanel();
					botPanel.add(lcdPanel);
					{
						lcdPanel.setLayout(new GridLayout(4, 1, 5, 5));
						lcdPanel.setBorder(new EtchedBorder());
						lcdPanel.setBackground(new Color(120, 150, 90));
						lcdPanel.setPreferredSize(new Dimension(145, 85));
						lcdPanel.setMaximumSize(new Dimension(150, 90));

						Font Display = new Font("Monospaced", Font.BOLD, 12);

						// füge Zeilen für die LCD Ausgabe ein
						lcdLine1 = new JLabel();
						lcdLine2 = new JLabel();
						lcdLine3 = new JLabel();
						lcdLine4 = new JLabel();

						lcdLine1.setFont(Display);
						lcdLine2.setFont(Display);
						lcdLine3.setFont(Display);
						lcdLine4.setFont(Display);

						lcdPanel.add(lcdLine1);
						lcdPanel.add(lcdLine2);
						lcdPanel.add(lcdLine3);
						lcdPanel.add(lcdLine4);
					}

					botPanel.add(Box.createRigidArea(smallGap));

					// LED Panel
					ledLabel = new JLabel("LEDs");
					ledLabel.setPreferredSize(labelDim);
					ledLabel.setMaximumSize(labelDimMax);

					botPanel.add(ledLabel);

					ledPanel = new JPanel();
					botPanel.add(ledPanel);
					{
						ledPanel.setMaximumSize(new Dimension(175, 25));
						Dimension ledDim = new Dimension(15, 15);

						ledPanel.setLayout(new GridLayout(1, 8, 5, 5));

						led1 = new JPanel();
						led2 = new JPanel();
						led3 = new JPanel();
						led4 = new JPanel();
						led5 = new JPanel();
						led6 = new JPanel();
						led7 = new JPanel();
						led8 = new JPanel();

						// Groesse und Farbe der LEDs setzen
						led1.setMaximumSize(ledDim);
						led1.setBackground(colLed1);
						led2.setMaximumSize(ledDim);
						led2.setBackground(colLed2);
						led3.setMaximumSize(ledDim);
						led3.setBackground(colLed3);
						led3.setMaximumSize(ledDim);
						led4.setBackground(colLed4);
						led5.setMaximumSize(ledDim);
						led5.setBackground(colLed5);
						led6.setMaximumSize(ledDim);
						led6.setBackground(colLed6);
						led7.setMaximumSize(ledDim);
						led7.setBackground(colLed7);
						led8.setMaximumSize(ledDim);
						led8.setBackground(colLed8);

						ledPanel.add(led1);
						ledPanel.add(led2);
						ledPanel.add(led3);
						ledPanel.add(led4);
						ledPanel.add(led5);
						ledPanel.add(led6);
						ledPanel.add(led7);
						ledPanel.add(led8);
					} // LED-Panel ENDE
				} // Bot-Panel ENDE

				// Anzeigen von Loggings
				logFrame = new LogFrame();
				jPanelLog = new JPanel();
				jButtonLog = new JButton("Log anzeigen");
				jPanelLog.add(jButtonLog);
				jButtonLog.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						
						if (false == logFrame.isVisible()) {
							logFrame.setVisible(true);
						}
						else {
							logFrame.setVisible(false);
						}
					}
				});
				mainPanelRight.add(jPanelLog);
				
				mainPanelRight.add(Box.createRigidArea(smallGap));

				// freien Platz fuellen
				// mainPanelRight.add(Box.createHorizontalGlue());
				// mainPanelRight.add(Box.createRigidArea(new
				// Dimension(0,700)));
				mainPanelRight.add(new Box.Filler(smallGap, new Dimension(10,
						10), new Dimension(10, Short.MAX_VALUE)));
			} // Ende der rechten Spalte
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc) Wird aufgerufen, wenn sich der Zustand des Bot veraendert
	 * hat
	 * 
	 * @see ctSim.View.ControlPanel#reactToChange()
	 */
	public void reactToChange() {

		/*
		 * Zuerst wird geprueft, ob die Slider oder der Bot die jeweiligen Werte
		 * bestimmen, dann werden die Werte in die Textfelder geschrieben:
		 */

		// Blickrichtung:
		if (((CtBot)getBot()).CAP_HEAD) {
			/*
			 * Falls Checkbox inaktiv ist, wird der Slider auf den Wert des
			 * Headings aus dem Bot gesetzt:
			 */
			if (!head) {
				headSlider.setValue(Math.round(Math.round(SimUtils
						.vec3fToDouble(((CtBot)getBot()).getHeading()))));
			} else {
				/* Ansonsten bestimmt der Slider den Wert beim Bot: */
				((CtBot)getBot()).setHeading(SimUtils.intToVec3f(headSlider.getValue()));
			}
		}
		// Hole aktuellen Wert aus dem Bot und setzt ihn ins Textfeld:
		getHeadField().setText(SimUtils.vec3fToString(((CtBot)getBot()).getHeading()));

		// Position:

		if (((CtBot)getBot()).CAP_POS) {
			/*
			 * Falls Checkbox fuer X inaktiv ist, wird der Slider auf den Wert
			 * der Position aus dem Bot gesetzt:
			 */
			if (!xpos) {
				xPosSlider.setValue(Math.round(getBot().getPos().x * 100));
			} else {
				/* Ansonsten bestimmt der Slider den Wert bei Bot und Panel: */
				getBot().setPos(new Vector3f((float) xPosSlider.getValue() / 100f,
						getBot().getPos().y, getBot().getPos().z));
			}

			/*
			 * Falls Checkbox fuer Y inaktiv ist, wird der Slider auf den Wert
			 * der Position aus dem Bot gesetzt:
			 */
			if (!ypos) {
				yPosSlider.setValue(Math.round(getBot().getPos().y * 100));
			} else {
				/* Ansonsten bestimmt der Slider den Wert beim Bot: */
				getBot().setPos(new Vector3f(getBot().getPos().x, (float) yPosSlider
						.getValue() / 100f, getBot().getPos().z));
			}
		}

		// Hole aktuelle Werte aus dem Bot und setze sie in die Textfelder,
		// aber runde sie vorher:
		Integer xApprox = new Integer(Math.round(Math
				.round(getBot().getPos().x * 100)));
		Integer yApprox = new Integer(Math.round(Math
				.round(getBot().getPos().y * 100)));
		getXPosField().setText(xApprox.toString());
		getYPosField().setText(yApprox.toString());

		// IR-Sensoren:

		if (((CtBot)getBot()).CAP_SENS_IR) {
			/*
			 * Falls Checkbox inaktiv ist, wird der Slider auf den Wert des
			 * Sensors aus dem Bot gesetzt:
			 */
			if (!irL) {
				irSliderL.setValue(((CtBot)getBot()).getSensIrL());
			} else {
				/*
				 * Ansonsten bestimmt der Slider den Wert beim Bot:
				 * (Multiplikation mit 1000, da setSensIr Angabe in Metern
				 * erwartet!)
				 */

				((CtBot)getBot()).setSensIrL((double) irSliderL.getValue() / 1000d);
			}

			/*
			 * Falls Checkbox inaktiv ist, wird der Slider auf den Wert des
			 * Sensors aus dem Bot gesetzt:
			 */
			if (!irR) {
				irSliderR.setValue(((CtBot)getBot()).getSensIrR());
			} else {
				/*
				 * Ansonsten bestimmt der Slider den Wert beim Bot:
				 * (Multiplikation mit 1000, da setSensIr Angabe in Metern
				 * erwartet!)
				 */
				((CtBot)getBot()).setSensIrR((double) irSliderR.getValue() / 1000d);
			}
		}

		// Hole aktuelle Werte aus dem Bot und setze sie in die Textfelder:
		irValueL.setText(Short.toString(((CtBot)getBot()).getSensIrL()));
		irValueR.setText(Short.toString(((CtBot)getBot()).getSensIrR()));

		// TODO: Slider fuer CtBot.CAP_AKT_MOT sind noch nicht vorgesehen!

		// Hole aktuellen Wert aus dem Bot und setzt ihn ins Textfeld:
		motorValueL.setText(Short.toString(((CtBot)getBot()).getActMotL()));
		// Hole aktuellen Wert aus dem Bot und setzt ihn ins Textfeld:
		motorValueR.setText(Short.toString(((CtBot)getBot()).getActMotR()));

		// aktualisiere DeltaX und DeltaY des Maussensors
		msDeltaX.setText(Integer.toString(((CtBot)getBot()).getSensMouseDX()));
		msDeltaY.setText(Integer.toString(((CtBot)getBot()).getSensMouseDY()));

		// aktualisiere die Liniensensoren
		lineL.setText(Integer.toString(((CtBot)getBot()).getSensLineL()));
		lineR.setText(Integer.toString(((CtBot)getBot()).getSensLineR()));

		// aktualisiere die Abgrundsensoren
		borderL.setText(Integer.toString(((CtBot)getBot()).getSensBorderL()));
		borderR.setText(Integer.toString(((CtBot)getBot()).getSensBorderR()));

		// aktualisiere die Lichtsensoren
		ldrL.setText(Integer.toString(((CtBot)getBot()).getSensLdrL()));
		ldrR.setText(Integer.toString(((CtBot)getBot()).getSensLdrR()));

		// Anzeige der Debugdaten des Bots (Display und LEDs).
		lcdLine1.setText(((CtBot)getBot()).getLcdText(0));
		lcdLine2.setText(((CtBot)getBot()).getLcdText(1));
		lcdLine3.setText(((CtBot)getBot()).getLcdText(2));
		lcdLine4.setText(((CtBot)getBot()).getLcdText(3));

		// Hole den Status der LEDs
		String ledStat = Integer.toBinaryString(((CtBot)getBot()).getActLed()); // Integerwert
																	// in eine
																	// binäre
																	// Zahl
																	// umwandel,
																	// als
																	// String
																	// speichern
		if (ledStat.length() <= 8) {
			for (int i = ledStat.length(); i < 8; i++) {
				ledStat = "0" + ledStat;
			}
		} // auf 8 Stellen auffüllen, falls kürzer
		else {
			ledStat = ledStat.substring((ledStat.length() - 8), ledStat
					.length());
		} // falls länger als 8 Stellen -> abschneiden

		for (int i = 7; i >= 0; i--) { // bei jede der 8 LEDs den Status im
										// Simulator aktualisieren
			setLed(8 - i, (int) ledStat.charAt(i) - 48); // die 48 kommt
															// durch die
															// Typenumwandlung
															// zustande
		}

		// Anzeigen der Loggings
		String temp = ((CtBot)getBot()).getLog().toString();
		if (temp.length() > 0) {
			logFrame.setLog(temp);
		}
		
		super.repaint();
		this.repaint();
	}

	/**
	 * Haelt den Bot an der Stelle, auf der er gerade steht, indem die die Werte
	 * fuer die x- und y-Position eingefroren werden.
	 * 
	 */
	public void stopBot() {
		if (((CtBot)getBot()).CAP_POS && !xpos)
			xPosSliderCheck.doClick();
		if (((CtBot)getBot()).CAP_POS && !ypos)
			yPosSliderCheck.doClick();
		if (((CtBot)getBot()).CAP_HEAD && !head)
			headSliderCheck.doClick();
	}

	/**
	 * Aktualiesiert die LEDs
	 * 
	 * @param _led
	 *            welche LED soll geaendert werden (1...8)
	 * @param _state
	 *            an (1) oder aus (0)
	 */
	private void setLed(int _led, int _state) {
		switch (_led) {
		case 1:
			if (_state == 1) {
				led1.setBackground(colLed1Akt);
			} else {
				led1.setBackground(colLed1);
			}
			break;
		case 2:
			if (_state == 1) {
				led2.setBackground(colLed2Akt);
			} else {
				led2.setBackground(colLed2);
			}
			break;
		case 3:
			if (_state == 1) {
				led3.setBackground(colLed3Akt);
			} else {
				led3.setBackground(colLed3);
			}
			break;
		case 4:
			if (_state == 1) {
				led4.setBackground(colLed4Akt);
			} else {
				led4.setBackground(colLed4);
			}
			break;
		case 5:
			if (_state == 1) {
				led5.setBackground(colLed5Akt);
			} else {
				led5.setBackground(colLed5);
			}
			break;

		case 6:
			if (_state == 1) {
				led6.setBackground(colLed6Akt);
			} else {
				led6.setBackground(colLed6);
			}
			break;

		case 7:
			if (_state == 1) {
				led7.setBackground(colLed7Akt);
			} else {
				led7.setBackground(colLed7);
			}
			break;
		case 8:
			if (_state == 1) {
				led8.setBackground(colLed8Akt);
			} else {
				led8.setBackground(colLed8);
			}
			break;
		default:
			break;
		}
	}
	/**
	 * @return Gibt eine Referenz auf headField zurueck
	 */
	public JTextField getHeadField() {
		return headField;
	}

	/**
	 * @param headField
	 *            Referenz auf headField, die gesetzt werden soll
	 */
	public void setHeadField(JTextField headField) {
		this.headField = headField;
	}

	/**
	 * @return Gibt eine Referenz auf xPosField zurueck
	 */
	public JTextField getXPosField() {
		return xPosField;
	}

	/**
	 * @param posField
	 *            Referenz auf xPosField, die gesetzt werden soll
	 */
	public void setXPosField(JTextField posField) {
		xPosField = posField;
	}

	/**
	 * @return Gibt eine Referenz auf yPosField zurueck
	 */
	public JTextField getYPosField() {
		return yPosField;
	}

	/**
	 * @param posField
	 *            Referenz auf yPosField, die gesetzt werden soll
	 */
	public void setYPosField(JTextField posField) {
		yPosField = posField;
	}
}
