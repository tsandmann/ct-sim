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

package ctSim.view;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;
import ctSim.model.rules.Judge;

/**
 * <p>
 * Interface, das der Controller verwendet, um auf die Views der Applikation zuzugreifen. ("Controller"
 * und "View" sind dabei im Sinne von
 * <a href="http://en.wikipedia.org/wiki/Model-view-controller" target="_blank">MVC</a>
 * zu verstehen.)
 * </p>
 * <p>
 * Alle Methoden des Interface haben Event-Handler-Charakter: Der Controller ruft sie auf, um die Views
 * darüber zu informieren, dass ein bestimmtes Event aufgetreten ist. Das Interface definiert also in
 * erster Linie, bei welchen Ereignissen Views vom Controller benachrichtigt werden. Daneben hat das
 * Interface die offensichtliche Bedeutung, dass Views der Applikation gegenüber dem Controller
 * (ausschließlich) unter diesem Interface erscheinen.
 * </p>
 *
 * @see Controller
 * @see ViewYAdapter
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public interface View {
	/**
	 * <p>
	 * Wird vom Controller einmalig kurz nach Programmstart aufgerufen, um mitzuteilen, dass die
	 * Applikation nun vollständig initialisiert ist.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode schließen einen Splash-Screen und
	 * machen ein Hauptfenster sichtbar (das zuvor im Konstruktor des Views aufgebaut wurde, ohne
	 * angezeigt zu werden).
	 * </p>
	 */
	public void onApplicationInited();

	/**
	 * <p>
	 * Wird vom Controller aufgerufen, wenn eben eine Welt geöffnet wurde.
	 * Falls bisher eine Welt offen war, gibt der Aufruf auch an, dass sie ab sofort permanent außer
	 * Gebrauch ist.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode umfassen die De-Initalisierung und
	 * Zerstörung der Darstellung der alten Welt (falls vorhanden) sowie die Initialisierung der
	 * Darstellung der neuen Welt. Implementierungen sind <strong>dringend</strong> dazu angehalten,
	 * bei Aufruf dieser Methode alle Referenzen auf die alte Welt zu überschreiben oder auf
	 * <code>null</code> zu setzen, um sie zur Garbage Collection freizugeben (d.h. um Speicherlecks
	 * zu vermeiden).
	 * </p>
	 *
	 * @param newWorld	Die Welt, die eben neu geöffnet wurde.
	 */
	public void onWorldOpened(World newWorld);

	/**
	 * <p>
	 * Wird vom Controller aufgerufen, wenn der für die Simulation verwendete Judge gesetzt wird.
	 * Der Judge wird beim Programmstart gesetzt, falls ein entsprechender Eintrag in der
	 * Konfigurationsdatei gefunden wird, oder falls ein View angefordert hat, dass ein Judge gesetzt
	 * wird (und der Controller der Anforderung nachgekommen ist). Die Methode kann aufgerufen werden,
	 * bevor {@link #onApplicationInited()} aufgerufen wird.
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode aktualisieren die Anzeige, die den
	 * Benutzer darüber informiert, welcher Judge gerade aktiv ist.
	 * </p>
	 *
	 * @param j	Der Judge, der eben gesetzt wurde
	 */
	public void onJudgeSet(Judge j);

	/**
	 * Handler für neuer Bot da
	 *
	 * @param bot	Bot
	 */
	public void onBotAdded(Bot bot);

	/**
	 * <p>
	 * Wird vom Controller einmal pro Simulationsschritt aufgerufen. Wird nie aufgerufen in der Zeit,
	 * in der keine Simulation läuft (was der Fall ist, weil entweder seit Programmstart noch keine
	 * Simulation gestartet wurde, oder weil eine laufende Simulation pausiert wurde).
	 * </p>
	 * <p>
	 * <strong>Typische Implementierungen</strong> dieser Methode umfassen Neuzeichnen von grafischen
	 * Darstellungen der Bots, der Welt usw.
	 * </p>
	 *
	 * @param simTimeInMs	Die Simulationszeit des aktuellen Schritts. Details siehe {@link World#getSimTimeInMs()}.
	 */
	public void onSimulationStep(long simTimeInMs);

	/**
	 * Wird vom Controller aufgerufen, wenn die Simulation beendet wurde.
	 * Simulationen werden beendet, wenn der aktive Judge angeordnet hat, das Spiel zu beenden;
	 * typischerweise wenn ein Bot das Zielfeld erreicht hat.
	 *
	 * @see Judge#isSimulationFinished(long)
	 */
	public void onSimulationFinished();

	/** Veranlasst einen Reset aller Bots */
	public void onResetAllBots();
}