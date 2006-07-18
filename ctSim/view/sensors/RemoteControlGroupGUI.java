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
package ctSim.view.sensors;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ctSim.model.bots.ctbot.components.RemoteControlSensor;
import ctSim.view.Debug;

/**
 * Klasse fuer grafische Anzeige von Fernbedienungen
 * @author Peter Koenig
 */
public class RemoteControlGroupGUI extends SensorGroupGUI<RemoteControlSensor>
		implements ActionListener {
	
	private JButton showRemoteControl;
	private RemoteControlGUI remoteControlGUI;
	private RemoteControlSensor remoteControlSensor;
	
	@Override
	public int getSortId() {
		
		return 1000;
	}

	@Override
	public void initGUI() {
		
		Set<RemoteControlSensor> sens = this.getAllSensors();
		
		// TODO: Was wenn keine (eigentlich nicht möglich)?
		if(sens.size() != 1) {
			
			Debug.out.println("Fehler: Mehrere Fernbedienungen werden von der GUI nicht unterstützt!");
		}
		
		Iterator<RemoteControlSensor> it = sens.iterator();
		
		this.remoteControlSensor = it.next();
		
		this.showRemoteControl = new JButton("-> Fernbedienung <-");
		this.showRemoteControl.addActionListener(this);
		
		this.add(this.showRemoteControl);
	}

	@Override
	public void updateGUI() {
		
		// NOTHING TO DO...
	}

	public void actionPerformed(ActionEvent e) {
		
		if(this.remoteControlGUI == null)
			this.remoteControlGUI = new RemoteControlGUI(this.remoteControlSensor, "Hier BotName");  // TODO:  <-----------------------
		
		this.remoteControlGUI.setVisible(true);
	}
}
	
	class RemoteControlGUI extends JDialog {
		
		/** RC5-Taste 0*/
		public static final int RC5_CODE_0 = 0x3940; 

		/** RC5-Taste 1*/
		public static final int RC5_CODE_1 = 0x3941; 

		/** RC5-Taste 2*/
		public static final int RC5_CODE_2 = 0x3942; 

		/** RC5-Taste 3*/
		public static final int RC5_CODE_3 = 0x3943; 

		/** RC5-Taste 4*/
		public static final int RC5_CODE_4 = 0x3944; 

		/** RC5-Taste 5*/
		public static final int RC5_CODE_5 = 0x3945; 

		/** RC5-Taste 6*/
		public static final int RC5_CODE_6 = 0x3946; 

		/** RC5-Taste 7*/
		public static final int RC5_CODE_7 = 0x3947; 

		/** RC5-Taste 8*/
		public static final int RC5_CODE_8 = 0x3948; 

		/** RC5-Taste 9*/
		public static final int RC5_CODE_9 = 0x3949; 

		/** RC5-Taste Up*/
		public static final int RC5_CODE_UP = 0x2950; 

		/** RC5-Taste down*/
		public static final int RC5_CODE_DOWN = 0x2951; 

		/** RC5-Taste left*/
		public static final int RC5_CODE_LEFT = 0x2955; 

		/** RC5-Taste right*/
		public static final int RC5_CODE_RIGHT = 0x2956;

		/** RC5-Taste PWR*/
		public static final int RC5_CODE_PWR = 0x394C; 

		/** RC5-Taste RED*/
		public static final int RC5_CODE_RED = 0x100B; 

		/** RC5-Taste GREEN*/
		public static final int RC5_CODE_GREEN = 0x102E; 

		/** RC5-Taste YELLOW*/
		public static final int RC5_CODE_YELLOW = 0x1038; 

		/** RC5-Taste BLUE*/
		public static final int RC5_CODE_BLUE = 0x1029; 
		
		/** RC5-Taste CODE VIEW */
		public static final int RC5_CODE_VIEW = 0x000F; 
		
		
		private RemoteControlSensor remCtrl;
		private Map<String,Integer> commandMappings;
		
		private Box box;
		
		RemoteControlGUI(RemoteControlSensor remCtrl, String title) {
			
			this.remCtrl = remCtrl;
			
			this.box = new Box(BoxLayout.PAGE_AXIS);
			//this.box.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			this.add(this.box);
			
			this.setTitle(title);
			this.setModal(false);
			this.setResizable(false);
			
			this.setDefaultCloseOperation(HIDE_ON_CLOSE);
			
			initCommandMap();
			initButtons();
			
			this.pack();
		}
		
		void initCommandMap() {
			
			this.commandMappings = new LinkedHashMap();
			
			this.commandMappings.put("PWR", RC5_CODE_PWR);
			
			this.commandMappings.put("1", RC5_CODE_1);
			this.commandMappings.put("2", RC5_CODE_2);
			this.commandMappings.put("3", RC5_CODE_3);
			this.commandMappings.put("4", RC5_CODE_4);
			this.commandMappings.put("5", RC5_CODE_5);
			this.commandMappings.put("6", RC5_CODE_6);
			this.commandMappings.put("7", RC5_CODE_7);
			this.commandMappings.put("8", RC5_CODE_8);
			this.commandMappings.put("9", RC5_CODE_9);
			this.commandMappings.put("0", RC5_CODE_0);
			
			this.commandMappings.put("Grün", RC5_CODE_GREEN);
			this.commandMappings.put("Rot", RC5_CODE_RED);
			this.commandMappings.put("Gelb", RC5_CODE_YELLOW);
			this.commandMappings.put("Blau", RC5_CODE_BLUE);
			
			this.commandMappings.put("Up", RC5_CODE_UP);
			this.commandMappings.put("Left", RC5_CODE_LEFT);
			this.commandMappings.put("View?", RC5_CODE_VIEW);
			this.commandMappings.put("Right", RC5_CODE_RIGHT);
			this.commandMappings.put("Down", RC5_CODE_DOWN);
		}
		
		void initButtons() {
			
			Iterator<Entry<String, Integer>> it = this.commandMappings.entrySet().iterator();
			
			// POWER-BUT:
			this.box.add(createButton(it.next()));
			
			// NUMBER-BUT:
			JPanel nums = new JPanel(new GridLayout(4, 3));
			for(int i=0; i<10; i++) {
				nums.add(createButton(it.next()));
			}
			this.box.add(nums);
			
			// COLOR-BUT:
			JPanel cols = new JPanel(new GridLayout(1, 4));
			for(int i=0; i<4; i++) {
				cols.add(createButton(it.next()));
			}
			this.box.add(cols);
			
			// ARROW-BUT:
			JPanel arrows = new JPanel(new GridLayout(3, 3));
			arrows.add(new JLabel());
			arrows.add(createButton(it.next()));
			arrows.add(new JLabel());
			arrows.add(createButton(it.next()));
			arrows.add(createButton(it.next()));
			arrows.add(createButton(it.next()));
			arrows.add(new JLabel());
			arrows.add(createButton(it.next()));
			this.box.add(arrows);
		}
		
		JButton createButton(Entry<String, Integer> ent) {
			
			final Integer val = ent.getValue();
			
			JButton but = new JButton(ent.getKey());
			but.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					
					RemoteControlGUI.this.remCtrl.setValue(val);
					//RemoteControlGroupGUI.this.remoteControlSensor.setValue(val);
				}
			});
			
			return but;
		}
	}
	
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//	private List<JPanel> keyboards;
//	
//	private JPanel dummy6, dummy5,dummy4,dummy3,dummy8,keyPanel,rc5Panel,colKeyPanel;
//
//	private JButton jButtonDown,jButtonRight,jButtonOnOff,jButtonView;
//	private JButton jButtonLeft;
//	private JButton jButtonUp;
//	private JButton jButton10;
//	private JButton jButton9;
//	private JButton jButton8;
//	private JButton jButton7;
//	private JButton jButton2;
//	private JButton jButton6;
//	private JButton jButton5;
//	private JButton jButton4;
//	private JButton jButton3;
//	private JButton jButton1;
//	private JButton jButtonRed;
//	private JButton jButtonGreen;
//	private JButton jButtonYellow;
//	private JButton jButtonBlue;
//
//	
//	/** 
//	 * @see ctSim.view.ComponentGroupGUI#initGUI()	
//	 * 
//	 */
//	@Override
//	public void initGUI() {
//		
//		this.setBorder(new TitledBorder(new EtchedBorder(), "Fernbedienung")); //$NON-NLS-1$
//		
//		JTabbedPane tab = new JTabbedPane();
//		
//		Set<RemoteControlSensor> rcs = this.getAllSensors();
//		
//		this.keyboards = new ArrayList<JPanel>(rcs.size());
//		
//		Iterator<RemoteControlSensor> it = rcs.iterator();
//		while(it.hasNext()) {
//			
//			RemoteControlSensor rc = it.next();
//			
//			JPanel keys = new JPanel();
//			
//			this.addKeys(keys);
//						
//			keys.setBorder(new EtchedBorder());
//			
//			JScrollPane scroll = new JScrollPane(keys); //, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//			
//			tab.addTab(rc.getName(), scroll); // + Desc. (?)
//						
//			this.keyboards.add(keys);
//		}
//		
//		this.add(tab);
//	}
//
//	private void addKeys(JPanel keyboard){
//		// Tastenfeld einfuegen
//			// keyPanel.setLayout(new BoxLayout(keyPanel, BoxLayout.Y_AXIS));
//
//			// Zahlen- und Pfeiltasten
//			GridLayout rc5PanelLayout = new GridLayout(1, 1);
//			rc5PanelLayout.setHgap(5);
//			rc5PanelLayout.setVgap(5);
//			rc5PanelLayout.setColumns(3);
//			rc5PanelLayout.setRows(7);
//			keyboard.setLayout(rc5PanelLayout);
//// 			keyboard.setPreferredSize(new java.awt.Dimension(200, 200));
//			{
//				jButton1 = new JButton();
//				rc5Panel.add(jButton1);
//				jButton1.setText("1");
//				jButton1.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_1);
//					}
//				});
//			}
//			{
//				jButton2 = new JButton();
//				rc5Panel.add(jButton2);
//				jButton2.setText("2");
//				jButton2.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//						//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_2);
//					}
//				});
//			}
//			{
//				jButton3 = new JButton();
//				rc5Panel.add(jButton3);
//				jButton3.setText("3");
//				jButton3.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//						//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_3);
//					}
//				});
//			}
//			{
//				jButton4 = new JButton();
//				rc5Panel.add(jButton4);
//				jButton4.setText("4");
//				jButton4.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//						//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_4);
//					}
//				});
//			}
//			{
//				jButton5 = new JButton();
//				rc5Panel.add(jButton5);
//				jButton5.setText("5");
//				jButton5.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//						//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_5);
//					}
//				});
//			}
//			{
//				jButton6 = new JButton();
//				rc5Panel.add(jButton6);
//				jButton6.setText("6");
//				jButton6.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//						//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_6);
//					}
//				});
//			}
//			{
//				jButton7 = new JButton();
//				rc5Panel.add(jButton7);
//				jButton7.setText("7");
//				jButton7.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_7);
//					}
//				});
//			}
//			{
//				jButton8 = new JButton();
//				rc5Panel.add(jButton8);
//				jButton8.setText("8");
//				jButton8.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//						//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_8);
//					}
//				});
//			}
//			{
//				jButton9 = new JButton();
//				rc5Panel.add(jButton9);
//				jButton9.setText("9");
//				jButton9.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_9);
//					}
//				});
//			}
//			{
//				jButtonOnOff = new JButton();
//				rc5Panel.add(jButtonOnOff);
//				jButtonOnOff.setText("ON / OFF");
//				jButtonOnOff.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_PWR);
//					}
//				});
//			}
//			{
//				jButton10 = new JButton();
//				rc5Panel.add(jButton10);
//				jButton10.setText("0");
//				jButton10.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_0);
//					}
//				});
//			}
//			{
//				jButtonView = new JButton();
//				rc5Panel.add(jButtonView);
//				jButtonView.setText("View");
//				jButtonView.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_VIEW);
//					}
//				});
//			}
///*					{
//				dummy2 = new JPanel();
//				rc5Panel.add(dummy2);
//			}
//			*/
//			{
//				dummy3 = new JPanel();
//				rc5Panel.add(dummy3);
//			}
//			{
//				jButtonUp = new JButton();
//				rc5Panel.add(jButtonUp);
//				jButtonUp.setText("UP");
//				jButtonUp.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_UP);
//					}
//				});
//			}
//			{
//				dummy4 = new JPanel();
//				rc5Panel.add(dummy4);
//			}
//			{
//				jButtonLeft = new JButton();
//				rc5Panel.add(jButtonLeft);
//				jButtonLeft.setText("LEFT");
//				jButtonLeft.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_LEFT);
//					}
//				});
//			}
//			{
//				dummy8 = new JPanel();
//				rc5Panel.add(dummy8);
//			}
//			{
//				jButtonRight = new JButton();
//				rc5Panel.add(jButtonRight);
//				jButtonRight.setText("RIGHT");
//				jButtonRight.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_RIGHT);
//					}
//				});
//			}
//			{
//				dummy5 = new JPanel();
//				rc5Panel.add(dummy5);
//			}
//			{
//				jButtonDown = new JButton();
//				rc5Panel.add(jButtonDown);
//				jButtonDown.setText("DOWN");
//				jButtonDown.addActionListener(new ActionListener() {
//					public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_DOWN);
//					}
//				});
//			}
//			{
//				dummy6 = new JPanel();
//				rc5Panel.add(dummy6);
//			}
//
//			// Farb-Tastenfeld einfuegen
//			colKeyPanel = new JPanel();
//			keyPanel.add(colKeyPanel);
//			colKeyPanel.setLayout(new GridLayout(1, 4, 5, 5));
//			{
//				{
//					jButtonRed = new JButton();
//					colKeyPanel.add(jButtonRed);
//					jButtonRed.setBackground(new Color(255, 0, 0));
//					jButtonRed.addActionListener(new ActionListener() {
//						public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//					//		((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_RED);
//						}
//					});
//				}
//				{
//					jButtonGreen = new JButton();
//					colKeyPanel.add(jButtonGreen);
//					jButtonGreen.setBackground(new Color(0, 255, 0));
//					jButtonGreen
//							.addActionListener(new ActionListener() {
//								public void actionPerformed(
//										@SuppressWarnings("unused") ActionEvent evt) {
//								//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_GREEN);
//								}
//							});
//				}
//				{
//					jButtonYellow = new JButton();
//					colKeyPanel.add(jButtonYellow);
//					jButtonYellow.setBackground(new Color(255, 255, 0));
//					jButtonYellow
//							.addActionListener(new ActionListener() {
//								public void actionPerformed(
//										@SuppressWarnings("unused") ActionEvent evt) {
//								//	((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_YELLOW);
//								}
//							});
//				}
//				{
//					jButtonBlue = new JButton();
//					colKeyPanel.add(jButtonBlue);
//					jButtonBlue.setBackground(new Color(0, 0, 255));
//					jButtonBlue.addActionListener(new ActionListener() {
//						public void actionPerformed(@SuppressWarnings("unused") ActionEvent evt) {
//							//((CtBot)getBot()).setSensRc5(CtBot.RC5_CODE_BLUE);
//						}
//					});
//				}
//			} // Ende des Farb-Tastenfeldes
//		} // Ende des Tastenfeldes		
//	
//	
//	/**
//	 * @see ctSim.view.ComponentGroupGUI#updateGUI()
//	 * 	
//	 */
//	@Override
//	public void updateGUI() {
//		
//		Iterator<RemoteControlSensor> it = this.getAllSensors().iterator();
//		for(int i=0; it.hasNext(); i++) {
//			
//			RemoteControlSensor log = it.next();
//			
//		}
//	}
//
//	/**
//	 * @see ctSim.view.ComponentGroupGUI#getSortId()
//	 * 	
//	 */
//	@Override
//	public int getSortId() {
//		return 100;
//	}
//}
