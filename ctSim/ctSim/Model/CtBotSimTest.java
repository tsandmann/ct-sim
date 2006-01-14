package ctSim.Model;


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

		// Ansteuerung fuer die Motoren in Abhaengigkeit vom Input
		// der IR-Abstandssensoren, welche die Entfernung in mm 
		// zum naechsten Hindernis in Blickrichtung zurueckgeben
		
		
		
		
		// Solange die Wand weit weg ist, wird Stoff gegeben:
		if (irL >= 500){
			ll = 20;
		}
		if (irR >= 500){
			rr = 20;
		}

		// Vorsicht, die Wand kommt naeher:
		// Jetzt den Motor auf der Seite, die weiter entfernt ist, 
		// langsamer laufen lassen als den auf der anderen Seite 
		// - dann bewegt sich der Bot selbst
		// bei Wandkollisionen noch etwas und kommt eventuell
		// wieder frei:
		if (irL <500 && irL >= 200){
			if (irL <= irR)
				ll = 7;
			else ll = 5; 
		}
		if (irR <500 && irR >= 200){
			if (irL >= irR)
				rr = 7;
			else rr = 5; 
		}
		
		// Kollision droht: Auf dem Teller rausdrehen! 
		if (irL < 200 || irR < 200){
			// Drehung von der Wand weg:
			if (irL <= irR){
				ll = 10;			
				rr = -10;
				} else {
					ll = -10;			
					rr = 10;
					}
		}
			

		this.setAktMotL(ll);
		this.setAktMotR(rr);		
		
//		this.setAktMotL((short)10);
//		this.setAktMotR((short)10);		
	
		
		super.work();
	}
}
