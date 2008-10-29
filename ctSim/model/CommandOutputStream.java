package ctSim.model;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import ctSim.util.BotID;
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
	private byte seq = 0;

	/** An wen gehen all die schoenen Pakete? */
	private BotID to = new BotID();
	
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
		c.setTo(to);
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

	/** 
	 * Setzt den Empfaenger all der schoenen Kommandos
	 * @param to
	 */
	public void setTo(BotID to) {
		this.to = to;
	}

	/** 
	 * Liefert den Empfaenger all der schoenen Kommandos
	 * @return Empfaenger-Id
	 */
	public BotID getTo() {
		return new BotID(to);
	}
}
