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
package ctSim.model.bots.components;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.view.gui.ComponentGroupGUI;
import ctSim.view.gui.PositionGUI;


/**
 * Abstakte Oberklasse aller Positionsanzeiger
 * @author Felix Beckwermert
 *
 */
public abstract class BotPosition extends BotComponent {

	/**
	 * Der Konstruktor
	 * @param name Name des Positionsanzeigers
	 * @param pos Position
	 * @param head Blickrichtung
	 */
	public BotPosition(String name, Point3d pos, Vector3d head) {

		super(name, pos, head);
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {

		return "Position of the bot in the world"; //$NON-NLS-1$
	}

	/**
	 * @return Gibt die GUI-Komponente fuer den Positionsanzeiger zurueck
	 */
	public ComponentGroupGUI getGUI() {

		return new PositionGUI(this);
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getRelPosition()
	 */
	@Override
	public abstract Point3d getRelPosition();

	/**
	 * @see ctSim.model.bots.components.BotComponent#getRelHeading()
	 */
	@Override
	public abstract Vector3d getRelHeading();

	/**
	 * @return die absolute Position des Positionsanzeigers
	 */
	public abstract Point3d getAbsPosition();

	/**
	 * @return die absolute Blickrichtung des Positionsanzeigers
	 */
	public abstract Vector3d getAbsHeading();

	/**
	 * Setzt eine Position fuer den Positionsanzeiger
	 * @param pos Die Position
	 */
	public abstract void setPos(Point3d pos);

	/**
	 * Setzt eine Blickrichtung fuer den Positionsanzeiger
	 * @param head Die Blickrichtung
	 */
	public abstract void setHead(Vector3d head);
}
