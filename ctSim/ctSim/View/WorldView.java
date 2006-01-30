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

import ctSim.Model.World;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.mouse.MouseBehavior;
import com.sun.j3d.utils.behaviors.mouse.MouseRotate;
import com.sun.j3d.utils.behaviors.mouse.MouseTranslate;
import com.sun.j3d.utils.behaviors.mouse.MouseZoom;
import com.sun.j3d.utils.image.TextureLoader;

// Zur Benutzung des org.j3d.ui.naviagtaion Paketes die Kommentarzeichen entfernen
//import org.j3d.ui.navigation.*;
//import java.awt.event.MouseEvent;

import javax.media.j3d.*;
import javax.vecmath.*;

import javax.swing.JFrame;

/**
 * Realisiert die Anzeige der Welt mit allen Hindernissen und Robotern
 * 
 * @author pek (pek@heise.de)
 * 
 */
public class WorldView extends JFrame {

	private static final long serialVersionUID = 1L;

	/** Die "Leinwand" fuer die 3D-Welt */
	private Canvas3D worldCanvas;

	/** Die TransformGroup fuer den Blickpunkt in die 3D-Welt */
	private TransformGroup tgViewPlatform;
	
//	 Zur Benutzung des org.j3d.ui.naviagtaion Paketes die Kommentarzeichen entfernen
//	/** Navigations Objekt */
//	private MouseViewHandler mvh;

	/** Das Universum */
	private SimpleUniverse universe;

	/** Aussehen von Hindernissen */
	private Appearance obstacleAppear;

	/** Pfad zu einer Textur fuer die Hindernisse */
	public static final String OBST_TEXTURE = "textures/rock_wall.jpg";
	
	/** Aussehen des Bodens */
	private Appearance playgroundAppear;

	/** Aussehen der Bots */
	private Appearance botAppear;

	/**
	 * Erzeugt ein neues Fenster zur Welt
	 * 
	 * @param w
	 *            Die Welt, die das Fenster darstellen soll
	 */
	public WorldView(World w) {
		super("c't-Sim");
				
		getContentPane().setLayout(new BorderLayout());
		this.setSize(500, 500);

		
        GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
        GraphicsEnvironment env =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice dev = env.getDefaultScreenDevice();

        // Leinwand fuer die Welt erzeugen
		worldCanvas = new Canvas3D(dev.getBestConfiguration(template));

		this.getContentPane().add(worldCanvas);

		// Aussehen des Bodens -- hellgrau:
		ColoringAttributes fieldAppCol = new ColoringAttributes((new Color3f(
				Color.LIGHT_GRAY)), ColoringAttributes.FASTEST);
		playgroundAppear = new Appearance();
		playgroundAppear.setColoringAttributes(fieldAppCol);

		// Aussehen der Hindernisse -- dunkelgrau:
		ColoringAttributes obstAppCol = new ColoringAttributes((new Color3f(
				Color.DARK_GRAY)), ColoringAttributes.FASTEST);
		obstacleAppear = new Appearance();
		obstacleAppear.setColoringAttributes(obstAppCol);

		// ...und mit einer Textur ueberzogen:
		TexCoordGeneration tcg = new TexCoordGeneration(
				TexCoordGeneration.OBJECT_LINEAR,
				TexCoordGeneration.TEXTURE_COORDINATE_3, new Vector4f(1.0f,
						1.0f, 0.0f, 0.0f),
				new Vector4f(0.0f, 1.0f, 1.0f, 0.0f), new Vector4f(1.0f, 0.0f,
						1.0f, 0.0f));
		obstacleAppear.setTexCoordGeneration(tcg);
		TextureLoader loader = new TextureLoader(ClassLoader.getSystemResource(OBST_TEXTURE), worldCanvas);
		Texture2D texture = (Texture2D) loader.getTexture();
		texture.setBoundaryModeS(Texture.WRAP);
		texture.setBoundaryModeT(Texture.WRAP);
		obstacleAppear.setTexture(texture);

		// Aussehen der Bots:
		botAppear = new Appearance(); // Bots sind rot ;-)
		botAppear.setColoringAttributes(new ColoringAttributes(new Color3f(
				Color.RED), ColoringAttributes.FASTEST));
	}

	/**
	 * Erzeugt die GUI
	 */
	public void initGUI() {

		/* hole die TransformGroup aus dem SimpleUniverse heraus */
		tgViewPlatform = universe.getViewingPlatform().getViewPlatformTransform() ;
		
//		 Zur Benutzung des org.j3d.ui.naviagtaion Paketes den folgenden Block auskommentieren
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

//      Zur Benutzung des org.j3d.ui.naviagtaion Paketes die Kommentarzeichen entfernen
//		/* erzeuge die Navigationsklasse */ 
//		mvh  = new MouseViewHandler();
//		
//		/* versorge die Navigationsklasse mit allen benötigten Informationen */
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

	/**
	 * @param uni
	 *            Referenz auf das Universum, die gesetzt werden soll
	 */
	public void setUniverse(SimpleUniverse uni) {
		this.universe = uni;
		universe.getViewingPlatform().setNominalViewingTransform();
	}

	/**
	 * @return Gibt das Erscheinungsbild der Hindernisse zurueck
	 */
	public Appearance getObstacleAppear() {
		return obstacleAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild des Bodens zurueck
	 */
	public Appearance getPlaygroundAppear() {
		return playgroundAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild der Bots zurueck
	 */
	public Appearance getBotAppear() {
		return botAppear;
	}
}
