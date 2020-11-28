/*
 * c't-Sim - Robotersimulator für den c't-Bot
 *
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307, USA.
 *
 */

package ctSim.util;

import java.lang.reflect.Method;

/** Buisitor-Klasse */
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
	 * @param args	Objekte
	 * @return Anzahl
	 */
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
	 * Überprüft eine Signatur
	 *
	 * @param m		Methode
	 * @param types	Typen
	 * @return true / false
	 */
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
	 * Überprüft einen Type
	 *
	 * @param supertype
	 * @param subtype
	 * @return true / false
	 */
	private static boolean isSupertype(Class<?> supertype, Class<?> subtype) {
		// isAssignableFrom() geht mit Referenztypen richtig um; berücksichtigt Autoboxing/-unboxing aber nicht
		if (supertype.isAssignableFrom(subtype)) {
			return true;
		} else {
			// Primitive müssen wir separat prüfen
			if (isBoolean(supertype)) {
				return isBoolean(subtype);
			}
			if (isByte(supertype)) {
				return isByte(subtype);
			}
			if (isShort(supertype)) {
				return isShort(subtype);
			}
			if (isInt(supertype)) {
				return isInt(subtype);
			}
			if (isLong(supertype)) {
				return isLong(subtype);
			}
			if (isFloat(supertype)) {
				return isFloat(subtype);
			}
			if (isDouble(supertype)) {
				return isDouble(subtype);
			}
			if (isChar(supertype)) {
				return isChar(subtype);
			}
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
	
	/**
	 * @param type
	 * @return true, falls type ein byte ist
	 */
	private static boolean isByte(Class<?> type) {
		return type.equals(Byte.class) || type.equals(byte.class);
	}
	
	/**
	 * @param type
	 * @return true, falls type ein short ist
	 */
	private static boolean isShort(Class<?> type) {
		return type.equals(Short.class) || type.equals(short.class);
	}
	
	/**
	 * @param type
	 * @return true, falls type ein int ist
	 */
	private static boolean isInt(Class<?> type) {
		return type.equals(Integer.class) || type.equals(int.class);
	}
	
	/**
	 * @param type
	 * @return true, falls type ein long ist
	 */
	private static boolean isLong(Class<?> type) {
		return type.equals(Long.class) || type.equals(long.class);
	}
	
	/**
	 * @param type
	 * @return true, falls type ein float ist
	 */
	private static boolean isFloat(Class<?> type) {
		return type.equals(Float.class) || type.equals(float.class);
	}
	
	/**
	 * @param type
	 * @return true, falls type ein double ist
	 */
	private static boolean isDouble(Class<?> type) {
		return type.equals(Double.class) || type.equals(double.class);
	}
	
	/**
	 * @param type
	 * @return true, falls type ein char ist
	 */
	private static boolean isChar(Class<?> type) {
		return type.equals(Character.class) || type.equals(char.class);
	}
}
