/**
 *
 */
package ctSim.controller;

import java.util.List;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.DefaultPicoContainer;

/**
 * Initialisierter Pico-Container 
 */
public class InitializingPicoContainer extends DefaultPicoContainer {
    /** UID */
	private static final long serialVersionUID = - 6940983694133437239L;

    /**
     * Initialisierter Pico-Container 
     */
    public InitializingPicoContainer() {
    	super();
    }

    /**
     * Initialisierter Pico-Container 
     * @param parent 
     */
	public InitializingPicoContainer(PicoContainer parent) {
		super(parent);
    }

	/**
	 * Getter
	 * @param <T>
	 * @param componentKey
	 * @return T
	 */
	@SuppressWarnings("unchecked")
    public <T> T get(Class<T> componentKey) {
	    return (T)super.getComponentInstance(componentKey);
	}

	/**
	 * Klasse initialisiert?
	 * @param classToInitialize Klasse
	 */
	private void ensureInitialized(Class<?> classToInitialize) {
		try {
	        Class.forName(classToInitialize.getName());
        } catch (ClassNotFoundException e) {
        	//$$ Obskurer Fehler
        	throw new RuntimeException(e);
        }
	}

	/**
	 * @param classMap
	 */
	public void registerImplementationMap(Class<?>... classMap) {
		if (classMap.length % 2 != 0)
			throw new IllegalArgumentException();
		for (int i = 0; i < classMap.length; i += 2)
			registerComponentImplementation(classMap[i], classMap[i + 1]);
    }

	/**
	 * @param classes
	 */
	public void registerImplementations(Class<?>... classes) {
		for (Class<?> c : classes)
			registerComponentImplementation(c);
	}

	/**
	 * @see org.picocontainer.defaults.DefaultPicoContainer#registerComponentImplementation(java.lang.Class)
	 */
	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
    @Override
    public ComponentAdapter registerComponentImplementation(
    	Class componentImplementation) {
	    ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentImplementation);
    }

	/**
	 * @see org.picocontainer.defaults.DefaultPicoContainer#registerComponentImplementation(java.lang.Object, java.lang.Class)
	 */
	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
	@Override
    public ComponentAdapter registerComponentImplementation(
    	Object componentKey, Class componentImplementation) {
		ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentKey,
	        componentImplementation);
    }

	/**
	 * @see org.picocontainer.defaults.DefaultPicoContainer#registerComponentImplementation(java.lang.Object, java.lang.Class, java.util.List)
	 */
	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
	@Override
    public ComponentAdapter registerComponentImplementation(
    	Object componentKey, Class componentImplementation, List parameters) {
		ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentKey,
	        componentImplementation, parameters);
    }

	/**
	 * @see org.picocontainer.defaults.DefaultPicoContainer#registerComponentImplementation(java.lang.Object, java.lang.Class, org.picocontainer.Parameter[])
	 */
	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
	@Override
    public ComponentAdapter registerComponentImplementation(
    	Object componentKey, Class componentImplementation,
    	Parameter[] parameters) {
		ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentKey,
	        componentImplementation, parameters);
    }

	/**
	 * @param componentImplementation
	 * @return ComponentAdapter
	 */
	public ComponentAdapter registerImplementation(
		Class<?> componentImplementation) {
    	return registerComponentImplementation(componentImplementation);
    }

	/**
	 * @param componentKey
	 * @param componentImplementation
	 * @return ComponentAdapter
	 */
	public ComponentAdapter registerImplementation(
		Object componentKey, Class<?> componentImplementation) {
		return registerComponentImplementation(
			componentKey, componentImplementation);
	}

	/**
	 * @param componentInstance
	 * @return ComponentAdapter
	 */
	public ComponentAdapter registerInstance(Object componentInstance) {
        return registerComponentInstance(componentInstance);
    }

	/**
	 * @param componentKey
	 * @param componentInstance
	 * @return ComponentAdapter
	 */
	public ComponentAdapter registerInstance(
    	Object componentKey, Object componentInstance) {
    	return registerComponentInstance(componentKey, componentInstance);
    }

	/**
	 * @param componentImplementation
	 * @return ComponentAdapter
	 */
	public ComponentAdapter reRegisterImplementation(
		Class<?> componentImplementation) {
		return reRegisterImplementation(
			componentImplementation, componentImplementation);
	}

	/**
	 * @param componentKey
	 * @param componentImplementation
	 * @return ComponentAdapter
	 */
	public ComponentAdapter reRegisterImplementation(
		Object componentKey, Class<?> componentImplementation) {
		unregisterComponent(componentKey);
		return registerComponentImplementation(componentKey,
			componentImplementation);
	}

	/**
	 * @param componentInstance
	 * @return ComponentAdapter
	 */
	public ComponentAdapter reRegisterInstance(Object componentInstance) {
		return reRegisterInstance(
			componentInstance.getClass(), componentInstance);
	}

	/**
	 * @param componentKey
	 * @param componentInstance
	 * @return ComponentAdapter
	 */
	public ComponentAdapter reRegisterInstance(
		Object componentKey, Object componentInstance) {
		unregisterComponent(componentKey);
		return registerInstance(componentKey, componentInstance);
	}
}
