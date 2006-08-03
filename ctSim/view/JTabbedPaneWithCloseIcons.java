package ctSim.view;

/*
 * Die folgenden Klassen stammen aus dem Java-Forum von Sun
 * und wurden dort von "Mr_Silly" gepostet...
 * 
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A JTabbedPane which has a close ('X') icon on each tab.
 * 
 * To add a tab, use the method addTab(String, Component)
 * 
 * To have an extra icon on each tab (e.g. like in JBuilder, showing the file
 * type) use the method addTab(String, Component, Icon). Only clicking the 'X'
 * closes the tab.
 */
/*
 * Die folgenden Klassen stammen aus dem Java-Forum von Sun
 * und wurden dort von "Mr_Silly" gepostet...
 * 
 * ...und wurden für unsere Zwecke ein wenig angepasst.
 * 
 */
public class JTabbedPaneWithCloseIcons extends JTabbedPane implements
		MouseListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JTabbedPaneWithCloseIcons() {
		super();
		addMouseListener(this);
	}

	public void addTab(String title, Component component) {
		
		//this.addTab(title, component, null);
		this.addTab(title, null, component);
	}

	// addTab(String title, Component component, Icon extraIcon)
	// -> addTab(String title, Icon extraIcon, Component component)
	public void addTab(String title, Icon extraIcon, Component component) {
		super.addTab(title, new CloseTabIcon(extraIcon), component);
	}
	
	public void addTab(String title, Icon extraIcon, Component component, String tip) {
		super.addTab(title, new CloseTabIcon(extraIcon), component, tip);
	}

	public void mouseClicked(MouseEvent e) {
		//int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
		int tabNumber = this.indexAtLocation(e.getX(), e.getY());
		
		if (tabNumber < 0)
			return;
		
		Rectangle rect = ((CloseTabIcon) getIconAt(tabNumber)).getBounds();
		if (rect.contains(e.getX(), e.getY())) {
			// the tab is being closed
			this.removeTabAt(tabNumber);
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}
}

/**
 * The class which generates the 'X' icon for the tabs. The constructor accepts
 * an icon which is extra to the 'X' icon, so you can have tabs like in
 * JBuilder. This value is null if no extra icon is required.
 */
class CloseTabIcon implements Icon {
	private int x_pos;

	private int y_pos;

	private int width;

	private int height;

	private Icon fileIcon;

	public CloseTabIcon(Icon fileIcon) {
		this.fileIcon = fileIcon;
		width = 9;
		height = 9;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
		this.x_pos = x;
		this.y_pos = y;

		Color col = g.getColor();

		g.setColor(Color.BLACK);
		int x_min = x;
		int y_min = y;
		int x_max = x_min + this.width-1;
		int y_max = y_min + this.height-1;
		float x_mid = (x_max - x_min) / 2f;
		float y_mid = (y_max - y_min) / 2f;
		int x_mid1 = x_min + (int)Math.floor(x_mid);
		int x_mid2 = x_min + (int)Math.ceil(x_mid);
		int y_mid1 = y_min + (int)Math.floor(y_mid);
		int y_mid2 = y_min + (int)Math.ceil(y_mid);
		
		// Wir fangen unten an:
//		int x_p = x;
//		int y_p = y;
//		Polygon poly = new Polygon();
//		poly.addPoint(x_p, y_p);
//		poly.addPoint(x_p+1, y_p);
//		poly.addPoint(x_p+3, y_p+2);
//		poly.addPoint(x_p+4, y_p+2);
//		poly.addPoint(x_p+6, y_p);
//		poly.addPoint(x_p+7, y_p);
//		poly.addPoint(x_p+7, y_p+1);
//		poly.addPoint(x_p+5, y_p+3);
//		poly.addPoint(x_p+5, y_p+4);
//		poly.addPoint(x_p+7, y_p+6);
//		poly.addPoint(x_p+7, y_p+7);
//		poly.addPoint(x_p+6, y_p+7);
//		poly.addPoint(x_p+4, y_p+5);
//		poly.addPoint(x_p+3, y_p+5);
//		poly.addPoint(x_p+1, y_p+7);
//		poly.addPoint(x_p, y_p+7);
//		poly.addPoint(x_p, y_p+6);
//		poly.addPoint(x_p+2, y_p+4);
//		poly.addPoint(x_p+2, y_p+3);
//		poly.addPoint(x_p, y_p+1);
		
		Polygon poly = new Polygon();
		poly.addPoint(x_min, y_min);
		poly.addPoint(x_min+1, y_min);
		poly.addPoint(x_min+2, y_min);
		poly.addPoint(x_mid1, y_mid1-2);
		poly.addPoint(x_mid2, y_mid1-2);
		poly.addPoint(x_max-2, y_min);
		poly.addPoint(x_max-1, y_min);
		poly.addPoint(x_max, y_min);
		poly.addPoint(x_max, y_min+1);
		poly.addPoint(x_max, y_min+2);
		poly.addPoint(x_mid2+2, y_mid1);
		poly.addPoint(x_mid2+2, y_mid2);
		poly.addPoint(x_max, y_max-2);
		poly.addPoint(x_max, y_max-1);
		poly.addPoint(x_max, y_max);
		poly.addPoint(x_max-1, y_max);
		poly.addPoint(x_max-2, y_max);
		poly.addPoint(x_mid2, y_mid2+2);
		poly.addPoint(x_mid1, y_mid2+2);
		poly.addPoint(x_min+2, y_max);
		poly.addPoint(x_min+1, y_max);
		poly.addPoint(x_min, y_max);
		poly.addPoint(x_min, y_max-1);
		poly.addPoint(x_min, y_max-2);
		poly.addPoint(x_mid1-2, y_mid2);
		poly.addPoint(x_mid1-2, y_mid1);
		poly.addPoint(x_min, y_min+2);
		poly.addPoint(x_min, y_min+1);
		
//		for(int i = 0; i<poly.xpoints.length;i++) {
//			
//			System.out.println(String.format("Punkt: %1$2d | %2$2d", poly.xpoints[i], poly.ypoints[i]));
//		}
		
		// Quadrat zeichnen:
//		g.drawLine(x + 1, y_p, x + 12, y_p);
//		g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
//		g.drawLine(x, y_p + 1, x, y_p + 12);
//		g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);
		
		// Dreifach-X zeichnen:
		g.setColor(new Color(255, 128, 128));
		g.fillPolygon(poly);
		g.setColor(Color.DARK_GRAY);
		g.drawPolygon(poly);
		
//		g.drawLine(x_p,     y_p,     x_p + 7, y_p + 7);
//		g.drawLine(x_p,     y_p + 1, x_p + 6, y_p + 7);
//		g.drawLine(x_p + 1, y_p,     x_p + 7, y_p + 6);
//		
//		g.drawLine(x_p + 7, y_p,     x_p,     y_p + 7);
//		g.drawLine(x_p + 7, y_p + 1, x_p + 1, y_p + 7);
//		g.drawLine(x_p + 6, y_p,     x_p,     y_p + 6);
		
		g.setColor(col);
		if (fileIcon != null) {
			fileIcon.paintIcon(c, g, x + width, y_min);
		}
	}
	
	public int getIconWidth() {
		return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
	}

	public int getIconHeight() {
		return height;
	}

	public Rectangle getBounds() {
		return new Rectangle(x_pos, y_pos, width, height);
	}
}

//class CloseTabIcon implements Icon {
//	private int x_pos;
//
//	private int y_pos;
//
//	private int width;
//
//	private int height;
//
//	private Icon fileIcon;
//
//	public CloseTabIcon(Icon fileIcon) {
//		this.fileIcon = fileIcon;
//		width = 16;
//		height = 16;
//	}
//
//	public void paintIcon(Component c, Graphics g, int x, int y) {
//		this.x_pos = x;
//		this.y_pos = y;
//
//		Color col = g.getColor();
//
//		g.setColor(Color.black);
//		int y_p = y + 2;
//		
//		// Quadrat zeichnen:
//		g.drawLine(x + 1, y_p, x + 12, y_p);
//		g.drawLine(x + 1, y_p + 13, x + 12, y_p + 13);
//		g.drawLine(x, y_p + 1, x, y_p + 12);
//		g.drawLine(x + 13, y_p + 1, x + 13, y_p + 12);
//		
//		// Dreifach-X zeichnen:
//		g.drawLine(x + 3, y_p + 3, x + 10, y_p + 10);
//		g.drawLine(x + 3, y_p + 4, x + 9, y_p + 10);
//		g.drawLine(x + 4, y_p + 3, x + 10, y_p + 9);
//		g.drawLine(x + 10, y_p + 3, x + 3, y_p + 10);
//		g.drawLine(x + 10, y_p + 4, x + 4, y_p + 10);
//		g.drawLine(x + 9, y_p + 3, x + 3, y_p + 9);
//		
//		g.setColor(col);
//		if (fileIcon != null) {
//			fileIcon.paintIcon(c, g, x + width, y_p);
//		}
//	}
//
//	public int getIconWidth() {
//		return width + (fileIcon != null ? fileIcon.getIconWidth() : 0);
//	}
//
//	public int getIconHeight() {
//		return height;
//	}
//
//	public Rectangle getBounds() {
//		return new Rectangle(x_pos, y_pos, width, height);
//	}
//}