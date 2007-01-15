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

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.Actuator.Log;
import ctSim.model.bots.components.actuators.Led;
import ctSim.model.bots.ctbot.components.RemoteControlSensor;
import ctSim.util.Flags;

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
 * dem Bot (Beispiel: {@link Led}, Gegenbeispiel: {@link MouseSensor}, der
 * immer innerhalb des Sim berechnet und nicht von der Verbindung gelesen wird).
 * Manche Components k&ouml;nnen sich auch aufs TCP (USB) schreiben (Beispiel:
 * {@link RemoteControlSensor}, Gegenbeispiel: {@link Log}, das nie aus dem
 * Sim herausgesendet wird). Die F&auml;higkeiten &quot;kann lesen&quot; und
 * &quot;kann schreiben&quot; sind unabh&auml;ngig, jede Kombination ist
 * m&ouml;glich.&lt;/p&gt;
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
 * </p>
 * $$ Aktuatoren vs. Sensoren
 *
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public abstract class BotComponent<M> {
	public interface CanWrite {
		/**
		 * Nicht aufrufen. Methode ist public als Nebenwirkung, weil wir gern
		 * protected h&auml;tten (es soll &uuml;ber Packagegrenzen hinweg
		 * funktionieren), aber Methoden in Packages nur public oder default
		 * sein k&ouml;nnen.
		 */
		public void writeTo(Command c);

		/**
		 * Nicht aufrufen. Methode ist public als Nebenwirkung, weil wir gern
		 * protected h&auml;tten (es soll &uuml;ber Packagegrenzen hinweg
		 * funktionieren), aber Methoden in Packages nur public oder default
		 * sein k&ouml;nnen.
		 */
		public Code getHotCmdCode();
	}

	public interface CanRead {
		/**
		 * Nicht aufrufen. Methode ist public als Nebenwirkung, weil wir gern
		 * protected h&auml;tten (es soll &uuml;ber Packagegrenzen hinweg
		 * funktionieren), aber Methoden in Packages nur public oder default
		 * sein k&ouml;nnen.
		 */
		public void readFrom(Command c) throws ProtocolException;

		/**
		 * Nicht aufrufen. Methode ist public als Nebenwirkung, weil wir gern
		 * protected h&auml;tten (es soll &uuml;ber Packagegrenzen hinweg
		 * funktionieren), aber Methoden in Packages nur public oder default
		 * sein k&ouml;nnen.
		 */
		public Code getHotCmdCode();
	}

	public interface SimpleSensor { //$$$ abstract class
		// Marker-Interface
	}

	public interface SimpleActuator { //$$$ abstract class
		// Marker-Interface
	}

	public static enum ConnectionFlags { READS, WRITES }

	private final M model;

	/** Anf&auml;nglich alles false */
	private Flags<ConnectionFlags> flags = new Flags<ConnectionFlags>();

	public BotComponent(M model) { this.model = model; }

	public M getModel() { return model; } //$$$ ? verunumstaendlichen

	private UnsupportedOperationException createUnsuppOp(String s) {
		return new UnsupportedOperationException("Bot-Komponente "+this+
			" unterst\u00FCtzt kein "+s);
	}

	public void setFlags(ConnectionFlags... flags) {
		// Wenn flags == null wird alles default, also false
		Flags<ConnectionFlags> f = new Flags<ConnectionFlags>(flags);
		// Pruefen, ob dieses Objekt in der Lage ist zu dem, was die von uns
		// wollen
		if (f.get(ConnectionFlags.READS)) {
			if (! (this instanceof CanRead))
				throw createUnsuppOp("Lesen");
		}
		if (f.get(ConnectionFlags.WRITES)) {
			if (! (this instanceof CanWrite))
				throw createUnsuppOp("Schreiben");
		}
		this.flags = f;
	}

	public boolean writesCommands() {
		return flags.get(ConnectionFlags.WRITES);
	}

	public boolean readsCommands() {
		return flags.get(ConnectionFlags.READS);
	}

	public boolean isGuiEditable() {
		return writesCommands();
	}

	public void offerRead(Command c) throws ProtocolException {
		if (readsCommands()) {
			// Cast kann nicht in die Hose gehen wegen setFlags()
			CanRead self = (CanRead)this;
			if (c.has(self.getHotCmdCode()))
				self.readFrom(c);
		}
	}

	public final void askForWrite(CommandOutputStream s) {
		if (writesCommands()) {
			// Cast kann nicht in die Hose gehen wegen setFlags()
			CanWrite self = (CanWrite)this;
			self.writeTo(s.getCommand(self.getHotCmdCode()));
		}
	}

	/** @return Gibt den Namen der Komponente zur&uuml;ck */
	public String getName() { return name; } //$$$ abstract machen

	/** @return Beschreibung der Komponente */
	public abstract String getDescription();

	////////////////////////////////////////////////////////////////////////

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
		model = null;
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
