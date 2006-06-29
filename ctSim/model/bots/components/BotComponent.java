package ctSim.model.bots.components;


public abstract class BotComponent {
	
	private static int ID_COUNT = 0;
	
	private int id;
	
	private String name;
	
	// TODO:
	private String relPos;
	private double relHead;
	
	// TODO: Darstellung usw. (?)
	
	public BotComponent(String name, String relativePosition, double relativeHeading) {
		
		this.name    = name;
		this.relPos  = relativePosition;
		this.relHead = relativeHeading;
		
		this.id = ID_COUNT++;
	}
	
	public int getId() {
		
		return this.id;
	}
	
	public String getName() {
		return name;
	}

	public double getRelHead() {
		return relHead;
	}

	public String getRelPos() {
		return relPos;
	}

	public abstract String getType();
	
	public abstract String getDescription();
}
