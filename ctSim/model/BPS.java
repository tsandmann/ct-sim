/*
 * c't-Sim - Robotersimulator fuer den c't-Bot
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

package ctSim.model;

import javax.vecmath.Point2f;
import javax.vecmath.Point2i;
import javax.vecmath.Point3d;

/**
 * Repraesentiert das Bot Positioning System (BPS) im Simulator
 * @author Timo Sandmann
 */
public class BPS {
	/** Z-Koordinate der IR-LED einer Landmarke */
	public static final float BPSZ = 0.1f;
	
	/** Raster-Breite fuer die Baken [mm] */
	private static final float BEACON_GRID_SIZE = 360.0f;
	
	/**
	 *
	 */
	public static class Beacon {	
		/** Position der Landmarke [BEACON_GRID_SIZE mm] */
		private final Point2f position;
		/** Block-Groesse des Parcours, in dem die Landmarke steht [mm] */
		private final float parcoursBlockSizeInMM;
		
		/**
		 * Erzeugt eine neue Landmarke
		 * @param parc Parcours, in dem die Landmarke steht
		 * @param beacon Position der Landmarke, so wie PickInfo sie ermittelt hat [m]
		 */
		public Beacon(Parcours parc, Point3d beacon) {
			float x = (float)beacon.y * (1000.0f / BEACON_GRID_SIZE);
			float y = ((parc.getWidthInM() - (float)beacon.x) * 1000.0f) / BEACON_GRID_SIZE;
			this.position = new Point2f(x, y);
			this.parcoursBlockSizeInMM = parc.getBlockSizeInMM();
		}
		
		/**
		 * @return Landmarken ID in 10 Bit-Codierung
		 */
		public int getID() {
			int x_int = Math.round(this.position.x);
			int y_int = Math.round(this.position.y);
			return (y_int & 0x1f) | ((x_int & 0x1f) << 5);
		}
		
		/**
		 * Errechnet die Parcours-Block-Koordinaten der Landmarke 
		 * @return Position in Parcours-Block-Koordinaten [parcoursBlockSizeInMM mm]
		 */
		private Point2i getPositionInBlocks() {
			int x = Math.round((this.position.x * BEACON_GRID_SIZE - this.parcoursBlockSizeInMM / 2.0f) / this.parcoursBlockSizeInMM);
			int y = Math.round((this.position.y * BEACON_GRID_SIZE - this.parcoursBlockSizeInMM / 2.0f) / this.parcoursBlockSizeInMM);
			return new Point2i(x, y);
		}
		
		/**
		 * Gibt die Position der Landmarke als String aus [BEACON_GRID_SIZE mm]
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
//			System.out.println("(" + this.position.x + "|" + this.position.y + ")");
			return "(" + Math.round(this.position.x) + "|" + Math.round(this.position.y) + ")";
		}
		
		/**
		 * Gibt die Position der Landmarke als String aus [Parcours-Block-Koordinaten]
		 * @return (X|Y) [Parcours-Block-Koordinaten]
		 * @see #toString()
		 */
		public String toStringInBlocks() {
			return "(" + getPositionInBlocks().x + "|" + getPositionInBlocks().y + ")";
		}
		
		/**
		 * Prueft, ob die angegebene Parcours-Position fuer eine BPS-Landmarke gueltig ist
		 * @param parc Parcours, in dem die Landmarke steht
		 * @param x X-Koordinate [Parcours-Block]
		 * @param y Y-Koordinate [Parcours-Block]
		 * @return true, falls Landmarke an (x|y) moeglich, sonst false
		 */
		public static boolean checkParcoursPosition(Parcours parc, int x, int y) {
			float tmp = (parc.getWidthInBlocks() - 1 - x) * parc.getBlockSizeInMM() + parc.getBlockSizeInMM() / 2.0f;
			if (tmp % BEACON_GRID_SIZE != 0) {
				System.out.println("x=" + x + " ungueltig, tmp=" + tmp);
				return false;
			}
			tmp = y * parc.getBlockSizeInMM() + parc.getBlockSizeInMM() / 2.0f;
			if (tmp % BEACON_GRID_SIZE != 0) {
				System.out.println("y=" + y + " ungueltig, tmp=" + tmp);
				return false;
			}
			return true;
		}
	}
}
