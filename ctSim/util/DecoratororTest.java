package ctSim.util;

import static ctSim.util.Decoratoror.doesImplement;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Testklasse
 */
public class DecoratororTest {
	/**
	 * Test-Interface
	 */
	interface A                    { /* some type */ }
	/**
	 * Test-Interface
	 */
	interface SubA    extends A    { /* some type */ }
	/**
	 * Test-Interface
	 */
	interface SubSubA extends SubA { /* some type */ }
	/**
	 * Test-Interface
	 */
	interface B { /* some type */ }
	
	/**
	 * Test-Interface
	 */
	interface C {
		/**
		 * @param someSubA
		 */
		void doSomething(SubA someSubA); // for parameter testing
		/**
		 * @return SubA
		 */
		SubA getA(); // for name and return type testing
	}

	/**
	 * @throws NoSuchMethodException
	 */
	@Test
	@SuppressWarnings("unused")
	public void doesImplementWithWrongName() throws NoSuchMethodException {
		// wrong name
		assertFalse(doesImplement(
			new Object() {
				public SubA getU() { return null; }
			}.getClass().getDeclaredMethods()[0],
			C.class.getMethod("getA", new Class[] {})));
	}

	/**
	 * @throws NoSuchMethodException
	 */
	@Test
	@SuppressWarnings("unused")
	public void doesImplementReturnTypeStuff() throws NoSuchMethodException {
		// wrong: unrelated return type
		assertFalse(doesImplement(
			new Object() {
				public B getA() { return null; }
			}.getClass().getDeclaredMethods()[0],
			C.class.getMethod("getA", new Class[] {})));

		// wrong: return type too general
		assertFalse(doesImplement(
			new Object() {
				public A getA() { return null; }
			}.getClass().getDeclaredMethods()[0],
			C.class.getMethod("getA", new Class[] {})));

		// ok: return type same
		assertTrue(doesImplement(
			new Object() {
				public SubA getA() { return null; }
			}.getClass().getDeclaredMethods()[0],
			C.class.getMethod("getA", new Class[] {})));

		// ok: return type specialized
		assertTrue(doesImplement(
			new Object() {
				public SubSubA getA() { return null; }
			}.getClass().getDeclaredMethods()[0],
			C.class.getMethod("getA", new Class[] {})));
	}

	/**
	 * 
	 */
	@Test
	@SuppressWarnings("unused")
	public void doesImplementParameterStuff() {
		// wrong: too many params
		assertFalse(doesImplement(
			new Object() {
				public void doSomething(SubA x, SubA y) { /* ... */ }
			}.getClass().getDeclaredMethods()[0],
			C.class.getDeclaredMethods()[0]));

		// wrong: too few params
		assertFalse(doesImplement(
			new Object() {
				public void doSomething() { /* ... */ }
			}.getClass().getDeclaredMethods()[0],
			C.class.getDeclaredMethods()[0]));

		// wrong: right number, type too general
		assertFalse(doesImplement(
			new Object() {
				public void doSomething(A x) { /* ... */ }
			}.getClass().getDeclaredMethods()[0],
			C.class.getDeclaredMethods()[0]));

		// wrong: right number, type too specific
		assertFalse(doesImplement(
			new Object() {
				public void doSomething(SubSubA x) { /* ... */ }
			}.getClass().getDeclaredMethods()[0],
			C.class.getDeclaredMethods()[0]));

		// ok
		assertFalse(doesImplement(
			new Object() {
				public void doSomething(SubA x) { /* ... */ }
			}.getClass().getDeclaredMethods()[0],
			C.class.getDeclaredMethods()[0]));

	}
}
