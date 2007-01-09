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

import java.io.IOException;
import java.net.ProtocolException;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.util.Flags;


/**
 * Abstrakte Oberklasse fuer alle Bot-Komponenten
 * @author Felix Beckwermert
 *
 */
public abstract class BotComponent<T> {
	public static enum IOFlagsEnum { CON_READ, CON_WRITE }

	private final T model;
	
	/** Anf&auml;nglich alles false */
	private Flags<IOFlagsEnum> flags = new Flags<IOFlagsEnum>();

	public BotComponent(T model) { this.model = model; }

	public T getModel() { return model; }

	public void setFlags(IOFlagsEnum... flags) {
		// Wenn flags == null wird alles default, also false
		this.flags = new Flags<IOFlagsEnum>(flags);
	}

	public boolean writesToTcp() {
		return flags.get(IOFlagsEnum.CON_WRITE);
	}

	public boolean readsFromTcp() {
		return flags.get(IOFlagsEnum.CON_READ);
	}

	public boolean isGuiEditable() {
		return writesToTcp();
	}

	public final void offerRead(Command c) throws ProtocolException {
		if (readsFromTcp())
			readFrom(c);
	}

	// Subklassen brauchen Parameter + Exception i.d.R., daher SuppressWarn
	@SuppressWarnings("unused")
	protected void readFrom(Command c) throws ProtocolException {
		// $$$ abstract
	}

	public final void askForWrite(CommandOutputStream s) throws IOException {
		if (writesToTcp())
			writeTo(s);
	}

	// Subklassen brauchen Parameter + Exception i.d.R., daher SuppressWarn
	@SuppressWarnings("unused")
	protected void writeTo(CommandOutputStream s) throws IOException {
		// $$$ abstract
	}

	/** @return Gibt den Namen der Komponente zur&uuml;ck */
	public String getName() { return name; } //$$$ abstract

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
