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
package ctSim.view.gui;

import java.util.EventObject;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
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

	private static final long serialVersionUID = 1L;

	private BotPosition position;

	private Vector<String> columns = new Vector<String>();

	private TableModel tabData;

	private boolean noupdate = false;

	//private JSpinner xSpin, ySpin, zSpin, hSpin;

	/**
	 * Der Konstruktor
	 *
	 * @param pos Referenz auf eine Bot-Position, die Zugriff auf den Bot ermoeglicht
	 */
	public PositionGUI(BotPosition pos) {

		super(BoxLayout.PAGE_AXIS);

		this.position = pos;
	}

	/**
	 * @see ctSim.view.gui.ComponentGroupGUI#getSortId()
	 *
	 */
	@Override
	public int getSortId() {

		return 10;
	}

	/**
	 * @see ctSim.view.gui.ComponentGroupGUI#initGUI()
	 *
	 */
	@Override
	public void initGUI() {

		this.setBorder(new TitledBorder(new EtchedBorder(), "Position")); //$NON-NLS-1$

		this.columns.add("Koordinate"); //$NON-NLS-1$
		this.columns.add("Wert"); //$NON-NLS-1$


		// Tabellendarstellung ohne JSpinner:
		this.tabData = new DefaultTableModel(this.columns, 4) {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isCellEditable(@SuppressWarnings("unused") int row, int col) {

				if(col == 0)
					return false;
				return true;
			}
		};

		this.tabData.setValueAt("X-Position [m]:", 0, 0); //$NON-NLS-1$
		this.tabData.setValueAt("Y-Position [m]:", 1, 0); //$NON-NLS-1$
		this.tabData.setValueAt("Z-Position [m]:", 2, 0); //$NON-NLS-1$
		this.tabData.setValueAt("Blickrichtung [Grad]:", 3, 0); //$NON-NLS-1$

		this.tabData.addTableModelListener(new TableModelListener() {

			@SuppressWarnings({"synthetic-access","boxing"})
			public void tableChanged(TableModelEvent e) {

				int column = e.getColumn();

				if(column != 1 || PositionGUI.this.noupdate)
		        	return;

//				System.out.println("soweit so gut");

		        int row = e.getFirstRow();
		        TableModel model = (TableModel)e.getSource();

		        if(model.getValueAt(row, column) == null)
		        	return;

		        double val = (Double)model.getValueAt(row, column);
		        //val /= 1000;

		        switch(row) {
		        case 0:
		        	PositionGUI.this.position.setPos(
		        			new Point3d(
		        					val,
		        					PositionGUI.this.position.getRelPosition().y,
		        					PositionGUI.this.position.getRelPosition().z));
		        	break;
		        case 1:
		        	PositionGUI.this.position.setPos(
		        			new Point3d(
		        					PositionGUI.this.position.getRelPosition().x,
		        					val,
		        					PositionGUI.this.position.getRelPosition().z));
		        	break;
		        case 2:
		        	PositionGUI.this.position.setPos(
		        			new Point3d(
		        					PositionGUI.this.position.getRelPosition().x,
		        					PositionGUI.this.position.getRelPosition().y,
		        					val));
		        	break;
		        case 3:
		        	PositionGUI.this.position.setHead(
		        		(Double)model.getValueAt(row, column));
		        	break;
		        }
			}
		});

 		JTable tab = new JTable(this.tabData);

 		tab.getColumnModel().getColumn(1).setCellEditor(new SpinnerCellEditor());
 		tab.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public void setValue(Object value) {

 		        setText((value == null) ? "" : String.format("%.2f", value)); //$NON-NLS-1$ //$NON-NLS-2$
 		    }
 		});

		JScrollPane scroll = new JScrollPane(tab);

		scroll.setMinimumSize(tab.getPreferredSize());

		this.add(scroll);
	}

	/**
	 * @see ctSim.view.gui.ComponentGroupGUI#updateGUI()
	 *
	 */
	@SuppressWarnings("boxing")
	@Override
	public void updateGUI() {

		this.noupdate = true;

		// Tabellendarstellung ohne JSpinner:
		this.tabData.setValueAt(this.position.getRelPosition().x, 0, 1);
		this.tabData.setValueAt(this.position.getRelPosition().y, 1, 1);
		this.tabData.setValueAt(this.position.getRelPosition().z, 2, 1);
		this.tabData.setValueAt(SimUtils.vec3dToDouble(this.position.getRelHeading()), 3, 1);

		this.noupdate = false;
	}

	private class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {

		protected int clickCountToStart = 2;

		private static final long serialVersionUID = 1L;
		private JSpinner spinner;
		private SpinnerNumberModel model;

		private ChangeListener last;

		SpinnerCellEditor() {

			this.model = new SpinnerNumberModel(0d, -180d, 1000d, 0.1d);

			this.spinner = new JSpinner(this.model);

			this.spinner.setEditor(new JSpinner.NumberEditor(this.spinner, "0.00")); //$NON-NLS-1$
		}

		public boolean isCellEditable(EventObject anEvent) {
			// Copied from the source of "DefaultCellEditor" by java.sun.com:
		    if (anEvent instanceof MouseEvent) {
		    	return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
		    }
		    return true;
		}

		public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

		protected void fireEditingStopped() {
			// TODO: Ein bisl dirty, aber naja...
			PositionGUI.this.noupdate = true;
			super.fireEditingStopped();
//			for(int i=0; i<this.getCellEditorListeners().length; i++)
//				System.out.println(this.getCellEditorListeners()[i].getClass());
			PositionGUI.this.noupdate = false;
		}

		/**
		 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
		 */
		@SuppressWarnings("boxing")
		public Component getTableCellEditorComponent(@SuppressWarnings("unused") JTable table, Object value, @SuppressWarnings("unused") boolean isSelected, int row, @SuppressWarnings("unused") int column) {

			////////////////////////////////////////////////////////////
			// Je nach Zeile, passende Werte initialiesieren:
			if(row == 3) {
				this.model.setMinimum(-180d);
				this.model.setMaximum(180d);
				this.model.setStepSize(1.0d);
			} else if(row == 2) {
				this.model.setMinimum(0d);
				this.model.setMaximum(1000d);
				this.model.setStepSize(0.01d);
			} else {
				this.model.setMinimum(0d);
				this.model.setMaximum(1000d);
				this.model.setStepSize(0.12d);
			}

			// this.spinner.setEditor(table.getCellRenderer(row, column));

			////////////////////////////////////////////////////////////
			// ChangeListener um direkt Wert-Änderungen in der Tabelle anzuzeigen:
			final JTable tab = table;
			final int r = row;
			final int c = column;

			if(last != null)
				this.spinner.removeChangeListener(last);
			this.last = new ChangeListener() {

				public void stateChanged(ChangeEvent e) {

					tab.setValueAt(spinner.getValue(), r, c);
				}
			};
			this.spinner.addChangeListener(last);

			////////////////////////////////////////////////////////////
			// Aktuellen Wert setzen:
			if(value != null)
				this.model.setValue(value);
			else
				this.model.setValue(0d);

			return this.spinner;
		}

		/**
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		public Object getCellEditorValue() {

			return this.model.getValue();
		}
	}
}