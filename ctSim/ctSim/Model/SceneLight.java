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
package ctSim.Model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.NodeReferenceTable;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import com.sun.j3d.utils.scenegraph.io.NamedObjectException;
import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamReader;
import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamWriter;

import ctSim.ErrorHandler;
import ctSim.View.ViewBotUpdate;

/**
 * Dies ist ein Container-Objekt fuer einen Szenegraphen und alle Hooks, die man
 * braucht, um die Welt anzuzeigen
 * 
 * @author bbe (bbe@heise.de)
 * 
 */
public class SceneLight {
	/** Sammelt Einsprungpunkte in den Szenegraphen */
	HashMap map = new HashMap();
	
	/** Der eigentliche Szenegraph */
	private BranchGroup scene;

	/**
	 * Initialisiere das Objekt
	 */
	public SceneLight() {
		super();
		scene = null;
	}

	/**
	 * Initialisiere das Objekt
	 * 
	 * @param scene
	 *            Die Szene
	 */
	public SceneLight(BranchGroup scene) {
		super();
		this.scene = scene;
	}

	/**
	 * @return Gibt eine Referenz auf scene zurueck
	 */
	public BranchGroup getScene() {
		return scene;
	}

	/**
	 * @param scene
	 *            Referenz auf scene, die gesetzt werden soll
	 */
	public void setScene(BranchGroup scene) {
		this.scene = scene;
	}

	/**
	 * Fuegt einen Bot hinzu, der schon im Szenegraphen ist
	 * 
	 * @param botId
	 *            Name des Bot
	 * @param tg
	 *            Translationsgruppe des Bot
	 * @param rg
	 *            Rotationsgruppe des Bot
	 */
	@SuppressWarnings("unchecked")
	public void addBot(String botId, TransformGroup tg) {
		map.put(botId+"TG",tg);
	}

	/**
	 * Fuegt einen Bot zur Laufzeit hinzu, der noch nicht im Szenegraphen ist
	 * d.h. alles wird geklont
	 * 
	 * @param botId
	 *            Name des Bot
	 * @param tg
	 *            Translationsgruppe des Originalbot
	 * @param rg
	 *            Rotationsgruppe des Originalbot
	 * @param bg
	 *            Branchgruppe des Originalbot
	 */
	public void addBot(String botId, TransformGroup tg, BranchGroup bg) {
		NodeReferenceTable nr = new NodeReferenceTable();
		BranchGroup newBg = (BranchGroup) bg.cloneTree(nr);
		((BranchGroup)map.get("obstBG")).addChild(newBg);

		addBot(botId,(TransformGroup) nr.getNewObjectReference(tg));
	}


	/**
	 * Suche einen ViewBot aus der Liste
	 * 
	 * @param id
	 *            Name des ViewBots
	 * @return Verweis auf den Bot
	 */
//	private ViewBot findViewBot(String id) {
//		Iterator it = bots.iterator();
//		while (it.hasNext()) {
//			ViewBot intern = (ViewBot) it.next();
//			if (intern.id.equals(id))
//				return intern;
//		}
//		return null;
//	}

	/**
	 * Gleicht die Positionen der internen Bots an die in der Liste an
	 * 
	 * @param list
	 *            Liste der Bots
	 * 
	 */
	public void update(SceneUpdate sceneUpdate) {

		
		Iterator it = sceneUpdate.getBots().iterator();
		while (it.hasNext()) {
			ViewBotUpdate extern = (ViewBotUpdate) it.next();

//			Map.Entry entry =(Map.Entry) it.next();
			
			Object o = map.get(extern.getId());
			
			TransformGroup tg = (TransformGroup) o;
			Transform3D trans = new Transform3D();
			trans.set(extern.getTransformMatrix());
			tg.setTransform(trans);
		}
	}

	/**
	 * @param obstBG
	 *            Referenz auf obstBG, die gesetzt werden soll
	 */
	@SuppressWarnings("unchecked")
	public void setObstBG(BranchGroup obstBG) {
		map.put("obstBG",obstBG);
	}

	/**
	 * Erstellt einen kompletten Klon des Objektes Achtung, das Ursprungsobjekt
	 * darf nicht compiliert oder aktiv sein!
	 */
	@SuppressWarnings("unchecked")
	public SceneLight clone() {
		// der Clone
		SceneLight sc = new SceneLight();
		
		NodeReferenceTable nr = new NodeReferenceTable();

		sc.setScene((BranchGroup) scene.cloneTree(nr));
		sc.setObstBG((BranchGroup) nr.getNewObjectReference(getObstBG()));

		Iterator it = map.entrySet().iterator(); 
		while (it.hasNext()) {
			Map.Entry entry =(Map.Entry) it.next();
			String key = (String) entry.getKey();
			Node value = (Node) entry.getValue();
			sc.map.put(key,nr.getNewObjectReference(value));
		}
		return sc;
	}

	/**
	 * @return Gibt eine Referenz auf obstBG zurueck
	 */
	public BranchGroup getObstBG() {
		return ((BranchGroup)map.get("obstBG"));
	}
	
	/**
	 * Schreibt das SceneLight-Objekt auf den Datenstrom
	 * @param os Der OutputStream
	 * @throws IOException
	 */
	public void writeStream(OutputStream os) throws IOException{
		SceneGraphStreamWriter writer = new SceneGraphStreamWriter(os);
		try {
			writer.writeBranchGraph(scene,map);
		} catch (NamedObjectException ex){
			ErrorHandler.error("Fehler beim Schreiben von SceneLight "+ex);
		}
		writer.close();
	}

	/**
	 * Liest ein SceneLight Objekt vom Datenstrom ein
	 * @param is Der InputStream
	 * @throws IOException
	 */
	public void readStream(InputStream is) throws IOException{
		SceneGraphStreamReader reader = new SceneGraphStreamReader(is);
		scene=reader.readBranchGraph(map);
	}

	public HashMap getMap() {
		return map;
	}

}
