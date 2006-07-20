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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ctSim.model.bots.ctbot.components.RemoteControlSensor;
import ctSim.view.Debug;

/**
 * Klasse fuer grafische Anzeige von Fernbedienungen
 * @author Peter Koenig
 */
public class RemoteControlGroupGUI extends SensorGroupGUI<RemoteControlSensor>
		implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private JButton showRemoteControl;
	private RemoteControlGUI remoteControlGUI;
	private RemoteControlSensor remoteControlSensor;
	
	/**
	 * @see ctSim.view.ComponentGroupGUI#getSortId()
	 */
	@Override
	public int getSortId() {
		
		return 1000;
	}

	/**
	 * @see ctSim.view.ComponentGroupGUI#initGUI()
	 */
	@Override
	public void initGUI() {
		
		Set<RemoteControlSensor> sens = this.getAllSensors();
		
		// TODO: Was wenn keine (eigentlich nicht moeglich)?
		if(sens.size() != 1) {
			
			Debug.out.println("Fehler: Mehrere Fernbedienungen werden von der GUI nicht unterst�tzt!"); //$NON-NLS-1$
		}
		
		Iterator<RemoteControlSensor> it = sens.iterator();
		
		this.remoteControlSensor = it.next();
		
		this.showRemoteControl = new JButton("-> Fernbedienung <-"); //$NON-NLS-1$
		this.showRemoteControl.addActionListener(this);
		
		this.add(this.showRemoteControl);
	}

	/**
	 * @see ctSim.view.ComponentGroupGUI#updateGUI()
	 */
	@Override
	public void updateGUI() {
		
		// NOTHING TO DO...
	}

	/** 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(@SuppressWarnings("unused")
	ActionEvent e) {
		
		if(this.remoteControlGUI == null)
			this.remoteControlGUI = new RemoteControlGUI(this.remoteControlSensor, "Hier BotName");  // TODO:  <----------------------- //$NON-NLS-1$
		
		this.remoteControlGUI.setVisible(true);
	}
}
	
	class RemoteControlGUI extends JDialog {
		
		private static final long serialVersionUID = 1L;
		
		/** RC5-Taste PWR*/
		public static final int RC5_CODE_PWR = 0x100C; 

		// Zahlentasten:
		
		/** RC5-Taste 0*/
		public static final int RC5_CODE_0 = 0x1000; 
		// TODO:  Ist das Taste 10?
		
		/** RC5-Taste 1*/
		public static final int RC5_CODE_1 = 0x1001; 

		/** RC5-Taste 2*/
		public static final int RC5_CODE_2 = 0x1002; 

		/** RC5-Taste 3*/
		public static final int RC5_CODE_3 = 0x1003; 

		/** RC5-Taste 4*/
		public static final int RC5_CODE_4 = 0x1004; 

		/** RC5-Taste 5*/
		public static final int RC5_CODE_5 = 0x1005; 

		/** RC5-Taste 6*/
		public static final int RC5_CODE_6 = 0x1006; 

		/** RC5-Taste 7*/
		public static final int RC5_CODE_7 = 0x1007; 

		/** RC5-Taste 8*/
		public static final int RC5_CODE_8 = 0x1008; 

		/** RC5-Taste 9*/
		public static final int RC5_CODE_9 = 0x1009; 

		/** RC5-Taste 11*/
		public static final int RC5_CODE_11 = 0x100A; 

		/** RC5-Taste 12*/
		public static final int RC5_CODE_12 = 0x100B; 
		// TODO: Achtung, Bot definiert den noch (faelschlicherweise?) als 0x1003!
		
		// Farbtasten
		
		/** RC5-Taste RED*/
		public static final int RC5_CODE_RED = 0x101E; 

		/** RC5-Taste GREEN*/
		public static final int RC5_CODE_GREEN = 0x101D; 

		/** RC5-Taste YELLOW*/
		public static final int RC5_CODE_YELLOW = 0x1027; 

		/** RC5-Taste BLUE*/
		public static final int RC5_CODE_BLUE = 0x101C; 

		
		// Sondertasten:
		
		/** RC5-Taste I/II*/
		public static final int RC5_CODE_I_II=0x1023;
		/** RC5-Taste TV/VCR*/
		public static final int  RC5_CODE_TV_VCR=0x1038;
		/** RC5-Taste mit dem Punkt*/
		public static final int  RC5_CODE_DOT=0x1037;
		/** RC5-Taste CH*P/C*/
		public static final int  RC5_CODE_CH_PC=0x100B;

		
		// Steuertasten:
		
		/** RC5-Taste Play*/
		public static final int  RC5_CODE_PLAY=0x11B5;
		/** RC5-Taste Pause*/
		public static final int  RC5_CODE_STILL=0x1029;
		/** RC5-Taste Stop*/
		public static final int  RC5_CODE_STOP=0x1036;
		/** RC5-Taste <<*/
		public static final int   RC5_CODE_BWD=0x1032;
		/** RC5-Taste >>*/
		public static final int   RC5_CODE_FWD=0x1034;

		/** RC5-Taste Mute*/
		public static final int   RC5_CODE_MUTE=0x003F;

		/** RC5-Taste Vol+*/
		public static final int   RC5_VOL_PLUS=0x1010;
		/** RC5-Taste Vol-*/
		public static final int   RC5_VOL_MINUS=0x1011;

		/** RC5-Taste CH+*/
		public static final int   RC5_CH_PLUS=0x1020;
		/** RC5-Taste CH-*/
		public static final int   RC5_CH_MINUS=0x1021;		
		
		private RemoteControlSensor remCtrl;
		private Map<String,Integer> commandMappings;
		
		private Box box;
		
		RemoteControlGUI(RemoteControlSensor rc, String title) {
			
			this.remCtrl = rc;
			
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
		
		@SuppressWarnings("boxing") void initCommandMap() {
			
			this.commandMappings = new LinkedHashMap<String,Integer>();
			
			this.commandMappings.put("PWR", RC5_CODE_PWR); //$NON-NLS-1$
			
			this.commandMappings.put("1", RC5_CODE_1); //$NON-NLS-1$
			this.commandMappings.put("2", RC5_CODE_2); //$NON-NLS-1$
			this.commandMappings.put("3", RC5_CODE_3); //$NON-NLS-1$
			this.commandMappings.put("4", RC5_CODE_4); //$NON-NLS-1$
			this.commandMappings.put("5", RC5_CODE_5); //$NON-NLS-1$
			this.commandMappings.put("6", RC5_CODE_6); //$NON-NLS-1$
			this.commandMappings.put("7", RC5_CODE_7); //$NON-NLS-1$
			this.commandMappings.put("8", RC5_CODE_8); //$NON-NLS-1$
			this.commandMappings.put("9", RC5_CODE_9); //$NON-NLS-1$
			this.commandMappings.put("10", RC5_CODE_0); //$NON-NLS-1$
			this.commandMappings.put("11", RC5_CODE_11); //$NON-NLS-1$
			this.commandMappings.put("12", RC5_CODE_12); //$NON-NLS-1$
			
			this.commandMappings.put("Gruen", RC5_CODE_GREEN); //$NON-NLS-1$
			this.commandMappings.put("Rot", RC5_CODE_RED); //$NON-NLS-1$
			this.commandMappings.put("Gelb", RC5_CODE_YELLOW); //$NON-NLS-1$
			this.commandMappings.put("Blau", RC5_CODE_BLUE); //$NON-NLS-1$

			this.commandMappings.put("I/II", RC5_CODE_I_II); //$NON-NLS-1$
			this.commandMappings.put("||", RC5_CODE_STILL); //$NON-NLS-1$
			this.commandMappings.put("TV/VCR", RC5_CODE_TV_VCR); //$NON-NLS-1$

			this.commandMappings.put("<<", RC5_CODE_BWD); //$NON-NLS-1$
			this.commandMappings.put(">", RC5_CODE_PLAY); //$NON-NLS-1$
			this.commandMappings.put(">>", RC5_CODE_FWD); //$NON-NLS-1$

			this.commandMappings.put("�", RC5_CODE_DOT); //$NON-NLS-1$
			this.commandMappings.put("#", RC5_CODE_STOP); //$NON-NLS-1$
			this.commandMappings.put("CH*P/P", RC5_CODE_CH_PC); //$NON-NLS-1$

			this.commandMappings.put("Vol+", RC5_VOL_PLUS); //$NON-NLS-1$
			this.commandMappings.put("Mute", RC5_CODE_MUTE); //$NON-NLS-1$
			this.commandMappings.put("Ch+", RC5_CH_PLUS); //$NON-NLS-1$

			this.commandMappings.put("Vol-", RC5_VOL_MINUS); //$NON-NLS-1$
			this.commandMappings.put("Ch-", RC5_CH_MINUS); //$NON-NLS-1$

		}
		
		void initButtons() {
			
			Iterator<Entry<String, Integer>> it = this.commandMappings.entrySet().iterator();
			
			// POWER-BUT:
			JPanel pow = new JPanel(new GridLayout(1, 3));
			pow.add(new JLabel());
			pow.add(new JLabel());
			pow.add(createButton(it.next()));
			this.box.add(pow);

			
			// NUMBER-BUT:
			JPanel nums = new JPanel(new GridLayout(4, 3));
			for(int i=0; i<12; i++) {
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
			for(int i=0; i<9; i++) {
				arrows.add(createButton(it.next()));
			}
			this.box.add(arrows);

			// VOLUME-BUT:
			JPanel vols = new JPanel(new GridLayout(2, 3));
			for(int i=0; i<4; i++) {
				vols.add(createButton(it.next()));
			}
			vols.add(new JLabel());
			vols.add(createButton(it.next()));

			this.box.add(vols);
		
		}
		
		JButton createButton(Entry<String, Integer> ent) {
			
			final Integer val = ent.getValue();
			
			JButton but = new JButton(ent.getKey());
			but.addActionListener(new ActionListener() {
				
				@SuppressWarnings("synthetic-access")
				public void actionPerformed(@SuppressWarnings("unused")
				ActionEvent e) {
					
					RemoteControlGUI.this.remCtrl.setValue(val);
					//RemoteControlGroupGUI.this.remoteControlSensor.setValue(val);
				}
			});
			
			return but;
		}
	}
	