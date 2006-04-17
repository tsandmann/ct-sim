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

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Node;
import javax.media.j3d.TransformGroup;
import ctSim.View.ViewBotUpdate;

/**
 * Die ist ein Container-Objekt fuer alle Aenderungen an einem Szenegraphen  
 * @author bbe (bbe@heise.de) 
 */
public class SceneUpdate implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Liste mit allen Bots, die in dieser Welt leben */
	private List<ViewBotUpdate> bots = null;

	/** ID fuer einen neuen Bot */
//	private String id = null;
	/** TransfromGroup fuer einen neuen Bot */
//	private TransformGroup tg = null;
	/** BranchGroup fuer einen neuen Bot */
//	private BranchGroup bg = null;

	/**
	 * @return Gibt eine Referenz auf bg zurueck
	 * @return Gibt den Wert von bg zurueck
	 */
//	public BranchGroup getBg() {
//		return bg;
//	}



	/**
	 * @return Gibt eine Referenz auf bots zurueck
	 */
	public List<ViewBotUpdate> getBots() {
		return bots;
	}



	/**
	 * @return Gibt den Wert von id zurueck
	 */
//	public String getId() {
//		return id;
//	}

	/**
	 * @return Gibt eine Referenz auf tg zurueck
	 */
//	public TransformGroup getTg() {
//		return tg;
//	}

	/**
	 * Erzeugt ein Update aus den vorhandenen Bots 
	 */
	public SceneUpdate(SceneLight sceneLight) {
		super();
		
		bots= new LinkedList<ViewBotUpdate>();
		
		// Einmal alle Bots durchgehen
		Iterator it = sceneLight.getMap().entrySet().iterator(); 
		while (it.hasNext()) {
			Map.Entry entry =(Map.Entry) it.next();
			String key = (String) entry.getKey();
			Node value = (Node) entry.getValue();
			
			if (!key.equals("obstBG")){
				bots.add(new ViewBotUpdate(key,(TransformGroup)value));				
			}
		}
	}
}
