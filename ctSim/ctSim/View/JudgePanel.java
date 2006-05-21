/* c't-Sim - Robotersimulator fuer den c't-Bot
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
package ctSim.View;

import java.awt.Dimension;
import java.util.Calendar;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ctSim.Model.Rules.Judge;

/**
 * Diese Klasse zeigt die Inhalte eines Judges an
 * 
 * @author bbe (bbe@heise.de)
 */
public class JudgePanel extends SimPanel {
	private static final long serialVersionUID = 1L;
	
	/** Der zugehoerige Judge */
	private Judge judge;
	
	/** Die Laufzeit des Wettbewerbs */
	private JTextField runTimeField;

	/** Hier kommen die Ergebnisse rein */
	JTextArea resultField;

	/**
	 * Konstruktor
	 * @param judge
	 */
	public JudgePanel(Judge judge) {
		super();
		this.judge = judge;
		initGUI();
	}

	@Override
	protected void initGUI() {
		Dimension fieldDim;
		fieldDim = new Dimension(100, 25);

		this.setLayout(new BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

		
		// Die Laufzeit
		JPanel panel = new JPanel();
		panel.add(new JLabel("Laufzeit"));
		runTimeField = new JTextField();
		runTimeField.setText("0 ms");
		runTimeField.setEditable(false);
		runTimeField.setPreferredSize(fieldDim);
		panel.add(runTimeField);
		this.add(panel);

		// Die Sieger
		panel = new JPanel();
		panel.add(new JLabel("Ergebnisse"));
		panel.setLayout(new BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
		
		resultField = new JTextArea();
		resultField.setPreferredSize(new Dimension(400,60));
		resultField.setEditable(false);
		resultField.setAutoscrolls(true);
		panel.add(resultField);
		this.add(panel);
		
				
	}

	/**
	 * Nix zu tun hier 
	 */
	@Override
	public void reactToChange() {
		// TODO Auto-generated method stub
	}

	/** 
	 * liefert den Judge zureuck
	 * 
	 * @return Der Judge
	 */
	public Judge getJudge() {
		return judge;
	}

	/** 
	 * Setzt die Laufzeit 
	 * @param time
	 */
	public void setRunTime(long time){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		
		runTimeField.setText((cal.get(Calendar.HOUR)-1)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND)+":"+cal.get(Calendar.MILLISECOND));

		super.repaint();
		this.repaint();
	}
	
	/** Erweitert die Ergebnisliste
	 * @param result
	 */
	public void addResult(String result){
		resultField.setText(resultField.getText()+"\n"+result);
		super.repaint();
		this.repaint();
	}
}
