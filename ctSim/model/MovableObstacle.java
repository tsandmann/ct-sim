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
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

//$$ Mergen mit AliveObstacle: Das sind zwei Dinger, die denselben Zweck haben offenbar
/**
 * Klasse f&uuml;r alle Hindernisse, die bewegt werden k&ouml;nnen
 *
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public interface MovableObstacle extends Obstacle {

	// TODO Was muessen MovableObstacle koennen?
	/** Mit dem Obstacle ist alles in Ordnung */
	public static final int OBST_STATE_NORMAL    = 0x0000;
	/** Das Obstacle hat eine Kollision */
	public static final int OBST_STATE_COLLISION = 0x0001;
	/** Das Obstacle faellt */
	public static final int OBST_STATE_FALLING   = 0x0002;

	// Weitere Konstanten in AliveObstacle Wert > 0x0100

	/**
	 * Bitmaske, die alle Bits ausblendet, die keine Rolle f&uuml;r die
	 * Sicherheit des Bots spielen. Der Bot gilt als sicher, wenn (obst_state &
	 * OBST_STATE_SAFE) ==0 )
	 */
	public static final int OBST_STATE_SAFE = 0x00FF;


	/**
	 * Liefert den Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 *
	 *@return Der Zustand des Objekts
	 */
	public int getObstState();

	/**
	 * Setzt den Zustand des Objektes zurueck. z.B. Ob es faellt, oder eine Kollision hat
	 * Zustaende sind ein Bitmaske aus den OBST_STATE_ Konstanten
	 *
	 * @param state Der Zustand, der gesetzt werden soll
	 */
	public void setObstState(int state);

	/**
	 * @param pos
	 *            Die Position, an die das Hindernis gesetzt werden soll
	 */
	/* TODO: ueber? (wenn man getPos vorgibt, ist setPos wohl ueber oder wer sollte die Pose setzen)
	 */
	//$$$public void setPosition(Point3d pos);

	/**
	 * @param head Die Blickrichtung, die gesetzt werden soll
	 */
	//$$$public void setHeading(Vector3d head);
}