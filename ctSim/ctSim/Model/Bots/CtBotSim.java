/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
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

package ctSim.Model.Bots;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;

import ctSim.SimUtils;
import ctSim.ErrorHandler;
import ctSim.Controller.Controller;
import ctSim.View.CtControlPanel;

/**
 * Superklasse fuer alle simulierten Bots.</br> Die Klasse ist abstrakt und
 * muss daher erst abgeleitet werden, um instanziiert werden zu koennen.</br>
 * Der Haupt-Thread kuemmert sich um die eigentliche Simulation und die
 * Koordination mit dem Zeittakt der Welt. Die Kommunikation muss von den
 * abgeleiteten Klassen selbst behandelt werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Stefan Geuken (mail@stefan-geuken.de)
 */

abstract public class CtBotSim extends CtBot {

	/** Interne Zeitbasis in Millisekunden. */
	protected long simulTime = 0;

	/**
	 * Interne Zeitbasis in Millisekunden -- Zeitaenderung seit letztem
	 * Simulationschritt
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

	/** Sollen die IR-Sensoren automatisch aktualisert werden? */
	boolean updateSensIr = true;

	/** Soll der Maussensor automatisch aktualisert werden? */
	boolean updateSensMouse = true;

	/** Sollen die Liniensensoren automatisch aktualisert werden? */
	boolean updateSensLine = true;

	/** Sollen die Abgrundsensoren automatisch aktualisert werden? */
	boolean updateSensBorder = true;

	/** Sollen die Lichtsensoren automatisch aktualisert werden? */
	boolean updateSensLdr = true;

	/** Ist das normale Aussehen des Bots gesetzt? */
	boolean isApperance = true;

	/** Ist das Aussehen nach einer Kollision des Bots gesetzt? */
	boolean isApperanceCollision = false;

	/** Ist das Aussehen nach einem Fall des Bots gesetzt? */
	boolean isApperanceFall = false;

	/**
	 * Erzeugt einen neuen Bot
	 * 
	 * @param pos
	 *            initiale Position
	 * @param head
	 *            initiale Blickrichtung
	 */
	public CtBotSim(Controller controller, Point3f pos, Vector3f head) {
		super(controller, pos, head);
		/* Die IR-Sensoren koennen ueber den Simulator beeinflusst werden */
		CAP_SENS_IR = true;
	}

	/*
	 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde
	 * 
	 * @param motPWM PWM-Verhaeltnis @return Umdrehungen pro Sekunde
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

		// alte Position und Heading merken
		// (wird spaeter fuer die Sensorberechnung benoetigt)
//		Vector3f oldPos = (Vector3f) this.getPos().clone();
//		Vector3f oldHeading = (Vector3f) this.getHeading().clone();

		// Anzahl der Umdrehungen der Raeder
		double turnsL = calculateWheelSpeed(this.getActMotL());
		turnsL = turnsL * (float) deltaT / 1000.0f;
		double turnsR = calculateWheelSpeed(this.getActMotR());
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

		/*
		 * // Zurueckgelegte Strecke linkes Rad als Vector Vector3f vecL = new
		 * Vector3f(this.getHeading()); vecL.scale((float) (turnsL *
		 * WHEEL_PERIMETER), vecL);
		 *  // Zurueckgelegte Strecke rechtes Rad als Vector Vector3f vecR = new
		 * Vector3f(this.getHeading()); vecR.scale((float) (turnsR *
		 * WHEEL_PERIMETER), vecR);
		 *  // Vektor vom Ursprung zum linken Rad Vector3f vec = new
		 * Vector3f((float) -this.getHeading().y, (float) this .getHeading().x,
		 * 0f); vec.scale((float) RAD_ABSTAND, vec);
		 *  // neue Position linkes Rad Vector3f posRadL = new
		 * Vector3f(this.getPos()); posRadL.add(vec); posRadL.add(vecL);
		 *  // Vektor vom Ursprung zum rechten Rad vec = new Vector3f((float)
		 * this.getHeading().y, (float) -this .getHeading().x, 0f);
		 * vec.scale((float) RAD_ABSTAND, vec);
		 *  // neue Position rechtes Rad Vector3f posRadR = new
		 * Vector3f(this.getPos()); posRadR.add(vec); posRadR.add(vecR);
		 *  // Neue Position berechnen Vector3f mid = new Vector3f(posRadR); //
		 * fange rechts an mid.sub(posRadL); // ziehe linke Position ab
		 * mid.scale(0.5f, mid); // bilde relative Mitte
		 * 
		 * Vector3f newPos = new Vector3f(posRadR); // und berechne die absolute
		 * newPos.sub(mid);
		 */

		// Fuer ausfuehrliche Erlaeuterung der Positionsberechnung siehe pdf
		// Absolut zurueckgelegte Strecke pro Rad berechnen
		double s_l = turnsL * WHEEL_PERIMETER;
		double s_r = turnsR * WHEEL_PERIMETER;

		// halben Drehwinkel berechnen
		double _gamma = (s_l - s_r) / (4.0 * WHEEL_DIST);

		/*
		 * // Jetzt fehlt noch die neue Blickrichtung Vector3f newHeading = new
		 * Vector3f(-mid.y, mid.x, 0);
		 */

		// neue Blickrichtung berechnen
		// ergibt sich aus Rotation der Blickrichtung um 2*_gamma
		Vector3f _hd = this.getHeading();
		double _s2g = Math.sin(2 * _gamma);
		double _c2g = Math.cos(2 * _gamma);
		Vector3f newHeading = new Vector3f(
				(float) (_hd.x * _c2g + _hd.y * _s2g),
				(float) (-_hd.x * _s2g + _hd.y * _c2g), 0f);

		newHeading.normalize();

		// Neue Position bestimmen
		Vector3f newPos = new Vector3f(this.getPos());
		double _sg = Math.sin(_gamma);
		double _cg = Math.cos(_gamma);
		double moveDistance;
		if (_gamma == 0) {
			// Bewegung geradeaus
			moveDistance = s_l; // = s_r
		} else {
			// anderfalls die Distanz laut Formel berechnen
			moveDistance = 0.5 * (s_l + s_r) * Math.sin(_gamma) / _gamma;
		}
		// Den Bewegungsvektor berechnen ...
		Vector3f moveDirection = new Vector3f((float) (_hd.x * _cg + _hd.y
				* _sg), (float) (-_hd.x * _sg + _hd.y * _cg), 0f);
		moveDirection.normalize();
		moveDirection.scale((float) moveDistance);
		// ... und die alte Position entsprechend veraendern.
		newPos.add(moveDirection);

		// Pruefen, ob Kollision erfolgt. Bei einer Kollision wird
		// der Bot blau gefaerbt.
		if (world.checkCollision((Shape3D)getNodeReference(BOTBODY), getBounds(), newPos, getName())) {
			// Wenn nicht, Position aktualisieren
			this.setPos(newPos);
			if (isApperanceCollision) {
				setAppearance("normal");
				isApperance = true;
				isApperanceCollision = false;
			}
		} else {

			moveDistance = 0; // Wird spaeter noch fuer den Maussensor benoetigt

			if (isApperance) {
				setAppearance("collision");
				isApperance = false;
				isApperanceCollision = true;
			}
		}
		
		// Blickrichtung immer aktualisieren:
		this.setHeading(newHeading);

		// Bodenkontakt ueberpruefen

		// Vektor vom Ursprung zum linken Rad
		Vector3f vecL = new Vector3f((float) newHeading.y,
				(float) newHeading.x, 0f);
		vecL.scale((float) WHEEL_DIST);
		// neue Position linkes Rad
		Vector3f posRadL = new Vector3f(this.getPos());
		posRadL.add(vecL);

		// Vektor vom Ursprung zum rechten Rad
		Vector3f vecR = new Vector3f((float) newHeading.y,
				(float) -newHeading.x, 0f);
		vecR.scale((float) WHEEL_DIST);
		// neue Position rechtes Rad
		Vector3f posRadR = new Vector3f(this.getPos());
		posRadR.add(vecR);

		// Winkel des heading errechnen
		double angle = SimUtils.getRotation(newHeading);
		// Transformations-Matrix fuer die Rotation erstellen
		Transform3D rotation = new Transform3D();
		rotation.rotZ(angle);

		// Bodenkontakt des Gleitpins ueberpruefen
		Vector3d skidVec = new Vector3d(BOT_SKID_X, BOT_SKID_Y, -BOT_HEIGHT / 2);
		// Position des Gleitpins gemaess der Ausrichtung des Bots anpassen
		rotation.transform(skidVec);
		skidVec.add(new Point3d(newPos));
		boolean isFalling = false;
		if (!world.checkTerrain(new Point3d(skidVec), BOT_GROUND_CLEARANCE,
				"Der Gleitpin von " + this.getName())) {
			isFalling = true;
		}

		// Bodenkontakt des linken Reifens ueberpruefen
		posRadL.z -= BOT_HEIGHT / 2;
		if (!world.checkTerrain(new Point3d(posRadL), BOT_GROUND_CLEARANCE,
				"Das linke Rad von " + this.getName())) {
			isFalling = true;
		}

		// Bodenkontakt des rechten Reifens ueberpruefen
		posRadR.z -= BOT_HEIGHT / 2;
		if (!world.checkTerrain(new Point3d(posRadR), BOT_GROUND_CLEARANCE,
				"Das rechte Rad von " + this.getName())) {
			isFalling = true;
		}

		// Wenn einer der Beruehrungspunkte keinen Boden mehr unter sich hat,
		// wird der Bot gestoppt und gruen gefaerbt.
		if (isFalling) {
			((CtControlPanel) this.getPanel()).stopBot();
			if (isApperance) {
				setAppearance("falling");//world.getBotAppearFall());
				isApperance = false;
				isApperanceFall = true;
			}
		} else {
			if (isApperanceFall) {
				setAppearance("normal");//world.getBotAppear());
				isApperance = true;
				isApperanceCollision = false;
			}
		}
		
		// IR-Abstandssensoren aktualisieren
		if (updateSensIr) {
			this.setSensIrL(world.watchObstacle(getSensIRPosition('L'),
					new Vector3d(newHeading), SENS_IR_ANGLE, (Shape3D)getNodeReference(BOTBODY)));
			this.setSensIrR(world.watchObstacle(getSensIRPosition('R'),
					new Vector3d(newHeading), SENS_IR_ANGLE, (Shape3D)getNodeReference(BOTBODY)));
		}

		// Liniensensoren aktualisieren
		if (updateSensLine) {
			this.setSensLineL(world.sensGroundReflectionCross(
					getSensLinePosition('L'), new Vector3d(newHeading),
					SENS_LINE_ANGLE, SENS_LINE_PRECISION));
			this.setSensLineR(world.sensGroundReflectionCross(
					getSensLinePosition('R'), new Vector3d(newHeading),
					SENS_LINE_ANGLE, SENS_LINE_PRECISION));
		}

		// Abgrundsensoren aktualisieren
		if (updateSensBorder) {
			this.setSensBorderL(world.sensGroundReflectionCross(
					getSensBorderPosition('L'), new Vector3d(newHeading),
					SENS_BORDER_ANGLE, SENS_BORDER_PRECISION));
			this.setSensBorderR(world.sensGroundReflectionCross(
					getSensBorderPosition('R'), new Vector3d(newHeading),
					SENS_BORDER_ANGLE, SENS_BORDER_PRECISION));
		}

		// Lichtsensoren aktualisieren
		if (updateSensLdr) {
			this.setSensLdrL(world.sensLight(getSensLdrPosition('L'),
					getSensLdrHeading(newHeading), SENS_LDR_ANGLE));
			this.setSensLdrR(world.sensLight(getSensLdrPosition('R'),
					getSensLdrHeading(newHeading), SENS_LDR_ANGLE));
		}

		// Maussensor aktualisieren
		if (updateSensMouse) {
			// DeltaY berechnen

			/*
			 * // Differenz bilden Vector3f vecY = new Vector3f(this.getPos());
			 * vecY.sub(oldPos); // die zurueckgelegte Strecke in Dots int
			 * deltaY = meter2Dots(vecY.length());
			 */

			int deltaY = meter2Dots(moveDistance);

			this.setSensMouseDY(deltaY);

			// DeltaX berechnen
			/*
			 * // Drehung um die eigene Achse berechenen double angleDiff =
			 * SimUtils.getRotation(newHeading) -
			 * SimUtils.getRotation(oldHeading);
			 */

			// Abstand des Maussensors von Zentrum berechnen
			/*
			 * Vector3f vecMs = new Vector3f((float) SENS_MOUSE_DIST_X, (float)
			 * SENS_MOUSE_DIST_Y, 0f); // Drehung(in rad) * Radius bestimmt die
			 * Laenge, die der Maussensor auf einem
			 */

			// Drehung(in rad) * SENS_MOUSE_DIST_Y bestimmt die Laenge, die der
			// Maussensor auf einem
			// imaginaeren Kreis um den Mittelpunkt des Bots abgelaufen ist.
			/*
			 * int deltaX = meter2Dots(angleDiff * vecMs.length());
			 */
			// !!Dies ist nur korrekt solange SENS_MOUSE_ABSTAND_X==0 gilt
			int deltaX = meter2Dots(2 * _gamma * SENS_MOUSE_DIST_Y);
			this.setSensMouseDX(deltaX);
		}

	}

	/**
	 * Errechnet die Blickrichtung eines Lichtsensors.
	 * 
	 * @param newHeading
	 *            Blickrichtung des Bots
	 * @return Gibt die Blickrichtung des Sensors zurueck
	 */
	private Vector3d getSensLdrHeading(Vector3f newHeading) {
		if (SENS_LDR_HEADING.z == 1)
			return SENS_LDR_HEADING;
		else
			return new Vector3d(newHeading);
	}

	/**
	 * Liefert die Position eines Liniensensors zurueck
	 * 
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	private Point3d getSensLinePosition(char side) {
		double angle = SimUtils.getRotation(getHeading());
		Transform3D rotation = new Transform3D();
		rotation.rotZ(angle);
		Point3d ptLine;
		if (side == 'L') {
			ptLine = new Point3d(-SENS_LINE_DIST_X, SENS_LINE_DIST_Y,
					SENS_LINE_DIST_Z);
		} else {
			ptLine = new Point3d(SENS_LINE_DIST_X, SENS_LINE_DIST_Y,
					SENS_LINE_DIST_Z);
		}
		rotation.transform(ptLine);
		ptLine.add(new Point3d(getPos()));
		return ptLine;
	}

	/**
	 * Liefert die Position eines Abgrundsensors zurueck
	 * 
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	private Point3d getSensBorderPosition(char side) {
		double angle = SimUtils.getRotation(getHeading());
		Transform3D rotation = new Transform3D();
		rotation.rotZ(angle);
		Point3d ptBorder;
		if (side == 'L') {
			ptBorder = new Point3d(-SENS_BORDER_DIST_X, SENS_BORDER_DIST_Y,
					SENS_BORDER_DIST_Z);
		} else {
			ptBorder = new Point3d(SENS_BORDER_DIST_X, SENS_BORDER_DIST_Y,
					SENS_BORDER_DIST_Z);
		}
		rotation.transform(ptBorder);
		ptBorder.add(new Point3d(getPos()));
		return ptBorder;
	}

	/**
	 * Liefert die Position eines Lichtsensors zurueck
	 * 
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	private Point3d getSensLdrPosition(char side) {
		double angle = SimUtils.getRotation(getHeading());
		Transform3D rotation = new Transform3D();
		rotation.rotZ(angle);
		Point3d ptLdr;
		if (side == 'L') {
			ptLdr = new Point3d(-SENS_LDR_DIST_X, SENS_LDR_DIST_Y,
					SENS_LDR_DIST_Z);
		} else {
			ptLdr = new Point3d(SENS_LDR_DIST_X, SENS_LDR_DIST_Y,
					SENS_LDR_DIST_Z);
		}
		rotation.transform(ptLdr);
		ptLdr.add(new Point3d(getPos()));
		return ptLdr;
	}

	/**
	 * Errechnet die Anzahl an Dots, die der Maussensor fuer eine Bewegung der
	 * angegebenen Laenge zurueckmeldet.
	 * 
	 * @param distance
	 *            die Laenge der Strecke in Metern
	 * @return
	 */
	private int meter2Dots(double distance) {
		// distance ist in Metern angegeben,
		// * 100 macht daraus cm; 2,54 cm sind ein Inch,
		// anschliessend Multiplikation mit der Aufloesung des Maussensors
		return (int) ((distance * 100 / 2.54) * SENS_MOUSE_DPI);
	}

	/**
	 * Liefert die Position eines IR-Sensors zurueck
	 * 
	 * @param side
	 *            Welcher Sensor 'L' oder 'R'
	 * @return Die Position
	 */
	private Point3d getSensIRPosition(char side) {
		// Vektor vom Ursprung in Axial-Richtung
		Vector3f vecX;
		if (side == 'L')
			vecX = new Vector3f(-this.getHeading().y, this.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_DIST_Z));
		else
			vecX = new Vector3f(this.getHeading().y, -this.getHeading().x,
					(float) (BOT_HEIGHT / 2 + SENS_IR_DIST_Z));

		vecX.scale((float) SENS_IR_DIST_X, vecX);

		// Vektor vom Ursprung in Voraus-Richtung
		Vector3f vecY = new Vector3f(this.getHeading());
		vecY.scale((float) SENS_IR_DIST_Y, vecY);

		// Ursprung
		Vector3f pos = new Vector3f(this.getPos());
		pos.add(vecX); // Versatz nach links
		pos.add(vecY); // Versatz nach vorne

		return new Point3d(pos);
	}

	/**
	 * In dieser Methode stehen die Routinen, die der Bot immer wieder
	 * durchlaeuft; sie darf keine Schleife enthalten! Die Methode kuemmert sich
	 * um das Timing, indem sie world.getSimulTime() aufruft, diese sorgt
	 * dafuer, dass jeder simulierte Bot pro Runde nur einmal seine
	 * work()-Methode durchlaufen kann. <br/> Unterklassen sollten diese Methode
	 * ueberschreiben, aber zu deren Beginn super.work() aufrufen
	 * 
	 * @see ctSim.Model.Bot#work()
	 * @see ctSim.Model.World#getSimulTime()
	 */
	protected void work() {
		super.work();
		long tmpTime = simulTime;
		try {
			simulTime = world.getSimulTime();
			// warten, bis World den naechsten Schritt macht
			deltaT = simulTime - tmpTime; // aktualisiere deltaT
			updateStats();
		} catch (InterruptedException e) {
			ErrorHandler.error("Bot: " + getName() + " wurde unterbrochen");
		}
	}

	/**
	 * Werden die IR-Sensoren automatisch aktualisert?
	 * 
	 * @return true wenn ja
	 */
	public boolean isUpdateSensIr() {
		return updateSensIr;
	}

	/**
	 * Sollen die IR-Sensoren automatisch aktualisiert werden?
	 * 
	 * @param updateSensIr
	 *            true wenn automatisch
	 */
	public void setUpdateSensIr(boolean updateSensIr) {
		this.updateSensIr = updateSensIr;
	}

	/**
	 * Wird der Maussensor automatisch aktualisert?
	 * 
	 * @return true wenn ja
	 */
	public boolean isUpdateSensMouse() {
		return updateSensMouse;
	}

	/**
	 * Soll der Maussensor automatisch aktualisiert werden?
	 * 
	 * @param updateSensMouse
	 *            true wenn automatisch
	 */
	public void setUpdateSensMouse(boolean updateSensMouse) {
		this.updateSensMouse = updateSensMouse;
	}

	/**
	 * Werden die Liniensensoren automatisch aktualisert?
	 * 
	 * @return true wenn ja
	 */
	public boolean isUpdateSensLine() {
		return updateSensLine;
	}

	/**
	 * Sollen die Liniensensoren automatisch aktualisiert werden?
	 * 
	 * @param updateSensLine
	 *            true wenn automatisch
	 */
	public void setUpdateSensLine(boolean updateSensLine) {
		this.updateSensLine = updateSensLine;
	}

	/**
	 * Werden die Abgrundsensoren automatisch aktualisert?
	 * 
	 * @return true wenn ja
	 */
	public boolean isUpdateSensBorder() {
		return updateSensBorder;
	}

	/**
	 * Sollen die Abgrundsensoren automatisch aktualisiert werden?
	 * 
	 * @param updateSensBorder
	 *            true wenn automatisch
	 */
	public void setUpdateSensBorder(boolean updateSensBorder) {
		this.updateSensBorder = updateSensBorder;
	}

	/**
	 * Werden die Lichtsensoren automatisch aktualisert?
	 * 
	 * @return true wenn ja
	 */
	public boolean isUpdateSensLdr() {
		return updateSensLdr;
	}

	/**
	 * Sollen die Lichtsensoren automatisch aktualisiert werden?
	 * 
	 * @param updateSensLdr
	 *            true wenn automatisch
	 */
	public void setUpdateSensLdr(boolean updateSensLdr) {
		this.updateSensLdr = updateSensLdr;
	}
}
