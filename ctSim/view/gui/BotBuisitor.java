package ctSim.view.gui;

import javax.swing.JPanel;

import ctSim.model.bots.Bot;
import ctSim.util.Buisitor;

//$$ doc
//$$ Klasse ist so klein, dass sie ueberfluessig ist (?)
public abstract class BotBuisitor extends JPanel {
	private final Buisitor buisitor = new Buisitor(this);
	private boolean shouldBeDisplayed = false;

	public BotBuisitor() {
		super(true); // Double-Buffering an
	}

	public void visit(Object o, Bot bot) {
		if (buisitor.dispatchBuisit(o) > 0)
			shouldBeDisplayed = true;
		if (buisitor.dispatchBuisit(o, bot) > 0)
			shouldBeDisplayed = true;
	}

	public boolean shouldBeDisplayed() {
		return shouldBeDisplayed;
	}
}
