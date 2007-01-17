package ctSim.model.bots.components;


import javax.swing.SpinnerNumberModel;

import ctSim.model.Command;
import ctSim.model.bots.ctbot.CtBotSimTcp.NumberTwinVisitor;

//$$ doc
// ist Haelfte eines Paars (z.B. IrL + IrR)
//$$$ Ganze links/rechts-Geschichte; statische Factory?
public abstract class NumberTwin extends BotComponent<SpinnerNumberModel> {
	protected final boolean isLeft;

	public NumberTwin(boolean isLeft) {
		super(new SpinnerNumberModel());
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
		// Verengende Konvertierung double -> int
		int value = getModel().getNumber().intValue();
		if (isLeft)
			c.setDataL(value);
		else
			c.setDataR(value);
	}

	public void set(Number n) {
		getModel().setValue(n);
	}

	public Number get() {
		return getModel().getNumber();
	}

	public void accept(NumberTwinVisitor visitor) {
		visitor.visit(this, isLeft);
	}
}
