package ctSim.util;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

//$$ doc
public class Menu extends JMenu {
	private static final long serialVersionUID = - 1890962781596714017L;

	public Menu(String title, MAction... items) {
    	super(title);
    	for (MAction a : items)
    		add(a.getMenuItem());
    }

	public static abstract class MAction extends AbstractAction {
		public MAction(String label, Icon icon) {
			super(label, icon);
		}

		public abstract JMenuItem getMenuItem();
	}

	public static class Entry extends MAction {
		private static final long serialVersionUID = 8468636621500013742L;

		private final Runnable code;
		private final JMenuItem ourMenuItem;

		/**
		 * Konstruiert eine {@link MAction} aus Beschriftung, Icon und
		 * auszuf&uuml;hrendem Code.
		 */
		public Entry(String label, Icon icon, Runnable code) {
			super(label, icon);
			this.code = code;
			ourMenuItem = new JMenuItem(this);
		}

		/** Wie Entry(String, Icon, Runnable), nur mit ohne Icon */
		public Entry(String name, Runnable code) {
			this(name, null, code);
		}

		@Override
		public JMenuItem getMenuItem() {
			return ourMenuItem;
		}

		public void actionPerformed(
		@SuppressWarnings("unused") ActionEvent e) {
			code.run();
		}
	}

	public static class Checkbox extends MAction {
		private static final long serialVersionUID = 3470458051483318867L;

		private final Runnable1<Boolean> code;
		private final JMenuItem ourMenuItem;

		public Checkbox(String label, Runnable1<Boolean> code) {
			super(label, null);
			this.code = code;
			ourMenuItem = new JCheckBoxMenuItem(this);
		}

		@Override
		public JMenuItem getMenuItem() {
			return ourMenuItem;
		}

		public Checkbox enable()  { setEnabled(true ); return this; }
		public Checkbox disable() { setEnabled(false); return this; }
		public Checkbox check()   { setChecked(true ); return this; }
		public Checkbox uncheck() { setChecked(false); return this; }

		public void setChecked(boolean isSelected) {
			ourMenuItem.setSelected(isSelected);
			code.run(isSelected);
		}

		public void actionPerformed(ActionEvent e) {
			setChecked(((JCheckBoxMenuItem)e.getSource()).isSelected());
		}
	}
}
