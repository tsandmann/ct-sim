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
package ctSim.model;

import static ctSim.model.ThreeDBot.Coord.X;
import static ctSim.model.ThreeDBot.Coord.Y;
import static ctSim.model.ThreeDBot.Coord.Z;
import static ctSim.model.ThreeDBot.State.COLLIDED;
import static ctSim.model.ThreeDBot.State.HALTED;
import static ctSim.model.ThreeDBot.State.IN_HOLE;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point2i;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Sphere;

import ctSim.controller.BotBarrier;
import ctSim.controller.Config;
import ctSim.model.bots.BasicBot;
import ctSim.model.bots.BotBuisitor;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.SimulatedBot.UnrecoverableScrewupException;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.ctbot.CtBotShape;
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.model.bots.ctbot.MasterSimulator;
import ctSim.util.Runnable1;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * <p>
 * Klasse fuer alle Bots, die eine 3D-Darstellung haben (= simulierte Bots, fuer
 * reale ist das unnoetig). Fungiert als Wrapper um eine {@link SimulatedBot}-Instanz,
 * d.h. diese Klasse hat eine Referenz auf einen {@code SimulatedBot} und ist
 * selbst ein Bot.
 * </p>
 * <p>
 * Die beiden Methoden, die fuer die Simulation zentral sind:
 * <ul>
 * <li>{@link #run()}, die als eigener Thread laeuft. Sie macht periodisch zwei
 * Dinge: Warten und dem SimulatedBot sagen "jetzt Simschritt machen" (fuer den
 * {@link CtBotSimTcp} heisst das er uebertraegt Sensordaten, wartet auf Antwort
 * vom C-Code, und aktualisiert dann die Aktuatoren wie vom C-Code gewuenscht)</li>
 * <li>{@link #updateSimulation(long)}, die von aussen aufgerufen wird. Die
 * Methode betrachtet die Aktuator-Werte (z.B. Motorgeschwindigkeit) und nimmt
 * an der Simulation die relevanten Aenderungen vor (im Beispiel: eine
 * Positions-/Drehungsaenderung). Ausserhalb dieser Klasse wird sichergestellt,
 * dass {@code updateSimulation()} immer dann aufgerufen wird, wenn der Thread
 * dieser Klasse gerade wartet, nicht wenn er gerade einen Simschritt macht.
 * </li>
 * </ul>
 * </p>
 *
 * @author Benjamin Benz (bbe@ctmagazin.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ThreeDBot extends BasicBot implements Runnable {
	/** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.model.ThreeDBot");

	/** Liste aller Appearance-Listener */
	private final List<Runnable1<Color>> appearanceListeners = Misc.newList();

	/**
	 * Bot-Status
	 */
	public enum State {
		/** Der Bot ist kollidiert (mit der Wand oder mit einem anderen Bot) */
		COLLIDED(0x001, "collision",
			"kollidiert",
			"ist nicht mehr kollidiert"),

		/**
		 * Der Bot haengt in einem Loch, d.h. eins der Raeder ist in eine Grube
		 * gerutscht. In diesem State kann sich der Bot noch drehen, aber nicht
		 * mehr bewegen.
		 */
		IN_HOLE(0x002, "falling",
			"hat keinen Boden mehr unter den F\u00FC\u00DFen",
			"hat wieder Boden unter den F\u00FC\u00DFen"),
			
		/**
		 * Klappenzustand des Bots
		 */
		DOOR_OPEN(0x003, "door_open",
			"Klappe ist nun geoeffnet",
			"Klappe ist nun geschlossen"),

		/**
		 * Der Bot ist von der weiteren Simulation ausgeschlossen, d.h. seine
		 * work()-Methode wird nicht mehr aufgerufen. Es steht damit nur noch
		 * rum, bis die Simulation irgendwann endet. Dieser Zustand kann
		 * eintreten, wenn die TCP-Verbindung abrei&szlig;t (Bot-Code
		 * abgestuerzt) oder ein anderer I/O-Fehler auftritt.
		 */
		HALTED(0x100, "halted",
			"wird aus der Simulation ausgeschlossen");

		/** Legacy-Status */
		final int legacyValue;
		/** Appearance als XML */
		final String appearanceKeyInXml;
		/** Enter-Message */
		final String messageOnEnter;
		/** Exit-Message */
		final String messageOnExit;

		/**
		 * Bot-Status
		 * @param legacyValue
		 * @param appearanceKeyInXml
		 * @param messageOnEnter
		 */
		State(int legacyValue, String appearanceKeyInXml, String messageOnEnter) {
			this(legacyValue, appearanceKeyInXml, messageOnEnter, null);
		}

		/**
		 * Bot-Status
		 * @param legacyValue
		 * @param appearanceKeyInXml
		 * @param messageOnEnter
		 * @param messageOnExit
		 */
		State(int legacyValue, String appearanceKeyInXml, String messageOnEnter, String messageOnExit) {
			this.legacyValue = legacyValue;
			this.appearanceKeyInXml = appearanceKeyInXml;
			this.messageOnEnter = messageOnEnter;
			this.messageOnExit = messageOnExit;
		}
	}

	/**
	 * 3D-Koordinaten eines Bots
	 */
	enum Coord { 
		/** X-Anteil */
		X,
		/** Y-Anteil */
		Y, 
		/** Z-Anteil */
		Z 
	}

	/**
	 * Positionskomponente fuer 3D-Bots 
	 */
	public class PositionCompnt extends BotComponent<SpinnerNumberModel> {
		/** Koordinaten */
		private final Coord coord;

		/**
		 * Erzeugt eine neue Position
		 * @param coord	Koordinaten
		 */
		public PositionCompnt(final Coord coord) {
			super(new SpinnerNumberModel());
			this.coord = coord;

			updateExternalModel(); // Initialen Wert setzen
			getExternalModel().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					double newValue = getExternalModel().getNumber().doubleValue();
					Point3d p = getPositionInWorldCoord();
					switch (coord) {
						case X:   p.x = newValue; break;
						case Y:   p.y = newValue; break;
						case Z:   p.z = newValue; break;
					}
					setPosition(p);
				}
			});
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#isGuiEditable()
		 */
		@Override
		public boolean isGuiEditable() {
			return true;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			return coord + " [m]";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return coord + "-Koordinate [m]";
		}

		/**
		 * Aktualisiert das externe Modell
		 */
		@Override
		public void updateExternalModel() {
			double newValue = 0;
			Point3d p = getPositionInWorldCoord();
			switch (coord) {
				case X:   newValue = p.x; break;
				case Y:   newValue = p.y; break;
				case Z:   newValue = p.z; break;
			}
			getExternalModel().setValue(newValue);
		}
	}
	
	/**
	 * Globalen Bot-Position (wie fuer Lokalisierung verwendet)
	 */
	public class PositionGlobal extends BotComponent<SpinnerNumberModel> {
		/** Koordinaten */
		private final Coord coord;
		
		/**
		 * Erzeugt eine neue Position
		 * @param coord	Koordinaten
		 */
		public PositionGlobal(final Coord coord) {
			super(new SpinnerNumberModel());
			this.coord = coord;
		}
		
		/**
		 * @see ctSim.model.bots.components.BotComponent#isGuiEditable()
		 */
		@Override
		public boolean isGuiEditable() {
			return false;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			return coord +" [mm]";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Globale" + coord + "-Koordinate [mm]";
		}
		
		/**
		 * Aktualisiert das externe Modell
		 */
		@Override
		public void updateExternalModel() {
			double newValue = 0;
			Point2i p = bot.getController().getWorld().transformWorldposToGlobalpos(getPositionInWorldCoord());
			switch (coord) {
				case X: newValue = p.x; break;
				case Y: newValue = p.y; break;
				case Z: break;
			}
			getExternalModel().setValue(newValue);
		}
	}

	/**
	 * Blickrichtung eines 3D-Bots
	 */
	public class HeadingCompnt extends BotComponent<SpinnerNumberModel> {
		/** Flag fuer Status-Aenderung ignorieren */
		protected boolean ignoreStateChange = false;

		/**
		 * Erzeugt eine neue Blickrichtung fuer einen 3D-Bot
		 */
		public HeadingCompnt() {
			super(new SpinnerNumberModel());

			updateExternalModel(); // Initialen Wert setzen
			getExternalModel().addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					/*
					 * $$ ignoreStateChange: setHeading() sollte erkennen, wann
					 * ein Aufruf ueberfluessig ist (weil das neue Heading sich
					 * nicht vom alten unterscheidet). Wegen der doofen Sache,
					 * dass Headings auf zwei Arten ausgedrueckt werden koennen
					 * (Vector3d, double), funktioniert die Erkennung nicht gut.
					 * Daher braucht wir ignoreStateChange. Wenn Heading mal
					 * komplett auf double umgestellt ist, ist ignoreStateChange
					 * ueberfluessig
					 */
					if (ignoreStateChange) {
						return;
					}
					double newValueDeg = getExternalModel().getNumber().doubleValue();
					getExternalModel().setValue(Misc.normalizeAngleDeg(newValueDeg));
					setHeading(Math.toRadians(newValueDeg));
				}
			});
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#isGuiEditable()
		 */
		@Override
		public boolean isGuiEditable() {
			return true;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			// Unicode 00B0: Grad-Zeichen
			return "Richtung [\u00B0]";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			// Unicode 00B0: Grad-Zeichen
			return "Richtung, in die der Bot blickt, gemessen in Grad; " +
					"Blick nach Norden = 0\u00B0; " +
					"Blick nach Westen = +90\u00B0";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public void updateExternalModel() {
			ignoreStateChange = true;
			getExternalModel().setValue(Math.toDegrees(getHeadingInRad()));
			ignoreStateChange = false;
		}
	}
	
	/**
	 * Globale Blickrichtung (wie fuer Lokalisierung verwendet)
	 */
	public class HeadingGlobal extends BotComponent<SpinnerNumberModel> {
		/**
		 * Erzeugt eine neue Blickrichtung fuer einen 3D-Bot
		 */
		public HeadingGlobal() {
			super(new SpinnerNumberModel());
			updateExternalModel(); // Initialen Wert setzen
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#isGuiEditable()
		 */
		@Override
		public boolean isGuiEditable() {
			return false;
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getName()
		 */
		@Override
		public String getName() {
			// Unicode 00B0: Grad-Zeichen
			return "Richtung [\u00B0]";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#getDescription()
		 */
		@Override
		public String getDescription() {
			return "Richtung, in die der Bot blickt, gemessen in Grad";
		}

		/**
		 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
		 */
		@Override
		public void updateExternalModel() {
			double heading = Math.toDegrees(getHeadingInRad());
			if (heading < 0.0) {
				heading += 360.0;
			}
			getExternalModel().setValue(heading);
		}
	}

	/** Bot-Stati */
	private final EnumSet<State> obstState = EnumSet.noneOf(State.class);

	/** Position */
	private Point3d posInWorldCoord = new Point3d();

	/**
	 * Letzte Position, an der der Bot nicht kollidiert oder ins Loch gefallen
	 * war. Wird verwendet zur Berechnung des Abstands zum Ziel, was auch dann
	 * funktionieren soll, wenn der Bot z.B. in ein Loch gefallen ist. Daher
	 * wird in dieser Variablen die letzte Position gehalten, wo der Bot noch
	 * au&szlig;erhalb des Lochs war.
	 */
	private Point3d lastSafePos = new Point3d();

	/** Heading */
	private Vector3d headingInWorldCoord = new Vector3d();

	/**
	 * Unser Teil des J3D-Szenegraph:
	 *
	 * <pre>
	 * BranchGroup-Instanz
	 *
	 *      |
	 *      | hat Kind
	 *      v
	 *
	 * TransformGroup-Instanz
	 *
	 *      |
	 *      | hat Kind
	 *      v
	 *
	 *    shape
	 * </pre>
	 */
	private final BranchGroup branchgrp;
	/** enthaelt Anzeigeelemente zu Debug-Zwecken */
	private final BranchGroup testBG;
	/** Transformgroup */
	private final TransformGroup transformgrp;
	/** Shape */
	private final Group shape;

	/** Thread des Bots */
	private Thread thrd;

	/** Simultime beim letzten Aufruf */
	private long lastSimulTime = 0;

	/** Zeit zwischen letztem Aufruf von UpdateSimulation und jetzt*/
	private long deltaT = 0;

	/** Barrier des Bots */
	private final BotBarrier barrier;

	/** Referenz auf Sim-Bot-Instanz */
	private final SimulatedBot bot;

	/** Simulator des Bots */
	private Runnable simulator;

	/**
	 * @param posInWorldCoord Position des Objekts
	 * @param headInWorldCoord Blickrichtung des Objekts
	 * @param barrier 	Barrier fuer den neuen Bot
	 * @param bot		Zugehoeriger Bot 
	 */
	public ThreeDBot(Point3d posInWorldCoord, Vector3d headInWorldCoord, BotBarrier barrier, SimulatedBot bot) {
		super(bot.toString());
		Color normalColor = Config.getBotColor(bot.getClass(), bot.getInstanceNumber(), "normal");
		shape = new CtBotShape(normalColor, this);
		this.barrier = barrier;
		this.bot = bot;

		/* TransformGroup fuer den Bot */
		transformgrp = new TransformGroup();
		transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		transformgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);
		transformgrp.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		/* jetzt wird noch alles nett verpackt */
		branchgrp = new BranchGroup();
		branchgrp.setCapability(BranchGroup.ALLOW_DETACH);
		branchgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);
		branchgrp.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		branchgrp.setCapability(Node.ALLOW_PICKABLE_WRITE);
		branchgrp.addChild(transformgrp);
		
		/* BranchGroup fuer Debug-Anzeigen */
		testBG = new BranchGroup();
		testBG.setCapability(BranchGroup.ALLOW_DETACH);
		testBG.setCapability(Group.ALLOW_CHILDREN_WRITE);
		testBG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		testBG.setPickable(false);
		branchgrp.addChild(testBG);
		
		transformgrp.addChild(shape);

		setPosition(posInWorldCoord);
		setHeading(headInWorldCoord);
		updateAppearance();

		// Die holen sich die Position in ihren Konstruktoren, daher muss die
		// schon gesetzt sein
		components.add(new PositionCompnt(X), new PositionCompnt(Y), new PositionCompnt(Z), new HeadingCompnt());
		
		if (Config.getValue("BPSSensor").equals("true")) {
			components.add(new PositionGlobal(X), new PositionGlobal(Y), new HeadingGlobal());
		}
		
		addDisposeListener(new Runnable() {
			@Override
			public void run() {
				((MasterSimulator) simulator).cleanup();
			}
		});
	}

	
	/**
	 * Setzt den Simulator des Bots
	 * @param simulator	Simulator
	 */
	public void setSimulator(Runnable simulator) {
		this.simulator = simulator;
	}

	/**
	 * @return Die 3D-Gestalt des Objekts
	 */
	public final Group getShape() {
		return shape;
	}

	/**
	 * Liefert die BranchGroup
	 * @return	BG
	 */
	public final BranchGroup getBranchGroup() {
		return branchgrp;
	}
	
	/**
	 * @return TG des Bots
	 */
	public final TransformGroup getTransformGroup() {
		return transformgrp;
	}
	
	/**
	 * @param relTrans
	 * @param comp
	 */
	public final void addBranchComponent(Transform3D relTrans, Node comp) {
		TransformGroup tg = new TransformGroup();
		tg.setTransform(relTrans);
		tg.addChild(comp);

		transformgrp.addChild(tg);
	}
	
	/**
	 * Loescht alle Elemente in TestBG (Branchgroup zu Debug-Zwecken)
	 */
	public void clearDebugBG() {
		testBG.removeAllChildren();
	}
	
	/**
	 * Zeichnet eine Kugel zu Debug-Zwecken, indem sie zu TestBG hinzugefuegt wird
	 * @param radius Radius der Kugel
	 * @param transform Transformation, die auf die Box angewendet werden soll
	 */
	public void showDebugSphere(final double radius, Transform3D transform) {
		final Sphere sphare = new Sphere((float) radius);
		TransformGroup tg = new TransformGroup();
		tg.setTransform(transform);
		tg.addChild(sphare);
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.addChild(tg);
		testBG.addChild(bg);
	}

	/**
	 * Zeichnet eine Box zu Debug-Zwecken, indem sie zu TestBG hinzugefuegt wird
	 * @param x Groesse in X-Richtung
	 * @param y Groesse in Y-Richtung
	 * @param z Groesse in Z-Richtung
	 * @param transform Transformation, die auf die Box angewendet werden soll
	 * @param angle Winkel, um den die Box gedreht werden soll
	 */
	public void showDebugBox(final double x, final double y, final double z, Transform3D transform, double angle) {
		final Box box = new Box((float) x, (float) y, (float) z, null);
		transform.setRotation(new AxisAngle4d(0, 0, 1, angle));
		TransformGroup tg = new TransformGroup();
		tg.setTransform(transform);
		tg.addChild(box);
		BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.addChild(tg);
		testBG.addChild(bg);
	}

	/**
	 * Startet den Bot (bzw. dessen Thread).
	 */
	public final void start() {
		thrd = new Thread(this, "ctSim-"+toString());
		addDisposeListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				if (thrd != null) {
					lg.fine("Stoppe Thread " + thrd.getName());
					Thread t = thrd;
					thrd = null;
					t.interrupt();
				}
			}
		});

		thrd.start();
		lg.fine("Thread " + thrd.getName() + " gestartet");
	}

	/**
	 *  Stoppt den Bot (bzw. dessen Thread).
	 */
	@Override
	public void dispose() {
		super.dispose(); // Unsere DisposeListener
		bot.dispose(); // und die vom Wrappee
	}

	/**
	 * @return Gibt die Position zurueck
	 */
	public final Point3d getPositionInWorldCoord() {
		return new Point3d(posInWorldCoord);
	}

	/**
	 * @return Heading als RAD
	 */
	public final double getHeadingInRad() {
		return radiansToYAxis(headingInWorldCoord);
	}

	/**
	 * @return Heading als Welt-Koordinaten
	 */
	public final Vector3d getHeadingVectorInWorldCoord() {
		return new Vector3d(headingInWorldCoord);
	}

	/**
	 * @param posInWorldCoord Die Position, an die der Bot gesetzt werden soll
	 */
	public final synchronized void setPosition(Point3d posInWorldCoord) {
		// Optimierung (Transform-Kram ist teuer)
		if (this.posInWorldCoord.equals(posInWorldCoord)) {
			return;
		}

		this.posInWorldCoord = posInWorldCoord;

		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.setTranslation(new Vector3d(posInWorldCoord));
		transformgrp.setTransform(t);

		if (! is(COLLIDED) && ! is(IN_HOLE)) {
			lastSafePos.set(posInWorldCoord);
		}
	}

	/**
	 * Setzt Heading
	 * @param headingInRad Heading als RAD
	 */
	public final synchronized void setHeading(double headingInRad) {
		setHeading(vectorFromAngle(headingInRad));
	}

	/**
	 * @param headingInRad	Heading als RAD
	 * @return	Heading als Vektor
	 */
	public static Vector3d vectorFromAngle(double headingInRad) {
		headingInRad = Misc.normalizeAngleRad(headingInRad);
		return new Vector3d(- Math.sin(headingInRad), + Math.cos(headingInRad), 0);
	}

	/**
	 * Setzt Heading
	 * @param headingInWorldCoord	Heading in Welt-Koordinaten
	 */	
	public final synchronized void setHeading(Vector3d headingInWorldCoord) {
		// Optimierung (Transform-Kram ist teuer)
		if (this.headingInWorldCoord.equals(headingInWorldCoord)) {
			return;
		}

		/*
		 * Sinn der Methode: Transform3D aktualisieren, das von Bot- nach
		 * Weltkoordinaten transformiert. (Dieses steckt in unserer
		 * TransformGroup.)
		 */
		this.headingInWorldCoord = headingInWorldCoord;

		// Winkel zwischen Welt pos. y-Achse und Bot pos. y-Achse
		double angleInRad = radiansToYAxis(headingInWorldCoord);

		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.setRotation(new AxisAngle4d(0, 0, 1, angleInRad));
		transformgrp.setTransform(t);
	}

	/**
	 * <p>
	 * Winkel zu einem Vektor:
	 *
	 * <pre>
	 *          0
	 *          .
	 *          .
	 * +&pi;/2 . . . . . &minus;&pi;/2
	 *          .
	 *          .
	 *         +&pi;
	 * </pre>
	 *
	 * </p>
	 * <p>
	 * Liefert den Winkel zwischen positiver y-Achse der Welt und
	 * &uuml;bergebenem Vektor. Ergebnis ist im Bogenma&szlig; und liegt im
	 * Intervall ]&minus;&pi;; +&pi;]. Gemessen im Gegenuhrzeigersinn
	 * ("mathematisch positiver Drehsinn"), d.h. positive Winkel liegen links,
	 * wenn man in Richtung der y-Achse guckt. Ein Vektor, der exakt in Richtung
	 * der negativen y-Achse zeigt, produziert +&pi; als Ergebnis.
	 * </p>
	 * @param v Vektor
	 * @return Winkel
	 */
	private static double radiansToYAxis(Vector3d v) {
		double rv = v.angle(new Vector3d(0, 1, 0));
		if (v.x > 0) {
			rv = -rv;
		}
		return rv;
	}

	/**
	 * 
	 * @param state
	 * @param setOrClear
	 */
	public void set(State state, boolean setOrClear) {
		if (setOrClear) {
			set(state);
		} else {
			clear(state);
		}
	}

	/**
	 * Fuegt einen Status hinzu
	 * @param state Status
	 */
	public void set(State state) {
		if (obstState.add(state)) {
			lg.info(toString() + " " + state.messageOnEnter);
			updateAppearance();
		}
	}

	/**
	 * Entfernt Status
	 * @param state Status
	 */
	public void clear(State state) {
		if (obstState.remove(state)) {
			lg.info(toString() + " " + state.messageOnExit);
			updateAppearance();
		}
	}

	/**
	 * @param s	Status
	 * @return true, falls s in obstState
	 */
	public boolean is(State s) {
		return obstState.contains(s);
	}

	/**
	 * @return obstState.isEmpty()
	 */
	private boolean isObstStateNormal() {
		return obstState.isEmpty();
	}

	// Gemaess dbfeld-ctsim-log-state.txt
	/**
	 * @return log-state
	 */
	public int getLegacyObstState() {
		int rv = 0;
		for (State s : obstState) {
			rv += s.legacyValue;
		}
		return rv;
	}

	/**
	 * &Uuml;berschreibt die run()-Methode aus der Klasse Thread und arbeitet in
	 * einer Endlosschleife zwei Schritte ab:
	 * <ul>
	 * <li>auf unserem SimulatedBot
	 * {@link SimulatedBot#doSimStep() doSimStep()} aufrufen</li>
	 * <li>{@link #updateView()} aufrufen</li>
	 * </ul>
	 * Die Schleife l&auml;uft so lang, bis sie von der Methode
	 * {@link #dispose()} beendet wird.
	 */
	public final void run() {
		Thread thisThread = Thread.currentThread();

		try {
			while (this.thrd == thisThread) {
				if (! is(HALTED)) {
					try {
						bot.doSimStep();
						updateView();
					} catch (UnrecoverableScrewupException e) {
						dieOrHalt();
					}
				}

				if (thrd != null) {
					barrier.awaitNextSimStep();
				}
			}
		} catch(InterruptedException ie) {
			// No-op: nochmal Meldung ausgeben, dann Ende
		}
		lg.fine("Thread " + Thread.currentThread().getName() + " wurde beendet");
	}

	/** Implementiert Bug 39 (http://www.heise.de/trac/ctbot/ticket/39) */
	private void dieOrHalt() {
		String eh = Config.getValue("simBotErrorHandling");
		String warning = toString()+" hat ein E/A-Problem: " + "Bot-Code ist wohl abgestuerzt; ";
		if ("kill".equals(eh)) {
			lg.warn(warning+"entferne Bot");
			dispose();
		} else if ("halt".equals(eh)) {
			lg.warn(warning+"Bot ist steckengeblieben");
			set(HALTED);
		}
	}

	
	/**
	 * @see ctSim.model.bots.BasicBot#updateView()
	 */
	@Override
	public void updateView() throws InterruptedException {
		super.updateView();	// Positionsanzeige updaten
		bot.updateView(); // Anzeige der Bot-Komponenten updaten
	}

	/**
	 * Aktualisiert die Bot-Ansicht
	 */
	private void updateAppearance() {
		String key;
		if (isObstStateNormal()) {
			key = "normal";
		} else {
			// appearanceKey des ersten gesetzten Elements
			// Im Fall, dass mehr als ein ObstState gesetzt ist (z.B. COLLIDED
			// und zugleich HALTED), werden alle ignoriert ausser dem ersten
			key = obstState.iterator().next().appearanceKeyInXml;
		}

		Color c = Config.getBotColor(bot.getClass(), bot.getInstanceNumber(), key);

		for (Runnable1<Color> listener : appearanceListeners) {
			listener.run(c);
		}
	}

	/**
	 * Diese Methode wird von au&szlig;en aufgerufen und erledigt die
	 * Aktualisierung der Simulation: Bot-Position weitersetzen je nach dem,
	 * wie schnell die Motoren gerade drehen usw.
	 *
	 * @param simTimeInMs Aktuelle Simulation in Millisekunden
	 */
	public void updateSimulation(long simTimeInMs) {
		if (is(HALTED)) { // Fix fuer Bug 44
			return;
		}
		
		/* Zeit aktualisieren */
		deltaT = simTimeInMs - lastSimulTime;
		if (lastSimulTime == 0) {
			sendRcStartCode();
		}
		lastSimulTime = simTimeInMs;

		/* Simulatoren des Bots ausfuehren */
		simulator.run(); 
	}

	/**
	 * Liefert die Sim-Zeit, die verstrichen ist seit dem vorigen Aufruf von
	 * updateSimulation().
	 *
	 * @return Delta-T in Millisekunden
	 */
	public long getDeltaTInMs() {
		return deltaT;
	}

	/**
	 * Liefert die letzte als sicher erachtete Position des Bots zurueck
	 * @return Returns the lastSavePos.
	 */
	public Point3d getLastSafePos() {
		return lastSafePos;
	}

	/**
	 * Rechnet Bot-Koordinaten in Welt-Koordinaten um
	 * @param inBotCoord	Bot-Koordis als Point
	 * @return				Welt-Koordis
	 */
	public Point3d worldCoordFromBotCoord(Point3d inBotCoord) {
		Point3d rv = (Point3d)inBotCoord.clone();
		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.transform(rv);
		return rv;
	}

	/**
	 * Rechnet Bot-Koordinaten in Welt-Koordinaten um
	 * @param inBotCoord	Bot-Koordis als Vektor
	 * @return				Welt-Koordis
	 */
	public Vector3d worldCoordFromBotCoord(Vector3d inBotCoord) {
		Vector3d rv = (Vector3d) inBotCoord.clone();
		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.transform(rv);
		return rv;
	}

	/**
	 * @see ctSim.model.bots.BasicBot#toString()
	 */
	@Override
	public String toString() {
		return bot.toString();
	}

	/**
	 * Setzt einen Handler fuer geandertes Aussehen
	 * @param calledWhenObstStateChanged
	 */
	public void addAppearanceListener(Runnable1<Color> calledWhenObstStateChanged) {
		if (calledWhenObstStateChanged == null) {
			throw new NullPointerException();
		}
		appearanceListeners.add(calledWhenObstStateChanged);
	}

	/**
	 * @see ctSim.model.bots.BasicBot#getDescription()
	 */
	@Override
	public String getDescription() {
		return bot.getDescription();
	}

	/**
	 * @see ctSim.model.bots.BasicBot#getInstanceNumber()
	 */
	@Override
	public int getInstanceNumber() {
		return bot.getInstanceNumber();
	}

	/**
	 * @see ctSim.model.bots.BasicBot#accept(ctSim.model.bots.BotBuisitor)
	 */
	@Override
	public void accept(BotBuisitor buisitor) {
		super.accept(buisitor);
		bot.accept(buisitor);
	}

	/**
	 * @return SimulatedBot-Instanz
	 */
	public SimulatedBot getSimBot() {
		return bot;
	}

	/**
	 * sendet Fernbedienungs-Startcode an den einen (TCP-)c't-Bot
	 */
	public void sendRcStartCode() {
		if (bot instanceof CtBotSimTcp) {
			((CtBotSimTcp)bot).sendRcStartCode();
		}
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_log()
	 */
	@Override
	public boolean get_feature_log() {
		return false;
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_rc5()
	 */
	@Override
	public boolean get_feature_rc5() {
		return false;
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_abl_program()
	 */
	@Override
	public boolean get_feature_abl_program() {
		return false;
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_basic_program()
	 */
	@Override
	public boolean get_feature_basic_program() {
		return false;
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_map()
	 */
	@Override
	public boolean get_feature_map() {
		return false;
	}

	/**
	 * @see ctSim.model.bots.Bot#get_feature_remotecall()
	 */
	@Override
	public boolean get_feature_remotecall() {
		return false;
	}
}
