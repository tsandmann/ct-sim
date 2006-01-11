package ctSim.View;

import ctSim.Model.World;

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
 * This code was edited or generated using CloudGarden's Jigloo SWT/Swing GUI
 * Builder, which is free for non-commercial use. If Jigloo is being used
 * commercially (ie, by a corporation, company or business for any purpose
 * whatever) then you should purchase a license for each developer using Jigloo.
 * Please visit www.cloudgarden.com for details. Use of Jigloo implies
 * acceptance of these licensing terms. A COMMERCIAL LICENSE HAS NOT BEEN
 * PURCHASED FOR THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED LEGALLY FOR
 * ANY CORPORATE OR COMMERCIAL PURPOSE.
 */

public class WorldView extends JFrame {

	/**
	 * Realisiert die Anzeige der Welt mit allen Hindernissen, Robotern usw.
	 */

	private static final long serialVersionUID = 1L;
	
	public static final String WALL_TEXTURE = "textures/rock_wall.jpg";

//	private WorldPanel worldPanel;
	private Canvas3D worldCanvas;
	public BranchGroup scene, obstBG;
	/** TransformGroup in der sich alles abspielt. hier kommen die Bots hinein */
	private TransformGroup worldTG;
	private SimpleUniverse simpleUniverse;
	private float playH, playW;
	
	public WorldView() {
		super("Bot-Playground");
		getContentPane().setLayout(new BorderLayout());
		this.setSize(500, 500);
		// Gr��e der Welt in Metern:
		playH = (float)World.PLAYGROUND_HEIGHT;
		playW = (float)World.PLAYGROUND_WIDTH;
		worldCanvas = new Canvas3D(null);

		// SimpleUniverse is a Convenience Utility class
		simpleUniverse = new SimpleUniverse(worldCanvas);

		// Create our Scene
		scene = createSceneGraph(worldCanvas);
		
//		scene.compile();
		
//		TransformGroup tg = new TransformGroup(new Transform3D());
//		tg.addChild(new ColorCube(0.4));
//		addBot(tg);
//		scene.compile();

		
		simpleUniverse.getViewingPlatform().setNominalViewingTransform();

		simpleUniverse.addBranchGraph(scene);

		this.getContentPane().add(worldCanvas);
		initGUI();
	}

	private void initGUI() {
		try {		
			worldCanvas.setVisible(true);
			this.setVisible(true);
			this.pack();
			this.repaint();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BranchGroup createSceneGraph(Canvas3D canvas) {
		// Create the root of the branch graph
		BranchGroup objRoot = new BranchGroup();
		
 //       worldTG = null;
        PickRotateBehavior pickRotate = null;
        Transform3D transform = new Transform3D();
        BoundingSphere behaveBounds = new BoundingSphere();
        
        // TODO Diesen deprecated Dreh-Code Durch echte Navigation ersetzen. 
        // TODO Dazu das Packet Navigation Packet von http://code.j3d.org/ verwenden 
		// Alles um einen konstanten z-Wert nach hinten verschieben,
		// damit die Welt zu sehen ist!!
        // Und erlauben es zu drehen

        pickRotate = new PickRotateBehavior(objRoot, canvas, behaveBounds);
        objRoot.addChild(pickRotate);
        
        transform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
        worldTG = new TransformGroup(transform);
        worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        worldTG.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        worldTG.setPickable(true);

        objRoot.addChild(worldTG);

		// Der Boden der Roboterwelt ist hellgrau:
		ColoringAttributes fieldAppCol = new ColoringAttributes((new Color3f(Color.LIGHT_GRAY)), ColoringAttributes.FASTEST);
		Appearance fieldApp = new Appearance();
		fieldApp.setColoringAttributes(fieldAppCol);

		// Der Boden selbst ist ein sehr flacher Quader:
		Box floor = new Box(playW, playH, (float)0.01, fieldApp);
		floor.setPickable(false);		
		worldTG.addChild(floor);
		
		// Die Tranformgroup für alle Hindernisse
		obstBG = new BranchGroup(); 
		obstBG.setPickable(true);

		// Vier quaderf�rmige, dunkelgraue Hindernisse begrenzen die Roboterwelt:
		ColoringAttributes obstAppCol = new ColoringAttributes((new Color3f(Color.DARK_GRAY)), ColoringAttributes.FASTEST);
		Appearance obstApp = new Appearance();
		obstApp.setColoringAttributes(obstAppCol);

		// Textur
		TexCoordGeneration tcg = new TexCoordGeneration(TexCoordGeneration.OBJECT_LINEAR,
				TexCoordGeneration.TEXTURE_COORDINATE_3,
				new Vector4f(1.0f, 1.0f, 0.0f, 0.0f),
				new Vector4f(0.0f, 1.0f, 1.0f, 0.0f),
				new Vector4f(1.0f, 0.0f, 1.0f, 0.0f));
		obstApp.setTexCoordGeneration(tcg);
		TextureLoader loader = new TextureLoader(WALL_TEXTURE, canvas);
		Texture2D texture = (Texture2D)loader.getTexture();
		texture.setBoundaryModeS(Texture.WRAP);
		texture.setBoundaryModeT(Texture.WRAP);
		obstApp.setTexture(texture);		
		
		// Die vier Hindernisse:
		Box north = new Box(playW + (float)0.2, (float)0.1, (float)0.2, obstApp);
		north.setPickable(true);
		north.setName("North");
		Box south = new Box(playW + (float)0.2, (float)0.1, (float)0.2,obstApp);
		south.setPickable(true);
		south.setName("South");
		Box east = new Box((float)0.1, playH + (float)0.2, (float)0.2,obstApp);
		east.setPickable(true);
		east.setName("East");
		Box west = new Box((float)0.1, playH + (float)0.2, (float)0.2,obstApp);
		west.setPickable(true);
		west.setName("West");

		// Hindernisse werden an die richtige Position geschoben:
		Transform3D translate = new Transform3D();

		translate.set(new Vector3f((float)0, playH + (float)0.1, (float)0.2));
		TransformGroup tg1 = new TransformGroup(translate);
        tg1.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tg1.setPickable(true);
		tg1.addChild(north);
		obstBG.addChild(tg1);
		
		translate.set(new Vector3f((float)0, -(playH + (float)0.1), (float)0.2));
		TransformGroup tg2 = new TransformGroup(translate);
        tg2.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        tg2.setPickable(true);
        tg2.addChild(south);
		obstBG.addChild(tg2);

		translate.set(new Vector3f(playW + (float)0.1, (float)0, (float)0.2));
		TransformGroup tg3 = new TransformGroup(translate);
        tg3.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        tg3.setPickable(true);
        tg3.addChild(east);
		obstBG.addChild(tg3);

		translate.set(new Vector3f(-(playW + (float)0.1), (float)0, (float)0.2));
		TransformGroup tg4 = new TransformGroup(translate);
        tg4.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
        tg4.setPickable(true);
        tg4.addChild(west);
        obstBG.addChild(tg4);
        
        
		obstBG.compile();
		
		// Die Hindernisse der Welt hinzufügen
		worldTG.addChild(obstBG);
		// es dürfen noch weitere dazukommen
		worldTG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		return objRoot;
	}
	
	/** Fügt einen Bot dem Universum hinzu */
	public void addBot(BranchGroup bot){
		worldTG.addChild(bot);
	}
		
	public void reactToChange(){
		//TODO: implement me!
	}

	/**
	 * @return Returns the worldTG.
	 */
	public TransformGroup getWorldTG() {
		return worldTG;
	}

	/**
	 * @return Returns the scene.
	 */
	public BranchGroup getScene() {
		return scene;
	}
} 

