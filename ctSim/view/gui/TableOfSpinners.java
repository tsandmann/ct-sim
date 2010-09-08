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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import ctSim.model.bots.components.BotComponent;

/**
 * Tabellen fuer Komponenten
 */
public abstract class TableOfSpinners extends GuiBotBuisitor {
	/**
	 * UID
	 */
	private static final long serialVersionUID = -4717996663435641535L;

	/**
	 * Komponenteneditor
	 */
	public static class CompntEditor extends AbstractCellEditor
	implements TableCellEditor {
		/** UID */
		private static final long serialVersionUID = 4073894569366140421L;

		/** letzte aktive Komponente */
		private Component lastActive = null;

		/**
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		public Object getCellEditorValue() {
			return lastActive;
		}

		/**
		 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
		 */
		public Component getTableCellEditorComponent(JTable table,
		Object value, boolean isSelected, int row, int column) {
			lastActive = (Component)value;
			return lastActive;
		}
	}

	/**
	 * Komponenten-Renderer
	 */
	public static class CompntRenderer implements TableCellRenderer {
		/**
		 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {
			return (Component)value;
		}
	}

    /**
     * Bot-Komponenten-Tabelle
     */
    public static class BotComponentTableModel extends DefaultTableModel {
    	/** UID */
    	private static final long serialVersionUID = 3066982780978636288L;

    	/**
    	 * Neue Tabelle fuer Bot-Komponenten
    	 */
    	public BotComponentTableModel() {
    		super(0, 2);
    	}

		/**
		 * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
		 */
		@Override
    	public boolean isCellEditable(int row, int column) {
			if (column == 0)
				return false;
			else
				return ((JSpinner)getValueAt(row, column)).isEnabled();
    	}

		/**
		 * Fuegt eine neue Zeile hinzu
		 * @param label			Text
		 * @param toolTip		Tooltip
		 * @param editable		editierbar?
		 * @param sModel		Spinner
		 * @param decimalFormat	Format
		 */
		public void addRow(String label, String toolTip, boolean editable,
		SpinnerModel sModel, String decimalFormat) {
			JSpinner spinner = new JSpinner(sModel);
			if (decimalFormat != null) {
				JSpinner.NumberEditor e = new JSpinner.NumberEditor(spinner, decimalFormat);
				/* Werte nicht grau anzeigen, falls nicht editierbar */
				e.getTextField().setDisabledTextColor(UIManager.getColor("TextField.foreground"));
				spinner.setEditor(e);
			}
			// setEditor() setzt den Font auf Courier, keine Ahnung warum
			spinner.setFont(Font.decode("SansSerif"));
			spinner.setEnabled(editable);
			spinner.setBorder(null);
			JLabel la = new JLabel(label);
			la.setBorder(null);
			la.setToolTipText(toolTip);
			spinner.setToolTipText(toolTip);
			addRow(new Object[] {la, spinner});

			// Events aus dem SpinnerModel sollen Neumalen der Tabelle ausloesen
			final int thisRow = getRowCount() - 1;
			sModel.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					fireTableCellUpdated(thisRow, 1);
				}
			});
		}

		/**
		 * @param c Bot-Komponente
		 */
		public void addRow(BotComponent<? extends SpinnerModel> c) {
			addRow(c.getName(), c.getDescription(), c.isGuiEditable(),
				c.getExternalModel(), null);
		}

		/**
		 * @param c Bot-Komponente
		 * @param decimalFormat Format
		 */
		public void addRow(BotComponent<? extends SpinnerModel> c,
		String decimalFormat) {
			addRow(c.getName(), c.getDescription(), c.isGuiEditable(),
				c.getExternalModel(), decimalFormat);
		}
    }

    /** Modell */
    protected BotComponentTableModel model = new BotComponentTableModel();

    /**
     * @see ctSim.view.gui.GuiBotBuisitor#shouldBeDisplayed()
     */
    @Override
    public boolean shouldBeDisplayed() {
    	return model.getRowCount() > 0;
    }

    /**
     * Tabellen fuer Komponenten
     */
    public TableOfSpinners() {
        final JTable t = new JTable(model);
        t.setRowHeight(new JSpinner().getMinimumSize().height +
        	t.getRowMargin());
        t.setTableHeader(null);

        TableCellRenderer renderer = new CompntRenderer();
        t.getColumnModel().getColumn(0).setCellRenderer(renderer);
        t.getColumnModel().getColumn(1).setCellRenderer(renderer);
        t.getColumnModel().getColumn(1).setCellEditor(new CompntEditor());

        setBorder(new TitledBorder(getPanelTitle()));

        /*
		 * Schweisstreibend: Hab t.setBorder() mit "lowered bevel border"
		 * versucht, aber das fuehrte zu doofen Ergebnissen; JTable kann
		 * offenbar keine Borders. -> Loesung: JTable in ScrollPane einwickeln.
		 * Dann wird die ScrollPane aber sehr gross, warum weiss ich nicht. ->
		 * Loesung: getMin/Pref/MaxSize() sollen zur Table weiterleiten. Dann
		 * ist aber in der ScrollPane nicht genug Platz fuer ihren
		 * Lowered-Bevel-Border _und_ die Table, das heisst (Tusch) ein
		 * Scrollbalken erscheint. -> Loesung: Insets dazurechnen.
		 */
        add(new JScrollPane(t) {
			private static final long serialVersionUID = 6362442061290466520L;

			@Override
        	public Dimension getMinimumSize() {
        		return t.getMinimumSize();
        	}

        	@Override
        	public Dimension getPreferredSize() {
        		Insets i = getInsets();
        		return new Dimension(
        			t.getPreferredSize().width  + i.left + i.right,
        			t.getPreferredSize().height + i.top  + i.bottom);
        	}

        	@Override
        	public Dimension getMaximumSize() {
        		return t.getMaximumSize();
        	}
        });
    }

    /**
     * @return Panel-Titel
     */
    protected abstract String getPanelTitle();
}
