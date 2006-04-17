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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.SceneGraphObject;

import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamReader;

import ctSim.ErrorHandler;

/**
 * Diese Hilfsklasse transferiert einen Scenegraphen und wird nur benoetigt, da SceneGraphStreamReader von j3d defekt ist
 * Achtung, sie "missbraucht" die UserData, um die namen der Objekte zu transerieren. Das koennte Kollisionen mit anderen Routinen erzeugen, tut es aber bislang nicht
 * 
 * @author bbe (bbe@heise.de)
 *
 */public class SceneGraphStreamReaderFixed extends SceneGraphStreamReader {

	public SceneGraphStreamReaderFixed(InputStream arg0) throws IOException {
		super(arg0);
		// TODO Auto-generated constructor stub
	}

	@Override
	public BranchGroup readBranchGraph(HashMap map) throws IOException {
		BranchGroup bg = super.readBranchGraph(map);
		reconstructMap(bg,map);
		return bg;
	}

	@SuppressWarnings("unchecked")
	private void reconstructMap(Group scene, HashMap map){
		Iterator it = map.keySet().iterator();
		
		while (it.hasNext()){
			String name = (String)it.next();
			SceneGraphObject so = findInScenegraph(scene,name);
			if (so != null)
				map.put(name,so);
			else
				ErrorHandler.error("Key "+name+" konnte nach der uebertragung nicht rekontruiert werden");
		}
	}

	
	private SceneGraphObject findInScenegraph(Group group,String name){
		if (group.getUserData() != null)
				if (((String)group.getUserData()).equals(name))
					return group;
		
		Enumeration en = group.getAllChildren();
		while (en.hasMoreElements()){
			SceneGraphObject so = (SceneGraphObject) en.nextElement();
			// Ist das eine Gruppe? Wenn ja, durchsuchen
			if (so instanceof Group){
				// rekursion
				SceneGraphObject res = findInScenegraph((Group)so, name); 
				if ( res != null) // etwas gefunen? 
					return res;		// abbruch der rekursion
			}
		}
		return null;
	}

}
