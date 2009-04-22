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

/**
 * Master-Simulator (CNY70, Maus, Raeder, Mater, Rest)
 */
public class MasterSimulator
implements NumberTwinVisitor, BotBuisitor, Runnable {
    /**
     * Rad-Simulator
     */
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

        /** Governor des Simulators */
        private Actuators.Governor governor;

        /**
         * Setzt den governor
         * @param governor
         */
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
        protected double revsThisSimStep() {
            // LODO Die Kennlinien der echten Motoren sind nicht linear
            double speedRatio = governor.get().floatValue()
                / PWM_MAX;
            double speedInRps = speedRatio * REVS_PER_SEC_MAX;
            double deltaTInSec = parent.getDeltaTInMs() / 1000.0d;
            return speedInRps * deltaTInSec;
        }
    }

    /**
     * Maussensor-Simulator
     */
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

        /** Maussensor des Simulators */
        private Sensors.Mouse sensor;

        /**
         * Setzt den Maussensor
         * @param sensor
         */
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

        /**
         * Setzt den Sensor auf value
         * @param value
         */
        public void set(double value) {
            double tmp = meter2Dots(value) + valueFraction;
            int valueInteger = (int)Math.floor(tmp); // Vorkommaanteil
            valueFraction = tmp - valueInteger; // Nachkommaanteil merken
            sensor.set(valueInteger);
        }
    }

    /**
     * Simulation der Botbewegung
     */
    class KrautUndRuebenSimulator implements Runnable {
        /** Umfang eines Rades [m] */
        private static final double WHEEL_CIRCUMFERENCE = 0.1781283d;

        /** Abstand Mittelpunkt Bot zum Rad [m] */
        private static final double WHEEL_DIST = 0.0486d;

        /** Abstand Zentrum Maussensor in Vorausrichtung (Y) [m] */
        private static final double SENS_MOUSE_DIST_Y = -0.015d;

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
			// Position und Heading berechnen:

			// Fuer ausfuehrliche Erlaeuterung der Positionsberechnung
			// siehe pdf

			// Absolut zurueckgelegte Strecke pro Rad berechnen
			double s_l = leftWheel.revsThisSimStep() * WHEEL_CIRCUMFERENCE;
			double s_r = rightWheel.revsThisSimStep() * WHEEL_CIRCUMFERENCE;

			// Haelfte des Drehwinkels, den der Bot in diesem Simschritt
			// hinzubekommt
			double _gamma = (s_l - s_r) / (4.0 * WHEEL_DIST);

			// neue Blickrichtung berechnen
			// ergibt sich aus Rotation der Blickrichtung um 2*_gamma
			Vector3d _hd = parent.getHeadingVectorInWorldCoord();
			double _s2g = Math.sin(2 * _gamma);
			double _c2g = Math.cos(2 * _gamma);
			Vector3d newHeading = new Vector3d((_hd.x * _c2g + _hd.y * _s2g),
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
				// andernfalls die Distanz laut Formel berechnen
				moveDistance = 0.5 * (s_l + s_r) * Math.sin(_gamma) / _gamma;
			}

			// Den Bewegungsvektor berechnen ...
			Vector3d moveDirection = new Vector3d((_hd.x * _cg + _hd.y * _sg),
					(-_hd.x * _sg + _hd.y * _cg), 0f);
			moveDirection.normalize();
			moveDirection.scale(moveDistance);
			// ... und die alte Position entsprechend veraendern.
			newPos.add(moveDirection);

			mouseSensorX.set(2 * _gamma * SENS_MOUSE_DIST_Y);
			mouseSensorY.set(moveDistance);

			boolean isCollided = world.isCollided(parent, new BoundingSphere(
					new Point3d(0d, 0d, 0d), CtBotSimTcp.BOT_RADIUS), newPos);
			// Wenn Kollision, Bot entsprechend faerben
			parent.set(COLLIDED, isCollided);

			// Bodenkontakt ueberpruefen

			// Vektor vom Ursprung zum linken Rad
			Vector3d vecL = new Vector3d(-newHeading.y, newHeading.x, 0f);
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
			Vector3d skidVec = new Vector3d(BOT_SKID_X, BOT_SKID_Y,
					-CtBotSimTcp.BOT_HEIGHT / 2);
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

			if (!parent.is(IN_HOLE) && !parent.is(COLLIDED))
				parent.setPosition(new Point3d(newPos));

			if (!parent.is(IN_HOLE)) {
				parent.setHeading(newHeading);
			} else {
				mouseSensorX.sensor.set(0);
			}

			if (!parent.isObstStateNormal()) {
//				mouseSensorX.sensor.set(0);
				mouseSensorY.sensor.set(0);
			}

			clock.setSimTimeInMs((int) world.getSimTimeInMs());
		}
	}

    /**
    * Repr&auml;sentiert einen optischen Sensor vom Typ CNY70. Beim c't-Bot
    * kommt der CNY70 u.a. als Liniensensor (2&times;) und als
    * Abgrundsensor (2&times;) zum Einsatz.
    */
    class Cny70Simulator implements Runnable {
        /** Oeffnungswinkel (rad) */
    	private final double OPENING_ANGLE_IN_RAD = Math.toRadians(80);
        /** Genauigkeit */
    	private static final short PRECISION = 10;

    	/** Distanz zur Mitte */
        private final Point3d distFromBotCenter;
        /** Heading */
        private final Vector3d headingInBotCoord;
         /** interne Daten */
        private final NumberTwin sensor;

        /**
         * CNY70-Simulator
         * @param distFromBotCenter
         * @param headingInBotCoord
         * @param sensor
         */
        public Cny70Simulator(Point3d distFromBotCenter,
        Vector3d headingInBotCoord, NumberTwin sensor) {
            this.distFromBotCenter = distFromBotCenter;
            this.headingInBotCoord = headingInBotCoord;
            this.sensor = sensor;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            sensor.set(
                world.sensGroundReflectionCross(
                    parent.worldCoordFromBotCoord(distFromBotCenter),
                    parent.worldCoordFromBotCoord(headingInBotCoord),
                    OPENING_ANGLE_IN_RAD,
                    PRECISION));
        }
    }

    /** Simulatoren */
    private final List<Runnable> simulators = Misc.newList();
     /** Simulator fuer Position */
    private final KrautUndRuebenSimulator krautUndRuebenSim;
    /** Welt */
    protected final World world;
    /** 3D-Repraesentation des Bots */
    protected final ThreeDBot parent;
    /** Bot-Buisitor*/
    private final Buisitor buisitor = new Buisitor(this);

    /** Rad links */
    protected final WheelSimulator leftWheel = new WheelSimulator();
    /** Rad rechts */
    protected final WheelSimulator rightWheel = new WheelSimulator();

    /** Maussensor-X */
    protected final MouseSensorSimulator mouseSensorX =
        new MouseSensorSimulator();
    /** Maussensor-Y */
    protected final MouseSensorSimulator mouseSensorY =
        new MouseSensorSimulator();
    /** Uhr / Systemzeit */
    private Clock clock;

    /**
     * @param x
     * @param y
     * @param z
     * @param flipX
     * @return 3D-Punkt aus Koordinaten
     */
    private static Point3d at(double x, double y, double z, boolean flipX) {
        return new Point3d((flipX ? -1 : +1 ) * x, y, z);
    }

    /**
     * @return Einheitsvektor nach vorn
     */
    private static Vector3d looksForward() {
        return new Vector3d(0, 1, 0);
    }

    /**
     * @param world		Welt, in der simuliert wird
     * @param parent	ThreeDBot
     */
    public MasterSimulator(final World world, final ThreeDBot parent) {
        this.world = world;
        this.parent = parent;
        krautUndRuebenSim = new KrautUndRuebenSimulator();
        parent.accept(this);
    }

    /**
     * @see ctSim.model.bots.BotBuisitor#visit(java.lang.Object, ctSim.model.bots.Bot)
     */
    public void visit(Object o, Bot b) {
        if (o instanceof BotComponent<?>)
            buisitor.dispatchBuisit((BotComponent<?>)o);
        if (o instanceof NumberTwin)
            ((NumberTwin)o).acceptNumTwinVisitor(this);
    }

    /**
     * @see ctSim.model.bots.components.NumberTwin.NumberTwinVisitor#visit(ctSim.model.bots.components.NumberTwin, boolean)
     */
    public void visit(NumberTwin numberTwin, boolean isLeft) {
        buisitor.dispatchBuisit(numberTwin, isLeft);
    }

    /**
     * @param g	governor
     * @param isLeft links?
     */
    public void buisitWheel(Governor g, boolean isLeft) {
        (isLeft ? leftWheel : rightWheel).setGovernor(g);
    }

    /**
     * @param encoderSensor	Encoder
     * @param isLeft	links?
     */
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
            * &uuml;brig geblieben ist (-1; 1). Wird hier gespeichert, um zu
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

    /**
     * @param sensor Distanzsensor
     * @param isLeft links?
     * @throws IOException
     */
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

    /**
     * @param s Liniensensor
     * @param isLeft links?
     */
    public void buisitLineSensorSim(Sensors.Line s, boolean isLeft) {
        simulators.add(new Cny70Simulator(
            at(0.004, 0.009, -0.011 - CtBotSimTcp.BOT_HEIGHT / 2, isLeft),
            looksForward(), s));
    }

    /**
     * @param s Border-Sensor
     * @param isLeft links?
     */
    public void buisitBorderSensorSim(Sensors.Border s, boolean isLeft) {
        simulators.add(new Cny70Simulator(
            at(0.036, 0.0384, - CtBotSimTcp.BOT_HEIGHT / 2, isLeft),
            looksForward(), s));
    }

    /**
     * @param sensor Lichtsensor
     * @param isLeft links?
     */
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

    /**
     * @param sensor Maussensor
     * @param isX X? (sonst Y)
     */
    public void buisitMouseSensorSim(final Sensors.Mouse sensor,
    boolean isX) {
        (isX ? mouseSensorX : mouseSensorY).setSensor(sensor);
    }

    /**
     * @param sensor Clock
     */
    public void buisitClockSim(final Sensors.Clock sensor) {
        this.clock  = sensor;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // Wichtig: Zuerst die Sensoren, dann Kraut + Rueben     
        for (Runnable simulator : simulators) {
            simulator.run();
        }
        krautUndRuebenSim.run();
    }
}