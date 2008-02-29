package ctSim.util;

import java.util.Arrays;

import org.junit.Test;
import static ctSim.util.Misc.*;
import static org.junit.Assert.*;

/**
 * Test-Klasse
 */
public class MiscTest {
	/**
	 * Tests
	 */
	@Test
	public void beginWith() {
		// einer auf der Whitelist
		assertTrue(startsWith("uuu", "u"));
		assertTrue(startsWith("u", "u"));
		assertTrue(startsWith("uuu", ""));
		assertTrue(startsWith("", ""));
		assertTrue(startsWith("uuu", "uuu"));

		assertFalse(startsWith("uuu", "uuuu"));
		assertFalse(startsWith("uuu", "e"));
		assertFalse(startsWith("ttt", "T"));
		assertFalse(startsWith("uuu", (String[])null));
		assertFalse(startsWith("", "u"));

		// mehrere auf der Whitelist
		assertTrue(startsWith("222", "2", "7"));
		assertTrue(startsWith("222", "7", "2"));
		assertTrue(startsWith("222", null, "7", "22", ""));

		assertFalse(startsWith("222", "9", "7"));
		assertFalse(startsWith("222", "9", "7", null, "3"));
		assertFalse(startsWith("222", "9", "7", null, "2222"));
	}

	/**
	 * Tests
	 */
	@Test(expected=NullPointerException.class)
	public void intersperseWithNullSep() {
		intersperse(null, "a");
	}

	/**
	 * Tests
	 */
	@Test
	public void testIntersperse() {
		String[] str;

		str = intersperse("u", "a");
		assertEquals(1, str.length);
		assertTrue(Arrays.equals(str, new String[] {"a"}));

		str = intersperse("m", null, "b");
		assertEquals(3, str.length);
		assertTrue(Arrays.equals(str, new String[] {null, "m", "b"}));

		str = intersperse("u", "a", "b", "c");
		assertEquals(5, str.length);
		assertTrue(Arrays.equals(str, new String[] {"a", "u", "b", "u", "c"}));

		str = intersperse("u", "a", "b", "c", "d", "e", "f");
		assertEquals(11, str.length);
		assertTrue(Arrays.equals(str, new String[] {"a", "u", "b", "u", "c",
			"u", "d", "u", "e", "u", "f"}));

		str = intersperse("f", null, null);
		assertEquals(3, str.length);
		assertTrue(Arrays.equals(str, new String[] {null, "f", null}));

		Integer[] intg = intersperse(42, 1, 2);
		assertEquals(3, intg.length);
		assertTrue(Arrays.equals(intg, new Integer[] {1, 42, 2}));
	}

	/**
	 * Tests
	 */
	@Test
	public void testJoin() {
		assertEquals("a", join("a"));
		assertEquals("abc", join("a", "b", "c"));
		assertEquals("lkjjkl", join("lkj", "jkl"));
		assertEquals("null", join((String)null));
		assertEquals("anullu", join("a", null, "u"));
	}
}
