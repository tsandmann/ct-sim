/*
 * c't-Sim - Robotersimulator für den c't-Bot
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

package ctSim.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTabbedPane;

/**
 * <p>
 * TabbedPane, deren Tabs ein Schließen-Icon haben (kleines "X", das auch einen eigenen Tooltip haben
 * kann). Diese Klasse ist ein Hack, um auch mit Java 5+ klickbare Icons an der Stelle machen zu können.
 * (Java 6 würde das direkt unterstützen, womit diese Klasse überflüssig wäre. Näheres:
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/components/tabbedpane.html">Sun's How to Use
 * Tabbed Panes</a>, Abschnitt "Tabs With Custom Components".)
 * </p>
 * <p>
 * Tabs mit Schließen-Icon werden nur bei Verwendung der addClosableTab(...)-Methoden erzeugt. addTab(...)
 * erzeugt reguläre Tabs.
 * </p>
 * <p>
 * Verwendung:<br>
 * <code>JComponent tabContent = new JPanel();</code><br>
 * <code>tabContent.add(...);</code><br>
 * <code>ClosableTabsPane ctp = new {@link #ClosableTabsPane(Icon) ClosableTabsPane}(...);</code><br>
 * <code>ctp.{@link #addClosableTab(String, Component, String) addClosableTab}("Tab-Titel", tabContent, "Tab-Tooltip");</code>
 * </p>
 * <p>
 * Code neu geschrieben und erweitert; hierbei wurde das Grundprinzip übernommen aus:
 * <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6632934">Bug JDK-6632934: JTabbedPane with closable tabs</a>
 * </p>
 * <p>
 * Der Sim verwendet derzeit nur eine Instanz dieser Klasse, in der <em>alle</em> Tabs ein Schließen-Icon
 * haben. <em>Theoretisch</em> kann man in einer Instanz dieser Klasse schließbare und nicht-schließbare
 * Tabs gemischt verwenden, aber das habe ich nie getestet. Es ist ein Hack wie gesagt.
 * </p>
 *
 * @author Hendrik Krauß
 */
public class ClosableTabsPane extends JTabbedPane {
	/**
	 * Icon-Wrapper, der sich merkt, wo das Icon zuletzt gezeichnet wurde (Koordinaten in Pixeln von der
	 * oberen linken Ecke der JComponent, in der das Icon gemalt wurde)
	 */
    public static class BoundedIcon implements Icon {
    	/** Icon */
    	private Icon wrappee;
    	/** X-Koordinate */
    	private int lastX;
    	/** Y-Koordinate */
		private int lastY;

		/**
		 * @param wrappee	Inhalt
		 */
		public BoundedIcon(Icon wrappee) {
    		this.wrappee = wrappee;
		}

		/**
		 * @return Rechteck
		 */
		public Rectangle getLastBounds() {
			return new Rectangle(lastX, lastY, getIconWidth(), getIconHeight());
		}

		/**
		 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
		 */
		public void paintIcon(Component c, Graphics g, int x, int y) {
			lastX = x;
			lastY = y;
			wrappee.paintIcon(c, g, x, y);
		}

		/**
		 * @return Wrappee
		 */
		public Icon getWrappee() { return wrappee; }
		/**
		 * Wrappee-Icon setzen
		 * 
		 * @param wrappee
		 */
		public void setWrappee(Icon wrappee) { this.wrappee = wrappee; }

		/**
		 * @see javax.swing.Icon#getIconHeight()
		 */
		public int getIconHeight() { return wrappee.getIconHeight(); }
		
		/**
		 * @see javax.swing.Icon#getIconWidth()
		 */
		public int getIconWidth() { return wrappee.getIconWidth(); }
    }

    /*
	 * Implementierungsprinzip: Die Schließen-Icons sind spezielle Icons, die wissen, an welchen Koordinaten
	 * sie zuletzt gemalt wurden. Die ClosableTabsPane hat zwei Listener:
	 * 1. einen MouseListener, der das Icon fragt, ob die Koordinaten des Klick-Event innerhalb des
	 * Icon liegen oder nicht; falls ja, wird das Tab geschlossen.
	 * 2. einen MouseMotionListener, der ebenfalls die Mauskoordinaten gegen das Icon prüft und ggf.
	 * das Icon austauscht, um den Hover-Effekt (mouseover / mouseout) zu erzielen.
	 * Die Tooltips der Icons werden ähnlich gemacht.
	 */
    /** UID */
	private static final long serialVersionUID = 1030593683059736787L;

	/** Nicht gewrapptes Schließen-Icon für dann, wenn Mauscursor nicht darüber */
	private final Icon rawCloseIcon;

	/** Nicht gewrapptes Schließen-Icon für dann, wenn Mauscursor darüber */
	private final Icon rawCloseIconHover;

	/**
	 * Tooltips der Schließen-Icons (nicht der Tab-Beschriftung). Indices: Wie Tab-Indices, die von
	 * JTabbedPane sonst immer verwendet werden.
	 */
	private final List<String> closeIconToolTips = Misc.newList();

	/** Schließen-Listener */
	private final List<Runnable1<Integer>> closeListeners = Misc.newList();

	/**
	 * @param closeIcon	Icon
	 */
	public ClosableTabsPane(Icon closeIcon) {
		this(closeIcon, closeIcon);
	}

	/**
	 * Wird nicht tatsächlich geschlossen, Event feuert nur
	 * 
	 * @param closeIcon
	 * @param closeIconHover
	 */
	public ClosableTabsPane(final Icon closeIcon, final Icon closeIconHover) {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx = indexOfBoundedIconAt(e.getX(), e.getY());
				if (idx != -1) {
					for (Runnable1<Integer> li : closeListeners)
						li.run(idx);
				}
			}

			// falls einer aus der TabbedPane nach oben rausfährt, sodass der MotionListener nicht greift
			@Override
			public void mouseExited(MouseEvent e) {
				handleMouseMotionEvent(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
        		handleMouseMotionEvent(e);
        	}
        });

		this.rawCloseIcon = closeIcon;
		this.rawCloseIconHover = closeIconHover;
	}

	/**
	 * Behandelt das MouseMotion-Event
	 * 
	 * @param e	Event
	 */
	private void handleMouseMotionEvent(MouseEvent e) {
		int mouseOverIdx = indexOfBoundedIconAt(e.getX(), e.getY());

		// falls mouseOverIdx -1 ist, wird mouseOverIdx == i nie wahr
		for (int i = 0; i < getTabCount(); i++) {
			Icon ic = getIconAt(i);
			if (ic instanceof BoundedIcon) {
				((BoundedIcon)ic).setWrappee(mouseOverIdx == i ?
					rawCloseIconHover : rawCloseIcon);
			}
		}
		repaint();
	}

	/**
	 * X/Y relativ zu dieser TabbedPane, d.h. man kann die Koordinaten, wie sie ein MouseEvent liefert,
	 * direkt weiterverarbeiten. Gibt einen Tab-Index zurück wie die {@code index...()}-Methoden in
	 * JTabbedPane.
	 * 
	 * @param x	X-Koordinate
	 * @param y	Y-Koordinate
	 * @return Tab-Index
	 */
	private int indexOfBoundedIconAt(int x, int y) {
		// Tab mit welcher Indexnummer geklickt?
		int idx = indexAtLocation(x, y);
		if (idx == -1)
			return -1;
		// War das auf dem Icon?
		Icon ic = getIconAt(idx);
		if (! (ic instanceof BoundedIcon))
			return -1;
		return ((BoundedIcon)ic).getLastBounds().contains(x, y) ?
			idx : -1;
	}

	/**
	 * @see javax.swing.JTabbedPane#getToolTipText(java.awt.event.MouseEvent)
	 */
	@Override
	public String getToolTipText(MouseEvent e) {
		int idx = indexOfBoundedIconAt(e.getX(), e.getY());
		if (idx != -1)
			return closeIconToolTips.get(idx);
		return super.getToolTipText(e);
	}

	/**
	 * Fügt neuen Tab hinzu
	 * 
	 * @param title		Titel
	 * @param component	Komponente
	 */
	public void addClosableTab(String title, Component component) {
		addClosableTab(title, component, null);
	}

	/**
	 * Fügt neuen Tab hinzu
	 * 
	 * @param title		Titel
	 * @param component	Komponente
	 * @param toolTip	Tooltip
	 */
	public void addClosableTab(String title, Component component,
		String toolTip) {

		addClosableTab(title, component, toolTip, "Tab schließen");
	}

	/**
	 * Fügt neuen Tab hinzu
	 * 
	 * @param title				Titel
	 * @param component			Komponente
	 * @param toolTip			Tooltip
	 * @param closeIconToolTip	Icon-Tooltip
	 */
	public void addClosableTab(String title, Component component,
		String toolTip, String closeIconToolTip) {

		// hier (und nur hier) BoundedIcon erstellen
		addTab(title, new BoundedIcon(rawCloseIcon), component, toolTip);
		closeIconToolTips.add(closeIconToolTip);
	}

	/**
	 * <p>
	 * Registriert einen Listener, der benachrichtigt wird, wenn der Benutzer ein Tab aus dieser TabsPane
	 * schließt. Der Parameter, der dem Listener übergeben wird, ist der Index des Tab (einer der in
	 * {@link JTabbedPane} üblichen Indizes). Verwendungsbeispiel:
	 * 
	 * <pre>
	 * tabsPane.addCloseListener(new Closure<Integer>() {
	 *     public void run(Integer index) { 
	 *         Component c = tabsPane.getComponentAt(index);
	 *         ...
	 *     }
	 * });
	 * </pre>
	 * </p>
	 * 
	 * @param li
	 * @throws NullPointerException	falls li == {@code null} ist
	 */
	public void addCloseListener(Runnable1<Integer> li) {
		if (li == null)
			throw new NullPointerException();
		closeListeners.add(li);
	}

	/**
	 * Entfernt einen Listener, den man zuvor registriert hat. Siehe {@link #addCloseListener(Runnable1)}.
	 * Der Aufruf wird stillschweigend ignoriert, falls der Listener schon entfernt ist oder nie
	 * registriert wurde.
	 * 
	 * @param li	Listener
	 */
	public void removeCloseListener(Runnable1<Integer> li) {
		closeListeners.remove(li);
	}
}
