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
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 * Die ist ein Container-Objekt für alle Aenderungen an einem Szenegraphen  
 * @author bbe (bbe@heise.de) *
 */
public class SceneUpdate {
	/** Liste mit allen Bots, die in dieser Welt leben */
	private List<ViewBotUpdate> bots = null;

	/** ID fuer einen neuen Bot */
	private String id = null;
	/** TransfromGroup fuer einen neuen Bot */
	private TransformGroup tg = null;
	/** TransfromGroup fuer einen neuen Bot */
	private TransformGroup rg = null;
	/** BranchGroup fuer einen neuen Bot */
	private BranchGroup bg = null;

	/**
	 * @return Gibt eine Referenz auf bg zurueck
	 * @return Gibt den Wert von bg zurueck
	 */
	public BranchGroup getBg() {
		return bg;
	}



	/**
	 * @return Gibt eine Referenz auf bots zurueck
	 * @return Gibt den Wert von bots zurueck
	 */
	public List<ViewBotUpdate> getBots() {
		return bots;
	}



	/**
	 * @return Gibt eine Referenz auf id zurueck
	 * @return Gibt den Wert von id zurueck
	 */
	public String getId() {
		return id;
	}



	/**
	 * @return Gibt eine Referenz auf rg zurueck
	 * @return Gibt den Wert von rg zurueck
	 */
	public TransformGroup getRg() {
		return rg;
	}



	/**
	 * @return Gibt eine Referenz auf tg zurueck
	 * @return Gibt den Wert von tg zurueck
	 */
	public TransformGroup getTg() {
		return tg;
	}



	/**
	 * Erzeugt ein Update aus den vorhandenen Bots 
	 */
	public SceneUpdate(SceneLight sceneLight) {
		super();
		bots= new LinkedList<ViewBotUpdate>();

		Iterator it = sceneLight.getBots().iterator();
		while (it.hasNext()) {
			ViewBot vBot = (ViewBot) it.next();
			bots.add(new ViewBotUpdate(vBot.id,vBot.tg,vBot.rg));
		}
		// TODO Auto-generated constructor stub
	}

	
	
	/**
	 * Hilfsklasse, die zu jedem Bot nur noch die Updates enthaelt
	 * @author bbe (bbe@heise.de)
	 */
	private class ViewBotUpdate{
		String id;
		Transform3D translation;
		Transform3D rotation;
		/**
		 * @param id
		 * @param rotation
		 * @param translation
		 */
		public ViewBotUpdate(String id, TransformGroup rotation, TransformGroup translation) {
			super();
			// TODO Auto-generated constructor stub
			this.id = id;
			
			this.rotation = new Transform3D();
			rotation.getTransform(this.rotation);
			
			this.translation = new Transform3D();
			translation.getTransform(this.translation);
		}
	}



	/**
	 * @param bg
	 * @param id
	 * @param rg
	 * @param tg
	 */
	public SceneUpdate(BranchGroup bg, String id, TransformGroup rg, TransformGroup tg) {
		super();
		// TODO Auto-generated constructor stub
		this.bg = bg;
		this.id = id;
		this.rg = rg;
		this.tg = tg;
	}
}
