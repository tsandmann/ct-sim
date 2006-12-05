package ctSim.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;

public class Decoratoror {
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

	@SuppressWarnings("unchecked")
	public static <T> T createDecorated(Class<T> resultInterface,
		Object... decorators) {
		// Sanity check -- newProxyInstance will also report that, but to
		// be clear and explicit ...
		if (! resultInterface.isInterface()) {
			throw new IllegalArgumentException("First argument must " +
			"represent an interface");
		}

		final HashMap<Method, Object> methodImpls =
			new HashMap<Method, Object>();
		for (Method ifcMethod : resultInterface.getMethods()) {
			for (int i = decorators.length - 1; i >= 0; i--) {
				if (providesImplementationOf(decorators[i], ifcMethod))
					methodImpls.put(ifcMethod, decorators[i]);
			}
		}

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
