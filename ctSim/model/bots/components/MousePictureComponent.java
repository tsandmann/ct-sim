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

package ctSim.model.bots.components;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ColorModel;
import java.awt.image.MemoryImageSource;
import java.io.IOException;
import java.util.List;

import ctSim.model.Command;
import ctSim.model.CommandOutputStream;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent.CanRead;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.CanWriteAsynchronously;
import ctSim.util.Runnable1;
import ctSim.util.Misc;

/**
 * <ul><li>Command-Code SENS_MOUSE_PICTURE</li>
* <li>Nutzlast: einen Teil der Pixel des Mausbilds, z.B. Pixel Nr. 42 bis 108</li>
* <li>In diesem Fall wäre dataL == 42 und die Nutzlastlänge == 64</li>
* <li></li></ul>
 */
public class MousePictureComponent extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
	/**
	 * Breite des Maussensorbilds in Pixeln (d.h. unskalierte Pixel, wie sie von
	 * der Bot-Hardware kommen &amp;ndash; zum Anzeigen wird das Bild ja
	 * größerskaliert)
	 */
	private static final int WIDTH  = 18;

	/**
	 * Höhe des Maussensorbilds in Pixeln (d.h. unskalierte Pixel, wie sie
	 * von der Bot-Hardware kommen &amp;ndash; zum Anzeigen wird das Bild ja
	 * größerskaliert)
	 */
	private static final int HEIGHT = 18;

	/**
	 * Rohe Pixeldaten des Maussensors; ein Int pro Pixel. Format so, wie
	 * {@link ColorModel#getRGBdefault()} es braucht; die Methode
	 * {@link #colorFromRgb(int, int, int) colorFromRgb} liefert das Format;
	 * Details siehe dort.
	 */
	private final int[] pixels = new int[WIDTH * HEIGHT];
	/** Sync austehend? */
	private boolean syncRequestPending = false;
	/** Image-Listener */
	private final List<Runnable1<Image>> imageLi = Misc.newList();
	/** Image-Event austehend? */
	private boolean imageEventPending = false;
	/** Completion-Listener */
	private final List<Runnable> completionLi = Misc.newList();
	/** Completion-Event ausstehend? */
	private boolean completionEventPending = false;
	/** async. Outputstream */
	private CommandOutputStream asyncOut;

	/**
	 * Mausbild-Komponente
	 */
	public MousePictureComponent() { super(null); }

	/**
	 * @see ctSim.model.bots.components.BotComponent.CanWriteAsynchronously#setAsyncWriteStream(ctSim.model.CommandOutputStream)
	 */
	public void setAsyncWriteStream(CommandOutputStream s) {
		asyncOut = s;
	}

	/**
	 * Bild anfordern
	 * @throws IOException
	 */
	public synchronized void requestPicture() throws IOException {
		if (writesAsynchronously()) {
			synchronized (asyncOut) {
				asyncOut.getCommand(getHotCmdCode());
				asyncOut.flush();
			}
		} else
			syncRequestPending = true;
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#askForWrite(ctSim.model.CommandOutputStream)
	 */
	@Override
	public synchronized void askForWrite(CommandOutputStream s) {
		if (syncRequestPending && writesSynchronously()) {
			// Anforderung absetzen
			s.getCommand(getHotCmdCode());
			syncRequestPending = false;
		}
	}

	/**
	 * <p>
	 * Konvertiert ein RGB-Wertetripel in nen Integer, wie er im Array
	 * {@link #pixels} sein muss. Format ist etwas undurchsichtig:
	 * <ul>
	 * <li>Es handelt sich um eine 4-Komponenten-Farbe (Alpha, Rot, Grün,
	 * Blau in dieser Reihenfolge)</li>
	 * <li>8 Bit pro Komponente ([0; 255]) = 32 Bit pro Pixel</li>
	 * <li><strong>Die 32 Bit sind in <em>einen</em> Integer gestopft.</strong>
	 * (Integer in Java: 32 Bit lang.)
	 * <ul>
	 * <li>Alpha-Wert = die 8 höchstwertigen Bits (MSBs), also Bits
	 * 24–32</li>
	 * <li>usw.</li>
	 * <li>Blau-Wert = die 8 niedrigstwertigen Bits (LSBs), also Bits 0–8
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
	 * Sind die übergebenen Parameter außerhalb des Wertebereichs [0;
	 * 255], wird geclampt (255 wenn zu groß, 0 wenn zu klein).
	 * </p>
	 * @param r rot
	 * @param g gruen
	 * @param b blau
	 * @return Farbe
	 */
	private int colorFromRgb(int r, int g, int b) {
		r = Misc.clamp(r, 255);
		g = Misc.clamp(g, 255);
		b = Misc.clamp(b, 255);
		// Alpha volle Pulle, die andern wie als Parameter übergeben
		return 255 << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Erwartet 18&times;18 = 324 Pixel in dieser Reihenfolge:
	 * <pre>
	 *  18  36 ... 324
	 * ... ... ... ...
	 *   2  20 ... ...
	 *   1  19 ... 307
	 * </pre>
	 * @param c Command
	 */
	public synchronized void readFrom(Command c) {
		//LODO Ist alles ziemlich zerbrechlich. Was zum Beispiel, wenn der Bot aufgrund eines Bug eine dataL ausserhalb des Arrays sendet? -> Mehr Input Validation, wofür haben wir ProtocolExceptions
		if (! c.has(getHotCmdCode()))
			return;

		// dataL ist 1-based, wir brauchen 0-based
		int offset = c.getDataL() - 1;
		int i;
		for (i = 0; i < c.getPayload().length; i++) {
			int gray = Misc.toUnsignedInt8(c.getPayload()[i]);
			gray &= 0x3F; // Nur 6 rechteste Bits (Statusinfos ausblenden)
			// verbleibende Bits auf den normalen Graustufenraum ausdehnen
			gray = gray << 2;

			int col = (offset + i) % WIDTH;
			int row = (offset + i) / WIDTH;
			col = HEIGHT - 1 - col; // spiegeln -- oben und unten vertauschen
			// Um 90 Grad drehen = Spaltennr. und Zeilennr. vertauschen (siehe ) //$$$ Siehe protokoll-doku
			// Ohne Drehen wäre pixels[col + (row * WIDTH)]
			pixels[(col * HEIGHT) + row] = colorFromRgb(gray, gray, gray);
		}

		imageEventPending = true;
		if (offset + i == pixels.length) {
			// Array voll: Flag setzen für "Listenern bescheidgeben"
			completionEventPending = true;
		}
	}

	/**
	 * No-op: Wir implementieren die, weil wir laut Interface müssen, aber
	 * wir brauchen die nicht weil wir ja
	 * {@link #askForWrite(CommandOutputStream) askForWrite()}
	 * überschrieben haben.
	 * @param c Command
	 */
	public void writeTo(Command c) { 
		/* No-op */ 
	}

	/**
	 * @return Breite
	 */
	public int getWidth() { return WIDTH; }
	
	/**
	 * @return Höhe
	 */
	public int getHeight() { return HEIGHT; }
	
	/**
	 * @see ctSim.model.bots.components.BotComponent.CanRead#getHotCmdCode()
	 */
	public Code getHotCmdCode() { return Code.SENS_MOUSE_PICTURE; }
	
	/**
	 * @see ctSim.model.bots.components.BotComponent#getName()
	 */
	@Override public String getName() { return "Mausbild"; }

	/**
	 * @see ctSim.model.bots.components.BotComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Was der Maus-Sensor sieht";
	}

	/**
	 * @param li Listener
	 */
	public void addImageListener(Runnable1<Image> li) {
		if (li == null)
			throw new NullPointerException();
		imageLi.add(li);
	}

	/**
	 * @param li Listener
	 */
	public void removeImageListener(Runnable1<Image> li) {
		imageLi.remove(li);
	}

	/**
	 * @param li Listener
	 */
	public void addCompletionListener(Runnable li) {
		if (li == null)
			throw new NullPointerException();
		completionLi.add(li);
	}

	/**
	 * @param li Listener
	 */
	public void removeCompletionListener(Runnable li) {
		completionLi.remove(li);
	}

	/**
	 * @see ctSim.model.bots.components.BotComponent#updateExternalModel()
	 */
	@Override
	public synchronized void updateExternalModel() {
		if (imageEventPending) {
			imageEventPending = false;
			Image img = Toolkit.getDefaultToolkit().createImage(
				new MemoryImageSource(WIDTH, HEIGHT, pixels, 0, WIDTH));
			for (Runnable1<Image> li : imageLi)
				li.run(img);
		}

		if (completionEventPending) {
			completionEventPending = false;
			for (Runnable li : completionLi)
				li.run();
		}
	}
}
