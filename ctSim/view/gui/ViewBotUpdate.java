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
package ctSim.view.gui;

import java.io.Serializable;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Matrix4d;
//import javax.vecmath.Matrix3d;


/**
 * Hilfsklasse, die zu jedem Bot nur noch die Updates enthaelt
 * @author bbe (bbe@heise.de)
 */
public class ViewBotUpdate implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	private Matrix4d transformMatrix;
//	private Transform3D trans = new Transform3D();
	/**
	 * @param id
	 * @param rotation
	 * @param translation
	 */
	public ViewBotUpdate(String id, TransformGroup tg) {
		super();
		this.id = id;
		
		Transform3D trans = new Transform3D();
		tg.getTransform(trans);
		
		transformMatrix = new Matrix4d();
		trans.get(transformMatrix);
	}
	
	/**
	 * Nur den Namen des Bots setzen
	 * @param id
	 */
	public ViewBotUpdate(String id) {
		super();
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Matrix4d getTransformMatrix() {
		return transformMatrix;
	}
	public void setTransformMatrix(Matrix4d transformMatrix) {
		this.transformMatrix = transformMatrix;
	}
}
