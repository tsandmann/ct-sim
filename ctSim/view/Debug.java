package ctSim.view;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Debug {
	
	public static final Debug out = new Debug();
	
	private final String LOG_FILE = "debug.txt";
	
	private final String TIME_PREFIX = "[HH:mm:ss] "; 
	
	private BufferedWriter bw;
	
	private DebugWindow win;
	
	private DateFormat timeFormatter;
	
	private boolean isNewLine = true;
	
	Debug() {
		
		this.timeFormatter = new SimpleDateFormat(TIME_PREFIX);
		
		try {
			File file = new File(LOG_FILE);
			
			FileWriter fw = new FileWriter(file);
			
			this.bw = new BufferedWriter(fw);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void setDebugWindow(DebugWindow win) {
		
		this.win = win;
	}
	
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(!str.endsWith("\n"))
			this.isNewLine = false;
	}
	
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		this.isNewLine = true;
	}
	
	protected void finalize() throws Throwable {
		
		try {
			this.bw.flush();
			this.bw.close();
		} finally {
			super.finalize();
		}
	}
	
	public static void registerDebugWindow(DebugWindow win) {
		
		Debug.out.setDebugWindow(win);
	}
}
