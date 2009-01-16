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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.j3d.AmbientLight;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.PickBounds;
import javax.media.j3d.PickConeRay;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickRay;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ctSim.controller.BotBarrier;
import ctSim.controller.Config;
import ctSim.model.bots.Bot;
import ctSim.model.bots.SimulatedBot;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * <p>Welt-Modell, kuemmert sich um die globale Simulation und das
 * Zeitmanagement.</p>
 *
 * <p>Zum Erzeugen einer Welt die statischen Methoden
 * <code>buildWorldFrom...()</code> verwenden.</p>
 *
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Werner Pirkl (morpheus.the.real@gmx.de)
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class World {
	/** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.model.World");

	/** Name des XML-Schemas fuer Parcours */
	private static final String PARCOURS_DTD = "/parcours.dtd";
	
	/** Breite des Spielfelds in m */
	public static final float PLAYGROUND_THICKNESS = 0f;

	/** Transforgroup der Welt */
	private TransformGroup worldTG;

	/** Branchgroup fuer Licht */
	private BranchGroup lightBG;
	/** Brachgroup fuer Terrain */
	private BranchGroup terrainBG;
	/** Branchgroup fuer Hindernisse */
	private BranchGroup obstBG;
	/** Branchgroup fuer Szene */
	private BranchGroup scene;
	/** Branchgroup fuer Parcours */
	private Parcours parcours;

	/** Viewer */
	private Set<ViewPlatform> viewPlatforms = new HashSet<ViewPlatform>();
	
	/** Die Quelle, aus der der Parcours dieser Welt gelesen wurde */
	private static String sourceString;

	///////////////////////////////////////////////////////////////////////////
	// "Geerbte" Zeit-Sachen

	/** @see #setSimStepIntervalInMs(int) */
	private int simStepIntervalInMs = 0;

	/** <p>Pro Simulationsschritt r&uuml;ckt die Simulationszeit-Uhr um diesen
	 * Wert vor. Einheit Millisekunden.</p>
	 *
	 * $$ Dokumentieren: Bot kriegt Zeit in diesen Schritten mitgeteilt; setzt indirekt max. Aufloesung
	 */
	private static final int SIM_TIME_PER_STEP =
		Integer.parseInt(Config.getValue("simTimePerStep"));

	/** <p>Gegenw&auml;rtige Simulationszeit. Sie ist gleich <em>Zahl der
	 * bisher ausgef&uuml;hrten Simulationsschritte &times;
	 * <code>SIM_TIME_PER_STEP</code></em>. Einheit Millisekunden.</p>
	 *
	 * @see #getSimTimeInMs()
	 */
	private long simTimeInMs = 0;

	///////////////////////////////////////////////////////////////////////////

	/** aktive Bots */
	private final List<ThreeDBot> botsRunning = Misc.newList();
	/** zu startende Bots */
	private final List<ThreeDBot> botsToStart = Misc.newList();

	
	/**
	 * Startet die neuen Bots
	 */
	public synchronized void startBots() {
		for (ThreeDBot b : botsToStart) {
			b.start();
			botsRunning.add(b);
		}
		botsToStart.clear();
	}

	/**
	 * Ermittelt die neue Anzahl an Bots (aktive + neue)
	 * @return Botanzahl
	 */
	public synchronized int getFutureNumOfBots() {
		return botsRunning.size() + botsToStart.size();
	}

	/**
	 * Entfernt alle Bots
	 */
	public synchronized void removeAllBotsNow() {
		// Listen kopieren: b.dispose() entfernt den Bot aus botsToStart und
		// botsRunning, ein Iterator wuerde also eine ConcurrentModificationExcp
		// werfen (d.h. for (ThreeDBot b : botsRunning) geht nicht)
		List<ThreeDBot> lb = Misc.newList();
		lb.addAll(botsToStart);
		lb.addAll(botsRunning);
		for (ThreeDBot b : lb)
			b.dispose();
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * <p>
	 * Liefert den Zeitraffer-/Zeitlupen-Faktor: Wieviel Realzeit
	 * (Armbanduhrenzeit) vergeht zwischen dem Beginn eines Simulatorschritts
	 * und dem Beginn des n&auml;chsten Simulatorschritts? &ndash; Einheit
	 * Millisekunden.
	 * </p>
	 * <p>
	 * N&auml;heres siehe {@link #setSimStepIntervalInMs(int)}.
	 * </p>
	 * @return Zeitintervall
	 */
	public int getSimStepIntervalInMs() {
		return this.simStepIntervalInMs;
	}

	/** <p>Setzen des Zeitraffer-/Zeitlupen-Faktors:
	 * Stellt ein, wieviel Realzeit (Armbanduhrenzeit) zwischen zwei
	 * Schritten der Simulation vergeht. Einheit Millisekunden.</p>
	 *
	 * <p>Vorg&auml;nge in der Simulation werden von diesem Wert nicht
	 * beeinflusst, da unabh&auml;ngig von ihm jeder Simulationsschritt
	 * ausgef&uuml;hrt wird. Der Wert beeinflusst nur, wie lange
	 * zwischen den Schritten (in Realzeit) gewartet wird.</p>
	 *
	 * @param timeInterval Zeitspanne [ms], wieviel Zeit zwischen dem
	 * Beginn eines Simulatorschritts und dem Beginn des folgenden
	 * Simulatorschritts liegen soll.
	 */
    public void setSimStepIntervalInMs(int timeInterval) {
    	this.simStepIntervalInMs = timeInterval;
    }

	/** Liefert die aktuelle Simulationszeit in Millisekunden.
	 *
	 * @return Die momentane Simulationszeit [ms]. Sie ist gleich der Zahl
	 * der bisher ausgef&uuml;hrten Simulationsschritte mal einem
	 * konstanten Faktor. Daher ist sie unabh&auml;ngig von der Realzeit
	 * (Armbanduhrenzeit): eine Sim-Sekunde ist im allgemeinen
	 * nicht gleich lang wie eine Armbanduhr-Sekunde, und zudem wird
	 * die Simzeit-Uhr beispielsweise angehalten, wenn in der GUI der
	 * Pause-Knopf gedr&uuml;ckt wird. Simzeit und Realzeit unterscheiden
	 * sich also sowohl um Summanden als auch um einen Faktor.
	 */
    public long getSimTimeInMs() {
		return simTimeInMs;
    }

	/** Setzt die Simulationszeit-Uhr um den Gegenwert eines
	 * Simulationsschrittes weiter.
	 *
	 * @see #SIM_TIME_PER_STEP
	 * @see #getSimTimeInMs()
	 */
	private void increaseSimulTime() {
		simTimeInMs += SIM_TIME_PER_STEP;
	}

	///////////////////////////////////////////////////////////////////////////
	// Statische Methoden, um eine Welt zu erzeugen

	/**
	 * L&auml;dt einen Parcours aus einer Datei und baut damit eine Welt.
	 * @param sourceFile Die zu &ouml;ffnende Datei. Sie
	 * hat in dem f&uuml;r Parcours vorgesehenen Schema zu sein.
	 * @return Die neue Welt
	 * @throws SAXException 
	 * @throws IOException 
	 */
	public static World buildWorldFromFile(File sourceFile)
	throws SAXException, IOException {
	    BufferedReader in = new BufferedReader(new FileReader(sourceFile));
	    String line;
	    sourceString = new String();
		while ((line = in.readLine()) != null) {
			sourceString += line + "\r\n";
		}
		in.close();
		return new World(new InputSource(sourceFile.toURI().toString()), 
				/* Der EntityResolver hat den Sinn, dem Parser zu sagen,
				 * er soll die parcours.dtd bitte im Verzeichnis "parcours"
				 * suchen. */
				new EntityResolver() {
					public InputSource resolveEntity(
							String publicId,
							String systemId)
					throws SAXException, IOException {
						if (systemId.endsWith(PARCOURS_DTD))
							return new InputSource(ClassLoader.getSystemResource(
									// "./" darf hier nicht enthalten sein
									Config.getValue("worlddir").substring(2) + PARCOURS_DTD).openStream());
		                return null; // Standard-EntityResolver verwenden
		            }
				}
			);
	}

	/** L&auml;dt einen Parcours aus einem String und baut damit eine Welt.
	 * @param parcoursAsXml Der String, der die XML-Darstellung des Parcours
	 * enth&auml;lt. Das XML muss in dem f&uuml;r Parcours vorgesehenen Schema
	 * sein. Die in Zeile&nbsp;2 des XML angegebene DTD-Datei wird von dieser
	 * Methode im Unterverzeichnis "parcours" gesucht. 
	 * @return Die neue Welt
	 * @throws SAXException 
	 * @throws IOException */
	public static World buildWorldFromXmlString(String parcoursAsXml)
	throws SAXException, IOException {
		sourceString = new String(parcoursAsXml);
		return new World(
				new InputSource(new StringReader(parcoursAsXml)),
				/* Der EntityResolver hat den Sinn, dem Parser zu sagen,
				 * er soll die parcours.dtd bitte im Verzeichnis "parcours"
				 * suchen. */
				new EntityResolver() {
					public InputSource resolveEntity(
							String publicId,
							String systemId)
					throws SAXException, IOException {
						if (systemId.endsWith(PARCOURS_DTD))
							return new InputSource(ClassLoader.getSystemResource(
									// "./" darf hier nicht enthalten sein
									Config.getValue("worlddir").substring(2) + PARCOURS_DTD).openStream());
		                return null; // Standard-EntityResolver verwenden
		            }
				});
	}

	///////////////////////////////////////////////////////////////////////////

	/**
	 * <p>
	 * Liest einen Parcours aus einer XML-Quelle und baut damit eine Welt.
	 * </p>
	 * <p>
	 * Der Konstruktor ist privat, da ihn niemand von au&szlig;en verwendet hat.
	 * Es stehen die statischen Methoden <code>buildWorldFromFile</code> und
	 * <code>buildWorldFromXmlString</code> aus dieser Klasse zu
	 * Verf&uuml;gung, um Welten zu erzeugen.
	 * </p>
	 *
	 * @param source Die Xerces-Eingabequelle, aus der der die XML-Darstellung
	 * des Parcours kommt. Das Parsen der Quelle liefert einen Parcours, auf
	 * dessen Grundlage eine Instanz der Welt konstruiert wird.
	 * <code>source</code> kann auf die Parcours-Datei zeigen oder (via einen
	 * java.io.StringReader) auf das in einem String stehende XML. Potentiell
	 * auch auf anderes.
	 * @param resolver Der Xerces-EntityResolver, der beim Parsen des XML
	 * verwendet werden soll. Details siehe
	 * {@link ParcoursLoader#loadParcours(InputSource, EntityResolver)}.
	 * @throws SAXException 
	 * @throws IOException 
	 * @see ParcoursLoader#loadParcours(InputSource, EntityResolver)
	 */
	private World(InputSource source, EntityResolver resolver)
	throws SAXException, IOException {
		ParcoursLoader pL = new ParcoursLoader();
		pL.loadParcours(source, resolver);
		Parcours p = pL.getParcours();

		init();
		setParcours(p);
	}

	/**
	 * Breite in Bloecken
	 * @return Breite
	 */
	public int getWidthInGridBlocks() {
		return parcours.getWidthInBlocks();
	}

	/**
	 * Hoehe in Bloecken
	 * @return Hoehe
	 */
	public int getHeightInGridBlocks() {
		return parcours.getHeightInBlocks();
	}

	/**
	 * @return X-Dimension des Spielfeldes in Meter
	 */
	public float getWidthInM() {
		return parcours.getWidthInM();
	}

	/**
	 * @return Y-Dimension des Spielfeldes in Meter
	 */
	public float getHeightInM() {
		return parcours.getHeightInM();
	}

	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * @param parc Der Parcours
	 */
	private void setParcours(Parcours parc) {

		this.parcours = parc;
		// Hindernisse werden an die richtige Position geschoben

		// Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau
		// buendig mit der Unterkante des Bodens ist:
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup obstTG = new TransformGroup(translate);
		obstTG.setCapability(Node.ENABLE_PICK_REPORTING);
		obstTG.setPickable(true);
		this.obstBG.addChild(obstTG);

	    obstTG.addChild(parc.getObstBG());
	    this.lightBG.addChild(parc.getLightBG());
	    this.terrainBG.addChild(parc.getTerrainBG());

	    this.obstBG.setCapability(Node.ENABLE_PICK_REPORTING);
	    this.obstBG.setCapability(Node.ALLOW_PICKABLE_READ);
	}

	/**
	 * Initialisiert die Welt
	 */
	private void init() {
//		VirtualUniverse.setJ3DThreadPriority(1);

		// Die Wurzel des Ganzen:
		this.scene = new BranchGroup();
		this.scene.setName("World");
		this.scene.setUserData(new String("World"));

		Transform3D worldTransform = new Transform3D();
		worldTransform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		this.worldTG = new TransformGroup(worldTransform);

		this.worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		this.worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		this.worldTG.setCapability(Node.ENABLE_PICK_REPORTING);
		this.worldTG.setCapability(Node.ALLOW_PICKABLE_READ);
		this.worldTG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		this.worldTG.setPickable(true);

		this.scene.addChild(this.worldTG);

		// Lichtquellen einfuegen
		// Streulicht (ambient light)
		BoundingSphere ambientLightBounds =
			new BoundingSphere(new Point3d(0d, 0d, 0d), 100d);
		Color3f ambientLightColor = new Color3f(0.33f, 0.33f, 0.33f);
		AmbientLight ambientLightNode = new AmbientLight(ambientLightColor);
		ambientLightNode.setInfluencingBounds(ambientLightBounds);
		ambientLightNode.setEnable(true);
		this.worldTG.addChild(ambientLightNode);

		// Die Branchgroup fuer die Lichtquellen
		this.lightBG = new BranchGroup();
		this.lightBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		this.lightBG.setPickable(true);
		this.worldTG.addChild(this.lightBG);

		// Die Branchgroup fuer den Boden
		this.terrainBG = new BranchGroup();
		this.terrainBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		this.terrainBG.setPickable(true);
		this.worldTG.addChild(this.terrainBG);

		// Damit spaeter Bots hinzugefuegt werden koennen:
		this.obstBG = new BranchGroup();
		this.obstBG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		this.obstBG.setCapability(BranchGroup.ALLOW_DETACH);
		this.obstBG.setCapability(Group.ALLOW_CHILDREN_WRITE);
		this.obstBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		this.obstBG.setPickable(true);
		this.worldTG.addChild(this.obstBG);
	}

	/**
	 * @return Gibt die BranchGroup der Szene zurueck
	 */
	public BranchGroup getScene() {
		return this.scene;
	}

	/**
	 * Fuegt eine ViewPlatform hinzu
	 * @param view Die neue Ansicht
	 */
	public void addViewPlatform(ViewPlatform view) {
		this.viewPlatforms.add(view);
	}

	/**
	 * Fuegt einen neuen Bot hinzu
	 * @param bot 		Der neue Bot
	 * @param barrier 	Barrier fuer den Bot
	 * @return Neue ThreeDBot-Instanz
	 */
	public ThreeDBot addBot(SimulatedBot bot, BotBarrier barrier) {
		if (bot == null)
			throw new NullPointerException();
		
		int newBot = getFutureNumOfBots() + 1;
		Point3d pos = parcours.getStartPosition(newBot);
		parcours.setStartFieldUsed(bot);
		// Die Bots schweben 7,5 cm ueber Null, damit keine Kollision mit der Grundflaeche erkannt wird
		pos.z = 0.075;
		Vector3d head = parcours.getStartHeading(newBot);

		final ThreeDBot botWrapper = new ThreeDBot(pos, head, barrier, bot);

		// Simulation
		Runnable simulator = SimulatorFactory.createFor(this, botWrapper, bot);
		botWrapper.setSimulator(simulator);

		botsToStart.add(botWrapper);
		obstBG.addChild(botWrapper.getBranchGroup());
		//botWrapper.updateSimulation(getSimTimeInMs());

		botWrapper.addDisposeListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				botsToStart.remove(botWrapper);
				botsRunning.remove(botWrapper);
				obstBG.removeChild(botWrapper.getBranchGroup());
			}
		});

		return botWrapper;
	}

	/* **********************************************************************
	 * **********************************************************************
	 * WORLD_FUNCTIONS
	 *
	 * Funktionen fuer die Sensoren usw. (Abstandsfunktionen u.ae.)
	 *
	 */
	/** Reichweite des Lichtes in m */
	private static final float LIGHT_SOURCE_REACH = 1f;

	/**
	 * Gibt den Gewinner zurueck
	 * @return ThreeDBot, der gewonnen hat
	 */
	public ThreeDBot whoHasWon() {
		for (ThreeDBot b : botsRunning) {
			if (finishReached(b.getPositionInWorldCoord()))
				return b;
		}
		return null;
	}

	/**
	 * @param posInWorldCoord Position
	 * @return {@code true}, falls pos auf einem der Zielfelder des Parcours
	 * liegt. {@code false} andernfalls.
	 */
	private boolean finishReached(Point3d posInWorldCoord) {
		return this.parcours.finishReached(new Vector3d(posInWorldCoord));
	}

	/**
	 * Prueft, ob ein Objekt mit irgendeinem anderen Objekt kollidiert
	 *
	 * @param obst das Objekt
	 * @param bounds die Grenzen des Objekts
	 * @param newPosition die angestrebte neue Position
	 * @return {@code true} wenn das Objekt kollidiert ist, {@code false} wenn
	 * es sich frei bewegen kann
	 */
	public boolean isCollided(ThreeDBot obst, Bounds bounds,
	Vector3d newPosition) {

		Group botBody = obst.getShape();
//		Bounds bounds = obst.getBounds();
		//String botName = obst.getName();

//		System.out.println(bounds.toString());

		// schiebe probehalber Bounds an die neue Position
		Transform3D transform = new Transform3D();
		transform.setTranslation(newPosition);
		bounds.transform(transform);

//		System.out.println(bounds.toString());

		// und noch die Welttransformation darauf anwenden
		this.worldTG.getTransform(transform);
		bounds.transform(transform);

//		System.out.println(bounds.toString());

		PickBounds pickShape = new PickBounds(bounds);
		PickInfo pickInfo;
		synchronized (this.obstBG) {
			// Eigenen Koerper des Roboters verstecken
			botBody.setPickable(false);

			pickInfo = this.obstBG.pickAny(PickInfo.PICK_BOUNDS,
					PickInfo.NODE, pickShape);

			// Eigenen Koerper des Roboters wieder "pickable" machen
			botBody.setPickable(true);
		}

		if ((pickInfo == null) || (pickInfo.getNode() == null))
			return false;
		//System.out.println(botName + " hatte einen Unfall!");
		//Debug.out.println("Bot \""+botName + "\" hatte einen Unfall!"); //$NON-NLS-1$ //$NON-NLS-2$
		//obst.stop();
		return true;
	}

	/**
	 * Prueft, ob unter dem angegebenen Punkt innerhalb der Bodenfreiheit des
	 * Bots noch Boden zu finden ist
	 *
	 * @param pos
	 *            Die Position, von der aus nach unten gemessen wird
	 * @param groundClearance
	 *            Die als normal anzusehende Bodenfreiheit
	 * @return True wenn Bodenkontakt besteht.
	 */
	// TODO: Ueberarbeiten... (GroundClearance?)
	public boolean checkTerrain(Point3d pos, double groundClearance) {

		return !parcours.checkHole(pos);
/*

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		this.worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(0d, 0d, -1d);
		transform.transform(relHeading);

		PickShape pickShape = new PickRay(relPos, relHeading);
		PickInfo pickInfo;
		synchronized (this.terrainBG) {
			pickInfo = this.terrainBG.pickClosest(PickInfo.PICK_GEOMETRY,
					PickInfo.CLOSEST_DISTANCE, pickShape);
		}
		if (pickInfo == null) {
			Debug.out.println(message + " faellt ins Bodenlose."); //$NON-NLS-1$
			return false;
		} else if (Math.round(pickInfo.getClosestDistance() * 1000) > Math
				.round(groundClearance * 1000)) {
			Debug.out.println(message + " faellt " //$NON-NLS-1$
					+ pickInfo.getClosestDistance() * 1000 + " mm."); //$NON-NLS-1$
			return false;
		} else
			return true;

*/
	}

	/**
	 * Liefert eine Angabe, wie viel Licht vom Boden absorbiert wird und den
	 * Linien- bzw. Abgrundsensor nicht mehr erreicht. Je mehr Licht reflektiert
	 * wird, desto niedriger ist der zurueckgegebene Wert. Der Wertebereich
	 * erstreckt sich von 0 (weiss oder maximale Reflexion) bis 1023 (minimale
	 * Reflexion, schwarz oder Loch).
	 *
	 * Es werden rayCount viele Strahlen gleichmaessig orthogonal zum Heading
	 * in die Szene geschossen.
	 *
	 * @param pos
	 *            Die Position, an der der Sensor angebracht ist
	 * @param heading
	 *            Die Blickrichtung
	 * @param openingAngle
	 *            Der Oeffnungswinkel des Sensors
	 * @param rayCount
	 *            Es werden rayCount viele Strahlen vom Sensor ausgewertet.
	 * @return Die Menge an Licht, die absorbiert wird, von 1023(100%) bis 0(0%)
	 */
	// TODO: Ueberarbeiten?
	public short sensGroundReflectionLine(Point3d pos, Vector3d heading,
			double openingAngle, short rayCount) {
		// Sensorposition
		Point3d sensPos = new Point3d(pos);
		// Sensorblickrichtung nach unten
		Vector3d sensHeading = new Vector3d(0d, 0d, -1d);

		// Falls die Welt verschoben wurde:
		Transform3D transform = new Transform3D();
		this.worldTG.getTransform(transform);
		transform.transform(sensPos);
		// oder rotiert:
		transform.transform(sensHeading);

		// Transformationsgruppen, fuer den Sensorsweep
		Transform3D transformX = new Transform3D();

		// Wenn mehr als ein Strahl ausgesendet werden soll, dann taste
		// den Sensorbereich parallel zur Achse des Bots ab.
		// Bei nur einem Strahl schaue in die Mitte.
		if (rayCount > 2) {
			// beginne links aussen
			AxisAngle4d rotationAxisX = new AxisAngle4d(heading,
					openingAngle / 2);
			transformX.set(rotationAxisX);
			transformX.transform(sensHeading);
			// arbeite dich nach rechts vor
			// sende Strahlen in gleichmaessigen Abstaenden aus
			rotationAxisX.set(heading, -(openingAngle / (rayCount - 1)));
			transformX.set(rotationAxisX);
		} else {
			// aendere die Blickrichtung NICHT
			transformX.setIdentity();
		}
		// Variablen und Objekte ausserhalb der Schleife vorbereiten.
		PickRay pickRay = new PickRay();
		PickInfo pickInfo;
		Shape3D shape;
		Color3f color = new Color3f();

		// Variable zur Auswertung der Absorption/Reflexion
		float absorption = 0f;
		for (int j = 0; j < rayCount; j++) {
			// PickRay modifizieren
			pickRay.set(sensPos, sensHeading);
			synchronized (this.terrainBG) {
				// Picking durchfuehren
				pickInfo = this.terrainBG.pickClosest(PickInfo.PICK_GEOMETRY,
						PickInfo.NODE, pickRay);
			}
			// Boden auswerten
			if (pickInfo == null) {
				// kein Boden = 100% des Lichts wird verschluckt
				absorption += 1;
			} else if (pickInfo.getNode() instanceof Shape3D) {
				shape = (Shape3D) pickInfo.getNode();
				shape.getAppearance().getMaterial().getDiffuseColor(color);
				// Je nach Farbe wird ein Teil des Lichts zurueckgeworfen.
				// Hierzu wird der Durchschnitt der Rot-, Gruen- und
				// Blau-Anteile
				// der Farbe bestimmt.
				absorption += 1 - (color.x + color.y + color.z) / 3;
			}
			// Heading anpassen
			transformX.transform(sensHeading);
		}// Ende for-Schleife
		return (short) (absorption * 1023 / (rayCount));
	}

	/**
	 * Liefert eine Angabe, wie viel Licht vom Boden absorbiert wird und den
	 * Linien- bzw. Abgrundsensor nicht mehr erreicht. Je mehr Licht reflektiert
	 * wird, desto niedriger ist der zurueckgegebene Wert. Der Wertebereich
	 * erstreckt sich von 0 (weiss oder maximale Reflexion) bis 1023 (minimale
	 * Reflexion, schwarz oder Loch).
	 *
	 * Es werden rayCount viele Strahlen gleichmaessig orthogonal zum Heading in
	 * die Szene geschossen.
	 *
	 * Es werden rayCount viele Strahlen gleichmaessig in Form eines "+" in in
	 * die Szene Geschossen.
	 *
	 * @param pos
	 *            Die Position, an der der Sensor angebracht ist
	 * @param heading
	 *            Die Blickrichtung
	 * @param openingAngle
	 *            Der Oeffnungswinkel des Sensors
	 * @param rayCount
	 *            Es werden rayCount viele Strahlen vom Sensor ausgewertet.
	 * @return Die Menge an Licht, die absorbiert wird, von 1023(100%) bis 0(0%)
	 */
	public short sensGroundReflectionCross(Point3d pos, Vector3d heading,
			double openingAngle, short rayCount) {
		double absorption;
		Vector3d xHeading = new Vector3d(heading);
		absorption = sensGroundReflectionLine(pos, heading, openingAngle,
				(short) (rayCount / 2));
		Transform3D rotation = new Transform3D();
		rotation.rotZ(Math.PI / 2);
		rotation.transform(xHeading);
		absorption += sensGroundReflectionLine(pos, xHeading, openingAngle,
				(short) (rayCount / 2));
		return (short) (absorption / 2);
	}

	/**
	 * <p>
	 * Liefert die Helligkeit, die auf einen Lichtsensor f&auml;llt.
	 * </p>
	 * <p>
	 * Da diese Methode unter Verwendung des PickConeRay implementiert ist, ist
	 * sie seinen Bugs unterworfen. Ausf&uuml;hrliche Dokumentation siehe
	 * watchObstacle(Point3d, Vector3d, double, Shape3D).
	 * </p>
	 *
	 * @param pos Die Position des Lichtsensors
	 * @param heading Die Blickrichtung des Lichtsensors
	 * @param openingAngle Der Oeffnungswinkel des Blickstrahls
	 * @return Die Dunkelheit um den Sensor herum, von 1023(100%) bis 0(0%)
	 * @see PickConeRay
	 */
	public int sensLight(Point3d pos, Vector3d heading, double openingAngle) {

		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		this.worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);

		pickConeRay.set(relPos, relHeading,	openingAngle / 2);
		PickInfo pickInfo;
		synchronized (this.terrainBG) {
			synchronized (this.obstBG) {
				this.obstBG.setPickable(false);
				this.terrainBG.setPickable(false);
				pickInfo = this.lightBG.pickClosest(PickInfo.PICK_GEOMETRY,
						PickInfo.CLOSEST_DISTANCE, pickConeRay);
				this.obstBG.setPickable(true);
				this.terrainBG.setPickable(true);
			}
		}
		if (pickInfo == null)
			return 1023;
		int darkness = (int) ((pickInfo.getClosestDistance() / LIGHT_SOURCE_REACH) * 1023);
		if (darkness > 1023)
			darkness = 1023;
		return darkness;
	}

	/**
	 * <p>
	 * Liefert die Distanz in Metern zum n&auml;chsten Objekt zur&uuml;ck, das
	 * man sieht, wenn man von der &uuml;bergebenen Position aus in Richtung des
	 * &uuml;bergebenen Vektors schaut.
	 * </p>
	 * <p>
	 * <h3>Bug-Warnung</h3>
	 * Der von dieser Methode studenweise angemietete {@link PickConeRay}
	 * (java3d.media.j3d) enth&auml;lt einen emp&ouml;renden Bug. Unter ganz
	 * bestimmten Umst&auml;nden meldet er Objekte, die nur in seinen tiefsten
	 * sadistischen Fantasien existieren. Das hei&szlig;t, seine Methode
	 * getClosestDistance() liefert zu kurze Werte &ndash; z.B. 42 cm zur
	 * n&auml;chsten wei&szlig;en Maus, wo sie aber nachweislich mindestens
	 * 6&times;9 cm entfernt ist.
	 * </p>
	 * <p>
	 * Die Umst&auml;nde, unter denen im PickConeRay die
	 * Distanzwertberechnungsvermasselung eintritt, sind hochspeziell (von
	 * dieser Position aus in diesem Winkel mit diesen Hindernissen in dieser
	 * Entfernung usw.). Daher wirkt der Bug typischerweise nur f&uuml;r
	 * einzelne Simschritte: Ein Bot vollzieht in aller Arglosigkeit einen
	 * Simschritt, ist im zweiten Schritt zuf&auml;llig diesen Umst&auml;nden
	 * ausgesetzt und erschrickt sich zu Tode, aber im dritten Schritt hat sich
	 * der Spuk so schnell verzogen, wie er gekommen ist. Das mag wie Pillepalle
	 * wirken, aber ein Simschritt lang irrsinnige Sensorwerte wirkt auf manche
	 * Bot-Steueralgorithmen wie Volksfest im Reinraum. Die verkorksten Werte
	 * machen sich &ndash; so vermuten wir &ndash; als die unerkl&auml;rliche
	 * Ausrei&szlig;er in den Kurven der Licht- und Distanzsensoren bemerkbar,
	 * die ja schon <a
	 * href="http://de.uncyclopedia.wikia.com/wiki/Ludwig_Wittgenstein">Wittgenstein</a>
	 * jahrzehntelang vor R&auml;tsel stellten und von zwei verschiedenen
	 * Messieurs an mindestens drei Stellen dokumentiert wurden:
	 * <ul>
	 * <li><a
	 * href="http://www.heise.de/ct/foren/go.shtml?t=1&T=lichtsensor&sres=1&msg_id=11156011&forum_id=89813">ctBot-Forum
	 * (1)</a></li>
	 * <li><a
	 * href="http://www.heise.de/ct/foren/go.shtml?t=1&T=lichtsensor&sres=1&msg_id=11109100&forum_id=89813">ctBot-Forum
	 * (2)</a></li>
	 * <li>E-Mail siehe unten &ndash; Auf Basis dieser Mail konnte der Bug in
	 * einer n&auml;chtlichen Gewaltaktion reproduziert werden (Java3D 1.4.0_01,
	 * WinXP). Nach geduldigen verdeckten Ermittlungen einem fehlgeschlagenen
	 * Zugriff unter Einsatz des Sonderkommandos konnte der PickConeRay nach
	 * einer halsbrecherischen G&auml;nsejagd quer durch den Quelltext als der
	 * Arsch dingfest gemacht werden.
	 * </p>
	 * </li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>Hinweis:</strong> Der {@link PickRay} scheint weniger
	 * schwuchtelig unterwegs zu sein, also sollte man wohl mal &uuml;berlegen,
	 * dem den Job zu &uuml;berlassen.
	 * </p>
	 * <p>
	 * <strong>Anhang: E-Mail, die auf den Bug hingewiesen hat</strong>
	 *
	 * <pre style="font-size: smaller;">
	 * Betreff: &uuml;berlauf / bug in sensor werten?
	 * Von: Nikolai Baumeister &lt;...&gt;
	 * Datum: Fri, 29 Sep 2006 15:32:27 +0200
	 * An: ct-sim@heise.de
	 *
	 * Liebe Mitarbeiter
	 *
	 * Wie im irc beschrieben habe ich m&ouml;glicherweise einen
	 * reproduzierbaren Fehler in den Rohwerten der Distanzsensoren im
	 * Wettbewerbs-Sim mit Wettbewerbsroboter gefunden:
	 *
	 * Das Labyrinth ist dieses:
	 * http://nikola.light.hl-users.com/angle-world-lamps.xml
	 *
	 * Der Fehler tritt bei mir genau dann auf wenn der bot in DIESEM Labyrinth
	 * GENAU von der Startposition aus startet.
	 *
	 * Der Bot f&amp;aumlhrt mit maximaler geschwindigkeit nach Norden auf die
	 * Wand zu und gibt dabei die Distanzwerte aus.
	 *
	 * Was der Bot genau macht ist unten in START_BOT() zu finden.
	 *
	 * Sensor Rohwerte habe ich zus&auml;tzlich ausgegeben
	 * zwischen Zeile 313 und 314 in command.c
	 * mit:
	 * printf(&quot;left raw: %i, right raw: %i\n&quot;,
	 *     received_command.data_l, received_command.data_l);
	 *
	 * Ansonsten waren sensor.c, command.c etc identisch mit der CVS version.
	 *
	 * Ausgabe (bereinigt):
	 *
	 * left raw: 115, right raw: 115
	 * sensDistL: 779, sensDistR: 999, y: 752
	 *
	 * left raw: 116, right raw: 116
	 * sensDistL: 767, sensDistR: 999, y: 752
	 *
	 * left raw: 117, right raw: 117
	 * sensDistL: 755, sensDistR: 999, y: 763
	 *
	 * -----&gt;left raw: 225, right raw: 225
	 * -----&gt;sensDistL: 281, sensDistR: 298, y: 775
	 *
	 * left raw: 118, right raw: 118
	 * sensDistL: 743, sensDistR: 999, y: 775
	 *
	 * left raw: 119, right raw: 119
	 * sensDistL: 732, sensDistR: 999, y: 787
	 *
	 * left raw: 120, right raw: 120
	 * sensDistL: 721, sensDistR: 999, y: 787
	 *
	 * Die Rohwerte der Distanzsensoren springen an einer Stelle auf 255 hoch.
	 *
	 * Gru&szlig;
	 * Niko
	 *
	 *
	 * void START_BOT(Behaviour_t *data) {
	 *
	 *     #define ACTION_1    0
	 *     #define ACTION_2    1
	 *     #define DONE        2
	 *
	 *     static int state = ACTION_1;
	 *
	 *     switch (state){
	 *
	 *         case ACTION_1:
	 *             speedWishLeft=BOT_SPEED_MAX;
	 *             speedWishRight=BOT_SPEED_MAX;
	 *             state = ACTION_2;
	 *             break;
	 *
	 *         case ACTION_2:
	 *             printf(&quot;sensDistL: %i, sensDistR: %i, y: %.0f\n&quot;,
	 *                 sensDistL, sensDistR, x_mou);
	 *             state = ACTION_1;
	 *             break;
	 *
	 *         default:
	 *             break;
	 *     }
	 * }
	 * </pre>
	 *
	 * </p>
	 *
	 * @param pos Die Position, von der aus der Seh-Strahl verfolgt wird
	 * @param heading Die Blickrichtung, d.h. die Richtung des Seh-Strahls
	 * @param openingAngle Der Sehstrahl ist in Wahrheit ein Kegel;
	 * {@code openingAngle} gibt seinen &Ouml;ffnungswinkel an (Bogenma&szlig;)
	 * @param botBody Der K&ouml;rper des Roboter, der anfragt. Dies ist
	 * erforderlich, da diese Methode mit einem PickConeRay implementiert ist,
	 * d.h. der K&ouml;rper des Bots muss auf &quot;not pickable&quot; gesetzt
	 * werden vor Anwendung des Ray und wieder auf &quot;pickable&quot; nach
	 * Anwendung. Der Grund ist, dass sonst in Grenzf&auml;llen der
	 * Botk&ouml;rper vom PickConeRay gefunden wird.
	 * @return Die Distanz zum n&auml;chsten Objekt (&quot;pickable&quot;) in
	 * Metern
	 * @see PickConeRay
	 * @see PickRay
	 */
	// TODO: Ueberarbeiten?
	synchronized public double watchObstacle(Point3d pos, Vector3d heading,
			double openingAngle, Node botBody) {

		// TODO: Wieder rein??
		// Falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
//		System.out.println(Math.floor(relPos.x*1000)+" | "+Math.floor(relPos.y*1000)+" | "+Math.floor(relPos.z*1000));
		Transform3D transform = new Transform3D();
		this.worldTG.getTransform(transform);
		transform.transform(relPos);
//		System.out.println(Math.floor(relPos.x*1000)+" | "+Math.floor(relPos.y*1000)+" | "+Math.floor(relPos.z*1000));

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
//		System.out.println(Math.floor(relHeading.angle(new Vector3d(0d, 1d, 0d))*100));
		transform.transform(relHeading);
//		System.out.println(Math.floor(relHeading.angle(new Vector3d(0d, 1d, 0d))*100));

//		Point3d relPos = pos;
//		Vector3d relHeading = heading;

		pickConeRay.set(relPos, relHeading, openingAngle);
		PickInfo pickInfo;
		synchronized (this.obstBG) {
			botBody.setPickable(false);
			pickInfo = this.obstBG.pickClosest(
					PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_DISTANCE,
					pickConeRay);
			try {
				botBody.setPickable(true);
			} catch (Exception e) {
				// NOP
			}
		}
		if (pickInfo == null)
			return 100.0; //$$ Ist das dokumentiert? Erwarten die Aufrufer das?
		double d = pickInfo.getClosestDistance();
		return d;
	}

	/**
	 * Damit jedes Obstacle fair behandelt wird, merken wir uns, wer das
	 * letzte Mal zuerst dran war
	 */
	private int runningBotsPtr = 0;

	/** PickConeRay der Welt */
	private PickConeRay pickConeRay = new PickConeRay();

	/**
	 * Diese Methode setzt die Simulation um einen Simulationsschritt weiter.
	 * Siehe {@link ctSim.controller.DefaultController#run()}.
	 */
	public void updateSimulation() {
		// Simzeit um einen Schritt weiter
		increaseSimulTime();

		ThreeDBot[] bots = botsRunning.toArray(new ThreeDBot[] {});
		// Zeiger koennte zu weit stehen
		if (runningBotsPtr >= bots.length)
			runningBotsPtr = 0;

		for (int i = runningBotsPtr; i < bots.length; i++)
			bots[i].updateSimulation(getSimTimeInMs());
		for (int i = 0; i < runningBotsPtr; i++)
			bots[i].updateSimulation(getSimTimeInMs());
		runningBotsPtr++;
	}

	/** Schreibt den Parcours der Welt in eine Datei. Es wird dasselbe XML
	 * in die Datei geschrieben, das beim
	 * Konstruieren dieser Instanz geparst wurde, um den Parcours der Welt zu
	 * erhalten.
	 *
	 * @param targetFile Datei, in die das XML des Parcours ausgegeben wird.
	 * Sie wird bei Bedarf angelegt.
	 */
	public void writeParcoursToFile(File targetFile) {
		try {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(
					targetFile));
				out.write(sourceString.getBytes());
				out.close();
				lg.info("Welt wurde gespeichert als \""+
						targetFile.getName()+"\".");
		} catch(IOException e) {
			lg.warn("Fehler: Datei konnte nicht gespeichert werden!");
			e.printStackTrace();
		}
	}

	/**
	 * Ermittelt die kuerzestet Distanz zum Ziel
	 * @param fromWhere	Anfangsposition
	 * @return	Entfernung
	 */
	public double getShortestDistanceToFinish(Point3d fromWhere) {
		return getShortestDistanceToFinish(new Vector3d(fromWhere));
	}

	/**
	 * Ermittelt die kuerzestet Distanz zum Ziel
	 * @param fromWhere	Anfangsposition
	 * @return	Entfernung
	 */
	public double getShortestDistanceToFinish(Vector3d fromWhere) {
		return parcours.getShortestDistanceToFinish(fromWhere);
	}

	/**
	 * Beseitige alles, was noch auf die Welt und den Szenegraphen verweist
	 *
	 */
	public void cleanup() {
		removeAllBotsNow();
		viewPlatforms.clear();
		scene = null;
		lightBG = null;
		obstBG = null;
		terrainBG = null;
		pickConeRay = null;
		worldTG = null;
	}

	/**
	 * Loescht einen Bot
	 * @param bot der zu loeschende Bot
	 */
	public void deleteBot(Bot bot) {
		parcours.setStartFieldUnused(bot);
	}
	
	/**
	 * Setzt alle Bots auf ihre Startpplaetze zurueck.
	 * Hier werden nur simulierte Bots (ThreeDBot-Instanzen) beruecksichtigt, weil
	 * sonstige Bots in World nicht bekannt sind! 
	 */
	public void resetAllBots() {
		for (ThreeDBot b : botsRunning) {
			Bot bot = b.getSimBot();
			/* Startfeld des Bots ermitteln */
			int startField = parcours.getStartPositionNumber(bot);
			/* Position und Heading des Startfelds setzen */
			Point3d pos = parcours.getUsedStartPosition(startField);
			pos.z = 0.075;
			Vector3d head = parcours.getStartHeading(startField);
			b.setHeading(head);
			b.setPosition(pos);
		}
	}
}
