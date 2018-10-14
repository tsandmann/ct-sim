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

package ctSim.model.bots.components;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ctSim.model.Command;

/**
 * Zahlendarstellung
 */
public abstract class NumberSingleton extends BotComponent<SpinnerNumberModel> {
	/**
	 * Zahlenwert
	 */
	protected Number internalModel = Double.valueOf(0);

	/**
	 * Zahl
	 */
	public NumberSingleton() {
		super(new SpinnerNumberModel());
		getExternalModel().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				internalModel = getExternalModel().getNumber();
			}
		});
	}

	/**
	 * @param c Command
	 */
	public synchronized void writeTo(Command c) {
		c.setDataL(internalModel.intValue());
	}

	/**
	 * @param c Command
	 */
	public void readFrom(Command c) {
		internalModel = c.getDataL();
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public void updateExternalModel() {
		getExternalModel().setValue(internalModel);
	}
}
