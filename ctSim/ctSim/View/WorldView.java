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

package ctSim.View;

import ctSim.ErrorHandler;
import ctSim.Model.World;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;

// Zur Benutzung des org.j3d.ui.naviagtaion Paketes die Kommentarzeichen entfernen
//import org.j3d.ui.navigation.*;
//import java.awt.event.MouseEvent;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.vecmath.*;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Realisiert die Anzeige der Welt mit allen Hindernissen und Robotern
 * 
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Benjamin Benz (bbe@heise.de)
 */
public class WorldView extends JFrame {

	/** Dateiname fuer den Screenshot */
	private static final String SCREENSHOT ="screenshot.jpg";
	
	private static final long serialVersionUID = 1L;

	/** Die "Leinwand" fuer die 3D-Welt */
	private Canvas3D worldCanvas;

	/** Die TransformGroup fuer den Blickpunkt in die 3D-Welt */
	private TransformGroup tgViewPlatform;
	
//	 Zur Benutzung des org.j3d.ui.navigation-Paketes die Kommentarzeichen entfernen
//	/** Navigations Objekt */
//	private MouseViewHandler mvh;

	/** Das Universum */
	private SimpleUniverse universe;


	/** Internes Objekt zum Grabben des Screen. 
	 * Hat rein garnichts mit einem Roboter zu tun!!! */
	private Robot robot;
	
	/**
	 * Erzeugt ein neues Fenster zur Welt	  
	 */
	public WorldView() {
		super("c't-Sim");
				
		getContentPane().setLayout(new BorderLayout());
		this.setSize(500, 500);

        GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice dev = env.getDefaultScreenDevice();

        // Leinwand fuer die Welt erzeugen
		worldCanvas = new Canvas3D(dev.getBestConfiguration(template));

		this.getContentPane().add(worldCanvas);
		
		
		// wird zum ScreenCapture gebraucht
		try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
	}

	/**
	 * Erzeugt die GUI
	 */
	public void initGUI() {

		/* Hole die TransformGroup aus dem SimpleUniverse heraus */
		tgViewPlatform = universe.getViewingPlatform().getViewPlatformTransform() ;
		
		// Zur Benutzung des org.j3d.ui.navigation Paketes den folgenden Block auskommentieren
		// Start Block	
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();
        BoundingSphere mouseBounds = null;
		
		mouseBounds = new BoundingSphere(new Point3d(), 1000.0);

		MouseRotate myMouseRotate = new MouseRotate(MouseBehavior.INVERT_INPUT);
        myMouseRotate.setTransformGroup(tgViewPlatform);
        myMouseRotate.setSchedulingBounds(mouseBounds);
        myMouseRotate.setFactor(0.001d);
        objRoot.addChild(myMouseRotate);

        MouseTranslate myMouseTranslate = new MouseTranslate(MouseBehavior.INVERT_INPUT);
        myMouseTranslate.setTransformGroup(tgViewPlatform);
        myMouseTranslate.setSchedulingBounds(mouseBounds);
        myMouseTranslate.setFactor(0.01d);
        objRoot.addChild(myMouseTranslate);

        MouseZoom myMouseZoom = new MouseZoom(MouseBehavior.INVERT_INPUT);
        myMouseZoom.setTransformGroup(tgViewPlatform);
        myMouseZoom.setSchedulingBounds(mouseBounds);
        myMouseZoom.setFactor(0.05d);
        objRoot.addChild(myMouseZoom);
        
        universe.addBranchGraph(objRoot);
        // End Block

//      Zur Benutzung des org.j3d.ui.navigation-Paketes die Kommentarzeichen entfernen
//		/* erzeuge die Navigationsklasse */ 
//		mvh  = new MouseViewHandler();
//		
//		/* versorge die Navigationsklasse mit allen ben�tigten Informationen */
//		mvh.setCanvas(worldCanvas);
//		mvh.setViewInfo(universe.getViewer().getView(),tgViewPlatform);
//		
//		/* verbinde die Mouseevents mit den verschiedenen Bewegungsarten */
//		mvh.setButtonNavigation(MouseEvent.BUTTON1_MASK, NavigationState.WALK_STATE);
//		mvh.setButtonNavigation(MouseEvent.BUTTON2_MASK, NavigationState.FLY_STATE);
//		mvh.setButtonNavigation(MouseEvent.BUTTON3_MASK, NavigationState.EXAMINE_STATE);
//		mvh.setNavigationSpeed(1.0f);
//		
//		BranchGroup bgt = new BranchGroup();
//		bgt.addChild(mvh.getTimerBehavior());
//		universe.addBranchGraph(bgt);
		
		try {
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			worldCanvas.setVisible(true);
			this.setVisible(true);
			this.pack();
			this.repaint();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Gibt eine Referenz auf worldCanvas zurueck
	 */
	public Canvas3D getWorldCanvas() {
		return worldCanvas;
	}

	public void setScene(BranchGroup scene){
		SimpleUniverse simpleUniverse = new SimpleUniverse(getWorldCanvas());
		setUniverse(simpleUniverse);
		simpleUniverse.addBranchGraph(scene);
	}
	
	/**
	 * @param uni
	 *            Referenz auf das Universum, die gesetzt werden soll
	 */
	public void setUniverse(SimpleUniverse uni) {
		this.universe = uni;
		// Blickpunkt so weit zuruecksetzen, dass alles zu sehen ist.

		double biggerOne;
		if (World.PLAYGROUND_HEIGHT > World.PLAYGROUND_WIDTH)
			biggerOne = World.PLAYGROUND_HEIGHT;
		else
			biggerOne = World.PLAYGROUND_WIDTH;

		Transform3D translate = new Transform3D();
		universe.getViewingPlatform().getViewPlatformTransform().getTransform(translate);
		translate.setTranslation(new Vector3d(0d,0d,(biggerOne/2)/Math.tan(universe.getViewer().getView().getFieldOfView()/2)));
		universe.getViewingPlatform().getViewPlatformTransform().setTransform(translate);
	}

	
	/**
	 * Macht einen Screenshot der Roboterwelt und schreibt ihn auf die Platte
	 * Der Dateiname steht in der Konstanten:
	 */
	public void dumpScreen() {
        BufferedImage image = captureScreen();
        System.out.println("image " + image);
        File f = new File(SCREENSHOT);
        
        String type = f.getName().substring(f.getName().lastIndexOf('.') + 1);
        System.out.println("type " + type);
        try {
            ImageIO.write(image,type,f);
        } catch (IOException ioe) {
        	ErrorHandler.error("Fehler beim Sichern des Bilds: "+ioe);
        }
    }
	
	/**
	 * Liefert einen Screenshot der Roboterwelt
	 * @return das Bild
	 */
    public BufferedImage captureScreen() {
        BufferedImage image = new BufferedImage(getContentPane().getWidth(), getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
        getContentPane().paint(image.createGraphics());
        if(isVisible()) {
            dumpAWT(getContentPane(), image);
        }
        
        return image;
    }
    
    /*
     * Hilfsfunktion, um Bilder zu sichern
     * @param Container Die ContentPane des zu sichernden Frames
     * @param BufferedImage das Zielbild
     */
    private void dumpAWT(Container container, BufferedImage image) {
        for(int i = 0; i < container.getComponentCount(); i++) {
            Component child = container.getComponent(i);
            if(!(child instanceof JComponent)) {
                Rectangle bounds = child.getBounds();
                Point location = bounds.getLocation();
                bounds.setLocation(child.getLocationOnScreen());
                BufferedImage capture = robot.createScreenCapture(bounds);
                bounds.setLocation(location);
                SwingUtilities.convertRectangle(child, bounds, getContentPane());
                image.createGraphics().drawImage(capture, location.x, location.y, this);
 
                if(child instanceof Container) {
                    dumpAWT(container, image);
                }
            }
        }
    }
}
