/**
 * 
 */
package ctSim.Model;

import javax.media.j3d.Bounds;

/**
 * Ein Hindernis, in der Welt der Bots 
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */

public interface Obstacle {
	
	/**
	 *  Liefert die Bounds des Hindernisses zurück 
	 *  @return Die Bounds
	 */
	abstract public Bounds getBounds();
}
