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

import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import ctSim.model.bots.components.sensors.SimpleSensor;

/**
 * GUI fuer eine Gruppe einfacher Sensoren
 * @author Felix Beckwermert
 * @param <E> Typ der Sensoren
 */
public class SimpleSensorGroupGUI<E extends SimpleSensor> extends SensorGroupGUI<E> {
	
private Vector<String> columns = new Vector<String>();
	
	private TableModel tabData;
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#initGUI()
	 */
	@Override
	public void initGUI() {
		
		this.setBorder(new TitledBorder(new EtchedBorder(), "Sensoren"));
		
		//System.out.println("Simple: "+this.getAllSensors().size());
		this.columns.add("Sensor");
		this.columns.add("Wert");
		
		this.tabData = new DefaultTableModel(columns, this.getAllSensors().size());
		
		Iterator<E> it = this.getAllSensors().iterator();
		for(int i=0; it.hasNext(); i++) {
			this.tabData.setValueAt(it.next().getName(), i, 0);
		}
		
		JTable tab = new JTable(this.tabData);
		
		JScrollPane scroll = new JScrollPane(tab);
		
		scroll.setMinimumSize(tab.getPreferredSize());
		
		this.add(scroll);
	}
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#updateGUI()
	 */
	@Override
	public void updateGUI() {
		
		//System.out.println("Simple: "+this.getAllSensors().size());
		
		Set<E> set = this.getAllSensors();
		
		//Vector<Number> data = new Vector<Number>(set.size());
		
		Iterator<E> it = set.iterator();
		
		for(int i=0; it.hasNext(); i++) {
			// TODO: Hier casten???
			//data.add((Number)it.next().getValue());
			// TODO: ...
			this.tabData.setValueAt(it.next().getValue(), i, 1);
		}
	}

	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#getSortId()
	 */
	@Override
	public int getSortId() {
		return 20;
	}
}
