/*
 * c't-Sim - Robotersimulator f�r den c't-Bot
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

import java.awt.GridLayout;
import java.awt.event.*;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import javax.vecmath.Vector3f;

import ctSim.Model.CtBot;
import ctSim.Model.World;
import ctSim.SimUtils;

/**
 * Ergaenzt das generische, abstrakte ControlPanel um die richtigen Felder 
 * und Regler fuer c't-Bots  
 * @author pek (pek@heise.de)
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

	// Zu welchem Bot gehoert das Panel?
	private CtBot bot;

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

	private Dimension labelDim, fieldDim;
	
	private boolean irL, irR, xpos, ypos, head;

	/**
	 * Der Konstruktor
	 * @param bot Eine Referenz auf den Bot, zu dem das Panel gehoert
	 */
	public CtControlPanel(CtBot bot) {
		super(bot);
		this.bot = bot;
		irL = false;
		irR = false;
		xpos = false;
		ypos = false;
		head = false;

		initGUI();
	}

	/* (non-Javadoc)
	 * Startet GUI 
	 * @see ctSim.View.ControlPanel#initGUI()
	 */
	protected void initGUI() {

		labelDim = new Dimension(70, 25);
		fieldDim = new Dimension(50, 25);
		
		try {
			BoxLayout thisLayout = new BoxLayout(this,
					javax.swing.BoxLayout.Y_AXIS);
			this.setLayout(thisLayout);
			singlesPanel = new JPanel();
			this.add(singlesPanel);
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
					headPanel.add(super.getHeadField());
					super.getHeadField().setText("0");
					super.getHeadField().setEditable(false);
				}
			}

			if (bot.CAP_HEAD) {
				{
					headSliderPanel = new JPanel();
					singlesPanel.add(headSliderPanel);
					BoxLayout headSliderPanelLayout = new BoxLayout(
							headSliderPanel, javax.swing.BoxLayout.Y_AXIS);
					headSliderPanel.setLayout(headSliderPanelLayout);
					{
						headSlider = new JSlider(JSlider.HORIZONTAL, -180, 180,
								0);
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

					xPosPanel.add(super.getXPosField());
					super.getXPosField().setEditable(false);
					super.getXPosField().setText("0");
				}

			}
			if (bot.CAP_POS) {
				{
					xPosSliderPanel = new JPanel();
					singlesPanel.add(xPosSliderPanel);
					BoxLayout xPosSliderPanelLayout = new BoxLayout(
							xPosSliderPanel, javax.swing.BoxLayout.Y_AXIS);
					xPosSliderPanel.setLayout(xPosSliderPanelLayout);
					{
						xPosSlider = new JSlider(JSlider.HORIZONTAL, Math.round(-100*World.PLAYGROUND_WIDTH) , Math.round(100*World.PLAYGROUND_WIDTH), 0);
						xPosSliderPanel.add(xPosSlider);
						xPosSlider.setMajorTickSpacing(20);
						xPosSlider.setMinorTickSpacing(10);
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
					yPosPanel.add(super.getYPosField());
					super.getYPosField().setText("0");
					super.getYPosField().setEditable(false);
				}
			}

			if (bot.CAP_POS) {
				{
					yPosSliderPanel = new JPanel();
					singlesPanel.add(yPosSliderPanel);
					BoxLayout yPosSliderPanelLayout = new BoxLayout(
							yPosSliderPanel, javax.swing.BoxLayout.Y_AXIS);
					yPosSliderPanel.setLayout(yPosSliderPanelLayout);
					{
						yPosSlider = new JSlider(JSlider.HORIZONTAL, Math.round(-100*World.PLAYGROUND_HEIGHT), Math.round(100*World.PLAYGROUND_HEIGHT), 0);
						yPosSliderPanel.add(yPosSlider);
						yPosSlider.setMajorTickSpacing(20);
						yPosSlider.setMinorTickSpacing(10);
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

			{
				multiplesPanel = new JPanel();
				this.add(multiplesPanel);
				BoxLayout multiplesPanelLayout = new BoxLayout(multiplesPanel,
						javax.swing.BoxLayout.X_AXIS);
				multiplesPanel.setLayout(multiplesPanelLayout);
			}

			keyPanel = new JPanel();
			super.add(keyPanel);
			{
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
							bot.setSensRc5(RC5_CODE_1);
						}
					});
				}
				{
					jButton2 = new JButton();
					rc5Panel.add(jButton2);
					jButton2.setText("2");
					jButton2.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_2);
						}
					});

				}
				{
					jButton3 = new JButton();
					rc5Panel.add(jButton3);
					jButton3.setText("3");
					jButton3.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_3);
						}
					});

				}
				{
					jButton4 = new JButton();
					rc5Panel.add(jButton4);
					jButton4.setText("4");
					jButton4.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_4);
						}
					});
				}
				{
					jButton5 = new JButton();
					rc5Panel.add(jButton5);
					jButton5.setText("5");
					jButton5.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_5);
						}
					});

				}
				{
					jButton6 = new JButton();
					rc5Panel.add(jButton6);
					jButton6.setText("6");
					jButton6.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_6);
						}
					});

				}
				{
					jButton7 = new JButton();
					rc5Panel.add(jButton7);
					jButton7.setText("7");
					jButton7.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_7);
						}
					});

				}
				{
					jButton8 = new JButton();
					rc5Panel.add(jButton8);
					jButton8.setText("8");
					jButton8.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_8);
						}
					});

				}
				{
					jButton9 = new JButton();
					rc5Panel.add(jButton9);
					jButton9.setText("9");
					jButton9.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_9);
						}
					});

				}
				{
					jButtonOnOff = new JButton();
					rc5Panel.add(jButtonOnOff);
					jButtonOnOff.setText("ON / OFF");
					jButtonOnOff.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_PWR);
						}
					});

				}
				{
					jButton10 = new JButton();
					rc5Panel.add(jButton10);
					jButton10.setText("0");
					jButton10.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							bot.setSensRc5(RC5_CODE_0);
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
							bot.setSensRc5(RC5_CODE_UP);
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
							bot.setSensRc5(RC5_CODE_LEFT);
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
							bot.setSensRc5(RC5_CODE_RIGHT);
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
							bot.setSensRc5(RC5_CODE_DOWN);
						}
					});

				}
				{
					dummy6 = new JPanel();
					rc5Panel.add(dummy6);
				}
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
					motorLabelL
							.setPreferredSize(labelDim);
				}
				{
					motorValueL = new JTextField();
					motPanelL.add(motorValueL);
					motorValueL.setText("0");
					motorValueL.setEditable(false);
					motorValueL
							.setPreferredSize(fieldDim);
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

			if (bot.CAP_SENS_IR) {
				{
					leftIRSliderPanel = new JPanel();
					leftPanel.add(leftIRSliderPanel);
					BoxLayout leftIRSliderPanelLayout = new BoxLayout(
							leftIRSliderPanel, javax.swing.BoxLayout.X_AXIS);
					leftIRSliderPanel.setLayout(leftIRSliderPanelLayout);
					{
						irSliderL = new JSlider(JSlider.VERTICAL, 0,
								1000, 0);
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
						leftIRSliderCheck.addItemListener(new ItemListener() {
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
						irLabelR
								.setPreferredSize(labelDim);
					}
					{
						irValueR = new JTextField();
						irPanelR.add(irValueR);
						irValueR.setText("0");
						irValueR.setEditable(false);
						irValueR.setPreferredSize(fieldDim);
					}
				}

				if (bot.CAP_SENS_IR) {

					{
						rightIRSliderPanel = new JPanel();
						BoxLayout rightIRSliderPanelLayout = new BoxLayout(
								rightIRSliderPanel,
								javax.swing.BoxLayout.X_AXIS);
						rightIRSliderPanel.setLayout(rightIRSliderPanelLayout);
						rightPanel.add(rightIRSliderPanel);
						{
							irSliderR = new JSlider(JSlider.VERTICAL,
									0, 1000, 0);
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

		if (bot.CAP_HEAD) {
			/*
			 * Falls Checkbox inaktiv ist, wird der Slider auf den Wert des
			 * Headings aus dem Bot gesetzt:
			 */
			if (!head) {
				headSlider.setValue(Math.round(Math.round(SimUtils.vec3fToDouble(bot.getHeading()))));
			} else {
				/* Ansonsten bestimmt der Slider den Wert beim Bot: */
				bot.setHeading(SimUtils.intToVec3f(headSlider.getValue()));
			}
		}
		// Hole aktuellen Wert aus dem Bot und setzt ihn ins Textfeld:
		super.getHeadField().setText(SimUtils.vec3fToString(bot.getHeading()));
		
		// Position:

		if (bot.CAP_POS) {
			/*
			 * Falls Checkbox fuer X inaktiv ist, wird der Slider auf den Wert
			 * der Position aus dem Bot gesetzt:
			 */
			if (!xpos) {
				xPosSlider.setValue(Math.round(bot.getPos().x * 100));
			} else {
				/* Ansonsten bestimmt der Slider den Wert bei Bot und Panel: */
				bot.setPos(new Vector3f((float)xPosSlider.getValue() / 100f, bot
						.getPos().y, bot.getPos().z));
			}

			/*
			 * Falls Checkbox fuer Y inaktiv ist, wird der Slider auf den Wert
			 * der Position aus dem Bot gesetzt:
			 */
			if (!ypos) {
				yPosSlider.setValue(Math.round(bot.getPos().y * 100));
			} else {
				/* Ansonsten bestimmt der Slider den Wert beim Bot: */
				bot.setPos(new Vector3f(bot.getPos().x,
						(float)yPosSlider.getValue() / 100f, bot.getPos().z));
			}
		}

		// Hole aktuelle Werte aus dem Bot und setze sie in die Textfelder, 
		// aber runde sie vorher:
		Integer xApprox = new Integer(Math.round(Math.round(bot.getPos().x * 100)));
		Integer yApprox = new Integer(Math.round(Math.round(bot.getPos().y * 100)));
		super.getXPosField().setText(xApprox.toString());
		super.getYPosField().setText(yApprox.toString());
		
		// IR-Sensoren:

		if (bot.CAP_SENS_IR) {
			/* Falls Checkbox inaktiv ist, wird der Slider auf den Wert des
			 * Sensors aus dem Bot gesetzt: */
			if (!irL) {
				irSliderL.setValue(bot.getSensIrL());
			} else {
				/* Ansonsten bestimmt der Slider den Wert beim Bot: 				 
				 * (Multiplikation mit 1000, da setSensIr Angabe in Metern erwartet!) */

				bot.setSensIrL((double)irSliderL.getValue()/1000d);
			}

			/* Falls Checkbox inaktiv ist, wird der Slider auf den Wert des
			 * Sensors aus dem Bot gesetzt: */
			if (!irR) {
				irSliderR.setValue( bot.getSensIrR());
			} else {
				/* Ansonsten bestimmt der Slider den Wert beim Bot:
				 * (Multiplikation mit 1000, da setSensIr Angabe in Metern erwartet!) */
				bot.setSensIrR((double)irSliderR.getValue()/1000d);
			}
		}

		// Hole aktuelle Werte aus dem Bot und setze sie in die Textfelder:
		irValueL.setText(Short.toString(bot.getSensIrL()));
		irValueR.setText(Short.toString(bot.getSensIrR()));

		// TODO: Slider fuer CtBot.CAP_AKT_MOT sind noch nicht vorgesehen!

		// Hole aktuellen Wert aus dem Bot und setzt ihn ins Textfeld:
		motorValueL.setText(Short.toString(bot.getAktMotL()));
		// Hole aktuellen Wert aus dem Bot und setzt ihn ins Textfeld:
		motorValueR.setText(Short.toString(bot.getAktMotR()));

		super.repaint();
		this.repaint();
	}
}
