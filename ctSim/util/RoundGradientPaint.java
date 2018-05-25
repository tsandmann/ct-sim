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
 * Ermöglicht runde (radiale) Farbverläufe. Implementierung mit Inspiration von
 * <a href="http://www.oreilly.com/catalog/java2d/chapter/ch04.html"></a>
 * </p>
 * <p>
 * Beispiel: Ein Farbverlauf, dessen Hintergrund rot ist und der mit Pixel x=42 y=37 als Zentrum einen
 * blauen radialen Farbverlauf mit 20 Pixel Radius hat
 * </p>
 *
 * <pre>
 * class EineComponent extends JComponent {
 *     Paint paint = new RoundGradientPaint(42, 37, Color.BLUE, 20, Color.RED);
 *
 *     @Override
 *     protected void paintComponent(Graphics graphics) {
 *         Graphics2D g = (Graphics2D)graphics;
 *         g.setPaint(paint);
 *         g.fillRect(...); // Wird gefüllt mit Farbverlauf
 *     }
 *     ...
 * }
 * </pre>
 *
 * @author Hendrik Krauß
 */
public class RoundGradientPaint implements Paint {
	/** RoundGradientPaint */
	public class RoundGradientContext implements PaintContext {
		/** Mittelpunkt */
		protected final Point2D center;
		/** Radius */
		protected final double radius;

		/**
		 * @param center
		 * @param radius
		 */
		public RoundGradientContext(Point2D center, double radius) {
			this.center = center;
			this.radius = radius;
		}

		/**
		 * @see java.awt.PaintContext#dispose()
		 */
		public void dispose() {
			// No-op
		}

		/**
		 * @see java.awt.PaintContext#getColorModel()
		 */
		public ColorModel getColorModel() {
			return ColorModel.getRGBdefault();
		}

		/**
		 * @see java.awt.PaintContext#getRaster(int, int, int, int)
		 */
		public Raster getRaster(int baseX, int baseY, int width, int height) {
			WritableRaster raster = getColorModel().createCompatibleWritableRaster(width, height);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double distance = center.distance(baseX + x, baseY + y);
					// Verhältnis Abstand / Radius
					double ratio = Misc.clamp(distance / radius, 1);
					raster.setPixel(x, y, getPixelColor(ratio));
				}
			}
			return raster;
		}

		/**
		 * @param ratio
		 * @return Pixelfarbe
		 */
		private double[] getPixelColor(double ratio) {
			return new double[] {
				interpolate(centerColor.getRed()  , ratio, bgColor.getRed()),
				interpolate(centerColor.getGreen(), ratio, bgColor.getGreen()),
				interpolate(centerColor.getBlue() , ratio, bgColor.getBlue()),
				interpolate(centerColor.getAlpha(), ratio, bgColor.getAlpha())
			};
		}

		/**
		 * @param a
		 * @param ratio
		 * @param b
		 * @return Interpolation
		 */
		private double interpolate(int a, double ratio, int b) {
			return a + ratio * (b - a);
		}

	}

	/** Center */
	protected final Point2D rawCenter;
	/** Radius */
	protected final double rawRadius;
	/** Center-Farbe */
	protected final Color centerColor;
	/** Farbe */
	protected final Color bgColor;

	/**
	 * @param centerX
	 * @param centerY
	 * @param centerColor
	 * @param radius
	 * @param backgroundColor
	 */
	public RoundGradientPaint(double centerX, double centerY, Color centerColor, double radius, Color backgroundColor) {

		if (radius <= 0)
			throw new IllegalArgumentException();

		rawCenter = new Point2D.Double(centerX, centerY);
		this.centerColor = centerColor;
		this.rawRadius = radius;
		this.bgColor = backgroundColor;
	}

	/**
	 * @see java.awt.Paint#createContext(java.awt.image.ColorModel, java.awt.Rectangle, java.awt.geom.Rectangle2D, java.awt.geom.AffineTransform, java.awt.RenderingHints)
	 */
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
			AffineTransform aff, RenderingHints h) {

		Point2D transformedRadius = aff.deltaTransform(new Point2D.Double(0, rawRadius), null);
		return new RoundGradientContext(aff.transform(rawCenter, null), transformedRadius.distance(0, 0));
	}

	/**
	 * @see java.awt.Transparency#getTransparency()
	 */
	public int getTransparency() {
		if (centerColor.getTransparency() == OPAQUE &&  bgColor.getTransparency() == OPAQUE)
			return OPAQUE;
		else
			return TRANSLUCENT;
	}
}
