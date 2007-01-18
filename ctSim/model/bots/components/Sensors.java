package ctSim.model.bots.components;

import java.io.IOException;

import javax.swing.SpinnerNumberModel;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.model.bots.components.BotComponent.SimpleSensor;
import ctSim.view.gui.RemoteControlViewer;

//	$$ doc
public class Sensors {
	/**
	 * Klasse der Liniensensoren
	 */
	public static class Line extends NumberTwin
	implements SimpleSensor, CanWrite {
		@Override
		protected String getBaseDescription() {
			return "Liniensensor [0; 1023]";
		}

		@Override protected String getBaseName() { return "Line"; }
		public Line(boolean isLeft) { super(isLeft); }
		public Code getHotCmdCode() { return Code.SENS_LINE; }
	}

	/**
	 * Klasse der Lichtsensoren
	 */
	public static class Light extends NumberTwin
	implements SimpleSensor, CanWrite {
		@Override
		protected String getBaseDescription() {
			return "Lichtsensor";
		}

		@Override protected String getBaseName() { return "Light"; }
		public Light(boolean isLeft) { super(isLeft); }
		public Code getHotCmdCode() { return Code.SENS_LDR; }
	}

	/**
	 * Klasse der Rad-Encoder
	 */
	public static class Encoder extends NumberTwin
	implements SimpleSensor, CanWrite {
		@Override
		protected String getBaseDescription() {
			return "Rad-Encoder-Sensor";
		}

		public Encoder(boolean isLeft) { super(isLeft); }
		@Override protected String getBaseName() { return "Enc"; }
		public Code getHotCmdCode() { return Code.SENS_ENC; }
	}

	/** Abstandssensor vom Typ GP2D12
	 */
	public static class Distance extends NumberTwin
	implements SimpleSensor, CanWrite {
		@Override
		protected String getBaseDescription() {
			return "Abstandssensor";
		}

		@Override protected String getBaseName() { return "Ir"; }
		public Distance(boolean isLeft) { super(isLeft); }
		public Code getHotCmdCode() { return Code.SENS_IR; }
	}

	/**
	 * Klasse der Abgrundsensoren
	 */
	public static class Border extends NumberTwin
	implements SimpleSensor, CanWrite {
		@Override
		protected String getBaseDescription() {
			return "Abgrundsensor [0; 1023]";
		}

		@Override protected String getBaseName() { return "Border"; }
		public Border(boolean isLeft) { super(isLeft); }
		public Code getHotCmdCode() { return Code.SENS_BORDER; }
	}

	public static class Mouse extends NumberTwin
	implements CanWrite, SimpleSensor {
		/**
		 * {@code true}: Diese Instanz verwaltet die X-Komponente,
		 * {@code false}: Y-Komponente. Im Konstruktor wird dieser Wert an den
		 * Superkonstruktor gegeben, wo er entscheidet, ob unser Wert im dataL-
		 * oder dataR-Feld eines Commands &uuml;bermittelt wird (sowohl
		 * Schreiben als auch Lesen). Die Zuordnung ist also X = dataL, Y =
		 * dataR.
		 */
		private boolean isX;

		public Mouse(boolean isX) {
			super(isX);
			this.isX = isX;
		}

		@Override
		public String getName() {
			return "Mouse" + (isX ? "X" : "Y");
		}

		@Override
		public String getDescription() {
			return "Maus-Sensor (" +
				(isX ? "X" : "Y") +
				"-Komponente)";
		}

		@Override protected String getBaseDescription() { return ""; }
		@Override protected String getBaseName() { return ""; }
		public Code getHotCmdCode() { return Code.SENS_MOUSE; }
	}

	/**
	 * <p>
	 * Software-Emulation der Fernbedienung. Spezielles Verhalten beim
	 * Schreiben: Nur, wenn sein Wert ungleich 0 ist, sendet der Sensor.
	 * Anschlie&szlig;end setzt er seinen Wert auf 0 zur&uuml;ck, so dass pro
	 * Tastendruck in der Fernbedienungs-Gui ein Command auf den Draht gegeben
	 * wird.
	 * </p>
	 * <p>
	 * <strong>c't-Bot-Protokoll:</strong> Der Fernbedienungssensor sendet eine
	 * Ganzzahl, die f&uuml;r einen Knopf steht (Zuordnung siehe
	 * {@link RemoteControlViewer}). Die Zahl steht im Feld {@code dataL}, die
	 * &uuml;brigen Felder werden nicht ver&auml;ndert.
	 * </p>
	 *
	 * @author Peter K&ouml;nig
	 * @author Hendrik Krau&szlig; &lt;<a
	 * href="mailto:hkr@heise.de">hkr@heise.de</a>>
	 */
	public static class RemoteControl extends BotComponent<Void>
	implements CanWrite, CanWriteAsynchronously {
		private CommandOutputStream asyncOut;
		private int syncPendingRcCode = 0;

		public void setAsyncWriteStream(CommandOutputStream s) {
			asyncOut = s;
		}

		public void writeTo(Command c) {
			if (syncPendingRcCode == 0)
				return;
			c.setDataL(syncPendingRcCode);
			syncPendingRcCode = 0;
		}

		public void send(int rc5Code) throws IOException {
			if (writesAsynchronously()) {
				// Gleich schreiben
				synchronized (asyncOut) {
					asyncOut.getCommand(getHotCmdCode()).setDataL(rc5Code);
					asyncOut.flush();
				}
			} else
				// Puffern bis zum writeTo
				syncPendingRcCode = rc5Code;
		}

		@Override public String getName() { return "RC5"; }
		@Override public String getDescription() { return "Fernbedienung"; }
		public RemoteControl() { super(null); }
		public Code getHotCmdCode() { return Code.SENS_RC5; }
	}

	public static class Door extends BotComponent<SpinnerNumberModel>
	implements SimpleSensor, CanWrite {
		@Override
		public String getDescription() {
			return "Affe-tot-Sensor";
		}

		@Override public String getName() { return "DoorSens"; }
		public Door() { super(new SpinnerNumberModel()); }
		public Code getHotCmdCode() { return Code.SENS_DOOR; }

		public void writeTo(Command c) {
			c.setDataL(getModel().getNumber().intValue());
		}
	}

	public static class Trans extends BotComponent<SpinnerNumberModel>
	implements SimpleSensor, CanWrite {
		@Override
		public String getDescription() {
			return "Lichtschranke im Transportfach";
		}

		@Override public String getName() { return "Trans"; }
		public Trans() { super(new SpinnerNumberModel()); }
		public Code getHotCmdCode() { return Code.SENS_TRANS; }

		public void writeTo(Command c) {
			c.setDataL(getModel().getNumber().intValue());
		}
	}

	public static class Error extends BotComponent<SpinnerNumberModel>
	implements SimpleSensor, CanWrite {
		@Override
		public String getDescription() {
			return "Sensor f\u00FCr Motor- oder Batteriefehler";
		}

		@Override public String getName() { return "Error"; }
		public Error() { super(new SpinnerNumberModel()); }
		public Code getHotCmdCode() { return Code.SENS_ERROR; }

		public void writeTo(Command c) {
			c.setDataL(getModel().getNumber().intValue());
		}
	}
}
