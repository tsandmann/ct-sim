package ctSim;

import java.io.IOException;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;
import jd2xx.*;

public class JD2xxConnection extends Connection {

	private JD2XX jd = new JD2XX();

	public void connect() throws IOException {
		try {
			jd.open(0);
			
			jd.setBaudRate(9500);
			jd.setDataCharacteristics(  8, JD2XX.STOP_BITS_1, JD2XX.PARITY_NONE	);
			jd.setFlowControl( JD2XX.FLOW_NONE, 0, 0);
			jd.setTimeouts(1000, 1000);
		
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
	
	public JD2xxConnection() {
		super();
		// TODO Auto-generated constructor stub
	}

}
