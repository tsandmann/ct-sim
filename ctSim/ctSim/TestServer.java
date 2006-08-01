package ctSim;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

public class TestServer implements Runnable {
	
	public static final String  HOST        = "localhost";
	public static final int     PORT        = 36721;
	
	/* 
	 * Content, der hin und her geschickt wird...
	 * 
	 */
	public static final String  CONTENT     = "Test";
	
	/* 
	 * Will man einen Java-Client?
	 * 
	 * Ansonsten C-Client "von Hand" starten...
	 * 
	 */
	public static final boolean JAVA_CLIENT = true;
	
	/* 
	 * TRUE: Server -> Client -> Server
	 * FALSE: Client -> Server -> Client
	 * 
	 */
	public static final boolean SERVER_TIME = true;
	
	/* 
	 * Soll der Server einen Worker-Thread
	 * benutzen, der den Ctrl. im Sim simuliert?
	 * 
	 * (synchronisiert wie im Sim der Ctrl.)
	 * 
	 */
	public static final boolean USE_WORKER  = true;
	
	/* 
	 * Wie lange der Worker pro Zyklus warten soll,
	 * bevor er den Com-Thread wieder laufen lässt
	 * (simuliert Arbeit des Ctrl.)
	 * 
	 */
	public static final long    WORKER_WAIT = 100;
	
	/* 
	 * Tick-Rate im Simulator: Jede Runde wird
	 * mindestens diese Zeit gewartet
	 * (+ simulierte Arbeit -- s.o.)
	 * 
	 */
	public static final long    TICK_RATE   = 10;
	
	/* 
	 * Server-Connection mit _einem_ Client. Diese entspricht einem
	 * Java-Bot im Simulator.
	 * 
	 * Bekommt vom Server den Client_Port übergeben
	 * und kommuniziert dann mit diesem.
	 * 
	 * Je nach Einstellung von SERVER_TIME schickt er...
	 * - wenn TRUE: ein Datum an den Client und wartet auf die Antwort
	 * - wenn FALSE: das ankommende Datum einfach an den Client zurück
	 * 
	 * Nach einem Empfangs-/Sende-Vorgang wird auf den Worker
	 * (wenn eingestellt über USE_WORKER) gewartet...
	 * 
	 */
	class ServerCom implements Runnable {
		
		private Socket socket;
		
		private Thread thrd;
		
		private Worker worker;
		
		private PrintWriter out;
		private BufferedReader in;
		
		ServerCom(Socket socket) {
			
			if(TestServer.USE_WORKER) {
				this.worker = new Worker();
				this.worker.start();
			}
			
			try {
				this.socket = socket;
				this.out = new PrintWriter(socket.getOutputStream(), true);
				this.in = new BufferedReader(
						new InputStreamReader(
						socket.getInputStream()));
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
			
			while(this.thrd == thisThrd) {
				
				try {
					if(TestServer.SERVER_TIME) {
						
						long time = System.nanoTime();
						
						this.out.println("Test");
						@SuppressWarnings("unused")
						String str = this.in.readLine();
						
						System.out.println(String.format("SERVER: Antwort nach " +
														//"%9d ns", (System.nanoTime()-time)));
														"%4.3f ms", ((float) (System.nanoTime()-time)) / 1000000.));
						
					} else {
						this.out.println(this.in.readLine());
					}
					
					if(this.worker != null)
						this.worker.waitOnWorker();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	
	/* 
	 * Optional einzustellender Worker. Dieser simuliert
	 * den Controller des Sim.
	 * 
	 * Wenn USE_WORKER gesetzt ist, wartet die 'ServerCom' (s.o.)
	 * jeden Zyklus auf den Worker (genauso sync., wie der Ctrl. mit den
	 * Bots im Sim).
	 * 
	 * Über WORKER_WAIT kann eingestellt werden, ob
	 * der Worker arbeit (des Ctrl.) simulieren soll (er schläft einfach die
	 * angegebene Zeit, bevor die 'ServerCom' wieder freigegeben wird und weiter
	 * machen darf).
	 * 
	 * Über TICK_RATE wird eine mindest (Bot-)Laufzeit eingestellt.
	 * Der Worker schläft diese, nachdem er die 'ServerCom' (entspr. einem Bot)
	 * wieder freigegeben hat. Ein neuer Zyklus (in diesem Fall Sende-Vorgang)
	 * kann also nicht vor Ablauf dieser Zeit _wieder_ beginnen (aber der
	 * Bot kann während dieser Zeit rechnen; in diesem Fall die ServerCom
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
		
		// Initialisiert Latches für den "nächsten" Zyklus
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
			// Überflüssig ?
			CountDownLatch done  = this.done;
			
			done.countDown();
			
			start.await();
		}
	}
	
	private Thread serverThrd;
	
	private Socket       clientSocket;
	private ServerSocket serverSocket;
	
	/* 
	 * Der TestServer lauscht auf Connections auf dem
	 * global angegebenen Port.
	 * 
	 * Bei einer ankommenden Connection wird diese an eine
	 * 'ServerCom' übergeben und weiter gelauscht...
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

/* 
 * Der TestClient schickt einfach ein ankommendes Datum zurück
 * an den Server, der dann die Zeit stoppt und ausgibt (SERVER_TIME).
 * 
 * Oder schickt ein Datum an den Server und wartet selbst bis
 * dieses zurückkommt (SERVER_TIME == FALSE).
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
		
		while(this.clientThrd == thisThrd) {
			
			try {
				if(TestServer.SERVER_TIME) {
					this.out.println(this.in.readLine());
					
				} else {
					long time = System.nanoTime();
					
					this.out.println("Test");
					@SuppressWarnings("unused")
					String str = this.in.readLine();
					
					System.out.println(String.format("CLIENT: Antwort nach " +
													//"%9d ns", (System.nanoTime()-time)));
													"%4.3f ms", ((float) (System.nanoTime()-time)) / 1000000.));
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