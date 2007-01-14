package ctSim.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

//$$ dococ
public class Decoratoror {
	//$$ Geht das nicht einfacher? Sind Method-Objekte nicht Singletons oder so? Dann wuerde "==" reichen
	public static boolean doesImplement(Method concreteMethod,
	Method abstractMethod) {
		if (! concreteMethod.getName().equals(abstractMethod.getName()))
			return false;
		// return type has to be the same or a specialization
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

	public static boolean providesImplementationOf(Object o,
	Method abstractMethod) {
		for (Method m : o.getClass().getMethods()) {
			if (doesImplement(m, abstractMethod))
				return true;
		}
		return false;
	}

	// throws NoSuchMethodException like e.g. Class.getMethod() does
	private static Object findImplementor(Object[] decorators, Method m)
	throws NoSuchMethodException {
		for (int i = decorators.length - 1; i >= 0; i--) {
			if (providesImplementationOf(decorators[i], m))
				return decorators[i];
		}
		throw new NoSuchMethodException("None of the decorators implements " +
				"method '"+m+"'");
	}

	public static <T> T createDecorated(
	Class<T> resultInterface, Object... decorators)
	throws NoSuchMethodException {
		// Sanity check -- newProxyInstance will also report that, but to
		// be clear and explicit ...
		if (! resultInterface.isInterface()) {
			throw new IllegalArgumentException("First argument must " +
			"represent an interface");
		}
		// Will also fail down the line, but to have a clearer error message
		for (int i = 0; i < decorators.length; i++) {
			if (decorators[i] == null)
				throw new NullPointerException("Decorator #"+i+" is null");
		}

		final Map<Method, Object> methodImpls = new HashMap<Method, Object>();
		for (Method ifcMeth : resultInterface.getMethods())
			methodImpls.put(ifcMeth, findImplementor(decorators, ifcMeth));

		/*
		 * Gotcha: Object's methods don't show up in
		 * resultInterface.getMethods(), but we need entries in methodImpls for
		 * them
		 */
		for (Method objMeth : Object.class.getMethods())
			// NoSuchMethodException is impossible here
			methodImpls.put(objMeth, findImplementor(decorators, objMeth));

		// Actual work
		return (T)Proxy.newProxyInstance(
			ClassLoader.getSystemClassLoader(),
			new Class[] { resultInterface },
			new InvocationHandler() {
				public Object invoke(@SuppressWarnings("unused") Object proxy,
					Method method, Object[] args) throws Throwable {
					return method.invoke(methodImpls.get(method), args);
				}
		});
	}
}
