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

package ctSim;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import ctSim.model.Command;
import ctSim.util.FmtLogger;

/**
 * Diese Klasse scheint ein extra Tool zu sein, was nicht direkt zum Sim
 * gehoert. Hab keine Ahnung, wofuer das sein soll. Ich nehm die Datei aus dem
 * Build Path, damit man bei Namensaenderungen in Sim-Klassen das nicht immer
 * hier mitfuehren muss. --hkr@heise.de
 */
public class TestServer implements Runnable {
	FmtLogger lg = FmtLogger.getLogger("ctSim.TestServer");
	
	public static final String  HOST        = "localhost";
	public static final int     PORT        = 10001;
	
	/** 
	 * Content, der hin und her geschickt wird...
	 * 
	 */
	public static final String  CONTENT     = "Test";
	
	/**
	 * Will man einen Java-Client?
	 * 
	 * Ansonsten C-Client "von Hand" starten...
	 * 
	 */
	public static final boolean JAVA_CLIENT = true;
	
	/** 
	 * Wie oft soll der Java-Client "flushen"?
	 * 
	 * Wenn TRUE, dann jedes byte, sonst
	 * ca. jedes "Command"...
	 * 
	 */
	public static final boolean FLUSH_ALL   = false;
	
//	/** 
//	 * TRUE: Server -> Client -> Server
//	 * FALSE: Client -> Server -> Client
//	 * 
//	 */
//	public static final boolean SERVER_TIME = true;
	
	/** 
	 * Soll der Server einen Worker-Thread
	 * benutzen, der den Ctrl. im Sim simuliert?
	 * 
	 * (synchronisiert wie im Sim der Ctrl.)
	 * 
	 */
	public static final boolean USE_WORKER  = true;
	
	/**
	 * Wie lange der Worker pro Zyklus warten soll,
	 * bevor er den Com-Thread wieder laufen laesst
	 * (simuliert Arbeit des Ctrl.)
	 * 
	 */
	public static final long    WORKER_WAIT = 100;
	
	/**
	 * Tick-Rate im Simulator: Jede Runde wird
	 * mindestens diese Zeit gewartet
	 * (+ simulierte Arbeit -- s.o.)
	 * 
	 */
	public static final long    TICK_RATE   = 10;
	
	/**
	 * Server-Connection mit _einem_ Client. Diese entspricht einem
	 * Java-Bot im Simulator.
	 * 
	 * Bekommt vom Server den Client_Port uebergeben
	 * und kommuniziert dann mit diesem.
	 * 
	 * Je nach Einstellung von SERVER_TIME schickt er...
	 * - wenn TRUE: ein Datum an den Client und wartet auf die Antwort
	 * - wenn FALSE: das ankommende Datum einfach an den Client zurueck
	 * 
	 * Nach einem Empfangs-/Sende-Vorgang wird auf den Worker
	 * (wenn eingestellt ueber USE_WORKER) gewartet...
	 * 
	 */
	class ServerCom
			extends Connection
			implements Runnable {
		
		private Socket socket;
		
		private Thread thrd;
		
		private Worker worker;
		
//		private PrintWriter out;
//		private BufferedReader in;
		
		private DataInputStream  in;
		private DataOutputStream out;
		
		ServerCom(Socket socket) {
			
			if(TestServer.USE_WORKER) {
				this.worker = new Worker();
				this.worker.start();
			}
			
			try {
				this.socket = socket;
//				this.out = new PrintWriter(socket.getOutputStream(), true);
//				this.in = new BufferedReader(
//						new InputStreamReader(
//						socket.getInputStream()));
				this.in  = new DataInputStream(this.socket.getInputStream());
				this.out = new DataOutputStream(this.socket.getOutputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		public void start() {
			
			this.thrd = new Thread(this);
			
			this.thrd.start();
		}
		
		public void stop() {
			
			Thread dummy = this.thrd;
			
			this.thrd = null;
			
			dummy.interrupt();
		}
		
		public void run() {
			
			Thread thisThrd = Thread.currentThread();
			
			System.out.println("Connection wurde hergestellt...");
			
			try {
				send((new Command(Command.CMD_WELCOME, 0,	0, 0)).getCommandBytes());
				System.out.println("Willkommen gesendet...");
			} catch (IOException e2) {
				e2.printStackTrace();
				System.exit(-1);
			}
			
			System.out.println("Warten auf willkommen...");
			
			Command cmd = new Command();
			try {
				if (cmd.readCommand(this) == 0) {
					
					if (cmd.getCommand() == Command.CMD_WELCOME) {
						System.out.println("Willkommen...");
					} else {
						System.out.println("Nix willkommen...");
						System.exit(-1);
					}
				} else {
					System.out.println("Fehler: Kein Willkommen!");
					System.exit(-1);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}
			
			while(this.thrd == thisThrd) {
				try {
					
//					if(TestServer.SERVER_TIME) {
//						
//						long time = System.nanoTime();
//						
//						this.out.println("Test");
//						String str = this.in.readLine();
//						
//						if (str != null)
//							System.out.println(String.format("SERVER: Antwort nach " +
//															//"%9d ns", (System.nanoTime()-time)));
//															"%4.3f ms", ((float) (System.nanoTime()-time)) / 1000000.));
//						else
//							//throw (new IOException("Stream ends here or connection broken"));
//							break;
//						
//					} else {
//						this.out.println(this.in.readLine());
//					}
					
					long time = System.nanoTime();
					
					transmitSensors();
					receiveCommands();
					
					System.out.println(String.format("SERVER: Antwort nach " +
							//"%9d ns", (System.nanoTime()-time)));
							"%4.3f ms", ((float) (System.nanoTime()-time)) / 1000000.));
					
					if(this.worker != null)
						this.worker.waitOnWorker();
				
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			try {
				this.out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				this.in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				this.socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(-1);
		}
		
		// Erweiterung f�r Connection-Ged�ns aus dem Bot:
		
		// Kopiert aus Connection:
		public void send(byte sendByte[]) throws IOException {
			try {
				synchronized (this.out) {
					this.out.write(sendByte);
					this.out.flush(); // write dos
				}
			} catch (IOException iOEx) {
				throw iOEx;
			}
		}
		
		public void read(byte[] b) throws IOException {
			in.readFully( b, 0, b.length);
		}

		public void read(byte[] b, int len) throws IOException {
			in.readFully( b, 0, len);
		}
		
		// Connection-Ged�ns aus dem Bot
		private int seq = 0;
		
		private synchronized void transmitSensors() {
			try {
				
				Command command = new Command();
				command.setCommand(Command.CMD_SENS_IR);
				command.setDataL(1000);
				command.setDataR(1000);
//				command.setDataL(((Double)this.irL.getValue()).intValue());
//				command.setDataR(((Double)this.irR.getValue()).intValue());
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
				command.setCommand(Command.CMD_SENS_ENC);
//				command.setDataL((Integer)this.encL.getValue());
//				command.setDataR((Integer)this.encR.getValue());
				command.setDataL(1000);
				command.setDataR(1000);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
				command.setCommand(Command.CMD_SENS_BORDER);
//				command.setDataL(((Short)this.borderL.getValue()).intValue());
//				command.setDataR(((Short)this.borderR.getValue()).intValue());
				command.setDataL((short)1000);
				command.setDataR((short)1000);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
				// TODO
				command.setCommand(Command.CMD_SENS_DOOR);
				command.setDataL(0);
				command.setDataR(0);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());

				command.setCommand(Command.CMD_SENS_LDR);
//				command.setDataL((Integer)this.lightL.getValue());
//				command.setDataR((Integer)this.lightR.getValue());
				command.setDataL(1000);
				command.setDataR(1000);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
				command.setCommand(Command.CMD_SENS_LINE);
//				command.setDataL(((Short)this.lineL.getValue()).intValue());
//				command.setDataR(((Short)this.lineR.getValue()).intValue());
				command.setDataL((short) 1000);
				command.setDataR((short) 1000);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
//				if(this.getObstState() != OBST_STATE_NORMAL) {
//					this.mouseX = 0;
//					this.mouseY = 0;
//				}
				command.setCommand(Command.CMD_SENS_MOUSE);
//				command.setDataL(this.mouseX);
//				command.setDataR(this.mouseY);
				command.setDataL(0);
				command.setDataR(0);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
				// TODO: nur fuer real-bot
				command.setCommand(Command.CMD_SENS_TRANS);
				command.setDataL(0);
				command.setDataR(0);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());

//				Object rc5 = this.rc5.getValue();
//				if(rc5 != null) {
//					
//					Integer val = (Integer)rc5;
//					
//					if(val != 0) {
//						command.setCommand(Command.CMD_SENS_RC5);
//						command.setDataL(val);
//						command.setDataR(42);
//						command.setSeq(seq++);
//						this.send(command.getCommandBytes());
//					}
//				}
				
				// TODO: nur fuer real-bot
				command.setCommand(Command.CMD_SENS_ERROR);
				command.setDataL(0);
				command.setDataR(0);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());

				
//				lastTransmittedSimulTime= (int)world.getSimulTime();
//				lastTransmittedSimulTime %= 10000;	// Wir haben nur 16 Bit zur verfuegung und 10.000 ist ne nette Zahl ;-)
				command.setCommand(Command.CMD_DONE);
//				command.setDataL(lastTransmittedSimulTime);
				command.setDataL(10);
				command.setDataR(0);
				command.setSeq(this.seq++);
				this.send(command.getCommandBytes());
				
			} catch (IOException e) {
				lg.severe(e, "Error sending Sensor data, dying");
//				die();
				System.exit(-1);
			}
		}
		
		public void receiveCommands() {
			
			int valid = 0;
			int run   = 0;
			
			while (run==0) {
				try {
					Command command = new Command();
					
					valid = command.readCommand(this);
					
					if (valid == 0) {// Kommando ist in Ordnung
						run = storeCommand(command);
					} else
						System.out.println("Ungueltiges Kommando"); //$NON-NLS-1$
				} catch (IOException e) {
					lg.severe(e, "Verbindung unterbrochen -- Bot stirbt");
//					die();
					System.exit(-1);
				}
			}
		}
		
		private ArrayList<Command> commandBuffer = new ArrayList<Command>();
		CountDownLatch waitForCommands = new CountDownLatch(1);
		
		public int storeCommand(Command command) {
			int result=0;
			synchronized (commandBuffer) {
				
				commandBuffer.add(command);
				
				if (command.getCommand() ==  Command.CMD_DONE){ 
					
					//if (command.getDataL() == lastTransmittedSimulTime){
					if (command.getDataL() == 10){
						
						result = 1;
						
						waitForCommands.countDown();
					}
				}
			}
			return result;
		}
	}
	
	/**
	 * Optional einzustellender Worker. Dieser simuliert
	 * den Controller des Sim.
	 * 
	 * Wenn USE_WORKER gesetzt ist, wartet die 'ServerCom' (s.o.)
	 * jeden Zyklus auf den Worker (genauso sync., wie der Ctrl. mit den
	 * Bots im Sim).
	 * 
	 * Ueber WORKER_WAIT kann eingestellt werden, ob
	 * der Worker arbeit (des Ctrl.) simulieren soll (er schlaeft einfach die
	 * angegebene Zeit, bevor die 'ServerCom' wieder freigegeben wird und weiter
	 * machen darf).
	 * 
	 * Ueber TICK_RATE wird eine mindest (Bot-)Laufzeit eingestellt.
	 * Der Worker schlaeft diese, nachdem er die 'ServerCom' (entspr. einem Bot)
	 * wieder freigegeben hat. Ein neuer Zyklus (in diesem Fall Sende-Vorgang)
	 * kann also nicht vor Ablauf dieser Zeit _wieder_ beginnen (aber der
	 * Bot kann waehrend dieser Zeit rechnen; in diesem Fall die ServerCom
	 * kommunizieren)...
	 * 
	 * 
	 */
	class Worker implements Runnable {
		
		private Thread thrd;
		
		private CountDownLatch start = new CountDownLatch(1);
		private CountDownLatch done  = new CountDownLatch(1);
		
		public void start() {
			
			this.thrd = new Thread(this);
			
			this.thrd.start();
		}
		
		public void stop() {
			
			Thread dummy = this.thrd;
			
			this.thrd = null;
			
			dummy.interrupt();
		}
		
		public void run() {
			
			Thread thisThrd = Thread.currentThread();
			
			System.out.println("Worker wurde gestartet...");
			
			while(this.thrd == thisThrd) {
				
				try {
					this.done.await();
					
					Thread.sleep(TestServer.WORKER_WAIT);
					
					this.reinit();
					
					Thread.sleep(TestServer.TICK_RATE);
				
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// Initialisiert Latches fuer den "naechsten" Zyklus
		public synchronized void reinit() {
			
			CountDownLatch start = this.start;
			
			this.start = new CountDownLatch(1);
			this.done  = new CountDownLatch(1);
			
			start.countDown();
		}
		
		// Hier warten die Bots, bzw. die ServerCom auf den
		// Ctrl., bzw. den Worker
		public void waitOnWorker()
				throws InterruptedException {
			
			CountDownLatch start = this.start;
			// ueberfluessig ?
			CountDownLatch done  = this.done;
			
			done.countDown();
			
			start.await();
		}
	}
	
	private Thread serverThrd;
	
	private Socket       clientSocket;
	private ServerSocket serverSocket;
	
	/**
	 * Der TestServer lauscht auf Connections auf dem
	 * global angegebenen Port.
	 * 
	 * Bei einer ankommenden Connection wird diese an eine
	 * 'ServerCom' uebergeben und weiter gelauscht...
	 * 
	 */
	TestServer(int port) {
		
		try {
			this.serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	public void start() {
		
		this.serverThrd = new Thread(this);
		
		this.serverThrd.start();
	}
	
	public void stop() {
		
		Thread dummy = this.serverThrd;
		
		this.serverThrd = null;
		
		dummy.interrupt();
	}

	public void run() {
		
		Thread thisThrd = Thread.currentThread();
		
		System.out.println("Server ist hoch und rennen...");
		
		while(this.serverThrd == thisThrd) {
			
			try {
				
				this.clientSocket = this.serverSocket.accept();
				
				System.out.println("Connection wird hergestellt...");
				ServerCom com = new ServerCom(this.clientSocket);
				com.start();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) {
		
		TestServer server = new TestServer(TestServer.PORT);
		
		server.start();
		
		if(TestServer.JAVA_CLIENT) {
			
			TestClient client = new TestClient(TestServer.HOST, TestServer.PORT);
			
			client.start();
		}
	}
}

/** 
 * Der TestClient schickt einfach ein ankommendes Datum zurueck
 * an den Server, der dann die Zeit stoppt und ausgibt (SERVER_TIME).
 * 
 * Oder schickt ein Datum an den Server und wartet selbst bis
 * dieses zurueckkommt (SERVER_TIME == FALSE).
 * 
 */
class TestClient implements Runnable {

	private Socket socket;
	private Thread clientThrd;

	PrintWriter out;

	BufferedReader in;

	TestClient(String host, int port) {

		try {
			this.socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void start() {
		
		this.clientThrd = new Thread(this);
		
		this.clientThrd.start();
	}
	
	public void stop() {
		
		Thread dummy = this.clientThrd;
		
		this.clientThrd = null;
		
		dummy.interrupt();
	}

	public void run() {
		
		Thread thisThrd = Thread.currentThread();
		
		System.out.println("Client ist hoch und rennen...");
		
		System.out.println("Warten und Senden willkommen...");
		int cmd = 0;
		while(cmd != 60) {
			try {
				cmd = this.in.read();
				System.out.println(cmd);
				this.out.write(cmd);
				this.out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		int c = 1;
		while(this.clientThrd == thisThrd) {
			
			try {
//				if(TestServer.SERVER_TIME) {
//					this.out.println(this.in.readLine());
//					
//				} else {
//					long time = System.nanoTime();
//					
//					this.out.println("Test");
//					String str = this.in.readLine();
//					
//					System.out.println(String.format("CLIENT: Antwort nach " +
//													//"%9d ns", (System.nanoTime()-time)));
//													"%4.3f ms", ((float) (System.nanoTime()-time)) / 1000000.));
//				}
				
				int str = this.in.read();
				//if(c==1) {
				System.out.println(c+".: "+str);
				this.out.write(str);
				//}
				if(TestServer.FLUSH_ALL || str == 60) {
					this.out.flush();
					c++;
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		this.out.close();
		try {
			this.in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}