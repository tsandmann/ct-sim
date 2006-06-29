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

public class SimpleSensorGroupGUI<E extends SimpleSensor> extends SensorGroupGUI<E> {
	
private Vector<String> columns = new Vector<String>();
	
	private TableModel tabData;
	
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

	@Override
	public int getSortId() {
		// TODO Auto-generated method stub
		return 20;
	}
}
