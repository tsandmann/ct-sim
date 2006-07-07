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

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import javax.vecmath.Point3d;

import ctSim.SimUtils;
import ctSim.model.bots.components.BotPosition;

/**
 * GUI-Klasse fuer Gruppen von Positionsanzeigern
 * 
 * @author Felix Beckwermert
 *
 * @param <E> Typ der Positionsanzeiger
 */
public class PositionGUI<E extends BotPosition> extends ComponentGroupGUI<E> {
	
	private BotPosition position;
	
	private Vector<String> columns = new Vector<String>();
	
	// private TableModel tabData;
	
	private JSpinner xSpin, ySpin, zSpin, hSpin;
	
	/**
	 * Der Konstruktor
	 * 
	 * @param pos Referenz auf eine Bot-Position, die Zugriff auf den Bot ermoeglicht
	 */
	public PositionGUI(BotPosition pos) {
		
		super(BoxLayout.PAGE_AXIS);
		
		this.position = pos;
	}
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#getSortId()
	 * @Override
	 */
	public int getSortId() {
		
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#initGUI()
	 * 
	 */
	@Override
	public void initGUI() {
				
		this.setBorder(new TitledBorder(new EtchedBorder(), "Position"));
		
		this.columns.add("Koordinate");
		this.columns.add("Wert");
		
		
//		 Tabellendarstellung ohne JSpinner:
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
					
		// Hart gecodete Grenzwerte orientiern sich an den 
		// maximalen Parcoursgroessen aus dem ParcoursGenerator
		// Angepasst an die Welt waere in X-Richtung:
	
		// 		Math.round(-100 * World.getPlaygroundDimX() / 2),
		// 		Math.round(100 * World.getPlaygroundDimX() / 2),
		// aber woher sollte der Bot die Welt kennen 8-)
		
		// TODO: SpinnerModel speichern, nicht Spinner
		// Außerdem: Editor benutzen (Posen * 10(0), damit "vernünftige Koordinaten ohne Komma)
		// Noch besser: Gleich Tabelle mit Editor
		xSpin = new JSpinner(new SpinnerNumberModel(position.getRelPosition().x, -7d, 7d, 0.1d));
		ySpin = new JSpinner(new SpinnerNumberModel(position.getRelPosition().y, -7d, 7d, 0.1d));
		zSpin = new JSpinner(new SpinnerNumberModel(position.getRelPosition().z, -1d, 1d, 0.1d));
		hSpin = new JSpinner(new SpinnerNumberModel((int)Math.round(SimUtils.vec3dToDouble(position.getRelHeading())), -180, 180, 1));
		
		
		// TODO: ChangeListener ändern: werden auch gefeurt, wenn der Bot die GUI ändert
		xSpin.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				double x = new Double(xSpin.getValue().toString());
//				position.setPos(new Point3d(x, position.getRelPosition().y, position.getRelPosition().z));
			}
		});				
		
		ySpin.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				
				double y = new Double(ySpin.getValue().toString());
//				position.setPos(new Point3d(position.getRelPosition().x, y, position.getRelPosition().z));
			}
		});				
		
		zSpin.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				
				double z = new Double(zSpin.getValue().toString());
//				position.setPos(new Point3d(position.getRelPosition().x, position.getRelPosition().y, z));
			}
		});				
		
		hSpin.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				
				Double x = new Double(hSpin.getValue().toString());
//				position.setHead(SimUtils.intToVec3d(x.intValue()));
			}
		});				
		
		// TODO: Irgendwie kommen die Werte noch nicht durch....
		
		tab.add(new JLabel(" X-Position"));
		tab.add(xSpin);
		tab.add(new JLabel(" Y-Position"));
		tab.add(ySpin);
		tab.add(new JLabel(" Z-Position"));
		tab.add(zSpin);
		tab.add(new JLabel(" Blickrichtung"));
		tab.add(hSpin);
		
		JScrollPane scroll = new JScrollPane(tab);
		
		scroll.setMinimumSize(tab.getPreferredSize());
		
		this.add(scroll);
	}
	
	/* (non-Javadoc)
	 * @see ctSim.view.ComponentGroupGUI#updateGUI()
	 * 
	 */
	@Override
	public void updateGUI() {
		
// Tabellendarstellung ohne JSpinner:
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
