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
	public RemoteView(TcpConnection connection) throws IOException{
		super();
		this.connection = connection;
	}

	/**
	 * Uebertraegt alle Aenderungen in SceneLight an die entfernte View
	 * @param sceneUpdate Die Aenderungen
	 */
	public void update(SceneUpdate sceneUpdate) throws IOException{
		try{
			ObjectOutputStream oos = new ObjectOutputStream(connection.getSocket().getOutputStream());
			oos.writeObject(sceneUpdate);
		} catch (IOException ex){
			ErrorHandler.error("Probleme beim Uebertragen des SceneUpdates "+ex);
		}
//		write(ConvertData.objectToBytes(sceneUpdate));
	}
	
	/**
	 * Uebertraegt den initialen SceneGraph an die Remote View
	 * @param sceneLight
	 */
	public void init(SceneLight sceneLight) throws IOException{
		sceneLight.writeStream(connection.getSocket().getOutputStream());
	}
	
	/**
	 * Fuegt einen neuen Bot in eine Remote View ein
	 * @param id
	 * @param tg
	 * @param rg
	 * @param bg
	 */
	public void addBotToView(String id, TransformGroup tg, BranchGroup bg){
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
