package ctSim.controller;

import java.io.File;

import ctSim.model.rules.Judge;
import ctSim.view.View;

//$$ doc alle Methoden
/**
 * <p>
 * Interface, das die Views der Applikation verwenden, um auf den Controller
 * zuzugreifen. ("View" und "Controller" sind dabei im Sinne von <a
 * href="http://en.wikipedia.org/wiki/Model-view-controller"
 * target="_blank">MVC</a> zu verstehen.)
 * </p>
 * <p>
 * Das Interface definiert also in erster Linie, welche Dienste ein Controller
 * den Views der Applikation zur Verf&uuml;gung stellt. Daneben hat das
 * Interface die offensichtliche Bedeutung, dass der Controller der Applikation
 * gegen&uuml;ber den Views (ausschlie&szlig;lich) unter diesem Interface
 * erscheint.
 * </p>
 *
 * @see View
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public interface Controller {
	public void setView(View view);

	public void closeWorld();

	/**
	 * Startet einen externen Bot
	 *
	 * @param filename Pfad zum Binary des Bots
	 */
	public void invokeBot(String filename);

	public void invokeBot(File file);

	public void addTestBot();

	public void connectToTcp(String hostname, String port);

	public void openWorldFromFile(File sourceFile);

	public void openWorldFromXmlString(String parcoursAsXml);

	public void openRandomWorld();

	public void setJudge(String judgeClassName);

	public void setJudge(Judge j);

	// $$ Vorlaeufige Methode
	public void pause();

	// $$ Vorlaeufige Methode
	public void unpause();

	/**
	 * Von au&szlig;en, d.h. von einer Bootstrap-Komponente, aufzurufen, sobald
	 * die Hauptkomponenten der Applikation (Controller und View) initialisiert
	 * und untereinander korrekt verdrahtet sind. Ab diesem Methodenaufruf liegt
	 * die Verantwortung f&uuml;r den Programmablauf beim Controller.
	 */
	void onApplicationInited();
}