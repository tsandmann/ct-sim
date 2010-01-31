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

import ctSim.model.ThreeDBot;
import ctSim.model.bots.Bot;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.model.bots.ctbot.RealCtBot;

/**
 * @author Felix Beckwermert
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class BotViewer extends JScrollPane {
	/** UID */
	private static final long serialVersionUID = - 7367493564649395707L;

	/** Buisitors */
	private Class[] buisitors;

	/** zugehoeriger Bot */
	public final Bot bot;

	/**
	 * @param bot	Bot, zu dem der Viewer gehoert
	 */
	public BotViewer(Bot bot) {
		super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
		this.bot = bot;
		
		/* buisitors nach Bot-Art anlegen */
		Bot botinstance = null;
		if (bot instanceof ThreeDBot) {
			/* simulierter Bot */
			botinstance = ((ThreeDBot) bot).getSimBot();
		} else if (bot instanceof RealCtBot) {
			/* echter Bot */
			botinstance = bot;
		} else {
			throw new AssertionError("Unbekannter Bot-Typ");
		}
		if (botinstance instanceof CtBotSimTest) {
			/* Test-Bots haben kein LCD, LOG, RemoteCall, ABL, Mausbild */
			buisitors = new Class[] {
					Tables.Position.class,
					Tables.Sensors.class,
					Tables.Actuators.class,
				};
		} else {
			buisitors = new Class[] {
					Tables.Position.class,
					Leds.class,
					AndEverything.class,
					Tables.Sensors.class,
					Tables.Actuators.class,
					MousePictureViewer.class,
				};
		}
		
		/* Panelbreite soll mindestens so gross sein, dass alle Elemente darin komplett sichtbar sind */
		this.setMinimumSize(new Dimension(220, this.getHeight()));

		setBorder(null);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (Class<?> b : buisitors) {
			try {
				GuiBotBuisitor buisitor = (GuiBotBuisitor)b.newInstance();
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
}
