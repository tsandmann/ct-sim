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

/**
 * Zeigt Statusinformationen zum Simulator an
 * 
 * @author Felix Beckwermert
 *
 */
public class StatusBar extends Box {
	
	/** UID */
	private static final long serialVersionUID = 1L;
	/** kleineste Tickrate */
	private static final int MIN_TICK_RATE = 0;
	/** Default-Tickrate*/
	private static final int INIT_TICK_RATE = 0;
	/** groesste Tickrate */
	private static final int MAX_TICK_RATE = 400;
	/** Intervall */
	private static final int MAJOR_TICK = 10;
	
	/** Main-Fenster */
	private MainWindow parent;
	
	/** Eine Stunde abziehen fuer Anzeige */
	private final long TIME_TO_SUB = 1000*60*60;
	/** Zeit-Feld */
	private JLabel timeLabel;
	/** Tickrate-Feld */
	private JFormattedTextField tickRateField;
	/** Tickrate-Slider */
	private JSlider tickRateSlider;
	
	/**
	 * Konstruktor
	 * @param par Referenz auf den Frame, in dem die StatusBar angezeigt wird
	 */
	StatusBar(MainWindow par) {
		
		super(BoxLayout.LINE_AXIS);
		
		this.parent = par;
		
//		this.timeFormatter = new SimpleDateFormat("HH:mm:ss.SSS");
		
		initTickRateSlider();
		initTickRateField();
		
//		this.timeField = new JTextField("Time: 0", 5);
//		this.timeField.setMaximumSize(new Dimension(50, 22));
		
		//this.timeLabel = new JLabel("Time: "+this.timeFormatter.format(new Date(-TIME_TO_SUB)));
		this.timeLabel = new JLabel("Zeit: "+String.format("%tT.%<tL", new Date(-this.TIME_TO_SUB))); //$NON-NLS-1$ //$NON-NLS-2$
		
		this.add(Box.createRigidArea(new Dimension(10, 0)));
		//this.add(this.timeField);
		this.add(this.timeLabel);
		
		this.add(Box.createHorizontalGlue());
		
		JTextField tr = new JTextField("TR:"); //$NON-NLS-1$
		tr.setHorizontalAlignment(SwingConstants.CENTER);
		tr.setEditable(false);
		//tr.setEnabled(false);
		tr.setBorder(BorderFactory.createEtchedBorder());
		tr.setMinimumSize(new Dimension(30, 22));
		tr.setPreferredSize(new Dimension(30, 22));
		tr.setMaximumSize(new Dimension(30, 23));
		
		this.add(tr);
		this.add(this.tickRateField);
		this.add(Box.createRigidArea(new Dimension(1,1)));
		this.add(this.tickRateSlider);
	}
	
	/**
	 * Erzeugt einen Schieberegler fuer den Simulatortakt
	 */
	void initTickRateSlider() {
		
		this.tickRateSlider = new JSlider(
				StatusBar.MIN_TICK_RATE,
				StatusBar.MAX_TICK_RATE,
				StatusBar.INIT_TICK_RATE);
		
		this.tickRateSlider.addChangeListener(new ChangeListener() {

			@SuppressWarnings({"synthetic-access","boxing"})
			public void stateChanged(@SuppressWarnings("unused") ChangeEvent e) {
				
				if(StatusBar.this.tickRateSlider.getValueIsAdjusting()) {
					StatusBar.this.tickRateField.setText(String.valueOf(StatusBar.this.tickRateSlider.getValue()));
				} else {
					StatusBar.this.tickRateField.setValue(StatusBar.this.tickRateSlider.getValue());
					StatusBar.this.parent.setTickRate(StatusBar.this.tickRateSlider.getValue());
				}
			}
		});
		
		this.tickRateSlider.setMajorTickSpacing(StatusBar.MAJOR_TICK);
		//this.tickRateSlider.setMinorTickSpacing(5);
		this.tickRateSlider.setPaintTicks(true);
		//this.tickRateSlider.setPaintLabels(true);
		this.tickRateSlider.setPreferredSize(new Dimension(150, 22));
		this.tickRateSlider.setBorder(BorderFactory.createEtchedBorder());
	}
	
	/**
	 * Erzeugt ein Anzeigefeld fuer den Simulatortakt
	 */
	@SuppressWarnings("boxing") void initTickRateField() {
		
		NumberFormatter formatter = new NumberFormatter(NumberFormat.getInstance());
		formatter.setMinimum(StatusBar.MIN_TICK_RATE);
		formatter.setMaximum(StatusBar.MAX_TICK_RATE);
		
		this.tickRateField = new JFormattedTextField(formatter);
		this.tickRateField.setValue(StatusBar.INIT_TICK_RATE);
		this.tickRateField.setColumns(3);
		this.tickRateField.setHorizontalAlignment(SwingConstants.CENTER);
		this.tickRateField.setMinimumSize(new Dimension(44, 22));
		this.tickRateField.setMaximumSize(new Dimension(44, 22));
		this.tickRateField.setPreferredSize(new Dimension(44, 22));
		//System.out.println(this.tickRateField.getPreferredSize());
		
		this.tickRateField.addPropertyChangeListener(new PropertyChangeListener() {

			@SuppressWarnings("synthetic-access")
			public void propertyChange(PropertyChangeEvent evt) {
				
				if("value".equals(evt.getPropertyName())) { //$NON-NLS-1$
					StatusBar.this.tickRateSlider.setValue((Integer)StatusBar.this.tickRateField.getValue());
				}
			}
		});
	}
	
	/**
	 * @param time Zeitspanne, um die Simulatorzeit erhoeht werden soll
	 */
	public void updateTime(long time) {
		
//		this.timeField.setText("Time: "+time);
		
		//this.timeLabel.setText("Time: "+this.timeFormatter.format(new Date(time-TIME_TO_SUB)));
		this.timeLabel.setText("Zeit: "+String.format("%tT.%<tL", new Date(time-this.TIME_TO_SUB))); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Setzt den Simulatortakt auf den initialen Wert zurueck
	 */
	@SuppressWarnings("boxing")
	protected void reinit() {
		
		this.tickRateField.setValue(StatusBar.INIT_TICK_RATE);
		this.updateTime(0);
	}
}
