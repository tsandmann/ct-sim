package ctSim.model.bots.ctbot.components;

import ctSim.model.Command.Code;
import ctSim.model.bots.components.NumberTwin;
import ctSim.model.bots.components.BotComponent.CanWrite;
import ctSim.model.bots.components.BotComponent.SimpleSensor;

//$$ doc
public class MouseSensor extends NumberTwin implements CanWrite, SimpleSensor {
	/**
	 * {@code true}: Diese Instanz verwaltet die X-Komponente, {@code false}:
	 * Y-Komponente. Im Konstruktor wird dieser Wert an den Superkonstruktor
	 * gegeben, wo er entscheidet, ob unser Wert im dataL- oder dataR-Feld eines
	 * Commands &uuml;bermittelt wird (sowohl Schreiben als auch Lesen). Die
	 * Zuordnung ist also X = dataL, Y = dataR.
	 */
	private boolean isX;

	public MouseSensor(boolean isX) {
		super(isX);
		this.isX = isX;
	}

	@Override
	public String getName() {
		return "Mouse" + (isX ? "X" : "Y");
	}

	@Override
	public String getDescription() {
		return "Maus-Sensor (" +
			(isX ? "X" : "Y") +
			"-Komponente)";
	}

	@Override protected String getBaseDescription() { return ""; }
	@Override protected String getBaseName() { return ""; }
	public Code getHotCmdCode() { return Code.SENS_MOUSE; }
}