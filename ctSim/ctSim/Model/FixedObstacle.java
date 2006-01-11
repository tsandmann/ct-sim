/**
 * 
 */
package ctSim.Model;

import javax.media.j3d.*;


/**
 * Ein festes Hindernis in der Welt der Bots.
 * @author Benjamin Benz (bbe@ctmagazin.de)
 */

public class FixedObstacle implements Obstacle {

	/** Die Grenzen des Objektes */
	private Bounds bounds; 
	
	/**
	 * Konstruktor 
	 */
	public FixedObstacle (){
		bounds = null;
	}
	
	/**
	 *  Liefert die Bounds des Hindernisses zurück 
	 *  @return Die Bounds
	 */
	public Bounds getBounds() {
		return bounds;
	}

	/**
	 * @param bounds The bounds to set.
	 */
	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}
}
