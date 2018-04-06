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

package ctSim.view.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ctSim.model.bots.components.Actuators;
import ctSim.util.Misc;
import ctSim.util.RoundGradientPaint;

/**
 * LED-GUI 
 */
public class Leds extends GuiBotBuisitor {
	/** UID */
	private static final long serialVersionUID = - 8033803343789440470L;

	/**
	 * LED-Viewer
	 */
	static class LedViewer extends JCheckBox {
		/** UID */
    	private static final long serialVersionUID = 5975141457176705163L;

    	/** Radius */
		private static int RADIUS = 11;

		// "Paint" als Nomen, also das, was der Maler an die Wand streicht
		/** an */
		private final Paint paintWhenOn;
		/** aus */
		private final Paint paintWhenOff;

		/**
		 * LED-Viewer
		 * @param model Button
		 * @param editable editierbar?
		 * @param tooltip Tooltip
		 * @param colorWhenOn An-Farbe
		 */
		LedViewer(final ButtonModel model, boolean editable, 
			final String tooltip, Color colorWhenOn) {

			setModel(model);
			model.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					LedViewer.this.setToolTipText(
						tooltip + " \u2013 " // Streckenstrich ("Minuszeichen")
						+ (model.isSelected() ? "leuchtet" : "leuchtet nicht"));
				}
			});
			setSelected(false); // um initialen Tooltip setzen zu lassen
			setEnabled(editable);

			// Farbverläufe setzen
			double center = Math.round(RADIUS / 4.0);
			double radius = Math.round(RADIUS / 2f);
			paintWhenOn  = new RoundGradientPaint(center, center,
				reduceSaturation(colorWhenOn), radius, colorWhenOn);
			paintWhenOff = new RoundGradientPaint(center, center,
				colorWhenOn, radius, colorWhenOn.darker().darker());
		}

		/**
		 * Berechnet aus einer Farbe eine Dunklere
		 * @param c Farbe
		 * @return neue Farbe
		 */
		private static Color reduceSaturation(Color c) {
			float[] rv = new float[3];
			Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), rv);
			rv[1] *= .4;
			return Color.getHSBColor(rv[0], rv[1], rv[2]);
		}

		/**
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D)graphics;
			Misc.enableAntiAliasing(g);

			// Fläche der Komponente loeschen
			g.setColor(getBackground());
			g.fillRect(0, 0, getSize().width, getSize().height);

			// LED-Fläche malen
			g.setPaint(isSelected() ? paintWhenOn : paintWhenOff);
			g.fillOval(0, 0, RADIUS, RADIUS);

			// LED-Rand malen
			g.setColor(Color.GRAY);
			g.drawOval(0, 0, RADIUS - 1, RADIUS - 1);
		}

		/**
		 * @return Größe
		 */
		
		@Override
		public Dimension getPreferredSize() {
			return getMinimumSize();
		}

		/**
		 * @return Größe
		 */
    	
    	@Override
		public Dimension getMinimumSize() {
    		return new Dimension(RADIUS, RADIUS);
    	}
    }

	/**
	 * @param led LED-Buisitor
	 */
	public void buisit(Actuators.Led led) {
		setBorder(new TitledBorder("LEDs"));
		add(new LedViewer(led.getExternalModel(), led.isGuiEditable(), 
			led.getName(), led.getColorWhenOn()));
	}
}
