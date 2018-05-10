/*
 * c't-Sim - Robotersimulator für den c't-Bot
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

package ctSim.model.scene;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.SceneGraphObject;

import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamReader;

import ctSim.util.FmtLogger;

/**
 * Diese Hilfsklasse transferiert einen Scenegraphen und wird nur benötigt, da SceneGraphStreamReader
 * von Java3D (j3d) defekt ist. Achtung, sie "missbraucht" die UserData, um die namen der Objekte zu
 * transferieren. Das könnte Kollisionen mit anderen Routinen erzeugen, tut es aber bislang nicht.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 */
public class SceneGraphStreamReaderFixed extends SceneGraphStreamReader {
	/** Logger */
	FmtLogger lg = FmtLogger.getLogger(
		"ctSim.model.scene.SceneGraphStreamReaderFixed");

	/**
	 * @param arg0
	 * @throws IOException
	 */
	public SceneGraphStreamReaderFixed(InputStream arg0) throws IOException {
		super(arg0);
	}

	/**
	 * @see com.sun.j3d.utils.scenegraph.io.SceneGraphStreamReader#readBranchGraph(java.util.HashMap)
	 */
	@Override
	public BranchGroup readBranchGraph(HashMap map) throws IOException {
		BranchGroup bg = super.readBranchGraph(map);
		reconstructMap(bg,map);
		return bg;
	}

	/**
	 * Rekonstruiert eine Map
	 * 
	 * @param scene	Gruppe
	 * @param map	Map
	 */
	private void reconstructMap(Group scene, HashMap map){
		if (scene == null){
			lg.warn("Keine Group empfangen");
			return;
		}
		
		Vector toKill = new Vector();
		
		Iterator it = map.keySet().iterator();
		
		while (it.hasNext()){
			String name = (String)it.next();
			SceneGraphObject so = findInScenegraph(scene,name);
			String string = "Key ";
			if (so != null){
				map.put(name,so);
				System.out.println(string+name+" rekonstruiert");
			} else {
				lg.warn(string+name+" konnte nach der Übertragung nicht " +
						"rekonstruiert werden");
				toKill.add(name);
			}
		}
		
		// Entferne alle Listeneinträge, die nicht korrekt übertragen wurden
		it=toKill.iterator();
		while (it.hasNext()){
			String name = (String)it.next();
			map.remove(name);
		}
	}

	
	/**
	 * Sucht etwas im Szenegraphen
	 * 
	 * @param group	Gruppe
	 * @param name	gesuchter Name
	 * @return SceneGraphObject
	 */
	private SceneGraphObject findInScenegraph(Group group,String name){
		if (group.getUserData() != null)
				if (((String)group.getUserData()).equals(name))
					return group;
		
		Enumeration en = group.getAllChildren();
		while (en.hasMoreElements()){
			SceneGraphObject so = (SceneGraphObject) en.nextElement();
			// Ist das eine Gruppe? Wenn ja, durchsuchen...
			if (so instanceof Group){
				// Rekursion
				SceneGraphObject res = findInScenegraph((Group)so, name); 
				if ( res != null)	// etwas gefunden? 
					return res;	// Abbruch der Rekursion
			}
		}
		return null;
	}

}