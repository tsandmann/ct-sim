/**
 *
 */
package ctSim.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * <p>
 * Erm&ouml;glicht runde (radiale) Farbverl&auml;ufe. Implementierung mit
 * Inspiration von <a
 * href="http://www.oreilly.com/catalog/java2d/chapter/ch04.html">http://www.oreilly.com/catalog/java2d/chapter/ch04.html</a>
 * </p>
 * <p>
 * Beispiel: Ein Farbverlauf, dessen Hintergrund rot ist und der mit Pixel x=42
 * y=37 als Zentrum einen blauen radialen Farbverlauf mit 20 Pixel Radius hat
 *
 * <pre>
 * class EineComponent extends JComponent {
 *     Paint paint = new RoundGradientPaint(42, 37, Color.BLUE, 20, Color.RED);
 *
 *     &#64;Override
 *     protected void paintComponent(Graphics graphics) {
 *         Graphics2D g = (Graphics2D)graphics;
 *         g.setPaint(paint);
 *         g.fillRect(...); // Wird gef&uuml;llt mit Farbverlauf
 *     }
 *     ...
 * }
 * </pre>
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class RoundGradientPaint implements Paint {
	public class RoundGradientContext implements PaintContext {
		protected final Point2D center;
		protected final double radius;

		public RoundGradientContext(Point2D center, double radius) {
			this.center = center;
			this.radius = radius;
		}

		public void dispose() {
			// no-op
		}

		public ColorModel getColorModel() {
			return ColorModel.getRGBdefault();
		}

		public Raster getRaster(int baseX, int baseY, int width, int height) {
			WritableRaster raster = getColorModel()
				.createCompatibleWritableRaster(width, height);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double distance = center.distance(baseX + x, baseY + y);
					// Verhaeltnis Abstand / Radius
					double ratio = Misc.clamp(distance / radius, 1);
					raster.setPixel(x, y, getPixelColor(ratio));
				}
			}
			return raster;
		}

		private double[] getPixelColor(double ratio) {
			return new double[] {
				interpolate(centerColor.getRed()  , ratio, bgColor.getRed()),
				interpolate(centerColor.getGreen(), ratio, bgColor.getGreen()),
				interpolate(centerColor.getBlue() , ratio, bgColor.getBlue()),
				interpolate(centerColor.getAlpha(), ratio, bgColor.getAlpha())
			};
		}

		private double interpolate(int a, double ratio, int b) {
			return a + ratio * (b - a);
		}

	}

	protected final Point2D rawCenter;
	protected final double rawRadius;
	protected final Color centerColor;
	protected final Color bgColor;

	public RoundGradientPaint(double centerX, double centerY, Color centerColor,
		double radius, Color backgroundColor) {

		if (radius <= 0)
			throw new IllegalArgumentException();

		rawCenter = new Point2D.Double(centerX, centerY);
		this.centerColor = centerColor;
		this.rawRadius = radius;
		this.bgColor = backgroundColor;
	}

	@SuppressWarnings("unused") // fuer die Parameter
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
		Rectangle2D userBounds, AffineTransform aff, RenderingHints h) {

		Point2D transformedRadius = aff.deltaTransform(
			new Point2D.Double(0, rawRadius), null);
		return new RoundGradientContext(
			aff.transform(rawCenter, null), transformedRadius.distance(0, 0));
	}

	public int getTransparency() {
		if (centerColor.getTransparency() == OPAQUE
		&&  bgColor.getTransparency() == OPAQUE)
			return OPAQUE;
		else
			return TRANSLUCENT;
	}
}
