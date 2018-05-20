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

package ctSim.util;

/**
 * Runnable-Interface
 * 
 * @param <T1>	Typ 1
 * @param <T2>	Typ 2
 * @param <T3>	Typ 3
 */
public interface Runnable3<T1, T2, T3> extends java.util.EventListener {
	/**
	 * @param argument1
	 * @param argument2
	 * @param argument3
	 */
	public void run(T1 argument1, T2 argument2, T3 argument3);
}
