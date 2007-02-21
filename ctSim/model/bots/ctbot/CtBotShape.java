package ctSim.model.bots.ctbot;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import java.awt.Color;
import java.util.ArrayList;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Node;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

import ctSim.model.ThreeDBot;
import ctSim.util.Runnable1;

//$$ doc mit 4 grafiken
//$$ pdf in doc-Ordner
/**
 * <a
 * href="http://www.heise.de/ct/ftp/projekte/ct-bot/pdf/grundplatte_bemassung.pdf">Grundplatte
 * mit Bema&szlig;ung</a>
 */
public class CtBotShape extends Group {
	/** Abstand zwischen Zentrum und Au&szlig;enkante des Bots [m] laut PDF */
	private static final double BOT_RADIUS = 0.060;

	/** H&ouml;he des Bots [m] laut Bauchgef&uuml;hl */
	private static final double BOT_HEIGHT = 0.120;

	/** &Ouml;ffnungswinkel des Mundes [Grad] aus den Ma&szlig;en im PDF. */
	private static final double MOUTH_OPENING_ANGLE_IN_DEG = toDegrees(
		asin(25 / 54.55));

	/** Tiefe des Mundes [m] laut PDF */
	private static final double MOUTH_DEPTH_IN_M = 0.05455 - 0.015;

	/**
	 * <p>
	 * Maximalwinkel [Grad] zwischen zwei Ecken in den Vielecken, die die
	 * Kreislinien am Bot ann&auml;hern. Die Kreisb&ouml;gen am Bot sind wie
	 * &uuml;blich keine echten Kreise, sondern haben Ecken: Sie werden
	 * n&auml;herungsweise durch regelm&auml;&szlig;ige Vielecke dargestellt.
	 * Der Winkel ist der zwischen den Ecken des Vielecks, wobei auch mal eine
	 * Ecke einen kleineren Winkel haben kann (z.B. dort, wo Mittelteil und
	 * Backen aneinandersto&szlig;en).
	 * </p>
	 * <p>
	 * Mehr = bessere (rundere) Darstellung, weniger = schnelleres Rechnen.
	 * </p>
	 */
	private static final double CORNER_INTERVAL_IN_DEG = 10;

    public static class PointList extends ArrayList<Point3d> { //$$ vielleicht util
        private static final long serialVersionUID = 436180486157724410L;

        public PointList interleave(PointList other) {
            if (size() != other.size())
                throw new IllegalArgumentException();

            PointList rv = new PointList();
            for (int i = 0; i < size(); i++) {
                rv.add(get(i));
                rv.add(other.get(i));
            }

            return rv;
        }

        public void reverse() {
        	Point3d p;
            for (int i = 0; i < size() / 2; i++) {
            	p = get(i);
            	set(i, get(size() - 1 - i));
            	set(size() - 1 - i, p);
            }
        }

        public Point3d last() {
            return get(size() - 1);
        }

        public void transform(Transform3D t) {
        	for (Point3d p : this)
        		t.transform(p, p);
        }

        public GeometryArray toStripGeometry () {
            return toGeometry(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        }

        public GeometryArray toFanGeometry() {
            return toGeometry(GeometryInfo.TRIANGLE_FAN_ARRAY);
        }

        //$$ nur fuer debug
        public GeometryArray toLineGeometry() {
            LineStripArray a = new LineStripArray(size(),
                GeometryArray.COORDINATES, new int[] { size() });
			a.setCoordinates(0, toArray(new Point3d[] {}));
			return a;
        }

        public GeometryArray toGeometry(int geometryInfoPrimitive) {
            GeometryInfo gi = new GeometryInfo(geometryInfoPrimitive);
            gi.setCoordinates(toArray(new Point3d[] {}));
            gi.setStripCounts(new int[] { size() });

            // Noch ein paar Beschwoerungsformeln, weiss der Henker warum
            new NormalGenerator().generateNormals(gi);
            gi.recomputeIndices();

            new Stripifier().stripify(gi);
            gi.recomputeIndices();

            return gi.getGeometryArray();
        }
    }

    /**
	 * <p>
	 * J3D-Szenegraph dieser Klasse:
	 *
	 * <pre>
     * this (instanceof CtBotShape, extends Group)
     *   |
     *   +-- leftCheek (instanceof Shape3D)
     *   |    |
     *   |    `-- Appearance 1
     *   |
     *   +-- TransformGroup (rotiert 180 Grad um z-Achse)
     *   |    |
     *   |    `-- rightCheek (instanceof Shape3D)
     *   |         |
     *   |         `-- Appearance 2
     *   |
     *   `-- middle (instanceof Shape3D)
     *        |
     *        `-- Appearance 3
     * </pre>
	 *
	 * Appearance 1 und 2 werden gemeinsam gesetzt, Appearance 3 unabh&auml;ngig
	 * davon.
	 * </p>
	 */
    private final Shape3D leftCheek;
    private final Shape3D rightCheek;
    private final Shape3D middle;

    public CtBotShape(Color baseColor, ThreeDBot appearanceEventSource) {
    	// Kann kollidieren -- Gilt auch fuer alle Kinder im Szenegraph
    	setPickable(true);

    	// "Kann kollidieren" waehrend der Anzeige noch aenderbar (World
    	// ruft auf uns spaeter setPickable() auf)
    	setCapability(Node.ALLOW_PICKABLE_WRITE);

    	// Form bauen
    	rightCheek = buildRightCheek();
    	addChild(rightCheek);

    	// Linke Backe: Shape der rechten, um 180 Grad gedreht
    	leftCheek = (Shape3D)rightCheek.cloneNode(false);
    	TransformGroup g = new TransformGroup(z180aboutCenter());
    	g.addChild(leftCheek);
    	addChild(g);

        // Mittelteil
    	middle = buildMiddle();
    	addChild(middle);

    	// Appearances waehrend der Anzeige aenderbar
    	rightCheek.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
    	leftCheek .setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
    	middle    .setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

    	setMiddleColor(baseColor);
    	setCheeksColor(baseColor);

    	appearanceEventSource.addAppearanceListener(new Runnable1<Color>() {
			public void run(Color newAppearance) {
				setCheeksColor(newAppearance);
			}
    	});
    }

    protected static Shape3D buildRightCheek() {
    	// Shape (= Backe) besteht aus 3 Teilen: Boden, Decke, Mantel
    	Shape3D rv = new Shape3D();

    	PointList floor = buildCheekArc(- BOT_HEIGHT / 2);
        rv.addGeometry(floor.toFanGeometry()); // ausliefern

        PointList ceil  = buildCheekArc(+ BOT_HEIGHT / 2);
        /*
		 * reverse() um Backface-Culling auszutricksen; "richtiger" waere, in
		 * den PolygonAttributes der Appearance setCullFace(FRONT) zu setzen,
		 * aber das wuerde die Appearance der gesamten Shape beeinflussen, und
		 * die soll hier nicht veraendert werden
		 */
        ceil.reverse();
        rv.addGeometry(ceil.toFanGeometry()); // ausliefern

        // Mantel aussen (vom Botzentrum weg gewandt); das ist ein Teil eines
        // Zylindermantels
        PointList arcBottom = buildCheekArc(- BOT_HEIGHT / 2);
        PointList arcTop    = buildCheekArc(+ BOT_HEIGHT / 2);
        PointList lateralSurface = arcBottom.interleave(arcTop);

        // Mantel innen (zum Botzentrum gewandt; d.h. Innenwand Mund); wir
        // schliessen einfach den Zylindermantel-Abschnitt
        lateralSurface.add(arcBottom.get(0));
        lateralSurface.add(arcTop.get(0));

        // Gesamtmantel ausliefern
        rv.addGeometry(lateralSurface.toStripGeometry());

        return rv;
    }

    protected static Shape3D buildMiddle() {
    	// Shape (= Mittelteil) besteht aus 5 Teilen: Zylinderboden, Zyl.-Decke,
    	// Abschnitt eines Zyl.-Mantel, 1. Quaderviertel, 2. Quaderviertel
    	Shape3D rv = new Shape3D();

    	PointList floor = buildStern(- BOT_HEIGHT / 2);
    	rv.addGeometry(floor.toFanGeometry());

    	PointList ceil  = buildStern(+ BOT_HEIGHT / 2);
    	ceil.reverse(); // wegen Backface-Culling; siehe buildRightCheek()
    	rv.addGeometry(ceil.toFanGeometry());

    	// Zylindermantel-Abschnitt
        PointList bottomArc = buildStern(- BOT_HEIGHT / 2);
        PointList topArc    = buildStern(+ BOT_HEIGHT / 2);
        PointList lateralSurface = bottomArc.interleave(topArc);

        rv.addGeometry(lateralSurface.toStripGeometry());

        // Quaderviertel
        PointList cuboidQuarter = new PointList();
        cuboidQuarter.add((Point3d)topArc.last().clone()); // 0
        cuboidQuarter.add((Point3d)topArc.get(0).clone()); // 1

        Transform3D moveDown = new Transform3D();
        moveDown.setTranslation(new Vector3d(0, - MOUTH_DEPTH_IN_M, 0));
        cuboidQuarter.add( // 2
        	transformPoint(topArc.get(0),  z180aboutCenter(), moveDown));
        cuboidQuarter.add( // 3
        	transformPoint(topArc.last(), z180aboutCenter(), moveDown));
        cuboidQuarter.add( // 4
        	transformPoint(bottomArc.last(), z180aboutCenter(), moveDown));

        rv.addGeometry(cuboidQuarter.toStripGeometry()); // ausliefern

        // Quaderviertel transformieren und nochmal verwenden
        Transform3D y180 = new Transform3D();
        y180.rotY(PI);
        cuboidQuarter.transform(y180);
        rv.addGeometry(cuboidQuarter.toStripGeometry()); // 2. Mal ausliefern

        return rv;
    }

    protected static Transform3D z180aboutCenter() {
        Transform3D rv = new Transform3D();
        rv.rotZ(PI);
        return rv;
    }

    protected static PointList buildStern(double z) {
        return buildBotCircumference(z,
            180 - MOUTH_OPENING_ANGLE_IN_DEG,
            180 + MOUTH_OPENING_ANGLE_IN_DEG);
    }

    protected static PointList buildCheekArc(double z) {
        return buildBotCircumference(z,
            MOUTH_OPENING_ANGLE_IN_DEG,
            180 - MOUTH_OPENING_ANGLE_IN_DEG);
    }

    protected static PointList buildBotCircumference(double z,
    double fromAngleInDeg, double toAngleInDeg) {
        PointList rv = new PointList();
        for (double a = fromAngleInDeg; a < toAngleInDeg;
            a += CORNER_INTERVAL_IN_DEG) {

            rv.add(buildPointOnCircumference(z, a));
        }
        // sicherstellen, dass genau der Zielwinkel erreicht wird (nicht
        // vorher aufgehoert)
        rv.add(buildPointOnCircumference(z, toAngleInDeg));
        return rv;
    }

    protected static Point3d buildPointOnCircumference(double z,
   	double angleInDeg) {
        double angleInRad = toRadians(angleInDeg);
        return new Point3d(
        	BOT_RADIUS * sin(angleInRad),
            BOT_RADIUS * cos(angleInRad),
            z);
    }

    //$$ util?
    private static Point3d transformPoint(Point3d p,
    Transform3D... transforms) {
    	Point3d rv = (Point3d)p.clone();
    	for (Transform3D t : transforms)
    		t.transform(rv);
        return rv;
    }

    protected void setMiddleColor(Color c) {
    	middle.setAppearance(getBotAppearance(c));
    }

    protected void setCheeksColor(Color c) {
    	leftCheek .setAppearance(getBotAppearance(c));
    	rightCheek.setAppearance(getBotAppearance(c));
    }

    protected Appearance getBotAppearance(Color c) {
    	Appearance rv = new Appearance();
    	rv.setColoringAttributes(new ColoringAttributes(new Color3f(c),
    		ColoringAttributes.SHADE_GOURAUD));
    	return rv;
    }
}