package ctSim.model.bots.components;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ctSim.model.Command;

//$$ doc
public abstract class NumberSingleton extends BotComponent<SpinnerNumberModel> {
	protected Number internalModel = Double.valueOf(0);

	public NumberSingleton() {
		super(new SpinnerNumberModel());
		getExternalModel().addChangeListener(new ChangeListener() {
			public void stateChanged(
			@SuppressWarnings("unused") ChangeEvent e) {
				internalModel = getExternalModel().getNumber();
			}
		});
	}

	public synchronized void writeTo(Command c) {
		c.setDataL(internalModel.intValue());
	}

	public void readFrom(Command c) {
		internalModel = c.getDataL();
	}

	@Override
	public void updateExternalModel() {
		getExternalModel().setValue(internalModel);
	}
}
