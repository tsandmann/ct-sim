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

import javax.media.j3d.Bounds;
import javax.vecmath.Vector3f;

/**
 * Klasse fuer alle Hindernisse bewegt werden können
 * 
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public interface MovableObstacle extends Obstacle{
	// TODO Was muessen MovableObstacle koennen?
	/** Mit dem Obstacle ist alles in Ordnung */
	public static final int OBST_STATE_NORMAL = 0;
	/** Das Obstacle hat eine Kollision */
	public static final int OBST_STATE_COLLISION = 1;
	/** Das Obstacle faellt */
	public static final int OBST_STATE_FALLING = 2;

	/**
	 * Liefert den Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 */
	public int getObstState();
	
	/**
	 * Setztden Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 */
	public void setObstState(int state);
	
	/**
	 * @return Gibt die Position zurueck
	 */
	public Vector3f getPos() ;
	
	/**
	 * @param pos
	 *            Die Position, an die der Bot gesetzt werden soll
	 */
	public void setPos(Vector3f pos);
	
	/**
	 * @return Gibt die Grenzen des Obstacles zurueck
	 */
	public Bounds getBounds() ;
}