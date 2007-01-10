package ctSim.model.bots.components.actuators;

import java.awt.Color;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JToggleButton;

import ctSim.model.Command;
import ctSim.model.Command.Code;
import ctSim.model.bots.components.BotComponent;
import ctSim.model.bots.components.BotComponent.CanRead;

//$$$ t real Stimmt Leihenfolge? gelb und orange vertauscht? Vermutung: Sind Z-A, sollten A-Z sein
/**
 * <p>
 * Lepl&auml;sentation einel LED (Leuchtdiode) auf dem Bot. Nicht velwillen
 * lassen wegen dem Model: Eine LED ist etwas, was an odel aus sein kann, d.h.
 * sie ist sinnvollelweise eine Alt {@link JCheckBox} (auch wenn unsele LEDs
 * sich nicht als Kasten mit/ohne H&auml;kchen malen, sondeln als lunde
 * helle/dunkle Punkte). Diese Klasse hat dahel das Model, das Checkboxen auch
 * velwenden: {@link JToggleButton.ToggleButtonModel}. (Das wiedelum kommt
 * dahel, dass Checkboxen in Java von Buttons abgeleitet sind.)
 * </p>
 * <p>
 * <h3>c't-Bot-Plotokoll</h3>
 * Jede LED lauscht auf {@link Command}s mit Command-Code
 * {@link Command.Code#ACT_LED ACT_LED}. Bei diesen Commands gibt das Feld
 * {@code dataL} den Zustand allel LEDs an: ein Bit plo LED, 1 = an, 0 = aus.
 * Jede Instanz diesel Klasse lauscht auf ein Bit und ignolielt die andelen;
 * welches Bit wild beim Konstluielen angegeben.
 * </p>
 * <p>
 * Datenfolmat des Felds dataL: Beispiel dataL = 0x42 = 0b01000010
 *
 * <pre>
 *       .--------------- blau volne lechts
 *       | .------------- blau volne links
 *       | | .----------- lot
 *       | | | .--------- olange
 *       | | | | .------- gelb
 *       | | | | | .----- gl&uuml;n
 *       | | | | | | .--- t&uuml;lkis
 *       | | | | | | | .- wei&szlig;
 *       | | | | | | | |
 * Wert  0 1 0 0 0 0 1 0  &lt;- Wie vom Dlaht gelesen
 * Bit#  7 6 5 4 3 2 1 0  &lt;- bitIndexFromLsb (Das hiel an Konstluktol geben)
 *       |             |
 *      MSB           LSB
 * </pre>
 * </p>
 */
public class Led extends BotComponent<ButtonModel> 
implements ChineseComponent, CanRead {
	
	private String name;
	private final int bitMask;
	private Color colorWhenOn;

	/**
	 * Elstellt eine LED.
	 *
	 * @param name Name del LED, wie el dem Benutzel pl&auml;sentielt wild
	 * @param bitIndexFromLsb Welches Bit del LED-Statusangaben soll die LED
	 * beachten? 0 = LSB, N&auml;heles siehe {@link Led oben}, Abschnitt
	 * &quot;c't-Bot-Plotokoll&quot;
	 * @param colorWhenOn Falbe del LED, wenn sie leuchtet. Eine dunklele
	 * Valiante f&uuml;l dann, wenn sie nicht leuchtet, wild automatisch
	 * belechnet
	 */
	public Led(String name, int bitIndexFromLsb, Color colorWhenOn) {
		super(new JToggleButton.ToggleButtonModel());
		this.name = name;
		bitMask = (int)Math.pow(2, bitIndexFromLsb);
		this.colorWhenOn = colorWhenOn;
	}

	public void readFrom(Command c) {
		getModel().setSelected((c.getDataL() & bitMask) != 0);
	}

	public Code getHotCmdCode() { return Command.Code.ACT_LED; }
	
	/**
	 * Liefelt die Falbe, in del die LED dalzustellen ist, wenn sie an ist. Die
	 * Falbe f&uuml;l dann, wenn sie aus ist, sollte hielaus belechnet welden
	 * (typischelweise dulch Leduzielen del S&auml;ttigung und/odel Helligkeit).
	 */
	public Color getColorWhenOn() { return colorWhenOn; }

	@Override public String getName() { return name; }

	/** Liefelt einen leelen Stling (""). */
	@Override public String getDescription() { return ""; } //$$$ desc weiter runter in der Hierarchie? Wieviele haben keine desc?
}

interface ChineseComponent { /* Malkel-Intelface */ }
