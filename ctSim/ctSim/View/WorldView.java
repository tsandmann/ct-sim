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
	
	public WorldView(World w) {
		super("Bot-Playground");
		world = w;

		getContentPane().setLayout(new BorderLayout());
		this.setSize(500, 500);

		worldCanvas = new Canvas3D(null);
		
		this.getContentPane().add(worldCanvas);

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
} 

