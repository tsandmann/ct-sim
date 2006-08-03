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

package ctSim.model.bots;

import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Bounds;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.AliveObstacle;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.BotPosition;
import ctSim.model.bots.components.Sensor;
import ctSim.view.Debug;

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
/* TODO:
 * Alles was mit der Darstellung des Bots zu tun hat wird ausgelagert...
 * - bei allgeimeinen Sachen in die Super-Klasse (bzw. abstract Methoden der (Super-)Klasse)
 * - bei speziellen Sachen in die jew. Unterklasse
 */
public abstract class Bot extends AliveObstacle{
	
	private BotPosition posHead;
	
//	private List<Position> pos;
	private List<Actuator> acts;
	private List<Sensor> sens;
	
	/** Steuerpanel des Bots */
//	private ControlPanel panel;

	/**
	 * Blickvektor
	 */
//	private Vector3f heading = new Vector3f(1f, 0f, 0f);

	/** Liste mit den verschiedenen Aussehen eines Bots */
//	private HashMap appearances;
	
	/** Konstanten fuer die Noderefernces */
//	public static final String BOTBODY = "botBody";
	
//	public static final String VP = "VP";
	
//	/**
//	 * Initialisierung des Bots
//	 * @param controller Verweis auf den zugehoerigen Controller
//	 */
//	public Bot(Controller controller) {
//		super(controller);
//	 	
//	 	// Zu diesem Zeitpunkt ist die ganze 3D-Repraesentation bereits aufgebaut
//		createViewingPlatform();
//		
//		this.acts = new ArrayList<Actuator>();
//		this.sens = new ArrayList<Sensor>();
//	}

	/** Konstanten fuer die ViewingPlatform 
	 * @param position Position  
	 * @param heading Blickrichtung
	 */
	public Bot(String name, Point3d position, Vector3d heading) {
	 	
		super(name, position, heading);
		
	 	// Zu diesem Zeitpunkt ist die ganze 3D-Repraesentation bereits aufgebaut
//		createViewingPlatform();
		
		this.posHead = new BotPosition(this.getName(), this.getPosition(), this.getHeading()) {

			@Override
			public String getName() {
				return Bot.this.getName();
			}
			
			@Override
			public Point3d getRelPosition() {
				
				return getPosition();
			}

			@Override
			public Vector3d getRelHeading() {
				// TODO Auto-generated method stub
				return getHeading();
			}

			@Override
			public Point3d getAbsPosition() {
				// TODO Auto-generated method stub
				return this.getRelPosition();
			}

			@Override
			public Vector3d getAbsHeading() {
				// TODO Auto-generated method stub
				return this.getRelHeading();
			}

			@Override
			public void setPos(Point3d pos) {
				
				setPosition(pos);
			}

			@Override
			public void setHead(Vector3d head) {
				
				setHeading(head);
			}
		};
		
		this.acts = new ArrayList<Actuator>();
		this.sens = new ArrayList<Sensor>();
//		this.pos = new ArrayList<Position>();
//		initPosition();
	}
	
//	private void initPosition(){
//		// X-Position
//		this.addPosition(new SimplePosition<Float>("X-Position", "", 0.0) {
//
//			@Override
//			public Float getValue() {
//				return new Float(getPos().x);
//			}
//
//			@Override
//			public void setValue(Float value) {
//				setPos (new Vector3f(value.floatValue(), getPos().y, getGroundClearance() + getHeight()/2));
//				// TODO: stimmt z-Wert ?
//			}
//
//			@Override
//			public String getType() {
//				
//				return "Metrisch";
//			}
//
//			@Override
//			public String getDescription() {
//				
//				return "Position im Parcours";
//			}
//		});
//
//		// TODO: Positionen und Winkel stimmen nicht mehr und lassen sich 
//		// noch nicht setzen
//		
//		// Y-Position
//		this.addPosition(new SimplePosition<Float>("Y-Position", "", 0.0) {
//
//			@Override
//			public Float getValue() {
//				return new Float(getPos().y);
//			}
//
//			@Override
//			public void setValue(Float value) {
//				setPos (new Vector3f(getPos().x, value.floatValue(), getGroundClearance() + getHeight()/2));
//				// TODO: korrekter z-Wert fehlt noch!!!
//			}
//
//			@Override
//			public String getType() {
//				
//				return "Metrisch";
//			}
//
//			@Override
//			public String getDescription() {
//				
//				return "Position im Parcours";
//			}
//		});
//
//
//		// Blickrichtung
//		this.addPosition(new SimplePosition<Double>("Blickrichtung", "", 0.0) {
//
//			@Override
//			public Double getValue() {
//				return new Double(SimUtils.vec3fToDouble(getPos()));
//			}
//
//			@Override
//			public void setValue(Double value) {
//
//				// TODO: echten Wert rausgeben
//
//			}
//
//			@Override
//			public String getType() {
//				
//				return "Winkel in Grad";
//			}
//
//			@Override
//			public String getDescription() {
//				
//				return "Blickrichtung des Bot";
//			}
//		});
//
//	}
	
	/**
	 * @return Die Liste der Aktuatoren
	 */
	public final List<Actuator> getActuators() {
		
		return this.acts;
	}
	
	/**
	 * @return Die Liste der Sensoren
	 */
	public final List<Sensor> getSensors() {
		
		return this.sens;
	}
	
	/**
	 * @return Der Positionsanzeiger
	 */
	public final BotPosition getBotPosition() {
		
		return this.posHead;
	}
	
//	public final List<BotComponent> getComponents() {
//		
//		List<BotComponent> comps = new ArrayList<BotComponent>(this.sens.size()+this.acts.size()+1);
//		
//		comps.add(this.posHead);
//		comps.addAll(this.sens);
//		comps.addAll(this.acts);
//		
//		return comps;
//	}
	
//	public final List<Position> getPositions() {
//		
//		return this.pos;
//	}
	
	protected final void addActuator(Actuator act) {
		
		this.acts.add(act);
	}
	
	protected final void addSensor(Sensor sen) {
		
		// TODO: rueber in Sensor selbst?
		this.sens.add(sen);
		
		this.addBranchComponent(sen.getRelTransform(), sen.getShape());
	}

	/* (non-Javadoc)
	 * @see ctSim.model.AliveObstacle#updateSimulation(long)
	 */
	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime);
		
//		Debug.out.println("  +-+  Bot '"+this.getName()+"':\n"
//			 + "             | +- Sensor-Werte werden geupdatet...");
//		long time = System.nanoTime();
//		
//		for(Sensor s : this.getSensors()) {
//			s.update();
//		}
//		
//		Debug.out.println("  +-+  Bot '"+this.getName()+"':\n"
//		     + "             | +- Sensor-Werte wurden geupdatet:              "+String.format("%2.9f",(float)(System.nanoTime()-time)/1000000000.));
		
		
		long time = System.nanoTime();
		
		for(Sensor s : this.getSensors()) {
			s.update();
		}
		
		Debug.out.println(String.format("%2.9f",(float)(System.nanoTime()-time)/1000000000.));
		
		
		
		// TODO:
//		for(Actuator a : this.getActuators()) {
//			
//			a.update();
//		}

	}

	/* (non-Javadoc)
	 * @see ctSim.model.MovableObstacle#getBounds()
	 */
	public Bounds getBounds() {
		// TODO Auto-generated method stub
		return null;
	}

//	protected final void addPosition(Position posi) {
//		
//		this.pos.add(posi);
//	}

	
	/**
	 * Der Aufruf dieser Methode direkt nach dem Erzeugen sorgt dafuer, dass der
	 * Bot ueber ein passendes ControlPanel verfuegt
	 */
//	abstract public void providePanel();

	/**
	 * Hier wird aufgeraeumt, wenn die Lebenszeit des Bots zuende ist:
	 * Verbindungen zur Welt und zum ControlPanel werden aufgeloest, das Panel
	 * wird aus dem ControlFrame entfernt
	 * 
	 * @see Bot#work()
	 */
//	protected void cleanup() {
//		super.cleanup();
//		
//		getController().removeFromView(getName());
//		panel.remove();
//		panel = null;
//	}


	/**
	 * @return Die Hoehe der Grundplatte des Bot ueber dem Boden in Metern
	 */
//	abstract public float getGroundClearance();
	
	/**
	 * @param heading
	 *            Die Blickrichtung des Bot, die gesetzt werden soll
	 */
//	public void setHeading(Vector3d heading) {
//		
//		synchronized (heading) {
//			this.heading = heading;
//			double angle = heading.angle(new Vector3d(1d, 0d, 0d));
//			if (heading.y < 0)
//				angle = -angle;
//			Transform3D transform = new Transform3D();
//
//			((TransformGroup)getNodeReference(TG)).getTransform(transform);
//			
//			transform.setRotation(new AxisAngle4f(0f, 0f, 1f, angle));
//			
//			((TransformGroup)getNodeReference(TG)).setTransform(transform);
//			
//			
//		}

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
//	}
	
//	/** Fuegt eine Kameraplatform fuer den Bot ein */
//	private void createViewingPlatform(){
//		TransformGroup tg = new TransformGroup();
//		Transform3D translate = new Transform3D();
////		translate.setTranslation(new Vector3f(0f,0f,1f));
//		tg.setTransform(translate);
//
//		TransformGroup rg = new TransformGroup();
//		Transform3D rotate = new Transform3D();
//		Transform3D tmp= new Transform3D();
//		
//		rotate.setRotation(new AxisAngle4d(0d, 0d, 1d, -Math.PI/2));
//		tmp.rotX(Math.PI/2);
//		rotate.mul(tmp);
//		
//		rg.setTransform(rotate);
//		tg.addChild(rg);
//		
//	 	ViewPlatform  viewPlatform = new ViewPlatform();	// Erzeuge eine neue Platform
//	 	rg.addChild(viewPlatform);
//	 	
//	 	((TransformGroup)getNodeReference(TG)).addChild(tg); // Fuege sie ein
//	 	addNodeReference(VP,viewPlatform);	// Trage sie in die Map ein
//	 	
//	}
	

	/**
	 * @return Gibt das ControlPanel des Bot zurueck
	 */
//	public ControlPanel getPanel() {
//		return panel;
//	}

	/**
	 * @param panel
	 *            Referenz auf das ControlPanel, die gesetzt werden soll
	 */
//	public void setPanel(ControlPanel panel) {
//		this.panel = panel;
//	}



	/**
	 * @return Gibt die Blickrichtung zurueck
	 */
//	public Vector3d getHeading() {
//		return this.heading;
//	}

	/**
	 * Sucht ein Erscheinungsbild des Bots aus der Liste heraus
	 * @param key
	 * @return
	 */
//	private Appearance getAppearance(String key) {
//		Appearance app = null;
//		if (appearances != null)
//			app =(Appearance) appearances.get(key);
//		
//		return app;
//	}

	/** Lagert die Erscheinungsbilder des Bots ein
	 * 
	 * @param appearances
	 */
//	public void setAppearances(HashMap appearances) {
//		this.appearances = appearances;
//		setAppearance("normal");
//	}

	/**
	 * Setzt das erscheinungsbild des Bots auf einenWert, zu dem bereits eine Appearance ind er Liste vorliegt
	 * @param key
	 */
//	public void setAppearance(String key) {
//		Shape3D botBody = (Shape3D) getNodeReference(BOTBODY);
//		if (botBody != null)
//			botBody.setAppearance(getAppearance(key));
//		else
//			ErrorHandler.error("Fehler beim Versuch eine Appearance zu setzen: botBody == null");
//	}
}
