package ctSim.model.bots.ctbot;

import java.io.IOException;

import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import ctSim.Connection;
import ctSim.ErrorHandler;
import ctSim.SimUtils;
import ctSim.TcpConnection;
import ctSim.model.Command;
import ctSim.model.World;
import ctSim.model.bots.AnsweringMachine;
import ctSim.model.bots.TcpBot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.ctbot.components.BorderSensor;
import ctSim.model.bots.ctbot.components.DistanceSensor;
import ctSim.model.bots.ctbot.components.EncoderSensor;
import ctSim.model.bots.ctbot.components.Governor;
import ctSim.model.bots.ctbot.components.LightSensor;
import ctSim.model.bots.ctbot.components.LineSensor;

public class CtBotSimTcp extends CtBotSim implements TcpBot {
	
	/** Die TCP-Verbindung */
	private TcpConnection connection;

	/** Der "Anrufbeantworter" fuer eingehende Kommandos */
	private AnsweringMachine answeringMachine;
	
	/** Sequenznummer der TCP-Pakete */
	int seq = 0;
	
	/** maximale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MAX = 255;
	
	/** maximale Geschwindigkeit in Umdrehungen pro Sekunde */
	public static final float UPS_MAX = (float) 151 / (float) 60;
	
	/** Umfang eines Rades [m] */
	public static final double WHEEL_PERIMETER = Math.PI * 0.059d;

	/** Abstand Mittelpunkt Bot zum Rad [m] */
	public static final double WHEEL_DIST = 0.0485d;
	
	/**
	 * Interne Zeitbasis in Millisekunden -- Zeitaenderung seit letztem
	 * Simulationschritt
	 */
	protected long deltaT = 10;
	
	
	private int first = 0;
	/* **********************************************************************
	 * **********************************************************************
	 * 
	 */
	
	// TODO: weg
	private World world;
	
	private Sensor irL, irR, lineL, lineR, borderL, borderR, lightL, lightR, encL, encR;
	
	private Actuator govL, govR;
	
	public CtBotSimTcp(World world, String name, Point3d pos, Vector3d head, Connection con) {
		super(world, name, pos, head);
		
		this.connection = (TcpConnection)con;
		this.answeringMachine = new AnsweringMachine(this, con);
		
		this.world = world;
		
		initActuators();
		initSensors();
	}
	
	private void initSensors() {
		
		this.encL = new EncoderSensor(this.world, this, "EncL", new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d), this.govL);
		this.encR = new EncoderSensor(this.world, this, "EncR", new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d), this.govR);
		
		this.irL = new DistanceSensor(this.world, this, "IrL", new Point3d(-0.036d, 0.0554d, 0.035d-BOT_HEIGHT/2), new Vector3d(0d, 1d, 0d));
		this.irR = new DistanceSensor(this.world, this, "IrR", new Point3d(0.036d, 0.0554d, 0.035d-BOT_HEIGHT/2), new Vector3d(0d, 1d, 0d));
		
		this.lineL = new LineSensor(this.world, this, "LineL", new Point3d(-0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d));
		this.lineR = new LineSensor(this.world, this, "LineR", new Point3d(0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d));
		
		this.borderL = new BorderSensor(this.world, this, "BorderL", new Point3d(-0.036d, 0.0384d, 0d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d));
		this.borderR = new BorderSensor(this.world, this, "BorderR", new Point3d(0.036d, 0.0384d, 0d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d));
		
		this.lightL = new LightSensor(this.world, this, "LightL", new Point3d(-0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d));
		this.lightR = new LightSensor(this.world, this, "LightR", new Point3d(0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d));
		
		this.addSensor(this.encL);
		this.addSensor(this.encR);
		
		this.addSensor(this.irL);
		this.addSensor(this.irR);
		
		// new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d)); // 
		this.addSensor(this.lineL);
		this.addSensor(this.lineR);
		
		this.addSensor(this.borderL);
		this.addSensor(this.borderR);
		
		this.addSensor(this.lightL);
		this.addSensor(this.lightR);
	}
	
	private void initActuators() {
		
		this.govL = new Governor("GovL", new Point3d(), new Vector3d(0d, 1d, 0d));
		this.govR = new Governor("GovR", new Point3d(), new Vector3d(0d, 1d, 0d));
		
		this.addActuator(this.govL);
		this.addActuator(this.govR);
	}
	
	/**
	 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde
	 * 
	 * @param motPWM PWM-Verhaeltnis 
	 * @return Umdrehungen pro Sekunde
	 */
	private float calculateWheelSpeed(int motPWM) {
		float tmp = ((float) motPWM / (float) PWM_MAX);
		tmp = tmp * UPS_MAX;
		return tmp;
		// TODO Die Kennlinien der echten Motoren ist nicht linear
	}
	
	/**
	 * Leite Sensordaten an den Bot weiter
	 */
	private synchronized void transmitSensors() {
		try {
			Command command = new Command();
			command.setCommand(Command.CMD_SENS_IR);
			command.setDataL(((Double)this.irL.getValue()).intValue());
			command.setDataR(((Double)this.irL.getValue()).intValue());
			command.setSeq(seq++);
			this.connection.send(command.getCommandBytes());
			
			command.setCommand(Command.CMD_SENS_ENC);
			command.setDataL((Integer)this.encL.getValue());
			command.setDataR((Integer)this.encR.getValue());
			// TODO: �berfl�ssig?
//			setSensEncL((short) 0);
//			setSensEncR((short) 0);
			command.setSeq(seq++);
			this.connection.send(command.getCommandBytes());
			
			command.setCommand(Command.CMD_SENS_BORDER);
			command.setDataL(((Short)this.borderL.getValue()).intValue());
			command.setDataR(((Short)this.borderR.getValue()).intValue());
			command.setSeq(seq++);
			this.connection.send(command.getCommandBytes());
			
//			command.setCommand(Command.CMD_SENS_DOOR);
//			command.setDataL(getSensDoor());
//			command.setDataR(0);
//			command.setSeq(seq++);
//			this.connection.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_LDR);
			command.setDataL((Integer)this.lightL.getValue());
			command.setDataR((Integer)this.lightR.getValue());
			command.setSeq(seq++);
			this.connection.send(command.getCommandBytes());
			
			command.setCommand(Command.CMD_SENS_LINE);
			command.setDataL(((Short)this.lineL.getValue()).intValue());
			command.setDataR(((Short)this.lineR.getValue()).intValue());
			command.setSeq(seq++);
			this.connection.send(command.getCommandBytes());
			
//			command.setCommand(Command.CMD_SENS_MOUSE);
//			command.setDataL(getSensMouseDX());
//			command.setDataR(getSensMouseDY());
//			command.setSeq(seq++);
//			this.connection.send(command.getCommandBytes());
//
//			command.setCommand(Command.CMD_SENS_TRANS);
//			command.setDataL(getSensTrans());
//			command.setDataR(0);
//			command.setSeq(seq++);
//			this.connection.send(command.getCommandBytes());
//
//			
//			if (getSensRc5() != 0) {
//				command.setCommand(Command.CMD_SENS_RC5);
//				command.setDataL(getSensRc5());
//				command.setDataR(42);
//				command.setSeq(seq++);
//				tcpCon.send(command.getCommandBytes());
//				setSensRc5(0);
//			}
			
			// TODO: sehr h�sslich: Bot-Verhalten "Wandverfolgung" starten
			first++;
			if(first==5) {
				
				int RC5_CODE_5 = 0x3945;
				command.setCommand(Command.CMD_SENS_RC5);
				command.setDataL(RC5_CODE_5);
				command.setDataR(42);
				command.setSeq(seq++);
				this.connection.send(command.getCommandBytes());
				//setSensRc5(0);
			}
//
//			command.setCommand(Command.CMD_SENS_ERROR);
//			command.setDataL(getSensError());
//			command.setDataR(0);
//			command.setSeq(seq++);
//			this.connection.send(command.getCommandBytes());
			
			
			////////////////////////////////////////////////////////////////////
			////////////////////////////////////////////////////////////////////
			// Position und Heading berechnen:
			
			// Anzahl der Umdrehungen der Raeder
			double turnsL = calculateWheelSpeed((Integer)this.govL.getValue());
			turnsL = turnsL * deltaT / 1000.0f;
			double turnsR = calculateWheelSpeed((Integer)this.govR.getValue());
			turnsR = turnsR * deltaT / 1000.0f;
			
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
			Vector3d _hd = this.getHeading();
			double _s2g = Math.sin(2 * _gamma);
			double _c2g = Math.cos(2 * _gamma);
			Vector3d newHeading = new Vector3d(
					(double) (_hd.x * _c2g + _hd.y * _s2g),
					(double) (-_hd.x * _s2g + _hd.y * _c2g), 0f);

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
			Vector3d moveDirection = new Vector3d((double) (_hd.x * _cg + _hd.y
					* _sg), (double) (-_hd.x * _sg + _hd.y * _cg), 0f);
			moveDirection.normalize();
			moveDirection.scale((double) moveDistance);
			// ... und die alte Position entsprechend veraendern.
			newPos.add(moveDirection);
			
			// TODO: Pos nur setzen, wenn Status o.k. -> s.u.
			this.setPosition(new Point3d(newPos));
			this.setHeading(newHeading);
			
			System.out.println(" -->  "+newPos+"   |   "+newHeading);
			
			
//			int oldState =getObstState(); 
//			// Pruefen, ob Kollision erfolgt. Bei einer Kollision wird
//			// der Bot blau gefaerbt.
//			if (getWorld().checkCollision((Shape3D)getNodeReference(BOTBODY), getBounds(), newPos, getName())) {
//				// Wenn nicht, Position aktualisieren
//				this.setPos(newPos);
//				// Zustand setzen
//				setObstState( oldState & ~OBST_STATE_COLLISION);
//			} else {
//				moveDistance = 0; // Wird spaeter noch fuer den Maussensor benoetigt
//				
//				// Zustand setzen
//				setObstState( oldState| OBST_STATE_COLLISION);
//			}
//			
//			
//			// Bodenkontakt ueberpruefen
//
//			// Vektor vom Ursprung zum linken Rad
//			Vector3f vecL = new Vector3f(newHeading.y,newHeading.x, 0f);
//			vecL.scale((float) WHEEL_DIST);
//			// neue Position linkes Rad
//			Vector3f posRadL = new Vector3f(this.getPos());
//			posRadL.add(vecL);
//
//			// Vektor vom Ursprung zum rechten Rad
//			Vector3f vecR = new Vector3f(newHeading.y, -newHeading.x, 0f);
//			vecR.scale((float) WHEEL_DIST);
//			// neue Position rechtes Rad
//			Vector3f posRadR = new Vector3f(this.getPos());
//			posRadR.add(vecR);
//
//			// Winkel des heading errechnen
//			double angle = SimUtils.getRotation(newHeading);
//			// Transformations-Matrix fuer die Rotation erstellen
//			Transform3D rotation = new Transform3D();
//			rotation.rotZ(angle);
//
//			// Bodenkontakt des Gleitpins ueberpruefen
//			Vector3d skidVec = new Vector3d(BOT_SKID_X, BOT_SKID_Y, -BOT_HEIGHT / 2);
//			// Position des Gleitpins gemaess der Ausrichtung des Bots anpassen
//			rotation.transform(skidVec);
//			skidVec.add(new Point3d(newPos));
//			
//			boolean isFalling = false;
//			if (!getWorld().checkTerrain(new Point3d(skidVec), BOT_GROUND_CLEARANCE,
//					"Der Gleitpin von " + this.getName())) {
//				isFalling = true;
//			}
//
//			// Bodenkontakt des linken Reifens ueberpruefen
//			posRadL.z -= BOT_HEIGHT / 2;
//			if (!getWorld().checkTerrain(new Point3d(posRadL), BOT_GROUND_CLEARANCE,
//					"Das linke Rad von " + this.getName())) {
//				isFalling = true;
//			}
//
//			// Bodenkontakt des rechten Reifens ueberpruefen
//			posRadR.z -= BOT_HEIGHT / 2;
//			if (!getWorld().checkTerrain(new Point3d(posRadR), BOT_GROUND_CLEARANCE,
//					"Das rechte Rad von " + this.getName())) {
//				isFalling = true;
//			}
//
//			// Wenn einer der Beruehrungspunkte keinen Boden mehr unter sich hat,
//			// wird der Bot gestoppt und gruen gefaerbt.
//			if (isFalling) 
//				setObstState(getObstState() | OBST_STATE_FALLING);
//			else 
//				setObstState(getObstState() & ~OBST_STATE_FALLING);
//			
//			// Update Appearance
//			int newState=getObstState();
//			if (newState == oldState){
//			} else if (newState == OBST_STATE_NORMAL)
//				setAppearance("normal");
//			else if ((newState & OBST_STATE_FALLING) != 0)
//				setAppearance("falling");
//			else if ((newState & OBST_STATE_COLLISION) != 0)
//				setAppearance("collision");

		} catch (IOException IoEx) {
			ErrorHandler.error("Error during sending Sensor data, dieing: "
					+ IoEx);
			die();
		}
	}

	/**
	 * Wertet ein empfangenes Kommando aus
	 * 
	 * @param command
	 *            Das Kommando
	 */
	public void evaluate_command(Command command) {
		Command answer = new Command();

		if (command.getDirection() == Command.DIR_REQUEST) {
			// Antwort vorbereiten
			answer.setDirection(Command.DIR_ANSWER);
			answer.setCommand(command.getCommand());
			answer.setSubcommand(command.getSubcommand());
			answer.setSeq(command.getSeq());

			switch (command.getCommand()) {
			case Command.CMD_SENS_IR:
//				setSensIrL( ((double)command.getDataL())/1000);
//				setSensIrR( ((double)command.getDataR())/1000);
//				break;
			case Command.CMD_SENS_ENC:
//				answer.setDataL(this.getSensEncL());
//				this.setSensEncL((short) 0); // nach Uebertragung aufraeumen
//				answer.setDataR(this.getSensEncR());
//				this.setSensEncR((short) 0); // nach Uebertragung aufraeumen
//				break;
			case Command.CMD_SENS_BORDER:
//				answer.setDataL(this.getSensBorderL());
//				answer.setDataR(this.getSensBorderR());
//				break;
			case Command.CMD_SENS_DOOR:
//				answer.setDataL(this.getSensDoor());
//				answer.setDataR(0);
//				break;
			case Command.CMD_SENS_LDR:
//				answer.setDataL(this.getSensLdrL());
//				answer.setDataR(this.getSensLdrR());
//				break;
			case Command.CMD_SENS_LINE:
//				answer.setDataL(this.getSensLineL());
//				answer.setDataR(this.getSensLineR());
//				break;
			case Command.CMD_SENS_MOUSE:
//				answer.setDataL(this.getSensMouseDX());
//				answer.setDataR(this.getSensMouseDY());
//				break;
			case Command.CMD_SENS_TRANS:
//				answer.setDataL(this.getSensTrans());
//				answer.setDataR(0);
//				break;
			case Command.CMD_SENS_RC5:
//				answer.setDataL(this.getSensRc5());
//				this.setSensRc5(0); // nicht zweimal lesen
//				answer.setDataR(0);
//				break;
			case Command.CMD_SENS_ERROR:
//				answer.setDataL(this.getSensError());
//				answer.setDataR(0);
				break;

			case Command.CMD_AKT_MOT:
				//this.setActMotL((short) command.getDataL());
				//this.setActMotR((short) command.getDataR());
				this.govL.setValue(command.getDataL());
				this.govR.setValue(command.getDataR());
				System.out.println("Angekommen");
				System.exit(-1);
				break;
			case Command.CMD_AKT_SERVO:
//				this.setActServo(command.getDataL());
//				break;
			case Command.CMD_AKT_DOOR:
//				this.setActDoor(command.getDataL());
//				break;
			case Command.CMD_AKT_LED:
				command.getDataL();
//				this.setActLed(command.getDataL());
				break;
			case Command.CMD_ACT_LCD:
				switch (command.getSubcommand()) {
				case Command.SUB_CMD_NORM:
//					this.setLcdText(command.getDataL(), command.getDataR(),
//							command.getDataBytesAsString());
//					break;
				case Command.SUB_LCD_CURSOR:
//					this.setCursor(command.getDataL(), command.getDataR());
//					break;
				case Command.SUB_LCD_CLEAR:
//					this.lcdClear();
//					break;
				case Command.SUB_LCD_DATA:
//					this.setLcdText(command.getDataBytesAsString());
					break;
				}
				break;
			case Command.CMD_LOG:
//				this.setLog(command.getDataBytesAsString());
//				break;
//				
			case Command.CMD_SENS_MOUSE_PICTURE:
//				// Empfangen eine Bildes
//				setMousePicture(command.getDataL(),command.getDataBytes());
				break;
				
			case Command.CMD_WELCOME:
				if (command.getSubcommand() != Command.SUB_WELCOME_SIM){
					ErrorHandler.error("Ich bin kein Sim-Bot! Sterbe vor Schreck ;-)");
					die();
				}
				break;
				
			default:
				ErrorHandler.error("Unknown Command:" + command.toString());
				break;
			}
			System.out.println("////////////////////////////////////////////////////////////////");
			System.out.println("Command: "+command);
			
			try {
				// tcpCon.send(answer.getCommandBytes());
			} catch (Exception ex) {
				ErrorHandler.error("Sending answer failed");
			}

		} else {
			// TODO: Antworten werden noch nicht gegeben
		}
	}
	
	@Override
	protected void init() {
		
		this.answeringMachine.start();
	}
	
	@Override
	protected void work() {
		
		super.work();
		transmitSensors();
	}
}