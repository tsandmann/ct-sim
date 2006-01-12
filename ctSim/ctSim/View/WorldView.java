package ctSim.View;

import ctSim.Model.World;
import ctSim.Controller.Controller;

import java.awt.BorderLayout;
import java.awt.Color;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.behaviors.picking.PickRotateBehavior;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.image.TextureLoader;

import javax.media.j3d.*;
import javax.vecmath.*;

import javax.swing.JFrame;


/**
 * Realisiert die Anzeige der Welt mit allen Hindernissen und Robotern
 * 
 * @author pek (pek@ctmagazin.de)
 *
 */
public class WorldView extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private World world;
	
	/** Die "Leinwand" fuer die 3D-Welt */
	private Canvas3D worldCanvas;
	
	/**  Das Universum */
	private SimpleUniverse universe;
	
	/** Aussehen von Hindernissen */
	private Appearance obstacleAppear;
	
	/** Pfad zu einer Textur fuer die Hindernisse */
	public static final String OBST_TEXTURE = "textures/rock_wall.jpg";
	
	/** Aussehen des Bodens */
private Appearance playgroundAppear;
	
	public WorldView(World w) {
		super("Bot-Playground");
		
		// Welt uebernehmen
		world = w;
		
		getContentPane().setLayout(new BorderLayout());
		this.setSize(500, 500);

		// Leinwand fuer die Welt erzeugen
		worldCanvas = new Canvas3D(null);
		
		this.getContentPane().add(worldCanvas);
		
		// Aussehen des Bodens -- hellgrau:
		ColoringAttributes fieldAppCol = new ColoringAttributes((new Color3f(Color.LIGHT_GRAY)), ColoringAttributes.FASTEST);
		playgroundAppear = new Appearance();
		playgroundAppear.setColoringAttributes(fieldAppCol);

		// Aussehen der Hindernisse -- dunkelgrau:
		ColoringAttributes obstAppCol = new ColoringAttributes((new Color3f(Color.DARK_GRAY)), ColoringAttributes.FASTEST);
		obstacleAppear = new Appearance();
		obstacleAppear.setColoringAttributes(obstAppCol);

		// ...und mit einer Textur ueberzogen:
		TexCoordGeneration tcg = new TexCoordGeneration(TexCoordGeneration.OBJECT_LINEAR,
				TexCoordGeneration.TEXTURE_COORDINATE_3,
				new Vector4f(1.0f, 1.0f, 0.0f, 0.0f),
				new Vector4f(0.0f, 1.0f, 1.0f, 0.0f),
				new Vector4f(1.0f, 0.0f, 1.0f, 0.0f));
		obstacleAppear.setTexCoordGeneration(tcg);
		TextureLoader loader = new TextureLoader(OBST_TEXTURE, worldCanvas);
		Texture2D texture = (Texture2D)loader.getTexture();
		texture.setBoundaryModeS(Texture.WRAP);
		texture.setBoundaryModeT(Texture.WRAP);
		obstacleAppear.setTexture(texture);		
	}

	/**
	 * Erzeugt die GUI 
	 */
	public void initGUI() {
		try {		
			worldCanvas.setVisible(true);
			this.setVisible(true);
			this.pack();
			this.repaint();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Gibt worldCanvas zurueck.
	 */
	public Canvas3D getWorldCanvas() {
		return worldCanvas;
	}

	/**
	 * @param universe Wert fuer universe, der gesetzt werden soll.
	 */
	public void setUniverse(SimpleUniverse uni) {
		this.universe = uni;		
		universe.getViewingPlatform().setNominalViewingTransform();
	}

	/**
	 * @return Gibt das Erscheinungsbild der Hindernisse zurueck.
	 */
	public Appearance getObstacleAppear() {
		return obstacleAppear;
	}

	/**
	 * @return Gibt das Erscheinungsbild des Bodens zurueck.
	 */
	public Appearance getPlaygroundAppear() {
		return playgroundAppear;
	}
} 

