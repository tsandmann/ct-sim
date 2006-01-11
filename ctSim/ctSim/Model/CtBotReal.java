package ctSim.Model;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * Die abstrakte Oberklasse f�r alle Bots, die au�erhalb der 
 * Grenzen des Simulators existieren. 
 * 
 */

abstract public class CtBotReal extends CtBot {

	public CtBotReal(Point3f pos, Vector3f head) {
		super(pos, head);
		// TODO Auto-generated constructor stub
	}

/*	public BotReal(Point3f pos) {
		super(pos);
		// TODO Auto-generated constructor stub
	}
*/	
}
