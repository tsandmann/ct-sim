package ctSim.Controller;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.Model.*;
import ctSim.View.*;
import ctSim.*;

/**
 * Die Startklasse des c't-Sim; stellt die Welt und die grafischen
 * Anzeigefenster bereit, kontrolliert den gesamten Ablauf des Simulators.
 * 
 * @author pek (pek@ctmagazin.de)
 * 
 */
public class Controller {
	protected static World world;
	protected static ControlFrame controlFrame;

	private static boolean test;

	// TODO Implement dieing

	public static void main(String[] args) {
		world = new World();
		controlFrame = new ControlFrame();
		controlFrame.setVisible(true);
		world.setControlFrame(controlFrame);
		world.start();

		// Falls true, hoert der Simulator nicht auf TCP/IP, sondern
		// beguegt sich mit einem Bot vom Typ CtBotSimTest
		test = true;

		if (test) {
			addBot("testbot", "Testbot", new Point3f(0.2f, 0f, 0f),
					new Vector3f(-1f, 0f, 0f));
		} else {
			System.out.println("Initializing Connection to c't-Bot");
			addBot("BotSimTcp", "BotSimTcp", new Point3f(0.0f, 0.5f, 0f),
					new Vector3f(1f, 0f, 0f));
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

		// TODO: In späteren Versionen soll hier ein eigener Thread
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
			bot.start();
		}
	}

	/**
	 * Beendet die Simulation, wird beim Schliessen des Fensters
	 * ControlFrame aufgerufen
	 */
	public static void endSim(){
		world.die();
	}
	
	/**
	 * Gibt der Welt den Hinweis, dass sich ihr Zustand geaendert hat
	 */
	public static void reactToChange() {
		world.reactToChange();
	}
	
	/**
	 * @return Gibt controlFrame zurueck.
	 */
	public static ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @return Gibt world zurueck.
	 */
	public static World getWorld() {
		return world;
	}
}
