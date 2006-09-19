package ctSim.view;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;

/**
 * <p>
 * Interface, das der Controller verwendet, um auf die Views der Applikation
 * zuzugreifen. ("Controller" und "View" sind dabei im Sinne von <a
 * href="http://en.wikipedia.org/wiki/Model-view-controller">MVC</a> zu
 * verstehen.)
 * </p>
 * <p>
 * Das Interface definiert also in erster Linie, was Views vom Controller
 * erwarten k&ouml;nnen. Daneben hat das Interface die offensichtliche
 * Bedeutung, dass Views der Applikation gegen&uuml;ber dem Controller
 * (ausschlie&szlig;lich) unter diesem Interface erscheinen.
 * </p>
 *
 * @see Controller
 * @see ViewYAdapter
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public interface View {
	/**
	 * <p>
	 * Wird einmalig kurz nach Programmstart aufgerufen, um mitzuteilen, dass
	 * die Applikation nun vollständig initialisiert ist.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode
	 * schlie&szlig;en den Splash-Screen und machen das Hauptfenster sichtbar
	 * (das zuvor im Konstruktor des Views aufgebaut wurde, ohne angezeigt zu
	 * werden).
	 * </p>
	 */
	public void onApplicationInited();

	/**
	 * <p>
	 * Wird aufgerufen, wenn sich die Welt &auml;ndert.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode umfassen die
	 * De-Initalisierung und Zerst&ouml;rung der Darstellung der alten Welt
	 * (falls vorhanden) sowie die Initialisierung der Darstellung der neuen
	 * Welt.
	 * </p>
	 */
	public void openWorld(World w);

	// $$ doc
	public void addBot(Bot bot);

	// $$ doc
	public void removeBot(Bot bot);

	/**
	 * <p>
	 * Wird einmal pro Simulationsschritt aufgerufen. Wird nie aufgerufen in der
	 * Zeit, in der keine Simulation l&auml;uft (was der Fall ist, weil entweder
	 * seit Programmstart noch keine Simulation gestartet wurde, oder weil eine
	 * laufende Simulation pausiert wurde).
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode umfassen
	 * Neuzeichnen von grafischen Darstellungen der Bots, der Welt usw.
	 * </p>
	 *
	 * @param simTimeInMs Die Simulationszeit des aktuellen Schritts. Details
	 *            siehe {@link World#getSimTimeInMs()}.
	 */
	public void update(long simTimeInMs);
}
