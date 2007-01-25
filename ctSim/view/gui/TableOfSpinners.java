package ctSim.view.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import ctSim.model.bots.components.BotComponent;

//$$ doc
public abstract class TableOfSpinners extends BotBuisitor {
	public static class ComponentCellEditor extends AbstractCellEditor
	implements TableCellEditor {
		private static final long serialVersionUID = 4073894569366140421L;

		private Component lastActive = null;

		public Object getCellEditorValue() {
            return lastActive;
        }

		@SuppressWarnings("unused")
		public Component getTableCellEditorComponent(JTable table,
			Object value, boolean isSelected, int row, int column) {

	        lastActive = (Component)value;
	        return lastActive;
        }
    }

	public static class ComponentCellRenderer implements TableCellRenderer {
		@SuppressWarnings("unused")
		public Component getTableCellRendererComponent(JTable table,
			Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

            return (Component)value;
        }
    }

    public static class BotComponentTableModel extends DefaultTableModel {
    	private static final long serialVersionUID = 3066982780978636288L;

    	public BotComponentTableModel() {
    		super(0, 2);
    	}

		@Override
    	public boolean isCellEditable(int row, int column) {
			if (column == 0)
				return false;
			else
				return ((JSpinner)getValueAt(row, column)).isEnabled();
    	}

		public void addRow(String label, String toolTip, boolean editable,
			SpinnerModel sModel) {

			JLabel la = new JLabel(label);
			la.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
			JSpinner s = new JSpinner(sModel);
			s.setEnabled(editable);
			s.setBorder(null);
			la.setToolTipText(toolTip);
			s.setToolTipText(toolTip);
			addRow(new Object[] {la, s});

			// Events aus dem SpinnerModel sollen Neumalen der Tabelle ausloesen
			final int thisRow = getRowCount() - 1;
			sModel.addChangeListener(new ChangeListener() {
				public void stateChanged(
				@SuppressWarnings("unused") ChangeEvent e) {
					fireTableCellUpdated(thisRow, 1);
				}
			});
		}

		public void addBotComponent(BotComponent<? extends SpinnerModel> c) {
			addRow(c.getName(), c.getDescription(), c.isGuiEditable(),
				c.getExternalModel());
		}
    }

    protected BotComponentTableModel model = new BotComponentTableModel();

    @Override
    public boolean shouldBeDisplayed() {
    	return model.getRowCount() > 0;
    }

    public TableOfSpinners() {
        final JTable t = new JTable(model);
        t.setRowHeight(new JSpinner().getMinimumSize().height);
        t.setTableHeader(null);

        TableCellRenderer renderer = new ComponentCellRenderer();
        t.getColumnModel().getColumn(0).setCellRenderer(renderer);
        t.getColumnModel().getColumn(1).setCellRenderer(renderer);
        t.getColumnModel().getColumn(1).setCellEditor(
        	new ComponentCellEditor());

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

    protected abstract String getPanelTitle();
}
