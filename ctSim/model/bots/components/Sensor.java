package ctSim.model.bots.components;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import ctSim.view.sensors.SensorGroupGUI;
import ctSim.view.sensors.Sensors;

/* not synchronized!! */
public abstract class Sensor<E> extends BotComponent {
	
	private E value;
	private Characteristic characteristic;
	private boolean useGuiValue = false;
	private boolean setable = true;
	
	public Sensor(String name, Point3d relPos, Vector3d relHead) {
		super(name, relPos, relHead);
		// TODO Auto-generated constructor stub
	}
	
	public final void setCharacteristic(Characteristic ch) {
		
		this.characteristic = ch;
	}
	
	public boolean isSetable() {
		
		return this.setable;
	}
	
	public void setIsSetable(boolean b) {
		
		this.setable = b;
	}
	
	public final E getValue() {
		
		return this.value;
	}
	
	/* Sollte nur von GUI aufgerufen werden:
	 * 
	 * - Setzen ist nur erlaubt, wenn entsprechendes flag gesetzt ist
	 * - Wert wird dann beim nächsten update zurückgegeben (kein updateValue()!)
	 * - nächster Aufruf von getVal() geschieht ohne Kennlinien-lookup
	 * 
	 * Vorsicht: Wenn update öfter als get geschieht, bekommt "man" die entsprechende Eingabe
	 *           von Hand/ über die GUI eventuell gar nicht mit (da Wert bereits wieder überschrieben)
	 */
	public final boolean setValue(E value) {
		
		if(!this.isSetable())
			return false;
		
		this.value = value;
		this.useGuiValue = true;
		
		return true;
	}
	
	public final void update() {
		
		if(this.useGuiValue) {
			
			this.useGuiValue = false;
			return;
		}
		
		this.value = updateValue();
		
		// TODO: Äußerst hässlich:
		if(this.characteristic != null) {
			
			this.value = (E)((Double)((Integer)this.characteristic.lookup(((Number)this.value).intValue())).doubleValue());
		}
	}
	
	public abstract E updateValue();
	
	public abstract SensorGroupGUI getSensorGroupGUI();
//	public SensorGroupGUI getSensorGroupGUI() {
//		
//		SensorGroupGUI gui = Sensors.getGuiFor(this);
//		gui.addSensor(this);
//		return gui;
//	}
}
