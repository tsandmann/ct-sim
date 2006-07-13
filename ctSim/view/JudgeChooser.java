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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
public class JudgeChooser extends JDialog implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String judge;
	
	private JButton ok;
	
	private Box buttons;
	//private JRadioButton defJudge, contestJudge, labyJudge;
	
	// Add Judges here; first one should be default:
	private final String[] judges = new String[] {"ctSim.model.rules.DefaultJudge", "ctSim.model.rules.LabyrinthJudge"}; // "LabyrinthContestJudge"
	
	/**
	 * Der Konstruktor
	 * @param own Der Frame, in dem der Auswahldialog laeuft
	 */
	JudgeChooser(Frame own) {
		
		super(own, "Schiedsrichter waehlen...", true);
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.setLayout(new BorderLayout());
		
		buttons = new Box(BoxLayout.PAGE_AXIS);
		
		buttons.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		
		buttons.add(new JLabel("Waehlen Sie einen Schiedsrichter:"));
		
		initRadioButtons();
		
		//buttons.add(this.defJudge);
		//buttons.add(this.labyJudge);
		//buttons.add(this.contestJudge);
		
		this.ok = new JButton("Ok");
		this.ok.addActionListener(this);
		
		this.add(buttons, BorderLayout.CENTER);
		this.add(ok, BorderLayout.SOUTH);
		
		this.setLocationRelativeTo(own);
		
		this.pack();
		this.setVisible(true);
	}
	
	private void initRadioButtons() {
		
		boolean first = true;
		
		ButtonGroup grp = new ButtonGroup();
		
		for(String s : this.judges) {
			
			JRadioButton but = new JRadioButton(s);
			but.setActionCommand(s);
			but.addActionListener(this);
			
			if(first) {
				but.setSelected(true);
				this.judge = s;
				
				first = false;
			}
			
			grp.add(but);
			this.buttons.add(but);
		}
	}
	
	private String getJudge() {
		
		return this.judge;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource() == this.ok) {
			this.dispose();
			return;
		}
		
	    this.judge = e.getActionCommand();
	}
	
	/**
	 * Oeffnet einen Auswahldialog fuer die Schiedsrichter-Instanz
	 * @param parent Der Frame, in dem der Auswahldialog laeuft
	 * @return Der Schiedsrichter
	 */
	public static String showJudgeChooserDialog(Frame parent) {
		
		JudgeChooser jc = new JudgeChooser(parent);
		
		return jc.getJudge();
	}
}
