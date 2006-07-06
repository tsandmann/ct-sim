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

public class WorldPanel extends JPanel {
	
	private WorldView view;
	private World world;
	
	WorldPanel() {
		
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		this.view = new WorldView();
		
		this.add(this.view);
		
		// TODO:
		//this.setPreferredSize(new Dimension(800, 500));
	}
	
	protected void setWorld(World world) {
		
		this.world = world;
		
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
	
	protected void update() {
		
		// TODO: ???
		//SceneUpdate su = new SceneUpdate(this.world.getSceneLight());
		
		// RemoteViews benachrichtigen (siehe alter Controller.update())
		
		this.view.repaint();
		
		//System.out.println(this.view.worldCanvas.getSize());
		//System.out.println(this.getSize());
	}
}
