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

package ctSim.Model;

import javax.media.j3d.*;

/**
 * Ein festes Hindernis in der Welt der Bots.
 * 
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */

public class FixedObstacle implements Obstacle {

	/** Die Grenzen des Objektes */
	private Bounds bounds;

	/**
	 * Konstruktor
	 */
	public FixedObstacle() {
		bounds = null;
	}

	/**
	 * @return Liefert die Grenzen des Hindernisses zurueck
	 */
	public Bounds getBounds() {
		return bounds;
	}

	/**
	 * @param bounds
	 *            Die gewuenschten Grenzen des Objekts
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}
}
