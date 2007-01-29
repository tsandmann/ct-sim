/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
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

import javax.media.j3d.Transform3D;
import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;

//$$$ externes Model: in Subklassen, internes: hier
//$$ doc
/**
 * <p>
 * Superklasse f&uuml;r alle Bot-Komponenten. Diese lassen sich in zwei Gruppen
 * einteilen: Sensoren (&uuml;ber sie bekommt der Bot-Steuercode Eingaben:
 * Lichtsensor, Abstandssensoren, ...) und Aktuatoren (dorthin kann der
 * Steuercode Anweisungen geben: Motoren, LEDs, ...).
 * </p>
 * <p>
 * Manche Bot-Components k&ouml;nnen lesen von der TCP-(oder USB-)Verbindung mit
 * dem Bot (Beispiel: {@link Actuators.Led}, Gegenbeispiel:
 * {@link Sensors.Mouse}, der immer innerhalb des Sim berechnet und nicht von
 * der Verbindung gelesen wird). Manche Components k&ouml;nnen sich auch aufs
 * TCP (USB) schreiben (Beispiel: {@link Sensors.RemoteControl}, Gegenbeispiel:
 * {@link Actuators.Log}, das nie aus dem Sim herausgesendet wird). Die
 * F&auml;higkeiten &quot;kann lesen&quot; und &quot;kann schreiben&quot; sind
 * unabh&auml;ngig, jede Kombination ist m&ouml;glich.</p>
 * <p>
 * Die <em>grunds&auml;tzliche</em> F&auml;higkeit &quot;Lesen
 * k&ouml;nnen&quot; oder &quot;Schreiben k&ouml;nnen&quot; wird durch die
 * Interfaces {@link CanRead} und {@link CanWrite} ausgedr&uuml;ckt. (Nur eine
 * Component, die CanWrite implementiert, kann &uuml;berhaupt schreiben.) Ob die
 * F&auml;higkeit <em>in einem Einzelfall</em> verwendet wird, h&auml;ngt
 * zus&auml;tzlich ab von den beiden {@link ConnectionFlags} READS und WRITES:
 * F&uuml;r einen Real-Bot setzt der Sim zum Beispiel (fast) alle Components auf
 * &quot;nur lesen&quot;, auch wenn sie potentiell schreiben k&ouml;nnten. Beim
 * Setzen der Flags wird gepr&uuml;ft, ob die Component das Flag &uuml;berhaupt
 * unterst&uuml;tzt; andernfalls tritt eine UnsupportedOperationException auf.
 * </p> $$ Aktuatoren vs. Sensoren
 *
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public abstract class BotComponent<M> {
	protected interface CanRead {
		/**
		 * Nicht aufrufen &ndash; stattdessen
		 * {@link BotComponent#offerRead(Command)} verwenden.
		 */
		void readFrom(Command c) throws ProtocolException;

		/**
		 * Nicht aufrufen &ndash; sollte nur von
		 * {@link BotComponent#askForWrite(CommandOutputStream) askForWrite()}
		 * und {@link BotComponent#offerRead(Command) offerRead()} verwendet
		 * werden.
		 */
		Code getHotCmdCode();

		/** Nur auf dem EDT laufenlassen */
		void updateExternalModel();
	}

	/**
	 * Schreibbare (genauer: von der UI aus &auml;nderbare) Dinger sollten sich
	 * auf ihrem externen Model als Listener anmelden
	 */
	protected interface CanWrite {
		/**
		 * Nicht aufrufen &ndash; stattdessen
		 * {@link BotComponent#askForWrite(CommandOutputStream)} verwenden.
		 */
		void writeTo(Command c);

		/**
		 * Nicht aufrufen &ndash; sollte nur von
		 * {@link BotComponent#askForWrite(CommandOutputStream) askForWrite()}
		 * und {@link BotComponent#offerRead(Command) offerRead()} verwendet
		 * werden.
		 */
		Code getHotCmdCode();
	}

	protected interface CanWriteAsynchronously {
		void setAsyncWriteStream(CommandOutputStream s);
	}

	public interface SimpleSensor { //$$$ abstract class
		// Marker-Interface
	}

	public interface SimpleActuator { //$$$ abstract class
		// Marker-Interface
	}

	public static enum ConnectionFlags { READS, WRITES, WRITES_ASYNCLY }

	///////////////////////////////////////////////////////////////////////////

	private final M externalModel;

	/** Anf&auml;nglich alles false */
	private EnumSet<ConnectionFlags> flags =
		EnumSet.noneOf(ConnectionFlags.class);

	public BotComponent(M externalModel) { this.externalModel = externalModel; }

	public M getExternalModel() { return externalModel; }

	private UnsupportedOperationException createUnsuppOp(String s) {
		return new UnsupportedOperationException("Bot-Komponente "+this+
			" unterst\u00FCtzt kein "+s);
	}

	public void setFlags(ConnectionFlags... flags) {
		EnumSet<ConnectionFlags> f = (flags == null || flags.length == 0)
			? EnumSet.noneOf(ConnectionFlags.class)
			: EnumSet.copyOf(Arrays.asList(flags));

		// Pruefen, ob dieses Objekt in der Lage ist zu dem, was die von uns
		// wollen
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

	public boolean writesSynchronously() {
		return flags.contains(ConnectionFlags.WRITES);
	}

	public boolean readsCommands() {
		return flags.contains(ConnectionFlags.READS);
	}

	public boolean writesAsynchronously() {
		return flags.contains(ConnectionFlags.WRITES_ASYNCLY);
	}

	public boolean isGuiEditable() {
		return writesSynchronously();
	}

	public void offerRead(final Command c) throws ProtocolException {
		if (! readsCommands())
			return;
		// Cast kann nicht in die Hose gehen wegen setFlags()
		CanRead self = (CanRead)this;
		if (c.has(self.getHotCmdCode())) {
			self.readFrom(c);
			c.setHasBeenProcessed(true);
		}
	}

	public void askToUpdateExternalModel() {
		if (! readsCommands())
			return;
		assert SwingUtilities.isEventDispatchThread();
		// Cast kann nicht in die Hose gehen wegen setFlags()
		((CanRead)this).updateExternalModel();
	}

	public void askForWrite(final CommandOutputStream s) {
		if (! writesSynchronously())
			return;
		// Cast kann nicht in die Hose gehen wegen setFlags()
		CanWrite self = (CanWrite)this;
		self.writeTo(s.getCommand(self.getHotCmdCode()));
	}

	public void offerAsyncWriteStream(CommandOutputStream s) {
		if (! writesAsynchronously())
			return;
		// Cast kann nicht in die Hose gehen wegen setFlags()
		((CanWriteAsynchronously)this).setAsyncWriteStream(s);
	}

	/** @return Gibt den Namen der Komponente zur&uuml;ck */
	public String getName() { return name; } //$$$ abstract machen

	/** @return Beschreibung der Komponente */
	public abstract String getDescription();

	////////////////////////////////////////////////////////////////////////
	//$$ Nur noch fuer BotPosition wichtig, alles unter der Linie

	private static int ID_COUNT = 0;

	private int id;
	private String name;
	private Point3d relPos;
	private Vector3d relHead;

	/**
	 * Der Konstruktor
	 * @param n Name der Komponente
	 * @param rP relative Position
	 * @param rH relative Blickrichtung
	 */
	public BotComponent(String n, Point3d rP, Vector3d rH) {
		externalModel = null;
		this.name    = n;
		this.relPos  = rP;
		this.relHead = rH;

		this.id = ID_COUNT++;
	}

	/**
	 * @return Gibt die ID der Komponente zurueck
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @return Gibt die relative Position der Komponente zurueck
	 */
	public Point3d getRelPosition() {
		return this.relPos;
	}

	/**
	 * @return Gibt die relative Blickrichtung der Komponente zurueck
	 */
	public Vector3d getRelHeading() {
		return this.relHead;
	}

	/**
	 * Gibt die absolute Position der Komponente zurueck
	 * @param absPos Die absolute Position des Bots
	 * @param absHead Die absolute Blickrichtung des Bots
	 * @return Die absolute Position der Komponente
	 */
	public Point3d getAbsPosition(Point3d absPos, Vector3d absHead) {
		Transform3D transform = SimUtils.getTransform(absPos, absHead);

		Point3d pos = new Point3d(this.relPos);
		transform.transform(pos);

		return pos;
	}

	/**
	 * Gibt die absolute Blickrichtung der Komponente zurueck
	 * @param absPos Die absolute Position des Bots
	 * @param absHead Die absolute Blickrichtung des Bots
	 * @return Die absolute Blickrichtung der Komponente
	 */
	public Vector3d getAbsHeading(Point3d absPos, Vector3d absHead) {
		Transform3D transform = SimUtils.getTransform(absPos, absHead);

		Vector3d vec = new Vector3d(this.relHead);
		transform.transform(vec);

		return vec;
	}

	/**
	 * @return Die relative 3D-Transformation
	 */
	public Transform3D getRelTransform() {
		return SimUtils.getTransform(this.getRelPosition(), this.getRelHeading());
	}
}
