package ctSim.view.gui;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import javax.swing.JPanel;

import ctSim.model.bots.Bot;

public abstract class BotBuisitor extends JPanel {
	private boolean shouldBeDisplayed = false;

	public BotBuisitor() {
		super(true); // DoubleBuffering an
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Buisit {
		// Marker-Annotation
	}

	public void visit(Object o, Bot bot) {
		for (Method m : getClass().getMethods()) {
			if (! m.isAnnotationPresent(Buisit.class))
				continue;
			try {
				if (hasSignature(m, o.getClass())) {
					m.invoke(this, new Object[] {o});
					shouldBeDisplayed = true;
				}
				if (hasSignature(m, o.getClass(), bot.getClass())) {
					m.invoke(this, new Object[] {o, bot});
					shouldBeDisplayed = true;
				}
			} catch (Exception e) {
				// kann nicht passieren ... eigentlich ...
				throw new AssertionError(e);
			}
		}
	}

	private static boolean hasSignature(Method m, Class... types) {
		Class[] pt = m.getParameterTypes();
		if (pt.length != types.length)
			return false;
		for (int i = 0; i < pt.length; i++) {
			if (! ((Class<?>)pt[i]).isAssignableFrom(types[i]))
				return false;
		}
		return true;
	}

	public boolean shouldBeDisplayed() {
		return shouldBeDisplayed;
	}
}
