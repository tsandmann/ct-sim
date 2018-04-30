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

package ctSim.model.rules;

import ctSim.SimUtils;
import ctSim.controller.DefaultController;
import ctSim.model.ThreeDBot;
import ctSim.model.World;
import ctSim.view.gui.Debug;

/** Schiedsrichter für Rennen von zwei Bots durch ein Labyrinth */
public class LabyrinthJudge extends Judge {
	/** Controller */
	private DefaultController controller;
	/** Welt */
	private World world;
	/** Teilnehmerzahl */
	private int participants = 2;
	
	/** 
	 * Variable um den ersten Start zu markieren
	 * 
	 * @param ctrl	Controller 
	 */
	public LabyrinthJudge(DefaultController ctrl) {
		super(ctrl);
		this.controller = ctrl;
	}

	/**
	 * @see ctSim.model.rules.Judge#setWorld(ctSim.model.World)
	 */
	@Override
	public void setWorld(World world) {
		this.world = world;
		super.setWorld(world);
	}

	/**
	 * @see ctSim.model.rules.Judge#isAddingBotsAllowed()
	 */
	@Override
	public boolean isAddingBotsAllowed() {
		if (this.controller.getParticipants() >= this.participants) {
			Debug.out.println("Fehler: Es sind schon " + this.participants + " Bots auf der Karte.");
			return false;
		}

		return super.isAddingBotsAllowed();
	}

	/**
	 * @see ctSim.model.rules.Judge#isStartingSimulationAllowed()
	 */
	@Override
	public boolean isStartingSimulationAllowed() {
		if (this.controller.getParticipants() < this.participants) {
			Debug.out.println("Fehler: Noch nicht genuegend Bots auf der Karte.");
			return false;
		}

		if (this.controller.getParticipants() > this.participants) {
			Debug.out.println("Fehler: Es sind zu viele Bots auf der Karte.");
			return false;
		}

		return super.isStartingSimulationAllowed();
	}

	/**
	 * @return true, wenn alle Regeln eingehalten werden
	 */
	@Override
	public boolean isSimulationFinished(){
		if (world == null) {
			return true;
		}
		
		ThreeDBot winner = world.whoHasWon();
		if (winner == null) {
			return false;
		} else {
			Debug.out.println("Zieleinlauf " + winner + " nach " + SimUtils.millis2time(this.getTime()));
			return true;
		}
	}
}
