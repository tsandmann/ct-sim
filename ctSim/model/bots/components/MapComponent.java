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

package ctSim.model.bots.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.vecmath.Point3i;

import ctSim.controller.Config;
import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.util.FmtLogger;
import ctSim.util.MapCircles;
import ctSim.util.MapLines;
import ctSim.util.Misc;

/**
 * Map-Repraesentation im Sim
 * <ul><li>Command-Code MAP</li>
 * <li>Nutzlast: Ein Block der Map-Rohdaten</li>
 * </ul>
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class MapComponent extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
	/** Breite der Map in Pixeln */	
	private final int WIDTH;
	/** Hoehe der Map in Pixeln */
	private final int HEIGHT;
	/** Groesse einer Sektion */
	private final int SECTION_SIZE;
	/** Groesse eines Makroblocks */
	private final int MAKROBLOCK_SIZE;
	/** Rohe Mapdaten; ein Int pro Pixel. */
	private final int[] pixels;
	/** eingezeichnete Linien */
	private List<MapLines> lines = Misc.newList();
	/** Mutex fuer lines */
	private final Object linesMutex = new Object();
	/** eingezeichnete Kreise */
	private List<MapCircles> circles = Misc.newList();
	/** Mutex fuer circles */
	private final Object circlesMutex = new Object();
	/** aktuelle Botposition (z fuer heading) */
	private final Point3i botPos;
	/** SyncRequest austehend? */
	private boolean syncRequestPending = false;
	/** Image-Listener */
	private final List<Runnable> imageLi = Misc.newList();
	/** Image-Event austehend? */
	private boolean imageEventPending = false;
	/** async. Outputstream */
	private CommandOutputStream asyncOut;
	/** Logger */
	private final FmtLogger lg = FmtLogger.getLogger("ctSim.model.bots.components.MapComponent");

	/** Empfangsstatus (0: noch keine Daten, 1: 1 Teil empfangen, 2: 2. Teil empfangen, 3: 3. Teil empfangen */
	private int receiveState = 0;
	/** Adresse des letzten empfangenen Blocks (muss fuer alle Teilbloecke gleich sein) */
	private int lastBlock = 0;
	/** Kleinste belegte X-Koordinate */
	private int min_x = 0xffffff;
	/** Kleinste belegte Y-Koordinate */
	private int min_y = 0xffffff;
	/** Groesste belegte X-Koordinate */
	private int max_x = 0;
	/** Groesste belegte Y-Kooridnate */
	private int max_y = 0;
	
	/** Zeitpunkt des letzten Bild-Updates */
	private long lastUpdate = 0;
	/** Intervall [ms], mit dem die Anzeige aktualisiert wird */
	private final int updateIntervall;
	/** Zuordnung pixel-Array <-> image */
	private final MemoryImageSource memImage;
	/** Image-Objekt fuer das Map-Bild */
	private final Image image;
	
	/**
	 * Map-Komponente
	 */
	public MapComponent() { 
		super(null);
		
		String size_str = Config.getValue("mapSize");
		float size = 0.0f;
		try {
			size = Float.parseFloat(size_str);
		} catch (NumberFormatException exc) {
			lg.warning(exc, "Problem beim Parsen der Konfiguration: " +
				"Parameter 'mapSize' ist keine gueltige Map-Groesse!");
		}
		String resolution_str = Config.getValue("mapResolution");
		int resolution = 0;
		try {
			resolution = Integer.parseInt(resolution_str);
		} catch (NumberFormatException exc) {
			lg.warning(exc, "Problem beim Parsen der Konfiguration: " +
				"Parameter 'mapResolution' ist keine gueltige Map-Groesse!");
		}
		size *= resolution;
		
		String section_size_str = Config.getValue("mapSectionSize");
		int section_size = 0;
		try {
			section_size = Integer.parseInt(section_size_str);
		} catch (NumberFormatException exc) {
			lg.warning(exc, "Problem beim Parsen der Konfiguration: " +
				"Parameter 'mapSectionSize' ist keine gueltige Sektionsgroesse!");
		}
		
		String makroblock_size_str = Config.getValue("mapMacroblockSize");
		int makroblock_size = 0;
		try {
			makroblock_size = Integer.parseInt(makroblock_size_str);
		} catch (NumberFormatException exc) {
			lg.warning(exc, "Problem beim Parsen der Konfiguration: " +
				"Parameter 'mapMacroblockSize' ist keine gueltige Sektionsgroesse!");
		}
		
		WIDTH = (int) size;
		HEIGHT = (int) size;
		SECTION_SIZE = section_size;
		MAKROBLOCK_SIZE = makroblock_size;
		lg.fine("Map-Paramter: WIDTH=" + WIDTH + " HEIGHT=" + HEIGHT + " SECTION_SIZE=" + SECTION_SIZE + " MAKROBLOCK_SIZE=" + MAKROBLOCK_SIZE);
		
		pixels = new int[WIDTH * HEIGHT];
		botPos = new Point3i(WIDTH / 2, HEIGHT / 2, 0);
		
		int color = colorFromRgb(128, 128, 128); // Map-Wert 0
		for (int i=0; i<pixels.length; i++) {
			pixels[i] = color;
		}
		int tmp = 500;
        try {
        	tmp = Integer.parseInt(Config.getValue("MapUpdateIntervall"));
        } catch(NumberFormatException exc) {
            lg.warning(exc, "Problem beim Parsen der Konfiguration: " +
                    "Parameter 'MapUpdateIntervall' ist keine Ganzzahl");
        } finally {
        	updateIntervall = tmp;
        }
        lg.fine("MapUpdateIntervall=" + updateIntervall + " ms");
        
		memImage = new MemoryImageSource(WIDTH, HEIGHT, pixels, 0, WIDTH);
		memImage.setAnimated(true);
		image = Toolkit.getDefaultToolkit().createImage(memImage);
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanWriteAsynchronously#setAsyncWriteStream(ctSim.model.CommandOutputStream)
	 */
	public void setAsyncWriteStream(CommandOutputStream s) {
		asyncOut = s;
	}

	/**
	 * Map anfordern
	 * @throws IOException
	 */
	public synchronized void requestMap() throws IOException {
		if (writesAsynchronously()) {
			synchronized (asyncOut) {
				asyncOut.getCommand(getHotCmdCode()).setSubCmdCode(Command.SubCode.MAP_REQUEST);
				asyncOut.flush();
			}
		} else {
			syncRequestPending = true;
		}
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#askForWrite(ctSim.model.CommandOutputStream)
	 */
	@Override
	public synchronized void askForWrite(CommandOutputStream s) {
		if (! writesSynchronously()) {
			return;
		}
		if (syncRequestPending) {
			s.getCommand(getHotCmdCode()).setSubCmdCode(Command.SubCode.MAP_REQUEST);
			syncRequestPending = false;
		}
	}

	/**
	 * <p>
	 * Konvertiert ein RGB-Wertetripel in nen Integer, wie er im Array
	 * {@link #pixels} sein muss. Format ist etwas undurchsichtig:
	 * <ul>
	 * <li>Es handelt sich um eine 4-Komponenten-Farbe (Alpha, Rot, Gr&uuml;n,
	 * Blau in dieser Reihenfolge)</li>
	 * <li>8 Bit pro Komponente ([0; 255]) = 32 Bit pro Pixel</li>
	 * <li><strong>Die 32 Bit sind in <em>einen</em> Integer gestopft.</strong>
	 * (Integer in Java: 32 Bit lang.)
	 * <ul>
	 * <li>Alpha-Wert = die 8 h&ouml;chstwertigen Bits (MSBs), also Bits
	 * 24&ndash;32</li>
	 * <li>usw.</li>
	 * <li>Blau-Wert = die 8 niedrigstwertigen Bits (LSBs), also Bits 0&ndash;8
	 * </li>
	 * </ul>
	 * </li>
	 * <li>Details siehe {@link ColorModel#getRGBdefault()}</li>
	 * </ul>
	 * </p>
	 * <p>
	 * Alpha setzt diese Methode immer auf 255 (voll deckend).
	 * </p>
	 * <p>
	 * Sind die &uuml;bergebenen Parameter au&szlig;erhalb des Wertebereichs [0;
	 * 255], wird geclampt (255 wenn zu gro&szlig;, 0 wenn zu klein).
	 * </p>
	 * @param r rot
	 * @param g gruen
	 * @param b blau
	 * @return Farbe
	 */
	private final int colorFromRgb(int r, int g, int b) {
		r = Misc.clamp(r, 255);
		g = Misc.clamp(g, 255);
		b = Misc.clamp(b, 255);
		// Alpha volle Pulle, die anderen wie als Parameter uebergeben
		return 255 << 24 | r << 16 | g << 8 | b;
	}
	
	/**
	 * Uebertraegt die empfangenen Daten in das Pixel-Array. 
	 * Die Koordinaten werden dabei gemaess der Map-Parameter aus der Blockadresse berechnet.
	 * @param data	Map-Rohdaten
	 * @param block	Blockadresse der Daten
	 * @param from	Startindex der Daten
	 * @param to	Endindex der Daten
	 */
	private final void updateInternalModel(byte[] data, int block, int from, int to) {
		/* Monsters here... */
		int x = ((block * (SECTION_SIZE * 2)) % MAKROBLOCK_SIZE + (block / MAKROBLOCK_SIZE) * MAKROBLOCK_SIZE) % WIDTH; // 2 sections pro Block in X-Richtung (nach Map-Orientierung)
		int y = (((block / SECTION_SIZE) * SECTION_SIZE) % MAKROBLOCK_SIZE) + (block / HEIGHT) * MAKROBLOCK_SIZE; // 1 section pro Block in Y-Richtung (nach Map-Orientierung)
		
//TODO:	Karte je nach Startausrichtung des Bots entsprechend drehen, derzeit wird von Startrichtung == Norden ausgegangen		
		/* neu empfangene Daten ins Map-Array kopieren */
		int pic_x = 0, pic_y = 0;
		int bufferIndex = 0;
		for (int j=from; j<=to; j++) {	// Zeilen
			pic_y = x + j; // X der Map ist Y beim Sim
			pic_y = HEIGHT - pic_y;	// Karte wird um 180 Grad gedreht, denn (0|0) ist hier "oben links"
			int row_offset = pic_y * WIDTH;
			for (int i=0; i<SECTION_SIZE; i++) {	// Spalten
				pic_x = y + i;	// Spaltenindex im Block berechnen, Y der Map ist X beim Sim
				pic_x = WIDTH - pic_x;	// Karte wird um 180 Grad gedreht, denn (0|0) ist hier "oben links"
				if (pic_x >= WIDTH || pic_y >= HEIGHT) {
					/* ungueltige Daten :( */
					lg.warn("(pic_x=" + pic_x + " | pic_y=" + pic_y + ") liegt ausserhalb der Karte! Breche Map-Update ab.");
					return;
				}
				/* Grauwert von int8_t nach int umrechnen */
				int gray = data[bufferIndex++];
				if (gray > 128) {
					/* negativer Wert (Hindernis) ==> dunkler */
					gray = 255 - gray;
				} else {
					/* positiver Wert (frei) ==> heller */
					gray += 128;
				}
				pixels[pic_x + row_offset] = colorFromRgb(gray, gray, gray);
			}
		}
		pic_x &= ~(SECTION_SIZE - 1); // Koordinaten innerhalb des Blocks ausblenden ==> Eckpunkt mit kleinsten Koordinaten
		pic_y &= ~(SECTION_SIZE * 2 - 1); // 2 sections pro Block in X-Richtung (Map-Orientierung) entspricht Y-Richtung (Sim-Orientierung)
		if (pic_x < min_x) {
			min_x = pic_x;
		} else if (pic_x > max_x) {
			max_x = pic_x;
		}
		if (pic_y < min_y) {
			min_y = pic_y;
		} else if (pic_y > max_y) {
			max_y = pic_y;
		}
	}
	
	/**
	 * Traegt uebertragene Zeichnungsdaten einer Linie in die interne Datenstruktur ein
	 * @param color	Farbe der Linie
	 * @param data	Rohdaten vom Kommando
	 */
	private final void updateDrawingsLine(int color, byte[] data) {
		int y1 = WIDTH - (Misc.toUnsignedInt8(data[0]) | Misc.toUnsignedInt8(data[1]) << 8);
		int x1 = HEIGHT - (Misc.toUnsignedInt8(data[2]) | Misc.toUnsignedInt8(data[3]) << 8);
		int y2 = WIDTH - (Misc.toUnsignedInt8(data[4]) | Misc.toUnsignedInt8(data[5]) << 8);
		int x2 = HEIGHT - (Misc.toUnsignedInt8(data[6]) | Misc.toUnsignedInt8(data[7]) << 8);
		synchronized (linesMutex) {
			lines.add(new MapLines(x1, y1, x2, y2, color));
		}
		lg.finer("Linie von (" + x1 + "|" + y1 + ") bis (" + x2 + "|" + y2 + ")");
	}
	
	/**
	 * Traegt uebertragene Zeichnungsdaten eines Kreises in die interne Datenstruktur ein
	 * @param color	Farbe der Kreislinie
	 * @param radius Radius des Kreises
	 * @param data	Rohdaten vom Kommando
	 */
	private final void updateDrawingsCircle(int color, int radius, byte[] data) {
		int y = WIDTH - (Misc.toUnsignedInt8(data[0]) | Misc.toUnsignedInt8(data[1]) << 8);
		int x = HEIGHT - (Misc.toUnsignedInt8(data[2]) | Misc.toUnsignedInt8(data[3]) << 8);
		synchronized (circlesMutex) {
			circles.add(new MapCircles(x, y, radius, color));
		}
		lg.finer("Kreis mit Radius " + radius + " an (" + x + "|" + y + ")");
	}
	
	/**
	 * Wertet ein Map-Kommando aus
	 * @param c Command
	 */
	public synchronized void readFrom(Command c) {
		if (! c.has(getHotCmdCode())) {
			return;
		}

		int block = c.getDataL();	// 16 Bit Adresse des Map-Blocks
		
		/* SubCode auswerten, ein Block wird in vier Teilen uebertragen. 
		 * Alle Teile muessen dieselbe Blockadresse in DataL mitfuehren! */
		Command.SubCode sub = c.getSubCode();
		if (sub.equals(Command.SubCode.MAP_DATA_1)) {
			if (receiveState != 0) {
				lg.warn("Abbruch bei SubCode MAP_DATA_1:");
				lg.warn(" State=:" + receiveState + "/soll:0");
				lg.warn("  block=" + block);
				receiveState = 0;
				return;
			}
			botPos.y = HEIGHT - c.getDataR();	// Bot-Position, X-Komponente, wird im Bild in Y-Richtung gezaehlt
			updateInternalModel(c.getPayload(), block, 0, 7);	// macht die eigentliche Arbeit
			receiveState = 1;
			lastBlock = block;
		} else if (sub.equals(Command.SubCode.MAP_DATA_2)) {
			if (receiveState != 1 || lastBlock != block) {
				lg.warn("Abbruch bei SubCode MAP_DATA_2:");
				lg.warn(" State=" + receiveState + "/soll:1");
				lg.warn("  block=" + block + "; last_block=" + lastBlock);
				receiveState = 0;
				return;
			}
			botPos.x = WIDTH - c.getDataR();	// Bot-Position, Y-Komponente, wird im Bild in X-Richtung gezaehlt
			updateInternalModel(c.getPayload(), block, 8, 15);	// macht die eigentliche Arbeit
			receiveState = 2;
		} else if (sub.equals(Command.SubCode.MAP_DATA_3)) {
			if (receiveState != 2 || lastBlock != block) {
				lg.warn("Abbruch bei SubCode MAP_DATA_3:");
				lg.warn(" State=" + receiveState + "/soll:2");
				lg.warn("  block=" + block + "; last_block=" + lastBlock);
				receiveState = 0;
				return;
			}
			botPos.z = c.getDataR();
			updateInternalModel(c.getPayload(), block, 16, 23);	// macht die eigentliche Arbeit
			receiveState = 3;
		} else if (sub.equals(Command.SubCode.MAP_DATA_4)) {
			if (receiveState != 3 || lastBlock != block) {
				lg.warn("Abbruch bei SubCode MAP_DATA_4:");
				lg.warn(" State=" + receiveState + "/soll:3");
				lg.warn("  block=" + block + "; last_block=" + lastBlock);
				receiveState = 0;
				return;
			}
			// DataR ist nicht belegt
			updateInternalModel(c.getPayload(), block, 24, 31);	// macht die eigentliche Arbeit
			receiveState = 0;
			
			/* GUI-Update freigeben */
			imageEventPending = true;
		} else if (sub.equals(Command.SubCode.MAP_LINE)) {
			updateDrawingsLine(block, c.getPayload());
			imageEventPending = true;
		} else if (sub.equals(Command.SubCode.MAP_CIRCLE)) {
			updateDrawingsCircle(block, c.getDataR(), c.getPayload());
			imageEventPending = true;
		} else if (sub.equals(Command.SubCode.MAP_CLEAR_LINES)) {
			synchronized (linesMutex) {
				int size = lines.size();
				int n = c.getDataL() > size ? size : size - c.getDataL();
				lines.subList(0, n).clear();
			}
			imageEventPending = true;
		} else if (sub.equals(Command.SubCode.MAP_CLEAR_CIRCLES)) {
			synchronized (circlesMutex) {
				int size = circles.size();
				int n = c.getDataL() > size ? size : size - c.getDataL();
				circles.subList(0, n).clear();
			}
			imageEventPending = true;
		} else {
			lg.warn("Abbruch, ungueltiger SubCode");
			receiveState = 0;
		}
	}

	/**
	 * No-op: Wir implementieren die, weil wir laut Interface m&uuml;ssen, aber
	 * wir brauchen die nicht weil wir ja
	 * {@link #askForWrite(CommandOutputStream) askForWrite()}
	 * &uuml;berschrieben haben.
	 * @param c Command
	 */
	public void writeTo(Command c) { 
		/* No-op */ 
	}

	/**
	 * Kartenbreite (X-Richtung)
	 * @return Breite
	 */
	public int getWidth() { return WIDTH; }
	
	/**
	 * Kartenhoehe (Y-Richtung)
	 * @return Hoehe
	 */
	public int getHeight() { return HEIGHT; }
	
	/**
	 * @return Image-Referenz
	 */
	public Image getImg() { return image; }
	
	/**
	 * @return MapLines-Referenz
	 */
	public List<MapLines> getMapLines() { return lines; }
	
	/**
	 * @return MapCircles-Referenz
	 */
	public List<MapCircles> getMapCircles() { return circles; }
	
	/** 
	 * @return this.linesMutes
	 */
	public Object getLinesMutex() { return linesMutex; }
	
	/**
	 * @return this.circlesMutex
	 */
	public Object getCirclesMutex() { return circlesMutex; }
	
	/**
	 * @return this.botPos
	 */
	public Point3i getBotPos() { return botPos; }
	
	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
	 */
	public Code getHotCmdCode() { return Code.MAP; }
	
	/**
	 * @see ctSim.model.bots.components.BotComponent#getName()
	 */
	@Override public String getName() { return "Map"; }

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Die aktuelle Karte";
	}

	/**
	 * Fuegt einen Listener hinzu, der ausgefuehrt wird, wenn sich die Karte veraendert hat 
	 * @param li Listener
	 */
	public void addImageListener(Runnable li) {
		if (li == null) {
			throw new NullPointerException();
		}
		imageLi.add(li);
	}
	
	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public synchronized void updateExternalModel() {
		if (imageEventPending) {
			/* Bild max. alle updateIntervall ms neu zeichnen */
			long now = System.currentTimeMillis();
			if (now > lastUpdate + updateIntervall) {
				lastUpdate = now;
	
				imageEventPending = false;
				memImage.newPixels();
				
				for (Runnable li : imageLi) {
					li.run();
				}
			}
		}
	}
	
	/**
	 * Speichert die Karte als png-Bild
	 * @param file Dateiname
	 * @throws IOException 
	 */
	public void saveImage(File file) throws IOException {		
		/* Grosse der Map berechnen */
		int width = max_x + (SECTION_SIZE - 1) - min_x + 1;
		int height = max_y + (SECTION_SIZE * 2 - 1) - min_y + 1;
		
		/* belegten Teil in neues Bild kopieren */
		BufferedImage bimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = bimg.createGraphics();
		g.setColor(new Color(128, 128, 128, 255)); // "Map-Wert 0"
		g.fillRect(0, 0, width, height);
		
		/* Pixel kopieren */
		int[] map = new int[width * height];
		for (int y=min_y+1; y<=max_y+SECTION_SIZE*2; y++) { // Zeilen
			System.arraycopy(pixels, min_x + 1 + y * WIDTH, map, (y - min_y - 1) * width, width); // alle Spalten einer Zeile
		}
		
		/* Bild zeichnen und als png speichern */
		g.drawImage(Toolkit.getDefaultToolkit().createImage(
			new MemoryImageSource(width, height, map, 0, width)), 0, 0, null);
		g.dispose();
		ImageIO.write(bimg, "png", file);
	}
}
