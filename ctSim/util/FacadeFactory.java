package ctSim.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FacadeFactory {
	// different name
	@SuppressWarnings("unchecked")
	public static <F> F createFacadeSimple(
		Class<F> facadeInterface, Object... implementations) {
		// Infer which implementation object implements which of
		// facadeInterface's superinterfaces
		Map<Class<?>, Object> ifcImplMap = new HashMap<Class<?>, Object>();
		for (Object implementation : implementations) {
			for (Class<?> ifc : facadeInterface.getInterfaces()) {
				if (ifc.isAssignableFrom(implementation.getClass())) {
					// Found the guy implementing ifc
					Object previous = ifcImplMap.put(ifc, implementation);
					if (previous != null) {
						throw new IllegalArgumentException("Ambiguity " +
								"while inferring types: '" + ifc + "' is " +
								"implemented by both '" + previous +
								"' and '" + implementation + "'. Use " +
								"createFacade(Class, Map) to explicitly" +
								"specify what you mean.");
					}
				}
			}
		}

		return createFacade(facadeInterface, ifcImplMap);
	}

	// adv: create for arbitrary amt of things
	@SuppressWarnings("unchecked")
	public static <F> F createFacade(Class<F> facadeInterface,
		final Map<Class<?>, Object> implementations) {
		// Sanity check -- newProxyInstance will also report that, but to
		// be clear and explicit ...
		if (! facadeInterface.isInterface()) {
			throw new IllegalArgumentException("First argument must " +
					"represent an interface");
		}

		// Sanity check that partials are compatible with facade ifc
		for (Class<?> ifc : implementations.keySet()) {
			if (ifc.equals(facadeInterface)) {
				throw new IllegalArgumentException("The type '" + ifc +
					"' is the same as the facade interface '" +
					facadeInterface + "'.");
			}
			if (! ifc.isAssignableFrom(facadeInterface)) {
				throw new IllegalArgumentException("Each partial interface " +
						"must be a subtype of the facade interface '" +
						facadeInterface.getCanonicalName() + "'; Interface '"+
						ifc.getCanonicalName() + "' is not a " +
						"subtype");
			}
		}

		// Sanity check that we got all implementations we need
		Set<Class> ifcWithoutImpl = new HashSet<Class>(Arrays.asList(
			facadeInterface.getInterfaces()));
		ifcWithoutImpl.removeAll(implementations.keySet());
		if (! ifcWithoutImpl.isEmpty()) {
			throw new IllegalArgumentException("No implementation was " +
					"supplied for the following interface(s): " +
					ifcWithoutImpl);
		}

		// Sanity check that we don't have more implementations than we need
		Set<Class> leftoverImpls = new HashSet<Class>(implementations.keySet());
		leftoverImpls.removeAll(Arrays.asList(facadeInterface.getInterfaces()));
		if (! leftoverImpls.isEmpty()) {
			throw new IllegalArgumentException("More implementations than " +
				"necessary were supplied -- the following are left over " +
				leftoverImpls);
		}

		// Actual work
		return (F)Proxy.newProxyInstance(
			ClassLoader.getSystemClassLoader(),
			new Class[] { facadeInterface },
			new InvocationHandler() {
				public Object invoke(@SuppressWarnings("unused") Object proxy,
					Method method, Object[] args) throws Throwable {
					return method.invoke(
						implementations.get(method.getDeclaringClass()),
						args);
				}
		});
	}
}
