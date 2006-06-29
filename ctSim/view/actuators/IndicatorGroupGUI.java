package ctSim.view.actuators;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import ctSim.model.bots.components.actuators.Indicator;

public class IndicatorGroupGUI extends ActuatorGroupGUI<Indicator> {
	
	private ArrayList<JPanel> leds;
	
	@Override
	public void initGUI() {
		
		//System.out.println("Indicator: "+this.getAllActuators().size());
		this.setBorder(new TitledBorder(new EtchedBorder(), "Indikatoren"));
		
		Set<Indicator> acts = this.getAllActuators();
		
		this.leds = new ArrayList<JPanel>(acts.size());
		
		JPanel ledPanel = new JPanel(new GridLayout(1, acts.size(), 5, 5));
		
		Iterator<Indicator> it = acts.iterator();
		while(it.hasNext()) {
			JPanel led = new JPanel();
			// TODO: siehe Indicator
			led.setBackground(it.next().getColor());
			led.setMinimumSize(new Dimension(15, 15));
			//led.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
			led.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			this.leds.add(led);
			ledPanel.add(led);
		}
		
		this.add(ledPanel);
	}
	
	@Override
	public void updateGUI() {
		// TODO
		
		//System.out.println("Indicator: "+this.getAllActuators().size());
		Iterator<Indicator> it = this.getAllActuators().iterator();
		for(int i=0; it.hasNext(); i++) {
			// TODO: siehe Indicator: getColor
			this.leds.get(i).setBackground(it.next().getColor());
		}
	}

	@Override
	public int getSortId() {
		// TODO Auto-generated method stub
		return 0;
	}
}