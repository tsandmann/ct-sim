package ctSim.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import ctSim.model.rules.Judge;

public class JudgeChooser extends JDialog {
	
	private Judge judge;
	
	private JRadioButton defJudge, contestJudge, labyJudge;
	
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
	
	public static Judge showJudgeChooserDialog(Frame parent) {
		
		JudgeChooser jc = new JudgeChooser(parent);
		
		return jc.getJudge();
	}
}
