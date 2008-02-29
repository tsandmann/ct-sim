package ctSim.model;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import ctSim.util.Misc;

/**
 * Output-Stream fuer Kommandos
 */
public class CommandOutputStream {
	/** Puffer */
	private final Map<Command.Code, Command> buffer = Misc.newMap();
	/** der Strean */
	private final BufferedOutputStream underlyingStream;
	/** Sequenznummer */
	private int seq = 0; //$$ Die Sequenznummer wird irgendwann groesser das Feld, in dem sie uebermittelt wird. Spaeter wrappt die auch und wird negativ. Damit sollte man irgendwie umgehen

	/**
	 * @param underlyingStream
	 */
	public CommandOutputStream(OutputStream underlyingStream) {
		if (underlyingStream == null)
			throw new NullPointerException();
		this.underlyingStream = new BufferedOutputStream(underlyingStream);
	}

	/**
	 * @param c Command-Code
	 * @return Command
	 */
	public synchronized Command getCommand(Command.Code c) {
		if (! buffer.containsKey(c))
			buffer.put(c, new Command(c));
		return buffer.get(c);
	}

	/**
	 * Schreibt ein Kommando, toleriert null schweigend
	 * @param c Kommando
	 * @throws IOException 
	 */
	private synchronized void write(Command c) throws IOException {
		if (c == null)
			return;
		c.setSeq(seq++);
		underlyingStream.write(c.getCommandBytes());
	}

	/**
	 * Reihenfolge wichtig, sonst kommt der Bot-Steuercode durcheinander 
	 * 1. Alles ausser SENS_ERROR und DONE
	 * 2. SENS_ERROR
	 * 3. DONE
	 * @throws IOException
	 */
	public synchronized void flush() throws IOException {
		Command error = buffer.remove(Command.Code.SENS_ERROR); // ist evtl null
		Command done = buffer.remove(Command.Code.DONE); // ist evtl null

		for (Command c : buffer.values())
			write(c);
		write(error); 
		write(done);

		underlyingStream.flush();
		buffer.clear();
	}
}
