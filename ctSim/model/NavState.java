package ctSim.model;

import static ctSim.model.NavState.CoordIdx.X;
import static ctSim.model.NavState.CoordIdx.Y;
import static ctSim.model.NavState.CoordIdx.Z;

import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.SpinnerNumberModel;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.SimUtils;
import ctSim.util.Misc;

public class NavState extends TransformGroup {
	/**
	 * Koordinaten-Index: Die &uuml;blichen Arrays, um Koordinaten aufzuheben,
	 * haben X an Index 0, Y an 1 und Z an 2.
	 */
	enum CoordIdx {
		X, // == 0
		Y, // == 1
		Z  // == 2
	}

	//$$$ Code-Duplikation
	//$$$ doc
	private class CoordinateModel extends SpinnerNumberModel {
        private static final long serialVersionUID = 6998596925610478733L;
		private final CoordIdx whichCoordinate;

		public CoordinateModel(CoordIdx whichCoordinate, double stepSize) {
			super(0d, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
				stepSize);
			this.whichCoordinate = whichCoordinate;
        }

		@Override
		public Object getValue() {
			double[] p = new double[3];
		    getPosition().get(p);
		    return p[whichCoordinate.ordinal()];
		}

		@Override
		public void setValue(Object value)
		throws ClassCastException, IllegalArgumentException {
			Double valAsD = (Double)value; // wirft ggf. ClassCastExcp
			if (valAsD.isNaN() || valAsD.isInfinite())
				throw new IllegalArgumentException(valAsD.toString());
			if (getMinimum().compareTo(value) > 0
			||  getMaximum().compareTo(value) < 0)
				throw new IllegalArgumentException(valAsD.toString());

			double[] v = new double[3];
			getPosition().get(v);
			v[whichCoordinate.ordinal()] = valAsD;
			setPositionInternal(new Point3d(v));
		    super.setValue(value);
		}
	}

	public class HeadingModel extends SpinnerNumberModel {
		private static final long serialVersionUID = - 5199590034511877988L;

		public HeadingModel(double stepSize) {
			super(0d, -180d, 180d, stepSize);
		}

		@Override
		public Double getValue() {
		    return (Double)super.getValue();
		}

		@Override
		public void setValue(Object value)
		throws ClassCastException, IllegalArgumentException {
			Double valAsD = (Double)value; // wirft ggf. ClassCastExcp
			if (valAsD.isNaN() || valAsD.isInfinite())
				throw new IllegalArgumentException(valAsD.toString());
			if (getMinimum().compareTo(value) > 0
			||  getMaximum().compareTo(value) < 0)
				throw new IllegalArgumentException(valAsD.toString());

			valAsD = Misc.normalizeAngle(valAsD);
			setHeadingInternal(valAsD);
		    super.setValue(value);
		}
	}

	//$$$ doc
	protected CoordinateModel xPosModel = new CoordinateModel(X, 0.12);

	//$$$ doc
	protected CoordinateModel yPosModel = new CoordinateModel(Y, 0.12);

	//$$$ doc
	protected CoordinateModel zPosModel = new CoordinateModel(Z, 0.01);

	//$$$ doc
	protected HeadingModel headingAngleModel = new HeadingModel(3);

	public NavState(Point3d position, double headingInDeg) {
		setCapability(ALLOW_TRANSFORM_WRITE);
		setCapability(ALLOW_TRANSFORM_READ);
		setCapability(ALLOW_CHILDREN_WRITE);

		setPosition(position);
		setHeadingInDeg(headingInDeg);
    }

    public synchronized Vector3d getHeading() {
    	return SimUtils.doubleToVec3d(headingAngleModel.getValue());
    }

	protected synchronized void setHeadingInternal(double headingInDeg) {
		// TransformGroup aktualisieren
		Transform3D t = getTransformCopy();
		t.rotZ(Math.toRadians(headingInDeg));
		setTransform(t);
	}

	/** $$$
	 * <p>
	 * Setzt die Blickrichtungs-Komponente. Mehrere Dinge in der gegenwärtigen
	 * Implementierung sind <strong>irreführend</strong>:
	 * <ul>
	 * <li>Blickrichtungen werden mit Vector3d-Objekten ausgedrückt, von diesen
	 * muss jedoch die dritte Komponente (Z) immer 0 sein. (Unvorhersagbares
	 * Verhalten, falls nicht.) Situationen wie z.B., dass der Bot auf einer Rampe steht und etwas nach oben guckt, werden <em>nicht</em>
	 * unterstützt, obwohl der Vector3d anderes weismachen will.</li>
	 * <li>Der Winkel, der von </li>
	 * </ul>
	 * </p>
	 * Die neue Blickrichtung propagiert zu angemeldeten Guis oder
	 * &auml;hnlichen Listenern. Diese Methode ist gedacht f&uuml;r den Aufruf
	 * aus dem Model heraus (nicht aus Guis oder &auml;hnlichen Listenern
	 * heraus).
	 *
	 * @param heading Vektor (im geometrischen Sinne), der die Blickrichtung
	 * beschreibt. Die L&auml;nge des Vektors wird ignoriert, es z&auml;hlt nur
	 * seine Richtung.
	 * @throws IllegalArgumentException, falls Z &ne; 0.
	 */
	public synchronized void setHeadingInDeg(double heading)
	throws IllegalArgumentException {
		// Hauptarbeit
		setHeadingInternal(heading);
		// SpinnerModel aktualisieren
		headingAngleModel.setValue(heading);
	}

	////////////////////////////////////////////////////////////
	/**
	 * Liefert die Positions-Komponente dieses NavState.
	 *
	 * @return Punkt in Weltkoordinaten: In Metern gemessen, Punkt 0,0,0 =
	 * S&uuml;dwestecke des Parcours
	 */
    public synchronized Point3d getPosition() {
    	Vector3d rv = new Vector3d();
    	getTransformCopy().get(new Matrix3d(), rv);
    	return new Point3d(rv);
    }

	/**
	 * Setzt die Positions-Komponente. Die neue Position propagiert
	 * <em>nicht</em> zu angemeldeten Guis oder &auml;hnlichen Listenern.
	 * Diese Methode ist gedacht f&uuml;r den Aufruf aus dem aus unseren
	 * SpinnerModels heraus, d.h. aus Guis oder &auml;hnlichen Listenern heraus.
	 *
	 * @param p Punkt in Weltkoordinaten: In Metern gemessen, Punkt 0,0,0 =
	 * S&uuml;dwestecke des Parcours
	 */
    protected synchronized void setPositionInternal(Point3d p) {
		// Transform3D aktualisieren
    	Transform3D t = getTransformCopy();
    	t.setTranslation(new Vector3d(p));
		setTransform(t);
	}

	/**
	 * Setzt die Positions-Komponente. Die neue Position propagiert zu
	 * angemeldeten Guis oder &auml;hnlichen Listenern. Diese Methode ist
	 * gedacht f&uuml;r den Aufruf aus dem Model heraus (nicht aus Guis oder
	 * &auml;hnlichen Listenern heraus).
	 *
	 * @param p Punkt in Weltkoordinaten: In Metern gemessen, Punkt 0,0,0 =
	 * S&uuml;dwestecke des Parcours
	 */
	public synchronized void setPosition(Point3d p) {
		// Hauptarbeit
		setPositionInternal(p);
		// SpinnerModels aktualisieren
		xPosModel.setValue(p.x);
		yPosModel.setValue(p.y);
		zPosModel.setValue(p.z);
	}

	/**
	 * (Zur Bequemlichkeit) Kopiert die Daten des Transform3D, das mit diesem
	 * NavState (mit dieser TransformGroup) assoziiert ist, in ein neues
	 * Transform3D und liefert dieses zur&uuml;ck.
	 */
	public synchronized Transform3D getTransformCopy() {
		Transform3D rv = new Transform3D();
		getTransform(rv);
		return rv;
	}

	public HeadingModel getHeadingAngleModel() { return headingAngleModel; }

	public CoordinateModel getXPosModel() { return xPosModel; }

	public CoordinateModel getYPosModel() { return yPosModel; }

	public CoordinateModel getZPosModel() { return zPosModel; }
}
