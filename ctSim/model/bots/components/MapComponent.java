package ctSim.model.bots.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.util.FmtLogger;
import ctSim.util.Misc;
import ctSim.util.Runnable2;

/**
 * Map-Repraesentation im Sim
 * <ul><li>Command-Code MAP</li>
 * <li>Nutzlast: Ein Block der Map-Rohdaten</li>
 * <li></li></ul>
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class MapComponent extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
//TODO:	Alle Map-spezifischen Parameter als Konstanten (oder in die Config-XML?)
	
	/** Breite der Map in Pixeln */
	private static final int WIDTH  = 1536;
	/** Hoehe der Map in Pixeln */
	private static final int HEIGHT = 1536;
	/** Rohe Mapdaten; ein Int pro Pixel. */
	private final int[] pixels = new int[WIDTH * HEIGHT];
	/** Mitte des Ausschnitts, der angezeigt werden soll (= aktuelle Botposition), Hoehe und Breite werden hier nicht benutzt */
	private final Rectangle center = new Rectangle(768, 768, 0, 0);
	/** SyncRequest austehend? */
	private boolean syncRequestPending = false;
	/** SyncFlush austehend? */
	private boolean syncFlushPending = false;
	/** Image-Listener */
	private final List<Runnable2<Image, Rectangle>> imageLi = Misc.newList();
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
	/** X-Komponente der aktuellen Bot-Position */
	private int bot_pos_x = 0;
	/** Y-Komponente der aktuellen Bot-Position */
	private int bot_pos_y = 0;
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
	
	/**
	 * Map-Komponente
	 */
	public MapComponent() { 
		super(null);
//TODO:	Hintergrundfarbe auf Map-Farbe fuer Wert 0 (so sieht man aber erstmal besser, wann ein Update ausgefuehrt wurde)		
//		for (int i=0; i<pixels.length; i++) {
//			pixels[i] = colorFromRgb(128, 128, 128);
//		}
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
				asyncOut.getCommand(getHotCmdCode());
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
		} else if (syncFlushPending) {
			s.getCommand(getHotCmdCode()).setSubCmdCode(Command.SubCode.MAP_FLUSH);
			syncFlushPending = false;			
		}
	}

//TODO: Auslagern, ist zur Methode vom Mausbild identisch
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
		/* Monsters here */
		int x = ((block * 32) % 512 + (block / 512) * 512) % 1536;
		int y = (((block / 16) * 16) % 512) + (block / 1536) * 512;
		
//TODO:	Karte je nach Startausrichtung des Bots entsprechend drehen, derzeit wird von Startrichtung == Norden ausgegangen		
		/* neu empfangene Daten ins Map-Array kopieren */
		int pic_x = 0, pic_y = 0;
		int bufferIndex = 0;
		for (int j=from; j<=to; j++) {	// Zeilen
			pic_y = x + j;
			pic_y = HEIGHT - pic_y;	// Karte wird um 180 Grad gedreht, denn (0|0) ist hier "oben links"
			int row_offset = pic_y * WIDTH;
			for (int i=0; i<16; i++) {	// Spalten
				pic_x = y + i;	// Spaltenindex im Block berechnen
				pic_x = WIDTH - pic_x;	// Karte wird um 180 Grad gedreht, denn (0|0) ist hier "oben links"
				if (pic_x > WIDTH || pic_y > HEIGHT) {
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
		pic_x &= ~15;	// Koordinaten innerhalb des Blocks ausblenden ==> Eckpunkt mit kleinsten Koordinaten
		pic_y &= ~31;
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
				receiveState = 0;
				lg.warn("Abbruch bei SubCode MAP_DATA_1");
				return;
			}
			bot_pos_y = HEIGHT - c.getDataR();	// Bot-Position, X-Komponente, wird im Bild in Y-Richtung gezaehlt
			updateInternalModel(c.getPayload(), block, 0, 7);	// macht die eigentliche Arbeit
			receiveState = 1;
			lastBlock = block;
		} else if (sub.equals(Command.SubCode.MAP_DATA_2)) {
			if (receiveState != 1 || lastBlock != block) {
				lg.warn("Abbruch bei SubCode MAP_DATA_2");
				lg.warn("State=" + receiveState);
				lg.warn("block=" + block + "; last_block=" + lastBlock);
				receiveState = 0;
				return;
			}
			bot_pos_x = WIDTH - c.getDataR();	// Bot-Position, Y-Komponente, wird im Bild in X-Richtung gezaehlt
			updateInternalModel(c.getPayload(), block, 8, 15);	// macht die eigentliche Arbeit
			receiveState = 2;
		} else if (sub.equals(Command.SubCode.MAP_DATA_3)) {
			if (receiveState != 2 || lastBlock != block) {
				lg.warn("Abbruch bei SubCode MAP_DATA_3");
				lg.warn("State=" + receiveState);
				lg.warn("block=" + block + "; last_block=" + lastBlock);
				receiveState = 0;
				return;
			}
			// DataR ist nicht belegt
			updateInternalModel(c.getPayload(), block, 16, 23);	// macht die eigentliche Arbeit
			receiveState = 3;
		} else if (sub.equals(Command.SubCode.MAP_DATA_4)) {
			if (receiveState != 3 || lastBlock != block) {
				lg.warn("Abbruch bei SubCode MAP_DATA_4");
				lg.warn("State=" + receiveState);
				lg.warn("block=" + block + "; last_block=" + lastBlock);
				receiveState = 0;
				return;
			}
			// DataR ist nicht belegt
			updateInternalModel(c.getPayload(), block, 24, 31);	// macht die eigentliche Arbeit
			receiveState = 0;
			
			/* Bot-Position fuer Auto-Scrolling verwenden */
			center.x = bot_pos_x;
			center.y = bot_pos_y;
			/* GUI-Update freigeben */
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
	public void addImageListener(Runnable2<Image, Rectangle> li) {
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
			/* Bild max. alle 200 ms neu zeichnen */
			long now = System.currentTimeMillis();
			if (now > lastUpdate + 200) {
				lastUpdate = now;
	
				imageEventPending = false;	
//TODO:	Nur belegte Bereiche zeichnen
				Image img = Toolkit.getDefaultToolkit().createImage(
					new MemoryImageSource(WIDTH, HEIGHT, pixels, 0, WIDTH));
				
				for (Runnable2<Image, Rectangle> li : imageLi) {
					li.run(img, center);
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
		int width = max_x + 15 - min_x + 1;
		int height = max_y + 31 - min_y + 1;
		
		/* belegten Teil in neues Bild kopieren */
		BufferedImage bimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = bimg.createGraphics();
		g.setColor(new Color(128, 128, 128, 255));	// "Map-Wert 0"
		g.fillRect(0, 0, width, height);
		
		/* Pixel kopieren */
		int[] map = new int[width * height];
		for (int y=min_y+1; y<=max_y+32; y++) {	// Zeilen
			System.arraycopy(pixels, min_x + 1 + y * WIDTH, map, (y - min_y - 1) * width, width);	// alle Spalten einer Zeile
		}
		
		/* Bild zeichnen und als png speichern */
		g.drawImage(Toolkit.getDefaultToolkit().createImage(
			new MemoryImageSource(width, height, map, 0, width)), 0, 0, null);
		g.dispose();
		ImageIO.write(bimg, "png", file);
	}
}
