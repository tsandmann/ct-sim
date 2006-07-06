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
package ctSim.view;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import ctSim.model.rules.Judge;

/**
 * Auswahldialog fuer die Schiedsrichter-Instanz
 * 
 * @author Felix Beckwermert
 */
public class JudgeChooser extends JDialog {
	
	private Judge judge;
	
	private JRadioButton defJudge, contestJudge, labyJudge;
	
	/**
	 * Der Konstruktor
	 * @param own Der Frame, in dem der Auswahldialog laeuft
	 */
	JudgeChooser(Frame own) {
		
		super(own, "Schiedsrichter wählen...", true);
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.setLayout(new BorderLayout());
		
		Box buttons = new Box(BoxLayout.PAGE_AXIS);
		
		buttons.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		
		initRadioButtons();
		
		buttons.add(new JLabel("Wählen Sie einen Schiedsrichter:"));
		buttons.add(this.defJudge);
		buttons.add(this.labyJudge);
		//buttons.add(this.contestJudge);
		
		
		this.add(buttons, BorderLayout.CENTER);
		this.add(new JButton("Ok"), BorderLayout.SOUTH);
		
		this.setLocationRelativeTo(own);
		
		this.pack();
		this.setVisible(true);
	}
	
	private void initRadioButtons() {
		
		this.defJudge = new JRadioButton("DefaultJudge", true);
		this.labyJudge = new JRadioButton("LabyrinthJudge");
		this.contestJudge = new JRadioButton("LabyrinthContestJudge");
		
		ButtonGroup grp = new ButtonGroup();
		grp.add(defJudge);
		grp.add(labyJudge);
		grp.add(contestJudge);
	}
	
	private Judge getJudge() {
		
		return this.judge;
	}
	
	/**
	 * Oeffnet einen Auswahldialog fuer die Schiedsrichter-Instanz
	 * @param parent Der Frame, in dem der Auswahldialog laeuft
	 * @return Der Schiedsrichter
	 */
	public static Judge showJudgeChooserDialog(Frame parent) {
		
		JudgeChooser jc = new JudgeChooser(parent);
		
		return jc.getJudge();
	}
}
