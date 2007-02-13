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
public class RemoteCallCompnt extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
	final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.model.bots.components.RemoteCallCompnt");

	public class Behavior implements Cloneable {
		private final byte[] name;
		// Waere auch final, aber clone() muss das aendern koennen
		private List<Parameter> parameters = Misc.newList();

		Behavior(final byte[] name) {
			this.name = name;
		}

		public void call() throws IOException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			String msg = "Sende Remote-Call "+getName();
			bytes.write(name);
			bytes.write(0); // terminierendes Nullbyte
			for (Parameter p : parameters) {
				msg += ", "+p.fullName+" = "+p.getNumber();
				p.writeTo(bytes);
			}
			lg.info(msg);
			doRemoteCall(bytes.toByteArray());
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
			return new String(name).trim();
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

		public void writeTo(ByteArrayOutputStream bytes) {
			bytes.write(Integer.reverseBytes(getIntRepresentation()));
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
		private static final long serialVersionUID =
			1030435953303383535L;

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
				// ignorieren
			}
		}

		@Override
		protected int getIntRepresentation() {
			return getNumber().intValue();
		}
	}

	static class FloatParam extends Parameter {
		private static final long serialVersionUID =
			9039021104381285572L;

		public FloatParam(String name) throws ProtocolException {
			super(name);
		}

		@Override
		protected int getIntRepresentation() {
			return Float.floatToIntBits(getNumber().floatValue());
		}
	}

	private CommandOutputStream asyncOut;
	private boolean syncCallListPending = false;
	private byte[] syncRCallPayload = null;
	private final List<Runnable1<Behavior>> behaviorListeners = Misc.newList();
	private final List<Runnable1<Integer>> doneListeners = Misc.newList();
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

	private void prepareListCmd(Command c) {
		c.setSubCmdCode(Command.SubCode.REMOTE_CALL_LIST);
	}

	public void requestRemoteCallList() throws IOException {
		if (writesAsynchronously()) {
			prepareListCmd(asyncOut.getCommand(getHotCmdCode()));
			asyncOut.flush();
		} else
			syncCallListPending = true;
	}

	private void prepareRCallCmd(Command c, byte[] payload) {
		c.setSubCmdCode(Command.SubCode.REMOTE_CALL_ORDER);
		c.setPayload(payload);
	}

	void doRemoteCall(byte[] payload) throws IOException {
		if (writesAsynchronously()) {
			prepareRCallCmd(asyncOut.getCommand(getHotCmdCode()), payload);
			asyncOut.flush();
		} else
			syncRCallPayload = payload;
	}

	public void writeTo(Command c) {
		if (syncCallListPending) {
			syncCallListPending = false;
			c.setSubCmdCode(Command.SubCode.REMOTE_CALL_LIST);
		}
		else if (syncRCallPayload != null) {
			prepareRCallCmd(c, syncRCallPayload);
			syncRCallPayload = null;
		}
	}

	public Code getHotCmdCode() { return Command.Code.REMOTE_CALL; }

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

	public void readFrom(Command c) throws ProtocolException {
		if (c.has(Command.SubCode.REMOTE_CALL_ENTRY))
			newBehaviors.add(decodeBehavior(c.getPayload()));
		else if (c.has(Command.SubCode.REMOTE_CALL_DONE))
			fireDoneEvent(c.getDataL());
	}

	private void fireDoneEvent(int rCallExitStatus) {
		lg.info("Bot meldet: Remote-Call erledigt; Status "+
			(rCallExitStatus == 1 ? "ok" : "Fehler"));
		for (Runnable1<Integer> li : doneListeners )
			li.run(rCallExitStatus);
	}

	private Behavior decodeBehavior(byte[] payload) throws ProtocolException {
		ByteArrayInputStream b = new ByteArrayInputStream(payload);
		b.skip(4); // Pointer (4 Byte) ignorieren
		int numParms = b.read();
		// Parameterlaengen ignorieren -- wir parsen die spaeter ausm String
		b.skip(numParms);
		Behavior rv = new Behavior(readArray(b, 20+1));
		// Parameterangaben lesen in jedem Fall, verarbeiten vielleicht
		String[] parmNames = new String(readArray(b, 40+1)).split(",");
		if (numParms > 0) {
			if (numParms != parmNames.length)
				throw new ProtocolException(numParms+" != "+parmNames.length);
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

	public void setAsyncWriteStream(CommandOutputStream s) {
		asyncOut = s;
	}

	public void addDoneListener(Runnable1<Integer> willBeCalledWhenRCallDone) {
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
