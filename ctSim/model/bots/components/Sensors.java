package ctSim.model.bots.components;

import java.io.IOException;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.model.bots.components.BotComponent.SimpleSensor;
import ctSim.util.FmtLogger;
import ctSim.view.gui.RemoteControlViewer;

//	$$ doc
public class Sensors {
	/**
	 * Klasse der Liniensensoren
	 */
	public static class Line extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
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
	implements SimpleSensor, CanRead, CanWrite {
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
	implements SimpleSensor, CanRead, CanWrite {
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
	implements SimpleSensor, CanRead, CanWrite {
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
	implements SimpleSensor, CanRead, CanWrite {
		@Override
		protected String getBaseDescription() {
			return "Abgrundsensor [0; 1023]";
		}

		@Override protected String getBaseName() { return "Border"; }
		public Border(boolean isLeft) { super(isLeft); }
		public Code getHotCmdCode() { return Code.SENS_BORDER; }
	}

	//$$ doc
	public static class Clock extends BotComponent<Void>
	implements CanRead, CanWrite {
		final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.components");

		private int lastTransmittedSimTime = -1;

		public synchronized void setSimTimeInMs(int simTime) {
			lastTransmittedSimTime = simTime;
			// Wir haben nur 16 Bit zur Verfuegung und 10.000 ist ne nette
			// Zahl ;-)
			//$$ "Nette Zahl"? Noch alle Nadeln anner Tanne?
			lastTransmittedSimTime %= 10000;
		}

		// Hat andere Aufgaben als {@link IntegerComponent#readFrom(Command)}
		public synchronized void readFrom(Command c) {
			if (c.getDataL() != lastTransmittedSimTime) {
				if (lastTransmittedSimTime == -1)
					// Fuer's allererste DONE-Kommando doch nicht warnen
					return;
				lg.warn("Bot-Steuercode hat unerwartete lastTransmitted-Zeit "+
						"gesendet (erwartet: %d, tats\u00E4chlich: %d); dies "+
						"deutet darauf hin, dass der Steuercode Simschritte "+
						"verschlafen hat",
						lastTransmittedSimTime, c.getDataL());
			}
		}

		public synchronized void writeTo(Command c) {
			c.setDataL(lastTransmittedSimTime);
		}

		@Override
		public void updateExternalModel() {
			// No-op, weil wir werden ja eh nicht angezeigt und operieren
			// direkt auf dem ExternalModel
		}

		@Override
		public String getDescription() {
			return "Simulationszeit-Uhr (Millisekunden)";
		}

		public Clock() { super(null); }
		@Override public String getName() { return "Uhr"; }
		public Code getHotCmdCode() { return Command.Code.DONE; }
	}

	//$$ doc
	public static class Mouse extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
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
			// Unicode 0394: grosses Delta
			return "Mouse \u0394" + (isX ? "X" : "Y");
		}

		@Override
		public String getDescription() {
			return "Maus-Sensor: " +
				(isX
				? "Drehgeschwindigkeit (X-Komponente)"
				: "Geradeaus-Geschwindigkeit (Y-Komponente)");
		}

		@Override protected String getBaseDescription() { return ""; }
		@Override protected String getBaseName() { return ""; }
		public Code getHotCmdCode() { return Code.SENS_MOUSE; }
	}

	/**
	 * <p>
	 * Software-Emulation der Fernbedienung.
	 * </p>
	 * <p>
	 * <strong>c't-Bot-Protokoll:</strong> Der Fernbedienungssensor sendet eine
	 * Ganzzahl, die f&uuml;r einen Knopf steht (Zuordnung siehe
	 * {@link RemoteControlViewer}). Die Zahl steht im Feld {@code dataL}, die
	 * &uuml;brigen Felder werden nicht ver&auml;ndert.
	 * </p>
	 */
	public static class RemoteControl extends BotComponent<Void>
	implements CanWrite, CanWriteAsynchronously {
		private CommandOutputStream asyncOut;
		private int syncPendingRcCode = 0;

		public void setAsyncWriteStream(CommandOutputStream s) {
			asyncOut = s;
		}

		public synchronized void writeTo(Command c) {
			if (syncPendingRcCode == 0) //$$$ t
				return;
			c.setDataL(syncPendingRcCode);
			syncPendingRcCode = 0;
		}

		public synchronized void send(int rc5Code) throws IOException {
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

		@Override
		public void updateExternalModel() {
			// No-op, wir zeigen nix an
		}
	}

	//$$ doc
	public static class Door extends NumberSingleton
	implements SimpleSensor, CanRead, CanWrite {
		@Override public String getDescription() { return "Affe-tot-Sensor"; }
		@Override public String getName() { return "DoorSens"; }
		public Code getHotCmdCode() { return Code.SENS_DOOR; }
	}

	//$$ doc
	public static class Trans extends NumberSingleton
	implements SimpleSensor, CanRead, CanWrite {
		@Override
		public String getDescription() {
			return "Lichtschranke im Transportfach";
		}

		@Override public String getName() { return "Trans"; }
		public Code getHotCmdCode() { return Code.SENS_TRANS; }
	}

	//$$ doc
	public static class Error extends NumberSingleton
	implements SimpleSensor, CanRead, CanWrite {
		@Override
		public String getDescription() {
			return "Sensor f\u00FCr Motor- oder Batteriefehler; 0 = Fehler; " +
					"1 = okay";
		}

		public Error() {
			// Hat 1 als Standardwert, nicht 0
			internalModel = Double.valueOf(1);
			updateExternalModel();
		}

		@Override public String getName() { return "Error"; }
		public Code getHotCmdCode() { return Code.SENS_ERROR; }
	}
}
