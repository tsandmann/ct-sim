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
package ctSim.model.bots.ctbot;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.Bot;

/**
 * Abstrakte Oberklasse fuer alle c't-Bots
 */
public abstract class CtBot extends Bot {
	//$$ Alle Konstanten: verwendet?
	/** Abstand vom Zentrum zur Aussenkante des Bots [m] */
	protected static final double BOT_RADIUS = 0.060d;

	/** Hoehe des Bots [m] */
	protected static final double BOT_HEIGHT = 0.120d;

	/** Bodenfreiheit des Bots [m] */
	protected static final double BOT_GROUND_CLEARANCE = 0.015d;

	/* TODO:
	 * Pos. u. Head. in Klassenhierarchie weiter nach oben:
	 * -> Jedes (Alive)Obstacle braucht (initiale) Pos.
	 *
	 */
	/**
	 * Der Konstruktor
	 * @param name Name
	 * @param pos Position
	 * @param head Blickrichtung
	 */
	public CtBot(String name, Point3d pos, Vector3d head) {
		super(name, pos, head);
		initShape(new CtBotShape(this));

		//$$$ Toter Code
		// Einfachen Konstruktor aufrufen:
//		Vector3f vec = new Vector3f(pos);
//		// TODO: Was das!?
//		vec.z += getHeight() / 2 + getGroundClearance();
//		setPos(vec);
//		setHeading(head);
	}

	//$$ Nirgends verwendet
	public Bounds getBounds() {
		return new BoundingSphere(new Point3d(getPositionInWorldCoord()), 
			BOT_RADIUS);
	}
}
