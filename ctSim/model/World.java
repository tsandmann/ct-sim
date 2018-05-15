/*
 * c't-Sim - Robotersimulator für den c't-Bot
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
import javax.media.j3d.PickConeSegment;
import javax.media.j3d.PickInfo;
import javax.media.j3d.PickRay;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Color3f;
import javax.vecmath.Point2i;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ctSim.controller.BotBarrier;
import ctSim.controller.Config;
import ctSim.model.BPS.Beacon;
import ctSim.model.Map.MapException;
import ctSim.model.bots.Bot;
import ctSim.model.bots.SimulatedBot;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.view.gui.StatusBar;

/**
 * <p>
 * Welt-Modell, kümmert sich um die globale Simulation und das Zeitmanagement.
 * </p>
 *
 * <p>
 * Zum Erzeugen einer Welt die statischen Methoden <code>buildWorldFrom...()</code> verwenden.
 * </p>
 *
 * @author Benjamin Benz
 * @author Peter König (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Werner Pirkl (morpheus.the.real@gmx.de)
 * @author Hendrik Krauß
 */
public class World {
	/** Logger */
	final FmtLogger lg = FmtLogger.getLogger("ctSim.model.World");

	/** Name des XML-Schemas für Parcours */
	private static final String PARCOURS_DTD = "/parcours.dtd";
	
	/** Dicke des Bodens in m */
	public static final float PLAYGROUND_THICKNESS = 0.0f;

	/** Transforgroup der Welt */
	private TransformGroup worldTG;

	/** Branchgroup für Licht */
	private BranchGroup lightBG;
	/** Branchgroup für BPS */
	private BranchGroup bpsBG;
	/** Brachgroup für Terrain */
	private BranchGroup terrainBG;
	/** Branchgroup für Hindernisse und Bots */
	private BranchGroup obstBG;
	/** Branchgroup für Szene */
	private BranchGroup scene;
	/** Parcours der Welt */
	private Parcours parcours;

	/** Viewer */
	private Set<ViewPlatform> viewPlatforms = new HashSet<ViewPlatform>();
	
	/** Die Quelle, aus der der Parcours dieser Welt gelesen wurde */
	private static String sourceString;

	/* ============================================================ */

	// "Geerbte" Zeit-Sachen

	/**
	 * @see #setSimStepIntervalInMs(int)
	 * */
	private int simStepIntervalInMs = 0;

	/**
	 * <p>
	 * Pro Simulationsschritt rückt die Simulationszeit-Uhr um diesen Wert vor.
	 * Einheit Millisekunden.
	 * </p>
	 *
	 * TODO: Dokumentieren: Bot kriegt Zeit in diesen Schritten mitgeteilt; setzt indirekt max. Auflösung
	 */
	private static final int SIM_TIME_PER_STEP =
		Integer.parseInt(Config.getValue("simTimePerStep"));

	/**
	 * <p>
	 * Gegenwärtige Simulationszeit. Sie ist gleich <em>Zahl der bisher ausgeführten Simulationsschritte
	 * x <code>SIM_TIME_PER_STEP</code></em>. Einheit Millisekunden.
	 * </p>
	 *
	 * @see #getSimTimeInMs()
	 */
	private long simTimeInMs = 0;

	/* ============================================================ */

	/** aktive Bots */
	private final List<ThreeDBot> botsRunning = Misc.newList();
	/** zu startende Bots */
	private final List<ThreeDBot> botsToStart = Misc.newList();

	
	/** Startet die neuen Bots */
	public synchronized void startBots() {
		for (ThreeDBot b : botsToStart) {
			b.start();
			botsRunning.add(b);
		}
		botsToStart.clear();
	}

	/**
	 * Ermittelt die neue Anzahl an Bots (aktive + neue)
	 * 
	 * @return Botanzahl
	 */
	public synchronized int getFutureNumOfBots() {
		return botsRunning.size() + botsToStart.size();
	}

	/** Entfernt alle Bots */
	public synchronized void removeAllBotsNow() {
		/*
		 * Listen kopieren: b.dispose() entfernt den Bot aus botsToStart und botsRunning, ein Iterator
		 * würde also eine ConcurrentModificationExcp werfen (d.h. for (ThreeDBot b : botsRunning)
		 * geht nicht)
		 */
		List<ThreeDBot> lb = Misc.newList();
		lb.addAll(botsToStart);
		lb.addAll(botsRunning);
		for (ThreeDBot b : lb) {
			b.dispose();
		}
	}

	/* ============================================================ */

	/**
	 * <p>
	 * Liefert den Zeitraffer-/Zeitlupen-Faktor: Wieviel Realzeit (Armbanduhrenzeit) vergeht zwischen
	 * dem Beginn eines Simulatorschritts und dem Beginn des nächsten Simulatorschritts?
	 * Die Einheit ist Millisekunden.
	 * </p>
	 * <p>
	 * Näheres siehe {@link #setSimStepIntervalInMs(int)}.
	 * </p>
	 * 
	 * @return Zeitintervall
	 */
	public int getSimStepIntervalInMs() {
		return simStepIntervalInMs;
	}

	/**
	 * <p>
	 * Setzen des Zeitraffer-/Zeitlupen-Faktors:
	 * Stellt ein, wieviel Realzeit (Armbanduhrenzeit) zwischen zwei Schritten der Simulation vergeht.
	 * Die Einheit ist Millisekunden.
	 * </p>
	 *
	 * <p>
	 * Vorgänge in der Simulation werden von diesem Wert nicht beeinflusst, da jeder Simulationsschritt
	 * unabhängig von ihm ausgeführt wird. Der Wert beeinflusst nur, wie lange zwischen den Schritten
	 * (in Realzeit) gewartet wird.
	 * </p>
	 *
	 * @param timeInterval
	 * 				Zeitspanne [ms], wieviel Zeit zwischen dem Beginn eines Simulatorschritts
	 * 				und dem Beginn des folgenden Simulatorschritts liegen soll.
	 */
    public void setSimStepIntervalInMs(int timeInterval) {
    	simStepIntervalInMs = Math.min(timeInterval, StatusBar.MAX_TICK_RATE);
    }

	/**
	 * Liefert die aktuelle Simulationszeit in Millisekunden.
	 *
	 * @return Die momentane Simulationszeit [ms]
	 * 			Sie ist gleich der Zahl der bisher ausgeführten Simulationsschritte mal einem
	 * 			konstanten Faktor. Daher ist sie unabhängig von der Realzeit (Armbanduhrenzeit):
	 * 			eine Sim-Sekunde ist im Allgemeinen	nicht gleich lang wie eine Armbanduhr-Sekunde,
	 * 			und zudem wird die Simzeit-Uhr beispielsweise angehalten, wenn in der GUI der
	 * 			Pause-Knopf gedrückt wird. Simzeit und Realzeit unterscheiden sich also sowohl
	 * 			um Summanden als auch um einen Faktor.
	 */
    public long getSimTimeInMs() {
		return simTimeInMs;
    }

	/**
	 * Setzt die Simulationszeit-Uhr um den Gegenwert eines Simulationsschrittes weiter
	 *
	 * @see #SIM_TIME_PER_STEP
	 * @see #getSimTimeInMs()
	 */
	private void increaseSimulTime() {
		simTimeInMs += SIM_TIME_PER_STEP;
	}

	/* ============================================================ */

	// Statische Methoden, um eine Welt zu erzeugen

	/**
	 * Lädt einen Parcours aus einer Datei und baut damit eine Welt
	 * 
	 * @param sourceFile	Die zu öffnende Datei. Sie hat in dem für Parcours vorgesehenen Schema zu sein.
	 * @return Die neue Welt
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 */
  
	public static World buildWorldFromFile(File sourceFile) throws SAXException, IOException,
				ParserConfigurationException {
	    BufferedReader in = new BufferedReader(new FileReader(sourceFile));
	    String line;
	    sourceString = new String();
		while ((line = in.readLine()) != null) {
			sourceString += line + "\r\n";
		}
		in.close();
		return new World(new InputSource(sourceFile.toURI().toString()), 
			/*
			 * Der EntityResolver hat den Sinn, dem Parser zu sagen,
			 * er soll die parcours.dtd im Verzeichnis "parcours" suchen.
			 */
			new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
					if (systemId.endsWith(PARCOURS_DTD))
						return new InputSource(ClassLoader.getSystemResource(
							// "./" darf hier nicht enthalten sein
							Config.getValue("worlddir").substring(2) + PARCOURS_DTD).openStream());
	                return null;	// Standard-EntityResolver verwenden
	            }
			}
		);
	}

	/**
	 * Lädt einen Parcours aus einem String und baut damit eine Welt
	 * 
	 * @param parcoursAsXml
	 * 				Der String, der die XML-Darstellung des Parcours enthält. Das XML muss in dem
	 * 				für Parcours vorgesehenen Schema sein. Die in Zeile 2 des XML angegebene
	 * 				DTD-Datei wird von dieser Methode im Unterverzeichnis "parcours" gesucht. 
	 * @return Die neue Welt
	 * @throws SAXException 
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */

	public static World buildWorldFromXmlString(String parcoursAsXml) throws SAXException, IOException,
				ParserConfigurationException {
		sourceString = new String(parcoursAsXml);
		return new World(
			new InputSource(new StringReader(parcoursAsXml)),
			/*
			 * Der EntityResolver hat den Sinn, dem Parser zu sagen,
			 * er soll die parcours.dtd im Verzeichnis "parcours" suchen.
			 */
			new EntityResolver() {
				public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
					if (systemId.endsWith(PARCOURS_DTD))
						return new InputSource(ClassLoader.getSystemResource(
						// "./" darf hier nicht enthalten sein
						Config.getValue("worlddir").substring(2) + PARCOURS_DTD).openStream());
                return null; // Standard-EntityResolver verwenden
            }
		});
	}

	/* ============================================================ */

	/**
	 * <p>
	 * Liest einen Parcours aus einer XML-Quelle und baut damit eine Welt.
	 * </p>
	 * 
	 * <p>
	 * Der Konstruktor ist privat, da ihn niemand von außen verwendet hat.
	 * Es stehen die statischen Methoden <code>buildWorldFromFile</code> und
	 * <code>buildWorldFromXmlString</code> aus dieser Klasse zu Verfügung, um Welten zu erzeugen.
	 * </p>
	 *
	 * @param source
	 * 				Die Xerces-Eingabequelle, aus der der die XML-Darstellung des Parcours kommt.
	 * 				Das Parsen der Quelle liefert einen Parcours, auf dessen Grundlage eine Instanz
	 * 				der Welt konstruiert wird. <code>source</code> kann auf die Parcours-Datei zeigen
	 * 				oder (via einen java.io.StringReader) auf das in einem String stehende XML. Potentiell
	 * 				auch auf anderes.
	 * @param resolver
	 * 				Der Xerces-EntityResolver, der beim Parsen des XML verwendet werden soll.
	 * 				Details siehe {@link ParcoursLoader#loadParcours(InputSource, EntityResolver)}.
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 *  
	 * @see ParcoursLoader#loadParcours(InputSource, EntityResolver)
	 */
	private World(InputSource source, EntityResolver resolver) throws SAXException, IOException,
				ParserConfigurationException {
		ParcoursLoader pl = new ParcoursLoader();
		pl.loadParcours(source, resolver);
		Parcours p = pl.getParcours();

		init();
		setParcours(p);
	}

	/**
	 * Breite in Blöcken
	 * 
	 * @return Breite
	 */
	public int getWidthInGridBlocks() {
		return parcours.getWidthInBlocks();
	}

	/**
	 * Höhe in Blöcken
	 * 
	 * @return Höhe
	 */
	public int getHeightInGridBlocks() {
		return parcours.getHeightInBlocks();
	}

	/**
	 * @return X-Dimension des Spielfeldes in Metern
	 */
	public float getWidthInM() {
		return parcours.getWidthInM();
	}

	/**
	 * @return Y-Dimension des Spielfeldes in Metern
	 */
	public float getHeightInM() {
		return parcours.getHeightInM();
	}

	/**
	 * Erzeugt einen Szenegraphen mit Boden und Grenzen der Roboterwelt
	 * 
	 * @param parc der Parcours
	 */
	private void setParcours(Parcours parc) {
		parcours = parc;
		/*
		 * Hindernisse werden an die richtige Position geschoben:
		 * Zuerst werden sie gemeinsam so verschoben, dass ihre Unterkante genau bündig mit der
		 * Unterkante des Bodens ist:
		 */
		Transform3D translate = new Transform3D();
		translate.set(new Vector3d(0d, 0d, 0.2d - PLAYGROUND_THICKNESS));
		TransformGroup obstTG = new TransformGroup(translate);
		obstTG.setCapability(Node.ENABLE_PICK_REPORTING);
		obstTG.setPickable(true);
		obstBG.addChild(obstTG);

	    obstTG.addChild(parc.getObstBG());
	    lightBG.addChild(parc.getLightBG());
	    bpsBG.addChild(parc.getBpsBG());
	    terrainBG.addChild(parc.getTerrainBG());

	    obstBG.setCapability(Node.ENABLE_PICK_REPORTING);
	    obstBG.setCapability(Node.ALLOW_PICKABLE_READ);
	}

	/** Initialisiert die Welt */
	private void init() {
		// die Wurzel des Ganzen:
		scene = new BranchGroup();
		scene.setName("World");
		scene.setUserData(new String("World"));

		Transform3D worldTransform = new Transform3D();
		worldTransform.setTranslation(new Vector3f(0.0f, 0.0f, -2.0f));
		worldTG = new TransformGroup(worldTransform);

		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		worldTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		worldTG.setCapability(Node.ENABLE_PICK_REPORTING);
		worldTG.setCapability(Node.ALLOW_PICKABLE_READ);
		worldTG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		worldTG.setPickable(true);

		scene.addChild(worldTG);

		/* Lichtquellen einfügen */
		// Streulicht (ambient light)
		BoundingSphere ambientLightBounds = new BoundingSphere(new Point3d(0d, 0d, 0d), 100d);
		Color3f ambientLightColor = new Color3f(0.33f, 0.33f, 0.33f);
		AmbientLight ambientLightNode = new AmbientLight(ambientLightColor);
		ambientLightNode.setInfluencingBounds(ambientLightBounds);
		ambientLightNode.setEnable(true);
		worldTG.addChild(ambientLightNode);

		/* Die Branchgroup für die Lichtquellen */
		lightBG = new BranchGroup();
		lightBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		lightBG.setPickable(true);
		worldTG.addChild(lightBG);
		
		/* Die Branchgroup für BPS (IR-Licht) */
		bpsBG = new BranchGroup();
		bpsBG.setPickable(true);
		worldTG.addChild(this.bpsBG);

		/* Die Branchgroup für den Boden */
		terrainBG = new BranchGroup();
		terrainBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		terrainBG.setPickable(true);
		worldTG.addChild(terrainBG);

		// Damit später Bots hinzugefügt werden können:
		obstBG = new BranchGroup();
		obstBG.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		obstBG.setCapability(BranchGroup.ALLOW_DETACH);
		obstBG.setCapability(Group.ALLOW_CHILDREN_WRITE);
		obstBG.setCapability(Node.ALLOW_PICKABLE_WRITE);
		obstBG.setPickable(true);
		worldTG.addChild(obstBG);
		
		int tickrate = 0;
		try {
			tickrate = Integer.parseInt(Config.getValue("ctSimTickRate"));
		} catch (NumberFormatException exc) {
			// egal
		}
		setSimStepIntervalInMs(tickrate);
	}

	/**
	 * @return Gibt die BranchGroup der Szene zurück
	 */
	public BranchGroup getScene() {
		return scene;
	}
	
	/**
	 * @return Gibt den Parcours der Welt zurück
	 */
	public Parcours getParcours() {
		return parcours;
	}
	
	/**
	 * @return Gibt die BranchGroup der Hindernisse zurück
	 */
	public BranchGroup getObstacles() {
		return obstBG;
	}
	
	/**
	 * @return TG der Welt
	 */
	public TransformGroup getWorldTG() {
		return worldTG;
	}

	/**
	 * Fügt eine ViewPlatform hinzu
	 * 
	 * @param view	die neue Ansicht
	 */
	public void addViewPlatform(ViewPlatform view) {
		viewPlatforms.add(view);
	}

	/**
	 * Fügt einen neuen Bot hinzu
	 * 
	 * @param bot 		der neue Bot
	 * @param barrier 	Barrier für den Bot
	 * @return Neue ThreeDBot-Instanz
	 */
	public ThreeDBot addBot(SimulatedBot bot, BotBarrier barrier) {
		if (bot == null) {
			throw new NullPointerException();
		}
		
		int newBot = getFutureNumOfBots() + 1;
		Point3d pos = parcours.getStartPosition(newBot);
		parcours.setStartFieldUsed(bot);
		// Die Bots schweben 7,5 cm über Null, damit keine Kollision mit der Grundfläche erkannt wird
		pos.z = 0.075;
		Vector3d head = parcours.getStartHeading(newBot);

		final ThreeDBot botWrapper = new ThreeDBot(pos, head, barrier, bot);

		// Simulation
		Runnable simulator = SimulatorFactory.createFor(this, botWrapper, bot);
		botWrapper.setSimulator(simulator);

		botsToStart.add(botWrapper);
		obstBG.addChild(botWrapper.getBranchGroup());
//		botWrapper.updateSimulation(getSimTimeInMs());

		botWrapper.addDisposeListener(new Runnable() {
			public void run() {
				botsToStart.remove(botWrapper);
				botsRunning.remove(botWrapper);
				obstBG.removeChild(botWrapper.getBranchGroup());
			}
		});

		return botWrapper;
	}

	/*
	 * **********************************************************************
	 *  WORLD_FUNCTIONS
	 * **********************************************************************
	 * 
	 * Funktionen für die Sensoren usw. (Abstandsfunktionen u.ä.)
	 */

	/**
	 * Gibt den Gewinner zurück
	 * 
	 * @return ThreeDBot, der gewonnen hat
	 */
	public ThreeDBot whoHasWon() {
		for (ThreeDBot b : botsRunning) {
			if (finishReached(b.getPositionInWorldCoord())) {
				return b;
			}
		}
		return null;
	}

	/**
	 * @param posInWorldCoord	Position
	 * @return {@code true}, falls pos auf einem der Zielfelder des Parcours liegt.
	 * 			{@code false} andernfalls.
	 */
	private boolean finishReached(Point3d posInWorldCoord) {
		return this.parcours.finishReached(new Vector3d(posInWorldCoord));
	}

	/**
	 * Prüft, ob ein Objekt mit einem Objekt aus der obstacle-BG der Welt kollidiert
	 * 
	 * @param body		der Körper des Objekts
	 * @param bounds	die Grenzen des Objekts
	 * @return PickInfo über die Kollision (null, falls keine vorhanden)
	 */
	public PickInfo getCollision(Group body, Bounds bounds) {
		/* Welttransformation anwenden */
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		bounds.transform(transform);
		
		PickBounds pickBounds = new PickBounds(bounds);
		PickInfo pickInfo = null;

		synchronized (obstBG) {
			/* eigenen Körper verstecken */
			body.setPickable(false);
			
			/* Kollisionserkennung von Java3D */
			pickInfo = obstBG.pickAny(PickInfo.PICK_BOUNDS, PickInfo.NODE, pickBounds);
			
			/* eigenen Körper wieder zurücksetzen */
			body.setPickable(true);
		}
		
//		if (pickInfo != null && pickInfo.getNode() != null) {
//			lg.info("Objekt=" + pickInfo.getNode().getParent().getName() + " kollidiert");
//		}
		
		return pickInfo;
	}

	/**
	 * Prüft, ob unter dem angegebenen Punkt innerhalb der Bodenfreiheit des
	 * Bots noch Boden zu finden ist
	 *
	 * @param pos				die Position, von der aus nach unten gemessen wird
	 * @param groundClearance	die als normal anzusehende Bodenfreiheit
	 * @return True wenn Bodenkontakt besteht.
	 */
	public boolean checkTerrain(Point3d pos, double groundClearance) {
		return ! parcours.checkHole(pos);
	}

	/**
	 * Liefert eine Angabe, wie viel Licht vom Boden absorbiert wird und den Linien- bzw. Abgrundsensor
	 * nicht mehr erreicht. Je mehr Licht reflektiert wird, desto niedriger ist der zurückgegebene Wert.
	 * Der Wertebereich erstreckt sich von 0 (weiß oder maximale Reflexion) bis 1023 (minimale Reflexion,
	 * schwarz oder Loch).
	 *
	 * Es werden rayCount viele Strahlen gleichmäßig orthogonal zum Heading in die Szene geschossen.
	 *
	 * @param pos			die Position, an der der Sensor angebracht ist
	 * @param heading		die Blickrichtung
	 * @param openingAngle	der Öffnungswinkel des Sensors
	 * @param rayCount		Es werden rayCount viele Strahlen vom Sensor ausgewertet.
	 * @return Die Menge an Licht, die absorbiert wird, von 1023 (100%) bis 0 (0%)
	 */
	public short sensGroundReflectionLine(Point3d pos, Vector3d heading, double openingAngle, short rayCount) {
		// Sensorposition
		Point3d sensPos = new Point3d(pos);
		// Sensorblickrichtung nach unten
		Vector3d sensHeading = new Vector3d(0d, 0d, -1d);

		// falls die Welt verschoben wurde:
		Transform3D transform = new Transform3D();
		this.worldTG.getTransform(transform);
		transform.transform(sensPos);
		// oder rotiert:
		transform.transform(sensHeading);

		// Transformationsgruppen, für den Sensorsweep
		Transform3D transformX = new Transform3D();

		/*
		 * Wenn mehr als ein Strahl ausgesendet werden soll, dann taste den Sensorbereich parallel zur
		 * Achse des Bots ab. Bei nur einem Strahl schaue in die Mitte.
		 */
		// 
		if (rayCount > 2) {
			// beginne links außen
			AxisAngle4d rotationAxisX = new AxisAngle4d(heading, openingAngle / 2);
			transformX.set(rotationAxisX);
			transformX.transform(sensHeading);
			// arbeite dich nach rechts vor
			// sende Strahlen in gleichmäßigen Abständen aus
			rotationAxisX.set(heading, -(openingAngle / (rayCount - 1)));
			transformX.set(rotationAxisX);
		} else {
			// ändere die Blickrichtung NICHT
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
			// Picking durchführen
			pickInfo = terrainBG.pickClosest(PickInfo.PICK_GEOMETRY, PickInfo.NODE, pickRay);
			// Boden auswerten
			if (pickInfo == null) {
				// kein Boden = 100% des Lichts wird verschluckt
				absorption += 1;
			} else if (pickInfo.getNode() instanceof Shape3D) {
				shape = (Shape3D) pickInfo.getNode();
				shape.getAppearance().getMaterial().getDiffuseColor(color);
				/*
				 * Je nach Farbe wird ein Teil des Lichts zurückgeworfen.
				 * Hierzu wird der Durchschnitt der Rot-, Grün- und Blau-Anteile der Farbe bestimmt.
				 */
				absorption += 1 - (color.x + color.y + color.z) / 3;
			}
			// Heading anpassen
			transformX.transform(sensHeading);
		}	// Ende der for-Schleife
		return (short) (absorption * 1023 / (rayCount));
	}

	/**
	 * Liefert eine Angabe, wie viel Licht vom Boden absorbiert wird und den Linien- bzw. Abgrundsensor
	 * nicht mehr erreicht. Je mehr Licht reflektiert wird, desto niedriger ist der zurückgegebene Wert.
	 * Der Wertebereich erstreckt sich von 0 (weiß oder maximale Reflexion) bis 1023 (minimale Reflexion,
	 * schwarz oder Loch).
	 *
	 * Es werden rayCount viele Strahlen gleichmäßig orthogonal zum Heading in die Szene geschossen.
	 *
	 * Es werden rayCount viele Strahlen gleichmäßig in Form eines "+" in die Szene geschossen.
	 *
	 * @param pos			die Position, an der der Sensor angebracht ist
	 * @param heading		die Blickrichtung
	 * @param openingAngle	der Öffnungswinkel des Sensors
	 * @param rayCount		Es werden rayCount viele Strahlen vom Sensor ausgewertet.
	 * @return Die Menge an Licht, die absorbiert wird, von 1023 (100%) bis 0 (0%)
	 */
	public short sensGroundReflectionCross(Point3d pos, Vector3d heading, double openingAngle, short rayCount) {
		double absorption;
		Vector3d xHeading = new Vector3d(heading);
		absorption = sensGroundReflectionLine(pos, heading, openingAngle, (short) (rayCount / 2));
		Transform3D rotation = new Transform3D();
		rotation.rotZ(Math.PI / 2);
		rotation.transform(xHeading);
		absorption += sensGroundReflectionLine(pos, xHeading, openingAngle, (short) (rayCount / 2));
		return (short) (absorption / 2);
	}

	/**
	 * <p>
	 * Liefert die Helligkeit, die auf einen Lichtsensor fällt.
	 * </p>
	 * 
	 * <p>
	 * Da diese Methode unter Verwendung des PickConeRay implementiert ist,
	 * ist sie seinen Bugs unterworfen. Ausführliche Dokumentation siehe
	 * watchObstacle(Point3d, Vector3d, double, Shape3D).
	 * </p>
	 *
	 * @param pos			die Position des Lichtsensors
	 * @param heading		die Blickrichtung des Lichtsensors
	 * @param lightReach	die Reichweite des Lichtsensors / m
	 * @param openingAngle	der Öffnungswinkel des Blickstrahls / radians
	 * @return Die Dunkelheit um den Sensor herum, von 1023 (100%) bis 0 (0%)
	 * 
	 * @see PickConeRay
	 */
	public int sensLight(Point3d pos, Vector3d heading, double lightReach, double openingAngle) {
		// falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);

		// oder rotiert:
		Vector3d relHeading = new Vector3d(heading);
		transform.transform(relHeading);

		PickConeRay picky = new PickConeRay(relPos, relHeading,	openingAngle);
		PickInfo pickInfo;
		pickInfo = lightBG.pickClosest(PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_DISTANCE, picky);

		if (pickInfo == null) {
			return 1023;
		}
		int darkness = (int) ((pickInfo.getClosestDistance() / lightReach) * 1023.0);
		if (darkness > 1023) {
			darkness = 1023;
		}
		return darkness;
	}

	/**
	 * <p>
	 * Liefert die Bakencodierung der Bake im Blickfeld mit der kürzesten Entfernung zum Sensor,
	 * oder 1023, falls keine Bake gesehen wird.
	 * </p>
	 *
	 * @param pos			die Position des Sensors
	 * @param end			die Position der maximalen Sensorreichweite
	 * @param openingAngle	der Öffnungswinkel des Blickstrahls
	 * @return Die Bakencodierung, der Bake im Blickfeld mit der kürzesten Entfernung zum Sensor,
	 * 				oder 1023, falls keine Bake gesehen wird
	 * 
	 * @see PickConeSegment
	 */
	public int sensBPS(Point3d pos, Point3d end, double openingAngle) {
		// falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Point3d endPos = new Point3d(end);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);
		transform.transform(endPos);

		PickConeSegment picky = new PickConeSegment(relPos, endPos, openingAngle);
		PickInfo pickInfo;
		pickInfo = bpsBG.pickClosest(PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_INTERSECTION_POINT |
				PickInfo.LOCAL_TO_VWORLD, picky);

		if (pickInfo == null) {
			/* keine Landmarke sichtbar */
			return BPS.NO_DATA;
		}
		
		Point3d source = pickInfo.getClosestIntersectionPoint();
		Transform3D t = pickInfo.getLocalToVWorld();
		t.transform(source);

		Beacon beacon = new Beacon(parcours, source);
		int result = beacon.getID();

//		lg.info("Baken-Position [mm]: " + beacon);
//		lg.info("Baken-Position [blocks]: " + beacon.toStringInBlocks());

		return result;
	}
	
	/**
	 * <p>
	 * Liefert die Distanz in Metern zum nächsten Objekt zurück, das man sieht,
	 * wenn man von der übergebenen Position aus in Richtung des übergebenen
	 * Endpunktes schaut.
	 * </p>
	 *
	 * @param pos	die Position, von der aus der Seh-Strahl verfolgt wird
	 * @param end	die Position, wo der Seh-Strahl spätestens enden soll (falls kein Objekt gesehen wird)
	 * @param openingAngle
	 * 				Der Sehstrahl ist in Wahrheit ein Kegel; {@code openingAngle} gibt seinen Öffnungswinkel
	 * 				an (Bogenmaß).
	 * @param botBody
	 * 				Der Körper des Roboter, der anfragt. Dies ist erforderlich, da diese Methode mit einem
	 * 				PickConeSegment implementiert ist, d.h. der Körper des Bots muss vor der Anwendung des
	 * 				Ray auf "not pickable" gesetzt werden und nach der Anwendung wieder auf "pickable".
	 * 				Der Grund ist, dass sonst in Grenzfällen der Botkörper vom PickConeSegment gefunden wird.
	 * @return Die Distanz zum nächsten Objekt ("pickable") in Metern oder 100 m, falls kein Objekt in Sichtweite
	 * 
	 * @see PickConeSegment
	 */
	public double watchObstacle(Point3d pos, Point3d end, double openingAngle, Node botBody) {
		// falls die Welt verschoben wurde:
		Point3d relPos = new Point3d(pos);
		Point3d endPos = new Point3d(end);
		Transform3D transform = new Transform3D();
		worldTG.getTransform(transform);
		transform.transform(relPos);
		transform.transform(endPos);

		PickConeSegment picky = new PickConeSegment(relPos, endPos, openingAngle);
		PickInfo pickInfo;
		try {
			botBody.setPickable(false);
		} catch (Exception e) {
			// egal
		}
		pickInfo = obstBG.pickClosest(PickInfo.PICK_GEOMETRY, PickInfo.CLOSEST_DISTANCE, picky);
		try {
			botBody.setPickable(true);
		} catch (Exception e) {
			// egal
		}
		if (pickInfo == null) {
			return 100.0;
		}
		double d = pickInfo.getClosestDistance();
		return d;
	}

	/** Damit jedes Obstacle fair behandelt wird, merken wir uns, wer das letzte Mal zuerst dran war */
	private int runningBotsPtr = 0;
	
	/**
	 * Diese Methode setzt die Simulation um einen Simulationsschritt weiter.
	 * siehe {@link ctSim.controller.DefaultController#run()}.
	 */
	public void updateSimulation() {
		// Simzeit um einen Schritt weiter
		increaseSimulTime();
		
		ThreeDBot[] bots = botsRunning.toArray(new ThreeDBot[] {});
		// Zeiger könnte zu weit stehen
		if (runningBotsPtr >= bots.length) {
			runningBotsPtr = 0;
		}

		for (int i = runningBotsPtr; i < bots.length; i++) {
			bots[i].updateSimulation(getSimTimeInMs());
		}
		for (int i = 0; i < runningBotsPtr; i++) {
			bots[i].updateSimulation(getSimTimeInMs());
		}
		runningBotsPtr++;
	}

	/**
	 * Schreibt den Parcours der Welt in eine Datei. Es wird dasselbe XML in die Datei geschrieben,
	 * das beim Konstruieren dieser Instanz geparst wurde, um den Parcours der Welt zu erhalten.
	 *
	 * @param targetFile	Datei, in die das XML des Parcours ausgegeben wird. Sie wird bei Bedarf angelegt.
	 */
	public void writeParcoursToFile(File targetFile) {
		try {
			OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile));
			out.write(sourceString.getBytes());
			out.close();
			lg.info("Welt wurde gespeichert als \"" + targetFile.getName() + "\".");
		} catch(IOException e) {
			lg.warn("Fehler: Datei konnte nicht gespeichert werden:");
			lg.warn(e.getMessage());
		}
	}

	/**
	 * Ermittelt die kürzeste Distanz zum Ziel
	 * 
	 * @param fromWhere	Anfangsposition
	 * @return Entfernung
	 */
	public double getShortestDistanceToFinish(Point3d fromWhere) {
		return getShortestDistanceToFinish(new Vector3d(fromWhere));
	}

	/**
	 * Ermittelt die kürzeste Distanz zum Ziel
	 * 
	 * @param fromWhere	Anfangsposition
	 * @return Entfernung
	 */
	public double getShortestDistanceToFinish(Vector3d fromWhere) {
		return parcours.getShortestDistanceToFinish(fromWhere);
	}

	/** Beseitige alles, was noch auf die Welt und den Szenegraphen verweist */
	public void cleanup() {
		removeAllBotsNow();
		viewPlatforms.clear();
		scene = null;
		lightBG = null;
		bpsBG = null;
		obstBG = null;
		terrainBG = null;
		worldTG = null;
	}

	/**
	 * Löscht einen Bot
	 * 
	 * @param bot	der zu löschende Bot
	 */
	public void deleteBot(Bot bot) {
		parcours.setStartFieldUnused(bot);
	}
	
	/**
	 * Setzt alle Bots auf ihre Startpplätze zurück.
	 * Hier werden nur simulierte Bots (ThreeDBot-Instanzen) berücksichtigt,
	 * weil sonstige Bots in World nicht bekannt sind! 
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
	
	/**
	 * Exportiert die aktuelle Welt in eine Bot-Map-Datei
	 * 
	 * @param bot		Bot-Nr., dessen Startfeld als Koordinatenursprung der Map benutzt wird
	 * @param free		Wert, mit dem freie Felder eingetragen werden (z.B. 100)
	 * @param occupied	Wert, mit dem Hindernisse eingetragen werden (z.B. -100)
	 * @throws IOException	falls Fehler beim Schreiben der Datei
	 * @throws MapException	falls keine Daten in der Map
	 */
	public void toMap(int bot, int free, int occupied) throws IOException, MapException {
		float size = Float.parseFloat(Config.getValue("mapSize"));
		int resolution = Integer.parseInt(Config.getValue("mapResolution"));
		int section_points = Integer.parseInt(Config.getValue("mapSectionSize"));
		int macroblock_length = Integer.parseInt(Config.getValue("mapMacroblockSize"));
		
		Map map = new Map(size, resolution, section_points, macroblock_length);
		
		try {
			map.createFromParcours(parcours, bot, free, occupied);
		} catch (MapException e) {
			lg.warn("Konnte Parcours nicht in die Map schreiben:");
			lg.warn(e.getMessage());
			throw map.new MapException();
		}
		map.export();
	}
	
	/**
	 * Rechnet eine Welt-Koordinate in eine globale Position um
	 * 
	 * @param worldPos	Welt-Position (wie von Java3D verwendet)
	 * @return globale Position (wie zur Lokalisierung verwendet) / mm
	 */
	public Point2i transformWorldposToGlobalpos(Point3d worldPos) {
		final int x = (int) (worldPos.y * 1000.0);
		final int y = (int) ((getWidthInM() - worldPos.x) * 1000.0);
		return new Point2i(x, y);
	}
}
