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
 * @author Christoph Grimmer (c.grimmer@futurio.de)
 * 
 */
public class Controller {
	private static World world;

	private static ControlFrame controlFrame;

	private static boolean test;
	private static boolean serial;

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
		serial=false;
		
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

		if (test) {
			addBot("testbot", "Testbot1", new Point3f(0f, 1.5f, 0f),
					new Vector3f(1f, 0f, 0f));
			if (n > 0) {
				addBot("testbot", "Testbot2", new Point3f(0f, 1f, 0f),
						new Vector3f(0f, 1f, 0f));
				addBot("testbot", "Testbot3", new Point3f(0f, -1.5f, 0f),
						new Vector3f(-1f, 0f, 0f));
			}
		}
		
		
		/*
		 * Der Sim sollte auch auf die TCP-Verbindung lauschen,
		 * wenn es Testbots gibt. Aus diesem Grund ist der thread, 
		 * der auf TCP-Verbindungen lauscht, an dieser Stelle eingebaut.
		 */

		System.out.println("Warte auf Verbindung vom c't-Bot");
		SocketListener listener = new SocketListener(10001);
		listener.start();

		if (serial){
			Bot bot = null;
			JD2xxConnection com=new JD2xxConnection();

			try {
				com.connect();
				
				bot = new CtBotRealTcp(new Point3f(0.5f,0f,0f),new Vector3f(1.0f,-0.5f,0f),com);
				System.out.println("Real Bot at COM comming up");
			} catch (Exception ex) {
				ErrorHandler
				.error("Serial Connection not possible: "+ ex);
			}
			
			if (bot != null){		
				bot.providePanel();
				bot.setBotName(bot.getClass().getName());
				Controller.getWorld().addBot(bot);
				Controller.getControlFrame().addBot(bot);
				bot.start();
			} else
				try {
					com.disconnect();
				}catch (Exception ex) {}


		}
	
	
	
	
	}

	/*
	 * Fuegt der Welt einen neuen Bot des gewuenschten Typs hinzu
	 * 
	 * Typ ist entweder "Testbot" oder "BotSimTCP"
	 * 
	 * Crimson 2006-06-13:
	 * Wird vorl�ufig noch f�r die Testbots verwendet. TCPBots k�nnen sich von au�er verbinden ohne dass man sie "einladen" muss.
	 */
	private static void addBot(String type, String name, Point3f pos,
			Vector3f head) {
		Bot bot = null;

		if (type.equalsIgnoreCase("testbot")) {
			bot = new CtBotSimTest(pos, head);
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

class SocketListener extends Thread {
	int port = 0;
	boolean listen = true;
	int num = 0;

	public SocketListener(int port) {
		super();
		this.port = port;
	}

	public void run() {
		Bot bot = null;
		TcpConnection tcp=null;
		try {
			ServerSocket server = new ServerSocket(port);
			while (listen) {
				bot=null;
				tcp = new TcpConnection();
				/*
				 * Da die Klasse TcpConnection staendig den ServerSocket neu aufbaut und wieder zerstoert, umgehe ich die
				 * eingebaute Methode listen() und uebergeben den Socket selbst. Da die TcpConncetion den ServerSocket
				 * aber auch nicht wieder frei gibt, habe ich den urspruenglich vorhandenen Aufruf gaenzlich entfernt.
				 */
				tcp.connect(server.accept());
				
				Command cmd = new Command();
				try {
					while (bot==null){
						if (cmd.readCommand(tcp) ==0) {
							System.out.print("Seq: "+cmd.getSeq()+" CMD: "+cmd.getCommand()+"SUB: "+cmd.getSubcommand()+"\n");
							if (cmd.getCommand() == Command.CMD_WELCOME){
								if (cmd.getSubcommand() == Command.SUB_WELCOME_REAL){
									bot = new CtBotRealTcp(new Point3f(0.5f,0f,0f),new Vector3f(1.0f,-0.5f,0f),tcp);
									System.out.print("Real Bot comming up");
								}else{
									bot = new CtBotSimTcp(new Point3f(0.5f,0f,0f),new Vector3f(1.0f,-0.5f,0f),tcp);
									System.out.print("Virtual Bot comming up");
								}
							}else {
								System.out.print("Non-Welcome-Command found: \n"+cmd.toString()+"\n ==> Bot is already running or deprecated bot \nDefaulting to virtual Bot\n");
								// TODO Strenger pruefen und alle nicht ordentlich angemeldeten Bots abweisen!
								bot = new CtBotSimTcp(new Point3f(0.5f,0f,0f),new Vector3f(1.0f,-0.5f,0f),tcp);								
							} 
						}else
							System.out.print("Broken Command found: \n");
							
					}
				} catch (IOException ex) {
					ErrorHandler
					.error("TCPConnection broken - not possibple to connect: "+ ex);
				}
				
				if (bot != null){		
					bot.providePanel();
					bot.setBotName(bot.getClass().getName()+"_" + num++);
					Controller.getWorld().addBot(bot);
					Controller.getControlFrame().addBot(bot);
					bot.start();
				} else
					try {
						tcp.disconnect();
					}catch (Exception ex) {}
					
			}
		} catch (IOException ioe) {
			System.err.format("Kann nicht an port %d binden.", new Integer(port));
			System.err.println(ioe.getMessage());
		}
	}
}
