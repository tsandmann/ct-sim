package ctSim.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

//$$ doc
public class Buisitor {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Buisit {
		// Marker-Annotation
	}

	private final Object target;

	public Buisitor(Object invocationTarget) {
		this.target = invocationTarget;
	}

	public int dispatchBuisit(Object... args) {
		Class[] argTypes = new Class[args.length];
		for (int i = 0; i < args.length; i++)
			argTypes[i] = args[i].getClass();
		int numInvocations = 0;

		for (Method m : target.getClass().getMethods()) {
			if (! m.isAnnotationPresent(Buisit.class))
				continue;
			try {
				if (hasSignature(m, argTypes)) {
					m.invoke(target, args);
					numInvocations++;
				}
			} catch (Exception e) {
				// kann nicht passieren ... eigentlich ...
				throw new AssertionError(e);
			}
		}
		return numInvocations;
	}

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

	private static boolean isBoolean(Class<?> type) {
		return type.equals(Boolean.class) || type.equals(boolean.class);
	}
}
