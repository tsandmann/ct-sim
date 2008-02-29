package ctSim;

import java.io.IOException;
import java.net.ServerSocket;

import ctSim.model.Command;

/**
 * Diese Klasse scheint ein extra Tool zu sein, was nicht direkt zum Sim
 * gehoert. Hab keine Ahnung, wofuer das sein soll. Ich nehm die Datei aus dem
 * Build Path, damit man bei Namensaenderungen in Sim-Klassen das nicht immer
 * hier mitfuehren muss. --hkr@heise.de
 */
public class EchoTest {

	public EchoTest() {
		super();
	}

	int lastTransmittedSimulTime =0;
	int seq =0;
	
	public void send(Connection connection) throws IOException{
		Command command = new Command();
		lastTransmittedSimulTime+=1;//(int)world.getSimulTime();
		command.setCommand(Command.CMD_DONE);
		command.setDataL(lastTransmittedSimulTime);
		command.setDataR(0);
		command.setSeq(seq++);
		connection.send(command.getCommandBytes());
//		System.out.println(world.getRealTime()+"ms: requesting @"+lastTransmittedSimulTime+" ms");

	}
	
	long t1, t2;
	public void receiveCommands(Connection connection) throws IOException {
		long start, duration;
		int run=0;

		t1= System.nanoTime()/1000;

		long aussen= t1-t2;
		
		while (run==0) {
			Command command = new Command();
			start= System.nanoTime();
			command.readCommand(connection);
			duration= (System.nanoTime()-start)/1000;

			if (command.getCommand() ==  Command.CMD_DONE) 
				if (command.getDataL() == lastTransmittedSimulTime)
					run=1;
			
			System.out.println("habe auf Kommando "+(char)command.getCommand()+" "+duration+" usec gewartet");
		}
		
		t2 = System.nanoTime()/1000;
		System.out.println("zeit in receiveCommands: "+(t2-t1)+" us   --  Zeit ausserhalb :"+aussen+ " us" );
	}	
	
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			EchoTest et = new EchoTest();
			
		//	long sendTime;
			
			ServerSocket server = new ServerSocket(10001);
			TcpConnection tcp = new TcpConnection();
	
			tcp.connect(server.accept());
			System.out.println("Eingehende Verbindung auf dem Bot-Port");
	
			while(1==1){
				et.send(tcp);
			//	sendTime=System.nanoTime()/1000;
				et.receiveCommands(tcp);
			}
			
		} catch (IOException ex){
			ex.printStackTrace();
		}
	}
}
