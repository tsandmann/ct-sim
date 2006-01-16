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

package ctSim.View;

import java.awt.Dimension;
import javax.swing.JTextField;

import ctSim.Model.Bot;

/**
 * Abstrakte Oberklasse fuer Kontrolltafeln simulierter Bots; 
 * muss abgeleitet werden, um instanziiert werden zu koennen 
 * @author pek (pek@heise.de)
 *
 */
public abstract class ControlPanel extends javax.swing.JPanel {

	private static final long serialVersionUID = 1L;

	private JTextField yPosField;

	private JTextField xPosField;

	private JTextField headField;
	
	// Zu welchem Bot gehoert das Panel?
	private Bot bot;

	// In welchem Rahmen wird das Panel angezeigt?
	private ControlFrame frame;
	
	/**
	 * Erzeugt ein neues Panel
	 * @param bot Referenz auf den Bot, zu dem das Panel gehoert
	 */
	public ControlPanel(Bot bot) {
		super();
		this.bot = bot;
		Dimension dim = new Dimension(30, 25);
		xPosField = new JTextField();
		yPosField = new JTextField();
		headField = new JTextField();

		xPosField.setPreferredSize(dim);
		yPosField.setPreferredSize(dim);
		headField.setPreferredSize(dim);

	}

	/**
	 * Startet GUI
	 */
	protected abstract void initGUI(); 
	
	public Bot getBot() {
		return bot;
	}

	/**
	 * Wird aufgerufen, wenn sich der Zustand des Bot veraendert hat
	 */
	public abstract void reactToChange();
	
	
	/**
	 * Entfernt dieses Panel aus dem ControlFrame
	 */
	public void remove(){
		frame.getControlPanels().remove(this);
	}
	

	/**
	 * @return Gibt eine Referenz auf den Rahmen zurueck, der dieses Panel enthaelt
	 */
	public ControlFrame getFrame() {
		return frame;
	}

	/**
	 * @param frame Der Rahmen, der dieses Panel enthalten soll
	 */
	public void setFrame(ControlFrame frame) {
		this.frame = frame;
	}

	/**
	 * @return Gibt eine Referenz auf headField zurueck
	 */
	public JTextField getHeadField() {
		return headField;
	}

	/**
	 * @param headField Referenz auf headField, die gesetzt werden soll
	 */
	public void setHeadField(JTextField headField) {
		this.headField = headField;
	}

	/**
	 * @return Gibt eine Referenz auf xPosField zurueck
	 */
	public JTextField getXPosField() {
		return xPosField;
	}

	/**
	 * @param posField Referenz auf xPosField, die gesetzt werden soll
	 */
	public void setXPosField(JTextField posField) {
		xPosField = posField;
	}

	/**
	 * @return Gibt eine Referenz auf yPosField zurueck
	 */
	public JTextField getYPosField() {
		return yPosField;
	}

	/**
	 * @param posField Referenz auf yPosField, die gesetzt werden soll
	 */
	public void setYPosField(JTextField posField) {
		yPosField = posField;
	}

	/**
	 * @param bot Referenz auf den Bot, die gesetzt werden soll
	 */
	public void setBot(Bot bot) {
		this.bot = bot;
	}
}
