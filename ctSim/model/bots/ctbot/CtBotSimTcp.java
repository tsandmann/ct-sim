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
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Transform3D;
import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.Connection;
import ctSim.SimUtils;
import ctSim.controller.Config;
import ctSim.model.AliveObstacle;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.World;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.Characteristic;
import ctSim.model.bots.components.NumberTwin;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.Actuators.Governor;
import ctSim.util.Buisitor;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.util.Buisitor.Buisit;

//$$$ DONE dataL = 20 wird nie gesendet
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

    final FmtLogger lg = FmtLogger.getLogger(
    	"ctSim.model.bots.ctbot.CtBotSimTcp");

    /** Die TCP-Verbindung */
    private final Connection connection;

    private ArrayList<Command> commandBuffer = Misc.newList();

    /** Sequenznummer der TCP-Pakete */
    private int seq = 0;

    private World world; //$$$ final

    private final MasterSimulator masterSimulator;

    ///////////////////////////////////////////////////////////////////////////

    public interface NumberTwinVisitor {
        public void visit(NumberTwin numberTwin, boolean isLeft);
    }

    //$$$ Auslagern
    public static class MasterSimulator	implements NumberTwinVisitor, Runnable {
        class WheelSimulator {
            /**
             * Maximale Geschwindigkeit als <a
             * href="http://en.wikipedia.org/wiki/Pulse-width_modulation">PWM-Wert</a>
             */
            private static final short PWM_MAX = 255;

            /**
             * Maximale Geschwindigkeit in Umdrehungen pro Sekunde ("revolutions
             * per second")
             */
            private static final float REVS_PER_SEC_MAX = 151f / 60f;

            private Actuators.Governor governor;

            public void setGovernor(Actuators.Governor governor) {
                this.governor = governor;
            }

            /**
			 * Errechnet aus einer PWM die Anzahl an Umdrehungen pro Sekunde.
			 * Methode kann in einem Simschritt ohne Nebenwirkungen mehrfach
			 * aufgerufen werden (idempotente Methode).
			 *
			 * @return Umdrehungen pro Sekunde (exakter Wert, d.h. mit
			 * Nachkommaanteil)
			 */
            protected float revsThisSimStep() {
                // LODO Die Kennlinien der echten Motoren sind nicht linear
                float speedRatio = governor.get().floatValue()
                    / PWM_MAX;
                float speedInRps = speedRatio * REVS_PER_SEC_MAX;
                float deltaTInSec = parent.getDeltaTInMs() / 1000f;
                return speedInRps * deltaTInSec;
            }
        }

        class MouseSensorSimulator {
        	/** Aufl&ouml;sung des Maussensors [dpi] */
        	private static final int SENS_MOUSE_DPI = 400;

        	/**
			 * Enh&auml;lt den Rest, der beim vorigen Aufruf von
			 * {@link #set(double)} entstanden ist. Sensor kann nur ints
			 * uebermitteln; damit die Nachkommaanteile der Berechnung nicht
			 * verlorengehen und sich die Abweichung mit der Zeit anh&auml;uft,
			 * merken wir sie uns hier
			 */
        	private double valueFraction = 0;

        	private Sensors.Mouse sensor;

        	public void setSensor(Sensors.Mouse sensor) {
				this.sensor = sensor;
			}

        	/**
			 * Errechnet die Anzahl an Dots, die der Maussensor f&uuml;r eine
			 * Bewegung der angegebenen L&auml;nge zurueckmeldet.
			 *
			 * @param distanceInM Die L&auml;nge der Strecke in Metern
			 * @return Anzahl der Dots
			 */
        	private double meter2Dots(double distanceInM) {
        		// distance ist in Metern angegeben,
        		// * 100 macht daraus cm; 2,54 cm sind ein Inch,
        		// anschliessend Multiplikation mit der Aufloesung des Maussensors
        		return distanceInM * 100 / 2.54 * SENS_MOUSE_DPI;
        	}

        	public void set(double value) {
        		double tmp = meter2Dots(value) + valueFraction;
        		int valueInteger = (int)Math.floor(tmp); // Vorkommaanteil
        		valueFraction = tmp - valueInteger; // Nachkommaanteil merken
        		sensor.set(valueInteger);
        	}
        }

        //$$ Dieses Ding sollte aufgeteilt und gruendlich vereinfacht werden. Wieso nicht J3D die Transformationen machen lassen, statt manuell an irgendwelchen Vektoren rumzupopeln? Beim Lichtsensor ist das z.B. auch so
		class KrautUndRuebenSimulator implements Runnable {
		    /** Umfang eines Rades [m] */
		    private static final double WHEEL_CIRCUMFERENCE = Math.PI * 0.057d;

		    /** Abstand Mittelpunkt Bot zum Rad [m] */
		    private static final double WHEEL_DIST = 0.0485d;

		    /** Abstand Zentrum Maussensor in Vorausrichtung (Y) [m] */
		    private static final double SENS_MOUSE_DIST_Y = -0.015d;

		    //TODO Bot soll in Loecher reinfahren, in Hindernisse aber nicht. Momentan: calcPos traegt Pos nicht ein, wenn Hindernis oder Loch. Gewuenscht: Bei Loch das erste Mal Pos updaten, alle weiteren Male nicht
		    public void run() {
		        // Position und Heading berechnen:

		        // Fuer ausfuehrliche Erlaeuterung der Positionsberechnung
		    	// siehe pdf
		    	// $$ Ja welches pdf?

		        // Absolut zurueckgelegte Strecke pro Rad berechnen
		        double s_l = leftWheel .revsThisSimStep() * WHEEL_CIRCUMFERENCE;
		        double s_r = rightWheel.revsThisSimStep() * WHEEL_CIRCUMFERENCE;

		        // Haelfte des Drehwinkels, den der Bot in diesem Simschritt
		        // hinzubekommt
		        double _gamma = (s_l - s_r) / (4.0 * WHEEL_DIST);

		        /*
		         * // Jetzt fehlt noch die neue Blickrichtung
		         * Vector3f newHeading = new
		         * Vector3f(-mid.y, mid.x, 0);
		         */

		        // neue Blickrichtung berechnen
		        // ergibt sich aus Rotation der Blickrichtung um 2*_gamma
		        Vector3d _hd = parent.getHeadingInWorldCoord();
		        double _s2g = Math.sin(2 * _gamma);
		        double _c2g = Math.cos(2 * _gamma);
		        Vector3d newHeading = new Vector3d(
		                (_hd.x * _c2g + _hd.y * _s2g),
		                (-_hd.x * _s2g + _hd.y * _c2g), 0f);

		        newHeading.normalize();

		        // Neue Position bestimmen
		        Vector3d newPos = new Vector3d(parent.getPositionInWorldCoord());
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

		        mouseSensorX.set(2 * _gamma * SENS_MOUSE_DIST_Y);
		        mouseSensorY.set(moveDistance);

		        int oldState = parent.getObstState();

		        //$$ Wieso BoundingSphere um den Ursprung und nicht um newPos? In dem Zusammenhang: wofuer ist Bot.getBounds() da?
		        boolean noCol = world.checkCollision(parent,
		        	new BoundingSphere(new Point3d(0d, 0d, 0d), BOT_RADIUS),
		        	newPos);
		        // Pruefen, ob Kollision erfolgt. Bei einer Kollision wird
		        // der Bot entsprechend gefaerbt
		        if(noCol && (oldState & OBST_STATE_COLLISION) ==
		        	OBST_STATE_COLLISION) {

		            parent.setObstState( oldState & ~OBST_STATE_COLLISION);
		        } else if (!noCol && (oldState & OBST_STATE_COLLISION) !=
		        	OBST_STATE_COLLISION) {

		            moveDistance = 0; // Wird spaeter noch fuer den Maussensor benoetigt //$$ Bezweifle ich, die Variable wird nicht mehr gelesen
		            parent.setObstState( oldState| OBST_STATE_COLLISION);
		        }

		        // Bodenkontakt ueberpruefen

		        // Vektor vom Ursprung zum linken Rad
		        Vector3d vecL = new Vector3d(-newHeading.y,newHeading.x, 0f);
		        vecL.scale((float) WHEEL_DIST);
		        // neue Position linkes Rad
		        Vector3d posRadL = new Vector3d(parent.getPositionInWorldCoord());
		        posRadL.add(vecL);

		        // Vektor vom Ursprung zum rechten Rad
		        Vector3d vecR = new Vector3d(newHeading.y, -newHeading.x, 0f);
		        vecR.scale((float) WHEEL_DIST);
		        // neue Position rechtes Rad
		        Vector3d posRadR = new Vector3d(parent.getPositionInWorldCoord());
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
		        Vector3d skidVec = new Vector3d(
		        	BOT_SKID_X, BOT_SKID_Y, -BOT_HEIGHT / 2);
		        // Position des Gleitpins gemaess der Ausrichtung des Bots anpassen
		        rotation.transform(skidVec);
		        skidVec.add(new Point3d(newPos));

		        boolean isFalling = !world.checkTerrain(new Point3d(skidVec),
		        	BOT_GROUND_CLEARANCE);

		        // Bodenkontakt des linken Reifens ueberpruefen
		        posRadL.z -= BOT_HEIGHT / 2;

		        isFalling |= !world.checkTerrain(new Point3d(posRadL),
		        	BOT_GROUND_CLEARANCE);

		        // Bodenkontakt des rechten Reifens ueberpruefen
		        posRadR.z -= BOT_HEIGHT / 2;

		        isFalling |= !world.checkTerrain(new Point3d(posRadR),
		        	BOT_GROUND_CLEARANCE);

		        // Wenn einer der Beruehrungspunkte keinen Boden mehr unter
		        // sich hat wird der Bot gestoppt und entsprechend gefaerbt
		        if(isFalling && (OBST_STATE_FALLING !=
		        	(parent.getObstState() & OBST_STATE_FALLING))) {

		            parent.setObstState(parent.getObstState() | OBST_STATE_FALLING);
		        } else if(!isFalling && (OBST_STATE_FALLING ==
		        	(parent.getObstState() & OBST_STATE_FALLING))) {

		            parent.setObstState(parent.getObstState() & ~OBST_STATE_FALLING);
		        }

		        // Wenn der Bot nicht kollidiert oder ueber einem Abgrund steht Position aktualisieren
		        if ((parent.getObstState() &
		        	(OBST_STATE_COLLISION | OBST_STATE_FALLING)) == 0 ){

		        	parent.setPosition(new Point3d(newPos));
		        	parent.setHeading(newHeading);
		        }
		        // Blickrichtung nur aktualisieren, wenn Bot nicht in ein
		        // Loch gefallen ist:
		        if ((parent.getObstState() & OBST_STATE_FALLING) == 0 ){
		        	parent.setHeading(newHeading);
		        }

		        if (parent.getObstState() != OBST_STATE_NORMAL) {
		        	//$$ Hack
		        	mouseSensorX.sensor.set(0);
		        	mouseSensorY.sensor.set(0);
		        }
		    }
		}

        /**
         * Repr&auml;sentiert einen optischen Sensor vom Typ CNY70. Beim c't-Bot
         * kommt der CNY70 u.a. als Liniensensor (2&times;) und als
         * Abgrundsensor (2&times;) zum Einsatz.
         */
        class Cny70Simulator implements Runnable {
            private final double OPENING_ANGLE_IN_RAD = Math.toRadians(80);
            private static final short PRECISION = 10;

            private final Point3d distFromBotCenter;
            private final Vector3d headingInBotCoord;
            private final NumberTwin sensor;

            public Cny70Simulator(Point3d distFromBotCenter,
            Vector3d headingInBotCoord, NumberTwin sensor) {
                this.distFromBotCenter = distFromBotCenter;
                this.headingInBotCoord = headingInBotCoord;
                this.sensor = sensor;
            }

            public void run() {
                sensor.set(
                    world.sensGroundReflectionCross(
                        parent.worldCoordFromBotCoord(distFromBotCenter),
                        parent.worldCoordFromBotCoord(headingInBotCoord),
                        OPENING_ANGLE_IN_RAD,
                        PRECISION));
            }
        }

        private final List<Runnable> simulators = Misc.newList();
        private final KrautUndRuebenSimulator krautUndRuebenSim;
        protected final World world;
        protected final AliveObstacle parent;
        private final Buisitor buisitor = new Buisitor(this);

        protected final WheelSimulator leftWheel = new WheelSimulator();
        protected final WheelSimulator rightWheel = new WheelSimulator();

        protected final MouseSensorSimulator mouseSensorX =
        	new MouseSensorSimulator();
        protected final MouseSensorSimulator mouseSensorY =
        	new MouseSensorSimulator();

        private static Point3d at(double x, double y, double z, boolean flipX) {
            return new Point3d((flipX ? -1 : +1 ) * x, y, z);
        }

        private static Vector3d looksForward() {
            return new Vector3d(0, 1, 0);
        }

        public MasterSimulator(final World world, final AliveObstacle parent) {
            this.world = world;
            this.parent = parent;
            krautUndRuebenSim = new KrautUndRuebenSimulator();
        }

        public void visit(NumberTwin numberTwin, boolean isLeft) {
            buisitor.dispatchBuisit(numberTwin, isLeft);
        }

        @Buisit
        public void initWheel(Governor g, boolean isLeft) {
            (isLeft ? leftWheel : rightWheel).setGovernor(g);
        }

        @Buisit
        public void buildEncoderSim(final Sensors.Encoder sensor,
        boolean isLeft) {
            final WheelSimulator wheel = isLeft
                ? leftWheel
                : rightWheel;

            simulators.add(new Runnable() {
                /** Anzahl an Encoder-Markierungen auf einem Rad */
                private static final short ENCODER_MARKS = 60;

                /**
				 * Bruchteil des Encoder-Schritts, der beim letztem Sim-Schritt
				 * &uuml;brig geblieben ist [0; 1[. Wird hier gespeichert, um zu
				 * verhindern, dass sich der Rundungsfehler mit der Zeit
				 * anh&auml;uft.
				 */
                private double encoderRest = 0;

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
                    sensor.set(encoderSteps); // commit
                }
            });
        }

        @Buisit
        public void buildDistanceSim(final Sensors.Distance sensor,
        boolean isLeft) {
            final Point3d distFromBotCenter = isLeft
                ? new Point3d(- 0.036, 0.0554, 0)
                : new Point3d(+ 0.036, 0.0554, 0);
            final Vector3d headingInBotCoord = looksForward();
            final Characteristic charstic = isLeft
                ? new Characteristic("characteristics/gp2d12Left.txt", 100)
                : new Characteristic("characteristics/gp2d12Right.txt", 80);

            simulators.add(new Runnable() {
                private final double OPENING_ANGLE_IN_RAD = Math.toRadians(3);

                public void run() {
                    double distInM = world.watchObstacle(
                        parent.worldCoordFromBotCoord(distFromBotCenter),
                        parent.worldCoordFromBotCoord(headingInBotCoord),
                        OPENING_ANGLE_IN_RAD,
                        parent.getShape());
                    double distInCm = 100 * distInM;
                    double sensorReading = charstic.lookupPrecise(distInCm);
                    sensor.set(sensorReading);
                }
            });
        }

        @Buisit
        public void buildLineSensorSim(Sensors.Line s, boolean isLeft) {
            simulators.add(new Cny70Simulator(
                at(0.004, 0.009, -0.011 - BOT_HEIGHT / 2, isLeft),
                looksForward(), s));
        }

        @Buisit
        public void buildBorderSensorSim(Sensors.Border s, boolean isLeft) {
            simulators.add(new Cny70Simulator(
                at(0.036, 0.0384, - BOT_HEIGHT / 2, isLeft),
                looksForward(), s));
        }

        @Buisit
        public void buildLightSim(final Sensors.Light sensor, boolean isLeft) {
            final Point3d distFromBotCenter = isLeft
                ? new Point3d(- 0.032, 0.048, 0.060 - BOT_HEIGHT / 2)
                : new Point3d(+ 0.032, 0.048, 0.060 - BOT_HEIGHT / 2);
            final Vector3d headingInBotCoord = looksForward();

            simulators.add(new Runnable() {
                private final double OPENING_ANGLE_IN_RAD = Math.toRadians(180);

                public void run() {
                    sensor.set(
                        world.sensLight(
                            parent.worldCoordFromBotCoord(distFromBotCenter),
                            parent.worldCoordFromBotCoord(headingInBotCoord),
                            OPENING_ANGLE_IN_RAD));
                }
            });
        }

        @Buisit
        public void buildMouseSensorSim(final Sensors.Mouse sensor, boolean isX) {
        	(isX ? mouseSensorX : mouseSensorY).setSensor(sensor);
        }

        public void run() {
        	//$$ Performance-Problem
        	// Wichtig: Zuerst die Sensoren, dann Kraut + Rueben
            for (Runnable simulator : simulators)
            	SwingUtilities.invokeLater(simulator);
            try {
				SwingUtilities.invokeAndWait(krautUndRuebenSim);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param w Die Welt
     * @param pos Position
     * @param head Blickrichtung
     * @param con Verbindung
     */
    public CtBotSimTcp(World w, Point3d pos, Vector3d head, Connection con) {
        super(w, "Sim-Bot", pos, head);
        this.connection = con;
        this.world = w;

        components.add(
            new Actuators.Governor(true),
            new Actuators.Governor(false),
            new Actuators.LcDisplay(20, 4),
            new Actuators.Log(),
            new Actuators.DoorServo(),
            new Sensors.Encoder(true),
            new Sensors.Encoder(false),
            new Sensors.Distance(true),
            new Sensors.Distance(false),
            new Sensors.Line(true),
            new Sensors.Line(false),
            new Sensors.Border(true),
            new Sensors.Border(false),
            new Sensors.Light(true),
            new Sensors.Light(false),
            new Sensors.Mouse(true),
            new Sensors.Mouse(false),
            new Sensors.RemoteControl(),
            new Sensors.Door(),
            new Sensors.Trans(),
            new Sensors.Error()
        );

        // LEDs
        int numLeds = ledColors.length;
        for (int i = 0; i < numLeds; i++) {
            String s = "LED " + (i + 1)
                     + (i == 0 ? " (vorn rechts)" :
                        i == 1 ? " (vorn links)" : "");
            components.add(new Actuators.Led(s, numLeds - i - 1, ledColors[i]));
        }

        // Component-Flag-Tabelle
        components.applyFlagTable(
            _(Actuators.Governor.class   , READS),
            _(Actuators.LcDisplay.class  , READS),
            _(Actuators.Log.class        , READS),
            _(Actuators.DoorServo.class  , READS),
            _(Actuators.Led.class        , READS),
            _(Sensors.Encoder.class      , WRITES),
            _(Sensors.Distance.class     , WRITES),
            _(Sensors.Line.class         , WRITES),
            _(Sensors.Border.class       , WRITES),
            _(Sensors.Light.class        , WRITES),
            _(Sensors.Mouse.class        , WRITES),
            _(Sensors.RemoteControl.class, WRITES),
            _(Sensors.Door.class         , WRITES),
            _(Sensors.Trans.class        , WRITES),
            _(Sensors.Error.class        , WRITES)
        );

        // Simulation
        masterSimulator = new MasterSimulator(world, this);
        for (BotComponent<?> c : components) {
            if (c instanceof NumberTwin)
                ((NumberTwin)c).accept(masterSimulator);
        }

        sendRcStartCode();
    }

    @Override
    public String getDescription() {
        return "Simulierter, in C geschriebener c't-Bot";
    }

    /**
	 * Sendet den Fernbedienungs-(RC5-)Code, der in der Konfigdatei angegeben
	 * ist. Methode tut nichts, falls nichts, 0 oder ein nicht von
	 * {@link Integer#decode(String)} verwertbarer Code angegeben ist.
	 */
    private void sendRcStartCode() {
        String rawStr = Config.getValue("rcStartCode");
        try {
            int rcStartCode = Integer.decode(rawStr);
            for (BotComponent<?> c : components) {
                if (c instanceof Sensors.RemoteControl) {
                	lg.fine("Sende RC5-Code %d (%#x) an %s",
                		rcStartCode, rcStartCode, getName());
                    ((Sensors.RemoteControl)c).set(rcStartCode);
                    break;
                }
            }
        } catch (NumberFormatException e) {
            lg.warn(e, "Konnte rcStartCode '%s' aus der Konfigdatei nicht " +
            		"verwerten; ignoriere", rawStr);
        }
    }

    /**
     * Variable, die sich merkt, wann wir zuletzt Daten &uuml;bertragen haben
     */
    private int lastTransmittedSimulTime = 0;

    /** Leite Sensordaten an den Bot weiter */
    private synchronized void transmitSensors() {
        try {
            CommandOutputStream s = connection.createCmdOutStream(); //$$$ Konstruktor
            for (BotComponent<?> c : components)
                c.askForWrite(s);
            s.flush();

            Command command;
            lastTransmittedSimulTime= (int)world.getSimTimeInMs();
            lastTransmittedSimulTime %= 10000;	// Wir haben nur 16 Bit zur verfuegung und 10.000 ist ne nette Zahl ;-)
            command = new Command(Command.Code.DONE);
            command.setDataL(lastTransmittedSimulTime);
            command.setDataR(0);
            command.setSeq(this.seq++);
            connection.write(command);
        } catch (IOException e) {
            lg.severe(e, "E/A-Problem beim Senden der Sensordaten; Abbruch");
            die();
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
                lg.warn("Unbekanntes Kommando%s", command);
                break;
            }
        } else {
            // TODO: Antworten werden noch nicht gegeben
        }
    }

    @Override
    protected void work() {
    	SwingUtilities.invokeLater(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				transmitSensors();
			}
    	});
        receiveCommands();
        SwingUtilities.invokeLater(new Runnable() {
        	@SuppressWarnings("synthetic-access")
			public void run() {
        		processCommands();
        	}
        });
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
		masterSimulator.run();
    }

    /** Sichert ein Kommando im Puffer */
    private int storeCommand(Command command) {
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
    private void processCommands(){
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

    private void receiveCommands() {
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

    //$$ Unterschied cleanup() und die()? Zusammenfassen (Death-Listener)
    @Override
    protected void cleanup() {
        super.cleanup();
        world = null;
    }
}