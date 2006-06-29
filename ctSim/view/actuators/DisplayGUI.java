package ctSim.view.actuators;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ctSim.model.bots.components.actuators.Display;

public class DisplayGUI extends ActuatorGroupGUI<Display> {
	
	private List<JTextArea> lcds;
	
	@Override
	public void initGUI() {
		
		this.setBorder(new TitledBorder(new EtchedBorder(), "Displays"));
		
		//JTabbedPane tab = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		JTabbedPane tab = new JTabbedPane();
		
		Set<Display> acts = this.getAllActuators();
		
		this.lcds = new ArrayList<JTextArea>(acts.size());
		
		Iterator<Display> it = acts.iterator();
		while(it.hasNext()) {
			
			JTextArea lcd = new JTextArea(4, 20);
			lcd.setBorder(new EtchedBorder());
			lcd.setFont(new Font("Monospaced", Font.BOLD, 12));
			lcd.setBackground(new Color(120, 150, 90));
			//lcd.setPreferredSize(new Dimension(145, 85));
			//lcd.setMaximumSize(new Dimension(150, 90));
			tab.addTab(it.next().getName(), lcd); // + Desc. (?)
			this.lcds.add(lcd);
		}
		
		this.add(tab);
	}

	@Override
	public void updateGUI() {
		
		Iterator<Display> it = this.getAllActuators().iterator();
		for(int i=0; it.hasNext(); i++) {
			
			this.lcds.get(i).setText(it.next().getValue());
		}
	}
}
