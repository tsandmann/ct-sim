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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.TransformGroup;
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
 * @author Peter Koenig (pek@heise.de)
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * @author Benjamin Benz (bbe@heise.de)
 * 
 */
public class Controller {
	/** Das Modell der Welt */
	private World world;

	private WorldView worldView;

	private ControlFrame controlFrame;

	/** Eine Liste aller verbundenen Remote-Views */
	private List<RemoteView> remoteViews;

	/**
	 * Benachrichtige alle Views, dass sich Daten geaendert haben
	 */
	public void update() {
		List<RemoteView> toRemove = null;

		SceneUpdate sU = new SceneUpdate(world.getSceneLight());

		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			try {
				rV.update(sU);
			} catch (IOException ex) {
				ErrorHandler.error("Remote View gestorben");

				// Alle Views merken, die zu entfernen sind
				if (toRemove == null)
					toRemove = new LinkedList<RemoteView>();
				toRemove.add(rV);
			}
		}

		// Views entfernen, die tot sind
		if (toRemove != null) {
			it = toRemove.iterator();
			while (it.hasNext()) {
				removeView((RemoteView) it.next());
			}
		}

		// sc.update(world.getSceneLight().getBots());

		worldView.repaint();
	}

	/**
	 * Frage nach der Betriebsart
	 * 
	 * @return 0 = TCP/Bots; 1= Test-Bots
	 */
	private int askMode() {
		Object[] options = { "externer TCP/IP-Bot", "integrierter Testbot",
				"realer Bot via USB" };
		return JOptionPane.showOptionDialog(null,
				"Mit welchem Bot-Typ wollen Sie den Simulator betreiben?",
				"Frage", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE, null, options, options[1]);

	}

	private int askTestBots() {
		Object[] options = { "1 Bot", "3 Bots" };
		int n = JOptionPane.showOptionDialog(null,
				"Wieviele Bots sollen gestartet werden?", "Frage",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
				options, options[1]);
		if (n == 0)
			return 1;
		else
			return 3;
	}

	// private SceneLight sc;

	/*
	 * Initilaisiert das gesamte Framework
	 */
	private void init() {
		int testBots = 0;
		remoteViews = new LinkedList<RemoteView>();

		world = new World(this);

		controlFrame = new ControlFrame(this);

		// sc= world.exportSceneGraph();

		/* Eine lokale WorldView braucht man */
		worldView = new WorldView();
		worldView.setScene(world.getSceneLight().getScene());
		worldView.initGUI();

		/*
		 * WorldView worldView2 = new WorldView();
		 * worldView2.setScene(sc.getScene()); worldView2.initGUI();
		 */

		int mode = askMode();
		if (mode != 0)
			testBots = askTestBots();

		controlFrame.setVisible(true);
		world.setControlFrame(controlFrame);
		world.start();

		if (mode == 1) {
			for (int i = 0; i < testBots; i++)
				addBot("testbot", "Testbot" + i, new Point3f(0f,
						(float) (1.5 - 0.5 * i), 0f), new Vector3f(1f, 0f, 0f));
		} else if (mode == 2)
			addBot("CtBotRealTcp", "Real Bot", new Point3f(0.5f, 0f, 0f),
					new Vector3f(1f, 0f, 0f));

		/*
		 * Der Sim sollte auch auf die TCP-Verbindung lauschen, wenn es Testbots
		 * gibt. Aus diesem Grund ist der thread, der auf TCP-Verbindungen
		 * lauscht, an dieser Stelle eingebaut.
		 */

		System.out.println("Warte auf Verbindung vom c't-Bot (Port 10001)");
		SocketListener BotListener = new BotSocketListener(10001);
		BotListener.start();

		/*
		 * System.out.println("Warte auf Verbindung von View-Clients (Port
		 * 10002)"); SocketListener ViewListener = new
		 * ViewSocketListener(10002); ViewListener.start();
		 */
	}

	/**
	 * Startet den c't-Sim
	 * 
	 * @param args
	 *            Argumente werden nicht eingelesen
	 */
	public static void main(String[] args) {
		System.out.println("Simulator startet");

		Controller controller = new Controller();
		controller.init();
	}

	/**
	 * Fuegt einen Bot in alle Views ein
	 * 
	 * @param id
	 *            Bezeichner des Bot
	 * @param tg
	 *            Translationsgruppe des Bot
	 * @param rg
	 *            Rotationsgruppe des Bot
	 * @param bg
	 *            BranchGroup
	 */
	public void addBotToView(String id, TransformGroup tg, TransformGroup rg,
			BranchGroup bg) {
		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			rV.addBotToView(id, tg, rg, bg);
		}
	}

	/**
	 * Entfernt einen Bot aus allen Views
	 * 
	 * @param id
	 *            Bezeichner des Bot
	 */
	public void removeBotFromView(String id) {
		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			rV.removeBot(id);
		}
		ErrorHandler
				.error("removeBotFromView nicht vollstaendig implementiert");
		// TODO Was ist mit der lokalen View?
		// TODO Was ist mit den Panels ?
	}

	/*
	 * Fuegt der Welt einen neuen Bot des gewuenschten Typs hinzu
	 * 
	 * Typ ist entweder "Testbot" oder "CtBotRealTcp"
	 * 
	 * Crimson 2006-06-13: Wird vorlaeufig noch fuer die Testbots und "serial
	 * bots" verwendet. TCPBots koennen sich immer von aussen verbinden
	 */
	private void addBot(String type, String name, Point3f pos, Vector3f head) {
		Bot bot = null;

		if (type.equalsIgnoreCase("testbot")) {
			bot = new CtBotSimTest(this, pos, head);
		}

		if (type.equalsIgnoreCase("CtBotRealTcp")) {
			JD2xxConnection com = new JD2xxConnection();
			try {
				com.connect();
				bot = new CtBotRealTcp(this, new Point3f(0.5f, 0f, 0f),
						new Vector3f(1.0f, -0.5f, 0f), com);
				System.out.println("Real Bot at COM comming up");
			} catch (Exception ex) {
				ErrorHandler.error("Serial Connection not possible: " + ex);
			}
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
	 * Entfernt eine View aus der Liste
	 * 
	 * @param remoteView
	 */
	public void removeView(RemoteView remoteView) {
		remoteViews.remove(remoteView);
	}

	/**
	 * Beendet die Simulation, wird durch den "Beenden"-Knopf des Fensters
	 * ControlFrame aufgerufen
	 */
	public void endSim() {
		world.die();
		controlFrame.dispose();

		// Alle Views schliessen
		Iterator it = remoteViews.iterator();
		while (it.hasNext()) {
			RemoteView rV = (RemoteView) it.next();
			rV.die();
		}

		System.exit(0);
	}

	/**
	 * Gibt der Welt den Hinweis, dass sich ihr Zustand geaendert hat
	 */
	public void reactToChange() {
		world.reactToChange();
	}

	/**
	 * @return Gibt eine Referenz auf das Fenster mit den Kontrolltafeln
	 *         zurueck.
	 */
	public ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @return Gibt eine Referenz auf die simulierte Welt zurueck.
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * Basisklasse, die auf einem TCP-Port lauscht
	 * 
	 * @author bbe (bbe@heise.de)
	 * 
	 */
	private abstract class SocketListener extends Thread {
		int port = 0;

		boolean listen = true;

		int num = 0;

		/**
		 * Eroeffnet einen neuen Lauscher
		 * 
		 * @param port
		 *            Port zum Lauschen
		 */
		public SocketListener(int port) {
			super();
			this.port = port;
		}

		/**
		 * Kuemmert sich um die Bearbeitung eingehnder Requests
		 */
		public abstract void run();
	}

	/**
	 * Lauscht auf einem Port und instanziiert dann einen Bot
	 * 
	 * @author bbe (bbe@heise.de)
	 * 
	 */
	private class BotSocketListener extends SocketListener {
		public BotSocketListener(int port) {
			super(port);
		}

		public void run() {
			Bot bot = null;
			TcpConnection tcp = null;
			try {
				ServerSocket server = new ServerSocket(port);
				while (listen) {
					bot = null;
					tcp = new TcpConnection();
					/*
					 * Da die Klasse TcpConnection staendig den ServerSocket neu
					 * aufbaut und wieder zerstoert, wird die eingebaute Methode
					 * listen() uebergangen und der Socket selbst uebergeben. Da
					 * die TcpConncetion den ServerSocket aber auch nicht wieder
					 * frei gibt, wurde der urspruenglich vorhandene Aufruf
					 * gaenzlich entfernt.
					 */
					tcp.connect(server.accept());

					Command cmd = new Command();
					try {
						while (bot == null) {
							if (cmd.readCommand(tcp) == 0) {
								System.out.print("Seq: " + cmd.getSeq()
										+ " CMD: " + cmd.getCommand() + "SUB: "
										+ cmd.getSubcommand() + "\n");
								if (cmd.getCommand() == Command.CMD_WELCOME) {
									if (cmd.getSubcommand() == Command.SUB_WELCOME_REAL) {
										bot = new CtBotRealTcp(Controller.this,
												new Point3f(0.5f, 0f, 0f),
												new Vector3f(1.0f, -0.5f, 0f),
												tcp);
										System.out.print("Real Bot comming up");
									} else {
										bot = new CtBotSimTcp(Controller.this,
												new Point3f(0.5f, 0f, 0f),
												new Vector3f(1.0f, -0.5f, 0f),
												tcp);
										System.out
												.print("Virtual Bot comming up");
									}
								} else {
									System.out
											.print("Non-Welcome-Command found: \n"
													+ cmd.toString()
													+ "\n ==> Bot is already running or deprecated bot \nDefaulting to virtual Bot\n");
									// TODO Strenger pruefen und alle nicht
									// ordentlich angemeldeten Bots abweisen!
									bot = new CtBotSimTcp(Controller.this,
											new Point3f(0.5f, 0f, 0f),
											new Vector3f(1.0f, -0.5f, 0f), tcp);
								}
							} else
								System.out.print("Broken Command found: \n");

						}
					} catch (IOException ex) {
						ErrorHandler
								.error("TCPConnection broken - not possibple to connect: "
										+ ex);
					}

					if (bot != null) {
						bot.providePanel();
						bot.setBotName(bot.getClass().getName() + "_" + num++);
						world.addBot(bot);
						Controller.this.controlFrame.addBot(bot);
						bot.start();
					} else
						try {
							tcp.disconnect();
						} catch (Exception ex) {
						}

				}
			} catch (IOException ioe) {
				System.err.format("Kann nicht an port %d binden.", new Integer(
						port));
				System.err.println(ioe.getMessage());
			}
		}

	}

	/**
	 * Lauscht auf einem Port und versorgt dann eine View mit den initialen
	 * Daten
	 * 
	 * @author bbe (bbe@heise.de)
	 */
	private class ViewSocketListener extends SocketListener {
		/**
		 * Instanziiere den Listener
		 * 
		 * @param port
		 */
		public ViewSocketListener(int port) {
			super(port);
		}

		/**
		 * Lauscht auf eine eingehende Verbindung.
		 */
		public void run() {
			TcpConnection tcp = null;
			try {
				ServerSocket server = new ServerSocket(port);
				while (listen) {
					tcp = new TcpConnection();
					tcp.connect(server.accept());

					RemoteView rm = new RemoteView(tcp);
					rm.init(world.exportSceneGraph());
					remoteViews.add(rm);

				}
			} catch (IOException ioe) {
				System.err.format("Kann nicht an port %d binden.", new Integer(
						port));
				System.err.println(ioe.getMessage());
			}
		}
	}

}