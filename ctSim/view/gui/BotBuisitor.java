package ctSim.view.gui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.swing.JPanel;

public abstract class BotBuisitor extends JPanel {
	private boolean shouldBeDisplayed = false;

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Buisit {
		// Marker-Annotation
	}

	public void visit(Object o) {
		for (Method m : getClass().getMethods()) {
			if (! m.isAnnotationPresent(Buisit.class))
				continue;
			Class[] parms = m.getParameterTypes();
			if (parms.length != 1)
				continue;
			if (! ((Class<?>)parms[0]).isAssignableFrom(o.getClass()))
				continue;
			try {
				m.invoke(this, new Object[] {o});
				shouldBeDisplayed = true;
			} catch (Exception e) {
				// kann nicht passieren ... eigentlich ...
				throw new AssertionError(e);
			}
		}
	}

	public boolean shouldBeDisplayed() {
		return shouldBeDisplayed;
	}
}
