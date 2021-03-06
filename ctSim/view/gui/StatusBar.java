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

package ctSim.view.gui;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;

import ctSim.controller.Config;

/**
 * Zeigt Statusinformationen zum Simulator an
 *
 * @author Felix Beckwermert
 */
public class StatusBar extends Box {
	/** UID */
	private static final long serialVersionUID = 1L;
	/** kleineste Tickrate */
	public static final int MIN_TICK_RATE = 0;
	/** Default-Tickrate*/
	private final int INIT_TICK_RATE;
	/** größte Tickrate */
	public static final int MAX_TICK_RATE = 400;
	/** Intervall */
	public static final int MAJOR_TICK = 10;

	/** Main-Fenster */
	private MainWindow parent;

	/** Eine Stunde abziehen für Anzeige */
	private final long TIME_TO_SUB = 1000*60*60;
	/** Zeit-Feld */
	private JLabel timeLabel;
	/** Tickrate-Feld */
	private JFormattedTextField tickRateField;
	/** Tickrate-Slider */
	private JSlider tickRateSlider;

	/**
	 * Konstruktor
	 *
	 * @param par	Referenz auf den Frame, in dem die StatusBar angezeigt wird
	 */
	StatusBar(MainWindow par) {
		super(BoxLayout.LINE_AXIS);

		parent = par;

		int tickrate = 0;
		try {
			tickrate = Integer.parseInt(Config.getValue("ctSimTickRate"));
		} catch (NumberFormatException exc) {
			// egal
		}
		INIT_TICK_RATE = Math.min(tickrate, MAX_TICK_RATE);

		initTickRateSlider();
		initTickRateField();

		timeLabel = new JLabel("Zeit: " + String.format("%tT.%<tL", new Date(- TIME_TO_SUB)));

		add(Box.createRigidArea(new Dimension(10, 0)));
		add(timeLabel);

		add(Box.createHorizontalGlue());

		JTextField tr = new JTextField("TR:");
		tr.setHorizontalAlignment(SwingConstants.CENTER);
		tr.setEditable(false);
		tr.setBorder(BorderFactory.createEtchedBorder());
		tr.setMinimumSize(new Dimension(30, 22));
		tr.setPreferredSize(new Dimension(30, 22));
		tr.setMaximumSize(new Dimension(30, 23));

		add(tr);
		add(tickRateField);
		add(Box.createRigidArea(new Dimension(1, 1)));
		add(tickRateSlider);
	}

	/** Erzeugt einen Schieberegler für den Simulatortakt */
	void initTickRateSlider() {
		tickRateSlider = new JSlider(StatusBar.MIN_TICK_RATE, StatusBar.MAX_TICK_RATE, INIT_TICK_RATE);

		tickRateSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (tickRateSlider.getValueIsAdjusting()) {
					tickRateField.setText(String.valueOf(tickRateSlider.getValue()));
				} else {
					tickRateField.setValue(tickRateSlider.getValue());
					parent.setTickRate(tickRateSlider.getValue());
				}
			}
		});

		tickRateSlider.setMajorTickSpacing(StatusBar.MAJOR_TICK);
		tickRateSlider.setPaintTicks(true);
		tickRateSlider.setPreferredSize(new Dimension(150, 22));
		tickRateSlider.setBorder(BorderFactory.createEtchedBorder());
	}

	/** Erzeugt ein Anzeigefeld für den Simulatortakt */
	void initTickRateField() {
		NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
		formatter.setMinimum(StatusBar.MIN_TICK_RATE);
		formatter.setMaximum(StatusBar.MAX_TICK_RATE);

		tickRateField = new JFormattedTextField(formatter);
		tickRateField.setValue(INIT_TICK_RATE);
		tickRateField.setColumns(3);
		tickRateField.setHorizontalAlignment(SwingConstants.CENTER);
		tickRateField.setMinimumSize(new Dimension(44, 22));
		tickRateField.setMaximumSize(new Dimension(44, 22));
		tickRateField.setPreferredSize(new Dimension(44, 22));

		tickRateField.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if ("value".equals(evt.getPropertyName())) {
					tickRateSlider.setValue((Integer) tickRateField.getValue());
				}
			}
		});
	}

	/**
	 * @param time	Zeitspanne, um die Simulatorzeit erhöht werden soll
	 */
	public void updateTime(long time) {
		timeLabel.setText("Zeit: " + String.format("%tT.%<tL", new Date(time - TIME_TO_SUB)));
	}

	/** Setzt den Simulatortakt auf den initialen Wert zurück */
	protected void reinit() {
		tickRateField.setValue(INIT_TICK_RATE);
		updateTime(0);
	}
}
