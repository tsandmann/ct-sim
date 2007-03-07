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
import ctSim.model.bots.ctbot.CtBotSimTcp;
import ctSim.util.Runnable1;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * <p>
 * Klasse für alle Bots, die eine 3D-Darstellung haben (= simulierte Bots, für
 * reale ist das unnötig). Fungiert als Wrapper um eine {@link SimulatedBot}-Instanz,
 * d.h. diese Klasse hat eine Referenz auf einen {@code SimulatedBot} und ist
 * selbst ein Bot.
 * </p>
 * <p>
 * Die beiden Methoden, die für die Simulation zentral sind:
 * <ul>
 * <li>{@link #run()}, die als eigener Thread läuft. Sie macht periodisch zwei
 * Dinge: Warten und dem SimulatedBot sagen "jetzt Simschritt machen" (für den
 * {@link CtBotSimTcp} heißt das er überträgt Sensordaten, wartet auf Antwort
 * vom C-Code, und aktualisiert dann die Aktuatoren wie vom C-Code gewünscht)</li>
 * <li>{@link #updateSimulation(long)}, die von außen aufgerufen wird. Die
 * Methode betrachtet die Aktuator-Werte (z.B. Motorgeschwindigkeit) und nimmt
 * an der Simulation die relevanten Änderungen vor (im Beispiel: eine
 * Positions-/Drehungsänderung). Außerhalb dieser Klasse wird sichergestellt,
 * dass {@code updateSimulation()} immer dann aufgerufen wird, wenn der Thread
 * dieser Klasse gerade wartet, nicht wenn er gerade einen Simschritt macht.
 * </li>
 * </ul>
 * </p>
 *
 * @author Benjamin Benz (bbe@ctmagazin.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ThreeDBot extends BasicBot implements Bot, Runnable {
	final FmtLogger lg = FmtLogger.getLogger("ctSim.model.ThreeDBot");

	private final List<Runnable1<Color>> appearanceListeners = Misc.newList();

	public enum State {
		/** Der Bot ist kollidiert (mit der Wand oder mit einem anderen Bot) */
		COLLIDED(0x001, "collision",
			"kollidiert",
			"ist nicht mehr kollidiert"),

		/**
		 * Der Bot hängt in einem Loch, d.h. eins der Räder ist in eine Grube
		 * gerutscht. In diesem State kann sich der Bot noch drehen, aber nicht
		 * mehr bewegen.
		 */
		IN_HOLE(0x002, "falling",
			"hat keinen Boden mehr unter den F\u00FC\u00DFen",
			"hat wieder Boden unter den F\u00FC\u00DFen"),

		/**
		 * Der Bot ist von der weiteren Simulation ausgeschlossen, d.h. seine
		 * work()-Methode wird nicht mehr aufgerufen. Es steht damit nur noch
		 * rum, bis die Simulation irgendwann endet. Dieser Zustand kann
		 * eintreten, wenn die TCP-Verbindung abrei&szlig;t (Bot-Code
		 * abgestürzt) oder ein anderer I/O-Fehler auftritt.
		 */
		HALTED(0x100, "halted",
			"wird aus der Simulation ausgeschlossen");

		final int legacyValue;
		final String appearanceKeyInXml;
		final String messageOnEnter;
		final String messageOnExit;

		State(int legacyValue, String appearanceKeyInXml,
		String messageOnEnter) {
			this(legacyValue, appearanceKeyInXml, messageOnEnter, null);
		}

		State(int legacyValue, String appearanceKeyInXml,
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
			// Unicode 00B0: Grad-Zeichen
			return "Richtung [\u00B0]";
		}

		@Override
		public String getDescription() {
			// Unicode 00B0: Grad-Zeichen
			return "Richtung, in die der Bot blickt, gemessen in Grad; " +
					"Blick nach Norden = 0\u00B0; " +
					"Blick nach Westen = +90\u00B0";
		}

		@Override
		public void updateExternalModel() {
			ignoreStateChange = true;
			getExternalModel().setValue(Math.toDegrees(getHeadingInRad()));
			ignoreStateChange = false;
		}
	}

	private final EnumSet<State> obstState = EnumSet.noneOf(State.class);

	private Point3d posInWorldCoord = new Point3d();

	/**
	 * Letzte Position, an der der Bot nicht kollidiert oder ins Loch gefallen
	 * war. Wird verwendet zur Berechnung des Abstands zum Ziel, was auch dann
	 * funktionieren soll, wenn der Bot z.B. in ein Loch gefallen ist. Daher
	 * wird in dieser Variablen die letzte Position gehalten, wo der Bot noch
	 * au&szlig;erhalb des Lochs war.
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
				lg.fine("Stoppe Thread "+thrd.getName());
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

	public void set(State state, boolean setOrClear) {
		if (setOrClear)
			set(state);
		else
			clear(state);
	}

	public void set(State state) {
		if (obstState.add(state)) {
			lg.info(toString()+" "+state.messageOnEnter);
			updateAppearance();
		}
	}

	public void clear(State state) {
		if (obstState.remove(state)) {
			lg.info(toString()+" "+state.messageOnExit);
			updateAppearance();
		}
	}

	public boolean is(State s) {
		return obstState.contains(s);
	}

	public boolean isObstStateNormal() {
		return obstState.isEmpty();
	}

	// Gemaess dbfeld-ctsim-log-state.txt
	public int getLegacyObstState() {
		int rv = 0;
		for (State s : obstState)
			rv += s.legacyValue;
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

				if (thrd != null)
					barrier.awaitNextSimStep();
			}
		} catch(InterruptedException ie) {
			// No-op: nochmal Meldung ausgeben, dann Ende
		}
		lg.fine("Thread "+Thread.currentThread().getName()+" wurde beendet");
	}

	/** Implementiert Bug 39 (http://www.heise.de/trac/ctbot/ticket/39) */
	private void dieOrHalt() {
		String eh = Config.getValue("simBotErrorHandling");
		String warning = toString()+" hat ein E/A-Problem: " +
				"Bot-Code ist wohl abgestürzt; ";
		if ("kill".equals(eh)) {
			lg.warn(warning+"entferne Bot");
			dispose();
		} else if ("halt".equals(eh)) {
			lg.warn(warning+"Bot ist steckengeblieben");
			set(HALTED);
		}
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
	 * Diese Methode wird von au&szlig;en aufgerufen und erledigt die
	 * Aktualisierung der Simulation: Bot-Position weitersetzen je nach dem,
	 * wie schnell die Motoren gerade drehen usw.
	 *
	 * @param simTimeInMs Aktuelle Simulation in Millisekunden
	 */
	public void updateSimulation(long simTimeInMs) {
		if (is(HALTED)) // Fix für Bug 44
			return;
		deltaT = simTimeInMs - lastSimulTime;
		lastSimulTime = simTimeInMs;
		/*$$$ Simulator sollte ein Runnable sein, das Excp werfen kann, so 
		 * dass es InterruptedExcp wirft, denn sonst kann passieren:
		 * 
Exception in thread "ctSim-Sequencer" java.lang.RuntimeException: java.lang.InterruptedException
	at ctSim.model.bots.ctbot.MasterSimulator.run(MasterSimulator.java:446)
	at ctSim.model.ThreeDBot.updateSimulation(ThreeDBot.java:634)
	at ctSim.model.World.updateSimulation(World.java:978)
	at ctSim.controller.DefaultController.run(DefaultController.java:181)
	at java.lang.Thread.run(Thread.java:595)
Caused by: java.lang.InterruptedException
	at java.lang.Object.wait(Native Method)
	at java.lang.Object.wait(Object.java:474)
	at java.awt.EventQueue.invokeAndWait(EventQueue.java:846)
	at javax.swing.SwingUtilities.invokeAndWait(SwingUtilities.java:1257)
	at ctSim.model.bots.ctbot.MasterSimulator.run(MasterSimulator.java:444)
	... 4 more
		 */
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
