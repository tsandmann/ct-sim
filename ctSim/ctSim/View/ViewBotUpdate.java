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
	 * @param id
	 * @param rotation
	 * @param translation
	 */
	public ViewBotUpdate(String id, TransformGroup tg) {
		super();
		// TODO Auto-generated constructor stub
		this.id = id;
		
		Transform3D trans = new Transform3D();
		tg.getTransform(trans);
		
		transformMatrix = new Matrix4d();
		trans.get(transformMatrix);
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
