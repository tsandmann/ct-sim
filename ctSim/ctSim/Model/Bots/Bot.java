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

package ctSim.Model.Bots;

import java.util.HashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.ViewPlatform;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Vector3f;

import ctSim.ErrorHandler;
import ctSim.Controller.Controller;
import ctSim.Model.AliveObstacle;
import ctSim.View.ControlPanel;

/**
 * Superklasse fuer alle Bots, unabhaengig davon, ob sie real oder simuliert
 * sind.</br> Die Klasse ist abstrakt und muss daher erst abgeleitet werden, um
 * instanziiert werden zu koennen.</br> Der Haupt-Thread kuemmert sich um die
 * eigentliche Simulation und die Koordination mit dem Zeittakt der Welt. Die
 * Kommunikation z.B. ueber eine TCP/IP-Verbindung muss von den abgeleiteten
 * Klassen selbst behandelt werden.
 * 
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */

abstract public class Bot extends AliveObstacle{

	/** Steuerpanel des Bots */
	private ControlPanel panel;

	/**
	 * Blickvektor
	 */
	private Vector3f heading = new Vector3f(1f, 0f, 0f);

	/** Liste mit den verschiedenen Aussehen eines Bots */
	private HashMap appearances;
	
	/** Konstanten fuer die Noderefernces */
	public static final String BOTBODY = "botBody";
	
	/** Konstanten fuer die ViewingPlatform */
	public static final String VP = "VP";
	
	/**
	 * Initialisierung des Bots
	 * @param controller Verweis auf den zugehoerigen Controller
	 */
	public Bot(Controller controller) {
		super(controller);
	 	
	 	// Zu diesem Zeitpunkt ist die ganze 3D-Repraesentation bereits aufgebaut
		createViewingPlatform();
	}

	/**
	 * Der Aufruf dieser Methode direkt nach dem Erzeugen sorgt dafuer, dass der
	 * Bot ueber ein passendes ControlPanel verfuegt
	 */
	abstract public void providePanel();

	/**
	 * Hier wird aufgeraeumt, wenn die Lebenszeit des Bots zuende ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest, das Panel
	 * wird aus dem ControlFrame entfernt
	 * 
	 * @see Bot#work()
	 */
	@Override
	protected void cleanup() {
		super.cleanup();
		
		getController().removeFromView(getName());
		panel.remove();
		panel = null;
	}


	/**
	 * @return Die Hoehe der Grundplatte des Bot ueber dem Boden in Metern
	 */
	abstract public float getGroundClearance();
	
	/**
	 * @param heading
	 *            Die Blickrichtung des Bot, die gesetzt werden soll
	 */
	public void setHeading(Vector3f heading) {
		float angle;
		synchronized (heading) {
			this.heading = heading;
			angle = heading.angle(new Vector3f(1f, 0f, 0f));
			if (heading.y < 0)
				angle = -angle;
			Transform3D transform = new Transform3D();

			((TransformGroup)getNodeReference(TG)).getTransform(transform);
			
			transform.setRotation(new AxisAngle4f(0f, 0f, 1f, angle));
			
			((TransformGroup)getNodeReference(TG)).setTransform(transform);
			
			
		}

/*//		Vector3f p= new Vector3f(getPos());
		Vector3f p= new Vector3f(0f,0f,0f);
//		p.sub(new Vector3f(0f,0.3f,1.8f)); 
		
		Transform3D transform = new Transform3D();
		//controller.getWorldView().getUniverse().getViewingPlatform().getViewPlatformTransform().getTransform(transform);
		
		transform.setTranslation(p);
		
		transform.rotX(Math.PI/2);
		transform.rotY(Math.PI/2);
		transform.rotZ(0);
		getController().getWorldView().getUniverse().getViewingPlatform().getViewPlatformTransform().setTransform(transform);
*/
	}
	
	/** Fuegt eine Kameraplatform fuer den Bot ein */
	private void createViewingPlatform(){
		TransformGroup tg = new TransformGroup();
		Transform3D translate = new Transform3D();
//		translate.setTranslation(new Vector3f(0f,0f,1f));
		tg.setTransform(translate);

		TransformGroup rg = new TransformGroup();
		Transform3D rotate = new Transform3D();
		Transform3D tmp= new Transform3D();
		
		rotate.setRotation(new AxisAngle4d(0d, 0d, 1d, -Math.PI/2));
		tmp.rotX(Math.PI/2);
		rotate.mul(tmp);
		
		rg.setTransform(rotate);
		tg.addChild(rg);
		
	 	ViewPlatform  viewPlatform = new ViewPlatform();	// Erzeuge eine neue Platform
	 	rg.addChild(viewPlatform);
	 	
	 	((TransformGroup)getNodeReference(TG)).addChild(tg); // Fuege sie ein
	 	addNodeReference(VP,viewPlatform);	// Trage sie in die Map ein
	 	
	}
	

	/**
	 * @return Gibt das ControlPanel des Bot zurueck
	 */
	public ControlPanel getPanel() {
		return panel;
	}

	/**
	 * @param panel
	 *            Referenz auf das ControlPanel, die gesetzt werden soll
	 */
	public void setPanel(ControlPanel panel) {
		this.panel = panel;
	}



	/**
	 * @return Gibt die Blickrichtung zurueck
	 */
	public Vector3f getHeading() {
		return heading;
	}

	/**
	 * Sucht ein Erscheinungsbild des Bots aus der Liste heraus
	 * @param key
	 * @return Die Appearance
	 */
	private Appearance getAppearance(String key) {
		Appearance app = null;
		if (appearances != null)
			app =(Appearance) appearances.get(key);
		
		return app;
	}

	/** Lagert die Erscheinungsbilder des Bots ein
	 * 
	 * @param appearances
	 */
	public void setAppearances(HashMap appearances) {
		this.appearances = appearances;
		setAppearance("normal");
	}

	/**
	 * Setzt das erscheinungsbild des Bots auf einenWert, zu dem bereits eine Appearance ind er Liste vorliegt
	 * @param key
	 */
	public void setAppearance(String key) {
		Shape3D botBody = (Shape3D) getNodeReference(BOTBODY);
		if (botBody != null)
			botBody.setAppearance(getAppearance(key));
		else
			ErrorHandler.error("Fehler beim Versuch eine Appearance zu setzen: botBody == null");
	}
}
