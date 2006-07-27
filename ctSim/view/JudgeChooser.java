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
	private final String[] judges = new String[] {"ctSim.model.rules.DefaultJudge", "ctSim.model.rules.LabyrinthJudge"}; // "LabyrinthContestJudge"  //$NON-NLS-1$//$NON-NLS-2$
	
	/**
	 * Der Konstruktor
	 * @param own Der Frame, in dem der Auswahldialog laeuft
	 */
	JudgeChooser(Frame own, String selectedJudge) {
		
		super(own, "Schiedsrichter waehlen...", true); //$NON-NLS-1$
		
		this.judge = selectedJudge;
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.setLayout(new BorderLayout());
		
		this.buttons = new Box(BoxLayout.PAGE_AXIS);
		
		this.buttons.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		
		this.buttons.add(new JLabel("Waehlen Sie einen Schiedsrichter:")); //$NON-NLS-1$
		
		initRadioButtons();
		
		//buttons.add(this.defJudge);
		//buttons.add(this.labyJudge);
		//buttons.add(this.contestJudge);
		
		this.ok = new JButton("Ok"); //$NON-NLS-1$
		this.ok.addActionListener(this);
		
		this.add(this.buttons, BorderLayout.CENTER);
		this.add(this.ok, BorderLayout.SOUTH);
		
		this.setLocationRelativeTo(own);
		
		this.pack();
		this.setVisible(true);
	}
	
	private void initRadioButtons() {
		
		ButtonGroup grp = new ButtonGroup();
		
		for(String s : this.judges) {
			
			JRadioButton but = new JRadioButton(s);
			but.setActionCommand(s);
			but.addActionListener(this);
			
			if(this.judge == null || this.judge.equals(s)) {
				but.setSelected(true);
				this.judge = s;
			}
			
			grp.add(but);
			this.buttons.add(but);
		}
	}
	
	private String getJudge() {
		
		return this.judge;
	}
	
	/** 
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
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
	public static String showJudgeChooserDialog(Frame parent, String selectedJudge) {
		
		JudgeChooser jc = new JudgeChooser(parent, selectedJudge);
		
		return jc.getJudge();
	}
}
