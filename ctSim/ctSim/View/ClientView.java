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

import java.io.IOException;
import java.io.ObjectInputStream;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.Model.SceneLight;
import ctSim.Model.SceneUpdate;

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
		// TODO echte Abmessungen der Welt ermitteln 
		worldView.setScene(sc.getScene(),5.0f,5.0f);
		worldView.initGUI();
		
		new ClientFrame(worldView);
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
	
	public void update(SceneUpdate sceneUpdate){
		sc.update(sceneUpdate);
	}
	
	/**
	 * Liest eine SceneUpdate-Nachricht ein
	 * @return
	 * @throws IOException
	 */
	public SceneUpdate getUpdate() throws IOException{
		SceneUpdate sU = null;
//		byte [] data = new byte[10000];
		
		ObjectInputStream ois = new ObjectInputStream(connection.getSocket().getInputStream());
		
		try {
			sU= (SceneUpdate) ois.readObject();
//			sU = (SceneUpdate)ConvertData.bytesToObject(data);
		} catch (ClassNotFoundException ex){
			ErrorHandler.error("Klasse konnte nicht gefunden werden "+ex);
		}
		ois.close();
		
		return sU;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ClientView clientView = null;
		try {
			clientView = new ClientView("localhost",10002);

			while (true){
				clientView.update(clientView.getUpdate());
			}

		
		} catch (Exception ex){
			ErrorHandler.error("Fehler beim oeffnen der ClientView "+ex);
		}
		
	}
	
	

}
