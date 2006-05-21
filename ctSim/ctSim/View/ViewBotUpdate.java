package ctSim.View;

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
	 * Konstruktor
	 * @param id ID des Bots
	 * @param tg Transformgruppe des Bots
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
	
	/**
	 * Liefer ID zuruek
	 * @return ID
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Setze ID
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Die Matrix enthalet Translation und Rotation
	 * @return Die Matrix
	 */
	public Matrix4d getTransformMatrix() {
		return transformMatrix;
	}
	
	/**
	 * Die Matrix enthalet Translation und Rotation
	 * @param transformMatrix
	 */
	public void setTransformMatrix(Matrix4d transformMatrix) {
		this.transformMatrix = transformMatrix;
	}
}
