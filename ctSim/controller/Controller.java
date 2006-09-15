package ctSim.controller;

import java.io.File;

import ctSim.view.View;

//$$ doc alle Methoden
/** Das Interface, das die Views der Applikation verwenden, um auf den
 * Controller zuzugreifen. "View" und "Controller" sind dabei im Sinne von 
 * <a href="http://en.wikipedia.org/wiki/Model-view-controller">MVC</a> zu
 * verstehen.
 * 
 * @see View
 * 
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public interface Controller {
	public void setView(View view);
	public void closeWorld();

	/**
	 * Startet einen externen Bot
	 * @param filename Pfad zum Binary des Bots
	 */
	public void invokeBot(String filename);
	public void invokeBot(File file);

	public void openWorldFromFile(File sourceFile);
	public void openWorldFromXmlString(String parcoursAsXml);
	public void openRandomWorld();

	public void setJudge(String judgeClassName);
	public String getJudge();
	
	//$$ Vorlaeufige Methode
	public void pause();
	//$$ Vorlaeufige Methode
	public void unpause();
	//$$ Vorlaeufige Methode
	public void stop();
	//$$ Vorlaeufige Methode
	public void reset();
	//$$ Vorlaeufige Methode
	public void addTestBot();
}