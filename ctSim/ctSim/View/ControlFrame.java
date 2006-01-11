package ctSim.View;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import javax.swing.WindowConstants;

import ctSim.ErrorHandler;
import ctSim.Model.*;
import ctSim.Controller.Controller;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class ControlFrame extends javax.swing.JFrame {
	/**
	 * Bildet den Rahmen, der die ControlPanels für alle Bots enthält. 
	 */
	private static final long serialVersionUID = 1L;
	private JTabbedPane controlPanels;

	private World world;

	private boolean haveABreak;
	
	/** Interne Zeitbasis in Millisekunden.*/
	protected long simulTime=0;
	private JPanel jPanel1;
	private JButton startButton;

	public ControlFrame() {
		super();
		haveABreak = false;
		world = Controller.getWorld();
		initGUI();
	}
	
	private void initGUI() {
		try {
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			BoxLayout thisLayout = new BoxLayout(
				getContentPane(),
				javax.swing.BoxLayout.Y_AXIS);
			getContentPane().setLayout(thisLayout);
			this.setTitle("Control Panel");
			{
				jPanel1 = new JPanel();
				BoxLayout jPanel1Layout = new BoxLayout(
					jPanel1,
					javax.swing.BoxLayout.Y_AXIS);
				getContentPane().add(jPanel1);
				jPanel1.setLayout(jPanel1Layout);
				{
					startButton = new JButton();
					jPanel1.add(startButton);
					if (!haveABreak){
					startButton.setText("Pause");
					} else {
						startButton.setText("Resume");							
					}
					startButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent evt) {
							haveABreak = !haveABreak;
							world.setHaveABreak(haveABreak);
							if (!haveABreak){
								startButton.setText("Pause");
								} else {
									startButton.setText("Resume");							
								}

						}
					});

				}
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
//		while (true){
//		work();
//		}
	}

	protected void work() {
		try {
			// warten bis World den naechsten Schritt macht:
			simulTime = world.getSimulTime();		
			System.out.println("ControlFrame refreshes at simulTime "+simulTime);
			this.repaint();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void addBot(Bot bot){
//		ControlPanel newPanel = new CtControlPanel((CtBot)bot);
// 		bot.setPanel(newPanel);
		controlPanels.addTab(bot.getBotName(), null, bot.getPanel(), null);
		pack();
	}
}
