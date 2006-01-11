package ctSim;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;

/**
 * Repräsentiert eine TCP-Verbindung 
 */
public class TcpConnection{
	/** Der Socket */
	private Socket socket = null;
	
	/** Ein Input-Stream */
	private LEDataInputStream dis =null;
	/** Ein Ausgabe Stream */
	private LEDataOutputStream dos=null;
//	private BufferedReader br = null;
    
	/**
	 * liest ein Byte
	 * @return ein Byte
	 */
	public int readUnsignedByte() throws IOException{
		int data;
		synchronized (dis) {
			data=dis.readUnsignedByte();
		}
		return data;
	}
	
	/**
	 * Liest einen int16
	 * @return das Datum
	 */
	public short readShort()  throws IOException{
		short data=0;
		synchronized (dis) {
				data=dis.readShort();
		}
		
		return data;
	}
	
	/*
	public synchronized int[] readByte(int length) throws IOException {
        
       	synchronized (dis) {
       		byte data[] = new 
       	}

		
	}
	*/
	
   	/*	 if true Thread does not read */
//   	private boolean pause = false;	
    	
	/*
	 * Schaltet den ReadThread solange ab, bis ein continue kommt
	 */
/*	public void pause_on(){
		pause=true;
	}
*/	
	/*
	 * Schaltet den ReadThread wieder an
	 */
/*	synchronized public void pause_off(){
		pause=false;
	}    	
*/	/*
    public void run() {
    	int length=512;
    	int[] gelesen= new int[length];
    	
		try { // since we need to handle some possible exceptions,

        	while (!isInterrupted()) { // run while needed

        		if (!pause){
        			// check if data is available
                	                    
                    if (! DataReady()) { // wait while no data
                    	sleep(10);
                    } else { 
                    	// receive data, 
                    	length= readBuffer(gelesen,length);
                    	dataReceived(gelesen,length);
                    }
            	}
        	}
    	
		} catch (InterruptedException iEx){
    			ErrorHandler.error("Read-Loop, Interrupted:  " + iEx);
		} catch (Exception ex) {
			ErrorHandler.error("Error in Read-Loop, Dying:  " + ex);
        }
    }
*/
    /* 
     * Dioese Funktion soll sich um die Verarbeitung der Empfangenen Daten kümmern.
     * Bitte überschreiben!
     */
/*    abstract protected void dataReceived(int[] data, int length);
    {
    	System.out.print("Please overwrite me, because I'm doing nothing at all");
    }
   */
	
	/**
	 * Neue Connection
	 */
	public TcpConnection() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Lauscht auf einem Port,
	 * Initialisiert die Connection und exit
	 * @param port Der Port zum lausen 
	 * @return 0 wenn alles ok, sonst -1
	 */
	public int listen(int port){
		try {
			ServerSocket server = new ServerSocket(port);
			socket= server.accept();
			
			connect();
			
		} catch (IOException iOEx){
			ErrorHandler.error("Error listening on port: "+port+" "+iOEx.getMessage());
			return -1;
		}
		return 0;
	}
	
	private void connect() throws IOException{
        // set up a buffered stream to read from XPort and write to PC
//        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        dis= new LEDataInputStream(socket.getInputStream());

        // set up a buffered stream to read from PC and write to XPort
        dos= new LEDataOutputStream(socket.getOutputStream());
        
        //this.start();
	}
	
    /**
     * this class does all the actual TCP/IP connection setup work
     * 
     * @param ipa
     * @param port
     * @throws IOException
     * @throws Exception
     */
    public void connect(InetAddress ipa, int port) throws IOException, Exception {
        try {
            //  really establish a TCP connection (at last...)
            socket = new Socket(ipa.getHostAddress(), port);
            connect();		// Init rest of stuff
            
        } catch (IOException IOEx) {
            throw IOEx;
        } catch (Exception Ex) {
            throw Ex;
        }
        	
    }	
    
    /**
     * This method converts the given String ipa_str and the int port into a
     * useable TCP/IP address and then opens the connection
     * 
     * @param ipa_str
     * @param port
     * @throws IOException
     * @throws Exception
     */
    public void connect(String ipa_str, int port) throws IOException, Exception {
        InetAddress ipa = null;
        try {
            ipa = InetAddress.getByName(ipa_str);
            
            connect(ipa, port);
        
        } catch (IOException IOEx) {
            throw IOEx;
        } catch (Exception Ex) {
            throw Ex;
        }
    }

    
	
    /**
     * this method will terminate the current connection
     * 
     * @throws IOException
     * @throws Exception
     */
    public synchronized void disconnect() throws IOException, Exception {
         try {
            dis.close(); 		// close input and output stream
//            br.close();
            dos.close(); 
            socket.close();		// as well as the socket
        } catch (IOException IOEx) {
            throw IOEx;
        } catch (Exception Ex) {
            throw Ex;
        }
    }
	
    /**
     * send a String 
     * @param sendString Der String
     * @throws IOException Irgendetwas mit DataOutputStream.writeBytes oder flush ist schief gegangen 
     */
    public void send(String sendString) throws IOException {
        
        try {
           	synchronized (dos) {
	           dos.writeBytes(sendString);
	           dos.flush(); // write dos
    		}

        } catch (IOException iOEx) {
            throw iOEx;
        }
    }

    /**
     * Übertrage Daten 
     * 
     * @param sendByte Nutzdaten (einzelne Bytes in einem int[]
     * @throws IOException
     */
    public void send(byte sendByte[]) throws IOException{
        try {
           	synchronized (dos) {
	        	dos.write(sendByte);
	        	dos.flush(); // write dos
           	}
        } catch (IOException iOEx) {
            throw iOEx;
        }
    }

    /**
     * Is data ready to receive
     * 
     * @return
     * @throws Exception
     */
 /*   private boolean DataReady(){
        try {
            return br.ready();
        } catch (Exception Ex) {
            return false;
        }
    }
*/   
    /**
     * this method will read from TCP/IP  

     * @return Number of bytes read
     * @throws IOException
     * @throws Exception
     */
 /*   public synchronized int readBuffer(int[] gelesen,int length) throws IOException{
        int i=0; // init helper variables
        
        while ((br.ready())& (i<length)){  // loop while data available
        	gelesen[i++]=dis.readUnsignedByte();// read from dis
        }

        return i;
    }
*/   
}
