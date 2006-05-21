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
package ctSim.Model.Scene;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Node;
import javax.media.j3d.NodeReferenceTable;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

import com.sun.j3d.utils.scenegraph.io.NamedObjectException;
import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamReader;
import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamWriter;

import ctSim.ErrorHandler;
import ctSim.Model.AliveObstacle;
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
	HashMap<String,SceneGraphObject> map = new HashMap<String,SceneGraphObject>();
	
	/** Der eigentliche Szenegraph */
	private BranchGroup scene;

	/** Konstanten fuer die Noderefernces */
	public static final String OBSTBG = "obstBG";

	
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
	 * @param botId Name des Bot
	 * @param tg Transformgruppe des Bot
	 */
//	public void addBot_(String botId, TransformGroup tg) {
//		map.put(botId+"TG",tg);
//	}

	/**
	 * Fuegt einen Bot zur Laufzeit hinzu, der noch nicht im Szenegraphen ist
	 * d.h. alles wird geklont
	 * 
	 * @param botId
	 *            Name des Bot
	 * @param tg
	 *            Translationsgruppe des Originalbot
	 * @param bg
	 *            Branchgruppe des Originalbot
	 */
//	public void addBot(String botId, TransformGroup tg, BranchGroup bg) {
//		// Kopie erzeugen
//		NodeReferenceTable nr = new NodeReferenceTable();
//		BranchGroup newBg = (BranchGroup) bg.cloneTree(nr);
//		
//		// Kopie in die Szene einfuegen
//		((BranchGroup)map.get("obstBG")).addChild(newBg);
//
//		addBot(botId,(TransformGroup) nr.getNewObjectReference(tg));
//	}


	/**
	 * Nimmt einen Bot in Scenelight auf
	 * @param name Der Name des Bots
	 * @param newMap Alle noetigen Referenzen
	 */
	public void addBot(String name, HashMap<String,SceneGraphObject> newMap){
		// Zuerst muessen wir die Root-Branchgroup des Bots finden
		// Wir wissen lediglich, dass sie mit der Zeichenkette Bot.BG endet
		BranchGroup bg = null;
		String key = null;
		
		System.out.println("Fuege Bot "+name+" hinzu");
		
		Iterator it = newMap.keySet().iterator();
		while (it.hasNext()){
			key = (String)it.next(); 
			if (key.contains(AliveObstacle.BG))
				bg=(BranchGroup)newMap.get(key);
		}
		
		// Bot einfuegen
		((BranchGroup)map.get(OBSTBG)).addChild(bg);
		
		addBotRefs(newMap);

	}

	
	/**
	 * Fuegt nur die Referenzen des Bots in die Scenelight ein
	 * @param newMap Alle noetigen Referenzen
	 */
	@SuppressWarnings("unchecked")
	public void addBotRefs(HashMap<String,SceneGraphObject> newMap){
		// Alle Eintraege aus der Map uebertragen
		// Alle Referenzen aktualisieren
		Iterator it = newMap.keySet().iterator();
		while (it.hasNext()){
			String key = (String) it.next();
			Object obj=newMap.get(key);
			try {
				map.put(key,(SceneGraphObject)obj);
			} catch (ClassCastException ex){
				ErrorHandler.error("Probleme beim Typecasting aus der Map ("+key+"): "+ex);
			}
		}		
	}

	/**
	 * Gleicht die Positionen der internen Bots an die in der Liste an
	 * 
	 * @param sceneUpdate Das Update
	 * 
	 */
	public void update(SceneUpdate sceneUpdate) {

		
		Iterator it = sceneUpdate.getBots().iterator();
		while (it.hasNext()) {
			ViewBotUpdate extern = (ViewBotUpdate) it.next();
			
			Object o = map.get(extern.getId());
			// Handelt es sich um eine Transformgroup?
			if (o instanceof TransformGroup){
				TransformGroup tg = (TransformGroup) o;
				Transform3D trans = new Transform3D();
				trans.set(extern.getTransformMatrix());
				tg.setTransform(trans);
			}
		}
	}

	/**
	 * Entfernt einen Bot restlos
	 * @param name ID des Bots
	 */
	@SuppressWarnings("unchecked")
	public void removeBot(String name) {
		List tmp = new LinkedList<String>();
		
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			
			if (key.contains(name)){
			
				if (key.contains(AliveObstacle.BG)){
					((BranchGroup) map.get(key)).detach();
				}
				// zum loeschen vormerken
				tmp.add(key);
			}			
		}
		
		// Jetzt alles loeschen
		it = tmp.iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			map.remove(key);
		}
	}
	
	
	/**
	 * @param obstBG
	 *            Referenz auf obstBG, die gesetzt werden soll
	 */
	public void setObstBG(BranchGroup obstBG) {
		map.put(OBSTBG,obstBG);
	}

	/**
	 * Erstellt einen kompletten Klon des Objektes Achtung, das Ursprungsobjekt
	 * darf nicht compiliert oder aktiv sein!
	 * @return der Klone
	 */
	@Override
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
		return ((BranchGroup)map.get(OBSTBG));
	}
	
	/**
	 * Schreibt das SceneLight-Objekt auf den Datenstrom
	 * @param os Der OutputStream
	 * @throws IOException
	 */
	public void writeStream(OutputStream os) throws IOException{
		SceneGraphStreamWriter writer = new SceneGraphStreamWriterFixed(os);
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
		SceneGraphStreamReader reader = new SceneGraphStreamReaderFixed(is);
		scene=reader.readBranchGraph(map);
	}

	/**
	 * Liefert die Liste mit allen Referenzen
	 * @return die Liste
	 */
	public HashMap<String,SceneGraphObject> getMap() {
		return map;
	}

	/**
	 * liefert alle Referenzen zu einem Teilschluessel
	 * @param partialKey Zeichenfolge mit dem Teilschluessel
	 * @return Hashmap mit den Referenzen
	 */
	public HashMap<String,SceneGraphObject> getReferences(String partialKey){
		HashMap<String,SceneGraphObject> h = new HashMap<String,SceneGraphObject>();
		
		Iterator it = map.entrySet().iterator(); 
		while (it.hasNext()){
			Map.Entry entry =(Map.Entry) it.next();
			String key = (String) entry.getKey();
			if (key.contains(partialKey))
					h.put(key,map.get(key));
		}
		
		return h;
	}

	/**
	 * Fuegt eine Referenz in die Map ein
	 * @param key
	 * @param so
	 */
	public void addReference(String key, SceneGraphObject so) {
		map.put(key,so);
		
	}

	/**
	 * Liefert einen Eintrag aus der Map zurueck
	 * @param key
	 * @return Der Eintrag
	 */
	public SceneGraphObject getNodeReference(String key) {
		return map.get(key);
	}
	
}
