package ctSim;

import java.io.IOException;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;
import jd2xx.*;

/**
 * Verbindet einen Bot ueber JD2XX-Verbindung mit dem Simulator
 */
public class JD2xxConnection extends Connection {

	private JD2XX jd = new JD2XX();

	private int list() throws IOException{
		int count=0;
		Object[] devs= jd.listDevicesByDescription();
		for (int i=0; i<devs.length; i++){
			System.out.println(devs[i]);
			count++;
		}
		return count;
	}
	
	/**
	 * Stellt die Verbindung her
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		try {
			if (list() ==0){
				ErrorHandler.error("No FT232 found - deinstalling Virtual comport might help");
				throw new IOException("No FT232 found - deinstalling Virtual comport might help");
			}
			
			jd.open(0);
			jd.setBaudRate(9600);
			jd.setDataCharacteristics(  8, JD2XX.STOP_BITS_1, JD2XX.PARITY_NONE	);
			jd.setFlowControl( JD2XX.FLOW_NONE, 0, 0);
			jd.setTimeouts(60000, 60000);
		
			setDis(new LEDataInputStream(new JD2XXInputStream(jd)));
			setDos(new LEDataOutputStream( new JD2XXOutputStream(jd)));
		} catch (IOException ex) {
			jd.close();
			ErrorHandler.error("Error while creating Streams "+ex);
			throw ex;			
		}
		
	}	

	/**
	 * Beendet die laufende Verbindung
	 * 
	 * @override CtSim.connection.disconnect
	 * @throws IOException
	 * @throws Exception
	 */
	public synchronized void disconnect() throws IOException, Exception {
		super.disconnect();
		try {
			jd.close(); // as well as the socket
		} catch (Exception Ex) {
			throw Ex;
		}
	}	
	
	/**
	 * Der Konstruktor
	 */
	public JD2xxConnection() {
		super();
	}

}
