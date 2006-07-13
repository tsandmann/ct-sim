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
package ctSim.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Robot;
import java.util.List;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.TransformGroup;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.sun.j3d.utils.universe.SimpleUniverse;

import ctSim.model.World;
import ctSim.model.bots.Bot;
//import ctSim.model.scene.SceneUpdate;

/**
 * Packt den Blick auf die Roboterwelt in ein Panel
 * @author Felix Beckwermert
 *
 */
public class WorldPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private WorldView view;
	private World world;
	
	/**
	 * Der Konstruktor
	 */
	WorldPanel() {
		
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		this.view = new WorldView();
		
		this.add(this.view);
		
		// TODO:
		//this.setPreferredSize(new Dimension(800, 500));
	}
	
	/**
	 * @param w Die Welt, auf die eine Referenz gesetzt werden soll
	 */
	protected void setWorld(World w) {
		
		this.world = w;
		
		this.view.init();
		
		this.view.setScene(//this.world.getSceneLight().getScene(),
						   this.world.getScene(),
						   this.world.getPlaygroundDimX(),
						   this.world.getPlaygroundDimY());
		this.view.initGUI();
		
		// TODO: ???
		//this.world.getSceneLight().addReference("World_"+Bot.VP,this.view.getUniverse().getViewingPlatform().getViewPlatform());
		this.world.addViewPlatform(this.view.getUniverse().getViewingPlatform().getViewPlatform());
		
		//this.world.start();
		// TODO: ...
	}
	
	/**
	 * Aktualisiert die GUI
	 */
	protected void update() {
		
		// TODO: ???
		//SceneUpdate su = new SceneUpdate(this.world.getSceneLight());
		
		// RemoteViews benachrichtigen (siehe alter Controller.update())
		
		this.view.repaint();
		
		//System.out.println(this.view.worldCanvas.getSize());
		//System.out.println(this.getSize());
	}
}
