package ctSim.view.gui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
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

//$$ doc WorldViewer
//$$ Kandidat fuer architektonischen Umbau: Wieso instanziiert _diese Klasse_ das Universe? Gehoert das nicht ins Model?
public class WorldViewer extends JPanel implements ScreenshotProvider {
    private static final long serialVersionUID = - 2085097216130758546L;

    private static final String NOTHING = "showing nothing";
    private static final String MODEL = "showing a model";

    // Hilfsding fuer die J3D-Buerokratie
    //$$ ist SimpleUniverse.getPreferredConfiguration() besser?
    protected GraphicsConfiguration gc = GraphicsEnvironment.
	    getLocalGraphicsEnvironment().getDefaultScreenDevice().
	    getBestConfiguration(new GraphicsConfigTemplate3D());

    protected final Canvas3D onScreenCanvas = new Canvas3D(gc, false);
    protected final Canvas3D offScreenCanvas = new Canvas3D(gc, true);

    // wird mit jedem neuen Model ausgetauscht
    protected SimpleUniverse universe;

    public WorldViewer() {
    	setLayout(new CardLayout());
    	JPanel p = new JPanel(new GridBagLayout()); // nur zum Zentrieren
    	p.add(new JLabel("Keine Welt geladen"));
    	add(p, NOTHING);
    	add(onScreenCanvas, MODEL);
    	Main.dependencies.registerInstance(ScreenshotProvider.class, this);
    }

    public void show(World world) {
    	deinit();

    	if (world == null)
    		((CardLayout)getLayout()).show(this, NOTHING); //$$ falscher platz, vgl MODEL
    	else
    		init(world);
    }

    /**
	 * <p>
	 * Altes Universum deinitialisieren (falls vorhanden), inkl. seiner
	 * Java3D-Threads. Es ist enorm wichtig, diese Methode aufzurufen!
	 * </p>
	 * <p>
	 * Mit jedem Aufruf von {@link #init(World)} werden ca. ein Dutzend
	 * Java3D-Threads instanziiert, die ohne <code>deinit()</code>
	 * dann nie mehr beendet w&uuml;rden und ein massives Ressourcenleck (v.a.
	 * Speicherleck wegen Referenzen auf alte Welten, Bots, etc.) darstellen.
	 * Bei h&auml;ufigem Welt-Wechsel, etwa in Wettbewerbssituationen, hat das
	 * gruselige Auswirkungen.
	 * </p>
	 */
	protected void deinit() {
    	if (universe != null)
    		universe.cleanup();
    }

    protected void init(World w) {
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
		 * Fenster über den ctSim geschoben hat, wird eben das abgelichtet. Die
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

    /**
	 * MinimumSize ist (1,1): Erm&ouml;glicht der SplitPane, die uns
	 * enth&auml;lt, ihren Divider zu &auml;ndern. W&uuml;rden wir unsere
	 * MinimumSize nicht auf was sehr kleines setzen, ginge das nicht.
	 */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(1,1);
    }
}
