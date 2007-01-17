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

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.AliveObstacle;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotPosition;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.components.BotComponent.ConnectionFlags;
import ctSim.view.gui.BotBuisitor;

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
 * Der Haupt-Thread k&uuml;mmert sich um die eigentliche Simulation und die
 * Koordination mit dem Zeittakt der Welt. Die Kommunikation z.B. &uuml;ber eine
 * TCP/IP-Verbindung muss von den abgeleiteten Klassen selbst behandelt werden.
 * </p>
 *
 * @author Benjamin Benz (bbe@heise.de)
 * @author Peter K&ouml;nig (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */
public abstract class Bot extends AliveObstacle {
	//$$ doc
	public static class BulkList<T> extends ArrayList<T> {
		private static final long serialVersionUID = - 8179783452023605404L;

		public void add(T... elements) {
            for (T e : elements)
                add(e);
        }
	}

	/**
	 * <p>
	 * Liste von BotComponents; wie ArrayList, aber kann zus&auml;tzlich 1.
	 * Component-Flag-Tabellen (siehe
	 * {@link #applyFlagTable(ctSim.model.bots.Bot.CompntWithFlag[]) applyFlagTable()})
	 * und 2. Massen-Hinzuf&uuml;gen:
	 *
	 * <pre>
	 * componentList.add(
	 *     new BotComponent&lt;...&gt;(...),
	 *     new BotComponent&lt;...&gt;(...),
	 *     new BotComponent&lt;...&gt;(...),
	 *     ...
	 * );</pre>
	 *
	 * </p>
	 *
	 * @author Hendrik Krau&szlig; &lt;<a
	 * href="mailto:hkr@heise.de">hkr@heise.de</a>>
	 */
	public static class BotComponentList extends BulkList<BotComponent<?>> {
        private static final long serialVersionUID = - 1331425647710880289L;

        /**
		 * <p>
		 * Eine Component-Flag-Tabelle gibt an, welche {@link BotComponent}s
		 * vom TCP (oder USB) lesen sollen, welche schreiben. Funktioniert so:
		 *
		 * <pre>
         * import static ctSim.model.bots.components.BotComponent.ConnectionFlags.READS;
		 * import static ctSim.model.bots.components.BotComponent.ConnectionFlags.WRITES;
		 * ...
		 *
         * public class C3PO extends Bot {
         *     private BotComponentList components = ...;
         *
         *     public C3PO() {
         *         // Alle BotComponents erzeugen
         *         components.add(
         *             new Plappermaul(...),
         *             new Goldbein("links"),
         *             new Goldbein("rechts"), // Beide Instanzen werden betroffen
         *             new Nervensaegmodul(...),
         *         );
         *
         *         // Hier die Component-Flag-Tabelle
         *         // Setzen, welche BotComponents lesen/schreiben
         *         components.applyFlagTable(
         *             _(Plappermaul.class, WRITES),   // schreibt ins TCP
         *             _(Nervensaegmodul.class, READS, WRITES), // liest + schreibt
         *             _(Goldbein.class)   // weder noch
         *         );
         *     }
         * }
         * </pre>
		 *
		 * Component-Flag-Tabellen sind also eine Verkn&uuml;pfung dieser
		 * Methode, einer Hilfsmethode mit Namen Unterstrich (_) und einer
		 * kleinen Klasse (CompntWithFlag). Vorteil: eine Superklasse, z.B.
		 * CtBot, kann die Komponenten instanziieren. Subklassen, z.B.
		 * SimulierterCtBot und UeberTcpVerbundenerRealerCtBot, haben ja alle
		 * dieselben Komponenten, aber betreiben sie in verschiedenen Modi (z.B.
		 * realer Bot: (fast) alle nur lesen). Die Superklasse macht also
		 * {@code components.add(...)}, die Subklassen k&ouml;nnen dann den
		 * {@code applyFlagsTable(...)}-Aufruf machen.
		 * </p>
		 * <p>
		 * Hat ein Bot mehrere Komponenten gleicher Klasse, werden die Flags von
		 * ihnen allen betroffen.
		 * </p>
		 */
        public void applyFlagTable(CompntWithFlag... compntFlagTable) {
        	for (BotComponent<?> compnt : this) {
        		for (CompntWithFlag cwf : compntFlagTable) {
        			if (cwf.compntClass.isAssignableFrom(compnt.getClass()))
        				compnt.setFlags(cwf.flags);
    			}
    		}
    	}
    }

	/**
	 * <p>
	 * Ordnet einer BotComponent {@link ConnectionFlags} zu.
	 * Component-Flag-Tabellen sind Arrays von diesem Typ. Siehe
	 * {@link BotComponentList#applyFlagTable(ctSim.model.bots.Bot.CompntWithFlag[]) BotComponentList.applyFlagTable()}.
	 * </p>
	 */
	protected static class CompntWithFlag {
		final Class<? extends BotComponent<?>> compntClass;
		final ConnectionFlags[] flags;

		CompntWithFlag(Class<? extends BotComponent<?>> compntClass,
		ConnectionFlags[] flags) {
			this.compntClass = compntClass;
			this.flags = flags;
		}
	}

	/**
	 * <p>
	 * Hilfsmethode, mit der man Component-Flag-Tabellen leicht schreiben kann
	 * &ndash; siehe
	 * {@link BotComponentList#applyFlagTable(ctSim.model.bots.Bot.CompntWithFlag[]) BotComponentList.applyFlagTable()}.
	 * </p>
	 */
	protected static CompntWithFlag _(
	Class<? extends BotComponent<?>> compntClass, ConnectionFlags... flags) {
		return new CompntWithFlag(compntClass, flags);
	}

	protected final BotComponentList components = new BotComponentList();

	private BotPosition posHead;

    private final List<Sensor> sens = new ArrayList<Sensor>();

	/** Liste mit den verschiedenen Aussehen eines Bots */
//	private HashMap appearances;

	/** Konstanten fuer die ViewingPlatform
	 * @param position Position
	 * @param heading Blickrichtung
	 */
	public Bot(String name, Point3d position, Vector3d heading) {
		super(name, position, heading);

	    //$$ ViewPlatforms: Toter Code
	 	// Zu diesem Zeitpunkt ist die ganze 3D-Repraesentation bereits aufgebaut
//		createViewingPlatform();

		posHead = new BotPosition(getName(), getPositionInWorldCoord(),
			getHeadingInWorldCoord()) {

			@Override
			public String getName() {
				return Bot.this.getName();
			}

			@Override
			public Point3d getRelPosition() {
				return getPositionInWorldCoord();
			}

			@Override
			public Vector3d getRelHeading() {
				// TODO Auto-generated method stub
				return getHeadingInWorldCoord();
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
    }

	/** @return Der Positionsanzeiger */
	public final BotPosition getBotPosition() { return this.posHead; }

	//$$$
	protected final void addSensor(Sensor sen) { sens.add(sen); }

	@Override
	public void updateSimulation(long simulTime) {
		super.updateSimulation(simulTime);
		for(Sensor s : sens)
			s.update();
	}

	public void accept(BotBuisitor buisitor) {
		for (BotComponent<?> c : components)
			buisitor.visit(c, this);
		for (Sensor<?> s : sens)
			buisitor.visit(s, this);
		buisitor.visit(posHead, this);
	}

    //$$ ViewPlatforms: Toter Code
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
