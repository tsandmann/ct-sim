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

import javax.media.j3d.Appearance;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Vector3f;

import com.sun.j3d.utils.geometry.Box;

/**
 * Diese Klasse hilft einen Parcours aus einer ASCII-Datei zu laden
 * @author bbe (bbe@heise.de)
 */
public class ParcoursLoader {
	public static float GRID = (float) (CtBot.BOT_RADIUS*2*2);
	
	/** Aussehen von Hindernissen */
	private Appearance wallAppear;
	
	String[] test = { 
			"XXXXXXXXXXXXX",	
			"X   X       X",
			"X X   XXXX  X",
			"X XXXXX     X",
			"X     X  XXXX",
			"XXXX XX     X",
			"X     X X   X",
			"XX XXXXXXX  X"
		};
	
	/**
	 * Erzeugt einen Wandquader
	 * Alle Postionen sind nicht in Welt-koordinaten, sondern in ganzen Einheiten, wie sie aus dem ASCII-File kommen
	 * @param x Position in X-Richtung
	 * @param y Position in X-Richtung
	 * @return
	 */
	private TransformGroup createWall(int x, int y){
		Box box = new Box(GRID/2, GRID/2 , 0.2f, wallAppear);
	    box.setPickable(true);

		Transform3D translate = new Transform3D();
		translate.set(new Vector3f((float)(x* GRID- GRID/2),(float)(y* GRID -GRID/2), 0f));
		
		TransformGroup tgBox = new TransformGroup(translate);
		tgBox.setCapability(TransformGroup.ENABLE_PICK_REPORTING);
		tgBox.setPickable(true);	
		tgBox.addChild(box);

		return tgBox;
	}

	/**
	 * Fuegt in die uebergebene TransformGroup das geladene ein
	 * @param root Die Gruppe zu der alles hibzugefuegt wird
	 */
	public void insertSceneGraph(TransformGroup root){
		char[] line = new char[test[0].length()];
		int xDim= line.length;
		int yDim= test.length;
		
		for (int i=0; i<test.length; i++){
			test[i].getChars(0,test[i].length(),line,0);
			for (int j=0; j<test[i].length(); j++)
				switch (line[j]){
					case 'X': root.addChild(createWall(j-xDim/2,yDim/2-i));
						break;
				}
		}
		
	}

	public void setWallAppear(Appearance wallAppear) {
		this.wallAppear = wallAppear;
	}
}
