package ctSim.view.actuators;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ctSim.model.bots.components.actuators.Display;
import ctSim.model.bots.components.actuators.LogScreen;

public class LogScreenGUI extends ActuatorGroupGUI<LogScreen> {
	
	private List<JTextArea> lcds;
	
	@Override
	public void initGUI() {
		
		this.setBorder(new TitledBorder(new EtchedBorder(), "LogScreens"));
		
		//JTabbedPane tab = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		JTabbedPane tab = new JTabbedPane();
		
		Set<LogScreen> acts = this.getAllActuators();
		
		this.lcds = new ArrayList<JTextArea>(acts.size());
		
		Iterator<LogScreen> it = acts.iterator();
		while(it.hasNext()) {
			
			LogScreen log = it.next();
			
			// Größe?? Unterscheiden zw. LogScreen u. Disp.???
			JTextArea lcd = new JTextArea(4, 20);
			lcd.setBorder(new EtchedBorder());
			lcd.setEditable(false);
			if(log.getFont()!=null)
				lcd.setFont(log.getFont());
			if(log.getBackgroundColor()!=null)
				lcd.setBackground(log.getBackgroundColor());
			
			JScrollPane scroll = new JScrollPane(lcd); //, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			
			tab.addTab(log.getName(), scroll); // + Desc. (?)
			
			//scroll.setMinimumSize(lcd.getPreferredSize());
			scroll.setMinimumSize(lcd.getPreferredScrollableViewportSize());
			
			this.lcds.add(lcd);
		}
		
		this.add(tab);
	}

	@Override
	public void updateGUI() {
		
		Iterator<LogScreen> it = this.getAllActuators().iterator();
		for(int i=0; it.hasNext(); i++) {
			
			LogScreen log = it.next();
			
			if(log.hasToRewrite()) {
				this.lcds.get(i).setText(log.getValue());
			} else {
				this.lcds.get(i).append(log.getValue());
			}
		}
	}

	@Override
	public int getSortId() {
		// TODO Auto-generated method stub
		return 80;
	}
}
