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
		Object[] devs= this.jd.listDevicesByDescription();
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
				ErrorHandler.error("No FT232 found - deinstalling Virtual comport might help"); //$NON-NLS-1$
				throw new IOException("No FT232 found - deinstalling Virtual comport might help"); //$NON-NLS-1$
			}
			
			this.jd.open(0);
			this.jd.setBaudRate(9600);
			this.jd.setDataCharacteristics(  8, JD2XX.STOP_BITS_1, JD2XX.PARITY_NONE	);
			this.jd.setFlowControl( JD2XX.FLOW_NONE, 0, 0);
			this.jd.setTimeouts(60000, 60000);
		
			setDis(new LEDataInputStream(new JD2XXInputStream(this.jd)));
			setDos(new LEDataOutputStream( new JD2XXOutputStream(this.jd)));
		} catch (IOException ex) {
			this.jd.close();
			ErrorHandler.error("Error while creating Streams "+ex); //$NON-NLS-1$
			throw ex;			
		}
		
	}	

	/**
	 * Beendet die laufende Verbindung
	 * 
	 * @Override CtSim.connection.disconnect 
	 * @throws IOException
	 * @throws Exception
	 */
	@Override 
	public synchronized void disconnect() throws IOException, Exception {
		super.disconnect();
		try {
			this.jd.close(); // as well as the socket
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
