package ctSim.view;

import ctSim.controller.Controller;
import ctSim.model.World;
import ctSim.model.bots.Bot;

//$$ doc alle Methoden
/** Interface, das der Controller verwendet, um auf die Views der Applikation
 * zuzugreifen. "Controller" und "View" sind dabei im Sinne von 
 * <a href="http://en.wikipedia.org/wiki/Model-view-controller">MVC</a> zu 
 * verstehen.
 * 
 * @see Controller
 * @see ViewYAdapter
 * 
 * @author Hendrik Krauss &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public interface View {
	public void update(long simTimeInMs);
	/** Aufzurufen, wenn sich die Welt &auml;ndert. 
	 * Schlie&szlig;t die alte Welt und zeigt die neue an.*/
	public void openWorld(World w);
	public void addBot(Bot bot);
	public void removeBot(Bot bot);
}
