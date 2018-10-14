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

package ctSim.view.contestConductor;

import ctSim.controller.Controller;
import ctSim.controller.Main;
import ctSim.view.contestConductor.ContestConductor.ContestJudge;

/**
 * <p>
 * Test-Case, der einen vollen Wettbewerbsdurchlauf testet. Der ctSim startet im
 * Contest-Modus, mit nur einer Unterschied: Als Judge wird ein Test-Judge
 * verwendet, der bei jedem Spiel im Simulatorschritt 1 den Bot 1 gewinnen
 * lässt. Sinn: Testläufe zeitlich nicht ausufern lassen.
 * </p>
 * <p>
 * Abgesehen vom Test-Judge wird der Test-Wettbewerb so ausgeführt, wie der
 * tatsächliche. Insbesondere wird als Wettbewerbsdatenbank die normale, in
 * der Konfigurationsdatei ct-sim-contest-conductor.xml angegebene verwendet
 * (also keine Testdatenbank wie in anderen Test-Cases).
 * </p>
 * <p>
 * <strong>Verwendung:</strong>
 * <code>java ctSim.view.contestConductor.ContestConductorTest</code>. Das
 * bedeutet: Dieser Test-Case ist ein eigenständiges Programm.
 * </p>
 * <p>
 * Dies ist also <strong>kein Unit-Test</strong>. Das in Eclipse integrierte
 * JUnit 4 wollte die entscheidende Methode nicht recht ausführen, wenn sie
 * als Test deklariert wurde (mit <code>&#64;Test</code>).
 * Möglicherweise ist ctSim zu groß für JUnit (zuviele Threads
 * usw.).
 * </p>
 *
 * @author Hendrik Krauß &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ContestConductorTest {
	/** Siehe {@link ContestConductorTest}. 
	 * @param args */
	public static void main(String[] args) {
		Main.dependencies.reRegisterImplementation(
			ContestJudge.class, MockContestJudge.class);
		Main.main("-conf", "config/ct-sim-contest-conductor-local.xml");
	}

	/**
	 * ContestJudge-Attrappe, die zu Testzwecken immer Bot 1 im ersten
	 * Simulationsschritt zum Sieger erklärt.
	 */
	public static class MockContestJudge extends ContestJudge {
		/**
		 * @param controller
		 * @param concon
		 */
		public MockContestJudge(Controller controller,
			ContestConductor concon) {
			super(controller, concon);
	    }

		/**
		 * @see ctSim.view.contestConductor.ContestConductor.ContestJudge#isSimulationFinished()
		 */
		@Override
		public boolean isSimulationFinished() {
			concon.lg.fine("ContestJudge-Attrappe: Bot 1 gewinnt, " +
					"beende Spiel");
            try {
            	GameOutcome o = new GameOutcome();
            	o.winner = ContestConductor.BotView.getAll().get(0);
                setWinner(o);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
	}
}
