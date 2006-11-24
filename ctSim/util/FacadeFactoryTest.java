package ctSim.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

//$$$ ist das hier schon fertig?
public class FacadeFactoryTest {
	public interface A { int getAnswer(); }
	public interface SubA extends A { /* empty interface */ }
	public interface B { /* empty */ }
	public interface Q { String getQuestion(); }
	public interface LifeUniverseEverything extends A, Q { /* empty */ }

	public static class SubAImpl implements SubA {
		public int getAnswer() {
			return 42;
		}
	}

	public HashMap<Class<?>, Object> ifcImplMap;
	public A someA;
	public B someB;
	public Q someQ;

	// runs before each test method
	@Before
	public void setUp() {
		ifcImplMap = new HashMap<Class<?>, Object>();
		someA = new A() {
			public int getAnswer() { return 42; }
		};
		someQ = new Q() {
			public String getQuestion() { return "6 * 9"; }
		};
		someB = new B() { /* empty */ };
	}

	@Test(expected=IllegalArgumentException.class)
	public void createFacadeWrongPartial() {
		// needed: subtype of LifeUniverseEverything, i.e. Q or A
		// supplying: Q and B
		ifcImplMap.put(Q.class, someQ);
		ifcImplMap.put(B.class, someB);
		FacadeFactory.createFacade(LifeUniverseEverything.class, ifcImplMap);
	}

	@Test(expected=IllegalArgumentException.class)
	public void createFacadeForgettingToSupplyQ() {
		ifcImplMap.put(A.class, someA);
		FacadeFactory.createFacade(LifeUniverseEverything.class, ifcImplMap);
	}

	@Test(expected=IllegalArgumentException.class)
	public void createFacadeSupplyingSameInterfaceAsFacadeAndPartial() {
		ifcImplMap.put(LifeUniverseEverything.class,
			new LifeUniverseEverything() {
				public int getAnswer() { return 0; }
				public String getQuestion() { return ""; } });
		FacadeFactory.createFacade(LifeUniverseEverything.class, ifcImplMap);
	}

	@Test(expected=IllegalArgumentException.class)
	public void createFacadeFacadeClassInsteadOfInterface() {
		FacadeFactory.createFacade(SubAImpl.class, ifcImplMap);
	}

	@Test(expected=IllegalArgumentException.class)
	public void createFacadeMapContainsSomethingElseThanInterfaces() {
		ifcImplMap.put(Q.class, someQ);
		ifcImplMap.put(SubAImpl.class, new SubAImpl());
		FacadeFactory.createFacade(LifeUniverseEverything.class, ifcImplMap);
	}

	@Test(expected=IllegalArgumentException.class)
	public void createFacadeMapContainsMoreImplsThanNecessary() {
		ifcImplMap.put(A.class, someA);
		ifcImplMap.put(Q.class, someQ);
		ifcImplMap.put(SubA.class, someA);
		FacadeFactory.createFacade(LifeUniverseEverything.class, ifcImplMap);
	}

	public void testCreateFacade() {
		ifcImplMap.put(A.class, someA);
		ifcImplMap.put(Q.class, someQ);
		LifeUniverseEverything facade = FacadeFactory.createFacade(
			LifeUniverseEverything.class, ifcImplMap);
		assertEquals(42, facade.getAnswer());
		assertEquals("6 * 9", facade.getQuestion());
	}

	@Test
	public void testCreateFacadeForTwo() {
		fail("Not yet implemented");
	}
}
