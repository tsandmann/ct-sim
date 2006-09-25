package ctSim.view.contestConductor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class TournamentTreeGuiTest extends JFrame {
	public class TournamentTreeModel implements TreeModel {
		ArrayList<TreeModelListener> listeners =
			new ArrayList<TreeModelListener>();
		int currentNodePayload;
		TournamentTree<Integer> carrier;

		public TournamentTreeModel() {
			init();
		}

		public void init() {
			currentNodePayload = 1;
			carrier = new TournamentTree<Integer>(currentNodePayload++);
			addNode();
		}

        public void addNode() {
			carrier = carrier.add(currentNodePayload++);
			for (TreeModelListener l : listeners)
				l.treeStructureChanged(new TreeModelEvent(this,
					new TreePath(carrier)));
        }

        public void removeNode() {
        	int u = currentNodePayload - 4;
        	init();
        	for (int i = 0; i < u; i++)
        		addNode();
        }

		public void addTreeModelListener(TreeModelListener l) {
	        listeners.add(l);
        }

		public void removeTreeModelListener(TreeModelListener l) {
			listeners.remove(l);
		}

		public int getChildCount(Object parent) {
			return isLeaf(parent) ? 0 : 2;
		}

		public Object getChild(Object parent, int index) {
			if (index < 0 || index > 1)
				throw new IllegalArgumentException();
	        return index == 0
	        	? ((TournamentTree)parent).left
	        	: ((TournamentTree)parent).right;
        }

		public int getIndexOfChild(Object parent, Object child) {
	        throw new UnsupportedOperationException();
        }

		public Object getRoot() {
	        return carrier;
        }

		public boolean isLeaf(Object node) {
	        return ((TournamentTree)node).payload != null;
        }

		public void valueForPathChanged(TreePath path, Object newValue) {
	        throw new UnsupportedOperationException();
        }
	}

    private static final long serialVersionUID = - 4828458620913240203L;
    TournamentTreeModel tm = new TournamentTreeModel();
    JTree t = new JTree(tm);
    JLabel label = new JLabel("2 Dinger");

    public TournamentTreeGuiTest() {
    	setLayout(new BorderLayout());
    	JButton her = new JButton("+");
    	her.addActionListener(new ActionListener() {
            public void actionPerformed(@SuppressWarnings("unused")
				ActionEvent e) {
	            tm.addNode();
	            for (int i = 0; i < t.getRowCount(); i++)
	            	t.expandRow(i);
	            label.setText(tm.currentNodePayload - 1 + " Dinger");
            }
    	});
    	JButton weg = new JButton("\u2013");
    	weg.addActionListener(new ActionListener() {
    		public void actionPerformed(@SuppressWarnings("unused")
    			ActionEvent e) {
    			tm.removeNode();
    			for (int i = 0; i < t.getRowCount(); i++)
    				t.expandRow(i);
    			label.setText(tm.currentNodePayload - 1 + " Dinger");
    		}
    	});
    	JPanel p = new JPanel(new BorderLayout());
    	p.setBorder(new EmptyBorder(3, 3, 3, 3));
    	JPanel buttons = new JPanel();
    	buttons.add(her);
    	buttons.add(weg);
    	p.add(buttons, BorderLayout.WEST);
    	p.add(label, BorderLayout.EAST);
    	add(p, BorderLayout.NORTH);
    	add(new JScrollPane(t), BorderLayout.CENTER);
    	setTitle("TournamentTree");
    	setSize(200, 600);
    	setLocationRelativeTo(null);
    	setVisible(true);
    	setLocation(getLocationOnScreen().x - 150, getLocationOnScreen().y);
    }

	public static void main(String[] args) {
		new TournamentTreeGuiTest();
	}
}
