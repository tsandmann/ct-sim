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

import javax.swing.SpinnerModel;

import ctSim.model.ThreeDBot.HeadingCompnt;
import ctSim.model.ThreeDBot.HeadingGlobal;
import ctSim.model.ThreeDBot.PositionCompnt;
import ctSim.model.ThreeDBot.PositionGlobal;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.SimpleActuator;
import ctSim.model.bots.components.BotComponent.SimpleSensor;

/**
 * Tabellen fuer GUI
 */
public abstract class Tables {
	/**
	 * Positionen
	 */
	public static class Position extends TableOfSpinners {
		/** UID */
		private static final long serialVersionUID = 4158210694642007178L;

	    /**
	     * @param c Positions-Komponente
	     */
	    public void buisit(PositionCompnt c) {
			c.getExternalModel().setStepSize(0.05);
			// vorm Semikolon: Pattern fuer positive Zahlen, nachm ; negative
			model.addRow(c, "0.000;\u22120.000");
		}

		/**
		 * @param c Heading-Kompomente
		 */
		public void buisit(HeadingCompnt c) {
			c.getExternalModel().setStepSize(6);
			model.addRow(c, "0.0;\u22120.0");
		}

		/**
		 * @see ctSim.view.gui.TableOfSpinners#getPanelTitle()
		 */
		@Override protected String getPanelTitle() { return "Sim-Position"; }
	}
	
	/**
	 * Positionen
	 */
	public static class GlobalPosition extends TableOfSpinners {
		/** UID */
		private static final long serialVersionUID = 4158211694642007178L;

	    /**
	     * @param c Positions-Komponente
	     */
	    public void buisit(PositionGlobal c) {
			// vorm Semikolon: Pattern fuer positive Zahlen, nachm ; negative
			model.addRow(c, "0;\u22120");
		}

		/**
		 * @param c Heading-Kompomente
		 */
		public void buisit(HeadingGlobal c) {
			model.addRow(c, "0.0;\u22120.0");
		}

		/**
		 * @see ctSim.view.gui.TableOfSpinners#getPanelTitle()
		 */
		@Override protected String getPanelTitle() { return "Bot-Position"; }
	}

	/**
	 * Aktuatoren-Anzeige
	 */
	public static class Actuators extends TableOfSpinners {
		/** UID */
		private static final long serialVersionUID = - 7560450995618737095L;

		/**
		 * @param a Aktuator
		 */
		public void buisit(SimpleActuator a) {
			model.addRow((BotComponent<? extends SpinnerModel>)a);
		}

		/**
		 * @see ctSim.view.gui.TableOfSpinners#getPanelTitle()
		 */
		@Override protected String getPanelTitle() { return "Aktuatoren"; }
	}

	/**
	 * Sensoren-Anzeige
	 */
	public static class Sensors extends TableOfSpinners {
		/** UID */
		private static final long serialVersionUID = - 1275101280635052797L;

		/**
		 * @param s Sensor
		 */
		public void buisit(SimpleSensor s) {
			model.addRow((BotComponent<? extends SpinnerModel>)s);
		}

		/**
		 * @see ctSim.view.gui.TableOfSpinners#getPanelTitle()
		 */
		@Override protected String getPanelTitle() { return "Sensoren"; }
	}
}