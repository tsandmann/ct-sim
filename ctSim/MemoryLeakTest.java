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

package ctSim;
import java.io.File;

import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.controller.Main;

/**
 * Test zum Untersuchen von Speicherlecks, wenn hintereinander viele
 * World-Objekte geladen werden. Als Hauptprogramm auszufuehren.
 */
public class MemoryLeakTest {
	/**
	 * main
	 * @param args
	 */
	public static void main(String[] args) {
		Main.dependencies.reRegisterImplementation(
			Controller.class, TestController.class);
		Main.main();
	}

	/**
	 * Testklasse
	 */
	public static class TestController extends DefaultController {
		/**
		 * @see ctSim.controller.DefaultController#onApplicationInited()
		 */
		@Override
        public void onApplicationInited() {
			super.onApplicationInited();
			for (int i = 0; i < 50; i++) {
				lg.info("\u00D6ffne Welt " + i);
				openWorldFromFile(new File("parcours/testparcours3.xml"));
				addTestBot();
				addTestBot();
				unpause();
				try {
					Thread.sleep(2000);
                } catch (InterruptedException e) {
                	// ist wurst
                }
			}
        }
	}
}
