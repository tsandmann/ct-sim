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
