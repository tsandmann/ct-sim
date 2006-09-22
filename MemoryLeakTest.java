import java.io.File;

import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.controller.Main;

/**
 * Test zum Untersuchen von Speicherlecks, wenn hintereinander viele
 * World-Objekte geladen werden. Als Hauptprogramm auszuführen.
 */
public class MemoryLeakTest {
	public static void main(String[] args) throws Exception {
		new Main() {
			@Override
			protected Controller buildController() {
				return new TestController();
			}
		};
	}

	public static class TestController extends DefaultController {
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
                } catch (InterruptedException e) { }
			}
        }
	}
}
