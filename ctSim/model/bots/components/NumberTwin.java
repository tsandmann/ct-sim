package ctSim.model.bots.components;


import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ctSim.model.Command;

//$$ doc
// ist Haelfte eines Paars (z.B. IrL + IrR)
//$$$ Ganze links/rechts-Geschichte; statische Factory?
public abstract class NumberTwin extends BotComponent<SpinnerNumberModel> {
	public interface NumberTwinVisitor {
		public void visit(NumberTwin numberTwin, boolean isLeft);
	}

	protected Number internalModel = Double.valueOf(0); 
	protected final boolean isLeft;

	public NumberTwin(boolean isLeft) {
		super(new SpinnerNumberModel());
		getExternalModel().addChangeListener(new ChangeListener() {
			public void stateChanged(
			@SuppressWarnings("unused") ChangeEvent e) {
				synchronized(NumberTwin.this) {
					internalModel = getExternalModel().getNumber();
				}
			}
		});
		this.isLeft = isLeft;
	}

	protected abstract String getBaseName();
	protected abstract String getBaseDescription();

	@Override
	public String getName() {
		return getBaseName() + (isLeft ? "L" : "R");
	}

	@Override
	public String getDescription() {
		return getBaseDescription()
		       + " "
		       + (isLeft ? "links" : "rechts");
	}

	public synchronized void readFrom(Command c) {
		internalModel = isLeft ? c.getDataL() : c.getDataR();
	}

	public synchronized void writeTo(Command c) {
		// Verengende Konvertierung Number -> int
		int value = internalModel.intValue();
		if (isLeft)
			c.setDataL(value);
		else
			c.setDataR(value);
	}

	public synchronized void updateExternalModel() {
		getExternalModel().setValue(internalModel);
	}

	/** Nur auf dem EDT laufenlassen */
	public synchronized void set(Number n) {
		getExternalModel().setValue(n);
	}

	/** Nur auf dem EDT laufenlassen */
	public synchronized Number get() {
		return getExternalModel().getNumber();
	}

	public void acceptNumTwinVisitor(NumberTwinVisitor visitor) {
		visitor.visit(this, isLeft);
	}
}
