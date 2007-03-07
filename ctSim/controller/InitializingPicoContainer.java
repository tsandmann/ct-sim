/**
 *
 */
package ctSim.controller;

import java.util.List;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.DefaultPicoContainer;

//$$ doc InitializingPicoContainer
// initiert Impls, aber nicht Keys (braucht man das jemals?)
public class InitializingPicoContainer extends DefaultPicoContainer {
    private static final long serialVersionUID = - 6940983694133437239L;

    public InitializingPicoContainer() {
    	super();
    }

	public InitializingPicoContainer(PicoContainer parent) {
		super(parent);
    }

	@SuppressWarnings("unchecked")
    public <T> T get(Class<T> componentKey) {
	    return (T)super.getComponentInstance(componentKey);
	}

	private void ensureInitialized(Class<?> classToInitialize) {
		try {
	        Class.forName(classToInitialize.getName());
        } catch (ClassNotFoundException e) {
        	//$$ Obskurer Fehler
        	throw new RuntimeException(e);
        }
	}

	//$$ Methode weg?
	public void registerImplementationMap(Class<?>... classMap) {
		if (classMap.length % 2 != 0)
			throw new IllegalArgumentException();
		for (int i = 0; i < classMap.length; i += 2)
			registerComponentImplementation(classMap[i], classMap[i + 1]);
    }

	public void registerImplementations(Class<?>... classes) {
		for (Class<?> c : classes)
			registerComponentImplementation(c);
	}

	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
    @Override
    public ComponentAdapter registerComponentImplementation(
    	Class componentImplementation) {
	    ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentImplementation);
    }

	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
	@Override
    public ComponentAdapter registerComponentImplementation(
    	Object componentKey, Class componentImplementation) {
		ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentKey,
	        componentImplementation);
    }

	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
	@Override
    public ComponentAdapter registerComponentImplementation(
    	Object componentKey, Class componentImplementation, List parameters) {
		ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentKey,
	        componentImplementation, parameters);
    }

	@SuppressWarnings("unchecked") // Geerbter Class-, nicht Class<?>-Parameter
	@Override
    public ComponentAdapter registerComponentImplementation(
    	Object componentKey, Class componentImplementation,
    	Parameter[] parameters) {
		ensureInitialized(componentImplementation);
	    return super.registerComponentImplementation(componentKey,
	        componentImplementation, parameters);
    }

	public ComponentAdapter registerImplementation(
		Class<?> componentImplementation) {
    	return registerComponentImplementation(componentImplementation);
    }

	public ComponentAdapter registerImplementation(
		Object componentKey, Class<?> componentImplementation) {
		return registerComponentImplementation(
			componentKey, componentImplementation);
	}

	public ComponentAdapter registerInstance(Object componentInstance) {
        return registerComponentInstance(componentInstance);
    }

	public ComponentAdapter registerInstance(
    	Object componentKey, Object componentInstance) {
    	return registerComponentInstance(componentKey, componentInstance);
    }

	//$$ reRegister-Methoden koennen vielleicht weg und in die normalen register-Methoden integriert werden; man muss nachdenken ob diese Aenderung der Pico-Semantik wuenschenswert ist
	public ComponentAdapter reRegisterImplementation(
		Class<?> componentImplementation) {
		return reRegisterImplementation(
			componentImplementation, componentImplementation);
	}

	public ComponentAdapter reRegisterImplementation(
		Object componentKey, Class<?> componentImplementation) {
		unregisterComponent(componentKey);
		return registerComponentImplementation(componentKey,
			componentImplementation);
	}

	public ComponentAdapter reRegisterInstance(Object componentInstance) {
		return reRegisterInstance(
			componentInstance.getClass(), componentInstance);
	}

	public ComponentAdapter reRegisterInstance(
		Object componentKey, Object componentInstance) {
		unregisterComponent(componentKey);
		return registerInstance(componentKey, componentInstance);
	}
}
