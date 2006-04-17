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
package ctSim.Model.Scene;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.media.j3d.Node;
import javax.media.j3d.TransformGroup;

import ctSim.Model.Bots.Bot;
import ctSim.View.ViewBotUpdate;

/**
 * Die ist ein Container-Objekt fuer alle Aenderungen an einem Szenegraphen  
 * @author bbe (bbe@heise.de) 
 */
public class SceneUpdate implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Es ist ein normales Update mit neune TG-Werten usw. */
	public static final int TYPE_UPDATE = 0;
	/** Es ist ein Bot dazu gekommen */
	public static final int TYPE_ADDBOT = 1;
	/** Es muss ein Bot sterben */
	public static final int TYPE_REMOVEBOT = 2;	
	/**
	 * Speichert, was das für ein Update-Packet ist. siehe TYPE_-Konstanten
	 */
	private int type = TYPE_UPDATE;
	
	/** Name des Bots, der zu loeschen ist */
	private String botToKill = null;
	
	/** Liste mit allen Bots, die in dieser Welt leben */
	private List<ViewBotUpdate> bots = null;

	/**
	 * @return Gibt eine Referenz auf bots zurueck
	 */
	public List<ViewBotUpdate> getBots() {
		return bots;
	}

	/**
	 * Erzeugt ein leeres Update  
	 */
	public SceneUpdate(){
		super();
	}
	
	/**
	 * Erzeugt ein Update aus einer Scene 
	 * @param sceneLight
	 */
	public SceneUpdate(SceneLight sceneLight) {
		super();
		
		bots= new LinkedList<ViewBotUpdate>();
		
		// Einmal alle Eintraege in der Map durchgehen
		Iterator it = sceneLight.getMap().entrySet().iterator(); 
		while (it.hasNext()) {
			Map.Entry entry =(Map.Entry) it.next();
			
			String key = (String) entry.getKey();
			Node value = (Node) entry.getValue();
			
			if (key.contains(Bot.TG)){
				bots.add(new ViewBotUpdate(key,(TransformGroup)value));				
			}
		}
	}

	/**
	 * @return Gibt eine Referenz auf type zurueck
	 * @return Gibt den Wert von type zurueck
	 */
	public int getType() {
		return type;
	}

	/**
	 * @param type Referenz auf type, die gesetzt werden soll
	 * @param type Der Wert von type, der gesetzt werden soll
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * @return Gibt eine Referenz auf botToKill zurueck
	 * @return Gibt den Wert von botToKill zurueck
	 */
	public String getBotToKill() {
		return botToKill;
	}

	/**
	 * @param botToKill Referenz auf botToKill, die gesetzt werden soll
	 * @param botToKill Der Wert von botToKill, der gesetzt werden soll
	 */
	public void setBotToKill(String botToKill) {
		this.botToKill = botToKill;
	}
}
