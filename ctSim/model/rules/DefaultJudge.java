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

import ctSim.controller.DefaultController;
import ctSim.view.gui.Debug;

/**  Das ist der "Ich-mache-nix"-Judge für den "Normal-Betrieb" mit einem einzelnen Bot */
public class DefaultJudge extends Judge {
	/** Controller */
	private DefaultController controller;

	/**
	 * Der Konstruktor
	 * 
	 * @param ctrl	der DefaultController
	 */
	public DefaultJudge(DefaultController ctrl) {
		super(ctrl);
		this.controller = ctrl;
	}

	/**
	 * @see ctSim.model.rules.Judge#isAddingBotsAllowed()
	 */
	@Override
	public boolean isAddingBotsAllowed() {
		return true;
	}

	/**
	 * @see ctSim.model.rules.Judge#isStartingSimulationAllowed()
	 */
	@Override
	public boolean isStartingSimulationAllowed() {
		if (controller.getParticipants() < 1) {
			Debug.out.println("Fehler: Noch kein Bot auf der Karte.");
			return false;
		}
		return true;
	}

	/**
	 * @see ctSim.model.rules.Judge#isSimulationFinished()
	 */
	@Override
	protected boolean isSimulationFinished() {
		return false;
	}
}
