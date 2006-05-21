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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.HashMap;

import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.SceneGraphObject;

import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamReader;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.Model.Scene.SceneGraphStreamReaderFixed;
import ctSim.Model.Scene.SceneLight;
import ctSim.Model.Scene.SceneUpdate;

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
	 * @param ip IP-Adresse oder Hostname
	 * @param port TCP-Port
	 * @throws Exception
	 */
	public ClientView(String ip, int port) throws Exception{
		super();

		connection.connect(ip,port);
		
		System.out.println("ClientView connected");
		sc=new SceneLight();
		sc.readStream(connection.getSocket().getInputStream());
	
		cleanupSceneGraph(sc.getScene());
		
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

	/**
	 * Reiche das Update weiter an das Scenelight-Objekt
	 * @param sceneUpdate
	 */
	public void update(SceneUpdate sceneUpdate){
		sc.update(sceneUpdate);
	}

	/**
	 * Entfernt einen Bot
	 * @param sceneUpdate
	 */
	public void removeBot(SceneUpdate sceneUpdate){
		sc.removeBot(sceneUpdate.getBotToKill());
	}
	
	/**
	 * Liest eine SceneUpdate-Nachricht ein
	 * @return Das Update
	 * @throws IOException
	 */
	public SceneUpdate getUpdate() throws IOException{
		SceneUpdate sU = null;
		
		ObjectInputStream ois = new ObjectInputStream(connection.getSocket().getInputStream());
		
		try {
			sU= (SceneUpdate) ois.readObject();
		} catch (ClassNotFoundException ex){
			ErrorHandler.error("Klasse konnte nicht gefunden werden "+ex);
		}
		
		return sU;
	}

	
	/**
	 * Entfernt alles aus dem Scenegraphen, was fuer ClientViews nicht noetig ist
	 * Unter anderem Pickable-Flags
	 * @param group
	 */
	public void cleanupSceneGraph(Group group){	
		Enumeration en = group.getAllChildren();
		while (en.hasMoreElements()){
			SceneGraphObject so = (SceneGraphObject) en.nextElement();
			// Ist das eine Gruppe? Wenn ja, durchsuchen
			if (so instanceof Node)
				((Node)so).setPickable(false);
			
			if (so instanceof Group){
				// rekursion
				cleanupSceneGraph((Group)so);
			}
		}
		return;
	}
	
	/**
	 * Fuegt einen Bot ein
	 */
	public void addBot(){
		HashMap<String,SceneGraphObject> newMap = new HashMap<String,SceneGraphObject>();
		try {
			InputStream is = connection.getSocket().getInputStream();
			SceneGraphStreamReader reader = new SceneGraphStreamReaderFixed(is);
			reader.readBranchGraph(newMap);
			
			sc.addBot(null,newMap);
		} catch (IOException e) {
			ErrorHandler.error("Problemem beim lesen empfangen eines neuen Bots "+e);
		}

		
	
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
				SceneUpdate sU = clientView.getUpdate();
				switch (sU.getType()) {
					case SceneUpdate.TYPE_UPDATE:
						clientView.update(sU);
						break;
					case SceneUpdate.TYPE_ADDBOT:
						clientView.addBot();
						break;
	
					case SceneUpdate.TYPE_REMOVEBOT:
						clientView.removeBot(sU);

					default:
						break;
				}
			}

		
		} catch (Exception ex){
			ErrorHandler.error("Fehler in der ClientView "+ex);
			ex.printStackTrace();
			
		}
		System.exit(0);
	}
	
	

}
