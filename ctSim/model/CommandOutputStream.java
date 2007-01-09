package ctSim.model;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

//$$ doc
public class CommandOutputStream {
	private final Map<Command.Code, Command> buffer =
		new HashMap<Command.Code, Command>();
	private Command doneCommand = null;
	private final OutputStream underlyingStream;
	private int seq = 0; //$$ Die Sequenznummer wird irgendwann groesser als 2^16. Spaeter wrappt die auch und wird negativ. Damit sollte man irgendwie umgehen

	public CommandOutputStream(OutputStream underlyingStream) {
		this.underlyingStream = underlyingStream;
	}

	public synchronized Command getCommand(Command.Code c) {
		// Spezialfall "DONE"
		if (c == Command.Code.DONE) {
			if (doneCommand == null)
				doneCommand = new Command(Command.Code.DONE);
			return doneCommand;
		}

		// Alle andern
		if (! buffer.containsKey(c))
			buffer.put(c, new Command(c));
		return buffer.get(c);
	}

	private synchronized void write(Command c) throws IOException {
		c.setSeq(seq++);
		underlyingStream.write(c.getCommandBytes());
	}

	public synchronized void flush() throws IOException {
		for (Command c : buffer.values())
			write(c);
		write(doneCommand);
		buffer.clear();
	}
}
