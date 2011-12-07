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
import java.util.Random;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.PickInfo;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.controller.Config;
import ctSim.model.BPS;
import ctSim.model.ThreeDBot;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.bots.BotBuisitor;
import ctSim.model.bots.components.Actuators;
import ctSim.model.bots.components.Actuators.DoorServo;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.Characteristic;
import ctSim.model.bots.components.NumberTwin;
import ctSim.model.bots.components.Sensors;
import ctSim.model.bots.components.Actuators.Governor;
import ctSim.model.bots.components.NumberTwin.NumberTwinVisitor;
import ctSim.model.bots.components.Sensors.Clock;
import ctSim.model.bots.components.Sensors.Door;
import ctSim.model.bots.components.Sensors.Shutdown;
import ctSim.util.Buisitor;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Master-Simulator (CNY70, Maus, Raeder, Mater, Rest)
 */
public class MasterSimulator
implements NumberTwinVisitor, BotBuisitor, Runnable {
	/** Reichweite der Lichtsensoren / m */
	private final double LIGHT_MAX_DISTANCE = 1.0;
	/** Oeffnungswinkel der Lichtsensoren / radians */
	private final double LIGHT_OPENING_ANGLE= Math.toRadians(90);
	
	/** Reichweite der Baken / m */
	private static final double BPS_LIGHT_DISTANCE = 2.0;
	/** Oeffnungswinkel des BPS-Sensors / radians */
	private static final double BPS_OPENING_ANGLE = Math.toRadians(0.5);
	
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
        
        /** Nachlauf des Rades, falls Richtungswechsel */
        private double lag = 0;

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
         * @return Umdrehungen pro Sekunde (exakter Wert, d.h. mit Nachkommaanteil)
         */
        protected double revsThisSimStep() {
            // LODO Die Kennlinien der echten Motoren sind nicht linear
            double speedRatio = governor.get().floatValue() / PWM_MAX;
            double speedInRps = speedRatio * REVS_PER_SEC_MAX;
            double deltaTInSec = parent.getDeltaTInMs() / 1000.0d;
            return speedInRps * deltaTInSec;
        }
        
        /**
         * @return lag
         */
        public double getLag() {
        	return lag;
        }
        
        /**
         * @param newLag
         */
        public void setLag(double newLag) {
        	lag = newLag;
        }
    }
    
    /**
     * Servo-Simulator
     */
    class ServoSimulator {
        /** Servo des Simulators */
        private Actuators.DoorServo servo;
        
        /** Zustand des Servos (< 12: Klappe zu; >= 12: Klappe offen; 0: Servo aus) */
        private int position = 7;

        /**
         * Setzt den Servo
         * @param servo
         */
        public void setServo(Actuators.DoorServo servo) {
            this.servo = servo;
        }

        /**
         * Wertet die Servo-Position aus
         * @return Servo-Position
         */
        protected int getServoPosition() {
        	if (servo.get().intValue() > 0) {
        		position = servo.get().intValue();
        	}
            
        	return position;
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
    	/** Logger */
    	final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.ctbot.KrautUndRuebenSimulator");
    	
        /** Umfang eines Rades [m] */
        private static final double WHEEL_CIRCUMFERENCE = 0.1781283d;

        /** Abstand Mittelpunkt Bot zum Rad [m] */
        private static final double WHEEL_DIST = 0.0486d;

        /** Abstand Zentrum Maussensor in Vorausrichtung (Y) [m] */
        private static final double SENS_MOUSE_DIST_Y = - 0.015d;

        /** letzter Wert von s_l */
        private double last_s_l = 0;
        /** letzter Wert von s_r */
        private double last_s_r = 0;
        /** Zufallsgenerator fuer Nachlauf der Raeder */
        private Random randGenerator = new Random();
        /** Haben die Raeder Nachlauf beim Richtungswechsel? */
        private final boolean wheelsWithLag = Config.getValue("WheelLag").equals("true");
        
        /** BranchGroup des Bot-Shapes */
        private final Group botShape = parent.getBranchGroup();
        
        /** Sensor fuer Transportfach-Klappe */
		private Door doorSensor;
		/** letztes Objekt, das im Transportfach gesehen wurde */
		private Node objectInPocket = null;
		/** Objekt, das derzeit transportiert wird */
		private Node associatedObject = null;
		
        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
			/* Position und Heading berechnen 
			 * fuer ausfuehrliche Erlaeuterung der Positionsberechnung siehe doc-files/odometrie.pdf */

			/* Absolut zurueckgelegte Strecke pro Rad berechnen */
			double s_l = leftWheel.revsThisSimStep() * WHEEL_CIRCUMFERENCE;
			double s_r = rightWheel.revsThisSimStep() * WHEEL_CIRCUMFERENCE;
			
			/* Nachlauf der Raeder berechnen, falls gewuenscht */
			if (wheelsWithLag) {
				if (s_l == 0 && last_s_l != 0) {
					double rand = randGenerator.nextDouble();
					if (last_s_l < 0) {
						rand = -rand;
					}
					last_s_l = 0;
					s_l = rand * (WHEEL_CIRCUMFERENCE / 60.0);
//					lg.fine("s_l=" + s_l * 1000 + " mm");
					leftWheel.setLag(rand);
				} else {
					last_s_l = s_l;
					leftWheel.setLag(0.0);
				}
				if (s_r == 0 && last_s_r != 0) {
					double rand = randGenerator.nextDouble();
					if (last_s_r < 0) {
						rand = -rand;
					}
					last_s_r = 0;
					s_r = rand * (WHEEL_CIRCUMFERENCE / 60.0);
//					lg.fine("s_r=" + s_r * 1000 + " mm");
					rightWheel.setLag(rand);
				} else {
					last_s_r = s_r;
					rightWheel.setLag(0.0);
				}
			}

			/* Haelfte des Drehwinkels, den der Bot in diesem Simschritt hinzubekommt */
			double _gamma = (s_l - s_r) / (4.0 * WHEEL_DIST);

			/* neue Blickrichtung berechnen, ergibt sich aus Rotation der Blickrichtung um 2 * _gamma */
			Vector3d _hd = parent.getHeadingVectorInWorldCoord();
			double _s2g = Math.sin(2 * _gamma);
			double _c2g = Math.cos(2 * _gamma);
			Vector3d newHeading = new Vector3d((_hd.x * _c2g + _hd.y * _s2g), (-_hd.x * _s2g + _hd.y * _c2g), 0);

			newHeading.normalize();

			/* Neue Position bestimmen */
			Vector3d newPos = new Vector3d(parent.getPositionInWorldCoord());
			double _sg = Math.sin(_gamma);
			double _cg = Math.cos(_gamma);
			double moveDistance;
			if (_gamma == 0) {
				/* Bewegung geradeaus */
				moveDistance = s_l;
			} else {
				/* andernfalls die Distanz laut Formel berechnen */
				moveDistance = 0.5 * (s_l + s_r) * Math.sin(_gamma) / _gamma;
			}

			/* Den Bewegungsvektor berechnen */
			Vector3d moveDirection = new Vector3d((_hd.x * _cg + _hd.y * _sg), (-_hd.x * _sg + _hd.y * _cg), 0);
			moveDirection.normalize();
			moveDirection.scale(moveDistance);
			/* die alte Position entsprechend veraendern */
			newPos.add(moveDirection);

			mouseSensorX.set(2 * _gamma * SENS_MOUSE_DIST_Y);
			mouseSensorY.set(moveDistance);
			
			final Point3d newPosPoint = new Point3d(newPos);
			final double newHeadAngle = SimUtils.getRotation(newHeading); // Winkel des Headings
			boolean collisionInPocket = false;

			/* Grundplatte auf Kollision pruefen */
			{
//				parent.clearDebugBG(); // loescht alte Debug-Anzeige

				/* Bounds fuer Grundplatte erstellen */
				Bounds plate = new BoundingSphere(new Point3d(0, 0, 0), CtBotSimTcp.BOT_RADIUS);

				/* schiebe probehalber Bounds an die neue Position */
				Transform3D t = new Transform3D();
				t.setTranslation(newPos);
				plate.transform(t);

//				parent.showDebugSphere(CtBotSimTcp.BOT_RADIUS, t);

				/* Kollision berechnen lassen */
				PickInfo platePickInfo = world.getCollision(botShape, plate);

				boolean botCollision = platePickInfo == null || platePickInfo.getNode() == null ? false : true;				

				if (botCollision && doorSensor.get().intValue() == 1) {
					/* Transportfach-Aussparung checken, falls Grundplatte kollidiert und Klappe auf */
					Transform3D transform = new Transform3D();

					/* Bounds fuer Transportfachs erstellen */
					Bounds pocket = createBounds(newPosPoint, newHeadAngle, 0, 0.04, 0.05 / 2, transform);

//					parent.showDebugBox(0.05 / 2, 0.055 / 2, 0.04 / 2, transform, newHeadAngle);
//					parent.showDebugSphere(0.05 / 2, transform);

					/* Kollision berechnen lassen */
					PickInfo pocketPickInfo = world.getCollision(botShape, pocket);
					
					if ((pocketPickInfo != null) && (pocketPickInfo.getNode() != null)) {
						/* Kollision ist (auch) innerhalb des Transportfachs */
						botCollision = false;					

						/* Check, ob Kollision auch ausserhalb des Fachs */

						/* Bounds fuer Innenseite erstellen */
						Bounds pocketBack = createBounds(newPosPoint, newHeadAngle, 0, - 0.011, 0.05 / 2, transform);

//						parent.showDebugBox(0.05 / 2, 0.015, 0.04 / 2, transform, newHeadAngle);
//						parent.showDebugSphere(0.05 / 2, transform);

						/* Kollision berechnen lassen */
						PickInfo pocketBackPickInfo = world.getCollision(botShape, pocketBack);

						if ((pocketBackPickInfo != null) && (pocketBackPickInfo.getNode() != null)) {
							botCollision = true;
							objectInPocket = null;
//							lg.info("Kollision im Transportfach und (mindestens) auch dahinter");
						} else {

							/* Bounds fuer Seitenflaeche links erstellen */
							Bounds pocketLeft = createBounds(newPosPoint, newHeadAngle, - 0.045, 0.034775, 0.03 / 2, transform);

//							parent.showDebugBox(0.035 / 2, 0.03955, 0.04 / 2, transform, newHeadAngle);
//							parent.showDebugSphere(0.03 / 2, transform);

							/* Kollision berechnen lassen */
							PickInfo pocketLeftPickInfo = world.getCollision(botShape, pocketLeft);

							if ((pocketLeftPickInfo != null) && (pocketLeftPickInfo.getNode() != null)) {
								botCollision = true;
								collisionInPocket = true;
								objectInPocket = null;
//								lg.info("Kollision im Transportfach und (mindestens) auch links davon");
							} else {					
								/* Bounds fuer Seitenflaeche rechts erstellen */
								Bounds pocketRight = createBounds(newPosPoint, newHeadAngle, 0.045, 0.034775, 0.03 / 2, transform);

//								parent.showDebugBox(0.035 / 2, 0.03955, 0.04 / 2, transform, newHeadAngle);
//								parent.showDebugSphere(0.03 / 2, transform);

								/* Kollision berechnen lassen */
								PickInfo pocketRightPickInfo = world.getCollision(botShape, pocketRight);

								if ((pocketRightPickInfo != null) && (pocketRightPickInfo.getNode() != null)) {
									botCollision = true;
									collisionInPocket = true;
									objectInPocket = null;
//									lg.info("Kollision im Transportfach und rechts davon");
								} else {
//									lg.info("Kollision nur im Transportfach");
									objectInPocket = pocketPickInfo.getNode().getParent();
//									lg.info("objectInPocket=" + objectInPocket.getName());
								}
							}
						}
					} else {
						objectInPocket = null;
					}
				} else {
					objectInPocket = null;
				}

				/* wenn Kollision, Bot entsprechend faerben */
				parent.set(COLLIDED, botCollision);
			}


			/* Bodenkontakt ueberpruefen */
			{
				/* Vektor vom Ursprung zum linken Rad */
				Vector3d vecL = new Vector3d(-newHeading.y, newHeading.x, 0);
				vecL.scale((float) WHEEL_DIST);
				/* neue Position linkes Rad */
				Vector3d posRadL = new Vector3d(parent.getPositionInWorldCoord());
				posRadL.add(vecL);

				/* Vektor vom Ursprung zum rechten Rad */
				Vector3d vecR = new Vector3d(newHeading.y, -newHeading.x, 0);
				vecR.scale((float) WHEEL_DIST);
				/* neue Position rechtes Rad */
				Vector3d posRadR = new Vector3d(parent.getPositionInWorldCoord());
				posRadR.add(vecR);

				/* Transformations-Matrix fuer die Rotation erstellen */
				Transform3D rotation = new Transform3D();
				rotation.rotZ(newHeadAngle);

				/** Abstand Zentrum Gleitpin in Achsrichtung (X) [m] */
				final double BOT_SKID_X = 0d;

				/** Abstand Zentrum Gleitpin in Vorausrichtung (Y) [m] */
				final double BOT_SKID_Y = -0.054d;

				/* Bodenkontakt des Gleitpins ueberpruefen */
				Vector3d skidVec = new Vector3d(BOT_SKID_X, BOT_SKID_Y, -CtBotSimTcp.BOT_HEIGHT / 2);
				/* Position des Gleitpins gemaess der Ausrichtung des Bots anpassen */
				rotation.transform(skidVec);
				skidVec.add(newPosPoint);

				boolean isFalling = ! world.checkTerrain(new Point3d(skidVec), CtBotSimTcp.BOT_GROUND_CLEARANCE);

				/* Bodenkontakt des linken Reifens ueberpruefen */
				posRadL.z -= CtBotSimTcp.BOT_HEIGHT / 2;

				isFalling |= ! world.checkTerrain(new Point3d(posRadL), CtBotSimTcp.BOT_GROUND_CLEARANCE);

				/* Bodenkontakt des rechten Reifens ueberpruefen */
				posRadR.z -= CtBotSimTcp.BOT_HEIGHT / 2;

				isFalling |= ! world.checkTerrain(new Point3d(posRadR), CtBotSimTcp.BOT_GROUND_CLEARANCE);

				/* Wenn einer der Beruehrungspunkte keinen Boden mehr unter
				 * sich hat wird der Bot gestoppt und entsprechend gefaerbt */
				parent.set(IN_HOLE, isFalling);

				if (! parent.is(IN_HOLE) && ! parent.is(COLLIDED)) {
					parent.setPosition(newPosPoint);
				}
	
				if (! parent.is(IN_HOLE) && ! collisionInPocket) {
					parent.setHeading(newHeading);
				} else {
					mouseSensorX.sensor.set(0);
				}
	
				if (parent.is(IN_HOLE) || parent.is(COLLIDED)) {
					mouseSensorY.sensor.set(0);
				}
			}

			/* Uhr aktualisieren */
			clock.setSimTimeInMs((int) world.getSimTimeInMs());
			
			/* Shutdown-Abfrage */
			if (shutdown.shutdownRequested()) {
				parent.dispose();
			}
		}

		/**
		 * @param newPos
		 * @param newHeading
		 * @param dX
		 * @param dY
		 * @param radius 
		 * @param t Transformationsmatrix (wird veraendert)
		 * @return erzeugte Bounds
		 */
		private Bounds createBounds(Point3d newPos, double newHeading, double dX, double dY, double radius, Transform3D t) {
			final double dZ = - CtBotSimTcp.BOT_HEIGHT / 2;
			
			/* Vektor fuer die Verschiebung erstellen */
			Vector3d v = new Vector3d(dX, dY, dZ);

			/* Transformations-Matrix fuer die Rotation erstellen */
			Transform3D r = new Transform3D();
			r.rotZ(newHeading);
			
			/* Transformation um Verschiebung ergaenzen */
			r.transform(v);
			v.add(newPos);
			t.setIdentity();
			t.setTranslation(v);
			
			/* Bounds erstellen */
			Bounds bounds = new BoundingSphere(new Point3d(0, 0, 0), radius);
			
			/* Bounds transformieren */
			bounds.transform(t);
			
			return bounds;
		}

		/**
		 * Setzt den Klappen-Sensor des Bots
		 * @param doorSensor Sensor
		 */
		public void setDoorSensor(Door doorSensor) {
			this.doorSensor = doorSensor;
		}
		
		/**
		 * @return this.objectInPocket
		 */
		public Node getObjectInPocket() {
			return objectInPocket;
		}
		
		/**
		 * @return derzeit assoziiertes Objekt
		 */
		public Node getAssociatedObject() {
			return associatedObject;
		}
		
		/**
		 * @param object zu setzendes Objekt
		 */
		public void setAssociatedObject(Node object) {
			if (object != null) {
				object.setPickable(false);
				TransformGroup tgObject = (TransformGroup) object.getParent().getParent().getParent();
				TransformGroup tgParcours = (TransformGroup) object.getParent().getParent();
				BranchGroup bg = (BranchGroup) tgObject.getParent();
				bg.detach();
				
				Transform3D tWorld = new Transform3D();
				tWorld.setTranslation(new Vector3d(0, 0, 0.2));
				tgParcours.setTransform(tWorld);
				
				Transform3D tPocket = new Transform3D();
				Vector3d diff = new Vector3d(0, 0.036, - CtBotSimTcp.BOT_HEIGHT / 2);
				tPocket.setTranslation(diff);
				tgObject.setTransform(tPocket);
				
				parent.getTransformGroup().addChild(bg);
			} else if (associatedObject != null) {
				Transform3D t = new Transform3D();
				associatedObject.getLocalToVworld(t);
				Point3d center = new Point3d();
				t.transform(center);
				
				TransformGroup tgObject = (TransformGroup) associatedObject.getParent().getParent().getParent();
				BranchGroup bg = (BranchGroup) tgObject.getParent();
				bg.detach();
				
				world.getParcours().createMovableObject((float) center.x, (float) center.y);
			}

			associatedObject = object;
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
    
    /** Servo Klappe */
    protected final ServoSimulator servoDoor = new ServoSimulator();

    /** Maussensor-X */
    protected final MouseSensorSimulator mouseSensorX = new MouseSensorSimulator();
    /** Maussensor-Y */
    protected final MouseSensorSimulator mouseSensorY = new MouseSensorSimulator();
    /** Uhr / Systemzeit */
    private Clock clock;
    /** Shutdown-Control */
    private Shutdown shutdown;

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
     * @param g	Governor
     * @param isLeft links?
     */
    public void buisitWheel(Governor g, boolean isLeft) {
        (isLeft ? leftWheel : rightWheel).setGovernor(g);
    }
    
    /**
     * @param s	Servo
     * @param isLeft links?
     */
    public void buisitServo(DoorServo s, boolean isLeft) {
    	if (isLeft) {
    		servoDoor.setServo(s);
    	}
    }
    
    /**
     * @param doorSensor Klappensensor
     * @param isLeft Servo 1 (links) fuer Klappe
     */
    public void buisitDoor(final Sensors.Door doorSensor, boolean isLeft) {
        final ServoSimulator servo = isLeft ? servoDoor : null;
        krautUndRuebenSim.setDoorSensor(doorSensor);
    	
        simulators.add(new Runnable() {
            @SuppressWarnings("null")
			public void run() {
                final int doorState = servo.getServoPosition() < 12 ? 0 : 1; // 0: Klappe zu; 1: Klappe auf
                final boolean change = doorState != doorSensor.get().intValue();
                if (change) {
                	doorSensor.set(doorState);
                	parent.set(DOOR_OPEN, doorState != 0);
                	
                	if (doorState == 0) {
                		/* Klappe wurde geschlossen */
                		krautUndRuebenSim.setAssociatedObject(krautUndRuebenSim.getObjectInPocket());
                	} else {
                		/* Klappe wurde geoeffnet */
                		krautUndRuebenSim.setAssociatedObject(null);
                	}
                }
            }
        });
    }

    /**
     * @param encoderSensor	Encoder
     * @param isLeft	links?
     */
    public void buisitEncoderSim(final Sensors.Encoder encoderSensor, boolean isLeft) {
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
            private double encoderRest = 0.0;

            public void run() {
                // Anzahl der Umdrehungen der Raeder
                double revs = wheel.revsThisSimStep();

                // Encoder-Schritte als Fliesskommazahl errechnen:
                // Anzahl der Drehungen mal Anzahl der Markierungen,
                // dazu der Rest der letzten Runde
                double tmp = (revs * ENCODER_MARKS) + encoderRest;
                if (encoderSensor.getHasLag()) {
//					if (wheel.getLag() != 0.0) {
//                		System.out.println("lag=" + wheel.getLag());
//                		System.out.println("tmp=" + tmp);
                	tmp += wheel.getLag();
//					}
                }
                // Der Bot bekommt nur ganze Schritte zu sehen,
                int encoderSteps = (int) Math.floor(tmp);
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
    	final double MAX_RANGE = 0.85;
        final Point3d distFromBotCenter = isLeft
            ? new Point3d(- 0.036, 0.0554, 0)
            : new Point3d(+ 0.036, 0.0554, 0);
        final Point3d endPoint = new Point3d(distFromBotCenter);
        endPoint.add(new Point3d(0.0, MAX_RANGE, 0.0));
        final Characteristic charstic = isLeft
            ? new Characteristic("characteristics/gp2d12Left.txt", 100)
            : new Characteristic("characteristics/gp2d12Right.txt", 80);

        simulators.add(new Runnable() {
            private final double OPENING_ANGLE_IN_RAD = Math.toRadians(3);

            public void run() {
                double distInM = world.watchObstacle(
                    parent.worldCoordFromBotCoord(distFromBotCenter),
                    parent.worldCoordFromBotCoord(endPoint),
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
            at(0.004, 0.009, - 0.011 - CtBotSimTcp.BOT_HEIGHT / 2, isLeft),
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
            public void run() {
            	sensor.set(world.sensLight(
            		parent.worldCoordFromBotCoord(distFromBotCenter),
            		parent.worldCoordFromBotCoord(headingInBotCoord),
            		LIGHT_MAX_DISTANCE, LIGHT_OPENING_ANGLE));
            }
        });
    }
    
    /**
	 * @param sensor BPS-Sensor
	 * @param isLeft immer true, denn es gibt nur einen Sensor
	 */
	public void buisitBPSSim(final Sensors.BPSReceiver sensor, final boolean isLeft) {
		if (! isLeft) {
			return; // es gibt nur einen Sensor, wir nehmen den Wert fuer links
		}
		simulators.add(new Runnable() {
			private final Point3d startOfSens = new Point3d();
			private final Point3d endOfSens = new Point3d(BPS_LIGHT_DISTANCE, 0.0, 0.0); // Sensor schaut nach -90 Grad

			public void run() {
				Point3d pos = parent.worldCoordFromBotCoord(startOfSens);				
				pos.setZ(BPS.BPSZ);
				Point3d end = parent.worldCoordFromBotCoord(endOfSens);
				end.setZ(BPS.BPSZ);
				sensor.set(world.sensBPS(pos, end, BPS_OPENING_ANGLE));
			}
		});
	}

    /**
	 * @param sensor Transportfach-Sensor
	 * @param isLeft immer true, denn es gibt nur einen Sensor
	 */
	public void buisitPocketSim(final Sensors.Trans sensor, final boolean isLeft) {
		if (! isLeft) {
			return; // es gibt nur einen Sensor, wir nehmen den Wert fuer links
		}
		final Point3d startOfSens = new Point3d(- 0.025, 0.025, 0);
		final Point3d endOfSens = new Point3d(startOfSens);
		endOfSens.add(new Point3d(0.05, 0, 0));
		
		simulators.add(new Runnable() {
            private final double OPENING_ANGLE_IN_RAD = Math.toRadians(3);

            public void run() {
            	if (krautUndRuebenSim.getAssociatedObject() != null) {
            		sensor.set(1);
            	} else {
	                final double distInM = world.watchObstacle(
	                    parent.worldCoordFromBotCoord(startOfSens),
	                    parent.worldCoordFromBotCoord(endOfSens),
	                    OPENING_ANGLE_IN_RAD,
	                    parent.getShape());
                	sensor.set(distInM < 100 ? 1 :0);
            	}
            }
		});
	}
	
    /**
     * @param sensor Maussensor
     * @param isX X? (sonst Y)
     */
    public void buisitMouseSensorSim(final Sensors.Mouse sensor, boolean isX) {
        (isX ? mouseSensorX : mouseSensorY).setSensor(sensor);
    }

    /**
     * @param sensor Clock
     */
    public void buisitClockSim(final Sensors.Clock sensor) {
        clock = sensor;
    }
    
    /**
     * @param sensor Shutdown-Control
     */
    public void buisitShutdownSim(final Sensors.Shutdown sensor) {
    	shutdown = sensor;
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
    
    /**
     * Destruktor
     */
    public void cleanup() {
    	krautUndRuebenSim.setAssociatedObject(null); // steckt ein eingeladenes Objekt wieder in die Welt
    }
}