package ctSim.controller;

import java.io.File;
import java.net.ProtocolException;

import ctSim.model.Command;
import ctSim.model.rules.Judge;
import ctSim.util.BotID;
import ctSim.view.View;

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
	/**
	 * @param view das neue View
	 */
	public void setView(View view);

	/**
	 * Schliesst die Welt
	 */
	public void closeWorld();
	
	/**
	 * Startet einen externen Bot
	 *
	 * @param file Zeigt auf die Binary des Bots
	 */
	public void invokeBot(File file);

	/**
	 * Fuegt einen Testbot hinzu
	 */
	public void addTestBot();

	/**
	 * Verbindet zu einem Bot
	 * @param hostname	Host
	 * @param port		Port
	 */
	public void connectToTcp(String hostname, String port);

	/**
	 * Laedt eine Welt aus einer Datei
	 * @param sourceFile Weltdatei
	 */
	public void openWorldFromFile(File sourceFile);

	/**
	 * Laedt eine Welt aus einem XML-formatierten String
	 * @param parcoursAsXml Welt als XML-String
	 */
	public void openWorldFromXmlString(String parcoursAsXml);

	/**
	 * Laedt eine zufaellig generierte Welt
	 */
	public void openRandomWorld();

	/**
	 * Setzt den Schiedsrichter
	 * @param judgeClassName
	 */
	public void setJudge(String judgeClassName);

	/**
	 * Setzt den Schiedsrichter
	 * @param j
	 */
	public void setJudge(Judge j);

	/**
	 * Haelt die Simulation fuer unbestimmte Zeit an
	 */
	public void pause();

	/**
	 * Setzt die Simulation fort
	 */
	public void unpause();

	/**
	 * Von au&szlig;en, d.h. von einer Bootstrap-Komponente, aufzurufen, sobald
	 * die Hauptkomponenten der Applikation (Controller und View) initialisiert
	 * und untereinander korrekt verdrahtet sind. Ab diesem Methodenaufruf liegt
	 * die Verantwortung f&uuml;r den Programmablauf beim Controller.
	 */
	void onApplicationInited();
	
	/**
     * Setzt alle Bots zurueck
     */
    public void resetAllBots();
	
	/**
	 * Liefert ein Kommando an einen Bot aus.
	 * Diese Routine kann dazu benutzt werden, um Bot-2-Bot-Kommunikation zu betreiben
	 * Sender und Empfänger stehen in dem command drin 
	 * @param command das zu übertragende Kommando
	 * @throws ProtocolException Falls kein passender empfaenger gefunden wurde
	 */
	public void deliverMessage(Command command) throws ProtocolException;
	
	/**
	 * Liefert eine Id aus dem Adresspoll zurück
	 * @return Die neue Id
	 * @throws ProtocolException Wenn keine Adresse mehr frei
	 */
	public BotID generateBotId() throws ProtocolException;
	
	/**
	 * Testet, ob bereits ein Bot diese Id hat
	 * @param id zu testende Id
	 * @return True, wenn noch kein Bot diese Id nutzt 
	 */
	public boolean isIdFree(BotID id);
}