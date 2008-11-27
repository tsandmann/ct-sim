package ctSim.model.bots.components;

import java.io.IOException;
import java.util.Map;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.model.bots.components.BotComponent.SimpleSensor;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.gui.RemoteControlViewer;

/**
 * Sensoren
 */
public class Sensors {
	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.model.bots.components.Sensors");

	/**
	 * Klasse der Liniensensoren
	 */
	public static class Line extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override
		protected String getBaseDescription() {
			return "Liniensensor [0; 1023]";
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return "Line"; }
		
		/**
		 * Liniensensoren
		 * @param isLeft links?
		 */
		public Line(boolean isLeft) { super(isLeft); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_LINE; }
	}

	/**
	 * Klasse der Lichtsensoren
	 */
	public static class Light extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override
		protected String getBaseDescription() {
			return "Lichtsensor";
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return "Light"; }
		
		/**
		 * Lichtsensoren
		 * @param isLeft links?
		 */
		public Light(boolean isLeft) { super(isLeft); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_LDR; }
	}

	/**
	 * Klasse der Rad-Encoder
	 */
	public static class Encoder extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override
		protected String getBaseDescription() {
			return "Rad-Encoder-Sensor";
		}

		/**
		 * Rad-Encoder
		 * @param isLeft links?
		 */
		public Encoder(boolean isLeft) { super(isLeft); }
		
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return "Enc"; }

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_ENC; }
	}

	/** Abstandssensor vom Typ GP2D12
	 */
	public static class Distance extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override
		protected String getBaseDescription() {
			return "Abstandssensor";
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return "Ir"; }
		
		/**
		 * Distanzsensoren GPD12
		 * @param isLeft links?
		 */
		public Distance(boolean isLeft) { super(isLeft); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_IR; }
	}

	/**
	 * Klasse der Abgrundsensoren
	 */
	public static class Border extends NumberTwin
	implements SimpleSensor, CanRead, CanWrite {
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override
		protected String getBaseDescription() {
			return "Abgrundsensor [0; 1023]";
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return "Border"; }
		
		/**
		 * Abgrundsensoren
		 * @param isLeft links?
		 */
		public Border(boolean isLeft) { super(isLeft); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_BORDER; }
	}

	/**
	 * Systemzeit des Bots
	 */
	public static class Clock extends BotComponent<Void>
	implements CanRead, CanWrite {
		/** letzte uebertragene Zeit */
		private int lastTransmittedSimTime = -1;

		/**
		 * Systemzeit setzen
		 * @param simTime Zeit in ms
		 */
		public synchronized void setSimTimeInMs(int simTime) {
			lastTransmittedSimTime = simTime;
			// Wir haben nur 16 Bit zur Verfuegung und 10.000 ist ne nette Zahl ;-)
			lastTransmittedSimTime %= 10000;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#readFrom(ctSim.model.Command)
		 */
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

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanWrite#writeTo(ctSim.model.Command)
		 */
		public synchronized void writeTo(Command c) {
			c.setDataL(lastTransmittedSimTime);
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public void updateExternalModel() {
			// No-op, weil wir werden ja eh nicht angezeigt und operieren
			// direkt auf dem ExternalModel
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Simulationszeit-Uhr (Millisekunden)";
		}

		/**
		 * Systemzeit
		 */
		public Clock() { super(null); }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "Uhr"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Command.Code.DONE; }
	}

	/**
	 * Maussensor
	 */
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

		/**
		 * Maussensor
		 * @param isX X? (sonst Y)
		 */
		public Mouse(boolean isX) {
			super(isX);
			this.isX = isX;
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getName()
		 */
		@Override
		public String getName() {
			// Unicode 0394: grosses Delta
			return "Mouse \u0394" + (isX ? "X" : "Y");
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Maus-Sensor: " +
				(isX
				? "Drehgeschwindigkeit (X-Komponente)"
				: "Geradeaus-Geschwindigkeit (Y-Komponente)");
		}

		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseDescription()
		 */
		@Override protected String getBaseDescription() { return ""; }
		
		/**
		 * @see ctSim.model.bots.components.NumberTwin#getBaseName()
		 */
		@Override protected String getBaseName() { return ""; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
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
	 * &uuml;brigen Felder werden nicht ver&auml;ndert. Falls der Benutzer einen
	 * Knopf gedrueckt hat, sendet die Instanz den entsprechenden Code. Falls
	 * nicht, sendet sie ein Command, das {@code dataL} == 0 hat.
	 * </p>
	 */
	public static class RemoteControl extends BotComponent<Void>
	implements CanWrite, CanWriteAsynchronously {
		/** async. Outputstream */
		private CommandOutputStream asyncOut;
		/** ausstehender RC5-Code */
		private int syncPendingRcCode = 0;
		
		/** Lookup-Tabelle fuer RC5-Codes */
		private Map<String, Integer> RC5Keys = null;

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanWriteAsynchronously#setAsyncWriteStream(ctSim.model.CommandOutputStream)
		 */
		public void setAsyncWriteStream(CommandOutputStream s) {
			asyncOut = s;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanWrite#writeTo(ctSim.model.Command)
		 */
		public synchronized void writeTo(Command c) {
			c.setDataL(syncPendingRcCode);
			syncPendingRcCode = 0;
		}

		/**
		 * Sendet einen RC5-Code
		 * @param rc5Code	RC5-Code gemaess Tabelle
		 * @throws IOException
		 */
		private synchronized void send(int rc5Code) throws IOException {
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
		
		/**
		 * Sendet einen Tastencode
		 * @param key Taste, dessen Code gesendet werden soll
		 * @throws IOException 
		 */
		public void send(String key) throws IOException {
			Integer rc5 = RC5Keys.get(key);
			if (rc5 != null) {
				lg.fine("sende RC5-Code 0x%x", rc5);
				send(rc5);
			}
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { 
			return "RC5"; 
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override public String getDescription() { 
			return "Fernbedienung"; 
		}

		/**
		 * Fernbedienung 
		 * @param type Typ der Fernbedienung (z.B. "RC_HAVE_HQ_RC_UNIVERS29_334")
		 */
		public RemoteControl(String type) {
			super(null);
			RemoteControlCodes RC5Type;
			try {
				RC5Type = new RemoteControlCodes(type);
				RC5Keys = RC5Type.getKeyMap();
			} catch (Exception e) {
				lg.warn("Konnte Fernbedienung nicht erzeugen: " + e.getMessage());
				RC5Keys = Misc.newMap();	// unbekannter Typ -> leere Zuordnung verwenden
			}
		}
		
		/**
		 * Fernbedienung 
		 */
		public RemoteControl() { 
			this("");
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent.CanWrite#getHotCmdCode()
		 */
		public Code getHotCmdCode() { 
			return Code.SENS_RC5; 
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public void updateExternalModel() {
			// No-op, wir zeigen nix an
		}
	}

	/**
	 * Transportfach-Klappensensor
	 */
	public static class Door extends NumberSingleton
	implements SimpleSensor, CanRead, CanWrite {
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override public String getDescription() { return "Transportfach-Klappensensor"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "DoorSens"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_DOOR; }
	}

	/**
	 * Transportfach-Ueberwachung
	 */
	public static class Trans extends NumberSingleton
	implements SimpleSensor, CanRead, CanWrite {
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Lichtschranke im Transportfach";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "Trans"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_TRANS; }
	}

	/**
	 * Error-Sensor
	 */
	public static class Error extends NumberSingleton
	implements SimpleSensor, CanRead, CanWrite {
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Sensor f\u00FCr Motor- oder Batteriefehler; 0 = Fehler; " +
					"1 = okay";
		}

		/**
		 * Error-Sensor
		 */
		public Error() {
			// Hat 1 als Standardwert, nicht 0
			internalModel = Double.valueOf(1);
			updateExternalModel();
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override public String getName() { return "Error"; }
		
		/**
		 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
		 */
		public Code getHotCmdCode() { return Code.SENS_ERROR; }
	}
}
