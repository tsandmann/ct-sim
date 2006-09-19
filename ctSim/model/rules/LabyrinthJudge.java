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
package ctSim.model.rules;

import java.util.Set;

import javax.vecmath.Vector3d;

import ctSim.controller.DefaultController;
import ctSim.model.AliveObstacle;
import ctSim.model.World;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.view.gui.Debug;
import ctSim.view.gui.sensors.RemoteControlGroupGUI;
import ctSim.SimUtils;

/**
 * Schiedsrichter fuer Rennen von zwei Bots durch ein Labyrinth
 *
 */
public class LabyrinthJudge extends Judge {
	private DefaultController controller;
	private World world;
	private int participants = 2;
	/** Variable umd den ersten Start zu markieren */
	private boolean first = true;

	public LabyrinthJudge(DefaultController ctrl) {
		super(ctrl);
		this.controller = ctrl;
	}

	@Override
    public void setWorld(World world) {
		this.world = world;
		super.setWorld(world);
	}

	@Override
	public boolean isAddingBotsAllowed() {
		// TODO: Bot-Anzahl pruefen
		if(this.controller.getParticipants() >= this.participants) {
			Debug.out.println("Fehler: Es sind schon "+this.participants+" Bots auf der Karte.");
			return false;
		}

		return super.isAddingBotsAllowed();
	}

	/**
	 * @see ctSim.model.rules.Judge#isStartingSimulationAllowed()
	 */
	@Override
	public boolean isStartingSimulationAllowed() {

		// TODO: Bot-Anzahl pruefen
		if(this.controller.getParticipants() < this.participants) {
			Debug.out.println("Fehler: Noch nicht genuegend Bots auf der Karte."); //$NON-NLS-1$
			return false;
		}

		if(this.controller.getParticipants() > this.participants) {
			Debug.out.println("Fehler: Es sind zu viele Bots auf der Karte."); //$NON-NLS-1$
			return false;
		}

		return super.isStartingSimulationAllowed();
	}

	/**
	 * @return true, wenn alle Regelen eingehalten werden
	 */
	@Override
	public boolean isSimulationFinished(){

		if(this.world == null)
			return true;

		Set<AliveObstacle> obsts = this.world.getAliveObstacles();

		if (obsts == null || obsts.isEmpty())
			return false;

		for(AliveObstacle obst : obsts) {

			if (first ==true){
				if (obst instanceof CtBotSimTcp)
					((CtBotSimTcp)obst).sendRCCommand(RemoteControlGroupGUI.RC5_CODE_5);
			}

			if(this.world.finishReached(new Vector3d(obst.getPosition()))) {
				Debug.out.println("Zieleinlauf \""+obst.getName()+"\" nach "
						+ SimUtils.millis2time(this.getTime()));
				return true;
			}
		}
		first=false;

		return false;
	}

	@Override
    public void reinit() {

		super.reinit();
		this.first = true;
	}
}
