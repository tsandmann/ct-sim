package ctSim.view.contestConductor;

import ctSim.controller.Controller;
import ctSim.controller.Main;
import ctSim.view.contestConductor.ContestConductor.ContestJudge;

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
		Main.dependencies.reRegisterImplementation(
			ContestJudge.class, MockContestJudge.class);
		Main.main("-conf", "config/ct-sim-contest-conductor-local.xml");
	}

	/**
	 * ContestJudge-Attrappe, die zu Testzwecken immer Bot 1 im ersten
	 * Simulationsschritt zum Sieger erkl&auml;rt.
	 */
	public static class MockContestJudge extends ContestJudge {
		public MockContestJudge(Controller controller,
			ContestConductor concon) {
			super(controller, concon);
	    }

		@Override
        public boolean isSimulationFinished() {
			concon.lg.fine("ContestJudge-Attrappe: Bot 1 gewinnt, " +
					"beende Spiel");
            try {
            	GameOutcome o = new GameOutcome();
            	o.winner = concon.botIds.keySet().iterator().next();
                setWinner(o);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }
	}
}
