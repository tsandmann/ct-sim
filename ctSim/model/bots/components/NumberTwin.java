package ctSim.model.bots.components;

import javax.swing.SpinnerNumberModel;

import ctSim.model.Command;

//$$ doc
// ist Haelfte eines Paars (z.B. IrL + IrR)
public abstract class NumberTwin extends BotComponent<NumberTwin.NumberModel> {
	public static class NumberModel extends SpinnerNumberModel {
		private static final long serialVersionUID = 15828077642311050L;

		@Override
		public Number getValue() {
			return (Number)super.getValue();
		}
	}

	protected final boolean isLeft;

	public NumberTwin(boolean isLeft) {
		super(new NumberModel());
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

	public void readFrom(Command c) {
		getModel().setValue(isLeft ? c.getDataL() : c.getDataR());
	}

	public void writeTo(Command c) {
		// Verengender Cast double -> int
		int value = getModel().getValue().intValue();
		if (isLeft)
			c.setDataL(value);
		else
			c.setDataR(value);
	}
}
