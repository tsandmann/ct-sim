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
import static ctSim.model.ThreeDBot.ObstState.COLLIDED;
import static ctSim.model.ThreeDBot.ObstState.HALTED;
import static ctSim.model.ThreeDBot.ObstState.IN_HOLE;

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
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.controller.BotBarrier;
import ctSim.controller.Config;
import ctSim.model.bots.BasicBot;
import ctSim.model.bots.Bot;
import ctSim.model.bots.BotBuisitor;
import ctSim.model.bots.SimulatedBot;
import ctSim.model.bots.SimulatedBot.UnrecoverableScrewupException;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.ctbot.CtBotShape;
import ctSim.util.Runnable1;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * Klasse fuer alle Hindernisse die sich selbst bewegen koennen
 *
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */
public class ThreeDBot extends BasicBot implements Bot, Runnable {
	final FmtLogger lg = FmtLogger.getLogger("ctSim.model.AliveObstacle");

	private final List<Runnable1<Color>> appearanceListeners = Misc.newList();

	public enum ObstState {
		/** Das Hindernis hat eine Kollision */
		COLLIDED(0x001, "collision",
			"kollidiert",
			"ist nicht mehr kollidiert"),

		IN_HOLE(0x002, "falling",
			"hat keinen Boden mehr unter den F\u00FC\u00DFen",
			"hat wieder Boden unter den F\u00FC\u00DFen"),

		/**
		 * <p>
		 * Das Obstacle ist von der weiteren Simulation ausgeschlossen, d.h.
		 * seine work()-Methode wird nicht mehr aufgerufen. Es steht damit nur
		 * noch rum, bis die Simulation irgendwann endet. Dieser Zustand kann
		 * eintreten, wenn &ndash;
		 * <ul>
		 * <li>die work()-Methode l&auml;nger rechnet, als der
		 * AliveObstacleTimeout in der Konfigdatei erlaubt, oder</li>
		 * <li>wenn die TCP-Verbindung eines CtBotSimTcp abrei&szlig;t (d.h.
		 * der CtBotSimTcp gibt sich dann diesen Status).</li>
		 * </ul>
		 * </p>
		 */
		HALTED(0x100, "halted",
			"wird aus der Simulation ausgeschlossen");

		final int legacyValue;
		final String appearanceKeyInXml;
		final String messageOnEnter;
		final String messageOnExit;

		ObstState(int legacyValue, String appearanceKeyInXml,
		String messageOnEnter) {
			this(legacyValue, appearanceKeyInXml, messageOnEnter, null);
		}

		ObstState(int legacyValue, String appearanceKeyInXml,
		String messageOnEnter, String messageOnExit) {
			this.legacyValue = legacyValue;
			this.appearanceKeyInXml = appearanceKeyInXml;
			this.messageOnEnter = messageOnEnter;
			this.messageOnExit = messageOnExit;
		}
	}

	enum Coord { X, Y, Z }

	public class PositionCompnt extends BotComponent<SpinnerNumberModel> {
		private final Coord coord;

		public PositionCompnt(final Coord coord) {
			super(new SpinnerNumberModel());
			this.coord = coord;

			updateExternalModel(); // Initialen Wert setzen
			getExternalModel().addChangeListener(new ChangeListener() {
				public void stateChanged(
				@SuppressWarnings("unused") ChangeEvent e) {
					double newValue = getExternalModel().getNumber()
						.doubleValue();
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

		@Override
		public boolean isGuiEditable() {
			return true;
		}

		@Override
		public String getName() {
			return coord+" [m]";
		}

		@Override
		public String getDescription() {
			return coord+"-Koordinate in Meter";
		}

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

	public class HeadingCompnt extends BotComponent<SpinnerNumberModel> {
		protected boolean ignoreStateChange = false;

		public HeadingCompnt() {
			super(new SpinnerNumberModel());

			updateExternalModel(); // Initialen Wert setzen
			getExternalModel().addChangeListener(new ChangeListener() {
				public void stateChanged(
				@SuppressWarnings("unused") ChangeEvent e) {
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
					if (ignoreStateChange)
						return;
					double newValueDeg = getExternalModel().getNumber()
						.doubleValue();
					getExternalModel().setValue(Misc.normalizeAngleDeg(
						newValueDeg));
					setHeading(Math.toRadians(newValueDeg));
				}
			});
		}

		@Override
		public boolean isGuiEditable() {
			return true;
		}

		@Override
		public String getName() {
			return "Richtung [°]";
		}

		@Override
		public String getDescription() {
			return "Richtung, in die der Bot blickt, gemessen in Grad; " +
					"Blick nach Norden = 0°; Blick nach Westen = +90°";
		}

		@Override
		public void updateExternalModel() {
			ignoreStateChange = true;
			getExternalModel().setValue(Math.toDegrees(getHeadingInRad()));
			ignoreStateChange = false;
		}
	}

	private final EnumSet<ObstState> obstState = EnumSet.noneOf(
		ObstState.class);

	private Point3d posInWorldCoord = new Point3d();

	/**
	 * Letzte Position, an dem sich das AliveObstacle befand und dabei der
	 * obstState &quot;SAFE&quot; war. Sinn: Dieses Feld wird verwendet zur
	 * Berechnung des Abstands zum Ziel, was auch funktionieren soll, wenn der
	 * Bot z.B. in ein Loch gefallen ist (d.h. jetzt un-&quot;SAFE&quot; ist).
	 * Daher wird in dieser Variablen die letzte Position gehalten, wo der Bot
	 * noch au&szlig;erhalb des Lochs war.
	 */
	private Point3d lastSafePos = new Point3d();

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
	private final TransformGroup transformgrp;
	private final Group shape;

	private Thread thrd;

	/** Simultime beim letzten Aufruf */
	// TODO
	private long lastSimulTime = 0;

	/** Zeit zwischen letztem Aufruf von UpdateSimulation und jetzt*/
	// TODO
	private long deltaT = 0;

	private final BotBarrier barrier;

	private final SimulatedBot bot;

	private Runnable simulator;

	/**
	 * @param posInWorldCoord Position des Objekts
	 * @param headInWorldCoord Blickrichtung des Objekts
	 */
	public ThreeDBot(Point3d posInWorldCoord, Vector3d headInWorldCoord,
	BotBarrier barrier,	SimulatedBot bot) {
		super(bot.toString());
		Color normalColor = Config.getBotColor(bot.getClass(),
			bot.getInstanceNumber(), "normal");
		this.shape = new CtBotShape(normalColor, this);
		this.barrier = barrier;
		this.bot = bot;

		// Translationsgruppe fuer das Obst
		transformgrp = new TransformGroup();
		transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		transformgrp.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		transformgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);

		// Jetzt wird noch alles nett verpackt
		branchgrp = new BranchGroup();
		branchgrp.setCapability(BranchGroup.ALLOW_DETACH);
		branchgrp.setCapability(Group.ALLOW_CHILDREN_WRITE);
		branchgrp.addChild(transformgrp);

		transformgrp.addChild(shape);

		setPosition(posInWorldCoord);
		setHeading(headInWorldCoord);
		updateAppearance();

		// Die holen sich die Position in ihren Konstruktoren, daher muss die
		// schon gesetzt sein
		components.add(
			new PositionCompnt(X),
			new PositionCompnt(Y),
			new PositionCompnt(Z),
			new HeadingCompnt()
		);
	}

	//$$$ Rest der Konstruktion (koennte schoener sein)
	public void setSimulator(Runnable simulator) {
		this.simulator = simulator;
	}

	/**
	 * @return Die 3D-Gestalt des Objekts
	 */
	public final Group getShape() {
		return shape;
	}

	public final BranchGroup getBranchGroup() {
		return branchgrp;
	}

	/**
	 * @param relTrans
	 * @param comp
	 */
	public final void addBranchComponent(Transform3D relTrans, Node comp) {

		TransformGroup tg = new TransformGroup();
		tg.setTransform(relTrans);
		tg.addChild(comp);

		this.transformgrp.addChild(tg);
	}

	/**
	 * Startet den Bot (bzw. dessen Thread).
	 */
	public final void start() {
		thrd = new Thread(this, "ctSim-"+toString());
		addDisposeListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				lg.info("Stoppe "+thrd.getName());
				Thread t = thrd;
				thrd = null;
				t.interrupt();
			}
		});

		thrd.start();
		lg.fine("Thread "+thrd.getName()+" gestartet");
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

	public final double getHeadingInRad() {
		return radiansToYAxis(headingInWorldCoord);
	}

	@Deprecated
	public final Vector3d getHeadingVectorInWorldCoord() {
		return new Vector3d(headingInWorldCoord);
	}

	/**
	 * @param posInWorldCoord Die Position, an die der Bot gesetzt werden soll
	 */
	public final synchronized void setPosition(Point3d posInWorldCoord) {
		// Optimierung (Transform-Kram ist teuer)
		if (this.posInWorldCoord.equals(posInWorldCoord))
			return;

		this.posInWorldCoord = posInWorldCoord;

		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.setTranslation(new Vector3d(posInWorldCoord));
		transformgrp.setTransform(t);

		if (! is(COLLIDED)
		&&  ! is(IN_HOLE)) {
			lastSafePos.set(posInWorldCoord);
		}
	}

	public final synchronized void setHeading(double headingInRad) {
		setHeading(vectorFromAngle(headingInRad));
	}

	public static Vector3d vectorFromAngle(double headingInRad) {
		headingInRad = Misc.normalizeAngleRad(headingInRad);
		return new Vector3d(- Math.sin(headingInRad),
		                    + Math.cos(headingInRad),
		                    0);
	}

	//$$ Ziemlicher Quatsch, dass das ein Vector3 ist: double mit dem Winkel drin waere einfacher und wuerde dasselbe leisten
	@Deprecated
	public final synchronized void setHeading(Vector3d headingInWorldCoord) {
		// Optimierung (Transform-Kram ist teuer)
		if (this.headingInWorldCoord.equals(headingInWorldCoord))
			return;

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
	 */
	private static double radiansToYAxis(Vector3d v) {
		double rv = v.angle(new Vector3d(0, 1, 0));
		if (v.x > 0)
			rv = -rv;
		return rv;
	}

	public void set(ObstState state, boolean setOrClear) {
		if (setOrClear)
			set(state);
		else
			clear(state);
	}

	public void set(ObstState state) {
		if (obstState.add(state)) {
			lg.info(toString()+" "+state.messageOnEnter);
			updateAppearance();
		}
	}

	public void clear(ObstState state) {
		if (obstState.remove(state)) {
			lg.info(toString()+" "+state.messageOnExit);
			updateAppearance();
		}
	}

	public boolean is(ObstState s) {
		return obstState.contains(s);
	}

	public boolean isObstStateNormal() {
		return obstState.isEmpty();
	}

	// Gemaess dbfeld-ctsim-log-state.txt
	public int getLegacyObstState() {
		int rv = 0;
		for (ObstState s : obstState)
			rv += s.legacyValue;
		return rv;
	}

	/**
	 * &Uuml;berschreibt die run()-Methode aus der Klasse Thread und arbeitet
	 * zwei Schritte ab: <br/> 1. {@link #work()} &ndash; wird in einer Schleife
	 * immer wieder aufgerufen <br/> 2. {@link #cleanup()} &ndash; r&auml;umt
	 * auf.<br/> Die Schleife l&auml;uft so lang, bis sie von der Methode die()
	 * beendet wird.
	 *
	 * @see AliveObstacle#work()
	 */
	public final void run() {
		Thread thisThread = Thread.currentThread();

		int timeout = 0;
		try {
			timeout = Integer.parseInt(Config.getValue("AliveObstacleTimeout"));
		} catch (Exception e) {
			// Wenn kein Teimout im Config-File steht, ignorieren wir dieses Feature
			timeout = 0;
		}

		try {
			while (this.thrd == thisThread) {
				// Stoppe die Zeit, die work() benoetigt
				long realTimeBegin = System.currentTimeMillis();

				// Ein AliveObstacle darf nur dann seine work()-Routine
				// ausfuehren, wenn es nicht Halted ist
				if (! is(HALTED)) {
					try {
						bot.doSimStep();
						updateView();
					} catch (UnrecoverableScrewupException e) {
						lg.warn(toString()+" hat schwere Probleme und ist " +
							"steckengeblieben");
						set(ObstState.HALTED);
					}
				}

				// berechne die Zeit, die work benoetigt hat
				long elapsedTime = System.currentTimeMillis() - realTimeBegin;

				// Wenn der Timeout aktiv ist und zuviel Zeit benoetigt wurde, halte dieses Alive Obstacle an
				if ((timeout > 0) && (elapsedTime > timeout))
					set(HALTED);

				if (thrd != null)
					barrier.awaitNextSimStep();
			}
		} catch(InterruptedException ie) {
			// No-op: nochmal Meldung ausgeben, dann Ende
		}
		lg.info(toString()+" wurde entfernt");
	}

	@Override
	public void updateView() throws InterruptedException {
		super.updateView(); // Unsere Komponenten aktualisieren
		bot.updateView(); // und die vom Wrappee
	}

	/**
	 * Hier wird aufgeraeumt, wenn die Lebenszeit des AliveObstacle zuende ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest, das Panel
	 * wird aus dem ControlFrame entfernt
	 *
	 * @see AliveObstacle#work()
	 */
	// $$ Koennte eine gute Idee sein mit dem detach()
//	protected void cleanup() {
//		((BranchGroup)getNodeReference(BG)).detach();
//		world.remove(this);
//		world = null;
//	}

	private void updateAppearance() {
		String key;
		if (isObstStateNormal())
			key = "normal";
		else
			// appearanceKey des ersten gesetzten Elements
			// Im Fall, dass mehr als ein ObstState gesetzt ist (z.B. COLLIDED
			// und zugleich HALTED), werden alle ignoriert ausser dem ersten
			key = obstState.iterator().next().appearanceKeyInXml;

		Color c = Config.getBotColor(bot.getClass(), bot.getInstanceNumber(),
			key);

		for (Runnable1<Color> listener : appearanceListeners)
			listener.run(c);
	}

	/**
	 * Diese Methode wird von au&szlig;en aufgerufen und erledigt die ganze
	 * Aktualisierung der Simulation. Steuerung des Bots hat hier jedoch nichts
	 * zu suchen. Die geh&ouml;rt in work().
	 *
	 * @param simulTime
	 * @see AliveObstacle#work()
	 */
	public void updateSimulation(long simulTime){
		deltaT = simulTime - lastSimulTime;
		lastSimulTime = simulTime;
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

	public Point3d worldCoordFromBotCoord(Point3d inBotCoord) {
		Point3d rv = (Point3d)inBotCoord.clone();
		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.transform(rv);
		return rv;
	}

	public Vector3d worldCoordFromBotCoord(Vector3d inBotCoord) {
		Vector3d rv = (Vector3d)inBotCoord.clone();
		Transform3D t = new Transform3D();
		transformgrp.getTransform(t);
		t.transform(rv);
		return rv;
	}

	@Override
	public String toString() {
		return bot.toString();
	}

	public void addAppearanceListener(
	Runnable1<Color> calledWhenObstStateChanged) {
		if (calledWhenObstStateChanged == null)
			throw new NullPointerException();
		appearanceListeners.add(calledWhenObstStateChanged);
	}

	@Override
	public String getDescription() {
		return bot.getDescription();
	}

	@Override
	public int getInstanceNumber() {
		return bot.getInstanceNumber();
	}

	@Override
	public void accept(BotBuisitor buisitor) {
		super.accept(buisitor);
		bot.accept(buisitor);
	}

	//$$ Nirgends verwendet, aber waere evtl. sinnvoll
	/*
	public Bounds getBounds() {
		return new BoundingSphere(new Point3d(getPositionInWorldCoord()),
			BOT_RADIUS);
	}
	*/
}
