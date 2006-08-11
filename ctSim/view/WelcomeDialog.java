package ctSim.view;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;

public class WelcomeDialog extends JDialog {
	
	private JPanel panel;
	
	WelcomeDialog() {
		
		this.setTitle("Sag hallo zur neuen Wettbewerbs-Version des Ct-Sim");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		this.panel = new JPanel(new BorderLayout());
		
		this.add(this.panel);
		
		
		
		this.pack();
	}
	
	
	
	public static void showWelcomeDialog() {
		
		WelcomeDialog wd = new WelcomeDialog();
		
		wd.setVisible(true);
	}
}
