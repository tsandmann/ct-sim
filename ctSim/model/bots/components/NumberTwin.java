package ctSim.model.bots.components;


import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ctSim.model.Command;

/**
 * Zahlendarstellung fuer links oder rechts
 * ist Haelfte eines Paars (z.B. IrL + IrR)
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
	 * Zahlendarstellung fuer links oder rechts
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
