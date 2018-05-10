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

package ctSim.model;

import javax.vecmath.Point2i;
import javax.vecmath.Point3d;

/**
 * Repräsentiert das Bot Positioning System (BPS) im Simulator
 *
 * @author Timo Sandmann
 */
public class BPS {
	/** Z-Koordinate der IR-LED einer Landmarke */
	public static final float BPSZ = 0.1f;

	/** Wert des Sensors, wenn keine Landmarke gesehen wird */
	public static final int NO_DATA = 0xffff;

	/** Eine Bake des BPS */
	public static class Beacon {
		/** Position der Landmarke [parcoursBlockSizeInMM mm] */
		private final Point2i position;
		/** Block-Größe des Parcours, in dem die Landmarke steht [mm] */
		private final int parcoursBlockSizeInMM;

		/**
		 * Erzeugt eine neue Landmarke
		 *
		 * @param parc Parcours, in dem die Landmarke steht
		 * @param source Position der Landmarke, so wie PickInfo sie ermittelt hat [m]
		 */
		public Beacon(Parcours parc, Point3d source) {
			parcoursBlockSizeInMM = parc.getBlockSizeInMM();
			position = parc.transformWorldposToGlobalpos(source);
		}

		/**
		 * @return Landmarken ID in 10 Bit-Codierung
		 */
		public int getID() {
			return (position.y & 0xff) | ((position.x & 0xff) << 8);
		}

		/**
		 * Gibt die Position der Landmarke als String aus [BEACON_GRID_SIZE mm]
		 *
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "(" + (position.x * parcoursBlockSizeInMM + parcoursBlockSizeInMM / 2) + "|" +
					(position.y	* parcoursBlockSizeInMM + parcoursBlockSizeInMM / 2) + ")";
		}

		/**
		 * Gibt die Position der Landmarke als String aus [Parcours-Block-Koordinaten]
		 *
		 * @return (X|Y) [Parcours-Block-Koordinaten]
		 * @see #toString()
		 */
		public String toStringInBlocks() {
			return "(" + position.x + "|" + position.y + ")";
		}

		// /**
		//  * Prüft, ob die angegebene Parcours-Position für eine BPS-Landmarke gueltig ist
		//
		//  * @param parc	Parcours, in dem die Landmarke steht
		//  * @param x		X-Koordinate [Parcours-Block]
		//  * @param y		Y-Koordinate [Parcours-Block]
		//  * @return true, falls Landmarke an (x|y) möglich, sonst false
		//  */
		// public static boolean checkParcoursPosition(Parcours parc, int x, int y) {
		// float tmp = (parc.getWidthInBlocks() - 1 - x) * parc.getBlockSizeInMM() + parc.getBlockSizeInMM() / 2.0f;
		// 		if (tmp % BEACON_GRID_SIZE != 0) {
		// 			System.out.println("x=" + x + " ungueltig, tmp=" + tmp);
		// 			return false;
		// 		}
		// 		tmp = y * parc.getBlockSizeInMM() + parc.getBlockSizeInMM() / 2.0f;
		// 		if (tmp % BEACON_GRID_SIZE != 0) {
		// 			System.out.println("y=" + y + " ungueltig, tmp=" + tmp);
		// 			return false;
		// 		}
		// 		return true;
		// }
	}
}
