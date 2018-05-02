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

package ctSim.controller;

import java.io.File;
import java.io.IOException;
import java.net.ProtocolException;

import ctSim.model.Command;
import ctSim.model.Map.MapException;
import ctSim.model.World;
import ctSim.model.rules.Judge;
import ctSim.util.BotID;
import ctSim.view.View;

/**
 * <p>
 * Interface, das die Views der Applikation verwendet, um auf den Controller
 * zuzugreifen. ("View" und "Controller" sind dabei im Sinne von
 * <a href="http://en.wikipedia.org/wiki/Model-view-controller" target="_blank">MVC</a> zu verstehen.)
 * </p>
 * <p>
 * Das Interface definiert also in erster Linie, welche Dienste ein Controller den Views der Applikation
 * zur Verfügung stellt. Daneben hat das Interface die offensichtliche Bedeutung, dass der Controller
 * der Applikation gegenüber den Views (ausschließlich) unter diesem Interface erscheint.
 * </p>
 *
 * @see View
 *
 * @author Hendrik Krauß (hkr@heise.de)
 */
public interface Controller {
	/**
	 * @param view	das neue View
	 */
	public void setView(View view);

	/** Schliesst die Welt */
	public void closeWorld();

	/**
	 * Startet einen externen Bot
	 *
	 * @param file	zeigt auf die Binary des Bots
	 */
	public void invokeBot(File file);

	/** Fügt einen Testbot hinzu */
	public void addTestBot();

	/**
	 * Verbindet zu einem Bot
	 *
	 * @param hostname	Host
	 * @param port		Port
	 */
	public void connectToTcp(String hostname, String port);

	/**
	 * Lädt eine Welt aus einer Datei
	 *
	 * @param sourceFile	Weltdatei
	 */
	public void openWorldFromFile(File sourceFile);

	/**
	 * Lädt eine Welt aus einem XML-formatierten String
	 *
	 * @param parcoursAsXml	Welt als XML-String
	 */
	public void openWorldFromXmlString(String parcoursAsXml);

	/** Lädt eine zufällig generierte Welt */
	public void openRandomWorld();

	/**
	 * Setzt den Schiedsrichter
	 *
	 * @param judgeClassName
	 */
	public void setJudge(String judgeClassName);

	/**
	 * Setzt den Schiedsrichter
	 *
	 * @param j
	 */
	public void setJudge(Judge j);

	/** Hält die Simulation für unbestimmte Zeit an */
	public void pause();

	/** Setzt die Simulation fort */
	public void unpause();

	/**
	 * Von außen, d.h. von einer Bootstrap-Komponente, aufrufen, sobald
	 * die Hauptkomponenten der Applikation (Controller und View) initialisiert
	 * und untereinander korrekt verdrahtet sind. Ab diesem Methodenaufruf liegt
	 * die Verantwortung für den Programmablauf beim Controller.
	 */
	void onApplicationInited();

	/** Setzt alle Bots zurück */
	public void resetAllBots();

	/**
	 * Liefert ein Kommando an einen Bot aus.
	 * Diese Routine kann dazu benutzt werden, um Bot-2-Bot-Kommunikation zu betreiben
	 * Sender und Empfänger stehen in dem Command drin.
	 *
	 * @param command	das zu übertragende Kommando
	 * @throws ProtocolException	falls kein passender Empfänger gefunden wurde
	 */
	public void deliverMessage(Command command) throws ProtocolException;

	/**
	 * Liefert eine Id aus dem Adresspoll zurück
	 *
	 * @return Die neue Id
	 * @throws ProtocolException	wenn keine Adresse mehr frei
	 */
	public BotID generateBotId() throws ProtocolException;

	/**
	 * Testet, ob bereits ein Bot diese Id hat
	 *
	 * @param id	zu testende Id
	 * @return True, wenn noch kein Bot diese Id nutzt
	 */
	public boolean isIdFree(BotID id);

	/**
	 * Exportiert die aktuelle Welt in eine Bot-Map-Datei
	 *
	 * @param bot		Bot-Nr., dessen Startfeld als Koordinatenursprung der Map benutzt wird
	 * @param free		Wert, mit dem freie Felder eingetragen werden (z.B. 100)
	 * @param occupied	Wert, mit dem Hindernisse eingetragen werden (z.B. -100)
	 * @throws IOException	falls Fehler beim Schreiben der Datei
	 * @throws MapException	falls keine Daten in der Map
	 */
	public void worldToMap(int bot, int free, int occupied) throws IOException, MapException;

	/**
	 * gibt die aktuelle Welt zurück
	 *
	 * @return geladenen Welt
	 */
	public World getWorld();
}
