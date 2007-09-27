/**
 *
 */
package ctSim.model.bots.ctbot;

import static ctSim.model.ThreeDBot.State.*;
import java.io.IOException;
import java.util.List;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.model.ThreeDBot;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.BotBuisitor;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.Characteristic;
import ctSim.model.bots.components.NumberTwin;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.Actuators.Governor;
import ctSim.model.bots.components.NumberTwin.NumberTwinVisitor;
import ctSim.model.bots.components.Sensors.Clock;
import ctSim.util.Buisitor;
import ctSim.util.Misc;

//$$ doc
//$$ Der koennte aufgeteilt werden in allgemein (nach CtSim) und spezifisch (hierher)
public class MasterSimulator
implements NumberTwinVisitor, BotBuisitor, Runnable {
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
        * Zahl der Umdrehungen, die das Rad im jetzigen Sim-Schritt macht
        * (Beispiel: 10 Umdrehungen pro Sekunde, Sim-Schritt ist 0,2
        * Sim-Sekunden lang -&gt; R&uuml;ckgabewert 2). Methode kann in
        * einem Simschritt ohne Nebenwirkungen mehrfach aufgerufen werden
        * (idempotente Methode).
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
            Vector3d _hd = parent.getHeadingVectorInWorldCoord();
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

            //$$ Wieso BoundingSphere um den Ursprung und nicht um newPos? In dem Zusammenhang: wofuer ist Bot.getBounds() da?
            boolean isCollided = world.isCollided(parent,
                new BoundingSphere(new Point3d(0d, 0d, 0d), CtBotSimTcp.BOT_RADIUS),
                newPos);
            // Wenn Kollision, Bot entsprechend faerben
            parent.set(COLLIDED, isCollided);

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
                BOT_SKID_X, BOT_SKID_Y, -CtBotSimTcp.BOT_HEIGHT / 2);
            // Position des Gleitpins gemaess der Ausrichtung des Bots anpassen
            rotation.transform(skidVec);
            skidVec.add(new Point3d(newPos));

            boolean isFalling = !world.checkTerrain(new Point3d(skidVec),
                CtBotSimTcp.BOT_GROUND_CLEARANCE);

            // Bodenkontakt des linken Reifens ueberpruefen
            posRadL.z -= CtBotSimTcp.BOT_HEIGHT / 2;

            isFalling |= !world.checkTerrain(new Point3d(posRadL),
                CtBotSimTcp.BOT_GROUND_CLEARANCE);

            // Bodenkontakt des rechten Reifens ueberpruefen
            posRadR.z -= CtBotSimTcp.BOT_HEIGHT / 2;

            isFalling |= !world.checkTerrain(new Point3d(posRadR),
                CtBotSimTcp.BOT_GROUND_CLEARANCE);

            // Wenn einer der Beruehrungspunkte keinen Boden mehr unter
            // sich hat wird der Bot gestoppt und entsprechend gefaerbt
            parent.set(IN_HOLE, isFalling);

            if (! parent.is(IN_HOLE) && ! parent.is(COLLIDED))
                parent.setPosition(new Point3d(newPos));

            if (! parent.is(IN_HOLE))
                parent.setHeading(newHeading);

            if (! parent.isObstStateNormal()) {
                //$$ Hack
                mouseSensorX.sensor.set(0);
                mouseSensorY.sensor.set(0);
            }

            //$$ Hack
            clock.setSimTimeInMs((int)world.getSimTimeInMs());
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
    protected final ThreeDBot parent;
    private final Buisitor buisitor = new Buisitor(this);

    protected final WheelSimulator leftWheel = new WheelSimulator();
    protected final WheelSimulator rightWheel = new WheelSimulator();

    protected final MouseSensorSimulator mouseSensorX =
        new MouseSensorSimulator();
    protected final MouseSensorSimulator mouseSensorY =
        new MouseSensorSimulator();
    private Clock clock;

    private static Point3d at(double x, double y, double z, boolean flipX) {
        return new Point3d((flipX ? -1 : +1 ) * x, y, z);
    }

    private static Vector3d looksForward() {
        return new Vector3d(0, 1, 0);
    }

    public MasterSimulator(final World world, final ThreeDBot parent) {
        this.world = world;
        this.parent = parent;
        krautUndRuebenSim = new KrautUndRuebenSimulator();
        parent.accept(this);
    }

    public void visit(Object o, Bot b) {
        if (o instanceof BotComponent<?>)
            buisitor.dispatchBuisit((BotComponent<?>)o);
        //$$$ das ist zu kompliziert
        if (o instanceof NumberTwin)
            ((NumberTwin)o).acceptNumTwinVisitor(this);
    }

    public void visit(NumberTwin numberTwin, boolean isLeft) {
        buisitor.dispatchBuisit(numberTwin, isLeft);
    }

    public void buisitWheel(Governor g, boolean isLeft) {
        (isLeft ? leftWheel : rightWheel).setGovernor(g);
    }

    public void buisitEncoderSim(final Sensors.Encoder encoderSensor,
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
                encoderSensor.set(encoderSteps); // commit
            }
        });
    }

    public void buisitDistanceSim(final Sensors.Distance sensor,
    boolean isLeft) throws IOException {
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

    public void buisitLineSensorSim(Sensors.Line s, boolean isLeft) {
        simulators.add(new Cny70Simulator(
            at(0.004, 0.009, -0.011 - CtBotSimTcp.BOT_HEIGHT / 2, isLeft),
            looksForward(), s));
    }

    public void buisitBorderSensorSim(Sensors.Border s, boolean isLeft) {
        simulators.add(new Cny70Simulator(
            at(0.036, 0.0384, - CtBotSimTcp.BOT_HEIGHT / 2, isLeft),
            looksForward(), s));
    }

    public void buisitLightSim(final Sensors.Light sensor, boolean isLeft) {
        final Point3d distFromBotCenter = isLeft
            ? new Point3d(- 0.032, 0.048, 0.060 - CtBotSimTcp.BOT_HEIGHT / 2)
            : new Point3d(+ 0.032, 0.048, 0.060 - CtBotSimTcp.BOT_HEIGHT / 2);
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

    public void buisitMouseSensorSim(final Sensors.Mouse sensor,
    boolean isX) {
        (isX ? mouseSensorX : mouseSensorY).setSensor(sensor);
    }

    public void buisitClockSim(final Sensors.Clock sensor) {
        this.clock  = sensor;
    }

    public void run() {
        // Wichtig: Zuerst die Sensoren, dann Kraut + Rueben     
        for (Runnable simulator : simulators) {
            simulator.run();
        }
        krautUndRuebenSim.run();
    }
}