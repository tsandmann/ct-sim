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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.Connection;
import ctSim.SimUtils;
import ctSim.TcpConnection;
import ctSim.model.Command;
import ctSim.model.World;
import ctSim.model.bots.AnsweringMachine;
import ctSim.model.bots.TcpBot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Characteristic;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.components.actuators.Display;
import ctSim.model.bots.components.actuators.Indicator;
import ctSim.model.bots.components.actuators.LogScreen;
import ctSim.model.bots.components.sensors.SimpleSensor;
import ctSim.model.bots.ctbot.components.BorderSensor;
import ctSim.model.bots.ctbot.components.DistanceSensor;
import ctSim.model.bots.ctbot.components.EncoderSensor;
import ctSim.model.bots.ctbot.components.Governor;
import ctSim.model.bots.ctbot.components.LightSensor;
import ctSim.model.bots.ctbot.components.LineSensor;
import ctSim.model.bots.ctbot.components.RemoteControlSensor;
import ctSim.util.FmtLogger;
import ctSim.view.gui.Debug;

/**
 * Klasse aller simulierten c't-Bots, die ueber TCP mit dem Simulator kommunizieren
 *
 */
public class CtBotSimTcp extends CtBotSim implements TcpBot {
	FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.ctbot.CtBotSimTcp");

	/** Die TCP-Verbindung */
	private TcpConnection connection;

	/** Der "Anrufbeantworter" fuer eingehende Kommandos */
	private AnsweringMachine answeringMachine;

	private ArrayList<Command> commandBuffer = new ArrayList<Command>();

	/** Sequenznummer der TCP-Pakete */
	private int seq = 0;

	/** Puffer fuer Logausgaben */
	public StringBuffer logBuffer = new StringBuffer(""); //$NON-NLS-1$

	/** Zustand der LEDs */
	private Integer actLed = new Integer(0);

	/** Anzahl der Zeilen im LCD */
	public static final short LCD_LINES = 4;

	/** Anzahl der Zeichen pro Zeile im LCD */
	public static final short LCD_CHARS = 20;

	/** Zustand des LCD */
	private String[] lcdText = new String[LCD_LINES];

	/** Cursorposition des LCD
	 * X : vor welchem Zeichen steht der Cursor (0 .. LCD_CHARS-1)
	 * Y : in welcher Zeile steht der Cursor (0 .. LCD_LINES-1)
	 */
	private int lcdCursorX = 0;

	private int lcdCursorY = 0;

	/** maximale Geschwindigkeit als PWM-Wert */
	public static final short PWM_MAX = 255;

	/** maximale Geschwindigkeit in Umdrehungen pro Sekunde */
	public static final float UPS_MAX = (float) 151 / (float) 60;

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

	private Sensor irL, irR, lineL, lineR, borderL, borderR, lightL, lightR, encL, encR, rc5;

	private Actuator govL, govR, log, disp;

	/**
	 * Der Konstruktor
	 * @param w Die Welt
	 * @param name Der Name des Bot
	 * @param pos Position
	 * @param head Blickrichtung
	 * @param con Verbindung
	 */
	public CtBotSimTcp(World w, String name, Point3d pos, Vector3d head, Connection con) {
		super(w, name, pos, head);

		this.connection = (TcpConnection)con;

		this.world = w;

		initActuators();
		initSensors();
	}


	private void initSensors() {

		this.encL = new EncoderSensor(this.world, this, "EncL", new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d), this.govL); //$NON-NLS-1$
		this.encR = new EncoderSensor(this.world, this, "EncR", new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d), this.govR); //$NON-NLS-1$

		this.irL = new DistanceSensor(this.world, this, "IrL", new Point3d(-0.036d, 0.0554d, 0d ), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.irR = new DistanceSensor(this.world, this, "IrR", new Point3d(0.036d, 0.0554d, 0d), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.irL.setCharacteristic(new Characteristic(new File("characteristics/gp2d12Left.txt"), 100f)); //$NON-NLS-1$
		this.irR.setCharacteristic(new Characteristic(new File("characteristics/gp2d12Right.txt"), 80f)); //$NON-NLS-1$

		this.lineL = new LineSensor(this.world, this, "LineL", new Point3d(-0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.lineR = new LineSensor(this.world, this, "LineR", new Point3d(0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$

		this.borderL = new BorderSensor(this.world, this, "BorderL", new Point3d(-0.036d, 0.0384d, 0d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.borderR = new BorderSensor(this.world, this, "BorderR", new Point3d(0.036d, 0.0384d, 0d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$

		this.lightL = new LightSensor(this.world, this, "LightL", new Point3d(-0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.lightR = new LightSensor(this.world, this, "LightR", new Point3d(0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$

		this.rc5 = new RemoteControlSensor("RC fuer '"+this.getName()+"'", new Point3d(), new Vector3d()); //$NON-NLS-1$

		this.addSensor(this.encL);
		this.addSensor(this.encR);

		this.addSensor(this.irL);
		this.addSensor(this.irR);

		this.addSensor(this.lineL);
		this.addSensor(this.lineR);

		this.addSensor(this.borderL);
		this.addSensor(this.borderR);

		this.addSensor(this.lightL);
		this.addSensor(this.lightR);

		this.addSensor(this.rc5);

		this.addSensor(new SimpleSensor<Integer>("MouseX", new Point3d(), new Vector3d()) { //$NON-NLS-1$

			@SuppressWarnings({"synthetic-access","boxing"})
			@Override
			public Integer updateValue() {

				return CtBotSimTcp.this.mouseX;
			}

			@Override
			public String getType() {

				return "Maus-Sensor"; //$NON-NLS-1$
			}

			@Override
			public String getDescription() {

				return "Maus-Sensor-Wert X"; //$NON-NLS-1$
			}

			@Override
			public Shape3D getShape() {

				return new Shape3D();
			}
		});

		this.addSensor(new SimpleSensor<Integer>("MouseY", new Point3d(), new Vector3d()) { //$NON-NLS-1$

			@SuppressWarnings({"synthetic-access","boxing"})
			@Override
			public Integer updateValue() {

				return CtBotSimTcp.this.mouseY;
			}

			@Override
			public String getType() {

				return "Maus-Sensor"; //$NON-NLS-1$
			}

			@Override
			public String getDescription() {

				return "Maus-Sensor-Wert Y"; //$NON-NLS-1$
			}

			@Override
			public Shape3D getShape() {

				return new Shape3D();
			}
		});


	}

	private void initActuators() {

		// !! cols.length == colsAct.length !!
		int ledCount = cols.length;

		// LEDs:
		for(int i=0; i<ledCount; i++) {

			final Integer idx = new Integer(ledCount-i-1);

			this.addActuator(
					new Indicator("LED "+i, new Point3d(), new Vector3d(), cols[i], colsAct[i]) { //$NON-NLS-1$

						@Override
						public void setValue(@SuppressWarnings("unused") Boolean value) {

							// TODO: ???
						}

						@SuppressWarnings({"synthetic-access","boxing"})
						@Override
						public Boolean getValue() {

							int soll = (int)Math.pow(2, idx);
							int ist = CtBotSimTcp.this.actLed & soll; // Bitweises "und"

							return (soll == ist);
						}
					}
			);
		}

		this.govL = new Governor("GovL", new Point3d(), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.govR = new Governor("GovR", new Point3d(), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$

		this.disp = new Display("Display", new Point3d(), new Vector3d()); //$NON-NLS-1$
		this.log  = new LogScreen("LogScreen", new Point3d(), new Vector3d()); //$NON-NLS-1$

		this.addActuator(this.govL);
		this.addActuator(this.govR);

		this.addActuator(this.disp);
		this.addActuator(this.log);
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

	/**
	 * Variable, die sich merkt, welche Daten wir zueltzt uebertragen haben
	 */
	private int lastTransmittedSimulTime =0;

	/**
	 * Sendbuffer fuer transmitSensors()
	 */
	private ArrayList<Byte> commandsToSend = new ArrayList<Byte>();

	/**
	 * Leite Sensordaten an den Bot weiter
	 */
	@SuppressWarnings({"unchecked","boxing"})
	private synchronized void transmitSensors() {
		try {
			commandsToSend.clear();	// Sendbuffer loeschen
			byte tmp[];	// Buffer pro Command

			Command command = new Command();
			command.setCommand(Command.CMD_SENS_IR);
			command.setDataL(((Double)this.irL.getValue()).intValue());
			command.setDataR(((Double)this.irR.getValue()).intValue());
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			command.setCommand(Command.CMD_SENS_ENC);
			command.setDataL((Integer)this.encL.getValue());
			command.setDataR((Integer)this.encR.getValue());
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			command.setCommand(Command.CMD_SENS_BORDER);
			command.setDataL(((Short)this.borderL.getValue()).intValue());
			command.setDataR(((Short)this.borderR.getValue()).intValue());
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			// TODO
			command.setCommand(Command.CMD_SENS_DOOR);
			command.setDataL(0);
			command.setDataR(0);
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			command.setCommand(Command.CMD_SENS_LDR);
			command.setDataL((Integer)this.lightL.getValue());
			command.setDataR((Integer)this.lightR.getValue());
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			command.setCommand(Command.CMD_SENS_LINE);
			command.setDataL(((Short)this.lineL.getValue()).intValue());
			command.setDataR(((Short)this.lineR.getValue()).intValue());
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			if(this.getObstState() != OBST_STATE_NORMAL) {
				this.mouseX = 0;
				this.mouseY = 0;
			}
			command.setCommand(Command.CMD_SENS_MOUSE);
			command.setDataL(this.mouseX);
			command.setDataR(this.mouseY);
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			// TODO: nur fuer real-bot
			command.setCommand(Command.CMD_SENS_TRANS);
			command.setDataL(0);
			command.setDataR(0);
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			Object rc5 = this.rc5.getValue();
			if(rc5 != null) {

				Integer val = (Integer)rc5;

				if(val != 0) {
					command.setCommand(Command.CMD_SENS_RC5);
					command.setDataL(val);
					command.setDataR(42);
					command.setSeq(seq++);
					tmp = command.getCommandBytes();
					for (int i=0; i<tmp.length; i++)
						commandsToSend.add(tmp[i]);
				}
			}

			// TODO: nur fuer real-bot
			command.setCommand(Command.CMD_SENS_ERROR);
			command.setDataL(0);
			command.setDataR(0);
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			lastTransmittedSimulTime= (int)world.getSimTimeInMs();
			lastTransmittedSimulTime %= 10000;	// Wir haben nur 16 Bit zur verfuegung und 10.000 ist ne nette Zahl ;-)
			command.setCommand(Command.CMD_DONE);
			command.setDataL(lastTransmittedSimulTime);
			command.setDataR(0);
			command.setSeq(this.seq++);
			tmp = command.getCommandBytes();
			for (int i=0; i<tmp.length; i++)
				commandsToSend.add(tmp[i]);

			byte toSend[] = new byte[commandsToSend.size()];
			for (int i=0; i<commandsToSend.size(); i++)
				toSend[i] = commandsToSend.get(i);
			this.connection.send(toSend);

		} catch (IOException e) {
			lg.severe(e, "Error sending sensor data; dying");
			die();
		}
	}

	/** sendet ein IR-Fernbedienungsklommandoi an den Bot
	 *
	 * @param command RC5-Code des Kommandos
	 */
	@SuppressWarnings("unchecked")
	public void sendRCCommand(int command){
		// TODO Warning entfernen
		boolean setValue = this.rc5.setValue((Object)(new Integer(command)));
		if (!setValue) {
			lg.warn("Kann RC5-Kommando nicht absetzen");
		}
	}

	/** Puffer fuer kleine Mausbewegungen */
	private double deltaXRest=0;
	/** Puffer fuer kleine Mausbewegungen */
	private double deltaYRest=0;

	//TODO Bot soll in Loecher reinfahren, in Hindernisse aber nicht. Momentan: calcPos traegt Pos nicht ein, wenn Hindernis oder Loch. Gewuenscht: Bei Loch das erste Mal Pos updaten, alle weiteren Male nicht
	//TODO calcPos umziehen nach Bot oder CtBotSim
	@SuppressWarnings({"unchecked","boxing"})
	private void calcPos() {

			////////////////////////////////////////////////////////////////////
			////////////////////////////////////////////////////////////////////
			// Position und Heading berechnen:

			// Anzahl der Umdrehungen der Raeder
			double turnsL = calculateWheelSpeed((Integer)this.govL.getValue());
			turnsL = turnsL * getDeltaT() / 1000.0f;
			double turnsR = calculateWheelSpeed((Integer)this.govR.getValue());
			turnsR = turnsR * getDeltaT() / 1000.0f;

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
	 * Wertet ein empfangenes Kommando aus
	 *
	 * @param command
	 *            Das Kommando
	 */
	@SuppressWarnings({"unchecked","boxing"})
	public void evaluateCommand(Command command) {
		StringBuffer buf;

		if (command.getDirection() == Command.DIR_REQUEST) {

			switch (command.getCommand()) {

			case Command.CMD_DONE:
				// nIx zu tun mit diesem Kommando
				break;
			case Command.CMD_AKT_MOT:
				this.govL.setValue(command.getDataL());
				this.govR.setValue(command.getDataR());
				break;
			case Command.CMD_AKT_SERVO:
//				this.setActServo(command.getDataL());
				break;
			case Command.CMD_AKT_DOOR:
//				this.setActDoor(command.getDataL());
				break;
			case Command.CMD_AKT_LED:
				this.setActLed(command.getDataL());
				break;
			case Command.CMD_ACT_LCD:
				switch (command.getSubcommand()) {
				case Command.SUB_CMD_NORM:

					this.setLcdText(command.getDataL(), command.getDataR(),
							command.getDataBytesAsString());

					// Neu:
					if(this.lcdText == null || this.lcdText.length == 0) {
						break;
					}

					buf = new StringBuffer();
					buf.append(this.lcdText[0]);

					for(int i=1; i<this.lcdText.length; i++) {

						buf.append("\n"); //$NON-NLS-1$
						buf.append(this.lcdText[i]);
					}

					this.disp.setValue(buf.toString());

					break;
				case Command.SUB_LCD_CURSOR:
					this.setCursor(command.getDataL(), command.getDataR());

					if(this.lcdText == null || this.lcdText.length == 0)
						break;

					buf = new StringBuffer();

					buf.append(this.lcdText[0]);

					for(int i=1; i<this.lcdText.length; i++) {

						buf.append("\n"); //$NON-NLS-1$
						buf.append(this.lcdText[i]);
					}

					this.disp.setValue(buf.toString());

					break;
				case Command.SUB_LCD_CLEAR:
					this.lcdClear();

					if(this.lcdText == null || this.lcdText.length == 0)
						break;

					buf = new StringBuffer();
					buf.append(this.lcdText[0]);

					for(int i=1; i<this.lcdText.length; i++) {

						buf.append("\n"); //$NON-NLS-1$
						buf.append(this.lcdText[i]);
					}

					this.disp.setValue(buf.toString());

					break;
				case Command.SUB_LCD_DATA:
					this.setLcdText(command.getDataBytesAsString());

					// Neu:
					if(this.lcdText == null || this.lcdText.length == 0)
						break;

					buf = new StringBuffer();
					buf.append(this.lcdText[0]);

					for(int i=1; i<this.lcdText.length; i++) {

						buf.append("\n"); //$NON-NLS-1$
						buf.append(this.lcdText[i]);
					}

					this.disp.setValue(buf.toString());

					break;
				}
				break;
			case Command.CMD_LOG:
				this.setLog(command.getDataBytesAsString());

				this.log.setValue(this.getLog().toString());

				break;
			case Command.CMD_SENS_MOUSE_PICTURE:
//				// Empfangen eine Bildes
//				setMousePicture(command.getDataL(),command.getDataBytes());
				break;

			case Command.CMD_WELCOME:
				if (command.getSubcommand() != Command.SUB_WELCOME_SIM){
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
	protected void init() {

		if (answeringMachine != null)
			this.answeringMachine.start();
	}

	CountDownLatch waitForCommands = new CountDownLatch(1);

	@Override
	protected void work() {

		transmitSensors();
		receiveCommands();
		processCommands();
	}

	/**
	 * Schreibt Logausgabe in den Puffer.
	 * @param str String fuer die Ausgabe
	 */
	public void setLog(String str) {
		synchronized(this.logBuffer) {
			this.logBuffer.append(str + "\n"); //$NON-NLS-1$
		}
	}

	/**
	 * Liefert Puffer fuer die Logausgabe.
	 * @return Logausgabe
	 */
	public StringBuffer getLog() {
		StringBuffer tempBuffer = new StringBuffer(""); //$NON-NLS-1$

		synchronized(this.logBuffer) {

			/* Puffer kopieren und leeren*/
			tempBuffer.append(this.logBuffer.toString());
			this.logBuffer.delete(0, this.logBuffer.length());
		}

		return tempBuffer;
	}

	/**
	 * Setzt Text an eine bestimmte Position im LCD.
	 *
	 * @param charPos Neue Cursorposition (Spalte 0..19)
	 * @param linePos Neue Cursorposition (Zeile 0..3)
	 * @param text    Der Text, der ab der neuen Cursorposition einzutragen ist
	 */
	public void setLcdText(int charPos, int linePos, String text) {
		setCursor(charPos, linePos);
		{
			String pre = ""; //$NON-NLS-1$
			String post = ""; //$NON-NLS-1$
			int max = Math.min(text.length(), LCD_CHARS - this.lcdCursorX - 1);

			// Der neue Zeilentext ist der alte bis zur Cursorposition, gefolgt
			// vom uebergebenen String 'text' gefolgt von den nicht ueberschriebenen Zeichen,
			// wenn sich die neue X-Position noch vor dem Zeilenende befindet.
			if (this.lcdCursorX > 0) {
				pre = new String(this.lcdText[this.lcdCursorY].substring(0,
						this.lcdCursorX - 1));
			}
			this.lcdCursorX += max;
			if (this.lcdCursorX < LCD_CHARS - 1) {
				post = new String(this.lcdText[this.lcdCursorY].substring(this.lcdCursorX));
			}
			synchronized (this.lcdText) {
				this.lcdText[this.lcdCursorY] = new String(pre + text + post);
			}
		}
	}

	/**
	 * Setzt Text in eine bestimmte Zeile im LCD.
	 *
	 * @param linePos Neue Cursorposition (Zeile 0..3)
	 * @param text    Der Text, der ab der neuen Cursorposition einzutragen ist
	 */
	public void setLcdText(int linePos, String text) {
		setLcdText(0, linePos, text);
	}

	/**
	 * Setzt Text ins LCD.
	 *
	 * @param text Der Text, der ab der neuen Cursorposition einzutragen ist
	 */
	public void setLcdText(String text) {
		setLcdText(this.lcdCursorX, this.lcdCursorY, text);
	}

	/**
	 * Setzt den Cursor an eine bestimmte Position im LCD
	 * @param charPos Neue Cursorposition (Spalte 0..19)
	 * @param linePos Neue Cursorposition (Zeile 0..3)
	 */
	public void setCursor(int charPos, int linePos) {
		if (charPos < 0) {
			charPos = 0;
		}
		if (charPos > LCD_CHARS - 1) {
			charPos = LCD_CHARS - 1;
		}
		if (linePos < 0) {
			linePos = 0;
		}
		if (linePos > LCD_LINES - 1) {
			linePos = LCD_LINES - 1;
		}

		this.lcdCursorX = charPos;
		this.lcdCursorY = linePos;
	}

	/**
	 * Loesche das Display
	 *
	 */
	public void lcdClear() {
		synchronized (this.lcdText) {
			for (int i = 0; i < this.lcdText.length; i++) {
				this.lcdText[i] = new String("                    "); //$NON-NLS-1$
			}
		}
	}

	/**
	*  Hier erfolgt die Aktualisierung der gesamten Simualtion
	* @see ctSim.model.AliveObstacle#updateSimulation(long)
	* @param simulTime
	*/
	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime);
		// TODO Diese Funktion hat hier rein gar nix zu suchen und muss nach ctbotsim
		calcPos();
	}

	/**
	 * Sichert ein Kommando im Puffer
	 * @param command Das Komamndo
	 */
	public int storeCommand(Command command) {
		int result=0;
		synchronized (commandBuffer) {
			// 	TODO verhidnern, dass teilpakete ankommen!!! Achtun geht nicht ohne Aenderungen mit dem C-Code

//			System.out.println("Put CMD: "+command.getCommand()+" DataL: "+command.getDataL()+" Seq: "+command.getSeq());

			commandBuffer.add(command);
			//System.out.println(command.toString());
		//	System.out.println("Command: "+(char)command.getCommand()+"  -  "+(char)command.getSubcommand());
				// Das DONE-kommando ist das letzte in einem Datensatz und beendet ein Paket
			if (command.getCommand() ==  Command.CMD_DONE){
//				System.out.println(world.getRealTime()+"ms: received Frame for "+command.getDataL()+" ms - expected "+lastTransmittedSimulTime+" ms");
				if (command.getDataL() == lastTransmittedSimulTime){
//					recvTime=System.nanoTime()/1000;
					result=1;
	//				System.out.println("warten auf Bot: "+(recvTime-sendTime)+" usec");

	//				System.out.println("releasing\n");
					waitForCommands.countDown();
				}
			}
		}
		return result;
	}

	/**
	* Verarbeitet alle eingegangenen Daten
	*/
	public void processCommands(){

		synchronized (commandBuffer) {
//			int i=0;
			Iterator<Command> it = commandBuffer.iterator();
//			System.out.println(commandBuffer.size()+" Elemente im Puffer");
			while (it.hasNext()){
				Command command = it.next();
//				System.out.println("GET("+(i++)+") CMD: "+command.getCommand()+" DataL: "+command.getDataL()+" Seq: "+command.getSeq());
				evaluateCommand(command);
			}
			commandBuffer.clear();
			// resete Signal
//			waitForCommands= new CountDownLatch(1);
		}

	}

	//long t1, t2;
	public void receiveCommands() {
//		long start, duration;
		int valid = 0;
		int run=0;


		//t1= System.nanoTime()/1000;

//		long aussen= t1-t2;

		while (run==0) {
			try {
				Command command = new Command();
//				start= System.nanoTime();
				valid = command.readCommand(connection);
//				duration= (System.nanoTime()-start)/1000;
//				System.out.println("habe auf Kommando "+(char)command.getCommand()+" "+duration+" usec gewartet");
				if (valid == 0) {// Kommando ist in Ordnung
					run=storeCommand(command);
				} else
					System.out.println("Ungueltiges Kommando"); //$NON-NLS-1$
			} catch (IOException e) {
				lg.severe(e, "Verbindung unterbrochen -- Bot stirbt");
				die();
				run =-1;
			}
		}

		//t2 = System.nanoTime()/1000;
//		System.out.println("zeit in receiveCommands: "+(t2-t1)+" us   --  Zeit ausserhalb :"+aussen+ " us" );
	//	die();
	}


	//LEDs
	/**
	 * @param actL
	 *            Der Wert von actLed, der gesetzt werden soll
	 */
	public void setActLed(int actL) {

		this.actLed = new Integer(actL);

//		int ledCount = cols.length;
//
//		int range = (int)Math.pow(2, ledCount);
//
//		while(this.actLed > range)
//			this.actLed -= range;
	}

	private static final Color[] cols = {
		new Color(137, 176, 255), // blau
		new Color(137, 176, 255), // blau
		new Color(255, 137, 137), // rot
		new Color(255, 230, 139), // orange
		new Color(255, 255, 159), // gelb
		new Color(170, 255, 170), // gruen
		new Color(200, 255, 245), // tuerkis
		new Color(245, 245, 245)  // weiss
	};

	private static final Color[] colsAct = {
		new Color(  0,  84, 255), // blau
		new Color(  0,  84, 255), // blau
		new Color(255,   0,   0), // rot
		new Color(255, 200,   0), // orange
		new Color(255, 255,   0), // gelb
		new Color(  0, 255,   0), // gruen
		new Color(  0, 255, 210), // tuerkis
		new Color(255, 255, 255)  // weiss
	};

	/**
	 * Erweitert stop() um das schliessen der TCP/Verbindung
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		super.stop();
		try {
			connection.disconnect();
		} catch (IOException e) {
			// uninteressant
		} catch (Exception e) {
			// uninteressant
		}
	}


	@Override
	protected void cleanup() {
		// TODO Auto-generated method stub
		super.cleanup();
		world=null;
	}

}