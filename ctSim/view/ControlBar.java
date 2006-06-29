package ctSim.view;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;


public final class ControlBar extends JPanel {
	
	private List<BotInfo> botList;
	
	private JTabbedPane botTabs;
	
	ControlBar() {
		
		super();
		
		this.botList = new ArrayList<BotInfo>();
		
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		//this.botList = new ArrayList<BotInfo>();
		
		//this.botTabs = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
		this.botTabs = new JTabbedPane();
		
		//this.add(Box.createVerticalGlue());
		this.add(botTabs);
		
		//TODO: ???
		//this.setPreferredSize(new Dimension(180, 500));
		//this.setMinimumSize(new Dimension(100, 150));
	}
	
	protected void addBot(BotInfo botInfo) {
		
		// TODO: Erst Panel oder erst Info???
		if(botInfo.getBotPanel() == null)
			return;
		
		//botInfo.getBotPanel().setBotInfo(botInfo);
		BotPanel botPanel = botInfo.getBotPanel();
		botPanel.init(botInfo);
		
		this.botList.add(botInfo);
		
//		JScrollPane scroll = new JScrollPane(botPanel);
//		//, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//		
//		this.botTabs.addTab(botInfo.getName(), null, scroll,
		this.botTabs.addTab(botInfo.getName(), null, botPanel,
				"Bot '"+botInfo.getName()+"' with type '"+botInfo.getType()+"'");
		
		botPanel.invalidate();
		//this.invalidate();
	}
	
	public void update() {
		
		for(BotInfo bi : this.botList) {
			bi.getBotPanel().update();
		}
	}
	
	protected void reinit() {
		
		this.botList = new ArrayList<BotInfo>();
		this.botTabs.removeAll();
	}
}