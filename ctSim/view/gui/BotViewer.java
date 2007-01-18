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
package ctSim.view.gui;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import ctSim.model.bots.Bot;

//$$ Doku ConCon nach oben
/**
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class BotViewer extends JScrollPane {
	private static final long serialVersionUID = - 7367493564649395707L;

	private static final Class[] buisitors = {
		//$$$ Wo ist eigentlich Pos hin?
		Leds.class,
		Simple.Actuators.class,
		Simple.Sensors.class,
		AndEverything.class,
    };

	public final Bot bot;

	public BotViewer(Bot bot) {
		this.bot = bot;

		setBorder(null);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (Class<?> b : buisitors) {
			try {
				BotBuisitor buisitor = (BotBuisitor)b.newInstance();
				bot.accept(buisitor);
				if (buisitor.shouldBeDisplayed()) 
					panel.add(buisitor);
			} catch (IllegalAccessException e) {
				/*
				 * Kommt nur vor, wenn ein BotBuisitor keinen Konstruktor hat,
				 * der public und parameterlos ist. Waere ein
				 * Compile-time-Fehler, nur wegen Verwendung von Reflection
				 * sehen wir das erst zur Laufzeit.
				 */
				throw new AssertionError(e);
			} catch (InstantiationException e) {
				// Dito
				throw new AssertionError(e);
			}
		}
		setViewportView(panel);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(180, 600);
	}
}
