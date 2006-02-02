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

package ctSim.Controller;

import javax.swing.JOptionPane;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.Model.*;
import ctSim.View.*;
import ctSim.*;

/**
 * Die Startklasse des c't-Sim; stellt die Welt und die grafischen
 * Anzeigefenster bereit, kontrolliert den gesamten Ablauf des Simulators.
 * 
 * @author pek (pek@heise.de)
 * 
 */
public class Controller {
	private static World world;

	private static ControlFrame controlFrame;

	private static boolean test;

	/**
	 * Startet den c't-Sim
	 * 
	 * @param args
	 *            Argumente werden nicht eingelesen
	 */
	public static void main(String[] args) {

		System.out.println("Simulator startet");
		world = new World();
		controlFrame = new ControlFrame();

		Object[] options = { "externer TCP/IP-Bot", "integrierter Testbot" };
		int n = JOptionPane.showOptionDialog(null,
				"Mit welchem Bot-Typ wollen Sie den Simulator betreiben?", "Frage",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, options, options[1]);

		test = true;
		
		if (n==0){
			test= false;
		} else {
			options[0] = "1 Bot";
			options[1] = "3 Bots";
			n = JOptionPane.showOptionDialog(null,
					"Wieviele Bots sollen gestartet werden?", "Frage",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, options, options[1]);
		}
				
		controlFrame.setVisible(true);
		world.setControlFrame(controlFrame);
		world.start();


		// Falls true, hoert der Simulator nicht auf TCP/IP-Verbindunen,
		// sondern beguegt sich mit einem Bot vom Typ CtBotSimTest

		if (test) {
			addBot("testbot", "Testbot", new Point3f(0f, 1.5f, 0f),
					new Vector3f(1f, 0f, 0f));
			if (n > 0) {
				addBot("testbot", "Testbot2", new Point3f(0f, 1f, 0f),
						new Vector3f(0f, 1f, 0f));
				addBot("testbot", "Testbot3", new Point3f(0f, -1.5f, 0f),
						new Vector3f(-1f, 0f, 0f));
			}
		} else {
			System.out.println("Warte auf Verbindung vom c't-Bot");
			addBot("BotSimTcp", "BotSimTcp", new Point3f(0f, 1.5f, 0f),
					new Vector3f(-1f, 0f, 0f));
		}
	}

	/*
	 * Fuegt der Welt einen neuen Bot des gewuenschten Typs hinzu
	 * 
	 * Typ ist entweder "Testbot" oder "BotSimTCP"
	 * 
	 */
	private static void addBot(String type, String name, Point3f pos,
			Vector3f head) {
		Bot bot = null;

		if (type.equalsIgnoreCase("testbot")) {
			bot = new CtBotSimTest(pos, head);
		}

		// TODO: In spaeteren Versionen soll hier ein eigener Thread
		// auf weitere Verbindungsversuche anderer Bots lauschen
		if (type.equalsIgnoreCase("BotSimTcp")) {
			TcpConnection listener = new TcpConnection();
			listener.listen(10001);
			bot = new CtBotSimTcp(pos, head, listener);
		}

		if (bot != null) {
			bot.providePanel();
			bot.setBotName(name);
			world.addBot(bot);
			controlFrame.addBot(bot);
			// Dann wird der eigene Bot-Thread gestartet:
			bot.start();
		}
	}

	/**
	 * Beendet die Simulation, wird durch den "Beenden"-Knopf des Fensters
	 * ControlFrame aufgerufen
	 */
	public static void endSim() {
		world.die();
		controlFrame.dispose();
		System.exit(0);
	}

	/**
	 * Gibt der Welt den Hinweis, dass sich ihr Zustand geaendert hat
	 */
	public static void reactToChange() {
		world.reactToChange();
	}

	/**
	 * @return Gibt eine Referenz auf das Fenster mit den Kontrolltafeln
	 *         zurueck.
	 */
	public static ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @return Gibt eine Referenz auf die simulierte Welt zurueck.
	 */
	public static World getWorld() {
		return world;
	}
}
