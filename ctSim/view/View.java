package ctSim.view;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;

/**
 * <p>
 * Interface, das der Controller verwendet, um auf die Views der Applikation
 * zuzugreifen. ("Controller" und "View" sind dabei im Sinne von <a
 * href="http://en.wikipedia.org/wiki/Model-view-controller" target="_blank">MVC</a>
 * zu verstehen.)
 * </p>
 * <p>
 * Alle Methoden des Interface haben Event-Handler-Charakter: Der Controller
 * ruft sie auf, um die Views dar&uuml;ber zu informieren, dass ein bestimmtes
 * Event aufgetreten ist. Das Interface definiert also in erster Linie, bei
 * welchen Ereignissen Views vom Controller benachrichtigt werden. Daneben hat
 * das Interface die offensichtliche Bedeutung, dass Views der Applikation
 * gegen&uuml;ber dem Controller (ausschlie&szlig;lich) unter diesem Interface
 * erscheinen.
 * </p>
 *
 * @see Controller
 * @see ViewYAdapter
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public interface View {
	/**
	 * <p>
	 * Wird vom Controller einmalig kurz nach Programmstart aufgerufen, um
	 * mitzuteilen, dass die Applikation nun vollständig initialisiert ist.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode
	 * schlie&szlig;en einen Splash-Screen und machen ein Hauptfenster sichtbar
	 * (das zuvor im Konstruktor des Views aufgebaut wurde, ohne angezeigt zu
	 * werden).
	 * </p>
	 */
	public void onApplicationInited();

	/**
	 * <p>
	 * Wird vom Controller aufgerufen, wenn sich eine Welt eben ge&ouml;ffnet
	 * wurde. Falls bisher eine Welt offen war, gibt der Aufruf auch an, dass
	 * sie ab sofort permanent au&szlig;er Gebrauch ist.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode umfassen die
	 * De-Initalisierung und Zerst&ouml;rung der Darstellung der alten Welt
	 * (falls vorhanden) sowie die Initialisierung der Darstellung der neuen
	 * Welt. Implementierungen sind <strong>dringend</strong> angehalten, bei
	 * Aufruf dieser Methode alle Referenzen auf die alte Welt zu
	 * &uuml;berschreiben oder auf <code>null</code> zu setzen, um sie zur
	 * Garbage Collection freizugeben (d.h. um Speicherlecks zu vermeiden).
	 * </p>
	 *
	 * @param w Die Welt, die eben neu ge&ouml;ffnet wurde.
	 */
	public void onWorldOpened(World w);

	/**
	 * <p>
	 * Wird vom Controller aufgerufen, wenn der f&uuml;r die Simulation
	 * verwendete Judge gesetzt wird. Der Judge wird gesetzt beim Programmstart,
	 * falls ein entsprechender Eintrag in der Konfigurationsdatei gefunden
	 * wird, oder falls ein View angefordert hat, dass ein Judge gesetzt wird
	 * (und der Controller der Anforderung nachgekommen ist). Die Methode kann
	 * aufgerufen werden, bevor {@link #onApplicationInited()} aufgerufen wird.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode aktualisieren
	 * die Anzeige, die den Benutzer dar&uuml;ber informiert, welcher Judge
	 * gerade aktiv ist.
	 * </p>
	 *
	 * @param j Der Judge, der eben gesetzt wurde.
	 */
	public void onJudgeSet(Judge j);

	// $$ doc
	public void onBotAdded(Bot bot);

	// $$ doc
	public void onBotRemoved(Bot bot);

	/**
	 * <p>
	 * Wird vom Controller einmal pro Simulationsschritt aufgerufen. Wird nie
	 * aufgerufen in der Zeit, in der keine Simulation l&auml;uft (was der Fall
	 * ist, weil entweder seit Programmstart noch keine Simulation gestartet
	 * wurde, oder weil eine laufende Simulation pausiert wurde).
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode umfassen
	 * Neuzeichnen von grafischen Darstellungen der Bots, der Welt usw.
	 * </p>
	 *
	 * @param simTimeInMs Die Simulationszeit des aktuellen Schritts. Details
	 * siehe {@link World#getSimTimeInMs()}.
	 */
	public void onSimulationStep(long simTimeInMs);

	//$$ sollte die hier nicht auch aufgerufen werden, wenn einer stop geklickt hat und sowas?
	/** Wird vom Controller aufgerufen, wenn die Simulation beendet wurde. Simulationen werden beendet, wenn der aktive Judge angeordnet hat,
	 * das Spiel zu beenden &ndash; typischerweise wenn ein Bot das Zielfeld erreicht hat.
	 *
	 * @see Judge#isSimulationFinished(long)
	 */
	public void onSimulationFinished();
}
