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

/**
 * Testklasse
 */
public class TournamentTreeGuiTest extends JFrame {
	/**
	 * fuer Tests
	 */
	public class TournamentTreeModel implements TreeModel {
		/** Listenerliste */
		ArrayList<TreeModelListener> listeners =
			new ArrayList<TreeModelListener>();
		/** Level */
		int currentLevel;
		/** Carrier-Baum */
		SpreadingTree<Integer> carrier;

		/**
		 * fuer Tests
		 */
		public TournamentTreeModel() {
			init();
		}

		/**
		 * fuer Tests
		 */
		public void init() {
			currentLevel = 1;
			addLevel();
		}

        /**
         * fuer Tests
         */
        public void addLevel() {
        	ArrayList<Integer> li = new ArrayList<Integer>();
        	for (int i = 1; i <= Math.pow(2, currentLevel); i++)
        		li.add(i);
        	carrier = SpreadingTree.buildTree(li);
			for (TreeModelListener l : listeners)
				l.treeStructureChanged(new TreeModelEvent(this,
					new TreePath(carrier)));
			currentLevel++;
        }

        /**
         * fuer Tests
         */
        public void removeNode() {
        	int u = currentLevel - 4;
        	init();
        	for (int i = 0; i < u; i++)
        		addLevel();
        }

		/**
		 * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
		 */
		public void addTreeModelListener(TreeModelListener l) {
	        listeners.add(l);
        }

		/**
		 * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
		 */
		public void removeTreeModelListener(TreeModelListener l) {
			listeners.remove(l);
		}

		/**
		 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
		 */
		public int getChildCount(Object parent) {
			return isLeaf(parent) ? 0 : 2;
		}

		/**
		 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
		 */
		@SuppressWarnings("unchecked")
		public Object getChild(Object parent, int index) {
			if (index < 0 || index > 1)
				throw new IllegalArgumentException();
	        return index == 0
	        	? ((SpreadingTree)parent).left
	        	: ((SpreadingTree)parent).right;
        }

		/**
		 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
		 */
		public int getIndexOfChild(Object parent, Object child) {
	        throw new UnsupportedOperationException();
        }

		/**
		 * @see javax.swing.tree.TreeModel#getRoot()
		 */
		public Object getRoot() {
	        return carrier;
        }

		/**
		 * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
		 */
		@SuppressWarnings("unchecked")
		public boolean isLeaf(Object node) {
	        return ((SpreadingTree)node).payload != null;
        }

		/**
		 * fuer Tests
		 * @param path 
		 * @param newValue 
		 */
		public void valueForPathChanged(TreePath path, Object newValue) {
	        throw new UnsupportedOperationException();
        }
	}

	/** UID */
    private static final long serialVersionUID = - 4828458620913240203L;
    /** Tournament-Baum */
    TournamentTreeModel tm = new TournamentTreeModel();
    /** JTree des Tournament-Baums */
    JTree t = new JTree(tm);
    /** Label */
    JLabel label = new JLabel("2 Level");

    /**
     * Tests
     */
    public TournamentTreeGuiTest() {
    	setLayout(new BorderLayout());
    	JButton her = new JButton("+");
    	her.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
	            tm.addLevel();
	            for (int i = 0; i < t.getRowCount(); i++)
	            	t.expandRow(i);
	            label.setText(tm.currentLevel - 1 + " Level");
            }
    	});
    	JButton weg = new JButton("\u2013");
    	weg.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			tm.removeNode();
    			for (int i = 0; i < t.getRowCount(); i++)
    				t.expandRow(i);
    			label.setText(tm.currentLevel - 1 + " Level");
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
    	setTitle("SpreadingTree");
    	setSize(200, 600);
    	setLocationRelativeTo(null);
    	setVisible(true);
    	setLocation(getLocationOnScreen().x - 150, getLocationOnScreen().y);
    }

    /**
     * main
     * @param args
     */
	public static void main(String[] args) {
		new TournamentTreeGuiTest();
	}
}
