/*
 * c't-Sim - Robotersimulator für den c't-Bot
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

import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ctSim.Connection;
import ctSim.controller.Controller;
import ctSim.model.Command;
import ctSim.model.ThreeDBot;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.ConnectionFlags;
import ctSim.model.bots.ctbot.CtBotSimTest;
import ctSim.util.BotID;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;

/**
 * <p>
 * Superklasse für alle Bots, unabhängig davon, ob sie:
 * <ul>
 * <li><strong>real</strong> sind, d.h. ein Bot aus Hardware wurde an den Sim angeschlossen und der Sim
 * spielt daher hauptsächlich die Rolle eines erweiterten Displays für Sensorwerte, die von echten
 * Sensoren stammen (mit anderen Worten, der Sim läuft im Slave-Modus).</li>
 * <li><strong>simuliert</strong> sind, d.h. es gibt keinen Bot aus Hardware, es läuft nur der Steuercode
 * auf einem PC. Sensordaten kommen in diesem Fall nicht von echter Hardware, sondern über TCP vom Sim,
 * der sie ausgerechnet hat (Sim im Master-Modus).</li>
 * <li><strong>c't-Bots</strong> sind oder nicht -- theoretisch könnte jemand ja mal den Simulator um
 * selbstgestrickte Bots erweitern, die keine c't-Bot sind.</li>
 * </ul>
 * </p>
 * <p>
 * Die Klasse ist abstrakt und muss daher erst abgeleitet werden, um instanziiert werden zu können.
 * </p>
 * <p>
 * Der Haupt-Thread kümmert sich um die eigentliche Simulation und die Koordination mit dem Zeittakt
 * der Welt. Die Kommunikation z.B. über eine TCP/IP-Verbindung muss von den abgeleiteten Klassen selbst
 * behandelt werden.
 * </p>
 *
 * @author Benjamin Benz
 * @author Peter König (pek@heise.de)
 * @author Lasse Schwarten (lasse@schwarten.org)
 */
public abstract class BasicBot implements Bot {
	/** Die Connection an der der Bot hängt */
	private Connection connection;
	
	/** Hier ist der Controller gespeichert, der den Bot verwaltet */
	private Controller controller;

	/**
	 * Liefert die Id eines Bots für die Adressierung der Commands zurück
	 * 
	 * @return Id des Bots
	 */
	public BotID getId() {
		if (getConnection() == null) {
			/* bei Testbots gibt's keine Connection / ID */
			return new BotID(Command.getBroadcastId());
		}
		return getConnection().getCmdOutStream().getTo();
	}

	/**
	 * Setzt die Id des Bots für die Adressierung der Commands
	 * 
	 * @param newId	ID des Bots
	 * @throws ProtocolException	wenn die Id bereits vergeben ist
	 */
	public void setId(BotID newId) throws ProtocolException {
		if (newId.equals(this.getId()))
			return;	// ID ist schon gesetzt
		if (controller != null) {
			if (!controller.isIdFree(newId)) {
				lg.warn("Die neue Id dieses Bots (" + newId + ") existiert schon im Controller!");
				throw new ProtocolException("Die neue Id dieses Bots (" + newId + ") existiert schon im Controller!");
			}
		}
		lg.info("Setze die Id von Bot " + toString() + " auf " + newId);
		getConnection().getCmdOutStream().setTo(newId);
	}
	
	/**
	 * Liste
	 * 
	 * @param <T>	Typ
	 */
	public static class BulkList<T> extends ArrayList<T> {
		/** UID */
		private static final long serialVersionUID = - 8179783452023605404L;
		
		/**
		 * Fügt Elemente hinzu
		 * 
		 * @param elements	die Elemente
		 */
		public void add(T... elements) {
            for (T e : elements)
                add(e);
        }
	}
	
	/** Zählklasse */
	public static class CountingMap
	extends HashMap<Class<? extends BasicBot>, Integer> {
		/** UID */
		private static final long serialVersionUID = 6419402218947363629L;

		/**
		 * @param c	Bot
		 */
		public synchronized void increase(Class<? extends BasicBot> c) {
			if (containsKey(c))
				put(c, get(c) + 1);
			else
				put(c, 0);
		}

		/**
		 * @param c	Bot
		 */
		public synchronized void decrease(Class<? extends BasicBot> c) {
			if (containsKey(c))
				put(c, get(c) - 1);
			else
				throw new IllegalStateException();
		}
	}

	/**
	 * <p>
	 * Liste von BotComponents; wie ArrayList, aber kann zusätzlich 1. Component-Flag-Tabellen
	 * (siehe {@link #applyFlagTable(ctSim.model.bots.BasicBot.CompntWithFlag[]) applyFlagTable()})
	 * und 2. Massen hinzufügen:
	 *
	 * <pre>
	 * componentList.add(
	 *     new BotComponent&lt;...&gt;(...),
	 *     new BotComponent&lt;...&gt;(...),
	 *     new BotComponent&lt;...&gt;(...),
	 *     ...
	 * );
	 * </pre>
	 * </p>
	 *
	 * @author Hendrik Krauß
	 */
	public static class BotComponentList extends BulkList<BotComponent<?>> {
        /** UID */
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
         *             new Goldbein("rechts"),	// beide Instanzen werden betroffen
         *             new Nervensaegmodul(...),
         *         );
         *
         *         // hier die Component-Flag-Tabelle
         *         // setzen, welche BotComponents lesen/schreiben
         *         components.applyFlagTable(
         *             _(Plappermaul.class, WRITES),	// schreibt ins TCP
         *             _(Nervensaegmodul.class, READS, WRITES),	// liest + schreibt
         *             _(Goldbein.class)	// weder noch
         *         );
         *     }
         * }
         * </pre>
		 *
		 * Component-Flag-Tabellen sind also eine Verknüpfung dieser Methode, einer Hilfsmethode mit
		 * Namen Unterstrich (_) und einer kleinen Klasse (CompntWithFlag). Vorteil: eine Superklasse,
		 * z.B. CtBot, kann die Komponenten instanziieren. Subklassen, z.B. SimulierterCtBot und
		 * UeberTcpVerbundenerRealerCtBot, haben ja alle dieselben Komponenten, aber betreiben sie in
		 * verschiedenen Modi (z.B. realer Bot: (fast) alle nur lesen). Die Superklasse macht also
		 * {@code components.add(...)}, die Subklassen können dann den {@code applyFlagsTable(...)}-Aufruf
		 * machen.
		 * </p>
		 * <p>
		 * Hat ein Bot mehrere Komponenten gleicher Klasse, werden die Flags von ihnen allen betroffen.
		 * </p>
		 * 
         * @param compntFlagTable	Flags
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
		 * Gibt ein empfangenes Kommando an alle Botkomponenten (= Sensoren und Aktuatoren). Die
		 * Komponente(n), die sich zuständig fühlt (fühlen), können etwas damit tun (typischerweise
		 * ihren eigenen Wert setzen auf den im Kommando gespeicherten).
		 * </p>
		 * <p>
		 * Implementiert das
		 * <a href="http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern">Chain-of-Responsibility-Pattern</a>
		 * </p>
		 * 
    	 * @param command	Kommando
    	 * @throws ProtocolException 
		 */
    	public void processCommand(Command command) throws ProtocolException {
    		if (command.getDirection() != Command.DIR_REQUEST) {
    			throw new ProtocolException("Kommando ist Unfug: Hat als Richtung nicht 'Anfrage'; ignoriere");
    		}
    		for (BotComponent<?> c : this)
    			c.offerRead(command);
    		if (! command.hasBeenProcessed())
    			throw new ProtocolException("Unbekanntes Kommando: " + command);
    	}

    	/** View-Update durchführen */
    	public void updateView() {
    		for (BotComponent<?> c : BotComponentList.this)
    			c.updateExternalModel();
        }
    }

	/**
	 * <p>
	 * Ordnet einer BotComponent {@link ConnectionFlags} zu.
	 * Component-Flag-Tabellen sind Arrays von diesem Typ; siehe
	 * {@link BotComponentList#applyFlagTable(ctSim.model.bots.BasicBot.CompntWithFlag[]) BotComponentList.applyFlagTable()}.
	 * </p>
	 */
	protected static class CompntWithFlag {
		/** Bot-Komponenten */
		final Class<? extends BotComponent<?>> compntClass;
		/** Flags der Verbindung */
		final ConnectionFlags[] flags;

		/**
		 * @param compntClass	Bot-Komponente
		 * @param flags			Connection-Flags
		 */
		CompntWithFlag(Class<? extends BotComponent<?>> compntClass,
		ConnectionFlags[] flags) {
			this.compntClass = compntClass;
			this.flags = flags;
		}
	}

	/**
	 * <p>
	 * Hilfsmethode, mit der man Component-Flag-Tabellen leicht schreiben kann; siehe
	 * {@link BotComponentList#applyFlagTable(ctSim.model.bots.BasicBot.CompntWithFlag[]) BotComponentList.applyFlagTable()}.
	 * </p>
	 * 
	 * @param compntClass	Bot-Komponente
	 * @param flags			Connection-Flags
	 * @return Component-Flag-Tabelle
	 */
	protected static CompntWithFlag createCompnt(
	Class<? extends BotComponent<?>> compntClass, ConnectionFlags... flags) {
		return new CompntWithFlag(compntClass, flags);
	}

	/** Instanz-Nummer */
	private static final CountingMap numInstances = new CountingMap();

	/** Logger */
	protected final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots");

	/** Komponenten-Liste */
	protected final BotComponentList components = new BotComponentList();

	/** Name */
	private final String name;

	/** Dispose-Listener */
	private final List<Runnable> disposeListeners = Misc.newList();

	/**
	 * @param name	Bot-Name
	 */
	public BasicBot(String name) {
		super();
		this.controller = null;
		// Instanz-Zahl erhöhen
		numInstances.increase(getClass());
		int num = numInstances.get(getClass()) + 1;
		if (num > 1 && !name.contains("(")) {
			this.name = name + " (" + num + ")";
		} else {
			this.name = name;
		}
//		// Wenn wir sterben, Instanz-Zahl reduzieren
//		addDisposeListener(new Runnable() {
//			public void run() {
//				numInstances.decrease(BasicBot.this.getClass());
//			}
//		});
    }

	/**
	 * @see ctSim.model.bots.Bot#addDisposeListener(java.lang.Runnable)
	 */
	@Override
	public void addDisposeListener(Runnable runsWhenAObstDisposes) {
		if (runsWhenAObstDisposes == null)
			throw new NullPointerException();
		disposeListeners.add(runsWhenAObstDisposes);
	}

	/**
	 * @see ctSim.model.bots.Bot#dispose()
	 */
	@Override
	public void dispose() {
		// keine Ausgabe für 3D-Bots, denn zu jedem 3D-Bot gibt es auch einen Sim-Bot
		if (! (this instanceof ThreeDBot)) {
			try {
				lg.info(name + " verkrümelt sich");
			} catch (Exception e) {
				// egal
			}
		}
		
		for (Runnable r : disposeListeners) {
			r.run();
		}
	}

	/**
	 * <p>
	 * Laufende Nummer des AliveObstacles, und zwar abhängig von der Subklasse:
	 * Laufen z.B. 2 GurkenBots und 3 TomatenBots (alle von AliveObstacle abgeleitet),
	 * dann sind die Instance-Numbers:
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
	 * @return Nummer
	 * @see #toString()
	 */
	@Override
	public int getInstanceNumber() {
		/*
		 * Bedenke: Wenn einer ne Subklasse instanziiert, die von AliveObstacle abgeleitet ist,
		 * wird eine AliveObstacle-Instanz automatisch miterzeugt -- Wenn wir hier getClass() aufrufen,
		 * liefert das aber die exakte Klasse (also in unserm Fall niemals AliveObstacle, sondern
		 * z.B. BestimmterDingsBot)
		 */
		return numInstances.get(getClass());
	}

	/**
	 * <p>
	 * Benutzerfreundlicher Name des Bots (wie dem Konstruktor übergeben), an die falls erforderlich
	 * eine laufende Nummer angehängt ist. Laufen z.B. 2 AliveObstacle-Instanzen mit Namen "Gurken-Bot"
	 * und 3 mit "Tomaten-Bot", sind die Rückgabewerte dieser Methode:
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
		return name;
	}

	/**
	 * @see ctSim.model.bots.Bot#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Unbekannter Bottyp";
	}

	/**
	 * @see ctSim.model.bots.Bot#accept(ctSim.model.bots.BotBuisitor)
	 */
	@Override
	public void accept(BotBuisitor buisitor) {
		for (BotComponent<?> c : components)
			buisitor.visit(c, this);
	}

	/**
	 * @see ctSim.model.bots.Bot#updateView()
	 */
	@Override
	public void updateView() throws InterruptedException {
		components.updateView();
	}

	/**
	 * Liefert den Controller zurück, der diesen Bot verwaltet
	 * 
	 * @return Controller	der Controller
	 */
	@Override
	public Controller getController() {
		return controller;
	}

	/**
	 * Setzt den zuständigen Controller
	 * 
	 * @param controller
	 * @throws ProtocolException	wenn die Id dieses Bots im Controller schon belegt ist
	 */
	@Override
	public void setController(Controller controller) throws ProtocolException {
		this.controller = controller;
		if (this instanceof CtBotSimTest)
			return;	// Test-Bots unterstützen keine IDs!
		if (controller != null){
			if (!controller.isIdFree(getId()))
				throw new ProtocolException("Die Id dieses Bots existiert schon im Controller!");
		}
	}
	
	/**
	 * Liefert die Connection zurück über die der Bot zu erreichen ist
	 * 
	 * @return connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Setzt die Connection über die der Bot zu erreichen ist
	 * 
	 * @param connection
	 */
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

}
