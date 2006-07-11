package ctSim.model.bots.components;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;


public abstract class BotComponent {
	
	private static int ID_COUNT = 0;
	
	private int id;
	
	private String name;
	
	private Point3d relPos;
	private Vector3d relHead;
	
	// TODO: Darstellung usw. (?)
	
	public BotComponent(String name, Point3d relPos, Vector3d relHead) {
		
		this.name    = name;
		this.relPos  = relPos;
		this.relHead = relHead;
		
		this.id = ID_COUNT++;
	}
	
	public int getId() {
		
		return this.id;
	}
	
	public String getName() {

		return name;
	}
	
	public Point3d getRelPosition() {
		
		return this.relPos;
	}
	
	public Vector3d getRelHeading() {
		
		return this.relHead;
	}
	
	public Point3d getAbsPosition(Point3d absPos, Vector3d absHead) {
		
//		Transform3D transform = new Transform3D();
//		
//		transform.setTranslation(new Vector3d(absPos));
//		
//		double angle = absHead.angle(new Vector3d(1d, 0d, 0d));
//		if(absHead.y < 0)
//			angle = -angle;
//		
//		transform.setRotation(new AxisAngle4d(0d, 0d, 1d, angle));
		
		Transform3D transform = SimUtils.getTransform(absPos, absHead);
		
		Point3d pos = new Point3d(this.relPos);
		transform.transform(pos);
		
		return pos;
	}
	
	public Vector3d getAbsHeading(Point3d absPos, Vector3d absHead) {
		
//		Transform3D transform = new Transform3D();
//		
//		transform.setTranslation(new Vector3d(absPos));
//		
//		double angle = absHead.angle(new Vector3d(1d, 0d, 0d));
//		if(absHead.y < 0)
//			angle = -angle;
//		
//		transform.setRotation(new AxisAngle4d(0d, 0d, 1d, angle));
		
		Transform3D transform = SimUtils.getTransform(absPos, absHead);
		
		Vector3d vec = new Vector3d(this.relHead);
		transform.transform(vec);
		
		return vec;
	}
	
	public Transform3D getRelTransform() {
		
//		Transform3D transform = new Transform3D();
//		
//		transform.setTranslation(new Vector3d(this.getRelPosition()));
//		
//		double angle = this.getRelHeading().angle(new Vector3d(1d, 0d, 0d));
//		if(this.getRelHeading().y < 0)
//			angle = -angle;
//		
//		transform.setRotation(new AxisAngle4d(0d, 0d, 1d, angle));
//		
//		return transform;
		
		return SimUtils.getTransform(this.getRelPosition(), this.getRelHeading());
	}
	
	public abstract String getType();
	
	public abstract String getDescription();
	
	// TODO: weg, dafuer getBranchGroup?
	public abstract Shape3D getShape();
}
