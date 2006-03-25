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
package ctSim.View;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.Model.SceneLight;

/**
 * Verwaltungsstation fuer Remote-Views auf Server-Seite. 
 * @author bbe (bbe@heise.de)
 */
public class ClientView {
	/** Die Verbindung, an der die View haengt*/
	private TcpConnection connection = new TcpConnection(); 

	private SceneLight sc;
	
	private WorldView worldView;
	
	/**
	 * Oeffnet eine ClientView zu einem gegebenn Rechner
	 * @param connection
	 */
	public ClientView(String ip, int port) throws Exception{
		super();

		connection.connect(ip,port);
		
		System.out.println("ClientView connected");
		sc=new SceneLight();
		sc.readStream(connection.getSocket().getInputStream());
		
		worldView = new WorldView();
		worldView.setScene(sc.getScene());
		worldView.initGUI();
		
		clientFrame clientFrame=new clientFrame(worldView);
	}
	
	
	/**
	 * Oeffnet eine ClientView, wenn die Verbindung bereits besteht
	 * @param connection
	 */
	public ClientView(TcpConnection connection) {
		super();
		// TODO Auto-generated constructor stub
		this.connection = connection;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			ClientView clientView = new ClientView("localhost",10002);
		} catch (Exception ex){
			ErrorHandler.error("Fehler beim oeffnen der ClientView "+ex);
		}
	}
	
	

}
