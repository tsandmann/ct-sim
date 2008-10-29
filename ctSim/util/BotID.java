package ctSim.util;

/**
 * Datentyp fuer Bot-IDs. Im Prinzip wie byte, gibt das Byte
 * aber als unsigned aus.
 * mit set() kann man der ID einen neuen Wert geben,
 * mit equals() kann man die ID und eine andere auf
 * Gleichheit pruefen.
 * Die get-Methoden sollten immer eine neue ID erstellen, 
 * (wg. call by referenz)
 * @author Timo Sandmann (mail@timosandmann.de)
 */
public class BotID extends java.lang.Number {
	/** UID */
	private static final long serialVersionUID = 2020798275037223330L;
	
	/** Datenwert */
	private int data = 0;

	/**
	 * Erzeugt eine neue ID (unsígned byte)
	 * @param id Wert der ID
	 */
	public BotID(byte id) {
		this.data = id;
		if (this.data < 0) {
			this.data += 256;
		}
	}
	
	/**
	 * Erzeugt eine neue ID (unsígned byte)
	 * @param id Wert der ID
	 */
	public BotID(int id) {
		this((byte)id);
	}	

	/**
	 * Erzeugt eine neue ID (unsígned byte)
	 * @param id Wert der ID
	 */
	public BotID(BotID id) {
		this(id.byteValue());
	}	
	
	/**
	 * Erzeugt eine neue ID (unsígned byte)
	 */
	public BotID() {
		this(0);
	}

	/**
	 * @see java.lang.Number#doubleValue()
	 */
	@Override
	public double doubleValue() {
		return intValue();
	}

	/**
	 * @see java.lang.Number#floatValue()
	 */
	@Override
	public float floatValue() {
		return intValue();
	}

	/**
	 * @see java.lang.Number#intValue()
	 */
	@Override
	public int intValue() {
		return this.data;
	}

	/**
	 * @see java.lang.Number#longValue()
	 */
	@Override
	public long longValue() {
		return intValue();
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.valueOf(intValue());
	}

	/**
	 * Setzt die ID auf einen neuen Wert
	 * @param id neuer Wert
	 */
	public void set(byte id) {
		int tmp = id;
		if (tmp < 0) {
			tmp += 256;
		}
		this.data = tmp;
	}
	
	/**
	 * Setzt die ID auf einen neuen Wert
	 * @param id neuer Wert
	 */
	public void set(int id) {
		set((byte)id); 
	}
	
	/**
	 * Vergleicht eine andere BotID mit Dieser
	 * @param id andere BotID
	 * @return true, falls beide IDs denselben Wert haben
	 */
	public boolean equals(BotID id) {
		return (this.intValue() == id.intValue()); 
	}
}
