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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.NodeReferenceTable;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 * Die ist ein Container-Objekt für einen Szenegraphen und alle Hooks, 
 * die man braucht um die Welt anzuzeigen 
 * @author bbe (bbe@heise.de)
 *
 */
public class SceneLight {

//	private static final long serialVersionUID = 8381732842937148198L;
	
	/** Der eigentliche SzeneGraph */
	private BranchGroup scene;
	/** Liste mit allen Bots, die in dieser Welt leben */
	private List<ViewBot> bots;
	
	/** Node unter der alle Bots haengen */
	private BranchGroup obstBG;
	
	/**
	 * Initialisiere das Objekt
	 */
	public SceneLight() {
		super();
		bots = new LinkedList<ViewBot> ();
		scene = null;
	}

	
	/**
	 * Initialisiere das Objekt
	 * @param scene Die Szene
	 */
	public SceneLight(BranchGroup scene) {
		super();
		bots = new LinkedList<ViewBot> ();
		this.scene = scene;
	}


	/**
	 * @return Gibt eine Referenz auf scene zurueck
	 * @return Gibt den Wert von scene zurueck
	 */
	public BranchGroup getScene() {
		return scene;
	}

	/**
	 * @param scene Referenz auf scene, die gesetzt werden soll
	 * @param scene Der Wert von scene, der gesetzt werden soll
	 */
	public void setScene(BranchGroup scene) {
		this.scene = scene;
	}

	/**
	 * Fuegt einen Bot hinzu, der schon im Szenegraphen ist
	 * @param botId
	 * @param tg
	 * @param rg
	 */
	public void addBot(String botId, TransformGroup tg,TransformGroup rg){
		bots.add(new ViewBot(botId, tg,rg));
	}

	/**
	 * Fuegt einen Bot  zur Laufzeit hinzu, der noch nicht im Szenegarphen ist
	 * d.h. alles wird geclonet
	 * @param botId Name des Bots
	 * @param tg Translationsgruppe des Originalbots
	 * @param rg Rotationsgruppe des Originalbots
	 * @param bg Branchgruppe des Originalbots
	 */
	public void addBot(String botId, TransformGroup tg,TransformGroup rg,BranchGroup bg){
		NodeReferenceTable nr= new NodeReferenceTable();
		BranchGroup newBg= (BranchGroup)bg.cloneTree(nr);

		bots.add(new ViewBot(botId, 
				(TransformGroup)nr.getNewObjectReference(tg),
				(TransformGroup)nr.getNewObjectReference(rg)));
		obstBG.addChild(newBg);
	}

	/**
	 * @return Gibt eine Referenz auf bots zurueck
	 * @return Gibt den Wert von bots zurueck
	 */
	public List<ViewBot> getBots() {
		return bots;
	}
	
	/**
	 * Suche einen ViewBot aus der Liste
	 * @param id Name des ViewBots
	 * @return Verweis auf den Bot
	 */
	private ViewBot findViewBot(String id){
		Iterator it = bots.iterator();
		while (it.hasNext()) {
			ViewBot intern = (ViewBot) it.next();
			if (intern.id.equals(id))
				return intern;
		}		
		return null;
	}
	
	/** Gleicht die Positionen der internen Bots an die in der Liste an
	 * @param  list Liste der Bots
	 *
	 */
	public void update(List<ViewBot> list){
		Iterator it = list.iterator();
		while (it.hasNext()) {
			ViewBot extern = (ViewBot) it.next();
			ViewBot intern = findViewBot(extern.id);
			if (intern != null){
				Transform3D trans = new Transform3D();
				extern.tg.getTransform(trans);
				intern.tg.setTransform(trans);

				extern.rg.getTransform(trans);
				intern.rg.setTransform(trans);}	
		
		}
	}


	/**
	 * @param obstBG Referenz auf obstBG, die gesetzt werden soll
	 * @param obstBG Der Wert von obstBG, der gesetzt werden soll
	 */
	public void setObstBG(BranchGroup obstBG) {
		this.obstBG = obstBG;
	}

	/**
	 * Erstellt einen kompleten Clone des Objektes
	 * Achtung, das Ursprungsobjekt darf nicht compiliert oder aktiv sein!
	 */
	public SceneLight clone(){
		// Baue alles neu auf
		SceneLight sc = new SceneLight ();
		NodeReferenceTable nr= new NodeReferenceTable();
					
		sc.setScene((BranchGroup)scene.cloneTree(nr));
		sc.setObstBG((BranchGroup)nr.getNewObjectReference(obstBG));
		
		Iterator it = bots.iterator();
		while (it.hasNext()) {
			Bot curr = (Bot) it.next();
			sc.addBot(curr.getBotName(),
					(TransformGroup)nr.getNewObjectReference(curr.getTranslationGroup()),
					(TransformGroup)nr.getNewObjectReference(curr.getRotationGroup()));
		}
		return sc;

	}


	/**
	 * @return Gibt eine Referenz auf obstBG zurueck
	 * @return Gibt den Wert von obstBG zurueck
	 */
	public BranchGroup getObstBG() {
		return obstBG;
	}
}
