package ctSim.model.bots.components;

import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.view.ComponentGroupGUI;
import ctSim.view.PositionGUI;


public abstract class BotPosition extends BotComponent {

	public BotPosition(String name, Point3d pos, Vector3d head) {
		
		super(name, pos, head);
	}

	@Override
	public String getType() {
		
		return "Bot-Position";
	}

	@Override
	public String getDescription() {
		
		return "Position of the bot in the world";
	}
	
	public ComponentGroupGUI getGUI() {
		
		return new PositionGUI(this);
	}
	
	@Override
	public Shape3D getShape() {
		
		// TODO: hier Bot-Shape zurückgeben?
		return new Shape3D();
	}
	
	public abstract Point3d getRelPosition();
	
	public abstract Vector3d getRelHeading();
	
	public abstract Point3d getAbsPosition();
	
	public abstract Vector3d getAbsHeading();
	
	public abstract void setPos(Point3d pos);
	
	public abstract void setHead(Vector3d head);
}
