package ctSim.view;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import ctSim.SimUtils;
import ctSim.model.bots.components.BotPosition;

public class PositionGUI<E extends BotPosition> extends ComponentGroupGUI<E> {
	
	private BotPosition position;
	
	private Vector<String> columns = new Vector<String>();
	
	private TableModel tabData;
	
	public PositionGUI(BotPosition pos) {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.position = pos;
	}
	
	@Override
	public int getSortId() {
		
		return 0;
	}

	@Override
	public void initGUI() {
		
		this.setBorder(new TitledBorder(new EtchedBorder(), "Position/Heading"));
		
		this.columns.add("Koordinate");
		this.columns.add("Wert");
		
		this.tabData = new DefaultTableModel(columns, 4) {
			
			public boolean isCellEditable(int row, int col) {
				
				if(col == 0)
					return false;
				return true;
			}
		};
		
		this.tabData.setValueAt("X:", 0, 0);
		this.tabData.setValueAt("Y:", 1, 0);
		this.tabData.setValueAt("Z:", 2, 0);
		this.tabData.setValueAt("H:", 3, 0);
		
		JTable tab = new JTable(this.tabData);
		
		JScrollPane scroll = new JScrollPane(tab);
		
		scroll.setMinimumSize(tab.getPreferredSize());
		
		this.add(scroll);
	}

	@Override
	public void updateGUI() {
		
		this.tabData.setValueAt(this.position.getRelPosition().x, 0, 1);
		this.tabData.setValueAt(this.position.getRelPosition().y, 1, 1);
		this.tabData.setValueAt(this.position.getRelPosition().z, 2, 1);
		this.tabData.setValueAt(String.format("%.2f", SimUtils.getRotation(this.position.getRelHeading())), 3, 1);
	}
}
