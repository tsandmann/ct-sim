package ctSim.view.gui;

import java.awt.CardLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
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

import ctSim.model.World;

//$$ doc WorldViewer
//$$ Kandidat fuer architektonischen Umbau: Wieso instanziiert _diese Klasse_ das Universe? Gehoert das nicht ins Model?
public class WorldViewer extends JPanel {
    private static final long serialVersionUID = - 2085097216130758546L;

    private static final String NOTHING = "showing nothing";
    private static final String MODEL = "showing a model";

    private final Canvas3D onScreenCanvas;
    private final Canvas3D offScreenCanvas;

    // wird mit jedem neuen Model ausgetauscht
	private SimpleUniverse universe;

    public WorldViewer() {
    	setLayout(new CardLayout());

    	JPanel p = new JPanel(new GridBagLayout()); // nur zum Zentrieren
    	p.add(new JLabel("Keine Welt geladen"));
    	add(p, NOTHING);

    	GraphicsConfiguration gc = GraphicsEnvironment.
	    	getLocalGraphicsEnvironment().getDefaultScreenDevice().
	    	getBestConfiguration(new GraphicsConfigTemplate3D());
    	onScreenCanvas = new Canvas3D(gc, false);
    	offScreenCanvas = new Canvas3D(gc, true);
    	add(onScreenCanvas, MODEL);
    }

    public void show(World world) {
    	deinit();

    	if (world == null)
    		((CardLayout)getLayout()).show(this, NOTHING);
    	else
    		init(world);
    }

    /* Altes Universum weg, inkl. seiner J3D-Threads.
	 * Dieser Aufruf ist enorm wichtig -- wir instanziieren mit
	 * jedem Aufruf von init() ca. ein Dutzend Java3D-Threads, die
	 * nie mehr beendet wuerden ohne universe.cleanup() */
	private void deinit() {
    	if (universe != null)
    		universe.cleanup();
    }

    private void init(World w) {
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
    			/* Syntaxhinweis zu den doppelten Schweifklammern: Es handelt
    			 * sich um "instance initializer", die hier den Code kompakter
    			 * und unbuerokratischer machen sollen -- siehe JLS �8.6 */
    			addMouseBehavior(new MouseRotate(MouseBehavior.INVERT_INPUT)
    				{{ setFactor(0.001); }});
    			addMouseBehavior(new MouseTranslate(MouseBehavior.INVERT_INPUT)
    				{{ setFactor(0.01); }});
    			addMouseBehavior(new MouseZoom()
    				{{ setFactor(0.035); }});
    			addMouseBehavior(new MouseWheelZoom()
    				{{ setFactor(0.3); }});
    		}

    		@SuppressWarnings("synthetic-access")
            private void addMouseBehavior(MouseBehavior b) {
    			b.setTransformGroup(
    				universe.getViewingPlatform().getViewPlatformTransform());
    			b.setSchedulingBounds(new BoundingSphere(new Point3d(), 1000)); //LODO Koennte besser sein: Was, wenn irgendne Welt mal > 1000 ist?
    			addChild(b);
    		}
    	});
		w.addViewPlatform(universe.getViewingPlatform().getViewPlatform());

		((CardLayout)getLayout()).show(this, MODEL);
    }

    // Position: X/Y: zentriert, Z: so, dass der Parcours
    // vollstaendig im Blick ist
    private void initPerspective(World m) {
    	double centerX = m.getWidthInM() / 2;
    	double centerY = m.getHeightInM() / 2;
    	double longerSide = Math.max(
    		m.getWidthInM(), m.getHeightInM());
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

    // null wenn nix geladen
    // Bsp: ImageIO.write(worldViewer.getScreenshot(), "png", File.createTempFile("screenshot", ".png"));
    public BufferedImage getScreenshot() {
    	/*
		 * Das hier ist sehr buerokratisch, aber leider hab ich keinen
		 * einfacheren Weg gefunden. Was man mit Google findet sind i.d.R.
		 * Beispiele, die java.awt.Robot.createScreenCapture() verwenden -- was
		 * aber einen "echten Screenshot" macht, d.h. wenn einer ein anderes
		 * Fenster �ber den ctSim geschoben hat, wird eben das abgelichtet. Die
		 * Alternative, auf dem Canvas3D oder auf diesem Objekt paint(...)
		 * aufzurufen, funktioniert nicht (ergibt immer einfarbig schwarze
		 * Screenshots -- vermutlich ist Canvas3D eben doch keine ganz so
		 * normale Swing-Komponente).
		 */
    	Screen3D onScr = onScreenCanvas.getScreen3D();
    	Screen3D offScr = offScreenCanvas.getScreen3D();
    	offScreenCanvas.setOffScreenBuffer(new ImageComponent2D(
    		ImageComponent2D.FORMAT_RGB,
    		onScr.getSize().width, onScr.getSize().height));
    	offScr.setSize(onScr.getSize());
    	offScr.setPhysicalScreenHeight(onScr.getPhysicalScreenHeight());
    	offScr.setPhysicalScreenWidth(onScr.getPhysicalScreenWidth());
    	offScreenCanvas.renderOffScreenBuffer();
    	offScreenCanvas.waitForOffScreenRendering();
    	return offScreenCanvas.getOffScreenBuffer().getImage();
    }

    public void update() {
		repaint();
    }
}