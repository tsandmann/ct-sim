package ctSim.Model;


import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;

/**
 * Superklasse für alle simulierten Bots.<b> 
 * Ist abstract und muss daher abgeleitet werden.<b>
 * Der Haupt-Thread kümmert sich um Simulation/Koordination. Kommunikation muss 
 * von den abgeleiteten Klassen anderweitig behandelt werden.
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter König (pek@heise.de)
 */
abstract public class CtBotSim extends CtBot {

	/** Interne Zeitbasis in Millisekunden.*/
	protected long simulTime=0;

	/** Interne Zeitbasis in Millisekunden Zeitänderung seit letzter Simulation */
	protected long deltaT=0;	
	
	/** Nach der letzten Simulation noch nicht verarbeitete Teil-Encoder-Schritte (links)*/
	private double encoderRestL=0;

	/** Nach der letzten Simulation noch nicht verarbeitete Teil-Encoder-Schritte (links)*/
	private double encoderRestR=0;
	
	/** Soll der IRSensor automatisch aktualisert werden? */
	boolean updateSensIr= true;
	
	public CtBotSim(Point3f pos, Vector3f head) {
		super(pos, head);
		/** Die IR-Sensoren können beeinflusst werden */
		CAP_SENS_IR  = true;

		// TODO Auto-generated constructor stub
	}

	/**
	 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde
	 * @param motPWM PWM-Verhältnis
	 * @return Umdrehungen pro Sekunde
	 */
	private float calculateWheelSpeed(int motPWM){
		float tmp=((float)motPWM / (float)PWM_MAX);
		tmp= tmp*UPS_MAX;
		return tmp;
		// TODO Bitte präzisieren. Das ist in echt nicht wirklich linear!!!
	}
	
	
	/**
	 * Aktualisiert die Stats des Bots. 
	 * In dieser Routine sitzt ein Großteil der Intelligenz des Simulators
	 * Sollte alles relativ zu deltaT machen
	 * @see BotSim#deltaT
	 */
	protected void updateStats(){
		// TODO Complete me

		// TODO Hier sitzt in BUg bei den Radencodern!!!
		
		// Anzahl der Umdrehungen der Räder
		double turnsL = calculateWheelSpeed(this.getAktMotL());
		turnsL= turnsL * (float)deltaT / 1000.0f ;
		double turnsR = calculateWheelSpeed(this.getAktMotR());
		turnsR = turnsR * (float)deltaT / 1000.0f ;
				
		// Encoder-Schritte als Gleitzahl
		double tmp =  (turnsL * ENCODER_MARKS) + encoderRestL;
		// Der Bot bekommt nur ganze Schritte zu sehen
		short encoderSteps = (short)Math.floor(tmp);
		// aber wir merken uns Teilschritte intern
		encoderRestL = tmp - encoderSteps;
		// und speichern
		this.setSensEncL((short)(this.getSensEncL()+encoderSteps));
		
		// Encoder-Schritte als Gleitzahl
		tmp =  (turnsR * ENCODER_MARKS) + encoderRestR;
		// Der Bot bekommt nur ganze Schritte zu sehen
		encoderSteps = (short)Math.floor(tmp);
		// aber wir merken uns Teilschritte intern
		encoderRestR = tmp - encoderSteps;
		// und speichern
		this.setSensEncR((short)(this.getSensEncR()+encoderSteps));
		
		// Zurückgelegte Strecke linkes Rad als Vector
		Vector3f vecL = new Vector3f(this.getHeading());
		vecL.scale((float)(turnsL * RAD_UMFANG),vecL);
		
		//	Zurückgelegte Strecke linkes Rad als Vector
		Vector3f vecR = new Vector3f(this.getHeading());
		vecR.scale((float)(turnsR * RAD_UMFANG),vecR);
				
		// Vektor vom Ursprung zum linken Rad
		Vector3f vec = new Vector3f((float)-this.getHeading().y,(float)this.getHeading().x,0f);
		vec.scale((float)RAD_ABSTAND,vec);
		
		// neue Position linkes Rad
		Vector3f posRadL = new Vector3f(this.getPos());
		posRadL.add(vec);
		posRadL.add(vecL);

		// Vektor vom Ursprung zum linken Rad
		vec = new Vector3f((float)this.getHeading().y,(float)-this.getHeading().x,0f);
		vec.scale((float)RAD_ABSTAND,vec);
		
		// neue Position linkes Rad
		Vector3f posRadR = new Vector3f(this.getPos());
		posRadR.add(vec);
		posRadR.add(vecR);
		
		// Neue Position berechnen
		Vector3f mid = new Vector3f(posRadR);	// fange rechts an
		mid.sub(posRadL);						// ziehe linke Position ab
		mid.scale(0.5f,mid);					// relative Mitte bilden
		
		Vector3f newPos = new Vector3f(posRadR);	// und absolute berechnen
		newPos.sub(mid);
		
		
		// jetzt fehlt noch die neue Heading
		Vector3f newHeading = new Vector3f(-mid.y,mid.x,0);
		newHeading.normalize();

		// Prüfen, ob Kollision erfolgt
		if (world.checkCollision(getBounds(),newPos)){
			//Wenn nicht, Position und Heading aktualisieren
			this.setPos(newPos);				// speichern
		}
		// TODO Überlegen, ob Heading auf jedenfall aktualisieren ?? 
		this.setHeading(newHeading);	// speichern
		
		
		// IR-Abstandssensoren aktualisieren
		if (updateSensIr){
			this.setSensIrL(world.watchObstacle(getSensPosition('L'),new Vector3d(newHeading)));
			this.setSensIrR(world.watchObstacle(getSensPosition('R'),new Vector3d(newHeading)));
		}
	}

	/**
	 * Liefert die Position des rechten IR-Sensors zurück
	 * @param side Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	private Point3d getSensPosition(char side) {
		// Vektor vom Ursprung in Axial-Richtung
		Vector3f vecX;
		if (side == 'L')
			vecX = new Vector3f(-this.getHeading().y,this.getHeading().x,(float)(BOT_HEIGHT/2+SENS_IR_ABSTAND_Z));
		else
			vecX = new Vector3f(this.getHeading().y,-this.getHeading().x,(float)(BOT_HEIGHT/2+SENS_IR_ABSTAND_Z));
			
		vecX.scale((float)SENS_IR_ABSTAND_X,vecX);

		// Vektor vom Ursprung in Voraus-Richtung		
		Vector3f vecY = new Vector3f(this.getHeading());
		vecY.scale((float)SENS_IR_ABSTAND_Y,vecY);
		
		// Ursprung
		Vector3f pos = new Vector3f(this.getPos());
		pos.add(vecX);	// Verschub nach links
		pos.add(vecY);	// Verschub nach vorne

		return new Point3d(pos);
	}

	/**
	 * Hier geschieht die eigentliche Arbeit
	 * Die Methode darf keine Schleife enthalten!
	 * Die Methode kümmert sich um das Timing, indem sie world.getSimulTime() aufruft. 
	 * Diese ist blockierend. <b>
	 * Unterklassen sollten diese Methode überschreiben, aber zu Beginn super.work(); aufrufen 
	 * @see ctSim.Model.AbstractBot#work()
	 * @see ctSim.Model.World#getSimulTime()
	 */
	protected void work() {
		long tmpTime=simulTime;
		try {
			simulTime=world.getSimulTime();		// warten bis World den nächsten Schritt macht
			deltaT=simulTime-tmpTime;			// aktualisiere deltaT
			updateStats();
			this.getPanel().reactToChange();
			System.out.println("Bot: "+getBotName()+" Zeit: "+simulTime);
		} catch (InterruptedException e) {
			//e.printStackTrace();
			ErrorHandler.error("Bot: "+getBotName()+" dies "+e);
			die();
		}
	}

	/**
	 * Wird er IRSensor automatisch aktualisert?
	 * @return true wenn ja.
	 */
	public boolean isUpdateSensIr() {
		return updateSensIr;
	}

	/**
	 * Soll der IR-Sensor automatisch aktualisiert werden?
	 * @param updateSensIr true, wenn automatisch
	 */
	public void setUpdateSensIr(boolean updateSensIr) {
		this.updateSensIr = updateSensIr;
	}
}
