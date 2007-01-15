package ctSim.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

//$$$ rechts oder links docken ueber setLocation, parent.getLocation
/**
 * <p>
 * Eine Art {@link JToggleButton}, der ein extra Fenster zeigt/verbirgt.
 * N&uuml;tzlich f&uuml;r Dinge wie das Logfenster und die Fernbedienung.
 * </p>
 * <p>
 * Er erscheint in der Oberfl&auml;che wie ein Knopf ({@link JButton}).
 * Dr&uuml;ckt man ihn, wird er dauerhaft als &quot;gedr&uuml;ckt&quot;
 * dargestellt und ein unabh&auml;ngiges Fenster ({@link JFrame}) wird
 * angezeigt. Dr&uuml;ckt man den Knopf erneut, springt er wieder heraus und das
 * Fenster verschwindet.
 * </p>
 * 
 * @author Hendrik Krau&szlig; &lt;<a href="mailto:hkr@heise.de">hkr@heise.de</a>>
 */
public class AuxFrameButton extends JToggleButton {
	//$$ SerialVersionUID mal generell dokumentieren
	private static final long serialVersionUID = - 7629302258050583L;
	private final JFrame auxFrame;

	/**
	 * Erzeugt einen JToggleButton, der mit einem extra Fenster verheiratet ist.
	 * Tooltip des Knopfs: Falls das {@code buttonLabel} z.B. "Bratwurst" ist,
	 * wird der Tooltip lauten &quot;Fenster anzeigen mit Bratwurst&quot;.
	 * 
	 * @param buttonLabel Text, der auf dem Knopf anzuzeigen ist
	 * @param frameTitle Text, der in die Titelzeile des extra Fensters zu
	 * schreiben ist
	 * @param frameContent Inhalt des Fensters, der beliebig komplex sein kann.
	 * Oft empfiehlt es sich, hier eine {@link JScrollPane} zu &uuml;bergeben,
	 * die alles weitere enth&auml;lt
	 */
	public AuxFrameButton(String buttonLabel, String frameTitle,
	JComponent frameContent) {
		super(buttonLabel);

		// Fenster erzeugen; konfigurieren spaeter
		auxFrame = new JFrame(frameTitle);

		// Uns selber konfigurieren
		setAlignmentX(Component.CENTER_ALIGNMENT);
		// Falls wir Platz haben, ausnutzen (keiner hat was von leerem 
		// nicht-klickbaren Platz) 
		setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		setToolTipText("Fenster anzeigen mit "+buttonLabel);
		addActionListener(new ActionListener() {
			// Fenster anzeigen/verbergen, wenn wir gedrueckt werden
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				auxFrame.setVisible(AuxFrameButton.this.isSelected());
			}
		});

		// Fenster konfigurieren, noch nicht anzeigen
		auxFrame.addWindowListener(new WindowAdapter() {
			// Wenn Fenster geschlossen wird soll der gedrueckte Button wieder
			// rausspringen
			@Override
			public void windowClosing(
			@SuppressWarnings("unused") WindowEvent e) {
				AuxFrameButton.this.setSelected(false);
			}
		});
		// HIDE, damit sich das Fenster Position + Groesse merkt
		auxFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		auxFrame.add(frameContent);
		auxFrame.pack(); // auf die Groesse, die der Inhalt will
	}

	// wenn der Knopf aus der Anzeige entfernt wird (z.B. weil der Container,
	// der ihn enthaelt, aus der UI entfernt wird), dann auch das Fenster
	// schliessen
	@Override
	public void removeNotify() {
		auxFrame.dispose();
		super.removeNotify();
	}
}
