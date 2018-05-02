/*
 * c't-Sim - Robotersimulator für den c't-Bot
 *
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 */

package ctSim.model.bots.components;

import java.net.ProtocolException;
import java.util.Arrays;
import java.util.EnumSet;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.CommandOutputStream;
import ctSim.model.bots.components.Actuators.Led;
import ctSim.model.bots.components.Actuators.Log;
import ctSim.model.bots.components.Sensors.Mouse;
import ctSim.model.bots.components.Sensors.RemoteControl;

/**
 * <p>
 * Superklasse für alle Bot-Komponenten. Diese lassen sich in zwei Gruppen einteilen:
 * Sensoren (über sie bekommt der Bot-Steuercode Eingaben: Lichtsensor, Abstandssensoren, ...)
 * und Aktuatoren (dorthin kann der Steuercode Anweisungen geben: Motoren, LEDs, ...).
 * </p>
 * <p>
 * Manche Bot-Components können lesen von der TCP-(oder USB-)Verbindung mit dem Bot (Beispiel:
 * {@link Led}, Gegenbeispiel: {@link Mouse}, der immer innerhalb des Sim berechnet und nicht von
 * der Verbindung gelesen wird). Manche Components können sich auch aufs TCP (USB) schreiben
 * (Beispiel: {@link RemoteControl}, Gegenbeispiel: {@link Log}, das nie aus dem Sim herausgesendet
 * wird). Die Fähigkeiten "kann lesen" und "kann schreiben" sind unabhängig, jede Kombination ist
 * möglich.</p>
 * <p>
 * Die <em>grundsätzliche</em> Fähigkeit "Lesen können" oder "Schreiben können" wird durch die
 * Interfaces {@link CanRead} und {@link CanWrite} ausgedrückt. (Nur eine Component, die CanWrite
 * implementiert, kann überhaupt schreiben.) Ob die Fähigkeit <em>in einem Einzelfall</em> verwendet
 * wird, hängt zusätzlich ab von den beiden {@link ConnectionFlags} READS und WRITES:
 * Für einen Real-Bot setzt der Sim zum Beispiel (fast) alle Components auf "nur lesen", auch wenn
 * sie potentiell schreiben könnten. Beim Setzen der Flags wird geprüft, ob die Component das Flag
 * überhaupt unterstützt; andernfalls tritt eine UnsupportedOperationException auf.
 * </p>
 * TODO Aktuatoren vs. Sensoren
 *
 * @param <M>	Typ der Komponente
 * 
 * @author Felix Beckwermert
 * @author Hendrik Krauß (hkr@heise.de)
 */
public abstract class BotComponent<M> {
	/** Interface für lesende Komponenten */
	protected interface CanRead {
		/**
		 * Nicht aufrufen - stattdessen {@link BotComponent#offerRead(Command)} verwenden
		 *
		 * @param c	Kommando
		 * @throws ProtocolException
		 */
		void readFrom(Command c) throws ProtocolException;

		/**
		 * Nicht aufrufen - sollte nur von
		 * {@link BotComponent#askForWrite(CommandOutputStream) askForWrite()}
		 * und {@link BotComponent#offerRead(Command) offerRead()} verwendet werden.
		 *
		 * @return Command-Code
		 */
		Code getHotCmdCode();
	}

	/**
	 * Schreibbare (genauer: von der UI aus änderbare) Dinge sollten sich auf ihrem externen Model
	 * als Listener anmelden.
	 */
	protected interface CanWrite {
		/**
		 * Nicht aufrufen - stattdessen {@link BotComponent#askForWrite(CommandOutputStream)}
		 * verwenden.
		 *
		 * @param c	Kommando
		 */
		void writeTo(Command c);

		/**
		 * Nicht aufrufen - sollte nur von
		 * {@link BotComponent#askForWrite(CommandOutputStream) askForWrite()}
		 * und {@link BotComponent#offerRead(Command) offerRead()} verwendet werden.
		 *
		 * @return Command-Code
		 */
		Code getHotCmdCode();
	}

	/** Interface für Komponenten, die asynchron schreiben */
	protected interface CanWriteAsynchronously {
		/**
		 * Setzt Asynchronen-Schreib-Stream
		 *
		 * @param s	OutputStream
		 */
		void setAsyncWriteStream(CommandOutputStream s);
	}

	/** Einfacher Sensor */
	public interface SimpleSensor {
		// Marker-Interface
	}

	/** Einfacher Aktuator */
	public interface SimpleActuator {
		// Marker-Interface
	}

	/** Connection-Eigenschaften */
	public static enum ConnectionFlags {
		/** liest */
		READS,
		/** schreibt */
		WRITES,
		/** schreibt asynchron */
		WRITES_ASYNCLY }

	///////////////////////////////////////////////////////////////////////////

	/** externes Modell */
	private final M externalModel;

	/** Anfänglich alles false */
	private EnumSet<ConnectionFlags> flags =
			EnumSet.noneOf(ConnectionFlags.class);

	/**
	 * @param externalModel	externes Modell vom Typ M
	 */
	public BotComponent(M externalModel) { this.externalModel = externalModel; }

	/**
	 * @return Liefert das externe Modell
	 */
	public M getExternalModel() { return externalModel; }

	/**
	 * @param s	nicht unterstützte Operation
	 * @return Exception
	 */
	private UnsupportedOperationException createUnsuppOp(String s) {
		return new UnsupportedOperationException("Bot-Komponente "+this+
				" unterstützt kein "+s);
	}

	/**
	 * Setzt die Connection-Flags
	 *
	 * @param flags	Flags
	 */
	public void setFlags(ConnectionFlags... flags) {
		EnumSet<ConnectionFlags> f = (flags == null || flags.length == 0)
				? EnumSet.noneOf(ConnectionFlags.class)
						: EnumSet.copyOf(Arrays.asList(flags));

				// Prüfen, ob dieses Objekt in der Lage ist zu dem, was die von uns wollen
				if (f.contains(ConnectionFlags.READS)) {
					if (! (this instanceof CanRead))
						throw createUnsuppOp("Lesen");
				}
				if (f.contains(ConnectionFlags.WRITES)) {
					if (! (this instanceof CanWrite))
						throw createUnsuppOp("Schreiben");
				}
				if (f.contains(ConnectionFlags.WRITES_ASYNCLY)) {
					if (! (this instanceof CanWriteAsynchronously))
						throw createUnsuppOp("asynchrones Schreiben");
				}

				this.flags = f;
	}

	/**
	 * @return schreibt synchron?
	 */
	public boolean writesSynchronously() {
		return flags.contains(ConnectionFlags.WRITES);
	}

	/**
	 * @return liest Kommandos?
	 */
	public boolean readsCommands() {
		return flags.contains(ConnectionFlags.READS);
	}

	/**
	 * @return schreibt asynchron?
	 */
	public boolean writesAsynchronously() {
		return flags.contains(ConnectionFlags.WRITES_ASYNCLY);
	}

	/**
	 * @return kann schreiben?
	 */
	public boolean isGuiEditable() {
		return writesSynchronously() || writesAsynchronously();
	}

	/**
	 * Liest Daten vom Kommando
	 *
	 * @param c	Kommando
	 * @throws ProtocolException
	 */
	public void offerRead(final Command c) throws ProtocolException {
		if (! readsCommands())
			return;
		// Cast kann nicht schiefgehen wegen setFlags()
		CanRead self = (CanRead)this;
		if (c.has(self.getHotCmdCode())) {
			self.readFrom(c);
			c.setHasBeenProcessed(true);
		}
	}

	/** Nur auf dem EDT laufen lassen */
	public abstract void updateExternalModel();

	/**
	 * Schreibanforderung
	 *
	 * @param s	CommandOutputStream
	 */
	public void askForWrite(final CommandOutputStream s) {
		if (! writesSynchronously())
			return;
		// Cast kann nicht schiefgehen wegen setFlags()
		CanWrite self = (CanWrite)this;
		self.writeTo(s.getCommand(self.getHotCmdCode()));
	}

	/**
	 * async Write-Stream setzen
	 *
	 * @param s	CommandOutputStream
	 */
	public void offerAsyncWriteStream(CommandOutputStream s) {
		if (! writesAsynchronously())
			return;
		// Cast kann nicht schiefgehen wegen setFlags()
		((CanWriteAsynchronously)this).setAsyncWriteStream(s);
	}

	/**
	 * @return Gibt den Namen der Komponente zurück
	 */
	public abstract String getName();

	/**
	 * @return Beschreibung der Komponente
	 */
	public abstract String getDescription();
}
