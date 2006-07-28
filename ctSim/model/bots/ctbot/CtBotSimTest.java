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
package ctSim.model.bots.ctbot;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.model.World;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.ctbot.components.BorderSensor;
import ctSim.model.bots.ctbot.components.DistanceSensor;
import ctSim.model.bots.ctbot.components.LightSensor;
import ctSim.model.bots.ctbot.components.LineSensor;

/**
 * Klasse aller simulierten c't-Bots, die nur innerhalb des Simulators existieren
 *
 */
public class CtBotSimTest extends CtBotSim {
	
	// TODO: weg
	private World world;
	
	private Sensor irL, irR, lineL, lineR, borderL, borderR, lightL, lightR;
	
	/**
	 * Der Konstruktor
	 * @param w Die Welt
	 * @param name Der Name des Bot
	 * @param pos Position
	 * @param head Blickrichtung
	 */
	public CtBotSimTest(World w, String name, Point3d pos, Vector3d head) {
		super(w, name, pos, head);
		
		this.world = w;
		
		initSensors();
		initActuators();
	}
	
	private void initSensors() {
		
		this.irL = new DistanceSensor(this.world, this, "IrL", new Point3d(-0.036d, 0.0554d, 0.035d-BOT_HEIGHT/2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.irR = new DistanceSensor(this.world, this, "IrR", new Point3d(0.036d, 0.0554d, 0.035d-BOT_HEIGHT/2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		
		this.lineL = new LineSensor(this.world, this, "LineL", new Point3d(-0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.lineR = new LineSensor(this.world, this, "LineR", new Point3d(0.004d, 0.009d, -0.011d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		
		this.borderL = new BorderSensor(this.world, this, "BorderL", new Point3d(-0.036d, 0.0384d, 0d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.borderR = new BorderSensor(this.world, this, "BorderR", new Point3d(0.036d, 0.0384d, 0d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		
		this.lightL = new LightSensor(this.world, this, "LightL", new Point3d(-0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		this.lightR = new LightSensor(this.world, this, "LightR", new Point3d(0.032d, 0.048d, 0.060d - BOT_HEIGHT / 2), new Vector3d(0d, 1d, 0d)); //$NON-NLS-1$
		
		this.addSensor(this.irL);
		this.addSensor(this.irR);
		
		// new Point3d(0d, 0d, 0d), new Vector3d(0d, 1d, 0d)); // 
		this.addSensor(this.lineL);
		this.addSensor(this.lineR);
		
		this.addSensor(this.borderL);
		this.addSensor(this.borderR);
		
		this.addSensor(this.lightL);
		this.addSensor(this.lightR);
	}
	
	private void initActuators() {
		
		// TODO!
	}
	
	@Override
	protected void init() {
		// TODO Auto-generated method stub
		
	}
	
	@SuppressWarnings({"boxing","unchecked"})
	@Override
	protected void work() {
		
		// TDO prüfen ob: super.work();
		
		@SuppressWarnings({"unused"}) double ll = 100d, rr = 100d;
		
		double irl = (Double)this.irL.getValue();
		double irr = (Double)this.irR.getValue();
		
		// Ansteuerung fuer die Motoren in Abhaengigkeit vom Input
		// der IR-Abstandssensoren, welche die Entfernung in mm
		// zum naechsten Hindernis in Blickrichtung zurueckgeben

		// Solange die Wand weit weg ist, wird Stoff gegeben:
		if (irl >= 500) {
			ll = 255;
		}
		if (irr >= 500) {
			rr = 255;
		}

		// Vorsicht, die Wand kommt naeher:
		// Jetzt den Motor auf der Seite, die weiter entfernt ist,
		// langsamer laufen lassen als den auf der anderen Seite
		// - dann bewegt sich der Bot selbst
		// bei Wandkollisionen noch etwas und kommt eventuell
		// wieder frei:
		if (irl < 500 && irl >= 200) {
			if (irl <= irr)
				ll = 80;
			else
				ll = 50;
		}
		if (irr < 500 && irr >= 200) {
			if (irl > irr)
				rr = 80;
			else
				rr = 50;
		}

		// Ist ein Absturz zu befuerchten?
		short borderl = (Short)this.borderL.getValue();
		short borderr = (Short)this.borderR.getValue();
		if (borderl > borderr) {
			ll = 100;
			rr = -100;
		} else if (borderl < borderr) {
			ll = -100;
			rr = 100;
		}
		
		// Kollision oder Abgrund droht: Auf dem Teller rausdrehen,
		// und zwar immer nach links!
		if (irl < 200 || irr < 200 || borderl > 1000 || borderr > 1000) {
			ll = -100;
			rr = 100;
		}
		
		// TODO:
//		this.setActMotL(ll);
//		this.setActMotR(rr);
	}
}