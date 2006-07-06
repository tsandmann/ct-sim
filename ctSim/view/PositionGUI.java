package ctSim.view;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JSpinner;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SpinnerNumberModel;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeListener;

import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import javax.vecmath.Point3d;

import ctSim.SimUtils;
import ctSim.model.bots.components.BotPosition;

public class PositionGUI<E extends BotPosition> extends ComponentGroupGUI<E> {
	
	private BotPosition position;
	
	private Vector<String> columns = new Vector<String>();
	
	private TableModel tabData;
	
	private JSpinner xSpin, ySpin, zSpin, hSpin;
	
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
		
		// Panel mit GridLayout: links 4 Labels mit X,Y,Z,H; rechts die Spinner, die dann aufrufen:
		//this.position.setPos(  -- Point3d --  );
		//this.position.setHead(  --  Vector3d  --  );
		// Am besten im Pause-Modus testen (da die Werte ansonsten -- vielleicht -- nicht genommen werden
		// (aber hoffentlich im Pause-Modus))
		
		this.setBorder(new TitledBorder(new EtchedBorder(), "Position"));
		
		this.columns.add("Koordinate");
		this.columns.add("Wert");
		
		
//		this.tabData = new DefaultTableModel(columns, 4) {
//			
//			public boolean isCellEditable(int row, int col) {
//				
//				if(col == 0)
//					return false;
//				return true;
//			}
//		};
//		
//		this.tabData.setValueAt("X:", 0, 0);
//		this.tabData.setValueAt("Y:", 1, 0);
//		this.tabData.setValueAt("Z:", 2, 0);
//		this.tabData.setValueAt("H:", 3, 0);
//		
// 			JTable tab = new JTable(this.tabData);

		JPanel tab = new JPanel(new GridLayout(4,2));
					
		// TODO: Vernuenftige Grenzwerte einbauen
		// Fuer X:
		// 		Math.round(-100 * World.getPlaygroundDimX() / 2),
		// 		Math.round(100 * World.getPlaygroundDimX() / 2),
				
		xSpin = new JSpinner(new SpinnerNumberModel(position.getRelPosition().x, -3d, 3d, 0.1d));
		ySpin = new JSpinner(new SpinnerNumberModel(position.getRelPosition().y, -3d, 3d, 0.1d));
		zSpin = new JSpinner(new SpinnerNumberModel(position.getRelPosition().z, -1d, 1d, 0.1d));
		hSpin = new JSpinner(new SpinnerNumberModel(SimUtils.vec3dToDouble(position.getRelHeading()), -180, 180, 1));
		
		xSpin.addChangeListener(new JSpinner.NumberEditor(xSpin) {
			public void actionPerformed(ActionEvent evt) {
				double x = new Double(xSpin.getValue().toString());
				position.setPos(new Point3d(x, position.getRelPosition().y, position.getRelPosition().z));
			}
		});				

		ySpin.addChangeListener(new JSpinner.NumberEditor(ySpin) {
			public void actionPerformed(ActionEvent evt) {
				double y = new Double(ySpin.getValue().toString());
				position.setPos(new Point3d(position.getRelPosition().x, y, position.getRelPosition().z));
			}
		});				

		zSpin.addChangeListener(new JSpinner.NumberEditor(zSpin) {
			public void actionPerformed(ActionEvent evt) {
				double z = new Double(zSpin.getValue().toString());
				position.setPos(new Point3d(position.getRelPosition().x, position.getRelPosition().y, z));
			}
		});				

		hSpin.addChangeListener(new JSpinner.NumberEditor(xSpin) {
			public void actionPerformed(ActionEvent evt) {
				int x = new Integer(hSpin.getValue().toString());
				position.setHead(SimUtils.intToVec3d(x));
			}
		});				

		// TODO: Irgendwie kommen die Werte noch nicht durch....
		
		tab.add(new JLabel("X-Position"));
		tab.add(xSpin);
		tab.add(new JLabel("Y-Position"));
		tab.add(ySpin);
		tab.add(new JLabel("Z-Position"));
		tab.add(zSpin);
		tab.add(new JLabel("Blickrichtung"));
		tab.add(hSpin);
		
		JScrollPane scroll = new JScrollPane(tab);
		
		scroll.setMinimumSize(tab.getPreferredSize());
		
		this.add(scroll);
	}

	@Override
	public void updateGUI() {
		
//		this.tabData.setValueAt(this.position.getRelPosition().x, 0, 1);
//		this.tabData.setValueAt(this.position.getRelPosition().y, 1, 1);
//		this.tabData.setValueAt(this.position.getRelPosition().z, 2, 1);
//		this.tabData.setValueAt(String.format("%.2f", SimUtils.getRotation(this.position.getRelHeading())), 3, 1);

	this.xSpin.setValue(this.position.getRelPosition().x);
	this.ySpin.setValue(this.position.getRelPosition().y);
	this.zSpin.setValue(this.position.getRelPosition().z);
	this.hSpin.setValue(SimUtils.vec3dToDouble(this.position.getRelHeading()));
	
	}
}
