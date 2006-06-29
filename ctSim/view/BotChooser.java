package ctSim.view;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import ctSim.controller.Controller;
import ctSim.model.bots.Bot;

public class BotChooser extends JDialog {
	
	private JButton testBot, cBot;
	
	private Controller controller;
	
	private Bot bot;
	
	BotChooser(Frame own, Controller ctrl) {
		
		super(own, "Bot wählen...", true);
		
		this.controller = ctrl;
		
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		//Box buttons = new Box(BoxLayout.PAGE_AXIS);
		JPanel buttons = new JPanel(new GridLayout(1, 4)); // TODO: 4, 1);
		
		initButtons();
		
		buttons.add(this.testBot);
		buttons.add(this.cBot);
		
		this.add(buttons);
		
		this.setLocationRelativeTo(own);
		
		this.pack();
		this.setVisible(true);
	}
	
	private void initButtons() {
		
		this.testBot = new JButton("TestBot");
		this.cBot    = new JButton("CBot");
		
		this.testBot.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				
				addTestBotClicked();
			}
		});
		this.cBot.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				
				addCBotClicked();
			}
		});
	}
	
	private void addTestBotClicked() {
		
		this.bot = this.controller.addBot("CtBotSimTest");
		this.dispose();
	}
	
	private void addCBotClicked() {
		
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				
				return (f.isDirectory() || f.getName().endsWith(".exe") || f.getName().endsWith(".elf"));
			}

			@Override
			public String getDescription() {
				
				return "Bot-Controller (*.exe, *.elf)";
			}
		});
		
		if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			// TODO:
			//this.bot = 
				this.controller.invokeBot(fc.getSelectedFile().getAbsolutePath());
			this.dispose();
		}
	}
	
	private Bot getBot() {
		
		return this.bot;
	}
	
	public static Bot showBotChooserDialog(Frame parent, Controller ctrl) {
		
		BotChooser bc = new BotChooser(parent, ctrl);
		
		return bc.getBot();
	}
}
