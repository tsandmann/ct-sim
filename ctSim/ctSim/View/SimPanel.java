/* c't-Sim - Robotersimulator fuer den c't-Bot
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
package ctSim.View;

import javax.swing.JPanel;

public abstract class SimPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	// In welchem Rahmen wird das Panel angezeigt?
	private ControlFrame frame;

	/**
	 * Standardkonstruktor
	 */
	public SimPanel(){
		super();
	}
	
	/**
	 * Startet GUI
	 */
	protected abstract void initGUI();

	/**
	 * Wird aufgerufen, wenn sich der Zustand des Bot veraendert hat
	 */
	public abstract void reactToChange();

	/**
	 * Entfernt dieses Panel aus dem ControlFrame
	 */
	public void remove() {
		frame.getControlPanels().remove(this);
	}

	/**
	 * @return Gibt eine Referenz auf den Rahmen zurueck, der dieses Panel
	 *         enthaelt
	 */
	public ControlFrame getFrame() {
		return frame;
	}
	
	/**
	 * @param frame
	 *            Der Rahmen, der dieses Panel enthalten soll
	 */
	public void setFrame(ControlFrame frame) {
		this.frame = frame;
	}

}
