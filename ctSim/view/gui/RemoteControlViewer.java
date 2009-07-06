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

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ctSim.model.bots.components.Sensors.RemoteControl;
import ctSim.util.FmtLogger;

/**
 * Fernbedienungs-GUI
 */
public class RemoteControlViewer extends JPanel {
	/** UID */
	private static final long serialVersionUID = - 6483687307396837800L;

	/** Logger */
	static final FmtLogger lg = FmtLogger.getLogger(
		"ctSim.view.gui.RemoteControlViewer");

	/** blau */
	private static final Color LIGHT_BLUE = new Color(150, 150, 255);
	/** gruen */
	private static final Color GR         = new Color(50,  200, 50);
	/** gelb */
	private static final Color YE         = new Color(200, 200, 0);

	/** Fernbedienungs-Komponente */
	private final RemoteControl sensor;
	/** Defaut-Color */
	private Color currentDefault = null;

	/**
	 * Setzt die Standardfarbe
	 * @param c Farbe
	 * @return null
	 */
	private JComponent defaultColor(Color c) {
		currentDefault = c;
		return null;
	}

	/**
	 * Button bauen
	 * @param label Label
	 * @param color Farbe
	 * @return Button
	 */
	private JButton b(String label, Color color) {
		// Bindestrich durch Streckenstrich ersetzen (ist laenger, Bindestrich
		// sieht so doof aus neben den grossen Pluszeichen)
		final String key = label.replaceAll("-", "\u2013");
		final JButton rv = new JButton(key);
		rv.setToolTipText("Taste "+ key);
		rv.setForeground(color);
		rv.setBackground(Color.DARK_GRAY);
		rv.addActionListener(new ActionListener() {
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(ActionEvent e) {
				lg.fine("Fernbedienungsknopf '%s' gedr\u00FCckt", rv.getText());
				try {
					sensor.send(key);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		return rv;
	}

	/**
	 * Button bauen
	 * @param label Label
	 * @return Button
	 */
	private JButton b(String label) {
		return b(label, currentDefault == null ? Color.LIGHT_GRAY : currentDefault);
	}

	/**
	 * Baut das Panel
	 * @param width Breite
	 * @param height Hoehe
	 * @param buttons Buttons
	 * @return Panel
	 */
	private JComponent grid(int width, int height, JButton... buttons) {
		// Aufpassen: GridLayout(rows, cols), also (Y, X)
		JPanel rv = new JPanel(new GridLayout(height, width, 8, 8));
		for (JButton b : buttons)
			rv.add(b == null ? new JPanel() : b);
		currentDefault = null;
		return rv;
	}

	/** Layout der Fernbedienung */
	private JComponent[] layout = new JComponent[] {
		defaultColor(LIGHT_BLUE),
		grid(3, 5,
			null,		null,		b("\u03A6", GR),	// 03A6: "Power-Symbol" (wie Kombination von O und |)
			b( "1"),	b( "2"),	b( "3"),
			b( "4"),	b( "5"),	b( "6"),
			b( "7"),	b( "8"),	b( "9"),
			b("10"),	b("11"),	b("12")
		),
		defaultColor(null),
		grid(4, 1,
			b("GR -", GR),	b("RE +", new Color(200, 50, 50)),
			b("YE -", YE),	b("BL +", LIGHT_BLUE)
		),
		defaultColor(Color.LIGHT_GRAY),
		grid(5, 5,
			b("I/II"),	null,		null,		null,		b("TV/VCR"),
			null,		null,		b("||"),	null,		null,
			null,		b("<<"),	b(">"),		b(">>"),	null,
			null,		null,		b("\u25A1"),null,		null,			// 25A1: Quadrat fuer "Stop"
			b("\u25CF"),null,		null,		null,		b("CH*P/P")		// 25CF: Dicker Punkt fuer "Record"
		),
		defaultColor(LIGHT_BLUE),
		grid(3, 2,
			b("Vol+"),	b("Mute"),	b("Ch+"),
			b("Vol-"),	null,		b("Ch-")
		)
	};	
	
	/**
	 * @param rcSensor RC5-Komponente
	 */
	public RemoteControlViewer(RemoteControl rcSensor) {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		this.sensor = rcSensor;
		for (JComponent c : layout) {
			if (c != null) {
				add(c);
				add(Box.createVerticalStrut(8));
			}
		}
	}
}
