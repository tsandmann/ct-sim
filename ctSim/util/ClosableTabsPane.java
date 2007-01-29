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
 * TabbedPane, deren Tabs ein Schlie&szlig;en-Icon haben (kleines &quot;X&quot;,
 * das auch einen eigenen Tooltip haben kann). Diese Klasse ist ein Hack, um
 * auch mit Java 5 klickbare Icons an der Stelle machen zu k&ouml;nnen. (Java 6
 * w&uuml;rde das direkt unterst&uuml;tzen, womit diese Klasse
 * &uuml;berfl&uuml;ssig w&auml;re. N&auml;heres: <a
 * href="http://java.sun.com/docs/books/tutorial/uiswing/components/tabbedpane.html">Sun's
 * How to Use Tabbed Panes</a>, Abschnitt "Tabs With Custom Components".)
 * </p>
 * <p>
 * Tabs mit Schlie&szlig;en-Icon werden nur bei Verwendung der
 * addClosableTab(...)-Methoden erzeugt. addTab(...) erzeugt regul&auml;re Tabs.
 * </p>
 * <p>
 * Verwendung: <br />
 * <code>JComponent tabContent = new JPanel();</code><br />
 * <code>tabContent.add(...);</code><br />
 * <code>ClosableTabsPane ctp = new {@link #ClosableTabsPane(Icon) ClosableTabsPane}(...);</code><br/>
 * <code>ctp.{@link #addClosableTab(String, Component, String) addClosableTab}("Tab-Titel", tabContent, "Tab-Tooltip");</code><br/>
 * </p>
 * <p>
 * Grundprinzip aus einem <a
 * href="http://forum.java.sun.com/thread.jspa?threadID=337070">Forumsposting
 * von einem Mr_Silly</a> &uuml;bernommen; Code neu geschrieben und erweitert.
 * </p>
 * <p>
 * Der Sim verwendet derzeit nur eine Instanz dieser Klasse, in der
 * <em>alle</em> Tabs ein Schlie&szlig;en-Icon haben. <em>Theoretisch</em>
 * kann man in einer Instanz dieser Klasse schlie&szlig;bare und
 * nicht-schlie&szlig;bare Tabs gemischt verwenden, aber das hab ich nie
 * getestet. Es ist ein Hack wie gesagt.
 * </p>
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ClosableTabsPane extends JTabbedPane {
	/**
	 * Icon-Wrapper, der sich merkt, wo das Icon zuletzt gezeichnet wurde
	 * (Koordinaten in Pixeln von der oberen linken Ecke der JComponent, in der
	 * das Icon gemalt wurde)
	 */
    public static class BoundedIcon implements Icon {
    	private Icon wrappee;
    	private int lastX;
		private int lastY;

		public BoundedIcon(Icon wrappee) {
    		this.wrappee = wrappee;
		}

		public Rectangle getLastBounds() {
			return new Rectangle(lastX, lastY, getIconWidth(), getIconHeight());
		}

		public void paintIcon(Component c, Graphics g, int x, int y) {
			lastX = x;
			lastY = y;
			wrappee.paintIcon(c, g, x, y);
		}

		public Icon getWrappee() { return wrappee; }
		public void setWrappee(Icon wrappee) { this.wrappee = wrappee; }

		public int getIconHeight() { return wrappee.getIconHeight(); }
		public int getIconWidth() { return wrappee.getIconWidth(); }
    }

    /*
	 * Implementierungsprinzip: Die Schliessen-Icons sind spezielle Icons, die
	 * wissen, an welchen Koordinaten sie zuletzt gemalt wurden. Die
	 * ClosableTabsPane hat zwei Listener: 1. einen MouseListener, der das Icon
	 * fragt, ob die Koordinaten des Klick-Event innerhalb des Icon liegen oder
	 * nicht; falls ja, wird das Tab geschlossen. 2. einen MouseMotionListener,
	 * der ebenfalls die Mauskoordinaten gegen das Icon prueft und ggf. das Icon
	 * austauscht, um den Hover-Effekt (mouseover / mouseout) zu erzielen. Die
	 * Tooltips der Icons werden aehnlich gemacht.
	 */

	private static final long serialVersionUID = 1030593683059736787L;

	/**
	 * Nicht gewrapptes Schlie&szlig;en-Icon f&uuml;r dann, wenn Mauscursor
	 * nicht dr&uuml;ber
	 */
	private final Icon rawCloseIcon;

	/**
	 * Nicht gewrapptes Schlie&szlig;en-Icon f&uuml;r dann, wenn Mauscursor
	 * dr&uuml;ber
	 */
	private final Icon rawCloseIconHover;

	/**
	 * Tooltips der Schlie&szlig;en-Icons (nicht der Tab-Beschriftung). Indices:
	 * Wie Tab-Indices, die von JTabbedPane sonst immer verwendet werden.
	 */
	private final List<String> closeIconToolTips = Misc.newList();

	//$$ doc
	private final List<Closure<Integer>> closeListeners = Misc.newList();

	public ClosableTabsPane(Icon closeIcon) {
		this(closeIcon, closeIcon);
	}

	// wird nicht tatsaechlich geschlossen, Event feuert nur
	public ClosableTabsPane(final Icon closeIcon, final Icon closeIconHover) {
		addMouseListener(new MouseAdapter() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx = indexOfBoundedIconAt(e.getX(), e.getY());
				if (idx != -1) {
					for (Closure<Integer> li : closeListeners)
						li.run(idx);
				}
			}

			// Falls einer aus der TabbedPane nach oben rausfaehrt, so dass
			// der MotionListener nicht greift
			@SuppressWarnings("synthetic-access")
			@Override
			public void mouseExited(MouseEvent e) {
				handleMouseMotionEvent(e);
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@SuppressWarnings("synthetic-access")
        	@Override
        	public void mouseMoved(MouseEvent e) {
        		handleMouseMotionEvent(e);
        	}
        });

		this.rawCloseIcon = closeIcon;
		this.rawCloseIconHover = closeIconHover;
	}

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
	 * X/Y relativ zu dieser TabbedPane, d.h. man kann die Koordinaten, wie sie
	 * ein MouseEvent liefert, direkt weiterverwursten. Gibt einen Tab-Index
	 * zur&uuml;ck wie die {@code index...()}-Methoden in JTabbedPane.
	 */
	private int indexOfBoundedIconAt(int x, int y) {
		// Tab mit welcher Indexnr. geklickt?
		int idx = indexAtLocation(x, y);
		if (idx == -1)
			return -1;
		// War das aufm Icon?
		Icon ic = getIconAt(idx);
		if (! (ic instanceof BoundedIcon))
			return -1;
		return ((BoundedIcon)ic).getLastBounds().contains(x, y) ?
			idx : -1;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		int idx = indexOfBoundedIconAt(e.getX(), e.getY());
		if (idx != -1)
			return closeIconToolTips.get(idx);
		return super.getToolTipText(e);
	}

	public void addClosableTab(String title, Component component) {
		addClosableTab(title, component, null);
	}

	public void addClosableTab(String title, Component component,
		String toolTip) {

		addClosableTab(title, component, toolTip, "Tab schlie\u00DFen");
	}

	public void addClosableTab(String title, Component component,
		String toolTip, String closeIconToolTip) {

		// Hier (und nur hier) BoundedIcon erstellen
		addTab(title, new BoundedIcon(rawCloseIcon), component, toolTip);
		closeIconToolTips.add(closeIconToolTip);
	}

	/**
	 * <p>
	 * Registriert einen Listener, der benachrichtigt wird, wenn der Benutzer
	 * ein Tab aus dieser TabsPane schlie&szlig;t. Der Parameter, der dem
	 * Listener &uuml;bergeben wird, ist der Index des Tab (einer der in
	 * {@link JTabbedPane} &uuml;blichen Indizes). Verwendungsbeispiel:
	 * 
	 * <pre>
	 * tabsPane.addCloseListener(new Closure&lt;Integer&gt;() {
	 *     public void run(Integer index) { 
	 *         Component c = tabsPane.getComponentAt(index);
	 *         ...
	 *     }
	 * });</pre>
	 * 
	 * </p>
	 * 
	 * @param li
	 * @throws NullPointerException Falls li == {@code null} ist.
	 */
	public void addCloseListener(Closure<Integer> li) {
		if (li == null)
			throw new NullPointerException();
		closeListeners.add(li);
	}

	/**
	 * Entfernt einen Listener, den man zuvor registriert hat. Siehe
	 * {@link #addCloseListener(Closure)}. Der Aufruf wird stillschweigend
	 * ignoriert, falls der Listener schon entfernt ist oder nie registriert
	 * wurde.
	 */
	public void removeCloseListener(Closure<Integer> li) {
		closeListeners.remove(li);
	}
}
