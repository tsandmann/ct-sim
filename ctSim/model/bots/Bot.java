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

import javax.media.j3d.Shape3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.AliveObstacle;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.BotPosition;
import ctSim.model.bots.components.Sensor;

/**
 * <p>
 * Superklasse f&uuml;r alle Bots, unabh&auml;ngig davon, ob sie &ndash;
 * <ul>
 * <li><strong>real</strong> sind, d.h. ein Bot aus Hardware wurde an den Sim
 * angeschlossen und der Sim spielt daher haupts&auml;chlich die Rolle eines
 * erweiterten Displays f&uuml;r Sensorwerte, die von echten Sensoren stammen
 * (mit anderen Worten, der Sim l&auml;uft im Slave-Modus)</li>
 * <li><strong>simuliert</strong> sind, d.h. es gibt keinen Bot aus Hardware,
 * es l&auml;uft nur der Steuercode auf einem PC. Sensordaten kommen in diesem
 * Fall nicht von echter Hardware, sondern &uuml;ber TCP vom Sim, der sie
 * ausgerechnet hat (Sim im Master-Modus)</li>
 * <li><strong>c't-Bots</strong> sind oder nicht &ndash; theoretisch
 * k&ouml;nnte jemand ja mal den Simulator um selbstgestrickte Bots erweitern,
 * die keine c't-Bot sind.</li>
 * </ul>
 * </p>
 * <p>
 * Die Klasse ist abstrakt und muss daher erst abgeleitet werden, um
 * instanziiert werden zu koennen.
 * </p>
 * <p>
 * Der Haupt-Thread kuemmert sich um die eigentliche Simulation und die
 * Koordination mit dem Zeittakt der Welt. Die Kommunikation z.B. über eine
 * TCP/IP-Verbindung muss von den abgeleiteten Klassen selbst behandelt werden.
 * </p>
 *
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter Koenig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */
public abstract class Bot extends AliveObstacle {
	private BotPosition posHead;

	private List<Actuator> acts = new ArrayList<Actuator>();
	private List<Sensor> sens = new ArrayList<Sensor>();

	/** Konstanten fuer die ViewingPlatform
	 * @param position Position
	 * @param heading Blickrichtung
	 */
	public Bot(Shape3D shape, String name, Point3d position,
		double headingInDeg) {
		super(shape, name, position, headingInDeg);

	 	// Zu diesem Zeitpunkt ist die ganze 3D-Repraesentation bereits aufgebaut
//		createViewingPlatform();

		posHead = new BotPosition(getName(), getPosition(), getHeading()) {
			@Override
			public Point3d getRelPosition() {
				return getPosition();
			}

			@Override
			public Vector3d getRelHeading() {
				return getHeading();
			}

			@Override
			public void setPos(Point3d pos) {
				setPosition(pos);
			}

			@Override
			public void setHead(double headingInDeg) {
				setHeadingInDeg(headingInDeg);
			}
		};
	}

	/**
	 * @return Die Liste der Aktuatoren
	 */
	public final List<Actuator> getActuators() { return acts; }

	/**
	 * @return Die Liste der Sensoren
	 */
	public final List<Sensor> getSensors() { return sens; }

	/**
	 * @return Der Positionsanzeiger
	 */
	public final BotPosition getBotPosition() { return posHead; }

	protected final void addActuator(Actuator act) { acts.add(act); }

	protected final void addSensor(Sensor sen) { sens.add(sen); }

	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime);

		for(Sensor s : this.getSensors())
			s.update();
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
}
