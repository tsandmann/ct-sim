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

import java.io.IOException;
import javax.comm.*;

import mindprod.ledatastream.LEDataInputStream;
import mindprod.ledatastream.LEDataOutputStream;

/**
 * Repraesentiert eine Serielle Verbindung
 * @author bbe (bbe@heise.de)
 *
 */
public class ComConnection extends Connection {

	private CommPortIdentifier portID; 
	private SerialPort serialPort;
	/**
	 * Stellt eine Verbindung zum angegebenen COM-Port her
	 * 
	 * @param port COM-Port
	 * @throws Exception
	 */
	public void connect(String port) throws Exception {
		try{
			portID = CommPortIdentifier.getPortIdentifier(port);
		} catch (NoSuchPortException ex) {
			ErrorHandler.error("Port: "+port+" does not exist: "+ex);
			throw ex;
		}
		
		try {
	        serialPort = (SerialPort) portID.open("c't-Sim",2000);
	    } catch (PortInUseException ex) {
			ErrorHandler.error("Not able to open Com-Port: "+port+" "+ex);
			throw ex;
		}

        try {
            serialPort. setSerialPortParams(9600, SerialPort.DATABITS_8,
                                           SerialPort.STOPBITS_1,
                                           SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException ex) {
        	serialPort.close();
			ErrorHandler.error("Not able to set Parameters "+ex);
			throw ex;
        }

	    
		try {
			setDis(new LEDataInputStream(serialPort.getInputStream()));
			setDos(new LEDataOutputStream(serialPort.getOutputStream()));
		} catch (IOException ex) {
			serialPort.close();
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
			serialPort.close(); // as well as the socket
		} catch (Exception Ex) {
			throw Ex;
		}
	}	
	
	/**
	 * 
	 */
	public ComConnection() {
		super();
		// TODO Auto-generated constructor stub
	}

}
