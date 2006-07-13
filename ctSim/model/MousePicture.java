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
		
		this.dimX = dimx;
		this.dimY = dimy;
		this.complete= false;
		this.data = new int[this.dimX * this.dimY];
	}

	/**
	 * betrachtet ein Byte als unsigned Int
	 * @param value Das Byte
	 * @return Der vorzeichenlose int-Wert
	 */
	private static int toUnsignedInt(byte value){
		return (value & 0x7F) + (value < 0 ? 128 : 0);
	}
	
	/**
	 * Fuegt Daten zum Maussensor-Bild hinzu
	 * @param start Nummer des ersten Pixels in data
	 * @param in Nutzdaten
	 */
	public void addPixels(int start, byte[] in){
		int i;
		if (start==1)	// Anfang des Frames ?
			this.complete = false;
		
		for (i=0; i< in.length; i++){
			int tmp= toUnsignedInt(in[i]);
			tmp &=0x3F;	// Statusinfos ausblenden
			tmp = tmp << 2;	// verbleibende Bits auf den normalen Farbraum ausdehnen
			
			
			this.data[i+start-1]=tmp ;
			this.data[i+start-1] += tmp <<8; 
			this.data[i+start-1] += tmp <<16 ;
			this.data[i+start-1] += 255 <<24 ;
			
//			System.out.println("tmp= "+tmp);
		}
		// Genug Daten empfangen?
		if ((i+start-1) == (this.dimX*this.dimY)) {
			this.complete=true;
			System.out.println("Bild vollstaendig"); //$NON-NLS-1$
		}
	}

	/**
	 * @return Gibt den Wert von dimX zurueck
	 */
	public int getDimX() {
		return this.dimX;
	}


	/**
	 * @return Gibt den Wert von dimY zurueck
	 */
	public int getDimY() {
		return this.dimY;
	}


	/**
	 * @return Gibt den Wert von complete zurueck
	 */
	public boolean isComplete() {
		return this.complete;
	}
	

	/**
	 * Liefert das Mausbild als Image zurueck
	 * @param width gewuenschte Breite
	 * @param height gewuenschte Hoehe
	 * @return Das Bild
	 */
	public Image getImage(int width, int height){
		int w = this.dimX;
		int h = this.dimY;
		int pix[] = new int[w * h];
		int index = 0;
		for (int y = 0; y < h; y++) {
		    int red = (y * 255) / (h - 1);
		    for (int x = 0; x < w; x++) {
			int blue = (x * 255) / (w - 1);
			pix[index++] = (255 << 24) | (red << 16) | blue;
		    }
		}
		
		
		
		
		
		
		
		Image image = Toolkit.getDefaultToolkit().createImage(
		                new MemoryImageSource(this.dimX, this.dimY,
		                this.data, 0, this.dimX));
	//     	            pix, 0, dimX));
		
	    ImageFilter replicate = new ReplicateScaleFilter
	           (width, height);
	      ImageProducer prod = 
	         new FilteredImageSource(image.getSource(),replicate);
	    return Toolkit.getDefaultToolkit().createImage(prod);
		
//		return image;
	}

}
