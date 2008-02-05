package ctSim.model.bots.components;

import java.net.ProtocolException;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.Command.SubCode;
import ctSim.model.bots.components.BotComponent.CanRead;

/**
 * Handshake fuer Bot-Sim Connection
 */
public class WelcomeReceiver extends BotComponent<Void> implements CanRead {
	/** Subcode */
	private final SubCode expectedForWelcome;

	/**
	 * Handshake fuer Connection
	 * @param expectedForWelcome Subcode fuer neue Connection
	 */
	public WelcomeReceiver(SubCode expectedForWelcome) {
		super(null);
		this.expectedForWelcome = expectedForWelcome;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Willkommenskommando-Auswerte-Ding";
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
	 */
	public Code getHotCmdCode() {
		return Command.Code.WELCOME;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
	 */
	public void readFrom(Command c) throws ProtocolException {
		if (! c.has(expectedForWelcome)) {
			throw new ProtocolException("Willkommenskommando empfangen, " +
					"das nicht den erwarteten Subcode "+expectedForWelcome+
					" hatte");
		}
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public void updateExternalModel() {
		// No-op
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getName()
	 */
	@Override
	public String getName() {
		// No-op
		return null;
	}
}
