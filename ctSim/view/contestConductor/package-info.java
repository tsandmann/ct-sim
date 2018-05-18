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

/**
 * <p>
 * Ermöglicht die Durchführung eines c't-Bot-Wettbewerbs wie den im Oktober 2006. Das Package verwendet eine
 * MySQL-Datenbank (<a href="doc-files/datenbankschema.pdf">Dokumentation des Schemas</a>,
 * <a href="doc-files/datenbankschema.sql">Schema als SQL-Skript</a>).
 * </p>
 * <p>
 * <h3>Hintergrund: Wie läuft ein Wettbewerb?</h3>
 * Das Turnier wird in zwei Phasen abgewickelt: Zunächst wird in einer Vorrunde jeder Bot einzeln durch einen
 * Parcours geschickt und die benötigte Zeit gestoppt, was eine Rangliste ergibt. In der Hauptphase kommt dann
 * das K.o.-System zum Einsatz, d.h. dass sich jeweils der Gewinner eines Duells für das nächste qualifiziert.
 * Nur unter den vier Besten werden alle Plätze ausgespielt.
 * </p>
 * <p>
 * Die Rangliste aus der Vorrunde ermöglicht Ausgewogenheit im Turnierplan der Hauptphase: Die Spieler werden
 * so platziert, dass sich der Schnellste und der Zweitschnellste aus der Vorrunde erst im Finale begegnen
 * können, der Schnellste und der Drittschnellste erst im Halbfinale usw. So werden allzu verzerrte
 * Wettbewerbsergebnisse vermieden - gäbe es z.B. eine einfache Auslosung statt einer Vorrunde, könnten zufällig
 * die beiden besten Implementierungen in der ersten Runde aufeinandertreffen. Das würde heißen, dass einer der
 * beiden Schnellsten schon im ersten Durchgang ausscheidet, was seine tatsächliche Stärke verfälscht
 * widerspiegelt. Die Vorrunde soll das vermeiden und helfen, die Spannung bis zuletzt aufrechtzuerhalten.
 * </p>
 * <p>
 * Für die Hauptphase gilt: Treten nicht genügend Teams an, um alle Zweikämpfe des ersten Durchgangs zu füllen,
 * erhalten möglichst viele Bots ein Freilos, das sie automatisch für die nächste Runde qualifiziert.
 * Im Extremfall findet somit in der ersten Runde des Turniers nur ein einziges Duell statt - dafür sind alle
 * anschließenden Durchgänge voll besetzt.
 * </p>
 * <p>
 * <a name="turnierbaum">
 * <h3>Turnierbaum</h3>
 * </a> <em>Beispiel:</em><br>
 * <a href="doc-files/turnierbaum.pdf"><img src="doc-files/turnierbaum-thumbnail.png" /> </a><br>
 * </p>
 * <p>
 * Der Turnierbaum beschreibt:
 * <ul>
 * <li>Welche Spiele es gibt</li>
 * <li>Welcher Bot in welchen Spielen spielt. (Ein Bot wird identifiziert über seine ID in der Tabelle ctsim_bot.
 * Im Beispiel: Nummern von 1 bis 21, in Kreisen dargestellt.)</li>
 * </ul>
 * </p>
 * <p>
 * <h4>Grobes Verfahren</h4>
 * Der Turnierbaum wird nach der Vorrunde und vor der Hauptrunde erzeugt und in die DB-Tabelle
 * <strong>ctsim_game</strong> geschrieben (<a href="doc-files/datenbankschema.pdf">siehe Datenbank-Doku</a>).
 * Das Beispiel stellt einen Turnierbaum zu diesem Zeitpunkt dar. Alle Spiele sind zu diesem Zeitpunkt angelegt,
 * aber für viele von ihnen ist noch nicht klar, welche Bots die zwei Kontrahenten sein werden (im Beispiel ist
 * das mit "?" markiert, in der Tabelle mit bot1 = NULL und/oder bot2 = NULL). Wenn später im Verlauf der
 * Hauptrunde ein Spiel, z.B. Bot 1 gegen Bot 9, abgeschlossen ist, scheidet der Verlierer aus (K.o.-System).
 * Der Sieger wird ins entsprechende Spiel des nächsthöheren Levels versetzt. Der Sieger kommt also auf den Platz
 * des Fragezeichens im höherliegenden Spiel. Das ist im Beispiel durch Pfeile markiert. (Die einzige Ausnahme
 * vom K.o.-System ist das Halbfinale, wo die Gewinner ins Finale und die Verlierer ins Spiel um den 3. Platz
 * gesetzt werden.)
 * </p>
 * <p>
 * <h4>Details</h4>
 * <ul>
 * <li>Ein Spiel wird identifiziert über die Datenbankfelder gameId und levelId, z.B. 5 und 8 für fünftes
 * Achtelfinalspiel.</li>
 * <li>levelIds sind Zweierpotenzen (1, 2, 4, 8 usw.). Es gibt zwei Ausnahmewerte: -1 = Vorrunde, 0 = Spiel
 * um den 3. Platz.</li>
 * <li>gameIds sind fortlaufend und 1-based (1, 2, 3 ...)</li>
 * <li>Je nach Teilnehmerzahl kann es sein, dass nicht alle Spiele des untersten Levels gefüllt sind -- im
 * Beispiel mit 21 Spielern: Von den 16 Spielen im Sechzehntelfinale bleiben 11 frei und werden nie gespielt.</li>
 * <li>Es stehen aber immer vollständige Levels in der Datenbank, also im Beispiel alle
 * 16 Sechzehntelfinalsspiele. Die "leeren" Spiele haben bot1 = NULL, bot2 = NULL, state = "not init" und
 * werden nach der Planung nie mehr angefasst.</li>
 * </ul>
 * </p>
 * <p>
 * <h3>Architektur</h3>
 * Das ContestConductor-Subsystem sieht so aus:
 *
 * <pre>
 *   Außenwelt
 *   (DefaultController)
 *      |
 *      |
 *      |
 *      |
 *      v                     hat einen
 *   ContestConductor -------------------------&gt; TournamentPlanner
 *          |                                            |
 *          |                                            |
 *          | hat einen                                  | hat einen
 *          |                                            |
 *          v                                            v
 * ConductorToDatabaseAdapter ----.     .---- PlannerToDatabaseAdapter
 *                                |     |
 *                                |     |
 *             ist abgeleitet von |     | ist abgeleitet von
 *                                |     |
 *                                v     v
 *                            DatabaseAdapter
 *                                   |
 *                                   |
 *                                   | ist verbunden mit
 *                                   |
 *                                   v
 *                            MySQL-Datenbank
 * </pre>
 *
 * Der TournamentPlanner ist wichtig <strong>vor</strong> dem eigentlichen Turnier: er plant die Vorrunde
 * und erstellt den Turnierbaum (s.o.). Der ContestConductor ist wichtig <strong>während</strong> dem
 * Turnier: Er lädt gemäß dem Turnierbaum die zwei Kontrahenten, lässt sie spielen, ermittelt den Gewinner
 * und trägt ihn im Turnierbaum ein.
 * </p>
 *
 * @author Hendrik Krauß
 */
package ctSim.view.contestConductor;
