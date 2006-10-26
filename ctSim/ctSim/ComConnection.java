package ctSim;


import java.io.IOException;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;

/**
 * Abgeleitete Klasse ComConnection, serielle Verbindung zu realen Bots
 * @author Maximilian Odendahl (maximilian.odendahl@rwth-aachen.de)
 */
public class ComConnection extends Connection {
	SerialPort  port;

	/**
	 * Baut eine Verbindung auf
	 * @param portName Name des Ports
	 * @param baudrate Baudrate
	 * @throws IOException
	 * @throws NoSuchPortException
	 * @throws UnsupportedCommOperationException
	 * @throws PortInUseException
	 */
	public void connect(String portName, int baudrate) throws IOException,NoSuchPortException,UnsupportedCommOperationException,PortInUseException{
		CommPortIdentifier portId;

		try {
			portId = CommPortIdentifier.getPortIdentifier(portName);
			port = (SerialPort)portId.open("CtSim",2000);
		 
			//TODO: Parameter configurierbar machen
			port.setSerialPortParams(baudrate,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);	
			port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			port.enableReceiveTimeout(60000);
		
			setInputStream(port.getInputStream());
			setOutputStream( port.getOutputStream());
		
			
		} catch (IOException ex) {
			ErrorHandler.error("Fehler beim Erzeigen der Streams "+ex);
			throw ex;			
		}
	}


	/**
	 * Beendet die laufende Verbindung
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	@Override
	public synchronized void disconnect() throws IOException, Exception {
		super.disconnect();
		try {
			port.close();
		} catch (Exception Ex) {
			throw Ex;
		}
	}	
	
	/**
	 * Konstruktor
	 *
	 */
	public ComConnection() {
		super();
		// TODO Auto-generated constructor stub
	}


}
