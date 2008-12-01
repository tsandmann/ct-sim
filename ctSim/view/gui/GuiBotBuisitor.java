package ctSim.view.gui;

import javax.swing.JPanel;

import ctSim.model.bots.Bot;
import ctSim.model.bots.BotBuisitor;
import ctSim.util.Buisitor;

/**
 * GUI der Buisitors 
 */
public abstract class GuiBotBuisitor extends JPanel implements BotBuisitor {
	/** UID */
	private static final long serialVersionUID = 1654996309645415223L;
	/** Buisitor */
	private final Buisitor buisitor = new Buisitor(this);
	/** Anzeige? */
	private boolean shouldBeDisplayed = false;

	/**
	 * neuer GUI-Buisitor
	 */
	public GuiBotBuisitor() {
		super(true); // Double-Buffering an
	}

	/**
	 * 
	 * @param o Objekt
	 * @param bot Bot
	 */
	public void visit(Object o, Bot bot) {
		if (buisitor.dispatchBuisit(o) > 0)
			shouldBeDisplayed = true;
		if (buisitor.dispatchBuisit(o, bot) > 0)
			shouldBeDisplayed = true;
	}

	/**
	 * @return shouldBeDisplayed
	 */
	public boolean shouldBeDisplayed() {
		return shouldBeDisplayed;
	}
}
