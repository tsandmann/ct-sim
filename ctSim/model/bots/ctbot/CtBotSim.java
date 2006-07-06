package ctSim.model.bots.ctbot;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.World;

public abstract class CtBotSim extends CtBot {
	
	public CtBotSim(World world, String name, Point3d pos, Vector3d head) {
		
		super(name, pos, head);
	}
}