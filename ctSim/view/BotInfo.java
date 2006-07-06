package ctSim.view;

import java.util.List;

import ctSim.model.bots.Bot;
import ctSim.model.bots.components.Actuator;
import ctSim.model.bots.components.Sensor;
import ctSim.model.bots.components.Position;

public final class BotInfo {
	
	private String name;
	private String type;
	
	// TODO: auch view (?)
	private Bot      bot;
	private BotPanel botPanel;
	
	
	
	public BotInfo(String name, String type, Bot bot, BotPanel panel) {
		
		// TODO: Bot adden!
		this.name     = name;
		this.type     = type;
		this.bot      = bot;
		this.botPanel = panel;
	}
	
	public String getName() {
		return name;
	}
	
	protected void setName(String name) {
		this.name = name;
	}
	
	public Bot getBot() {
		return this.bot;
	}
	
	public BotPanel getBotPanel() {
		return botPanel;
	}
	
//	protected void setBotPanel(BotPanel panel) {
//		this.botPanel = panel;
//	}
	
	public String getType() {
		return type;
	}
	
	// TODO:
	public List<Actuator> getActuators() {
		
		return this.bot.getActuators();
	}

	public List<Position> getPositions() {
		
		return this.bot.getPositions();
	}
	
	public List<Sensor> getSensors() {
		
		return this.bot.getSensors();
	}
}
