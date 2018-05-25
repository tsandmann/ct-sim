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

import ctSim.view.gui.ComponentJFrame;
import ctSim.view.gui.RemoteCallViewer;

/**
 * <p>
 * Eine Art {@link JToggleButton}, der ein extra Fenster zeigt/verbirgt.
 * Nützlich für Dinge wie das Logfenster und die Fernbedienung.
 * </p>
 * <p>
 * Er erscheint in der Oberfläche wie ein Knopf ({@link JButton}). Drückt man ihn, wird er dauerhaft als
 * "gedrückt" dargestellt und ein unabhängiges Fenster ({@link JFrame}) wird angezeigt. Drückt man den Knopf
 * erneut, springt er wieder heraus und das Fenster verschwindet.
 * </p>
 *
 * @author Hendrik Krauß
 */
public class AuxFrameButton extends JToggleButton {
	/** UID */
	private static final long serialVersionUID = - 7629302258050583L;
	/** Logger */
//	private final FmtLogger lg = FmtLogger.getLogger("ctSim.utils.AuxFrameButton");
	/** Frame */
	private final ComponentJFrame auxFrame;

	/**
	 * Erzeugt einen JToggleButton, der mit einem extra Fenster verheiratet ist. Tooltip des Knopfs:
	 * Falls das {@code buttonLabel} z.B. "Bratwurst" ist, wird der Tooltip lauten "Fenster anzeigen
	 * mit Bratwurst".
	 *
	 * @param buttonLabel	Text, der auf dem Knopf anzuzeigen ist
	 * @param frameTitle	Text, der in die Titelzeile des extra Fensters zu schreiben ist
	 * @param frameContent	Inhalt des Fensters, der beliebig komplex sein kann. Oft empfiehlt es sich,
	 * 				hier eine {@link JScrollPane} zu übergeben, die alles weitere enthält
	 * @param enabled		soll der Button aktiviert sein?
	 */
	public AuxFrameButton(String buttonLabel, String frameTitle, final JComponent frameContent, boolean enabled) {
		super(buttonLabel);

		// Fenster erzeugen, aber erst später konfigurieren
		auxFrame = new ComponentJFrame(frameTitle, frameContent, this);

//		auxFrame.setLocation(300, 300);


		// uns selber konfigurieren
		setAlignmentX(Component.CENTER_ALIGNMENT);
		// Falls wir Platz haben, ausnutzen (niemand hat etwas von leerem nicht-klickbaren Platz)
		setMaximumSize(new Dimension(Integer.MAX_VALUE, getMaximumSize().height));

		setEnabled(enabled);

		String keyinfo = "";
		if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
			keyinfo = "Strg";
		} else if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
			keyinfo = "Strg";
		} else if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
			keyinfo = "Cmd";
		}
		keyinfo += " + w)";
		if (enabled) {
			setToolTipText("Fenster \"" + buttonLabel + "\" anzeigen (schließen mit " + keyinfo);
		} else {
			setToolTipText("Komponente \"" + buttonLabel + "\" im Bot-Code nicht aktiviert");
		}
		addActionListener(new ActionListener() {
			// Fenster anzeigen/verbergen, wenn wir gedrückt werden
			public void actionPerformed(ActionEvent e) {
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

		// Fenster konfigurieren aber noch nicht anzeigen
		auxFrame.addWindowListener(new WindowAdapter() {
			// wenn Fenster geschlossen wird soll der gedrückte Button wieder rausspringen
			@Override
			public void windowClosing(WindowEvent e) {
				AuxFrameButton.this.setSelected(false);
			}
		});
		// HIDE, damit sich das Fenster Position und Größe merkt
		auxFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		auxFrame.add(frameContent);
		if (!(frameContent instanceof RemoteCallViewer)) {
			auxFrame.pack();	// auf die Größe, die der Inhalt will
		} else {
			/* Remote-Call-Fenster größer */
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			if (dim.width > 1300) {
				auxFrame.setSize(new Dimension(1300, 500));
			} else {
				auxFrame.setSize(new Dimension(dim.width, 500));
			}
		}
		auxFrame.setMinimumSize(frameContent.getMinimumSize());
	}

	/*
	 * wenn der Knopf aus der Anzeige entfernt wird (z.B. weil der Container, der ihn enthält, aus der
	 * UI entfernt wird), dann auch das Fenster schließen
	 */
	/**
	 * @see javax.swing.JComponent#removeNotify()
	 */
	@Override
	public void removeNotify() {
//		lg.fine("AuxFrameButton::removeNotify()");
		auxFrame.dispose();
		super.removeNotify();
	}
}
