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

package ctSim.view.gui;

import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Sorgt fuer Debug-Ausgaben
 * @author Felix Beckwermert
 */
public class Debug {
	
	/** Output */
	public static final Debug out = new Debug();
	/** Log-File */
	@SuppressWarnings("unused")
	private final String LOG_FILE = "debug.txt";
	/** Zeitformat */
	private final String TIME_PREFIX = "[HH:mm:ss] ";
	/** Writer */
	private BufferedWriter bw;
	/** Fenster */
	private DebugWindow win;
	/** Datumsformat */
	private DateFormat timeFormatter;
	/** Neue Zeile? */
	private boolean isNewLine = true;
	
	/**
	 * Der Konstruktor
	 */
	Debug() {
		
		this.timeFormatter = new SimpleDateFormat(this.TIME_PREFIX);
		
//		try {
//			File file = new File(this.LOG_FILE);
//			
//			FileWriter fw = new FileWriter(file);
//			
//			this.bw = new BufferedWriter(fw);
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	/**
	 * Setzt das Debug-Fenster
	 * @param window
	 */
	private void setDebugWindow(DebugWindow window) {
		this.win = window;
	}
	
	/**
	 * @param str
	 */
	public synchronized void print(String str) {
		
		if(this.isNewLine)
			str = this.timeFormatter.format(new Date()) + str;
		
		System.out.print(str);
		
		if(this.win != null)
			this.win.print(str);
		
		if(this.bw != null) {
			try {
				this.bw.write(str);
				this.bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(!str.endsWith("\n")) //$NON-NLS-1$
			this.isNewLine = false;
	}
	
	/**
	 * @param str
	 */
	public synchronized void println(String str) {
		
		str = this.timeFormatter.format(new Date()) + str;
		
		System.out.println(str);
		
		if(this.win != null) {
			this.win.println(str);
		}
		
		if(this.bw != null) {
			try {
				this.bw.write(str);
				this.bw.newLine();
				this.bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		this.isNewLine = true;
	}
	
	/**
	 * @see java.lang.Object#finalize()
	 */
	@SuppressWarnings({ "deprecation", "javadoc" })
	@Override
	protected void finalize() throws Throwable {
		
		try {
			this.bw.flush();
			this.bw.close();
		} finally {
			super.finalize();
		}
	}
	
	/**
	 * @param win
	 */
	public static void registerDebugWindow(DebugWindow win) {
		
		Debug.out.setDebugWindow(win);
	}
}
