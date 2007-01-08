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
package ctSim.view.gui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Sensor;
import ctSim.view.gui.actuators.ActuatorGroupGUI;
import ctSim.view.gui.sensors.SensorGroupGUI;

/**
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class BotViewer extends JPanel implements Updatable {
	private static final long serialVersionUID = - 7367493564649395707L;

	public final Bot bot;

	private final List<ComponentGroupGUI<?>> compList =
		new ArrayList<ComponentGroupGUI<?>>();

	public BotViewer(Bot bot) {
		this.bot = bot;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		List<ActuatorGroupGUI<?>> actsList = new ArrayList<ActuatorGroupGUI<?>>();
		List<SensorGroupGUI<?>>   sensList = new ArrayList<SensorGroupGUI<?>>();

		for (Actuator a : bot.getActuators()) {
			ActuatorGroupGUI gGUI = a.getActuatorGroupGUI();
			int idx = actsList.indexOf(gGUI);
			if(idx < 0)
				actsList.add(gGUI);
			else
				actsList.get(idx).join(gGUI);
		}

		for (Sensor s : bot.getSensors()) {
			SensorGroupGUI gGUI = s.getSensorGroupGUI();
			int idx = sensList.indexOf(gGUI);
			if(idx < 0)
				sensList.add(gGUI);
			else
				sensList.get(idx).join(gGUI);
		}

		compList.add(bot.getBotPosition().getGUI());
		compList.addAll(actsList);
		compList.addAll(sensList);
		Collections.sort(compList, new Comparator<ComponentGroupGUI<?>>() {
			public int compare(ComponentGroupGUI<?> gui1,
				ComponentGroupGUI<?> gui2) {

				if (gui1.getSortId() < gui2.getSortId())
					return -1;
				if (gui1.getSortId() > gui2.getSortId())
					return 1;
				return 0;
			}
		});

		for (ComponentGroupGUI<?> gui : compList) {
			gui.initGUI();
			add(gui);
		}

		//this.add(Box.createVerticalGlue());
	}

	public void update() {
		for (ComponentGroupGUI<?> gui : compList)
			gui.updateGUI();
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(180, 600);
	}
}
