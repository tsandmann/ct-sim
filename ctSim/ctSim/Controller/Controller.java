package ctSim.Controller;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.Model.*;
import ctSim.View.*;
import ctSim.*;

public class Controller {
	protected static World world;	
	protected static WorldView worldView; 
	protected static ControlFrame controlFrame;
		
	// If true, there is no tcp/ip-connection and just one BotSimTest!
	private static boolean test;
	
	// TODO Implement dieing
	
	public static void main(String[] args) {		
		world = new World();
		worldView = new WorldView();
		world.setWorldView(worldView);		
		controlFrame = new ControlFrame();
		controlFrame.setVisible(true);
		world.setControlFrame(controlFrame);
		world.start();
		
		test = true;
		if (test){
			System.out.println("Test: Initializing Schnuffi");
			addBot("testbot", "Testbot",new Point3f(0.2f,0f,0f),new Vector3f(-1f,0f,0f));
//			System.out.println("Test: Initializing Rappacino");
//			addBot("testbot", "Rappacino",new Point3f(-0.2f,0.0f,0f),new Vector3f(1f,0f,0f));
		} else {
			System.out.println("Initializing Connectiong to c't-Bot");
			addBot("BotSimTcp", "BotSimTcp",new Point3f(0.0f,0.5f,0f),new Vector3f(1f,0f,0f));
		}		
	}
	
	private static void addBot(String type, String name, Point3f pos, Vector3f head){
		Bot bot = null;
		
		if (type.equalsIgnoreCase("testbot")){
			bot = new CtBotSimTest(pos,head);			
		}

		// This should be done by an own thread later on!!!
		if (type.equalsIgnoreCase("BotSimTcp")){
			TcpConnection listener = new TcpConnection();
			listener.listen(10001);			
			bot = new CtBotSimTcp(pos,head,listener);
		}

		if (bot != null) {
			bot.providePanel();
			bot.setBotName(name);
			world.addBot(bot);	
			controlFrame.addBot(bot);
			bot.start();
		}

		
		
//		noteChange();
	}
	
	public static void noteChange(){
		worldView.repaint();
	}

	/**
	 * @return Returns the controlFrame.
	 */
	public static ControlFrame getControlFrame() {
		return controlFrame;
	}

	/**
	 * @param controlFrame The controlFrame to set.
	 */
	public static void setControlFrame(ControlFrame controlFrame) {
		Controller.controlFrame = controlFrame;
	}

	/**
	 * @return Returns the world.
	 */
	public static World getWorld() {
		return world;
	}

	/**
	 * @param world The world to set.
	 */
	public static void setWorld(World world) {
		Controller.world = world;
	}

	/**
	 * @return Returns the worldView.
	 */
	public static WorldView getWorldView() {
		return worldView;
	}

	/**
	 * @param worldView The worldView to set.
	 */
	public static void setWorldView(WorldView worldView) {
		Controller.worldView = worldView;
	}	
}
