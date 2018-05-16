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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Decoratoror */
public class Decoratoror {
	/**
	 * @param concreteMethod
	 * @param abstractMethod
	 * @return true / false
	 */
	public static boolean doesImplement(Method concreteMethod,
	Method abstractMethod) {
		if (! concreteMethod.getName().equals(abstractMethod.getName()))
			return false;
		// return type has to be the same or a specialisation
		if (! abstractMethod.getReturnType().isAssignableFrom(
			concreteMethod.getReturnType()))
			return false;

		Class<?>[] concParms = concreteMethod.getParameterTypes();
		Class<?>[] abstParms = abstractMethod.getParameterTypes();
		if (concParms.length != abstParms.length)
			return false;
		for (int i = 0; i < concParms.length; i++) {
			if (! concParms[i].equals(abstParms[i]))
				return false;
		}
		return true;
	}

	/**
	 * @param o
	 * @param abstractMethod
	 * @return true / false
	 */
	public static boolean providesImplementationOf(Object o,
	Method abstractMethod) {
		for (Method m : o.getClass().getMethods()) {
			if (doesImplement(m, abstractMethod))
				return true;
		}
		return false;
	}

	/**
	 * @param decorators	Objekte
	 * @param m				Methode
	 * @return Objekt
	 * @throws NoSuchMethodException
	 */
	private static Object findImplementor(Object[] decorators, Method m)
	throws NoSuchMethodException {
		for (int i = decorators.length - 1; i >= 0; i--) {
			if (providesImplementationOf(decorators[i], m))
				return decorators[i];
		}
		throw new NoSuchMethodException("None of the decorators implements " + "method '" + m + "'");
	}

	/**
	 * @param <T>
	 * @param resultInterface
	 * @param decorators
	 * @return T
	 * @throws NoSuchMethodException
	 */
	public static <T> T createDecorated(
	Class<T> resultInterface, Object... decorators)
	throws NoSuchMethodException {
		// Sanity check - newProxyInstance will also report that, but to be clear and explicit ...
		if (! resultInterface.isInterface()) {
			throw new IllegalArgumentException("First argument must " + "represent an interface");
		}
		// will also fail down the line, but to have a clearer error message
		for (int i = 0; i < decorators.length; i++) {
			if (decorators[i] == null)
				throw new NullPointerException("Decorator #" + i + " is null");
		}

		final Map<Method, Object> methodImpls = Misc.newMap();
		for (Method ifcMeth : resultInterface.getMethods())
			methodImpls.put(ifcMeth, findImplementor(decorators, ifcMeth));

		/*
		 * Gotcha: Object's methods don't show up in resultInterface.getMethods(),
		 * but we need entries in methodImpls for them
		 */
		for (Method objMeth : Object.class.getMethods())
			// NoSuchMethodException is impossible here
			methodImpls.put(objMeth, findImplementor(decorators, objMeth));

		// actual work
		return (T)Proxy.newProxyInstance(
			ClassLoader.getSystemClassLoader(),
			new Class[] { resultInterface },
			new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return method.invoke(methodImpls.get(method), args);
				}
		});
	}
}
