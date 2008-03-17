package ctSim.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import ctSim.view.gui.RemoteCallViewer;

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
	/** UID */
	private static final long serialVersionUID = - 7629302258050583L;
	/** Frame */
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
	final JComponent frameContent) {
		super(buttonLabel);

		// Fenster erzeugen; konfigurieren spaeter
		auxFrame = new JFrame(frameTitle);

		// Uns selber konfigurieren
		setAlignmentX(Component.CENTER_ALIGNMENT);
		// Falls wir Platz haben, ausnutzen (keiner hat was von leerem 
		// nicht-klickbaren Platz) 
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 
			getMaximumSize().height));
		setToolTipText("Fenster anzeigen mit "+buttonLabel);
		addActionListener(new ActionListener() {
			// Fenster anzeigen/verbergen, wenn wir gedrueckt werden
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(
			@SuppressWarnings("unused") ActionEvent e) {
				auxFrame.setVisible(AuxFrameButton.this.isSelected());
				if (frameContent instanceof RemoteCallViewer) {
					/* RemoteCall-Liste holen, falls noch nicht geschehen */
					RemoteCallViewer rcViewer = (RemoteCallViewer) frameContent;
					if (!rcViewer.getListReceived()) {
						rcViewer.requestRCallList();
					}
				}
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
		if (!(frameContent instanceof RemoteCallViewer)) {
			auxFrame.pack(); // auf die Groesse, die der Inhalt will
		} else {
			/* Remote-Call-Fenster groesser */
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			if (dim.width > 1300) {
				auxFrame.setSize(new Dimension(1300, 500));
			} else {
				auxFrame.setSize(new Dimension(dim.width, 500));
			}
		}
	}

	// wenn der Knopf aus der Anzeige entfernt wird (z.B. weil der Container,
	// der ihn enthaelt, aus der UI entfernt wird), dann auch das Fenster
	// schliessen
	/**
	 * @see javax.swing.JComponent#removeNotify()
	 */
	@Override
	public void removeNotify() {
		auxFrame.dispose();
		super.removeNotify();
	}
}
