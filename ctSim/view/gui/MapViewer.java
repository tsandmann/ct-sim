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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.MapComponent;
import ctSim.util.FmtLogger;
import ctSim.util.MapLines;
import ctSim.util.Misc;
import ctSim.util.Runnable2;

/**
 * Stellt das Fenster f&uuml;r die Map-Anzeige dar.
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class MapViewer extends JPanel {
	/** UID	*/
	private static final long serialVersionUID = 3285763592662732927L;
	/** Logger fuer das Map-Fenster */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.view.gui.LogViewer");
	/** Map-Komponente */
	private final MapComponent mapCompnt;
	/** Image-Viewer */
	private final ImageViewer imageViewer;
	/** ScrollPane des Fensters */
	private final JScrollPane scrollPane;

	/**
	 * Stellt unsere Buttons dar mit Icon und Tooltip
	 */
	class Button extends JButton {
		/** UID */
		private static final long serialVersionUID = 5494027181895045961L;

		/**
		 * Button-Klasse
		 * @param label			Name
		 * @param toolTipText	Tooltip
		 * @param icon			Icon
		 * @param onClick		onClick-Handler
		 */
		public Button(String label, String toolTipText, Icon icon,
		final Runnable onClick) {
			super(label);
			setToolTipText(toolTipText);
			setIcon(icon);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onClick.run();
				}
			});
		}
	}
	
	/**
	 * Filter fuer den Dialog zur Dateiauswahl
	 */
	class PNGFilter extends FileFilter {
	    /**
	     * Ermittelt die Dateinamenerweiterung
	     * @param f Datei
	     * @return Erweiterung
	     */  
	    public String getExtension(File f) {
	        String ext = null;
	        String s = f.getName();
	        int i = s.lastIndexOf('.');

	        if (i > 0 &&  i < s.length() - 1) {
	            ext = s.substring(i + 1).toLowerCase();
	        }
	        return ext;
	    }
		
	    /**
	     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
	     */
	    @Override
		public boolean accept(File f) {
	        if (f.isDirectory()) {
	            return true;
	        }

	        String extension = getExtension(f);
	        if (extension != null) {
	            if (extension.equals("png")) {
	            	return true;
	            } else {
	                return false;
	            }
	        }

	        return false;
	    }

	    /**
	     * @see javax.swing.filechooser.FileFilter#getDescription()
	     */
	    @Override
		public String getDescription() {
	        return "PNG-Bilder";
	    }
	}	
	
	/**
	 * Fordert die komplette Karte neu an
	 */
	private final Runnable onReload = new Runnable() {
		public void run() {
			try {
				mapCompnt.requestMap();
			} catch (IOException e) {
				lg.warn("Konnte Karte nicht anfordern");
			}
		}
	};
	
	/**
	 * Speichert die Karte als png-Bild
	 */
	private final Runnable onSave = new Runnable() {
		public void run() {
			JFileChooser fc = new JFileChooser();
			PNGFilter filter = new PNGFilter();
			fc.addChoosableFileFilter(filter);
			int userChoice = fc.showSaveDialog(MapViewer.this);
			if (userChoice != JFileChooser.APPROVE_OPTION) {
				// Benutzer hat abgebrochen
				return;
			}

			File file = fc.getSelectedFile();
			if (filter.getExtension(file) == null || ! filter.getExtension(file).equals("png")) {
				file = new File(file.getAbsolutePath() + ".png");
			}
			try {
				mapCompnt.saveImage(file);
				lg.fine("Karte nach " + file.getName() + " gespeichert");
			} catch (IOException e) {
				lg.warn("Konnte Karte nicht speichern: " + e.toString());
			}
		}
	};
	
	/**
	 * Erzeugt das Map-Fenster.
	 * @param map	Map-Komponenten, die vom Fenster verwendet werden soll.
	 * @param bot	Bot-Referenz
	 */
	public MapViewer(MapComponent map, Bot bot) {
		mapCompnt = map;
		imageViewer = new ImageViewer(mapCompnt);
	
		setLayout(new BorderLayout());
		
		/* Button bauen */
		JButton save = new Button("speichern", "Karte als png-Bild speichern", null, onSave);
		JButton reload = new Button("neu laden", "Karte komplett (neu) übertragen", null, onReload);
		
		/* Toolbar bauen */
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER));
		toolbar.add(reload);
		toolbar.add(save);
		
		/* Gesamtgroesse setzen */
		scrollPane = new JScrollPane(imageViewer);
		scrollPane.setPreferredSize(new Dimension(500, 600));
		int w = getInsets().left + scrollPane.getInsets().left +
			scrollPane.getPreferredSize().width +
			scrollPane.getInsets().right + getInsets().right + 20;
		int h = getInsets().top + scrollPane.getInsets().top +
			scrollPane.getPreferredSize().height + 
			scrollPane.getInsets().bottom + getInsets().bottom +
			toolbar.getPreferredSize().height;
		setPreferredSize(new Dimension(w, h));
		
		/* ausliefern */
		add(scrollPane, BorderLayout.CENTER);
		add(toolbar, BorderLayout.SOUTH);
	}
	
	/**
	 * Map-Anzeige
	 */
	public static class ImageViewer extends JPanel implements Runnable2<Image, MapLines[]> {
		/** UID */
		private static final long serialVersionUID = -4764621960629937298L;
		/** Bild */
		private Image image;
		/** Breite */
		private final int targetWidth;
		/** Hoehe */
		private final int targetHeight;
		/** Bereich, der immer sichtbar sein soll */
		private final Rectangle scrollPosition = new Rectangle(250, 250);
		/** Farbe der Bot-Position */
		private final Color botColor = new Color(255, 0, 0);
		/** Liste fuer einzuzeichnende Linien */
		private MapLines[] lines;
		
		/**
		 * @param c Map-Komponente
		 */
		public ImageViewer(MapComponent c) {
			c.addImageListener(this);
			setToolTipText(c.getDescription());
			setBorder(BorderFactory.createLoweredBevelBorder());
			targetWidth  = c.getWidth();
			targetHeight = c.getHeight();
		}

		/** 
		 * Methode einer Swing-Komponente, aber thread-sicher 
		 * @param img	Image
		 * @param mapLines	Punkte fuer Bot-Position und Linien	
		 */
		public synchronized void run(Image img, MapLines[] mapLines) {			
			this.image = img;
			MapLines center = mapLines[mapLines.length - 1];
			this.lines = mapLines;
			repaint();
			
			/* Bereich um den Bot in den sichtbaren Bereich scrollen */
			this.scrollPosition.x = Misc.clamp(center.x1 - 125, 1535);
			this.scrollPosition.y = Misc.clamp(center.y1 - 125, 1535);
			scrollRectToVisible(this.scrollPosition);
		}
		
		/**
		 * @see javax.swing.JComponent#paint(java.awt.Graphics)
		 */
		@Override
		public void paint(Graphics g) {
			/* Image zeichnen */
			g.drawImage(image, 0, 0, null);

			/* Linien einzeichnen */
			if (lines != null) {
				for (int i=0; i<lines.length-1; i++) {
					Color color;
					switch (lines[i].color) {
					case 0:
						color = Color.GREEN;
						break;
					case 1:
						color = Color.RED;
						break;
					default:
						color = Color.BLACK;
						break;
					}
					g.setColor(color);
					g.drawLine(lines[i].x1, lines[i].y1, lines[i].x2, lines[i].y2);
				}
				
				/* Bot-Position einzeichnen */
				g.setColor(botColor);
				g.fillArc(lines[lines.length - 1].x1 - 7, lines[lines.length - 1].y1 - 7, 14, 14, lines[lines.length - 1].x2 + 120, 300);

			}			
		}

		/**
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		@Override
		public Dimension getPreferredSize() {
			Insets is = getBorder().getBorderInsets(this);
			return new Dimension(targetWidth  + is.left + is.right,
			                     targetHeight + is.top  + is.bottom);
		}
	}

	/**
	 * Baut den Viewer
	 * @param compnt Map-Komponente
	 */
	public void buisitMapViewer(final MapComponent compnt) {
		// Container
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(compnt.getName()));
		JPanel controls = new JPanel();

		// Map als Bild anzeigen
		ImageViewer v = new ImageViewer(compnt);
		p.add(v, BorderLayout.CENTER);

		// Ausliefern
		p.add(controls, BorderLayout.CENTER);

		add(p);
		add(Box.createRigidArea(new Dimension(0, 5)));
	}
}
