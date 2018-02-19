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
 * Zahlendarstellung für links oder rechts
 * ist Hälfte eines Paars (z.B. IrL + IrR)
 */
public abstract class NumberTwin extends BotComponent<SpinnerNumberModel> {
	/**
	 * NumberTwinVisitor
	 */
	public interface NumberTwinVisitor {
		/**
		 * @param numberTwin	Zahl
		 * @param isLeft		links?
		 */
		public void visit(NumberTwin numberTwin, boolean isLeft);
	}

	/** Zahlenwert */
	protected Number internalModel = Double.valueOf(0); 
	/** "linke" oder "rechte" Zahl */
	protected final boolean isLeft;

	/**
	 * Zahlendarstellung für links oder rechts
	 * @param isLeft links?
	 */
	public NumberTwin(boolean isLeft) {
		super(new SpinnerNumberModel());
		getExternalModel().addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				synchronized(NumberTwin.this) {
					internalModel = getExternalModel().getNumber();
				}
			}
		});
		this.isLeft = isLeft;
	}

	/**
	 * @return Kurzname
	 */
	protected abstract String getBaseName();
	
	/**
	 * @return Beschreibung
	 */
	protected abstract String getBaseDescription();

	/**
	 * @see ctSim.model.bots.components.BotComponent#getName()
	 */
	@Override
	public String getName() {
		return getBaseName() + (isLeft ? "L" : "R");
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return getBaseDescription()
		       + " "
		       + (isLeft ? "links" : "rechts");
	}

	/**
	 * @param c Command
	 */
	public synchronized void readFrom(Command c) {
		internalModel = isLeft ? c.getDataL() : c.getDataR();
	}

	/**
	 * @param c Command
	 */
	public synchronized void writeTo(Command c) {
		// Verengende Konvertierung Number -> int
		int value = internalModel.intValue();
		if (isLeft)
			c.setDataL(value);
		else
			c.setDataR(value);
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public synchronized void updateExternalModel() {
		getExternalModel().setValue(internalModel);
	}

	/** 
	 * Nur auf dem EDT laufenlassen 
	 * @param n Number 
	 */
	public synchronized void set(Number n) {
		getExternalModel().setValue(n);
	}

	/** 
	 * Nur auf dem EDT laufenlassen 
	 * @return Number 
	 */
	public synchronized Number get() {
		return getExternalModel().getNumber();
	}

	/**
	 * @param visitor NumberTwinVisitor
	 */
	public void acceptNumTwinVisitor(NumberTwinVisitor visitor) {
		visitor.visit(this, isLeft);
	}
}
