package ctSim.util;

import java.util.BitSet;

//$$ doc
//$$ Glaub ich hab da nur eine seltsame Art EnumSet implementiert
// Konvention: 0 = LSB, 63 = MSB
public class Flags<T extends Enum<?>> {
	private static final long serialVersionUID = 3185762205479976647L;

	private final BitSet carrier = new BitSet();

	public Flags() {
		// no-op
	}

	public Flags(T... initialValues) {
		if (initialValues == null)
			return;
		for (T iv : initialValues)
			set(iv);
	}

	public boolean get(T bitIndex) {
		return carrier.get(bitIndex.ordinal());
	}

	public boolean getEach(T[] bitIndices) {
		for (T b : bitIndices) {
			if (! get(b))
				return false;
		}
		return true;
	}

	public boolean getAny(T[] bitIndices) {
		for (T b : bitIndices) {
			if (get(b))
				return true;
		}
		return false;
	}

	public Flags<T> set(T bitIndex) {
		carrier.set(bitIndex.ordinal());
		return this;
	}

	public Flags<T> clear(T bitIndex) {
		carrier.clear(bitIndex.ordinal());
		return this;
	}
}
