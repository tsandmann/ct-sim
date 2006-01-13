package ctSim.View;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import javax.swing.WindowConstants;

import ctSim.ErrorHandler;
import ctSim.Model.*;
import ctSim.Controller.Controller;

public class ControlFrame extends javax.swing.JFrame {
	/**
	 * Bildet den Rahmen, der die ControlPanels für alle Bots enthält. 
	 */
	private static final long serialVersionUID = 1L;
	private JTabbedPane controlPanels;
	private World world;
	private boolean haveABreak;
	private JPanel buttonPanel;
	private JButton pauseButton;
	private JButton endButton;

	public ControlFrame() {
		super();
		haveABreak = false;
		world = Controller.getWorld();
		initGUI();
	}
	
	private void initGUI() {
		
		Dimension buttDim = new Dimension (20, 70);
		
		try {
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			BoxLayout thisLayout = new BoxLayout(
				getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
			getContentPane().setLayout(thisLayout);
			this.setTitle("Kontrolltafel");
			{
				buttonPanel = new JPanel();
				BoxLayout panelLayout = new BoxLayout(
					buttonPanel,
					javax.swing.BoxLayout.X_AXIS);
				getContentPane().add(buttonPanel);
				buttonPanel.setLayout(panelLayout);
				{
					pauseButton = new JButton();
					pauseButton.setPreferredSize(buttDim);
					buttonPanel.add(pauseButton);
					if (!haveABreak){
					pauseButton.setText("Pause");
					} else {
						pauseButton.setText("Resume");							
					}
					pauseButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							haveABreak = !haveABreak;
							world.setHaveABreak(haveABreak);
							if (!haveABreak){
								pauseButton.setText("Pause");
								} else {
									pauseButton.setText("Weiter");							
								}

						}
					});

				}
				{
					endButton = new JButton();
					endButton.setPreferredSize(buttDim);
					buttonPanel.add(endButton);
					endButton.setText("Beenden");
					}
					endButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							Controller.endSim();							
						}
					});

				}

			
			{
				controlPanels = new JTabbedPane();
				getContentPane().add(controlPanels);
			}
			setSize(300, 200);
			pack();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void dispose (){
		Controller.endSim();
		super.dispose();
	}
	
	public void addBot(Bot bot){
		// Ein neues Panel fuer den Bot erzeugen
		controlPanels.addTab(bot.getBotName(), null, bot.getPanel(), null);
		// Dem Panel anzeigen, wo es dargestellt wird 
		bot.getPanel().setFrame(this);
		pack();
	}

	/**
	 * @return Gibt controlPanels zurueck.
	 */
	public JTabbedPane getControlPanels() {
		return controlPanels;
	}
}
