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
package ctSim.model.scene;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.DanglingReferenceException;
import javax.media.j3d.SceneGraphObject;

import com.sun.j3d.utils.scenegraph.io.NamedObjectException;
import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamWriter;

/**
 * Diese Hilfsklasse transferiert einen Scenegraphen und wird nur benoetigt, da SceneGraphStreamWriter von j3d defekt ist
 * Achtung, sie "missbraucht" die UserData, um die namen der Objekte zu transerieren. Das koennte Kollisionen mit anderen Routinen erzeugen, tut es aber bislang nicht
 * 
 * @author bbe (bbe@heise.de)
 *
 */public class SceneGraphStreamWriterFixed extends SceneGraphStreamWriter {

	/**
	 * @param arg0
	 * @throws IOException
	 */
	public SceneGraphStreamWriterFixed(OutputStream arg0) throws IOException {
		super(arg0);
	}

	/**
	 * @see com.sun.j3d.utils.scenegraph.io.SceneGraphStreamWriter#writeBranchGraph(javax.media.j3d.BranchGroup, java.util.HashMap)
	 */
	@Override
	public void writeBranchGraph(BranchGroup bg, HashMap map) throws IOException, DanglingReferenceException, NamedObjectException {
		prepareMap(map);
		super.writeBranchGraph(bg, map);
	}

	/**
	 * Bereitet eine Map vor
	 * @param map Map
	 */
	private void prepareMap(HashMap map){
		Iterator it = map.keySet().iterator();
		
		while (it.hasNext()){
			String name = (String)it.next();
			SceneGraphObject so = (SceneGraphObject)map.get(name);
			so.setUserData(new String(name));
			
			System.out.println("Key "+name+" vorbereitet");

		}
	}
}
