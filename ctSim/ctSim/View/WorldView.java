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
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.image.TextureLoader;

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

		// Leinwand fuer die Welt erzeugen
		worldCanvas = new Canvas3D(null);

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
