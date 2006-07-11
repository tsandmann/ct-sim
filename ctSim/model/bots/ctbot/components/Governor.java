package ctSim.model.bots.ctbot.components;

import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.actuators.SimpleActuator;

// TODO: anpassen... nicht abstract
// TODO: sollte double sein?
public class Governor extends SimpleActuator<Integer> {
	
	private Integer val;
	
	public Governor(String name, Point3d relPos, Vector3d relHead) {
		
		super(name, relPos, relHead);
		
		this.val = 0;
	}
	
	// TODO: Ueberfluessig?
	@Override
	public String getType() {
		
		return "Regulator";
	}
	
	// TODO: should be abstract (?)
	@Override
	public String getDescription() {
		// TODO:
		return null;
	}
	
	// TODO: should be abstract (?)
	@Override
	public void setValue(Integer value) {
		
		this.val = value;
	}
	
	// TODO: should be abstract (?)
	@Override
	public Integer getValue() {
		
		return this.val;
	}

	@Override
	public Shape3D getShape() {
		// TODO Auto-generated method stub
		return new Shape3D();
	}
}