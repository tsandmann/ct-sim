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

/**
 * <a href="doc-files/grundplatte_bemassung.pdf">Grundplatte mit Bemaßung</a>
 */
public class CtBotShape extends Group {
	/** Abstand zwischen Zentrum und Außenkante des Bots [m] laut PDF */
	private static final double BOT_RADIUS = 0.060;

	/** Höhe des Bots [m] laut Bauchgefühl */
	private static final double BOT_HEIGHT = 0.120;

	/** Öffnungswinkel des Mundes [Grad] aus den Maßen im PDF. */
	private static final double MOUTH_OPENING_ANGLE_IN_DEG = toDegrees(
		asin(25 / 54.55));

	/** Tiefe des Mundes [m] laut PDF */
	private static final double MOUTH_DEPTH_IN_M = 0.05455 - 0.015;

	/**
	 * <p>
	 * Maximalwinkel [Grad] zwischen zwei Ecken in den Vielecken, die die Kreislinien am Bot annähern.
	 * Die Kreisbögen am Bot sind wie üblich keine echten Kreise, sondern haben Ecken: Sie werden
	 * näherungsweise durch regelmäßige Vielecke dargestellt. Der Winkel ist der zwischen den Ecken
	 * des Vielecks, wobei auch mal eine Ecke einen kleineren Winkel haben kann (z.B. dort, wo
	 * Mittelteil und Backen aneinanderstoßen).
	 * </p>
	 * <p>
	 * Mehr = bessere (rundere) Darstellung, weniger = schnelleres Rechnen.
	 * </p>
	 */
	private static final double CORNER_INTERVAL_IN_DEG = 10;

    /** Punkt-Liste */
    public static class PointList extends ArrayList<Point3d> {
        /** UID */
    	private static final long serialVersionUID = 436180486157724410L;

        /**
         * interleave
         *
         * @param other
         * @return PointList
         */
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

        /** reverse */
        public void reverse() {
        	Point3d p;
            for (int i = 0; i < size() / 2; i++) {
            	p = get(i);
            	set(i, get(size() - 1 - i));
            	set(size() - 1 - i, p);
            }
        }

        /**
         * @return last Point
         */
        public Point3d last() {
            return get(size() - 1);
        }

        /**
         * transform
         *
         * @param t
         */
        public void transform(Transform3D t) {
        	for (Point3d p : this)
        		t.transform(p, p);
        }

        /**
         * @return GeometryArray
         */
        public GeometryArray toStripGeometry () {
            return toGeometry(GeometryInfo.TRIANGLE_STRIP_ARRAY);
        }

        /**
         * @return GeometryArray
         */
        public GeometryArray toFanGeometry() {
            return toGeometry(GeometryInfo.TRIANGLE_FAN_ARRAY);
        }


        /**
         * @return GeometryArray
         */
        public GeometryArray toLineGeometry() {
            LineStripArray a = new LineStripArray(size(), GeometryArray.COORDINATES, new int[] { size() });
			a.setCoordinates(0, toArray(new Point3d[] {}));
			return a;
        }

        /**
         * @param geometryInfoPrimitive
         * @return GeometryArray
         */
        public GeometryArray toGeometry(int geometryInfoPrimitive) {
            GeometryInfo gi = new GeometryInfo(geometryInfoPrimitive);
            gi.setCoordinates(toArray(new Point3d[] {}));
            gi.setStripCounts(new int[] { size() });

            // noch ein paar Beschwörungsformeln, unklar warum...
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
	 * Appearance 1 und 2 werden gemeinsam gesetzt, Appearance 3 unabhängig davon.
	 * </p>
	 */
    private final Shape3D leftCheek;
    /** Rechts */
    private final Shape3D rightCheek;
    /** Mitte */
    private final Shape3D middle;

    /**
     * @param baseColor				Farbe
     * @param appearanceEventSource	ThreeDBot
     */
    public CtBotShape(Color baseColor, ThreeDBot appearanceEventSource) {
    	// "kann kollidieren" - gilt auch für alle Kinder im Szenegraph
    	setPickable(true);

    	// "kann kollidieren" während der Anzeige noch änderbar (World ruft auf uns später setPickable() auf)
    	setCapability(Node.ALLOW_PICKABLE_WRITE);

    	// Form bauen
    	rightCheek = buildRightCheek();
    	addChild(rightCheek);

    	// linke Backe: Shape der rechten Backe, um 180 Grad gedreht
    	leftCheek = (Shape3D)rightCheek.cloneNode(false);
    	TransformGroup g = new TransformGroup(z180aboutCenter());
    	g.addChild(leftCheek);
    	addChild(g);

        // Mittelteil
    	middle = buildMiddle();
    	addChild(middle);

    	// Appearances während der Anzeige änderbar
    	rightCheek.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
    	leftCheek .setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
    	middle    .setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

    	if (baseColor != null) {
    		setMiddleColor(baseColor);
    		setCheeksColor(baseColor);
    	}

    	if (appearanceEventSource != null) {
	    	appearanceEventSource.addAppearanceListener(new Runnable1<Color>() {
				public void run(Color newAppearance) {
					setCheeksColor(newAppearance);
				}
	    	});
    	}
    }

    /**
     * Seite bauen
     *
     * @return 3D-Shape
     */
    protected static Shape3D buildRightCheek() {
    	// Shape (= Backe) besteht aus 3 Teilen: Boden, Decke, Mantel
    	Shape3D rv = new Shape3D();

    	PointList floor = buildCheekArc(- BOT_HEIGHT / 2);
        rv.addGeometry(floor.toFanGeometry());	// ausliefern

        PointList ceil  = buildCheekArc(+ BOT_HEIGHT / 2);
        /*
		 * reverse() um Backface-Culling auszutricksen; "richtiger" wäre, in den PolygonAttributes der
		 * Appearance setCullFace(FRONT) zu setzen, aber das würde die Appearance der gesamten Shape
		 * beeinflussen, und diese soll hier nicht verändert werden.
		 */
        ceil.reverse();
        rv.addGeometry(ceil.toFanGeometry());	// ausliefern

        // Mantel außen (vom Botzentrum weg gewandt); das ist ein Teil eines Zylindermantels
        PointList arcBottom = buildCheekArc(- BOT_HEIGHT / 2);
        PointList arcTop    = buildCheekArc(+ BOT_HEIGHT / 2);
        PointList lateralSurface = arcBottom.interleave(arcTop);

        // Mantel innen (zum Botzentrum gewandt; d.h. Innenwand Mund); wir schließen einfach den Zylindermantel-Abschnitt
        lateralSurface.add(arcBottom.get(0));
        lateralSurface.add(arcTop.get(0));

        // Gesamtmantel ausliefern
        rv.addGeometry(lateralSurface.toStripGeometry());

        return rv;
    }

    /**
     * Mitte bauen
     *
     * @return 3D-Shape
     */
    protected static Shape3D buildMiddle() {
    	/*
    	 * Shape (= Mittelteil) besteht aus 5 Teilen:
    	 * 	1) Zylinderboden,
    	 * 	2) Zyl.-Decke,
    	 * 	3) Abschnitt eines Zyl.-Mantel,
    	 * 	4) 1. Quaderviertel,
    	 * 	5) 2. Quaderviertel
    	 */
    	Shape3D rv = new Shape3D();

    	PointList floor = buildStern(- BOT_HEIGHT / 2);
    	rv.addGeometry(floor.toFanGeometry());

    	PointList ceil  = buildStern(+ BOT_HEIGHT / 2);
    	ceil.reverse();	// wegen Backface-Culling; siehe buildRightCheek()
    	rv.addGeometry(ceil.toFanGeometry());

    	// Zylindermantel-Abschnitt
        PointList bottomArc = buildStern(- BOT_HEIGHT / 2);
        PointList topArc    = buildStern(+ BOT_HEIGHT / 2);
        PointList lateralSurface = bottomArc.interleave(topArc);

        rv.addGeometry(lateralSurface.toStripGeometry());

        // Quaderviertel
        PointList cuboidQuarter = new PointList();
        cuboidQuarter.add((Point3d)topArc.last().clone());	// 0
        cuboidQuarter.add((Point3d)topArc.get(0).clone());	// 1

        Transform3D moveDown = new Transform3D();
        moveDown.setTranslation(new Vector3d(0, - MOUTH_DEPTH_IN_M, 0));
        cuboidQuarter.add( // 2
        	transformPoint(topArc.get(0),  z180aboutCenter(), moveDown));
        cuboidQuarter.add( // 3
        	transformPoint(topArc.last(), z180aboutCenter(), moveDown));
        cuboidQuarter.add( // 4
        	transformPoint(bottomArc.last(), z180aboutCenter(), moveDown));

        rv.addGeometry(cuboidQuarter.toStripGeometry());	// ausliefern

        // Quaderviertel transformieren und nochmal verwenden
        Transform3D y180 = new Transform3D();
        y180.rotY(PI);
        cuboidQuarter.transform(y180);
        rv.addGeometry(cuboidQuarter.toStripGeometry());	// 2. Mal ausliefern

        return rv;
    }

    /**
     * @return 180 Grad Drehung um Z
     */
    protected static Transform3D z180aboutCenter() {
        Transform3D rv = new Transform3D();
        rv.rotZ(PI);
        return rv;
    }

    /**
     * @param z	Z
     * @return Punktliste
     */
    protected static PointList buildStern(double z) {
        return buildBotCircumference(z,
            180 - MOUTH_OPENING_ANGLE_IN_DEG,
            180 + MOUTH_OPENING_ANGLE_IN_DEG);
    }

    /**
     * @param z	Z
     * @return Punktliste
     */
    protected static PointList buildCheekArc(double z) {
        return buildBotCircumference(z,
            MOUTH_OPENING_ANGLE_IN_DEG,
            180 - MOUTH_OPENING_ANGLE_IN_DEG);
    }

    /**
     * @param z	Z
     * @param fromAngleInDeg	von (Winkel in Deg)
     * @param toAngleInDeg		bis (Winkel in Deg)
     * @return Punktliste
     */
    protected static PointList buildBotCircumference(double z, double fromAngleInDeg, double toAngleInDeg) {
        PointList rv = new PointList();
        for (double a = fromAngleInDeg; a < toAngleInDeg; a += CORNER_INTERVAL_IN_DEG) {
            rv.add(buildPointOnCircumference(z, a));
        }
        // sicherstellen, dass genau der Zielwinkel erreicht wird (nicht vorher aufgehört)
        rv.add(buildPointOnCircumference(z, toAngleInDeg));
        return rv;
    }

    /**
     * @param z				Z
     * @param angleInDeg	Winkel
     * @return 3D-Punkt
     */
    protected static Point3d buildPointOnCircumference(double z, double angleInDeg) {
        double angleInRad = toRadians(angleInDeg);
        return new Point3d(
        	BOT_RADIUS * sin(angleInRad),
            BOT_RADIUS * cos(angleInRad),
            z);
    }

    /**
     * transformiert einen Punkt
     *
     * @param p				3D-Punkt
     * @param transforms	Transformierung(en)
     * @return 3D-Punkt
     */
    private static Point3d transformPoint(Point3d p, Transform3D... transforms) {
    	Point3d rv = (Point3d)p.clone();
    	for (Transform3D t : transforms)
    		t.transform(rv);
        return rv;
    }

    /**
     * @param c	Farbe
     */
    protected void setMiddleColor(Color c) {
    	middle.setAppearance(getBotAppearance(c));
    }

    /**
     * @param c	Farbe
     */
    protected void setCheeksColor(Color c) {
    	leftCheek .setAppearance(getBotAppearance(c));
    	rightCheek.setAppearance(getBotAppearance(c));
    }

    /**
     * @param c	Farbe
     * @return Bot-Appearance
     */
    protected Appearance getBotAppearance(Color c) {
    	Appearance rv = new Appearance();
    	rv.setColoringAttributes(new ColoringAttributes(new Color3f(c), ColoringAttributes.SHADE_GOURAUD));
    	return rv;
    }
}
