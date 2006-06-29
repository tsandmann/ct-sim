package ctSim.view;

import javax.swing.JPanel;

public abstract class BotPanel extends JPanel {
	
	private BotInfo botInfo = null;
	private boolean updated = false;
	
	// TODO: erst Panel oder erst Info???
	BotPanel() {
		
		super();
	}
	
//	BotPanel(BotInfo botInfo) {
//		
//		super();
//		
//		this.botInfo = botInfo;
//		botInfo.setBotPanel(this);
//	}
	
	protected final BotInfo getBotInfo() {
		
		return this.botInfo;
	}
	
	private final void setBotInfo(BotInfo botInfo) {
		
		if(botInfo.getBotPanel() != this)
			return;
		
		// Überflüssig: (?)
		if(!updated)
			this.botInfo = botInfo;
		
		// TODO: Sonst Error
	}
	
	protected final void init(BotInfo botInfo) {
		
		this.setBotInfo(botInfo);
		
		this.initGUI();
	}
	
	protected final void update() {
		
		if(this.botInfo == null) {
			// TODO: Error
			System.err.println("Error: BotInfo not set!");
			System.exit(-1);
		}
		
		this.updated = true;
		updateGUI();
	}
	
	protected abstract void initGUI();
	protected abstract void updateGUI();
	
	// TODO: ???
//	@Override
//	public abstract String getName();
}
