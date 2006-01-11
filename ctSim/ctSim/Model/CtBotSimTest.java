package ctSim.Model;

import java.util.Random;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

public class CtBotSimTest extends CtBotSim {

	private short ll = 10;
	private short rr = 11;
	
	public CtBotSimTest(Point3f pos, Vector3f head) {
		super(pos,head);		
	}

	protected void init() {
	}	

	public void work(){

		ll = rr = 0;
		
		int irL = this.getSensIrL();
		int irR = this.getSensIrR();

		if (irL >= 500){
			ll = 20;
		}
		
		if (irL <500 && irL >= 200){
			ll = 5;
		}
		
		if (irL < 200 && irL >= 50){
			ll = 10;
		}

		if (irL < 50){
			ll = -5;			
		}

		if (irR >= 500){
			rr = 20;
		}
		
		if (irR <500 && irR >= 200){
			rr = 5;
		}
		
		if (irR < 200 && irR >= 50){
			rr = 20;
		}

		if (irR < 50){
			rr = -5;
		}
			
		this.setAktMotL(ll);
		this.setAktMotR(rr);		
		
		super.work();
	}
}
