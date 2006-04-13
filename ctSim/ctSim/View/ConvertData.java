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
package ctSim.View;

import java.io.IOException;
/**
 * Serialisieren und deserialisieren von Klassen
 * @author unbekannt
 *
 */
public class ConvertData{
 
	public static byte[] objectToBytes (Object o) throws IOException{
		java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream();
		java.io.ObjectOutputStream out = new java.io.ObjectOutputStream (bs);
		out.writeObject(o);
		out.close ();
		return bs.toByteArray();	
	}
 
	public static Object bytesToObject (byte bytes[]) throws IOException,ClassNotFoundException{
		Object o;
		java.io.ObjectInputStream in;
		java.io.ByteArrayInputStream bs;
		bs = new java.io.ByteArrayInputStream (bytes);
		in = new java.io.ObjectInputStream(bs);
		o = in.readObject ();
		in.close ();
		bs.close ();
		return o;
	}
}
