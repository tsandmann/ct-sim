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

package ctSim.model;

import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Interface für Hindernisse aller Art in der Bot-Welt
 *
 * @author Benjamin Benz
 */

public abstract interface Obstacle {

	/**
	 * @return Liefert die Grenzen des Hindernisses zurück
	 */
	public abstract Bounds getBounds();

	/**
	 * @return Gibt die Position zurück
	 */
	public abstract Point3d getPositionInWorldCoord() ;

	/**
	 * @return Gibt die Blickrichtung zurück
	 */
	public abstract Vector3d getHeadingInWorldCoord();

	/** Erzeugt die 3D-Repräsentation eines Objektes */
//	public abstract void createBranchGroup();

	/**
	 * @return die 3D-Repräsentation eines Objektes
	 */
	public abstract BranchGroup getBranchGroup();
}
