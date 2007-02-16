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

//$$ dic doc
//$$$ t MousePicture "laufend" reisst manchmal ab: TcpDump ansehen
/**
 * <ul><li>Command-Code SENS_MOUSE_PICTURE</li>
* <li>Nutzlast: einen Teil der Pixel des Mausbilds, z.B. Pixel Nr. 42 bis 108</li>
* <li>In diesem Fall w�re dataL == 42 und die Nutzlastl�nge == 64</li>
* <li></li></ul>
 */
public class MousePictureComponent extends BotComponent<Void>
implements CanRead, CanWrite, CanWriteAsynchronously {
	/**
	 * Breite des Maussensorbilds in Pixeln (d.h. unskalierte Pixel, wie sie von
	 * der Bot-Hardware kommen &amp;ndash; zum Anzeigen wird das Bild ja
	 * gr&ouml;&szlig;erskaliert)
	 */
	private static final int WIDTH  = 18;

	/**
	 * H&ouml;he des Maussensorbilds in Pixeln (d.h. unskalierte Pixel, wie sie
	 * von der Bot-Hardware kommen &amp;ndash; zum Anzeigen wird das Bild ja
	 * gr&ouml;&szlig;erskaliert)
	 */
	private static final int HEIGHT = 18;

	/**
	 * Rohe Pixeldaten des Maussensors; ein Int pro Pixel. Format so, wie
	 * {@link ColorModel#getRGBdefault()} es braucht; die Methode
	 * {@link #colorFromRgb(int, int, int) colorFromRgb} liefert das Format;
	 * Details siehe dort.
	 */
	private final int[] pixels = new int[WIDTH * HEIGHT];
	private boolean syncRequestPending = false;
	private final List<Runnable1<Image>> imageLi = Misc.newList();
	private boolean imageEventPending = false;
	private final List<Runnable> completionLi = Misc.newList();
	private boolean completionEventPending = false;
	private CommandOutputStream asyncOut;

	public MousePictureComponent() { super(null); }

	public void setAsyncWriteStream(CommandOutputStream s) {
		asyncOut = s;
	}

	public synchronized void requestPicture() throws IOException {
		if (writesAsynchronously()) {
			synchronized (asyncOut) {
				asyncOut.getCommand(getHotCmdCode());
				asyncOut.flush();
			}
		} else
			syncRequestPending = true;
	}

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
	 */
	private int colorFromRgb(int r, int g, int b) {
		r = Misc.clamp(r, 255);
		g = Misc.clamp(g, 255);
		b = Misc.clamp(b, 255);
		// Alpha volle Pulle, die andern wie als Parameter uebergeben
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
	 */
	public synchronized void readFrom(Command c) {
		//LODO Ist alles ziemlich zerbrechlich. Was zum Beispiel, wenn der Bot aufgrund eines Bug eine dataL ausserhalb des Arrays sendet? -> Mehr Input Validation, wofuer haben wir ProtocolExceptions
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
			// Um 90� drehen = Spaltennr. und Zeilennr. vertauschen (siehe ) //$$$ Siehe protokoll-doku
			// Ohne Drehen waere pixels[col + (row * WIDTH)]
			pixels[(col * HEIGHT) + row] = colorFromRgb(gray, gray, gray);
		}

		imageEventPending = true;
		if (offset + i == pixels.length) {
			// Array voll: Flag setzen fuer "Listenern bescheidgeben"
			completionEventPending = true;
		}
	}

	/**
	 * No-op: Wir implementieren die, weil wir laut Interface m&uuml;ssen, aber
	 * wir brauchen die nicht weil wir ja
	 * {@link #askForWrite(CommandOutputStream) askForWrite()}
	 * &uuml;berschrieben haben.
	 */
	public void writeTo(@SuppressWarnings("unused") Command c) { /* No-op */ }

	public int getWidth() { return WIDTH; }
	public int getHeight() { return HEIGHT; }
	public Code getHotCmdCode() { return Code.SENS_MOUSE_PICTURE; }
	@Override public String getName() { return "Mausbild"; }

	@Override
	public String getDescription() {
		return "Was der Maus-Sensor sieht";
	}

	public void addImageListener(Runnable1<Image> li) {
		if (li == null)
			throw new NullPointerException();
		imageLi.add(li);
	}

	public void removeImageListener(Runnable1<Image> li) {
		imageLi.remove(li);
	}

	public void addCompletionListener(Runnable li) {
		if (li == null)
			throw new NullPointerException();
		completionLi.add(li);
	}

	public void removeCompletionListener(Runnable li) {
		completionLi.remove(li);
	}

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