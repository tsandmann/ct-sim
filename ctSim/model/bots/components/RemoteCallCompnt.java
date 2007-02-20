package ctSim.model.bots.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ProtocolException;
import java.util.List;

import javax.swing.SpinnerNumberModel;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.util.Runnable1;

//$$ doc
/**
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class RemoteCallCompnt extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.model.bots.components.RemoteCallCompnt");

	public enum BehaviorExitStatus {
		FAILURE(0, "Fehler"),
		SUCCESS(1, "Fertig"),
		CANCELLED(3, "Abgebrochen");

		private final int onTheWire;
		private final String displayableName;

		private BehaviorExitStatus(int onTheWire, String displayableName) {
			this.onTheWire = onTheWire;
			this.displayableName = displayableName;
		}

		public static BehaviorExitStatus decode(int received) {
			for (BehaviorExitStatus b : values()) {
				if (received == b.onTheWire)
					return b;
			}
			lg.warn("Ung\u00FCltiger Exit-Status '"+received+"' empfangen; " +
					"behandle wie Exit-Status f\u00FCr Fehler");
			return FAILURE;
		}

		@Override
		public String toString() {
			return displayableName;
		}
	}

	/**
	 * Repr&auml;sentiert ein Behavior, wie es vom Bot geliefert wurde. Ein
	 * Behavior ist eine Funktion (Routine), die vom Sim aus aufgerufen werden
	 * kann. Es hat:
	 * <ul>
	 * <li>einen Namen</li>
	 * <li>eine Liste von {@link Parameter}n (jeder Parameter hat einen Namen
	 * und als Typ int oder float)</li>
	 * <li>eine Methode, um ihn aufzurufen, d.h. an den Bot die Anforderung
	 * abzusetzen "f&uuml;hr dieses Behavior aus".</li>
	 * </ul>
	 */
	public class Behavior implements Cloneable {
		/**
		 * Name des Behavior. Wird dem Benutzer angezeigt und dient dem
		 * Bot-Steuercode gegen&uuml;ber als eindeutiger Bezeichner des
		 * Behavior. Wenn der Bot die Liste schickt, welche Behaviors er
		 * unterst&uuml;tzt, steht der Name in einem Feld mit fester L&auml;nge:
		 * Nach dem Namen an sich kommt eine Anzahl Null-Bytes, um auf die im
		 * Protokoll vorgeschriebenen soundsoviel Bytes zu kommen. Trotzdem
		 * enth&auml;lt diese Variable nur den Namen an sich, die Null-Bytes
		 * werden abgeschnitten.
		 */
		private final String name;

		/** Die Parameter des Behavior. */
		// Waere auch final, aber clone() muss das aendern koennen
		private List<Parameter> parameters = Misc.newList();

		/**
		 * Erstellt eine {@code Behavior}-Instanz mit dem angegebenen Namen.
		 * Hinten am Namen h&auml;ngende Null-Bytes werden ignoriert.
		 */
		private Behavior(final byte[] name) {
			// Letztes Byte muss terminierendes Nullbyte sein
			assert name[name.length - 1] == 0;
			// trim() schneidet die ganzen Nullbytes ab, die zum Padding da sind
			this.name = new String(name).trim();
		}

		/**
		 * <p>
		 * F&uuml;hrt das Behavior mit den gegenw&auml;rtigen Parameterwerten
		 * aus. Der Behavior serialisiert sich und seine Parameter in ein
		 * Byte-Array. In diesem stehen:
		 * <ol>
		 * <li>Name des Behavior (variable L&auml;nge)</li>
		 * <li>ein Null-Byte</li>
		 * <li>vier Byte f&uuml;r den serialisierten Parameter 1 ({@linkplain Parameter#writeTo(ByteArrayOutputStream) Wie serialisiert der sich?})</li>
		 * <li>vier Byte f&uuml;r den serialisierten Parameter 2</li>
		 * <li>&hellip;</li>
		 * <li>vier Byte f&uuml;r den serialisierten letzten Parameter.</li>
		 * </ol>
		 * </p>
		 * <p>
		 * Das Behavior &uuml;bergibt das Byte-Array an seine
		 * {@link RemoteCallCompnt}, die es an den Bot sendet. Das Senden
		 * erfolgt nicht sofort, sondern zu einem sp&auml;teren Zeitpunkt, falls
		 * die {@code RemoteCallCompnt} im synchronen Modus l&auml;uft.
		 * </p>
		 *
		 * @throws IOException Falls die {@code RemoteCallCompnt} im asynchronen
		 * Modus l&auml;uft und beim Senden ein E/A-Problem auftritt.
		 */
		@SuppressWarnings("synthetic-access")
		public void call() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			String msg = "Sende Remote-Call, um Behavior "+getName()+
				" aufzurufen";
			bytes.write(name.getBytes());
			bytes.write(0); // terminierendes Nullbyte
			for (Parameter p : parameters) {
				msg += ", "+p.fullName+" = "+p.getNumber();
				p.writeTo(bytes);
			}
			lg.info(msg);
			executeBehavior(bytes.toByteArray());
		}

		@Override
		public Behavior clone() {
			Behavior rv;
			try {
				rv = (Behavior)super.clone();
			} catch (CloneNotSupportedException e) {
				// Kann nicht passieren, wir rufen Object.clone() auf
				throw new AssertionError(e);
			}
			// Nach super.clone() zeigt rv's parameters-Ref auf das Objekt, auf
			// das auch unsere parameters-Ref zeigt
			rv.parameters = Misc.newList();
			for (Parameter p : parameters)
				rv.parameters.add(p.clone());
			return rv;
		}

		public String getName() {
			return name;
		}

		public List<Parameter> getParameters() {
			return parameters;
		}
	}

	public abstract static class Parameter extends SpinnerNumberModel
	implements Cloneable {
		private static final long serialVersionUID = - 6872518108087767878L;

		public final String name;
		public final String fullName;

		Parameter(final String fullName) throws ProtocolException {
			this.fullName = fullName;
			try {
				this.name = fullName.split(" ")[1];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new ProtocolException("Ung\u00FCltiger Parametertyp '"+
					fullName+"' (muss Leerzeichen enthalten)");
			}
		}

		// Gemaess Remote-Call-Protokoll 32 Bit (4 Byte) schreiben -- LE
		public void writeTo(ByteArrayOutputStream bytes) {
			int value = getIntRepresentation();
			// Konvertierung nach Little Endian
			bytes.write((value >>  0) & 255);
			bytes.write((value >>  8) & 255);
			bytes.write((value >> 16) & 255);
			bytes.write((value >> 24) & 255);
		}

		abstract int getIntRepresentation();

		@Override
		public Parameter clone() {
			Parameter rv;
			try {
				rv = getClass().getConstructor(String.class).newInstance(
					fullName);
			} catch (InstantiationException e) {
				// Passiert nur, wenn einer den Konstruktor aendert
				throw new AssertionError(e);
			} catch (IllegalAccessException e) {
				// Dito
				throw new AssertionError(e);
			} catch (NoSuchMethodException e) {
				// Dito
				throw new AssertionError(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
			rv.setValue(getValue());
			rv.setMinimum(getMinimum());
			rv.setMaximum(getMaximum());
			return rv;
		}
	}

	static class IntParam extends Parameter {
		private static final long serialVersionUID = 1030435953303383535L;

		public IntParam(String name) throws ProtocolException {
			super(name);

			// 'name' ist z.B. "int16 dingsbums" oder "uint8 bla"
			boolean isUnsigned = name.charAt(0) == 'u';
			// int oder uint am Anfang abschneiden
			String s = name.split("u?int")[0];
			s = s.split(" ")[0]; // alles nachm " " abschneiden
			try {
				int bitCount = Integer.parseInt(s);
				// Gesundheitscheck
				if (bitCount < 0) {
					throw new ProtocolException("Bitzahl des Int ist "+
						bitCount+" (muss aber positiv sein)");
				}
				if (bitCount > 16) {
					throw new ProtocolException("Ints mit mehr als 16 Bit " +
							"nicht unterst\u00FCtzt");
				}
				// Hauptarbeit
				if (isUnsigned) {
					setMinimum(0);
					setMaximum(+2^bitCount - 1);
				} else {
					// Fuer signed int
					bitCount--; // 16 Bit sind -(2^15) bis +(2^15)-1
					setMinimum(-2^bitCount);
					setMaximum(+2^bitCount - 1);
				}
			} catch (NumberFormatException e) {
				// ignorieren, dann halt ohne Minimum + Maximum
			}
		}

		@Override
		protected int getIntRepresentation() {
			return getNumber().intValue();
		}
	}

	static class FloatParam extends Parameter {
		private static final long serialVersionUID = 9039021104381285572L;

		public FloatParam(String name) throws ProtocolException {
			super(name);
		}

		@Override
		protected int getIntRepresentation() {
			return Float.floatToIntBits(getNumber().floatValue());
		}
	}

	private CommandOutputStream asyncOut;
	private boolean syncListCallsPending = false;
	private byte[] syncOrderPayload = null;
	private boolean syncAbortCurrentPending = false;

	private final List<Runnable1<Behavior>> behaviorListeners = Misc.newList();
	private final List<Runnable1<BehaviorExitStatus>> doneListeners =
		Misc.newList();
	// Internes Model; cacht Behavior-Instanzen nachdem sie empfangen wurden
	// und bis sie an die behaviorListener gegeben werden
	private final List<Behavior> newBehaviors = Misc.newList();

	public RemoteCallCompnt() {
		super(null);
	}

	@Override
	public String getDescription() {
		return "Ding, was dem Bot Remote-Calls (Funktionsaufrufe) schicken " +
				"kann";
	}

	@Override
	public String getName() {
		return "Remote-Calls";
	}

	@Override
	public void updateExternalModel() {
		while (! newBehaviors.isEmpty()) {
			Behavior b = newBehaviors.remove(0);
			for (Runnable1<Behavior> li : behaviorListeners)
				li.run(b);
		}
	}

	public Code getHotCmdCode() { return Command.Code.REMOTE_CALL; }

	// E/A -- Schreiben ///////////////////////////////////////////////////////

	public void setAsyncWriteStream(CommandOutputStream s) {
		asyncOut = s;
	}

	@Override
	public void askForWrite(CommandOutputStream s) {
		if (! writesSynchronously())
			return;

		if (syncAbortCurrentPending) {
			prepareAbortCmd(s);
			syncAbortCurrentPending = false;
		} else if (syncListCallsPending) {
			prepareListCmd(s);
			syncListCallsPending = false;
		} else if (syncOrderPayload != null) {
			prepareOrderCmd(s, syncOrderPayload);
			syncOrderPayload = null;
		}
	}

	public void abortCurrentBehavior() throws IOException {
		lg.info("Breche gegenw\u00E4rtig laufendes Behavior ab");
		if (writesAsynchronously()) {
			prepareAbortCmd(asyncOut);
			asyncOut.flush();
		} else
			syncAbortCurrentPending = true;
	}

	public void listRemoteCalls() throws IOException {
		lg.info("Fordere beim Bot eine Liste der m\u00F6glichen " +
			"Behaviors an");
		if (writesAsynchronously()) {
			prepareListCmd(asyncOut);
			asyncOut.flush();
		} else
			syncListCallsPending = true;
	}

	private void executeBehavior(byte[] payload) throws IOException {
		if (writesAsynchronously()) {
			prepareOrderCmd(asyncOut, payload);
			asyncOut.flush();
		} else
			syncOrderPayload = payload;
	}

	private void prepareListCmd(CommandOutputStream s) {
		s.getCommand(getHotCmdCode()).setSubCmdCode(
			Command.SubCode.REMOTE_CALL_LIST);
	}

	private void prepareAbortCmd(CommandOutputStream s) {
		s.getCommand(getHotCmdCode()).setSubCmdCode(
			Command.SubCode.REMOTE_CALL_ABORT);
	}

	private void prepareOrderCmd(CommandOutputStream s, byte[] payload) {
		Command c = s.getCommand(getHotCmdCode());
		c.setSubCmdCode(Command.SubCode.REMOTE_CALL_ORDER);
		c.setPayload(payload);
	}

	/**
	 * No-op: Wir implementieren die, weil wir laut Interface m&uuml;ssen, aber
	 * wir brauchen die nicht weil wir ja
	 * {@link #askForWrite(CommandOutputStream) askForWrite()}
	 * &uuml;berschrieben haben.
	 */
	public void writeTo(@SuppressWarnings("unused") Command c) { /* No-op */ }

	// E/A -- Lesen ///////////////////////////////////////////////////////////

	public void readFrom(Command c) throws ProtocolException {
		if (c.has(Command.SubCode.REMOTE_CALL_ENTRY))
			newBehaviors.add(decodeBehavior(c));
		else if (c.has(Command.SubCode.REMOTE_CALL_DONE))
			fireDoneEvent(c.getDataL());
	}

	@SuppressWarnings("synthetic-access")
	private Behavior decodeBehavior(Command command)
	throws ProtocolException {
		ByteArrayInputStream b = new ByteArrayInputStream(command.getPayload());
		int numParms = b.read();
		// Da kommen irgendwelche komischen 3 Bytes, ignorieren
		b.skip(3);
		// 20 Byte + terminierendes Nullbyte
		Behavior rv = new Behavior(readArray(b, 20+1));
		// Parameterangaben lesen in jedem Fall, verarbeiten vielleicht
		String[] parmNames = new String(readArray(b, 40+1)).split(",");
		if (numParms > 0) {
			if (numParms != parmNames.length) {
				lg.warn("Bot-Code scheint fehlerhaft; hat " +
						"angek\u00FCndigt, der Remote-Call hat "+numParms+
						" Parameter; tats\u00E4chlich hat er "+parmNames.length+
						" Parameter; Gehe von "+parmNames.length+
						" Parametern aus; Kommando folgt"+command);
			}
			for (int i = 0; i < parmNames.length; i++) {
				String name = parmNames[i].trim();
				Parameter p;
				if (name.startsWith("int")
				||  name.startsWith("uint"))
					p = new IntParam(name);
				else if (name.startsWith("float"))
					p = new FloatParam(name);
				else {
					throw new ProtocolException("Unbekannter Parametertyp '"+
						name+"'");
				}
				rv.getParameters().add(p);
			}
		}
		return rv;
	}

	private byte[] readArray(ByteArrayInputStream is, int lengthOfString)
	throws ProtocolException {
		byte[] rv = new byte[lengthOfString];
		try {
			is.read(rv);
		} catch (IOException e) {
			throw new ProtocolException("Remote Call Entry: Ung\u00FCltige " +
					"Payload");
		}
		return rv;
	}

	private void fireDoneEvent(int rCallExitStatus) {
		BehaviorExitStatus status = BehaviorExitStatus.decode(rCallExitStatus);
		lg.info("Bot meldet: Behavior erledigt; Status "+status);
		for (Runnable1<BehaviorExitStatus> li : doneListeners)
			li.run(status);
	}

	// Listenerverwaltung /////////////////////////////////////////////////////

	public void addDoneListener(
	Runnable1<BehaviorExitStatus> willBeCalledWhenRCallDone) {
		if (willBeCalledWhenRCallDone == null)
			throw new NullPointerException();
		doneListeners.add(willBeCalledWhenRCallDone);
	}

	public void addBehaviorListener(
	Runnable1<Behavior> willBeCalledWhenBotSentBehavior) {
		if (willBeCalledWhenBotSentBehavior == null)
			throw new NullPointerException();
		behaviorListeners.add(willBeCalledWhenBotSentBehavior);
	}
}
