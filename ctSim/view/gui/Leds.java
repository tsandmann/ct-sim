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
import ctSim.util.Buisitor.Buisit;

//$$ doc
public class Leds extends BotBuisitor {
	private static final long serialVersionUID = - 8033803343789440470L;

	static class LedViewer extends JCheckBox {
    	private static final long serialVersionUID = 5975141457176705163L;

		private static int RADIUS = 11;

		// "Paint" als Nomen, also das, was der Maler an die Wand streicht
		private final Paint paintWhenOn;
		private final Paint paintWhenOff;

		LedViewer(final ButtonModel model, boolean editable, 
			final String tooltip, Color colorWhenOn) {

			setModel(model);
			model.addChangeListener(new ChangeListener() {
				public void stateChanged(
					@SuppressWarnings("unused") ChangeEvent e) {
					LedViewer.this.setToolTipText(
						tooltip + " \u2013 " // Streckenstrich ("Minuszeichen")
						+ (model.isSelected() ? "leuchtet" : "leuchtet nicht"));
				}
			});
			setSelected(false); // um initialen Tooltip setzen zu lassen
			setEnabled(editable);

			// Farbverlaeufe setzen
			double center = Math.round(RADIUS / 4.0);
			double radius = Math.round(RADIUS / 2f);
			paintWhenOn  = new RoundGradientPaint(center, center,
				reduceSaturation(colorWhenOn), radius, colorWhenOn);
			paintWhenOff = new RoundGradientPaint(center, center,
				colorWhenOn, radius, colorWhenOn.darker().darker());
		}

		private static Color reduceSaturation(Color c) {
			float[] rv = new float[3];
			Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), rv);
			rv[1] *= .4;
			return Color.getHSBColor(rv[0], rv[1], rv[2]);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D)graphics;
			Misc.enableAntiAliasing(g);

			// Flaeche der Komponente loeschen
			g.setColor(getBackground());
			g.fillRect(0, 0, getSize().width, getSize().height);

			// LED-Flaeche malen
			g.setPaint(isSelected() ? paintWhenOn : paintWhenOff);
			g.fillOval(0, 0, RADIUS, RADIUS);

			// LED-Rand malen
			g.setColor(Color.GRAY);
			g.drawOval(0, 0, RADIUS - 1, RADIUS - 1);
		}

		@Override
		public Dimension getPreferredSize() {
			return getMinimumSize();
		}

    	@Override
    	public Dimension getMinimumSize() {
    		return new Dimension(RADIUS, RADIUS);
    	}
    }

	@Buisit
	public void buisit(Actuators.Led led) {
		setBorder(new TitledBorder("LEDs"));
		add(new LedViewer(led.getExternalModel(), led.isGuiEditable(), 
			led.getName(), led.getColorWhenOn()));
	}
}
