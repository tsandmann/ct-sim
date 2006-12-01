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

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;


/**
 * Abstrakte Oberklasse fuer alle Bot-Komponenten
 * @author Felix Beckwermert
 *
 */
public abstract class BotComponent {
	
	private static int ID_COUNT = 0;
	
	private int id;
	
	private String name;
	
	private Point3d relPos;
	private Vector3d relHead;
	
	// TODO: Darstellung usw. (?)
	
	/**
	 * Der Konstruktor
	 * @param n Name der Komponente
	 * @param rP relative Position
	 * @param rH relative Blickrichtung
	 */
	public BotComponent(String n, Point3d rP, Vector3d rH) {
		
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
	 * @return Gibt den Namen der Komponente zurueck
	 */
	public String getName() {

		return this.name;
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
	
	/**
	 * @return Der Komponenten-Typ
	 */
	public abstract String getType();
	
	/**
	 * @return Beschreibung der Komponente
	 */
	public abstract String getDescription();
}
