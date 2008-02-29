package ctSim.util;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * Menu-Klasse
 */
public class Menu extends JMenu {
	/** UID */
	private static final long serialVersionUID = - 1890962781596714017L;

	/**
	 * @param title	Titel
	 * @param items	Inhalte
	 */
	public Menu(String title, MAction... items) {
    	super(title);
    	for (MAction a : items)
    		add(a.getMenuItem());
    }

	/**
	 * Menu-Action
	 */
	public static abstract class MAction extends AbstractAction {
		/**
		 * @param label	Text
		 * @param icon	Icon
		 */
		public MAction(String label, Icon icon) {
			super(label, icon);
		}

		/**
		 * @return Menu-Item
		 */
		public abstract JMenuItem getMenuItem();
	}

	/**
	 * Menue-Eintraege
	 */
	public static class Entry extends MAction {
		/** UID */
		private static final long serialVersionUID = 8468636621500013742L;

		/** Event-Handler */
		private final Runnable code;
		/** Eintrag */
		private final JMenuItem ourMenuItem;

		/**
		 * Konstruiert eine {@link MAction} aus Beschriftung, Icon und
		 * auszuf&uuml;hrendem Code.
		 * @param label Text
		 * @param icon Icon
		 * @param code Handler
		 */
		public Entry(String label, Icon icon, Runnable code) {
			super(label, icon);
			this.code = code;
			ourMenuItem = new JMenuItem(this);
		}

		/** 
		 * Wie Entry(String, Icon, Runnable), nur mit ohne Icon 
		 * @param name Text 
		 * @param code Handler
		 */
		public Entry(String name, Runnable code) {
			this(name, null, code);
		}

		/**
		 * @see ctSim.util.Menu.MAction#getMenuItem()
		 */
		@Override
		public JMenuItem getMenuItem() {
			return ourMenuItem;
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(
		@SuppressWarnings("unused") ActionEvent e) {
			code.run();
		}
	}

	/**
	 * Checkboxes
	 */
	public static class Checkbox extends MAction {
		/** UID */
		private static final long serialVersionUID = 3470458051483318867L;
		/** Event-Handler */
		private final Runnable1<Boolean> code;
		/** Eintrag */
		private final JMenuItem ourMenuItem;

		/**
		 * @param label	Text
		 * @param code	Handler
		 */
		public Checkbox(String label, Runnable1<Boolean> code) {
			super(label, null);
			this.code = code;
			ourMenuItem = new JCheckBoxMenuItem(this);
		}

		/**
		 * @see ctSim.util.Menu.MAction#getMenuItem()
		 */
		@Override
		public JMenuItem getMenuItem() {
			return ourMenuItem;
		}

		/**
		 * @return Checkbox
		 */
		public Checkbox enable()  { setEnabled(true ); return this; }
		/**
		 * @return Checkbox
		 */
		public Checkbox disable() { setEnabled(false); return this; }
		/**
		 * @return Checkbox
		 */
		public Checkbox check()   { setChecked(true ); return this; }
		/** 
		 * @return Checkbox
		 */
		public Checkbox uncheck() { setChecked(false); return this; }

		/**
		 * @param isSelected
		 */
		public void setChecked(boolean isSelected) {
			ourMenuItem.setSelected(isSelected);
			code.run(isSelected);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			setChecked(((JCheckBoxMenuItem)e.getSource()).isSelected());
		}
	}
}
