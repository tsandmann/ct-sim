package ctSim.Model;

import java.io.IOException;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;

/**
 * Simulierter Bot, der Daten per TCP/IP- empfängt und ausliefert
 * @author Benjamin Benz (bbe@heise.de)
 */
public class CtBotSimTcp extends CtBotSim {

	/**
	 * Kümmert sich um die Beantwortung eingehender Kommanods
	 * @author Benjamin Benz (bbe@heise.de)
	 */
	private class AnsweringMachine extends Thread{
		
		/** Soll der Thread noch laufen */
		private boolean run = true;
		
		/**
		 * Beendet den Thread<b>
		 * @see AbstractBot#work()
		 */
		public void die(){
			run=false;
			this.interrupt();
			CtBotSimTcp.this.die();	// Alles muss sterben
		}
		
		/**
		 * Kümmert sich um die Beantwortung eingehender Kommanods
		 * @see java.lang.Thread#run()
		 */
		public void run() {
			super.run();
			Command command = new Command();
			int valid=0;
			while (run){
				try {
					valid=command.readCommand(tcpCon);
					if (valid == 0){											// Command is ok					
						//System.out.println(command.toString());
						evaluate_command(command);
					} else
						System.out.println("Invalid Command");
				} catch (IOException ex){
					ErrorHandler.error("TCPConnection broken - BotSimTcp dies: "+ex);
					die();
				}
			}
			die();
		}
	}

	/** Die TCP-Verbindung */
	private TcpConnection tcpCon;
	
	/** Der Anrufbeantowrter für eingehende Kommandos */
	private AnsweringMachine answeringMachine;
	
	/**
	 * @param pos
	 * @param head
	 */
	public CtBotSimTcp(Point3f pos, Vector3f head) {
		super(pos, head);
		// TODO Auto-generated constructor stub
	}


	/** 
	 * Erzeuge einen neuen BotSimTcp mit einer schon bestehenden Verbindung
	 * @param tc Die Verbindung für den Bot 
	 */ 
	public CtBotSimTcp(Point3f pos, Vector3f head, TcpConnection tc) {
		super(pos,head);
		tcpCon = (TcpConnection)tc;
		answeringMachine= new AnsweringMachine();
	}

	/** Sequenznummer der TCP-Packete */
	int seq=0;
	/**
	 * Transmit Sensor-Data to Bot
	 */
	private synchronized void transmitSensors(){
		try {
			Command command = new Command(Command.CMD_SENS_IR,getSensIrL(),getSensIrR(),seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_ENC);
			command.setDataL(getSensEncL()); command.setDataR(getSensEncR());
			setSensEncL((short)0); setSensEncR((short)0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());
			
			command.setCommand(Command.CMD_SENS_BORDER);
			command.setDataL(getSensBorderL()); command.setDataR(getSensBorderR());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_DOOR);
			command.setDataL(getSensDoor()); command.setDataR(0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_LDR);
			command.setDataL(getSensLdrL()); command.setDataR(getSensLdrR());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_LINE);
			command.setDataL(getSensLineL()); command.setDataR(getSensLineR());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

			command.setCommand(Command.CMD_SENS_MOUSE);
			command.setDataL(getSensMouseDX()); command.setDataR(getSensMouseDY());
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());
			
			command.setCommand(Command.CMD_SENS_TRANS);
			command.setDataL(getSensTrans()); command.setDataR(0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());
			
			if (getSensRc5() != 0){
				command.setCommand(Command.CMD_SENS_RC5);
				command.setDataL(getSensRc5()); command.setDataR(42);
				command.setSeq(seq++);
				tcpCon.send(command.getCommandBytes());
				setSensRc5(0);
			}
			
			command.setCommand(Command.CMD_SENS_ERROR);
			command.setDataL(getSensError()); command.setDataR(0);
			command.setSeq(seq++);
			tcpCon.send(command.getCommandBytes());

		} catch (IOException IoEx){
			ErrorHandler.error("Error during sending Sensor data, dieing: "+IoEx);
			die();
		}
	}
	
	//private void transmitCommand();
	

	/**
	 * Wertet ein empfangenes Kommando aus
	 * @param command Das Kommando
	 */
	private void evaluate_command(Command command){
		Command answer = new Command();
		
		if (command.getDirection() == Command.DIR_REQUEST) {	// requests
			// Prepare answer
			answer.setDirection(Command.DIR_ANSWER);
			answer.setCommand(command.getCommand());
			answer.setSubcommand(command.getSubcommand());
			answer.setSeq(command.getSeq());
			
			switch (command.getCommand()) {
		        case Command.CMD_SENS_IR:
		        	answer.setDataL(this.getSensIrL());
		        	answer.setDataR(this.getSensIrR());
		        	break;
		        case Command.CMD_SENS_ENC:
		        	answer.setDataL(this.getSensEncL());
		        	this.setSensEncL((short)0);	// Clean up after transmission
		        	answer.setDataR(this.getSensEncR());
		        	this.setSensEncR((short)0);	// Clean up after transmission
		        	break;
		        case Command.CMD_SENS_BORDER:
		        	answer.setDataL(this.getSensBorderL());
		        	answer.setDataR(this.getSensBorderR());
		        	break;
		        case Command.CMD_SENS_DOOR:
		        	answer.setDataL(this.getSensDoor());
		        	answer.setDataR(0);
		        	break;		        	
		        case Command.CMD_SENS_LDR:
		        	answer.setDataL(this.getSensLdrL());
		        	answer.setDataR(this.getSensLdrR());
		        	break;
		        case Command.CMD_SENS_LINE:
		        	answer.setDataL(this.getSensLineL());
		        	answer.setDataR(this.getSensLineR());
		        	break;	        
		        case Command.CMD_SENS_MOUSE:
			       	answer.setDataL(this.getSensMouseDX());
			       	answer.setDataR(this.getSensMouseDY());
			       	break;	
		        case Command.CMD_SENS_TRANS:
		        	answer.setDataL(this.getSensTrans());
		        	answer.setDataR(0);
		        	break;						    
		        case Command.CMD_SENS_RC5:
		        	answer.setDataL(this.getSensRc5());
		        	this.setSensRc5(0);	// nicht zweimal lesen
		        	answer.setDataR(0);
		        	break;
		        case Command.CMD_SENS_ERROR:
		        	answer.setDataL(this.getSensError());
		        	answer.setDataR(0);
		        	break;
		        	
		        case Command.CMD_AKT_MOT:
		        	this.setAktMotL((short)command.getDataL());
		        	this.setAktMotR((short)command.getDataR());
		        	System.out.println("MotorL:"+command.getDataL()+" MotorR: "+command.getDataR());
		        	break;
		        case Command.CMD_AKT_SERVO:
		        	this.setAktServo(command.getDataL());
		        	break;
		        case Command.CMD_AKT_DOOR:
		        	this.setAktDoor(command.getDataL());
		        	break;
		        case Command.CMD_AKT_LED:
		        	this.setAktLed(command.getDataL());
		        	break;
		        	
		        default: 
		        	ErrorHandler.error("Unknown Command:"+command.toString()); 
		            break;
			}
			System.out.println("Command: "+command.getCommand());
			
			try {
//				tcpCon.send(answer.getCommandBytes());
			} catch (Exception ex){
				ErrorHandler.error("Sending answer failed");
			}
			
		} else {	// Answers
			// Still ToDo
		}
	}

	/**
	 * Hier muss alles rein, was ausgeführt werden soll, 
	 * bevor der Thread seine arbeit aufnimmt work()
	 * @see BotSim#work()
	 */ 
	protected void init() {
		// TODO Auto-generated method stub		
	}

	/**
	 * Hier wird aufgeräumt, wenn die Arbeit beendet ist
	 * connections auflösen, Handler zerstören, usw.
	 * @see BotSim#work()
	 */
	 protected void cleanup() {
		super.cleanup();
		try {
			if (tcpCon != null)
				tcpCon.disconnect();
			if (answeringMachine != null)
				answeringMachine.die();
			super.cleanup();
		} catch (Exception ex){			
		}
	}

	/**
	 *  Startet den Bot und den Thread für eingehende Kommandos
	 * @see java.lang.Thread#start()
	 */
	public synchronized void start() {
		super.start();
		answeringMachine.start();
		
	}


	/**
	 * Überträgt nach dem Aufruf von super.work alle Sensordaten an den Bot
	 * @see ctSim.Model.BotSim#work()
	 */
	protected void work() {
		super.work();
		transmitSensors();
	}
}
