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
import java.io.ObjectOutputStream;
import java.util.HashMap;

import javax.media.j3d.BranchGroup;

import com.sun.j3d.utils.scenegraph.io.SceneGraphStreamWriter;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.Model.AliveObstacle;
import ctSim.Model.Scene.SceneGraphStreamWriterFixed;
import ctSim.Model.Scene.SceneLight;
import ctSim.Model.Scene.SceneUpdate;

/**
 * Verwaltungsstation fuer Remote-Views auf Server-Seite. 
 * @author bbe (bbe@heise.de)
 *
 */
public class RemoteView {
	/** Die Verbindung, an der die View haengt*/
	private TcpConnection connection; 
		
	/**
	 * Der Konstruktor
	 * @param connection Die Verbindung
	 */
	public RemoteView(TcpConnection connection) {
		super();
		this.connection = connection;
		System.out.println("Neue ClientView hat sich angemeldet von: "+connection.getSocket().getInetAddress().getHostName()+" auf Port "+connection.getSocket().getLocalPort());
	}

	/**
	 * Uebertraegt alle Aenderungen in SceneLight an die entfernte View
	 * @param sceneUpdate Die Aenderungen
	 * @throws IOException
	 */
	public void update(SceneUpdate sceneUpdate) throws IOException{
		try{
			ObjectOutputStream oos = new ObjectOutputStream(connection.getSocket().getOutputStream());
			oos.writeObject(sceneUpdate);
		} catch (IOException ex){
			ErrorHandler.error("Probleme beim Uebertragen des SceneUpdates "+ex);
			throw ex;
		}
	}
	
	/**
	 * Uebertraegt den initialen SceneGraph an die Remote View
	 * @param sceneLight
	 * @throws IOException
	 */
	public void init(SceneLight sceneLight) throws IOException{
		sceneLight.writeStream(connection.getSocket().getOutputStream());
//		sceneLight.writeFile();
	}
	
	/**
	 * Fuegt einen neuen Bot in eine Remote View ein
	 * @param name Name des Bots
	 * @param map Die Hashmap mit Referenzen
	 */
	public void addBotToView(String name, HashMap map){
		try {
			// Zuerst den neune Bot ankuendigen
			SceneUpdate sU = new SceneUpdate();
			sU.setType(SceneUpdate.TYPE_ADDBOT);
			update(sU);
			
			// Dann uebertragen
			SceneGraphStreamWriter writer = new SceneGraphStreamWriterFixed(connection.getSocket().getOutputStream());
			BranchGroup bg = (BranchGroup)map.get(name+"_"+AliveObstacle.BG);
			writer.writeBranchGraph(bg,map);
			writer.close();
		} catch (Exception ex){
			ErrorHandler.error("Fehler beim Uebertragen eines neuen Bots: "+ex);
		}
	}

	/**
	 * Entfernt einen Bot aus der View
	 * @param id
	 */
	public void removeBot(String id){
		SceneUpdate sU = new SceneUpdate();
		sU.setType(SceneUpdate.TYPE_REMOVEBOT);
		sU.setBotToKill(id);
		
		try {
			update(sU);
		} catch (Exception ex){
			ErrorHandler.error("Fehler beim Loeschen eines Bots: "+ex);
		}
	}
	
	/**
	 * Beendet eine Remote View und raeumt auf
	 */
	public void die(){
		try{
			connection.disconnect();
		} catch (Exception ex){
			// Wenn noch was schiefgeht, isses auch egal
		}
	}
}
