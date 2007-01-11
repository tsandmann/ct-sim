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
package ctSim.model.bots.ctbot;

import static ctSim.model.bots.components.BotComponent.ConnectionFlags.READS;
import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.Connection;
import ctSim.SimUtils;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.World;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.Characteristic;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.components.Actuator.Governor;
import ctSim.model.bots.components.actuators.LcDisplay;
import ctSim.model.bots.components.actuators.Led;
import ctSim.model.bots.components.sensors.SimpleSensor;
import ctSim.model.bots.ctbot.components.BorderSensor;
import ctSim.model.bots.ctbot.components.DistanceSensor;
import ctSim.model.bots.ctbot.components.EncoderSensor;
import ctSim.model.bots.ctbot.components.LightSensor;
import ctSim.model.bots.ctbot.components.LineSensor;
import ctSim.model.bots.ctbot.components.RemoteControlSensor;
import ctSim.util.FmtLogger;
import ctSim.view.gui.Debug;

/**
 * Klasse aller simulierten c't-Bots, die ueber TCP mit dem Simulator
 * kommunizieren
 */
public class CtBotSimTcp extends CtBotSim {
	private static final Color[] ledColors = {
		new Color(  0,  84, 255), // blau
		new Color(  0,  84, 255), // blau
		Color.RED,
		new Color(255, 200,   0), // orange
		Color.YELLOW,
		Color.GREEN,
		new Color(  0, 255, 210), // tuerkis
		Color.WHITE,
	};

	FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.ctbot.CtBotSimTcp");

	/** Die TCP-Verbindung */
	private Connection connection;

	private ArrayList<Command> commandBuffer = new ArrayList<Command>();

	/** Sequenznummer der TCP-Pakete */
	private int seq = 0;

	/** Umfang eines Rades [m] */
	public static final double WHEEL_PERIMETER = Math.PI * 0.057d;

	/** Abstand Mittelpunkt Bot zum Rad [m] */
	public static final double WHEEL_DIST = 0.0485d;

	/** Abstand Zentrum Maussensor in Vorausrichtung (Y) [m] */
	public static final double SENS_MOUSE_DIST_Y = -0.015d;

	/** Aufloesung des Maussensors [DPI] */
	public static final int SENS_MOUSE_DPI = 400;

	private World world;

	private int  mouseX, mouseY;

	private Sensor<?>
		irL, irR,
		lineL, lineR,
		borderL, borderR,
		lightL, lightR,
		rc5;

	///////////////////////////////////////////////////////////////////////////

	//$$ deltaT
	class WheelSimulator {
		/**
		 * Maximale Geschwindigkeit als <a
		 * href="http://en.wikipedia.org/wiki/Pulse-width_modulation">PWM-Wert</a>
		 */
		private static final short PWM_MAX = 255;

		/**
		 * Maximale Geschwindigkeit in Umdrehungen pro Sekunde ("revolutions per
		 * second")
		 */
		private static final float REVS_PER_SEC_MAX = 151f / 60f;

		private final Actuator.Governor governor;

		public WheelSimulator(final Governor governor) {
			this.governor = governor;
		}

		/**
		 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde
		 *
		 * @return Umdrehungen pro Sekunde (exakter Wert, d.h. mit Nachkommaanteil)
		 */
		protected float revsThisSimStep() {
			// LODO Die Kennlinien der echten Motoren sind nicht linear
			float speedRatio = governor.getModel().getValue().floatValue() //$$$ umstaendlich
				/ PWM_MAX;
			float speedInRps = speedRatio * REVS_PER_SEC_MAX;
			float deltaTInSec = getDeltaTInMs() / 1000f;
			return speedInRps * deltaTInSec;
		}
	}

	///////////////////////////////////////////////////////////////////////////

	static class EncoderSimulator implements Runnable {
		/** Anzahl an Encoder-Markierungen auf einem Rad */
		private static final short ENCODER_MARKS = 60;

		/**
		 * Bruchteil des Encoder-Schritts, der beim letztem Sim-Schritt
		 * &uuml;brig geblieben ist [0; 1[
		 */
		private double encoderRest = 0;

		private final WheelSimulator wheel;
		private final EncoderSensor encoder;

		public EncoderSimulator(WheelSimulator wheel, EncoderSensor encoder) {
			this.wheel = wheel;
			this.encoder = encoder;
		}

		public void run() {
			// Anzahl der Umdrehungen der Raeder
			double revs = wheel.revsThisSimStep();

			// Encoder-Schritte als Fliesskommazahl errechnen:
			// Anzahl der Drehungen mal Anzahl der Markierungen,
			// dazu der Rest der letzten Runde
			double tmp = (revs * ENCODER_MARKS) + encoderRest;
			// Der Bot bekommt nur ganze Schritte zu sehen,
			int encoderSteps = (int)Math.floor(tmp);
			// aber wir merken uns Teilschritte intern
			encoderRest = tmp - encoderSteps;
			// und speichern sie.
			encoder.getModel().setValue(encoderSteps); // commit //$$$ umstaendlich
		}
	}

	///////////////////////////////////////////////////////////////////////////

	private final WheelSimulator leftWheel;
	private final WheelSimulator rightWheel;
	private final List<Runnable> simulators = new ArrayList<Runnable>();

	/**
	 * @param w Die Welt
	 * @param name Der Name des Bot
	 * @param pos Position
	 * @param head Blickrichtung
	 * @param con Verbindung
	 */
	public CtBotSimTcp(World w, String name, Point3d pos, Vector3d head,
		Connection con) {

		super(w, name, pos, head);
		this.connection = con;
		this.world = w;

		Actuator.Governor govL;
		Actuator.Governor govR;
		final EncoderSensor encL;
		final EncoderSensor encR;

		components.add(
			govL = new Actuator.Governor(true),
			govR = new Actuator.Governor(false),
			encL = new EncoderSensor(true),
			encR = new EncoderSensor(false),
			new LcDisplay(20, 4),
			new Actuator.Log()
		);

        // LEDs
        int numLeds = ledColors.length;
        for (int i = 0; i < numLeds; i++) {
        	String s = "LED " + (i + 1)
        	         + (i == 0 ? " (vorn rechts)" :
        	            i == 1 ? " (vorn links)" : "");
        	components.add(new Led(s, numLeds - i - 1, ledColors[i]));
        }

        // Component-Flag-Tabelle
		components.applyFlagTable(
			_(Actuator.Governor.class, READS),
			_(EncoderSensor.class    , WRITES),
			_(LcDisplay.class        , READS),
			_(Actuator.Log.class     , READS),
			_(Led.class              , READS)
		);

		// Simulation
		leftWheel = new WheelSimulator(govL);
		rightWheel = new WheelSimulator(govR);

		simulators.add(new EncoderSimulator(leftWheel , encL));
		simulators.add(new EncoderSimulator(rightWheel, encR));

		// ...

		this.irL = new DistanceSensor(this.world, this, "IrL",
			new Point3d(-0.036d, 0.0554d, 0d ), new Vector3d(0d, 1d, 0d));
		this.irR = new DistanceSensor(this.world, this, "IrR",
			new Point3d(0.036d, 0.0554d, 0d), new Vector3d(0d, 1d, 0d));
		this.irL.setCharacteristic(new Characteristic(
			new File("characteristics/gp2d12Left.txt"), 100f));
		this.irR.setCharacteristic(new Characteristic(
			new File("characteristics/gp2d12Right.txt"), 80f));

		this.lineL = new LineSensor(this.world, this, "LineL",
			new Point3d(-0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2),
			new Vector3d(0d, 1d, 0d));
		this.lineR = new LineSensor(this.world, this, "LineR",
			new Point3d(0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2),
			new Vector3d(0d, 1d, 0d));

		this.borderL = new BorderSensor(this.world, this, "BorderL",
			new Point3d(-0.036d, 0.0384d, 0d - BOT_HEIGHT / 2),
			new Vector3d(0d, 1d, 0d));
		this.borderR = new BorderSensor(this.world, this, "BorderR",
			new Point3d(0.036d, 0.0384d, 0d - BOT_HEIGHT / 2),
			new Vector3d(0d, 1d, 0d));

		this.lightL = new LightSensor(this.world, this, "LightL",
			new Point3d(-0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2),
			new Vector3d(0d, 1d, 0d));
		this.lightR = new LightSensor(this.world, this, "LightR",
			new Point3d(0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2),
			new Vector3d(0d, 1d, 0d));

		this.rc5 = new RemoteControlSensor("RC fuer '"+this.getName()+"'",
			new Point3d(), new Vector3d());

		this.addSensor(this.rc5);

		this.addSensor(new SimpleSensor<Integer>("MouseX", new Point3d(),
			new Vector3d()) {

			@SuppressWarnings({"synthetic-access"})
			@Override
			public Integer updateValue() {
				return CtBotSimTcp.this.mouseX;
			}

			@Override
			public String getDescription() {
				return "Maus-Sensor-Wert X";
			}
		});

		this.addSensor(new SimpleSensor<Integer>("MouseY", new Point3d(),
			new Vector3d()) {

			@SuppressWarnings({"synthetic-access"})
			@Override
			public Integer updateValue() {
				return CtBotSimTcp.this.mouseY;
			}

			@Override
			public String getDescription() {
				return "Maus-Sensor-Wert Y";
			}
		});

		this.addSensor(this.irL);
		this.addSensor(this.irR);

		this.addSensor(this.lineL);
		this.addSensor(this.lineR);

		this.addSensor(this.borderL);
		this.addSensor(this.borderR);

		this.addSensor(this.lightL);
		this.addSensor(this.lightR);
	}

	/**
	 * Errechnet die Anzahl an Dots, die der Maussensor fuer eine Bewegung der
	 * angegebenen Laenge zurueckmeldet.
	 *
	 * @param distance
	 *            die Laenge der Strecke in Metern
	 * @return Anzahl der Dots
	 */
	private double meter2Dots(double distance) {
		// distance ist in Metern angegeben,
		// * 100 macht daraus cm; 2,54 cm sind ein Inch,
		// anschliessend Multiplikation mit der Aufloesung des Maussensors
		return distance * 100 / 2.54 * SENS_MOUSE_DPI;
	}

	/** Puffer fuer kleine Mausbewegungen */
	private double deltaXRest = 0;

	/** Puffer fuer kleine Mausbewegungen */
	private double deltaYRest = 0;

	//TODO Bot soll in Loecher reinfahren, in Hindernisse aber nicht. Momentan: calcPos traegt Pos nicht ein, wenn Hindernis oder Loch. Gewuenscht: Bei Loch das erste Mal Pos updaten, alle weiteren Male nicht
	//TODO calcPos umziehen nach Bot oder CtBotSim
	private void calcPos() {
		////////////////////////////////////////////////////////////////////
		// Position und Heading berechnen:

		// Fuer ausfuehrliche Erlaeuterung der Positionsberechnung siehe pdf //$$ Ja welches pdf?

		// Absolut zurueckgelegte Strecke pro Rad berechnen
		double s_l = leftWheel .revsThisSimStep() * WHEEL_PERIMETER;
		double s_r = rightWheel.revsThisSimStep() * WHEEL_PERIMETER;

		// halben Drehwinkel berechnen
		double _gamma = (s_l - s_r) / (4.0 * WHEEL_DIST);

		/*
		 * // Jetzt fehlt noch die neue Blickrichtung Vector3f newHeading = new
		 * Vector3f(-mid.y, mid.x, 0);
		 */

		// neue Blickrichtung berechnen
		// ergibt sich aus Rotation der Blickrichtung um 2*_gamma
		Vector3d _hd = this.getHeading();
		double _s2g = Math.sin(2 * _gamma);
		double _c2g = Math.cos(2 * _gamma);
		Vector3d newHeading = new Vector3d(
				(_hd.x * _c2g + _hd.y * _s2g),
				(-_hd.x * _s2g + _hd.y * _c2g), 0f);

		newHeading.normalize();

		// Neue Position bestimmen
		Vector3d newPos = new Vector3d(this.getPosition());
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
		Vector3d moveDirection = new Vector3d((_hd.x * _cg + _hd.y
				* _sg), (-_hd.x * _sg + _hd.y * _cg), 0f);
		moveDirection.normalize();
		moveDirection.scale(moveDistance);
		// ... und die alte Position entsprechend veraendern.
		newPos.add(moveDirection);


		double tmp = meter2Dots(2 * _gamma * SENS_MOUSE_DIST_Y) + deltaXRest;
		int deltaX = (int) Math.floor(tmp);
		deltaXRest = tmp - deltaX;
		this.mouseX = deltaX;

		tmp = meter2Dots(moveDistance) + deltaYRest;
		int deltaY = (int) Math.floor(tmp);
		deltaYRest = tmp - deltaY;
		this.mouseY = deltaY;


		int oldState = this.getObstState();

		boolean noCol = this.world.checkCollision(this, new BoundingSphere(new Point3d(0d, 0d, 0d), BOT_RADIUS), newPos);
		// Pruefen, ob Kollision erfolgt. Bei einer Kollision wird
		// der Bot blau gefaerbt.
		if(noCol && (oldState & OBST_STATE_COLLISION) == OBST_STATE_COLLISION) {
			// Zustand setzen
			Debug.out.println("Bot "+this.getName()+" hat keinen Unfall mehr!");
			setObstState( oldState & ~OBST_STATE_COLLISION);
		} else if (!noCol && (oldState & OBST_STATE_COLLISION) != OBST_STATE_COLLISION) {
			Debug.out.println("Bot "+this.getName()+" hatte einen Unfall!");
			moveDistance = 0; // Wird spaeter noch fuer den Maussensor benoetigt

			// Zustand setzen
			setObstState( oldState| OBST_STATE_COLLISION);
		}

		// Bodenkontakt ueberpruefen

		// Vektor vom Ursprung zum linken Rad
		Vector3d vecL = new Vector3d(-newHeading.y,newHeading.x, 0f);
		vecL.scale((float) WHEEL_DIST);
		// neue Position linkes Rad
		Vector3d posRadL = new Vector3d(this.getPosition());
		posRadL.add(vecL);

		// Vektor vom Ursprung zum rechten Rad
		Vector3d vecR = new Vector3d(newHeading.y, -newHeading.x, 0f);
		vecR.scale((float) WHEEL_DIST);
		// neue Position rechtes Rad
		Vector3d posRadR = new Vector3d(this.getPosition());
		posRadR.add(vecR);

		// Winkel des heading errechnen
		double angle = SimUtils.getRotation(newHeading);
		// Transformations-Matrix fuer die Rotation erstellen
		Transform3D rotation = new Transform3D();
		rotation.rotZ(angle);

		/** Abstand Zentrum Gleitpin in Achsrichtung (X) [m] */
		double BOT_SKID_X = 0d;

		/** Abstand Zentrum Gleitpin in Vorausrichtung (Y) [m] */
		double BOT_SKID_Y = -0.054d;

		// Bodenkontakt des Gleitpins ueberpruefen
		Vector3d skidVec = new Vector3d(BOT_SKID_X, BOT_SKID_Y, -BOT_HEIGHT / 2);
		// Position des Gleitpins gemaess der Ausrichtung des Bots anpassen
		rotation.transform(skidVec);
		skidVec.add(new Point3d(newPos));

		boolean isFalling = !this.world.checkTerrain(new Point3d(skidVec), BOT_GROUND_CLEARANCE);

		// Bodenkontakt des linken Reifens ueberpruefen
		posRadL.z -= BOT_HEIGHT / 2;

		isFalling |= !this.world.checkTerrain(new Point3d(posRadL), BOT_GROUND_CLEARANCE);

		// Bodenkontakt des rechten Reifens ueberpruefen
		posRadR.z -= BOT_HEIGHT / 2;

		isFalling |= !this.world.checkTerrain(new Point3d(posRadR), BOT_GROUND_CLEARANCE);

		// Wenn einer der Beruehrungspunkte keinen Boden mehr unter sich hat,
		// wird der Bot gestoppt und gruen gefaerbt.
		if(isFalling && (OBST_STATE_FALLING != (getObstState() & OBST_STATE_FALLING))) {
			Debug.out.println("Bot "+this.getName()+" faellt in ein Loch!");
			setObstState(getObstState() | OBST_STATE_FALLING);
		} else if(!isFalling && (OBST_STATE_FALLING == (getObstState() & OBST_STATE_FALLING))) {
			Debug.out.println("Bot "+this.getName()+" faellt nicht mehr!");
			setObstState(getObstState() & ~OBST_STATE_FALLING);
		}

		// Wenn der Bot nicht kollidiert oder ueber einem Abgrund steht Position aktualisieren
		if ((getObstState() & (OBST_STATE_COLLISION | OBST_STATE_FALLING)) == 0 ){
			this.setPosition(new Point3d(newPos));
			this.setHeading(newHeading);
		}
		// Blickrichtung nur aktualisieren, wenn Bot nicht in ein
		// Loch gefallen ist:
		if ((getObstState() & OBST_STATE_FALLING) == 0 ){
			this.setHeading(newHeading);
		}
	}

	/**
	 * Variable, die sich merkt, wann wir zuletzt Daten &uuml;bertragen haben
	 */
	private int lastTransmittedSimulTime =0;

	/** Leite Sensordaten an den Bot weiter */
	private synchronized void transmitSensors() {
		try {
			Command command;
			command = new Command(Command.Code.SENS_IR);
			command.setDataL(((Double)this.irL.getValue()).intValue());
			command.setDataR(((Double)this.irR.getValue()).intValue());
			command.setSeq(this.seq++);
			connection.write(command);

			CommandOutputStream s = connection.createCmdOutStream();
			for (BotComponent<?> c : components)
				c.askForWrite(s);
			s.flush();
			
			command = new Command(Command.Code.SENS_BORDER);
			command.setDataL(((Short)this.borderL.getValue()).intValue());
			command.setDataR(((Short)this.borderR.getValue()).intValue());
			command.setSeq(this.seq++);
			connection.write(command);

			// TODO
			command = new Command(Command.Code.SENS_DOOR);
			command.setDataL(0);
			command.setDataR(0);
			command.setSeq(this.seq++);
			connection.write(command);

			command = new Command(Command.Code.SENS_LDR);
			command.setDataL((Integer)this.lightL.getValue());
			command.setDataR((Integer)this.lightR.getValue());
			command.setSeq(this.seq++);
			connection.write(command);

			command = new Command(Command.Code.SENS_LINE);
			command.setDataL(((Short)this.lineL.getValue()).intValue());
			command.setDataR(((Short)this.lineR.getValue()).intValue());
			command.setSeq(this.seq++);
			connection.write(command);

			if(this.getObstState() != OBST_STATE_NORMAL) {
				this.mouseX = 0;
				this.mouseY = 0;
			}
			command = new Command(Command.Code.SENS_MOUSE);
			command.setDataL(this.mouseX);
			command.setDataR(this.mouseY);
			command.setSeq(this.seq++);
			connection.write(command);

			// TODO: nur fuer real-bot
			command = new Command(Command.Code.SENS_TRANS);
			command.setDataL(0);
			command.setDataR(0);
			command.setSeq(this.seq++);
			connection.write(command);

			Object rc5 = this.rc5.getValue();
			if(rc5 != null) {
				Integer val = (Integer)rc5;
				if(val != 0) {
					command = new Command(Command.Code.SENS_RC5);
					command.setDataL(val);
					command.setDataR(42);
					command.setSeq(seq++);
					connection.write(command);
				}
			}

			// TODO: nur fuer real-bot
			command = new Command(Command.Code.SENS_ERROR);
			command.setDataL(0);
			command.setDataR(0);
			command.setSeq(this.seq++);
			connection.write(command);

			lastTransmittedSimulTime= (int)world.getSimTimeInMs();
			lastTransmittedSimulTime %= 10000;	// Wir haben nur 16 Bit zur verfuegung und 10.000 ist ne nette Zahl ;-)
			command = new Command(Command.Code.DONE);
			command.setDataL(lastTransmittedSimulTime);
			command.setDataR(0);
			command.setSeq(this.seq++);
			connection.write(command);
		} catch (IOException e) {
			lg.severe(e, "Error sending sensor data; dying");
			die();
		}
	}

	/** sendet ein IR-Fernbedienungsklommandoi an den Bot
	 *
	 * @param command RC5-Code des Kommandos
	 */
	public void sendRCCommand(int command){
		// TODO Warning entfernen
		boolean setValue = ((Sensor<Integer>)rc5).setValue(new Integer(command));
		if (!setValue) {
			lg.warn("Kann RC5-Kommando nicht absetzen");
		}
	}

	/**
	 * Wertet ein empfangenes Kommando aus
	 *
	 * @param command Das Kommando
	 */
	public void evaluateCommand(Command command) throws ProtocolException {
		if (command.getDirection() == Command.DIR_REQUEST) {

			switch (command.getCommandCode()) {

			case DONE:
				// nIx zu tun mit diesem Kommando
				break;
			case ACT_SERVO:
//				this.setActServo(command.getDataL());
				break;
			case ACT_DOOR:
//				this.setActDoor(command.getDataL());
				break;
			case ACT_MOT:
			case ACT_LED:
			case ACT_LCD:
			case LOG:
				for (BotComponent<?> c : components)
					c.offerRead(command);
				break;
			case SENS_MOUSE_PICTURE:
//				// Empfangen eine Bildes
//				setMousePicture(command.getDataL(),command.getDataBytes());
				break;

			case WELCOME:
				if (! command.has(Command.SubCode.WELCOME_SIM)) {
					lg.severe("Ich bin kein Sim-Bot! Sterbe vor Schreck ;-)");
					die();
				}
				break;

			default:
				lg.warn("Unbekanntes Kommando '%s'", command.toString());
				break;
			}
		} else {
			// TODO: Antworten werden noch nicht gegeben
		}
	}

	@Override
	protected void work() {
		transmitSensors();
		receiveCommands();
		processCommands();
	}

	/**
	 * Hier erfolgt die Aktualisierung der gesamten Simulation
	 *
	 * @see ctSim.model.AliveObstacle#updateSimulation(long)
	 * @param simulTime
	 */
	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime); // Da drin auch Alt-Sensoren-Updates (eigentl. Simulation)
		for (Runnable sim : simulators)
			sim.run();
		// TODO Diese Funktion hat hier rein gar nix zu suchen und muss nach ctbotsim
		calcPos();
	}

	/** Sichert ein Kommando im Puffer */
	public int storeCommand(Command command) {
		int result=0;
		synchronized (commandBuffer) {
			commandBuffer.add(command);
			// Das DONE-kommando ist das letzte in einem Datensatz und beendet ein Paket
			if (command.has(Command.Code.DONE)) {
				if (command.getDataL() == lastTransmittedSimulTime)
					result = 1;
			}
		}
		return result;
	}

	/** Verarbeitet alle eingegangenen Daten */
	public void processCommands(){
		synchronized (commandBuffer) {
			Iterator<Command> it = commandBuffer.iterator();
			while (it.hasNext()){
				Command command = it.next();
				try {
					evaluateCommand(command);
				} catch (ProtocolException e) {
					lg.warning(e, "Fehler beim Verarbeiten eines Kommandos");
				}
			}
			commandBuffer.clear();
		}
	}

	public void receiveCommands() {
		int run = 0; //$$ Variable stinkt
		while (run == 0) {
			try {
				try {
					run = storeCommand(new Command(connection));
				} catch (ProtocolException e) {
					lg.warn(e, "Ungu\00FCltiges Kommando; ignoriere");
				}
			} catch (IOException e) {
				lg.severe(e, "Verbindung unterbrochen -- Bot stirbt"); //$$ Wie, "der stirbt"? Wo ist der die()-Aufruf?
				setHalted(true);
				run = -1;
			}
		}
	}

	/** Erweitert die() um das Schliessen der TCP-Verbindung */
	@Override
	public void die() {
		super.die();
		try {
			connection.close();
		} catch (IOException e) {
			// uninteressant
		}
	}

	//$$ Unterschied cleanup() und die()? Zusammenfassen
	@Override
	protected void cleanup() {
		super.cleanup();
		world = null;
	}
}