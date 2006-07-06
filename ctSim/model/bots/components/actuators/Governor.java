package ctSim.model.bots.components.actuators;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.bots.components.Actuator;

// TODO: anpassen... nicht abstract
public abstract class Governor extends SimpleActuator<Double> {
	
	private Double val;
	
	Governor(String name, Point3d relPos, Vector3d relHead) {
		super(name, relPos, relHead);
		// TODO Auto-generated constructor stub
	}
	
	// TODO: Überflüssig?
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
	protected void setValue(Double value) {
		
		this.val = value;
	}
	
	// TODO: should be abstract (?)
	@Override
	public Double getValue() {
		
		return this.val;
	}
	
	//////////////////////////////////////////////////////////////////////
	// TestfÃ¤lle:
	
	// Testfall 1:
//	public static Actuator getTestActuator1(String name) {
//		
//		Actuator act1 = new Governor("Test:Actuator:Governor - "+name, "blubb", 0.342d);
//		
//		return act1;
//	}
}
