package ctSim.util;

import java.lang.reflect.Method;

/**
 * Buisitor-Klasse
 */
public class Buisitor {
	/** Ziel */
	private final Object target;

	/**
	 * @param invocationTarget
	 */
	public Buisitor(Object invocationTarget) {
		this.target = invocationTarget;
	}

	/**
	 * @param args Objekte
	 * @return Anzahl
	 */
	@SuppressWarnings("unchecked")
	public int dispatchBuisit(Object... args) {
		Class[] argTypes = new Class[args.length];
		for (int i = 0; i < args.length; i++)
			argTypes[i] = args[i].getClass();
		int numInvocations = 0;

		for (Method m : target.getClass().getMethods()) {
			if (! m.getName().startsWith("buisit"))
				continue;
			try {
				if (hasSignature(m, argTypes)) {
					m.invoke(target, args);
					numInvocations++;
				}
			} catch (Exception e) {
				/* keine Anzeige */
				return 0;
			}
		}
		return numInvocations;
	}

	/**
	 * Checkt eine Signatur
	 * @param m Methode
	 * @param types Typen
	 * @return true / false
	 */
	@SuppressWarnings("unchecked")
	private static boolean hasSignature(Method m, Class... types) {
		Class[] pt = m.getParameterTypes();
		if (pt.length != types.length)
			return false;
		for (int i = 0; i < pt.length; i++) {
			if (! isSupertype(pt[i], types[i]))
				return false;
		}
		return true;
	}

	/**
	 * Checkt einen Tyen
	 * @param supertype
	 * @param subtype
	 * @return true / false 
	 */
	private static boolean isSupertype(Class<?> supertype, Class<?> subtype) {
		// isAssignableFrom() geht mit Referenztypen richtig um; beruecksichtigt
		// Autoboxing/-unboxing aber nicht
		if (supertype.isAssignableFrom(subtype))
			return true;
		else {
			// Primitive muessen wir zu Fuss pruefen
			if (isBoolean(supertype))
				return isBoolean(subtype);
			//$$ Restliche 8 Primitivtypen pruefen
			return false;
		}
	}

	/**
	 * @param type
	 * @return true, falls type ein boolean ist
	 */
	private static boolean isBoolean(Class<?> type) {
		return type.equals(Boolean.class) || type.equals(boolean.class);
	}
}
