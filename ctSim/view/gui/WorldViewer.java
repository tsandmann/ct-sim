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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.ImageComponent2D;
import javax.media.j3d.Screen3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseWheelZoom;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.universe.SimpleUniverse;

import ctSim.controller.Main;
import ctSim.model.World;
import ctSim.view.ScreenshotProvider;

/**
 * Klasse zur Darstellung einer Welt
 */
public class WorldViewer extends JPanel implements ScreenshotProvider {
    /** UID */
	private static final long serialVersionUID = - 2085097216130758546L;
	/** nichts-Meldung */
    private static final String NOTHING = "showing nothing";
    /** model-Meldung */
    private static final String MODEL = "showing a model";

    /** Hilfsding für die J3D-Bürokratie */
    final protected GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getBestConfiguration(new GraphicsConfigTemplate3D());
    /** Das letzte verwendete GraphicsDevice, wird für das Erzeugen einer neuen Welt in einem Multi-Monitor Setup benötigt */
    protected GraphicsDevice last_gd = gc.getDevice();

    /** OnScreen Fläche */
    protected Canvas3D onScreenCanvas = new Canvas3D(gc, false);
    /** OffScreen Fläche */
    protected Canvas3D offScreenCanvas = new Canvas3D(gc, true);

    /** wird mit jedem neuen Model ausgetauscht */
    protected SimpleUniverse universe;
    
	/**
	 * Gets the best graphics conifguration to display on the current device.
	 * 
	 * @return GraphicsConfiguration
	 */
	protected GraphicsConfiguration getBestConfigurationOnSameDevice() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		GraphicsConfiguration good = null;

		GraphicsConfigTemplate3D gct = new GraphicsConfigTemplate3D();

		for (GraphicsDevice newgd : gs) {
			if (newgd == last_gd) {
				good = gct.getBestConfiguration(newgd.getConfigurations());
				if (good != null) {
					break;
				}
			}
		}

		return good;
	}

    /**
     * Erzeugt einen neuen WorldViewer, der erstmal leer ist
     */
    public WorldViewer() {
    	setLayout(new CardLayout());
    	JPanel p = new JPanel(new GridBagLayout()); // nur zum Zentrieren
    	p.add(new JLabel("Keine Welt geladen"));
    	add(p, NOTHING);
    	add(onScreenCanvas, MODEL);
    	Main.dependencies.registerInstance(ScreenshotProvider.class, this);
    }

    /**
     * Zeigt eine Welt an, oder einen dummy, falls world == null
     * @param world	Die Welt
     */
    public void show(World world) {
    	deinit();

    	if (world == null) {
    		((CardLayout)getLayout()).show(this, NOTHING);
    	} else {
    		init(world);
    		/* einmal rendern erzwingen, damit sofort die Welt angezeit wird */
    		universe.getViewer().getCanvas3D().print(universe.getCanvas().getGraphics());
    	}
    }

    /**
	 * <p>
	 * Altes Universum deinitialisieren (falls vorhanden), inkl. seiner
	 * Java3D-Threads. Es ist enorm wichtig, diese Methode aufzurufen!
	 * </p>
	 * <p>
	 * Mit jedem Aufruf von {@link #init(World)} werden ca. ein Dutzend
	 * Java3D-Threads instanziiert, die ohne <code>deinit()</code>
	 * dann nie mehr beendet würden und ein massives Ressourcenleck (v.a.
	 * Speicherleck wegen Referenzen auf alte Welten, Bots, etc.) darstellen.
	 * Bei häufigem Welt-Wechsel, etwa in Wettbewerbssituationen, hat das
	 * gruselige Auswirkungen.
	 * </p>
	 */
	protected void deinit() {
		if (onScreenCanvas != null) {
			last_gd = onScreenCanvas.getGraphicsConfiguration().getDevice();
		}
    	if (universe != null) {
    		universe.cleanup();
    	}
		/* remove all Java3D Canvas components */
		while (getComponentCount() > 1) {
			remove(1);
		}
		universe = null;
		onScreenCanvas = null;
		offScreenCanvas = null;
    }

    /**
     * Initialisiert eine Welt
     * @param w die Welt
     */
    protected void init(World w) {    	
    	onScreenCanvas = new Canvas3D(getBestConfigurationOnSameDevice() /*GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getBestConfiguration(new GraphicsConfigTemplate3D())*/, false);
    	offScreenCanvas = new Canvas3D(getBestConfigurationOnSameDevice() /*GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getBestConfiguration(new GraphicsConfigTemplate3D())*/, true);
    	add(onScreenCanvas, MODEL);
    	universe = new SimpleUniverse(onScreenCanvas);
        universe.getViewer().getView().addCanvas3D(offScreenCanvas);
    	initPerspective(w);
    	universe.addBranchGraph(w.getScene());

    	/* Aktivieren von:
    	 * Ziehen/linke Maustaste = Ansicht rotieren
    	 * Ziehen/rechte Maustaste = Ansicht verschieben
    	 * Ziehen/mittlere Maustaste = Ansicht zoomen
    	 * Mausrad = Ansicht zoomen
    	 */
    	universe.addBranchGraph(new BranchGroup() {
    		{
    			/*
				 * Syntaxhinweis zu den doppelten Schweifklammern: Es handelt
				 * sich um "instance initializer", die den Code kompakter machen
				 * sollen -- siehe JLS §8.6 und
				 * http://www.c2.com/cgi/wiki?DoubleBraceInitialization
				 */
    			addMouseBehavior(new MouseRotate(MouseBehavior.INVERT_INPUT)
    				{{ setFactor(0.001); }});
    			addMouseBehavior(new MouseTranslate(MouseBehavior.INVERT_INPUT)
    				{{ setFactor(0.01); }});
    			addMouseBehavior(new MouseZoom()
    				{{ setFactor(0.035); }});
    			addMouseBehavior(new MouseWheelZoom()
    				{{ setFactor(0.3); }});
    		}

    		private void addMouseBehavior(MouseBehavior b) {
    			b.setTransformGroup(
    				universe.getViewingPlatform().getViewPlatformTransform());
    			b.setSchedulingBounds(new BoundingSphere(new Point3d(), 1000)); //LODO Könnte besser sein: Was, wenn irgendne Welt mal > 1000 ist?
    			addChild(b);
    		}
    	});
		w.addViewPlatform(universe.getViewingPlatform().getViewPlatform());

		((CardLayout)getLayout()).show(this, MODEL);
    }

    /**
     * Position: X/Y: zentriert, Z: so, dass der Parcours
     * vollständig im Blick ist
     * @param w Welt
     */
    private void initPerspective(World w) {
    	double centerX = w.getWidthInM() / 2;
    	double centerY = w.getHeightInM() / 2;
    	double longerSide = Math.max(
    		w.getWidthInM(), w.getHeightInM());
    	// Winkel im Bogenmass
    	double field = universe.getViewer().getView().getFieldOfView();

    	Transform3D targetTfm = new Transform3D();
    	TransformGroup tg =
    		universe.getViewingPlatform().getViewPlatformTransform();

    	tg.getTransform(targetTfm);
    	targetTfm.setTranslation(new Vector3d(centerX, centerY,
    		(longerSide/2) / Math.tan(field/2)));
    	tg.setTransform(targetTfm);
    }

    /**
     * Baut einen Screenshot
     * @return	null wenn nix geladen, sonst Screenshot
	 * Bsp: ImageIO.write(worldViewer.getScreenshot(), "png", File.createTempFile("screenshot", ".png"));
     */
    public BufferedImage getScreenshot() {
    	/*
		 * Das hier ist sehr bürokratisch, aber leider hab ich keinen
		 * einfacheren Weg gefunden. Was man mit Google findet sind i.d.R.
		 * Beispiele, die java.awt.Robot.createScreenCapture() verwenden -- was
		 * aber einen "echten Screenshot" macht, d.h. wenn einer ein anderes
		 * Fenster über den ctSim geschoben hat, wird eben das abgelichtet. Die
		 * Alternative, auf dem Canvas3D oder auf diesem Objekt paint(...)
		 * aufzurufen, funktioniert nicht (ergibt immer einfarbig schwarze
		 * Screenshots -- vermutlich ist Canvas3D eben doch keine ganz so
		 * normale Swing-Komponente).
		 */
    	Screen3D onScr = onScreenCanvas.getScreen3D();
    	Screen3D offScr = offScreenCanvas.getScreen3D();
    	offScreenCanvas.setOffScreenBuffer(new ImageComponent2D(ImageComponent2D.FORMAT_RGB, onScr.getSize().width, onScr.getSize().height));
    	offScr.setSize(onScr.getSize());
    	offScr.setPhysicalScreenHeight(onScr.getPhysicalScreenHeight());
    	offScr.setPhysicalScreenWidth(onScr.getPhysicalScreenWidth());
    	offScreenCanvas.renderOffScreenBuffer();
    	offScreenCanvas.waitForOffScreenRendering();
    	return offScreenCanvas.getOffScreenBuffer().getImage();
    }

    /**
	 * MinimumSize ist (1,1): Ermöglicht der SplitPane, die uns
	 * enthält, ihren Divider zu ändern. Würden wir unsere
	 * MinimumSize nicht auf was sehr kleines setzen, ginge das nicht.
	 */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(1,1);
    }
}
