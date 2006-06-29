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

package ctSim.model;

import java.io.File;
import java.io.IOException;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.xml.sax.SAXException;

import ctSim.Model.AliveObstacle;
import ctSim.model.Parcours;
import ctSim.model.ParcoursLoader;
import ctSim.model.scene.SceneLight;

/**
 * Welt-Modell, kuemmert sich um die globale Simulation und das Zeitmanagement
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Werner Pirkl (morpheus.the.real@gmx.de)
 */
public class World_New {
	
	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_THICKNESS = 0f;
	
	private TransformGroup worldTG;
	
	private BranchGroup lightBG, terrainBG;
	
	private SceneLight sceneLight, sceneLightBackup;
	
	private Parcours parcours;
	
	public World_New() {
		
		this.sceneLight = new SceneLight();
		
		init();
		
		this.sceneLightBackup = this.sceneLight.clone();
	}
	
	public World_New(Parcours parcours) {
		
		this.parcours = parcours;
		
		this.sceneLight = new SceneLight();
		
		init();
		setParcours(parcours);
		
		this.sceneLightBackup = this.sceneLight.clone();
		
		this.sceneLight.getScene().compile();
	}
	
	/**
	 * @return Gibt eine Referenz auf sceneLight zurueck
	 * @return Gibt den Wert von sceneLight zurueck
	 */
	public SceneLight getSceneLight() {
		return this.sceneLight;
	}
	
	/**
	 * X-Dimension des Spielfldes im mm
	 * @return
	 */
	public float getPlaygroundDimX() {
		return this.parcours.getWidth();
	}

	/**
	 * Y-Dimension des Spielfldes im mm
	 * @return
	 */
	public float getPlaygroundDimY() {
		return this.parcours.getHeight();
	}
	
	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * @param parcoursFile Dateinamen des Parcours
	 * @return der Szenegraph
	 */
	private void setParcours(Parcours parcours) {
		
		this.parcours = parcours;
		// Hindernisse werden an die richtige Position geschoben

		
		// Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau
		// buendig mit der Unterkante des Bodens ist:
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup obstTG = new TransformGroup(translate);
		obstTG.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		obstTG.setPickable(true);
		this.sceneLight.getObstBG().addChild(obstTG);

	    obstTG.addChild(parcours.getObstBG());
	    this.lightBG.addChild(parcours.getLightBG());
	    this.terrainBG.addChild(parcours.getTerrainBG());		
		
	    this.sceneLight.getObstBG().setCapability(Node.ENABLE_PICK_REPORTING);
	    this.sceneLight.getObstBG().setCapability(Node.ALLOW_PICKABLE_READ);

		// Die Hindernisse der Welt hinzufuegen
		this.worldTG.addChild(this.sceneLight.getObstBG());
		// es duerfen noch weitere dazukommen
		this.worldTG.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
	}
	
	private void init() {

		// Die Wurzel des Ganzen:
		BranchGroup root = new BranchGroup();
		root.setName("World");
		root.setUserData(new String("World"));
		
		Transform3D worldTransform = new Transform3D();
		worldTransform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		this.worldTG = new TransformGroup(worldTransform);
		
		this.worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		this.worldTG.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		this.worldTG.setCapability(TransformGroup.ALLOW_PICKABLE_READ);
		this.worldTG.setPickable(true);

		root.addChild(this.worldTG);
		
		// Lichtquellen einfuegen
		// Streulicht (ambient light)
		BoundingSphere ambientLightBounds = 
			new BoundingSphere(new Point3d(0d, 0d, 0d), 100d);
		Color3f ambientLightColor = new Color3f(0.3f, 0.3f, 0.3f);
		AmbientLight ambientLightNode = new AmbientLight(ambientLightColor);
		ambientLightNode.setInfluencingBounds(ambientLightBounds);
		ambientLightNode.setEnable(true);
		this.worldTG.addChild(ambientLightNode);
		
		// Die Branchgroup fuer die Lichtquellen
		this.lightBG = new BranchGroup();
		this.lightBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		this.lightBG.setPickable(true);
		this.worldTG.addChild(lightBG);

		// Die Branchgroup fuer den Boden
		this.terrainBG = new BranchGroup();
		this.terrainBG.setCapability(TransformGroup.ALLOW_PICKABLE_WRITE);
		this.terrainBG.setPickable(true);
		this.worldTG.addChild(terrainBG);
		
		// Die TranformGroup fuer alle Hindernisse:
		this.sceneLight.setObstBG(new BranchGroup());
		// Damit spaeter Bots hinzugefuegt werden koennen:
		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_DETACH);
		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		this.sceneLight.getObstBG().setCapability(BranchGroup.ALLOW_PICKABLE_WRITE);

		
		// Objekte sind fest
		this.sceneLight.getObstBG().setPickable(true);
		
		this.sceneLight.setScene(root);
	}
	
	/**
	 * Entfernt einen Bot wieder
	 * @param bot
	 */
	public void remove(AliveObstacle obst){
		aliveObstacles.remove(obst);
		sceneLight.removeBot(obst.getName());
		sceneLightBackup.removeBot(obst.getName());
	}
	
	public static World parseWorldFile(File file)
			throws SAXException, IOException {
		
		ParcoursLoader pL = new ParcoursLoader();
		pL.load_xml_file(file.getAbsolutePath());
		Parcours parcours = pL.getParcours();
		
		return new World(parcours);
	}
}
