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

package ctSim.Model;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.MemoryImageSource;
import java.awt.image.ReplicateScaleFilter;

/** Speichert ein Bild vom Maussensor 
 * @author Benjamin Benz
 */
public class MousePicture {

	/** Breite */
	private int dimX;

	/** Hoehe */
	private int dimY;
	
	/** Ist dieses Bild vollstaendig */
	private boolean complete;
	
	/** Hier liegen die Rohdaten des Maussensors */
	private int[] data;
	
	
	/**
	 * Erzeugt ein neues, leeres Bild
	 * @param dimx
	 * @param dimy
	 */
	public MousePicture(int dimx, int dimy) {
		super();
		
		dimX = dimx;
		dimY = dimy;
		complete= false;
		data = new int[dimX * dimY];
	}

	/**
	 * Fuegt Daten zum Maussensor-Bild hinzu
	 * @param start Nummer des ersten Pixels in data
	 * @param data Nutzdaten
	 */
	public void addPixels(int start, byte[] data){
		int i;
		if (start==1)	// Anfang des Frames ?
			complete = false;
		
		for (i=0; i< data.length; i++)
			this.data[i+start-1]=data[i];
		
		// Genug Daten empfangen?
		if ((i+start-1) == (dimX*dimY))
			complete=true;
	}

	/**
	 * @return Gibt eine Referenz auf dimX zurueck
	 * @return Gibt den Wert von dimX zurueck
	 */
	public int getDimX() {
		return dimX;
	}


	/**
	 * @return Gibt eine Referenz auf dimY zurueck
	 * @return Gibt den Wert von dimY zurueck
	 */
	public int getDimY() {
		return dimY;
	}


	/**
	 * @return Gibt eine Referenz auf complete zurueck
	 * @return Gibt den Wert von complete zurueck
	 */
	public boolean isComplete() {
		return complete;
	}
	

	/**
	 * Liefert das Mausbild als Image zurueck
	 * @param width gewuenschte Breite
	 * @param height gewuenschte Hoehe
	 * @return
	 */
	public Image getImage(int width, int height){
		Image image = Toolkit.getDefaultToolkit().createImage(
		                new MemoryImageSource(dimX, dimY,
		                data, 0, dimX));
		
	    ImageFilter replicate = new ReplicateScaleFilter
	           (width, height);
	      ImageProducer prod = 
	         new FilteredImageSource(image.getSource(),replicate);
	    return Toolkit.getDefaultToolkit().createImage(prod);
		
//		return image;
	}

}
