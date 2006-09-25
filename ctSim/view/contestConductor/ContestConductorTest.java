package ctSim.view.contestConductor;

import java.sql.SQLException;

import ctSim.controller.Controller;
import ctSim.controller.DefaultController;
import ctSim.controller.Main;
import ctSim.model.rules.Judge;
import ctSim.view.View;

/**
 * <p>
 * Test-Case, der einen vollen Wettbewerbsdurchlauf testet. Der ctSim startet im
 * Contest-Modus, mit nur einer Unterschied: Als Judge wird ein Test-Judge
 * verwendet, der bei jedem Spiel im Simulatorschritt 1 den Bot 1 gewinnen
 * l&auml;sst. Sinn: Testl&auml;ufe zeitlich nicht ausufern lassen.
 * </p>
 * <p>
 * Abgesehen vom Test-Judge wird der Test-Wettbewerb so ausgef&uuml;hrt, wie der
 * tats&auml;chliche. Insbesondere wird als Wettbewerbsdatenbank die normale, in
 * der Konfigurationsdatei ct-sim-contest-conductor.xml angegebene verwendet
 * (also keine Testdatenbank wie in anderen Test-Cases).
 * </p>
 * <p>
 * <strong>Verwendung:</strong>
 * <code>java ctSim.view.contestConductor.ContestConductorTest</code>. Das
 * bedeutet: Dieser Test-Case ist ein eigenst&auml;ndiges Programm.
 * </p>
 * <p>
 * Dies ist also <strong>kein Unit-Test</strong>. Das in Eclipse integrierte
 * JUnit 4 wollte die entscheidende Methode nicht recht ausf&uuml;hren, wenn sie
 * als Test deklariert wurde (mit <code>&#64;Test</code>).
 * M&ouml;glicherweise ist ctSim zu gro&szlig; f&uuml;r JUnit (zuviele Threads
 * usw.).
 * </p>
 *
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class ContestConductorTest {
	/** Siehe {@link ContestConductorTest}. */
	public static void main(String[] args) throws Exception {
		new Main("-conf", "config/ct-sim-contest-conductor.xml") {
			@Override
	        protected View buildContestConductor(Controller c)
			throws SQLException, ClassNotFoundException {
				return new MockContestConductor(c);
			}
		};
	}

	/**
	 * Attrappe, die in den ContestConductor den Test-ContestJudge injiziert.
	 */
	public static class MockContestConductor extends ContestConductor {
		public MockContestConductor(Controller controller)
		throws SQLException, ClassNotFoundException {
			super(controller);
        }

		/**
		 * Liefert einen ContestJudge, der zu Testzwecken immer Bot 1 im ersten
		 * Simulationsschritt zum Sieger erkl&auml;rt.
		 */
		@Override
        protected Judge buildJudge(Controller c) {
            return this.new ContestJudge((DefaultController)c) {
				@Override
                public boolean isSimulationFinished() {
					lg.fine("ContestJudge-Attrappe: Bot 1 gewinnt, " +
							"beende Spiel");
                    try {
                        setWinner(botIds.keySet().iterator().next());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                }
            };
        }
	}
}
