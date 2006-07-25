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

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import ctSim.ConfigManager;
import ctSim.controller.Controller;
import ctSim.model.bots.Bot;

/**
 * Dialog zur Auswahl von Bots verschiedenen Typs
 * 
 * @author Felix Beckwermert
 */
public class BotChooser extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JButton testBot, cBot;
	
	private Controller controller;
	
	private Bot bot;
	
	/**
	 * Der Konstruktor
	 * @param own Der Frame, in dem der Chooser laeuft
	 * @param ctrl Die Controller-Instanz
	 */
	BotChooser(Frame own, Controller ctrl) {
		
		super(own, "Bot waehlen...", true); //$NON-NLS-1$
		
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
		
		this.testBot = new JButton("TestBot"); //$NON-NLS-1$
		this.cBot    = new JButton("CBot"); //$NON-NLS-1$
		
		this.testBot.addActionListener(new ActionListener() {

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				addTestBotClicked();
			}
		});
		this.cBot.addActionListener(new ActionListener() {

			/* 
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@SuppressWarnings("synthetic-access")
			public void actionPerformed(@SuppressWarnings("unused") ActionEvent e) {
				
				addCBotClicked();
			}
		});
	}
	
	private void addTestBotClicked() {
		
		this.bot = this.controller.addBot("CtBotSimTest"); //$NON-NLS-1$
		this.dispose();
	}
	
	private void addCBotClicked() {
		
		JFileChooser fc = new JFileChooser(".");
		
		if(ConfigManager.getConfigValue("botdir") != null)
			fc = new JFileChooser(ConfigManager.getConfigValue("botdir")); //$NON-NLS-1$
		
		fc.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				
				return (f.isDirectory() || f.getName().endsWith(".exe") || f.getName().endsWith(".elf")); //$NON-NLS-1$ //$NON-NLS-2$
			}

			@Override
			public String getDescription() {
				
				return "Bot-Controller (*.exe, *.elf)"; //$NON-NLS-1$
			}
		});
		
		if(fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			// TODO:
			//this.bot = 
				this.controller.invokeBot(fc.getSelectedFile().getAbsolutePath());
		}
		
		this.dispose();
	}
	
	private Bot getBot() {
		
		return this.bot;
	}
	
	/**
	 * Zeigt den Bot-Auswahldialog
	 * @param parent Der Frame, in dem der Dialog laeuft
	 * @param ctrl Die betreffende Controller-Instanz
	 * @return Der ausgewaehlte Bot
	 */
	public static Bot showBotChooserDialog(Frame parent, Controller ctrl) {
		
		BotChooser bc = new BotChooser(parent, ctrl);
		
		return bc.getBot();
	}
}
