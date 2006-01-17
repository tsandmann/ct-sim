/*
 * c't-Sim - Robotersimulator für den c't-Bot
 * 
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version. 
 * This program is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR 
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public 
 * License along with this program; if not, write to the Free 
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 * 
 */
package ctSim.Model;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;

/**
 * Superklasse fuer alle simulierten Bots.</br> Die Klasse ist abstrakt und
 * muss daher erst abgeleitet werden, um instanziiert werden zu koennen.</br>
 * Der Haupt-Thread kuemmert sich um die eigentliche Simulation und die
 * Koordination mit dem Zeittakt der Welt. Die Kommunikation muss von den
 * abgeleiteten Klassen selbst behandelt werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 */

abstract public class CtBotSim extends CtBot {

	/** Interne Zeitbasis in Millisekunden. */
	protected long simulTime = 0;

	/**
	 * Interne Zeitbasis in Millisekunden -- Zeitaenderung seit letzter
	 * Simulation
	 */
	protected long deltaT = 0;

	/**
	 * Nach der letzten Simulation noch nicht verarbeitete Teil-Encoder-Schritte
	 * (links)
	 */
	private double encoderRestL = 0;

	/**
	 * Nach der letzten Simulation noch nicht verarbeitete Teil-Encoder-Schritte
	 * (rechts)
	 */
	private double encoderRestR = 0;

	/** Soll der IR-Sensor automatisch aktualisert werden? */
	boolean updateSensIr = true;

	/**
	 * Erzeugt einen neuen Bot
	 * 
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 */
	public CtBotSim(Point3f pos, Vector3f head) {
		super(pos, head);
		/* Die IR-Sensoren koennen ueber den Simulator beeinflusst werden */
		CAP_SENS_IR = true;
	}

	/*
	 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde @param
	 * motPWM PWM-Verhaeltnis @return Umdrehungen pro Sekunde
	 */
	private float calculateWheelSpeed(int motPWM) {
		float tmp = ((float) motPWM / (float) PWM_MAX);
		tmp = tmp * UPS_MAX;
		return tmp;
		// TODO Die Kennlinien der echten Motoren ist nicht linear
	}

	/**
	 * Aktualisiert den Status des Bot. In dieser Routine sitzt ein Grossteil
	 * der Intelligenz des Simulators.<br/> Dabei wird alles relativ zu deltaT
	 * berechnet.
	 * 
	 * @see BotSim#deltaT
	 */
	protected void updateStats() {

		// TODO Hier sitzt der Bug bei den Radencodern!!!

		// Anzahl der Umdrehungen der Raeder
		double turnsL = calculateWheelSpeed(this.getAktMotL());
		turnsL = turnsL * (float) deltaT / 1000.0f;
		double turnsR = calculateWheelSpeed(this.getAktMotR());
		turnsR = turnsR * (float) deltaT / 1000.0f;

		// Encoder-Schritte als Gleitzahl errechnen:
		// Anzahl der Drehungen mal Anzahl der Markierungen,
		// dazu der Rest der letzten Runde
		double tmp = (turnsL * ENCODER_MARKS) + encoderRestL;
		// Der Bot bekommt nur ganze Schritte zu sehen,
		short encoderSteps = (short) Math.floor(tmp);
		// aber wir merken uns Teilschritte intern
		encoderRestL = tmp - encoderSteps;
		// und speichern sie.
		this.setSensEncL((short) (this.getSensEncL() + encoderSteps));

		// Encoder-Schritte als Gleitzahl errechnen:
		// Anzahl der Drehungen mal Anzahl der Markierungen,
		// dazu der Rest der letzten Runde
		tmp = (turnsR * ENCODER_MARKS) + encoderRestR;
		// Der Bot bekommt nur ganze Schritte zu sehen,
		encoderSteps = (short) Math.floor(tmp);
		// aber wir merken uns Teilschritte intern
		encoderRestR = tmp - encoderSteps;
		// und speichern sie.
		this.setSensEncR((short) (this.getSensEncR() + encoderSteps));

		// Zurueckgelegte Strecke linkes Rad als Vector
		Vector3f vecL = new Vector3f(this.getHeading());
		vecL.scale((float) (turnsL * RAD_UMFANG), vecL);

		// Zurueckgelegte Strecke rechtes Rad als Vector
		Vector3f vecR = new Vector3f(this.getHeading());
		vecR.scale((float) (turnsR * RAD_UMFANG), vecR);

		// Vektor vom Ursprung zum linken Rad
		Vector3f vec = new Vector3f((float) -this.getHeading().y, (float) this
				.getHeading().x, 0f);
		vec.scale((float) RAD_ABSTAND, vec);

		// neue Position linkes Rad
		Vector3f posRadL = new Vector3f(this.getPos());
		posRadL.add(vec);
		posRadL.add(vecL);

		// Vektor vom Ursprung zum rechten Rad
		vec = new Vector3f((float) this.getHeading().y, (float) -this
				.getHeading().x, 0f);
		vec.scale((float) RAD_ABSTAND, vec);

		// neue Position rechtes Rad
		Vector3f posRadR = new Vector3f(this.getPos());
		posRadR.add(vec);
		posRadR.add(vecR);

		// Neue Position berechnen
		Vector3f mid = new Vector3f(posRadR); // fange rechts an
		mid.sub(posRadL); // ziehe linke Position ab
		mid.scale(0.5f, mid); // bilde relative Mitte

		Vector3f newPos = new Vector3f(posRadR); // und berechne die absolute
		newPos.sub(mid);

		// Jetzt fehlt noch die neue Blickrichtung
		Vector3f newHeading = new Vector3f(-mid.y, mid.x, 0);
		newHeading.normalize();

		// Pruefen, ob Kollision erfolgt
		if (world.checkCollision(getBounds(), newPos)) {
			// Wenn nicht, Position und Heading aktualisieren
			this.setPos(newPos);
		}
		this.setHeading(newHeading);

		// IR-Abstandssensoren aktualisieren
		if (updateSensIr) {
			this.setSensIrL(world.watchObstacle(getSensPosition('L'),
					new Vector3d(newHeading)));
			this.setSensIrR(world.watchObstacle(getSensPosition('R'),
					new Vector3d(newHeading)));
		}
	}

	/**
	 * Liefert die Position eines IR-Sensors zurueck
	 * 
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	private Point3d getSensPosition(char side) {
		// Vektor vom Ursprung in Axial-Richtung
		Vector3f vecX;
		if (side == 'L')
			vecX = new Vector3f(-this.getHeading().y, this.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_ABSTAND_Z));
		else
			vecX = new Vector3f(this.getHeading().y, -this.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_ABSTAND_Z));

		vecX.scale((float) SENS_IR_ABSTAND_X, vecX);

		// Vektor vom Ursprung in Voraus-Richtung
		Vector3f vecY = new Vector3f(this.getHeading());
		vecY.scale((float) SENS_IR_ABSTAND_Y, vecY);

		// Ursprung
		Vector3f pos = new Vector3f(this.getPos());
		pos.add(vecX); // Versatz nach links
		pos.add(vecY); // Versatz nach vorne

		return new Point3d(pos);
	}

	/**
	 * In dieser Methode stehen die Routinen, die der Bot immer wieder
	 * durchlauft; sie darf keine Schleife enthalten! Die Methode kuemmert sich
	 * um das Timing, indem sie world.getSimulTime() aufruft, diese sorgt
	 * dafuer, dass jeder simulierte Bot pro Runde nur einmal seine
	 * work()-Methode durchlaufen kann. <br/> Unterklassen sollten diese Methode
	 * ueberschreiben, aber zu deren Beginn super.work() aufrufen
	 * 
	 * @see ctSim.Model.Bot#work()
	 * @see ctSim.Model.World#getSimulTime()
	 */
	protected void work() {
		long tmpTime = simulTime;
		try {
			simulTime = world.getSimulTime(); // warten bis World den
												// naechsten Schritt macht
			deltaT = simulTime - tmpTime; // aktualisiere deltaT
			updateStats();
			this.getPanel().reactToChange();
		} catch (InterruptedException e) {
			ErrorHandler.error("Bot: " + getBotName() + " dies " + e);
			die();
		}
	}

	/**
	 * Wird er IRSensor automatisch aktualisert?
	 * 
	 * @return true wenn ja
	 */
	public boolean isUpdateSensIr() {
		return updateSensIr;
	}

	/**
	 * Soll der IR-Sensor automatisch aktualisiert werden?
	 * 
	 * @param updateSensIr
	 *            true, wenn automatisch
	 */
	public void setUpdateSensIr(boolean updateSensIr) {
		this.updateSensIr = updateSensIr;
	}
}
