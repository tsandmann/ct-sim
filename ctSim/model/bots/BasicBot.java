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

import java.lang.reflect.InvocationTargetException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.SwingUtilities;

import ctSim.model.Command;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.ConnectionFlags;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

//$$ doc
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
public abstract class BasicBot implements Bot {
	//$$ nach util?
	public static class BulkList<T> extends ArrayList<T> {
		private static final long serialVersionUID = - 8179783452023605404L;

		public void add(T... elements) {
            for (T e : elements)
                add(e);
        }
	}

	public static class CountingMap
	extends HashMap<Class<? extends BasicBot>, Integer> {
		private static final long serialVersionUID = 6419402218947363629L;

		public synchronized void increase(Class<? extends BasicBot> c) {
			if (containsKey(c))
				put(c, get(c) + 1);
			else
				put(c, 0);
		}

		public synchronized void decrease(Class<? extends BasicBot> c) {
			if (containsKey(c))
				put(c, get(c) - 1);
			else
				throw new IllegalStateException();
		}
	}

	/**
	 * <p>
	 * Liste von BotComponents; wie ArrayList, aber kann zus&auml;tzlich 1.
	 * Component-Flag-Tabellen (siehe
	 * {@link #applyFlagTable(ctSim.model.bots.BasicBot.CompntWithFlag[]) applyFlagTable()})
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

    	/**
		 * <p>
		 * Gibt ein empfangenes Kommando an alle Botkomponenten (= Sensoren und
		 * Aktuatoren). Die Komponente(n), die sich zust&auml;ndig f&uuml;hlt
		 * (f&uuml;hlen), k&ouml;nnen etwas damit tun (typischerweise ihren
		 * eigenen Wert setzen auf den im Kommando gespeicherten).
		 * </p>
		 * <p>
		 * Implementiert das <a
		 * href="http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern">Chain-of-Responsibility-Pattern</a>.
		 * </p>
		 */
    	public void processCommand(Command command) throws ProtocolException {
    		if (command.getDirection() != Command.DIR_REQUEST) {
    			throw new ProtocolException("Kommando ist Unfug: Hat als " +
    					"Richtung nicht 'Anfrage'; ignoriere");
    		}

    		for (BotComponent<?> c : this)
    			c.offerRead(command);
    		if (! command.hasBeenProcessed())
    			throw new ProtocolException("Unbekanntes Kommando: "+command);
    	}

    	public void updateView() throws InterruptedException {
    		try {
    			SwingUtilities.invokeAndWait(new Runnable() {
    				@SuppressWarnings("synthetic-access")
    				public void run() {
    					for (BotComponent<?> c : BotComponentList.this)
    						c.updateExternalModel();
    				}
    			});
    		} catch (InvocationTargetException e) {
    			throw new RuntimeException(e);
    		}
        }
    }

	/**
	 * <p>
	 * Ordnet einer BotComponent {@link ConnectionFlags} zu.
	 * Component-Flag-Tabellen sind Arrays von diesem Typ. Siehe
	 * {@link BotComponentList#applyFlagTable(ctSim.model.bots.BasicBot.CompntWithFlag[]) BotComponentList.applyFlagTable()}.
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
	 * {@link BotComponentList#applyFlagTable(ctSim.model.bots.BasicBot.CompntWithFlag[]) BotComponentList.applyFlagTable()}.
	 * </p>
	 */
	protected static CompntWithFlag _(
	Class<? extends BotComponent<?>> compntClass, ConnectionFlags... flags) {
		return new CompntWithFlag(compntClass, flags);
	}

	private static final CountingMap numInstances = new CountingMap();

	//$$ Schoener waere: fuer alle lg-Ausgaben ein Praefix getName()
	protected final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots");

	protected final BotComponentList components = new BotComponentList();

	private final String name;

	private final List<Runnable> disposeListeners = Misc.newList();

	public BasicBot(String name) {
		this.name = name;

		// Instanz-Zahl erhoehen
		numInstances.increase(getClass());
		// Wenn wir sterben, Instanz-Zahl reduzieren
		addDisposeListener(new Runnable() {
			@SuppressWarnings("synthetic-access")
			public void run() {
				numInstances.decrease(BasicBot.this.getClass());
			}
		});
    }

	public void addDisposeListener(Runnable runsWhenAObstDisposes) {
		if (runsWhenAObstDisposes == null)
			throw new NullPointerException();
		disposeListeners.add(runsWhenAObstDisposes);
	}

	public void dispose() {
		lg.info("Bot %s verkr\u00FCmelt sich", toString());
		for (Runnable r : disposeListeners)
			r.run();
	}

	/**
	 * <p>
	 * Laufende Nummer des AliveObstacles, und zwar abh&auml;ngig von der
	 * Subklasse: Laufen z.B. 2 GurkenBots und 3 TomatenBots (alle von
	 * AliveObstacle abgeleitet), dann sind die Instance-Numbers:
	 * <ul>
	 * <li>GurkenBot 0</li>
	 * <li>GurkenBot 1</li>
	 * <li>TomatenBot 0</li>
	 * <li>TomatenBot 1</li>
	 * <li>TomatenBot 2</li>
	 * </ul>
	 * Instance-Numbers fangen immer bei 0 an.
	 * </p>
	 *
	 * @see #toString()
	 */
	public int getInstanceNumber() {
		/*
		 * Drandenken: Wenn einer ne Subklasse instanziiert, die von
		 * AliveObstacle abgeleitet ist, wird eine AliveObstacle-Instanz
		 * automatisch miterzeugt -- Wenn wir hier getClass() aufrufen, liefert
		 * das aber die exakte Klasse (also in unserm Fall niemals
		 * AliveObstacle, sondern z.B. BestimmterDingsBot)
		 */
		return numInstances.get(getClass());
	}

	/**
	 * <p>
	 * Benutzerfreundlicher Name des Bots (wie dem Konstruktor &uuml;bergeben),
	 * an die falls erforderlich eine laufende Nummer angeh&auml;ngt ist. Laufen
	 * z.B. 2 AliveObstacle-Instanzen mit Namen &quot;Gurken-Bot&quot; und 3 mit
	 * &quot;Tomaten-Bot&quot;, sind die R&uuml;ckgabewerte dieser Methode:
	 * <ul>
	 * <li>Gurken-Bot</li>
	 * <li>Gurken-Bot (2)</li>
	 * <li>Tomaten-Bot</li>
	 * <li>Tomaten-Bot (2)</li>
	 * <li>Tomaten-Bot (3)</li>
	 * </ul>
	 * </p>
	 */
	@Override
	public String toString() {
		int n = getInstanceNumber() + 1; // 1-based ist benutzerfreundlicher
		return name + ((n < 2) ? "" : " (" + n + ")");
	}

	public String getDescription() {
		return "Unbekannter Bottyp";
	}

	public void accept(BotBuisitor buisitor) {
		for (BotComponent<?> c : components)
			buisitor.visit(c, this);
	}

	public void updateView() throws InterruptedException {
		components.updateView();
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
