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

import javax.media.j3d.BranchGroup;
import javax.media.j3d.TransformGroup;

import ctSim.ErrorHandler;
import ctSim.TcpConnection;
import ctSim.Model.SceneLight;
import ctSim.Model.SceneUpdate;

/**
 * Dies ist die Verwaltungsstation fuer Remote-Views. 
 * Es geht also hier um den Server und noicht den Anzeigeteil
 * @author bbe (bbe@heise.de)
 *
 */
public class RemoteView {
	/** Die Verbindung an der das View haengt*/
	private TcpConnection connection; 
	
	/** Datenstrom */
	private ObjectOutputStream oos = null;
	
	/**
	 * @param connection
	 */
	public RemoteView(TcpConnection connection) throws IOException{
		super();
		this.connection = connection;
		oos = new ObjectOutputStream(connection.getSocket().getOutputStream());
	}

	/**
	 * Uebertraegt alle aenderungen in sceneLight an das entfernte View
	 * @param sceneLight
	 */
	public void update(SceneUpdate sceneUpdate) throws IOException{
		oos.writeObject(sceneUpdate);
//		oos.writeChars("Update");
//		ErrorHandler.error("RemoteView.update() not implemented yet");
		// TODO implement RemoteView.update();
	}
	
	/**
	 * Uebertraegt den initialen Scenegraphen an die Remote View
	 * @param sceneLight
	 */
	public void init(SceneLight sceneLight) throws IOException{
		oos.writeObject(sceneLight);
		
//		ErrorHandler.error("RemoteView.init() not implemented yet");
		// TODO implement RemoteView.init();
	}
	
	/**
	 * Fuegt einen neuen Bot in eine Remote View ein
	 * @param id
	 * @param tg
	 * @param rg
	 * @param bg
	 */
	public void addBotToView(String id, TransformGroup tg, TransformGroup rg, BranchGroup bg){
		ErrorHandler.error("RemoteView.addBotToView() not implemented yet");
		// TODO implement RemoteView.addBotToView();
	}

	/**
	 * Entfernt einen Bot aus der View
	 * @param id
	 */
	public void removeBot(String id){
		ErrorHandler.error("RemoteView.removeBot() not implemented yet");
		// TODO implement RemoteView.removeBot();
	}
	
	/**
	 * Beendet eine Remote View und raeumt auf
	 */
	public void die(){
		try{
			connection.disconnect();
		} catch (Exception ex){}
	}
}
