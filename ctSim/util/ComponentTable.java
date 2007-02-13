package ctSim.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

//$$ doc
public class ComponentTable extends JTable {
	private static final long serialVersionUID = 2766695602066190632L;

	// Konstruktoren wie in der Superklasse ///////////////////////////////////

	public ComponentTable() {
		super();
		init();
	}

	public ComponentTable(int numRows, int numColumns) {
		super(numRows, numColumns);
		init();
	}

	public ComponentTable(Object[][] rowData, Object[] columnNames) {
		super(rowData, columnNames);
		init();
	}

	public ComponentTable(TableModel dm, TableColumnModel cm,
	ListSelectionModel sm) {
		super(dm, cm, sm);
		init();
	}

	public ComponentTable(TableModel dm, TableColumnModel cm) {
		super(dm, cm);
		init();
	}

	public ComponentTable(TableModel dm) {
		super(dm);
		init();
	}

	public ComponentTable(Vector rowData, Vector columnNames) {
		super(rowData, columnNames);
		init();
	}

	private void init() {
		setDefaultRenderer(Object.class, new CellRenderer());
		setDefaultEditor(Object.class, new CellEditor());
	}

	protected Component configure(JComponent compnt, JTable table,
	boolean isSelected) {
		Color bg = isSelected ? table.getSelectionBackground()
                              : table.getBackground();
		Color fg = isSelected ? table.getSelectionForeground()
		                      : table.getForeground();
		compnt.setBackground(bg);
		compnt.setForeground(fg);
		return compnt;
	}

	/* $$$ doc
	 * In der Tabelle: Falls das hinzugefuegte Ding breiter ist als die
	 * Spalte, dann diese Spalte verbreitern; Falls das hinzugefuegte
	 * Ding hoeher ist als die Zeile, dann alle (!) Zeilen hoeher machen
	 * (unterschiedlich hohe Zeilen werden von JTable nicht unterstuetzt
	 * soweit ich weiss und saehen sowieso doof aus)
	 */
	public void accomodateContent() {
		int maxHeight = 0;
		int maxWidth = 0;
		for (int i = 0; i < getColumnCount(); i++) {
			for (int j = 0; j < getRowCount(); j++) {
				maxHeight = Math.max(maxHeight,
					((Component)getValueAt(j, i)).getPreferredSize().height);
				maxWidth = Math.max(maxWidth,
					((Component)getValueAt(j, i)).getPreferredSize().width);
			}
			// Spaltenbreite setzen (1x pro Spalte)
			getColumnModel().getColumn(i).setMinWidth(
				maxWidth + getIntercellSpacing().width);
			maxWidth = 0;
		}
		// Zeilenbreite setzen (1x fuer ganze Tabelle)
		setRowHeight(maxHeight + getIntercellSpacing().height);
	}

	// Workaround fuer Bug "horizontale Scrollbars" ///////////////////////////

	// Bug: Eine JTable in einer JScrollPane loest nie die horizontale Scrollbar
	// aus -- siehe http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4127936
	// Hier der Workaround wie auf der Bug-Seite beschrieben

	// when the viewport shrinks below the preferred size, stop tracking the
	// viewport width
	@Override
	public boolean getScrollableTracksViewportWidth() {
		if (autoResizeMode != AUTO_RESIZE_OFF) {
			if (getParent() instanceof JViewport)
				return getParent().getWidth() > getPreferredSize().width;
		}
		return false;
	}

	// when the viewport shrinks below the preferred size, return the minimum
	// size so that scrollbars will be shown
	@Override
	public Dimension getPreferredSize() {
		if (getParent() instanceof JViewport) {
			if (getParent().getWidth() < super.getPreferredSize().width)
				return getMinimumSize();
		}
		return super.getPreferredSize();
	}

	// Hilfsklassen ///////////////////////////////////////////////////////////

	class CellRenderer implements TableCellRenderer {
		@SuppressWarnings("unused")
		public Component getTableCellRendererComponent(JTable table,
		Object value, boolean isSelected, boolean hasFocus, int row,
		int column) {
			return configure((JComponent)value, table, isSelected);
	    }
	}

	class CellEditor extends AbstractCellEditor implements TableCellEditor {
		private static final long serialVersionUID = 4073894569366140421L;

		private Component lastActive = null;

		public Object getCellEditorValue() {
			return lastActive;
		}

		@SuppressWarnings("unused")
		public Component getTableCellEditorComponent(JTable table,
		Object value, boolean isSelected, int row, int column) {
			lastActive = configure((JComponent)value, table, true);
			return lastActive;
		}
	}
}
