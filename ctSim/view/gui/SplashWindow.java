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

package ctSim.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import ctSim.util.FmtLogger;

/**
 * Splash-Screen für den c't-Sim
 * 
 * Grundidee übernommen von:
 * <a href="http://www.randelshofer.ch/oop/javasplash/javasplash.html">Java Splash -- Werner Randelshofer</a> 
 * 
 * @author Timo Sandmann
 */
public class SplashWindow extends Window {
	/** UID	*/
	private static final long serialVersionUID = -5856500612291500590L;

	/** Instanz des Splash-Fensters (Singleton) */
	private static SplashWindow instance;

	/** Das Splash-Image des Fensters */
	private Image image;
	
	/** Textnachricht, die unter dem Bild angezeigt wird */
	private static String message = "";
	
	/** Name und Versionsnummer */
	private String version;
	
	/** Log-Handler, um Log-Nachrichten ausgeben zu können */
	private static Handler logHandler;

	/**
	 * This attribute indicates whether the method paint(Graphics) has been called at least once since
	 * the construction of this window.<br>
	 * This attribute is used to notify method splash(Image) that the window has been drawn at least once
	 * by the AWT event dispatcher thread.<br>
	 * This attribute acts like a latch. Once set to true, it will never be changed back to false again.
	 *
	 * @see #paint
	 */
	private boolean paintCalled = false;

	/**
	 * Erzeugt ein neues Splash-Fenster
	 * 
	 * @param parent	Parent
	 * @param image		Splash-Image
	 * @param version	Versionsnummer
	 */
	private SplashWindow(Frame parent, Image image, String version) {
		super(parent);
		this.image = image;
		this.version = version;
		this.setAlwaysOnTop(true);
		setForeground(Color.RED);
		setFont(new Font("Verdana", Font.BOLD, 12));

		/* Bild laden */
		MediaTracker mt = new MediaTracker(this);
		mt.addImage(image, 0);
		try {
			mt.waitForID(0);
		} catch (InterruptedException ie) {
			// egal
		}

		/* Abbruch bei Fehler */
		if (mt.isErrorID(0)) {
			setSize(0, 0);
			System.err.println("Warning: SplashWindow could not load splash image.");
			synchronized (this) {
				paintCalled = true;
				notifyAll();
			}
			return;
		}

		/* Zentrieren */
		int imgWidth = image.getWidth(this);
		int imgHeight = image.getHeight(this);
		setSize(imgWidth, imgHeight);
		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenDim.width - imgWidth) / 2, (screenDim.height - imgHeight) / 2);

		/* Schließen des Splash-Screens per Mausklick */
		MouseAdapter disposeOnClick = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				synchronized (SplashWindow.this) {
					SplashWindow.this.paintCalled = true;
					SplashWindow.this.notifyAll();
				}
				dispose();
			}
		};
		addMouseListener(disposeOnClick);
		
		logHandler = new SplashLogHandler();
	}

	/**
	 * Aktualisiert den Splash-Screen
	 * 
	 * @param g	Graphics
	 */
	@Override
	public void update(Graphics g) {
		paint(g);
	}

	/**
	 * Zeichnet den Splash-Screen
	 * 
	 * @param g	Graphics-Objekt
	 */
	@Override
	public void paint(Graphics g) {
		g.drawImage(image, 0, 0, this);
		g.drawString(version, image.getWidth(null) / 2 - 40, 95);
		g.drawString(message, 5, image.getHeight(null)-7);
		
		if (!paintCalled) {
			paintCalled = true;
			synchronized (this) {
				notifyAll();
			}
		}
	}

	/**
	 * Zeigt ein Image als Splash-Screen an
	 * 
	 * @param image		Splash-Image, das angezeigt wird
	 * @param version	Name und Versionsnummer
	 */
	public static void splash(Image image, String version) {
		Frame f = new Frame();
		instance = new SplashWindow(f, image, version);
		instance.setVisible(true);

		/**
		 * Note: To make sure the user gets a chance to see the splash window we wait until its paint 
		 * method has been called at least once by the AWT event dispatcher thread. If more than one
		 * processor is available, we don't wait, and maximize CPU throughput instead.
		 */
		if (!EventQueue.isDispatchThread() && Runtime.getRuntime().availableProcessors() == 1) {
			synchronized (instance) {
				while (!instance.paintCalled) {
					try {
						instance.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Zeigt ein Image als Splash-Screen an
	 * 
	 * @param image		Splash-Image, das angezeigt wird
	 * @param version	Name und Versionsnummer
	 */
	public static void splash(URL image, String version) {
		if (image != null) {
			splash(Toolkit.getDefaultToolkit().createImage(image), version);
		}
	}

	/** Schließt das Splash-Fenster */
	public static void disposeSplash() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// egal
		}
		if (instance != null) {
			FmtLogger.getLogger("ctSim").removeHandler(logHandler);
			instance.getOwner().dispose();
			instance = null;
		}
	}
	
	/**
	 * Zeigt eine Nachricht auf dem Splash-Screen an, löscht dabei die alte Nachricht
	 * 
	 * @param msg	Nachricht, die angezeigt werden soll
	 */
	public static void setMessage(String msg) {
		message = msg;	
		instance.paint(instance.getGraphics());
	}
	
	/**
	 * Gibt den Log-Handler zurück
	 * 
	 * @return unser Log-Handler
	 */
	public static Handler getLogHandler() {
		return logHandler;
	}
	
	/**
	 * @return Windows-Instanz
	 */
	public static Window getWindow() {
		return instance;
	}
	
	/**
	 * Log-Handler für den Splash-Screen, damit dort auch die Konsolenausgaben erscheinen
	 * 
	 * @author Timo Sandmann
	 */
	class SplashLogHandler extends Handler {
		/**
		 * @see java.util.logging.Handler#close()
		 */
		@Override
		public void close() throws SecurityException {
			// No-op
		}

		/**
		 * @see java.util.logging.Handler#flush()
		 */
		@Override
		public void flush() {
			// No-op
		}

		/**
		 * Schreibt die Log-Message in das Splash-Fenster
		 * 
		 * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
		 */
		@Override
		public void publish(LogRecord record) {
			if (record == null) return;
			if (record.getLevel().intValue() >= Level.INFO.intValue()) {
				SplashWindow.setMessage(record.getMessage());
			}
		}
	}
}
