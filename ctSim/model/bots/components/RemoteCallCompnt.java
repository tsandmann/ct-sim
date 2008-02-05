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

/**
 * Remote-Call Komponente
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class RemoteCallCompnt extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.model.bots.components.RemoteCallCompnt");

	/**
	 * Exit-Status eines Verhaltens
	 */
	public enum BehaviorExitStatus {
		/**
		 * Fehler
		 */
		FAILURE(0, "Fehler"),
		/**
		 * Fertig, alles OK
		 */
		SUCCESS(1, "Fertig"),
		/**
		 * Abgebrochen
		 */
		CANCELLED(3, "Abgebrochen");

		/** Status auf der Leitung */
		private final int onTheWire;
		/** Name */
		private final String displayableName;

		/**
		 * Exit-Status eines Verhaltens
		 * @param onTheWire Code auf der Leitung
		 * @param displayableName Name
		 */
		private BehaviorExitStatus(int onTheWire, String displayableName) {
			this.onTheWire = onTheWire;
			this.displayableName = displayableName;
		}

		/**
		 * Decodiert einen Verhaltensstatus
		 * @param received Status als int
		 * @return Status
		 */
		public static BehaviorExitStatus decode(int received) {
			for (BehaviorExitStatus b : values()) {
				if (received == b.onTheWire)
					return b;
			}
			lg.warn("Ung\u00FCltiger Exit-Status '"+received+"' empfangen; " +
					"behandle wie Exit-Status f\u00FCr Fehler");
			return FAILURE;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
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
		 * @param name Name der Botenfunktion des Verhaltens
		 */
		public Behavior(final byte[] name) {
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

		/**
		 * @see java.lang.Object#clone()
		 */
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

		/**
		 * @return Verhaltensname (Botenfunktion)
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return Parameterliste zum Verhalten
		 */
		public List<Parameter> getParameters() {
			return parameters;
		}
	}

	/**
	 * Parameter fuer Remote-Calls
	 */
	public abstract static class Parameter extends SpinnerNumberModel
	implements Cloneable {
		/** UID */
		private static final long serialVersionUID = - 6872518108087767878L;

		/**
		 * Kurzname (ohne Typ)
		 */
		public final String name;
		
		/**
		 * Name (mit Typ)
		 */
		public final String fullName;

		/**
		 * Parameter
		 * @param fullName Parameter-Name mit Typ 
		 * @throws ProtocolException
		 */
		Parameter(final String fullName) throws ProtocolException {
			this.fullName = fullName;
			try {
				this.name = fullName.split(" ")[1];
			} catch (ArrayIndexOutOfBoundsException e) {
				throw new ProtocolException("Ung\u00FCltiger Parametertyp '"+
					fullName+"' (muss Leerzeichen enthalten)");
			}
		}

		/**
		 * Gemaess Remote-Call-Protokoll 32 Bit (4 Byte) schreiben -- LE
		 * @param bytes Daten
		 */
		public void writeTo(ByteArrayOutputStream bytes) {
			int value = getIntRepresentation();
			// Konvertierung nach Little Endian
			bytes.write((value >>  0) & 255);
			bytes.write((value >>  8) & 255);
			bytes.write((value >> 16) & 255);
			bytes.write((value >> 24) & 255);
		}

		/**
		 * @return Int eines Parameters
		 */
		abstract int getIntRepresentation();

		/**
		 * @see java.lang.Object#clone()
		 */
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

	/**
	 * Int-Parameter (fuer RemoteCalls)
	 */
	public static class IntParam extends Parameter {
		/** UID */
		private static final long serialVersionUID = 1030435953303383535L;

		/**
		 * @param name	Parameterbeschreibung, z.B. "int16 dingsbums" oder "uint8 bla"
		 * @throws ProtocolException
		 */
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

		/**
		 * @see ctSim.model.bots.components.RemoteCallCompnt.Parameter#getIntRepresentation()
		 */
		@Override
		protected int getIntRepresentation() {
			return getNumber().intValue();
		}
	}

	/**
	 * Float-Parameter (fuer RemoteCalls)
	 */
	public static class FloatParam extends Parameter {
		/** UID */
		private static final long serialVersionUID = 9039021104381285572L;

		/**
		 * @param name Parameterbeschreibung, z.B. "float dingsbums"
		 * @throws ProtocolException
		 */
		public FloatParam(String name) throws ProtocolException {
			super(name);
		}

		/**
		 * @see ctSim.model.bots.components.RemoteCallCompnt.Parameter#getIntRepresentation()
		 */
		@Override
		protected int getIntRepresentation() {
			return Float.floatToIntBits(getNumber().floatValue());
		}
	}

	/** Kommando-Outputstream */
	private CommandOutputStream asyncOut;
	/** List-Calls austehend ?*/
	private boolean syncListCallsPending = false;
	/** Payload */
	private byte[] syncOrderPayload = null;
	/** Austehende Daten verwerfen? */
	private boolean syncAbortCurrentPending = false;

	/** Verhaltens-Listener */
	private final List<Runnable1<Behavior>> behaviorListeners = Misc.newList();
	/** Done-Listener */
	private final List<Runnable1<BehaviorExitStatus>> doneListeners =
		Misc.newList();
	/** Internes Model; cacht Behavior-Instanzen nachdem sie empfangen wurden
	 * und bis sie an die behaviorListener gegeben werden
	 */
	private final List<Behavior> newBehaviors = Misc.newList();

	/**
	 * RemoteCallCompnt
	 */
	public RemoteCallCompnt() {
		super(null);
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Ding, was dem Bot Remote-Calls (Funktionsaufrufe) schicken " +
				"kann";
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#getName()
	 */
	@Override
	public String getName() {
		return "Remote-Calls";
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public void updateExternalModel() {
		while (! newBehaviors.isEmpty()) {
			Behavior b = newBehaviors.remove(0);
			for (Runnable1<Behavior> li : behaviorListeners)
				li.run(b);
		}
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
	 */
	public Code getHotCmdCode() { return Command.Code.REMOTE_CALL; }

	// E/A -- Schreiben ///////////////////////////////////////////////////////

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanWriteAsynchronously#setAsyncWriteStream(ctSim.model.CommandOutputStream)
	 */
	public void setAsyncWriteStream(CommandOutputStream s) {
		asyncOut = s;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#askForWrite(ctSim.model.CommandOutputStream)
	 */
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

	/**
	 * Bricht das aktuelle Verhalten per RemoteCall ab
	 * @throws IOException
	 */
	public void abortCurrentBehavior() throws IOException {
		lg.info("Breche gegenw\u00E4rtig laufendes Behavior ab");
		if (writesAsynchronously()) {
			prepareAbortCmd(asyncOut);
			asyncOut.flush();
		} else
			syncAbortCurrentPending = true;
	}

	/**
	 * Fordert eine Liste aller RemoteCalls vom Bot an
	 * @throws IOException
	 */
	public void listRemoteCalls() throws IOException {
		lg.info("Fordere beim Bot eine Liste der m\u00F6glichen " +
			"Behaviors an");
		if (writesAsynchronously()) {
			prepareListCmd(asyncOut);
			asyncOut.flush();
		} else
			syncListCallsPending = true;
	}

	/**
	 * Fuehrt ein Verhalten aus
	 * @param payload Daten
	 * @throws IOException
	 */
	private void executeBehavior(byte[] payload) throws IOException {
		if (writesAsynchronously()) {
			prepareOrderCmd(asyncOut, payload);
			asyncOut.flush();
		} else
			syncOrderPayload = payload;
	}

	/**
	 * Bereitet das List-Kommando vor
	 * @param s Stream
	 */
	private void prepareListCmd(CommandOutputStream s) {
		s.getCommand(getHotCmdCode()).setSubCmdCode(
			Command.SubCode.REMOTE_CALL_LIST);
	}

	/**
	 * Bereitet das Abbruch-Kommando vor
	 * @param s Stream
	 */
	private void prepareAbortCmd(CommandOutputStream s) {
		s.getCommand(getHotCmdCode()).setSubCmdCode(
			Command.SubCode.REMOTE_CALL_ABORT);
	}

	/**
	 * Bereitet das Aufruf-Kommando vor
	 * @param s Stream
	 * @param payload Daten
	 */
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
	 * @param c Command
	 */
	public void writeTo(@SuppressWarnings("unused") Command c) { /* No-op */ }

	// E/A -- Lesen ///////////////////////////////////////////////////////////

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
	 */
	public void readFrom(Command c) throws ProtocolException {
		if (c.has(Command.SubCode.REMOTE_CALL_ENTRY))
			newBehaviors.add(decodeBehavior(c));
		else if (c.has(Command.SubCode.REMOTE_CALL_DONE))
			fireDoneEvent(c.getDataL());
	}

	/**
	 * Dekodiert ein Verhalten
	 * @param command Kommando
	 * @return Verhalten
	 * @throws ProtocolException
	 */
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

	/**
	 * List ein Array aus einem Stream ein
	 * @param is Input-Stream
	 * @param lengthOfString Laenge
	 * @return Array
	 * @throws ProtocolException
	 */
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

	/**
	 * Done-Event-Handler
	 * @param rCallExitStatus
	 */
	public void fireDoneEvent(int rCallExitStatus) {
		BehaviorExitStatus status = BehaviorExitStatus.decode(rCallExitStatus);
		lg.info("Bot meldet: Behavior erledigt; Status "+status);
		for (Runnable1<BehaviorExitStatus> li : doneListeners)
			li.run(status);
	}

	// Listenerverwaltung /////////////////////////////////////////////////////

	/**
	 * Done-Event-Listener
	 * @param willBeCalledWhenRCallDone
	 */
	public void addDoneListener(
	Runnable1<BehaviorExitStatus> willBeCalledWhenRCallDone) {
		if (willBeCalledWhenRCallDone == null)
			throw new NullPointerException();
		doneListeners.add(willBeCalledWhenRCallDone);
	}

	/**
	 * Listen-Event-Listener
	 * @param willBeCalledWhenBotSentBehavior
	 */
	public void addBehaviorListener(
	Runnable1<Behavior> willBeCalledWhenBotSentBehavior) {
		if (willBeCalledWhenBotSentBehavior == null)
			throw new NullPointerException();
		behaviorListeners.add(willBeCalledWhenBotSentBehavior);
	}
}
