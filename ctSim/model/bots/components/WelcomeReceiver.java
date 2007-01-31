package ctSim.model.bots.components;

import java.net.ProtocolException;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.Command.SubCode;
import ctSim.model.bots.components.BotComponent.CanRead;

//$$ doc
public class WelcomeReceiver extends BotComponent<Void> implements CanRead {
	private final SubCode expectedForWelcome;

	public WelcomeReceiver(SubCode expectedForWelcome) {
		super(null);
		this.expectedForWelcome = expectedForWelcome;
	}

	@Override
	public String getDescription() {
		return "Willkommenskommando-Auswerte-Ding";
	}

	public Code getHotCmdCode() {
		return Command.Code.WELCOME;
	}

	public void readFrom(Command c) throws ProtocolException {
		if (! c.has(expectedForWelcome)) {
			throw new ProtocolException("Willkommenskommando empfangen, " +
					"das nicht den erwarteten Subcode "+expectedForWelcome+
					" hatte");
		}
	}

	@Override
	public void updateExternalModel() {
		// No-op
	}

	@Override
	public String getName() {
		// No-op
		return null;
	}
}
